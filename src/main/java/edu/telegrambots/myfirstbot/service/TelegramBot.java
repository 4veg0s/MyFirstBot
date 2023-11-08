package edu.telegrambots.myfirstbot.service;

import com.vdurmont.emoji.EmojiParser;
import edu.telegrambots.myfirstbot.config.BotConfig;
import edu.telegrambots.myfirstbot.enums.UserState;
import edu.telegrambots.myfirstbot.model.User;
import edu.telegrambots.myfirstbot.model.UserEduInfo;
import edu.telegrambots.myfirstbot.model.UserEduInfoRepository;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* TODO:
    --реализовать вывод сообщения "сейчас нет ни одной активной команды при нажатии /cancel с помощью проверки состояния пользователя
*   --реализовать отдельное редактирование факультета и тд
*   --добавить специалитет в degree
*/


@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserEduInfoRepository userEduInfoRepository;
    final BotConfig config;

    List<Long> superUsers;
    List<BotCommand> listOfCommands;
    static final String MY_DATA_TEMPLATE_BOT_INFO = "Ваши пользовательские данные:%n%n" +
            "ID чата: %d%n" +
            "Имя: %s%n" +
            "Фамилия: %s%n" +
            "Имя пользователя: %s%n%n" +
            "Время последней регистрации в боте: %s%n";

    static final String MY_DATA_TEMPLATE_EDU_INFO = "Ваши студенческие данные:%n%n" +
            "Уровень образования: %s%n" +
            "Факультет: %s%n" +
            "Курс: %s%n" +
            "Группа: %s%n";

    static final String START_TEXT = "Здравствуй, %s!" +
            "На данный момент бот может корректно реагировать только на команды, доступные из меню";
    static final String HELP_TEXT = "Самый быстрый и доступный частный VPN :zap:Blitz:zap:\n" +
            "Для подключения обращайтесь в личные сообщения @VPN_Blitz\n\n" +
            "Вы можете использовать команды из главного меню слева или набирать их вручную:\n\n" +
            "Наберите /start, чтобы увидеть приветственное сообщение\n\n" +
            "Наберите /edit_edu_info, чтобы изменить информацию о факультете, курсе и группе\n\n" +
            "Наберите /my_data, чтобы увидеть собранные о Вас данные\n\n" +
            //"Наберите /delete_my_data, чтобы удалить собранные о Вас данные\n\n" +
            "Наберите /help, чтобы увидеть это сообщение снова\n\n" +
            "Наберите /settings, чтобы изменить некоторые настройки бота\n(функционал в разработке)" +
            "Наберите /cancel, чтобы отменить выполнение последней команды";


    public TelegramBot(BotConfig config) {
        this.config = config;

        superUsers = new ArrayList<>();
        superUsers.add((long) 416657716);   // Андрей

        listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "начать общение"));
        listOfCommands.add(new BotCommand("/edit_edu_info", "изменить учебную информацию"));
        listOfCommands.add(new BotCommand("/my_data", "просмотреть собранные данные"));
        listOfCommands.add(new BotCommand("/delete_my_data", "удалить собранные данные"));
        listOfCommands.add(new BotCommand("/help", "справочная информация о боте"));
        listOfCommands.add(new BotCommand("/settings", "персонализированная настройка"));
        listOfCommands.add(new BotCommand("/cancel", "отмена выполняемой команды"));

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
                case "/start" -> {
                    startCommandReceived(update);
                }
                case "/edit_edu_info" -> {
                    editEduInfoCommandReceived(chatId);
                }
                case "/my_data" -> {
                    myDataCommandReceived(chatId, update.getMessage().getChat().getUserName());
                }
                case "/delete_my_data" -> {
                    deleteMyDataMessageReceived(chatId, update.getMessage().getChat().getUserName());
                }
                case "/help" -> {
                    helpCommandReceived(chatId, update.getMessage().getChat().getUserName());
                }
                case "/cancel" -> {
                    cancelCommandReceived(chatId);
                }
                default -> unusualMessageReceived(update);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();

            switch (callbackData) {
                case CallbackConstants.YES_DELETE_MY_DATA -> {
                    confirmDeleteMyData(update);
                }
                case CallbackConstants.NO_DELETE_MY_DATA -> {
                    cancelDeleteMyData(update);
                }
                case CallbackConstants.YES_FACULTY -> {
                    confirmFaculty(update);
                }
                case CallbackConstants.NO_FACULTY -> {
                    returnToFaculty(update);
                }
                case CallbackConstants.YES_DEGREE -> {
                    confirmDegree(update);
                }
                case CallbackConstants.NO_DEGREE -> {
                    returnToDegree(update);
                }
                case CallbackConstants.YES_CANCEL -> {
                    confirmCancel(update);
                }
                case CallbackConstants.NO_CANCEL -> {
                    returnToCommand(update);
                }
                default -> {
                    if (CallbackConstants.FACULTY_FA.equals(callbackData) ||
                            CallbackConstants.FACULTY_FCE.equals(callbackData) ||
                            CallbackConstants.FACULTY_FEE_MS.equals(callbackData) ||
                            CallbackConstants.FACULTY_FARB.equals(callbackData) ||
                            CallbackConstants.FACULTY_FEM.equals(callbackData) ||
                            CallbackConstants.FACULTY_FFLCT.equals(callbackData) ||
                            CallbackConstants.FACULTY_IPTS.equals(callbackData) ||
                            CallbackConstants.FACULTY_ICE.equals(callbackData)) {
                        facultyReceived(update);
                    }
                    if (CallbackConstants.DEGREE_BACHELOR.equals(callbackData) ||
                            CallbackConstants.DEGREE_MASTER.equals(callbackData)) {
                        degreeReceived(update);
                    }
                }
            }
        }
    }

    private void returnToFaculty(Update update) {
        EditMessageText message = editMessage(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), "Выбор отменен");
        executeMessage(message);
        editFacultyMessage(update.getCallbackQuery().getMessage().getChatId());
        //editEduInfoCommandReceived(update.getCallbackQuery().getMessage().getChatId());
    }
    private void confirmFaculty(Update update) {
        EditMessageText message;
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String textToSend = "Факультет изменен на %s";
        try {
            String faculty = findUserInUserEduInfoRepository(chatId).getFaculty();
            message = editMessage(chatId, messageId, String.format(textToSend, faculty));
        } catch (NullPointerException exception) {
            message = editMessage(chatId, messageId, "Ошибка изменения факультета");
            log.error("Couldn't find user in userEduInfoRepository with chatId = " + chatId);
        }
        executeMessage(message);

        editDegreeMessage(chatId);     // запуск выбора уровня образования после успешного выбора факультета
    }

    private void returnToDegree(Update update) {
        EditMessageText message = editMessage(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getMessage().getMessageId(), "Выбор отменен");
        executeMessage(message);
        editDegreeMessage(update.getCallbackQuery().getMessage().getChatId());
        //editEduInfoCommandReceived(update.getCallbackQuery().getMessage().getChatId());
    }

    private void confirmDegree(Update update) {
        EditMessageText message;
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String textToSend = "Уровень образовательной программы изменен на %s";
        try {
            String degree = findUserInUserEduInfoRepository(chatId).getDegree();
            message = editMessage(chatId, messageId, String.format(textToSend, degree));
        } catch (NullPointerException exception) {
            message = editMessage(chatId, messageId, "Ошибка изменения уровня образования");
            log.error("Couldn't find user in userEduInfoRepository with chatId = " + chatId);
        }
        executeMessage(message);
    }

    private UserEduInfo findUserInUserEduInfoRepository(long chatId) {
        if (userEduInfoRepository.findById(chatId).isEmpty()) {
            return null;
        } else {
            return userEduInfoRepository.findById(chatId).get();
        }
    }

    private void changeUserFaculty(long chatId, String faculty) {
        var user = findUserInUserEduInfoRepository(chatId);
        if (user != null) {
            user.setFaculty(faculty);
            userEduInfoRepository.save(user);
        }
    }

    private void changeUserDegree(long chatId, String degree) {
        var user = findUserInUserEduInfoRepository(chatId);
        if (user != null) {
            user.setDegree(degree);
            userEduInfoRepository.save(user);
        }
    }

    private void registerEduInfo(Message msg) {

        if (findUserInUserRepository(msg.getChatId()) == null) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setState(UserState.BASIC_STATE);
            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    private void facultyReceived(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        String textToSend = "Факультет %s, верно?";
        String faculty = facultyCallbackToName(callbackData);
        changeUserFaculty(chatId, faculty);

        EditMessageText message = editMessage(chatId, messageId, String.format(textToSend, faculty), yesNoInlineMarkup(CallbackConstants.YES_FACULTY, CallbackConstants.NO_FACULTY));
        executeMessage(message);
    }

    private void degreeReceived(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        String callbackData = update.getCallbackQuery().getData();
        String textToSend = "%s, верно?";
        String degree = degreeCallbackToName(callbackData);
        changeUserDegree(chatId, degree);

        EditMessageText message = editMessage(chatId, messageId, String.format(textToSend, degree), yesNoInlineMarkup(CallbackConstants.YES_DEGREE, CallbackConstants.NO_DEGREE));
        executeMessage(message);
    }

    private String facultyCallbackToName(String callbackData) {
        String faculty = "ОШИБКА";
        switch (callbackData) {
            case CallbackConstants.FACULTY_FA -> faculty = "АФ";
            case CallbackConstants.FACULTY_FCE -> faculty = "СФ";
            case CallbackConstants.FACULTY_FEE_MS -> faculty = "ФИЭиГХ";
            case CallbackConstants.FACULTY_FARB -> faculty = "АДФ";
            case CallbackConstants.FACULTY_FEM -> faculty = "ФЭУ";
            case CallbackConstants.FACULTY_FFLCT -> faculty = "ФСЭиПСТ";
            case CallbackConstants.FACULTY_IPTS -> faculty = "ИБФО";
            case CallbackConstants.FACULTY_ICE -> faculty = "ИДО";
        }
        return faculty;
    }

    private String degreeCallbackToName(String callbackData) {
        String degree = "ОШИБКА";
        switch (callbackData) {
            case CallbackConstants.DEGREE_BACHELOR -> degree = "Бакалавриат";
            case CallbackConstants.DEGREE_MASTER -> degree = "Магистратура";
        }
        return degree;
    }

    private void editEduInfoCommandReceived(long chatId) {
        editFacultyMessage(chatId);
    }

    private void editDegreeMessage(long chatId) {
        sendMessage(chatId, "Выберите уровень образовательной программы:", degreeInlineMarkup());
    }

    private void editFacultyMessage(long chatId) {
        sendMessage(chatId, "Выберите факультет:", facultyInlineMarkup());
    }

    private InlineKeyboardMarkup degreeInlineMarkup() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("Бакалавриат", CallbackConstants.DEGREE_BACHELOR))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("Магистратура", CallbackConstants.DEGREE_MASTER))));

        keyboardMarkup.setKeyboard(rowsInline);

        return keyboardMarkup;
    }

    private InlineKeyboardMarkup facultyInlineMarkup() {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("АФ", CallbackConstants.FACULTY_FA))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("СФ", CallbackConstants.FACULTY_FCE))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("ФИЭиГХ", CallbackConstants.FACULTY_FEE_MS))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("АДФ", CallbackConstants.FACULTY_FARB))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("ФЭУ", CallbackConstants.FACULTY_FEM))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("ФСЭиПСТ", CallbackConstants.FACULTY_FFLCT))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("ИБФО", CallbackConstants.FACULTY_IPTS))));
        rowsInline.add(new ArrayList<>(Arrays.asList(createInlineButton("ИДО", CallbackConstants.FACULTY_ICE))));

        keyboardMarkup.setKeyboard(rowsInline);

        return keyboardMarkup;
    }

    private InlineKeyboardButton createInlineButton(String name, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(name);
        button.setCallbackData(callbackData);

        return button;
    }

    private void cancelDeleteMyData(Update update) {
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        EditMessageText message = editMessage(chatId, messageId, "Ваши данные не были удалены");

        executeMessage(message);
    }

    private void confirmDeleteMyData(Update update) {
        int messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        userRepository.deleteById(chatId);
        String textToSend;
        if (userRepository.findById(chatId).isEmpty()) {
            textToSend = "Ваши данные были успешно удалены";
            log.info("Successfully deleted data of user https://t.me/" + update.getCallbackQuery().getMessage().getChat().getUserName() + " with chatId = " + chatId);
        } else {
            textToSend = "При удалении ваших данных произошла ошибка";
            log.error("Error occurred while attempting to delete data of user https://t.me/" + update.getCallbackQuery().getMessage().getChat().getUserName() + " with chatId = " + chatId);
        }
        EditMessageText message = editMessage(chatId, messageId, textToSend);
        executeMessage(message);
    }

    private void returnToCommand(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        EditMessageText message = editMessage(chatId, messageId, "Возврат к команде");

        executeMessage(message);
    }

    private void confirmCancel(Update update) {
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        EditMessageText message = editMessage(chatId, messageId, "Команда отменена");

        setUserState(chatId, UserState.BASIC_STATE);

        executeMessage(message);
    }

    private void setUserState(long chatId, UserState state) {
        var user = findUserInUserRepository(chatId);
        user.setState(state);
        userRepository.save(user);
    }

    private void cancelCommandReceived(long chatId) {
        sendMessage(chatId, "Отменить команду?", yesNoInlineMarkup(CallbackConstants.YES_CANCEL, CallbackConstants.NO_CANCEL));
    }

    private void unusualMessageReceived(Update update) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        if (messageText.contains("/all")) {
            if (superUsers.contains(chatId)) {
                sendToEveryoneMessageReceived(chatId, update.getMessage().getChat().getUserName(), messageText);
            } else {
                sendMessage(chatId, "Вы не имеете доступа к этой команде");
            }
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


    private User findUserInUserRepository(long chatId) {
        if (userRepository.findById(chatId).isEmpty()) {
            return null;
        } else {
            return userRepository.findById(chatId).get();
        }
    }

    private void registerUser(Message msg) {

        if (findUserInUserRepository(msg.getChatId()) == null) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setState(UserState.BASIC_STATE);
            userRepository.save(user);
            log.info("User saved: " + user);

            UserEduInfo userEduInfo = new UserEduInfo();
            userEduInfo.setChatId(chatId);
            userEduInfoRepository.save(userEduInfo);
            log.info("UserEduInfo saved: " + userEduInfo);
        }
    }

    private void startCommandReceived(Update update) {
        long chatId = update.getMessage().getChatId();
        String name = update.getMessage().getChat().getFirstName();
        String userName = update.getMessage().getChat().getUserName();
        //String startResponse = String.format(START_TEXT, name, userName);
        String startResponse = String.format(START_TEXT, name);

        sendMessage(chatId, startResponse, replyKeyboardMarkup());
        log.info("Replied to START command from user https://t.me/" + userName + " with chatId = " + chatId);

        registerUser(update.getMessage());
        editEduInfoCommandReceived(chatId);
    }

    private void myDataCommandReceived(long chatId, String userName) {
        if (userRepository.findById(chatId).isEmpty()) {
            sendMessage(chatId, "Записей о Ваших пользовательских данных не обнаружено");
        } else {
            User user = findUserInUserRepository(chatId);
            sendMessage(chatId, String.format(MY_DATA_TEMPLATE_BOT_INFO, user.getChatId(), user.getFirstName(), user.getLastName(), user.getUserName(), user.getRegisteredAt()));
        }
        if (userEduInfoRepository.findById(chatId).isEmpty()) {
            sendMessage(chatId, "Записей о Ваших студенческих данных не обнаружено");
        } else {
            UserEduInfo userEduInfo = findUserInUserEduInfoRepository(chatId);
            sendMessage(chatId, String.format(MY_DATA_TEMPLATE_EDU_INFO, userEduInfo.getDegree(), userEduInfo.getFaculty(), userEduInfo.getCourse(), userEduInfo.getGroupName()));
        }
        log.info("Replied to MY_DATA command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void deleteMyDataMessageReceived(long chatId, String userName) {
        if (userRepository.findById(chatId).isEmpty()) {
            sendMessage(chatId, "Записей о Ваших данных не обнаружено");
        } else {
            sendMessage(chatId, "Вы уверены, что хотите удалить ваши данные из бота?", yesNoInlineMarkup(CallbackConstants.YES_DELETE_MY_DATA, CallbackConstants.NO_DELETE_MY_DATA));
        }
        log.info("Replied to DELETE_MY_DATA command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private InlineKeyboardMarkup yesNoInlineMarkup(String callbackNameYes, String callbackNameNo) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Да");
        yesButton.setCallbackData(callbackNameYes);

        var noButton = new InlineKeyboardButton();
        noButton.setText("Нет");
        noButton.setCallbackData(callbackNameNo);

        row.add(yesButton);
        row.add(noButton);

        rowsInline.add(row);

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        return inlineKeyboardMarkup;
    }

    private void helpCommandReceived(long chatId, String userName) {
        sendMessage(chatId, EmojiParser.parseToUnicode(HELP_TEXT));
        log.info("Replied to HELP command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        executeMessage(message);
    }

    private void sendMessage(long chatId, String textToSend, ReplyKeyboard keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
            log.info("Message sent successfully to user with chatId = " + message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Error occurred while attempting to send a message to user with chatId = " + message.getChatId() + ": " + e.getMessage());
        }
    }

    private void executeMessage(EditMessageText message) {
        try {
            execute(message);
            log.info("Message sent successfully to user with chatId = " + message.getChatId());
        } catch (TelegramApiException e) {
            log.error("Error occurred while attempting to send a message to user with chatId = " + message.getChatId() + ": " + e.getMessage());
        }
    }

    private EditMessageText editMessage(long chatId, int messageId, String textToSend) {
        EditMessageText message = new EditMessageText();

        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(textToSend);

        return message;
    }

    private EditMessageText editMessage(long chatId, int messageId, String textToSend, InlineKeyboardMarkup inlineKeyboardMarkup) {
        EditMessageText message = new EditMessageText();

        message.setChatId(chatId);
        message.setMessageId(messageId);
        message.setText(textToSend);
        message.setReplyMarkup(inlineKeyboardMarkup);

        return message;
    }

    private ReplyKeyboardMarkup replyKeyboardMarkup() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setIsPersistent(false);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add("/start");
        row1.add("/help");
        row1.add("/my_data");
        KeyboardRow row2 = new KeyboardRow();
        row2.add("/edit_edu_info");
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

}
