package edu.telegrambots.myfirstbot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {

    @Value("${bot.name}")
    String botName;
    @Value("${bot.token}")
    String botToken;
    // TODO: реализовать возможность задавать множество суперпользователей в application.properties
    @Value("${bot.superusers}")
    ArrayList<Long> botSuperusers;
}
