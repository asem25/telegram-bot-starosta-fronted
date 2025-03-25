package ru.semavin.bot.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BotRegisterConfig {
    @Value("${bot.token}")
    private String botToken;

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsLongPollingApplication() {
        return new TelegramBotsLongPollingApplication();
    }

    @Bean
    public CommandLineRunner registerBotRunner(TelegramBotsLongPollingApplication longPollingApp,
                                               LongPollingUpdateConsumer updateConsumer) {
        log.info("Registering bot");
        return args -> {
            try {
                longPollingApp.registerBot(botToken, updateConsumer);
                log.info("Bot registered successfully");
            } catch (TelegramApiException e) {
                log.error("Error registering bot: {}", e.getMessage());

            }
        };

    }
}
