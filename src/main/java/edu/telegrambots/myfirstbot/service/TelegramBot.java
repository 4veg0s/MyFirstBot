package edu.telegrambots.myfirstbot.service;

import edu.telegrambots.myfirstbot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startMessageReceived(chatId, update.getMessage().getChat().getFirstName(), update.getMessage().getChat().getUserName());
                    break;
                default:
                    sendMessage(chatId, "Я пока не умею обрабатывать такие команды :(");
            }
        }
    }

    private void startMessageReceived(long chatId, String name, String userName) {
        String startResponse = "Привет, " + name + "! Или мне называть тебя @" + userName + "?";

        sendMessage(chatId, startResponse);
        log.info("Replied to start command from user https://t.me/" + userName + " with chatId = " + chatId);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
            log.info("Message sent successfully to user with chatId = " + chatId);
        }
        catch (TelegramApiException exception) {
            log.error("Error occurred: " + exception.getMessage());
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
