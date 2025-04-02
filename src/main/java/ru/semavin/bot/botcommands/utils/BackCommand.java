package ru.semavin.bot.botcommands.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;

import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Service
public class BackCommand implements BotCommand {
    private final UserService userService;
    private final MessageSenderService messageSenderService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "Назад".equalsIgnoreCase(message.getText().trim());
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

       return userService.getUserForTelegramTag(message.getFrom().getUserName()).thenAccept(user -> {
            if (userService.isStarosta(user)) {
                messageSenderService.sendButtonMessage(KeyboardUtils.createMessageStarostaMainMenu(chatId));
            } else {
                messageSenderService.sendButtonMessage(KeyboardUtils.createMessageBackMenu(chatId));
            }
        }).exceptionally(e -> {
            log.error("Ошибка при обработке команды 'Назад': {}", e.getMessage());
            messageSenderService.sendTextMessage(chatId, "Ошибка при выполнении команды");
            return null;
        }).thenApply(messageId -> {
                    return null;
                });
    }

}
