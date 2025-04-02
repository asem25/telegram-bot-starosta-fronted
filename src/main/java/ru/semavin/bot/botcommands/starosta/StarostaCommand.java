package ru.semavin.bot.botcommands.starosta;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;

import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Component
public class StarostaCommand implements BotCommand {
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
        return "Староста".equalsIgnoreCase(message.getText().trim());
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
                messageSenderService.sendButtonMessage(KeyboardUtils.createMessageWithStarostaMenu(chatId));
            } else {
                messageSenderService.sendTextMessage(chatId, "Похоже, что Вы не староста!");
            }
        }).exceptionally(e -> {
            log.error("Ошибка при проверке роли старосты: {}", e.getMessage());
            messageSenderService.sendTextMessage(chatId, "Ошибка при выполнении команды");
            return null;
        });
    }
}
