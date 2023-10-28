package edu.telegrambots.myfirstbot.service;

import com.vdurmont.emoji.EmojiParser;
import edu.telegrambots.myfirstbot.config.BotConfig;
import edu.telegrambots.myfirstbot.model.User;
import edu.telegrambots.myfirstbot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    static final String USER_DATA_TEMPLATE = "Ваши данные:%n%n" +
            "ID чата: %d%n" +
            "Имя: %s%n" +
            "Фамилия: %s%n" +
            "Имя пользователя: %s%n" +
            "Время последней регистрации в боте: %s";
    static final String START_TEXT = "Привет, %s! Или мне называть тебя @%s?%n%n" +
            "На данный момент бот может корректно реагировать только на команды, доступные из меню";
    static final String HELP_TEXT = "Самый быстрый и доступный частный VPN :zap:Blitz:zap:\n" +
            "Для подключения обращайтесь в личные сообщения @VPN_Blitz\n\n" +
            "Вы можете использовать команды из главного меню слева или набирать их вручную:\n\n" +
            "Наберите /start, чтобы увидеть приветственное сообщение\n\n" +
            "Наберите /mydata, чтобы увидеть собранные о Вас данные\n\n" +
            "Наберите /deletemydata, чтобы удалить собранные о Вас данные\n\n" +
            "Наберите /help, чтобы увидеть это сообщение снова\n\n" +
            "Наберите /settings, чтобы изменить некоторые настройки бота\n(функционал в разработке)";

    static final String YES_DELETE_MY_DATA = "YES_DELETE_MY_DATA";

    static final String NO_DELETE_MY_DATA = "NO_DELETE_MY_DATA";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "начать общение"));
        listOfCommands.add(new BotCommand("/mydata", "просмотреть собранные данные"));
        listOfCommands.add(new BotCommand("/deletemydata", "удалить собранные данные"));
        listOfCommands.add(new BotCommand("/help", "справочная информация о боте"));
        listOfCommands.add(new BotCommand("/settings", "персонализированная настройка"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error while setting up bot's command list: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startMessageReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getUserName());
                    registerUser(update.getMessage());
                    break;
                case "/mydata":
                    myDataMessageReceived(chatId, update.getMessage().getChat().getUserName());
                    break;
                case "/deletemydata":
                    deleteMyDataMessageReceived(chatId, update.getMessage().getChat().getUserName());
                    break;
                case "/help":
                    helpMessageReceived(chatId, update.getMessage().getChat().getUserName());
                    break;
                default:
                    unusualMessageReceived(update);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            int messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            EditMessageText message = new EditMessageText();

            switch (callbackData) {
                case YES_DELETE_MY_DATA:
                    userRepository.deleteById(chatId);
                    message.setChatId(chatId);
                    message.setMessageId(messageId);
                    if (userRepository.findById(chatId).isEmpty()) {
                        message.setText("Ваши данные были успешно удалены");
                        sendMessageExecute(message);
                        log.info("Successfully deleted data of user https://t.me/" + update.getCallbackQuery().getMessage().getChat().getUserName() + " with chatId = " + chatId);
                    } else {
                        message.setText("При удалении ваших данных произошла ошибка");
                        sendMessageExecute(message);
                        log.error("Error occurred while attempting to delete data of user https://t.me/" + update.getCallbackQuery().getMessage().getChat().getUserName() + " with chatId = " + chatId);
                    }
                    break;
                case NO_DELETE_MY_DATA:
                    message.setChatId(chatId);
                    message.setMessageId(messageId);
                    message.setText("Ваши данные не были удалены");
                    sendMessageExecute(message);
                    break;
            }
        }
    }

    private void unusualMessageReceived(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        if (messageText.contains("/all") && config.getBotSuperusers().contains(chatId)) {
            sendToEveryoneMessageReceived(chatId, update.getMessage().getChat().getUserName(), messageText);
        } else {
            sendMessage(chatId, "К сожалению, я пока не умею обрабатывать данную команду :(");
        }
    }

    private void sendToEveryoneMessageReceived(long chatId, String userName, String messageText) {

        for (User user : userRepository.findAll()) {
            sendMessage(user.getChatId(), messageText.substring(messageText.indexOf(" ")));
        }
        log.info("Successfully sent a message to every user from superuser https://t.me/" + userName + " with chatId = " + chatId);
    }


    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    private void startMessageReceived(long chatId, String name, String userName) {
        String startResponse = String.format(START_TEXT, name, userName);

        sendMessage(chatId, startResponse);
        log.info("Replied to START command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void myDataMessageReceived(long chatId, String userName) {
        if (userRepository.findById(chatId).isEmpty()) {
            sendMessage(chatId, "Записей о Ваших данных не обнаружено");
        } else {
            User user = userRepository.findById(chatId).get();
            sendMessage(chatId, String.format(USER_DATA_TEMPLATE, user.getChatId(), user.getFirstName(), user.getLastName(), user.getUserName(), user.getRegisteredAt()));
        }
        log.info("Replied to MY_DATA command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void deleteMyDataMessageReceived(long chatId, String userName) {
        if (userRepository.findById(chatId).isEmpty()) {
            sendMessage(chatId, "Записей о Ваших данных не обнаружено");
        } else {
            confirmDeleteMyData(chatId);
        }
        log.info("Replied to DELETE_MY_DATA command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void confirmDeleteMyData(long chatId) {
        SendMessage message = new SendMessage();
        message.setText("Вы уверены, что хотите удалить ваши данные из бота?");
        message.setChatId(chatId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData(YES_DELETE_MY_DATA);

        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(NO_DELETE_MY_DATA);

        row.add(yesButton);
        row.add(noButton);

        rowsInline.add(row);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        message.setReplyMarkup(inlineKeyboardMarkup);

        sendMessageExecute(message);
    }

    private void helpMessageReceived(long chatId, String userName) {
        sendMessage(chatId, EmojiParser.parseToUnicode(HELP_TEXT));
        log.info("Replied to HELP command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(keyboardMarkup());

        sendMessageExecute(message);
    }

    private void sendMessageExecute(SendMessage message) {
        try {
            execute(message);
            log.info("Message sent successfully to user with chatId = " + message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Error occurred while attempting to send a message to user with chatId = " + message.getChatId() + ": " + e.getMessage());
        }
    }

    private void sendMessageExecute(EditMessageText message) {
        try {
            execute(message);
            log.info("Message sent successfully to user with chatId = " + message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Error occurred while attempting to send a message to user with chatId = " + message.getChatId() + ": " + e.getMessage());
        }
    }

    private ReplyKeyboardMarkup keyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setIsPersistent(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/start");
        row1.add("/help");
        row1.add("/mydata");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("/deletemydata");
        row2.add("/settings");

        keyboardRows.add(row1);
        keyboardRows.add(row2);

        keyboardMarkup.setKeyboard(keyboardRows);

        return keyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    public ArrayList<Long> getBotSuperusers() {
        return config.getBotSuperusers();
    }
}
