package ru.semavin.bot.botcommands.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.util.KeyboardUtils;

import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Service
public class SettingCommand implements BotCommand {
    private final MessageSenderService messageSenderService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "Настройки".equalsIgnoreCase(message.getText().trim());
    }

    /**
     * Выполняет обработку команды.
     *
     * @param message входящее сообщение
     * @return CompletableFuture, позволяющий работать асинхронно
     */
    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        SendMessage settingsMessage = SendMessage.builder()
                .chatId(chatId.toString())
                .text("Настройки профиля")
                .replyMarkup(KeyboardUtils.ActionsMenu())
                .build();
        return messageSenderService.sendButtonMessage(settingsMessage)
                .thenAccept(msgId -> log.info("Команда 'Настройки' выполнена для чата {}", chatId));
    }
}
