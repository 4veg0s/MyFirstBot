package edu.telegrambots.myfirstbot.service;

import com.vdurmont.emoji.Emoji;
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
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Date;
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
    static final String HELP_TEXT = "Самый быстрый и доступный частный :zap:VPN Blitz:zap:\n" +
            "Для подключения обращайтесь в личные сообщения @VPN_Blitz\n\n" +
            "Вы можете использовать команды из главного меню слева или набирать их вручную:\n\n" +
            "Наберите /start, чтобы увидеть приветственное сообщение\n\n" +
            "Наберите /mydata, чтобы увидеть собранные о Вас данные\n(функционал в разработке)\n\n" +
            "Наберите /deletemydata, чтобы удалить собранные о Вас данные\n(функционал в разработке)\n\n" +
            "Наберите /help, чтобы увидеть это сообщение снова\n\n" +
            "Наберите /settings, чтобы изменить некоторые настройки бота\n(функционал в разработке)";

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
                case "/help":
                    helpMessageReceived(chatId, update.getMessage().getChat().getUserName());
                    break;
                default:
                    sendMessage(chatId, "К сожалению, я пока не умею обрабатывать данную команду :(");
            }
        }
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
        String startResponse = "Привет, " + name + "! Или мне называть тебя @" + userName + "?";

        sendMessage(chatId, startResponse);
        log.info("Replied to START command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void myDataMessageReceived(long chatId, String userName) {
        if (userRepository.findById(chatId).isEmpty()) {
            sendMessage(chatId, "Записей о Ваших данных не обнаружено");
        } else {
            User user = userRepository.findById(chatId).get();
            var date = new Date(user.getRegisteredAt().getTime());
            // FIXME
            System.out.println(user.getRegisteredAt().getTime());
            System.out.println(date);

            sendMessage(chatId, String.format(USER_DATA_TEMPLATE, user.getChatId(), user.getFirstName(), user.getLastName(), user.getUserName(), user.getRegisteredAt()));
        }

        log.info("Replied to MYDATA command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void helpMessageReceived(long chatId, String userName) {
        sendMessage(chatId, EmojiParser.parseToUnicode(HELP_TEXT));
        log.info("Replied to HELP command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
            log.info("Message sent successfully to user with chatId = " + chatId);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }
}
