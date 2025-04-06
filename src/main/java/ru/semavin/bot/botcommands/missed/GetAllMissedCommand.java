package ru.semavin.bot.botcommands.missed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.notification.SkipNotificationService;
import ru.semavin.bot.service.users.UserService;

import java.util.concurrent.CompletableFuture;
@RequiredArgsConstructor
@Service
@Slf4j
public class GetAllMissedCommand implements BotCommand {
    private final SkipNotificationService skipNotificationService;
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
        return message.getText().equalsIgnoreCase("Пропуски");
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
        String username = message.getFrom().getUserName();

        return userService.getUserForTelegramTag(username) // => CompletableFuture<UserDTO>
                .thenCompose(user -> {
                    if (!"STAROSTA".equalsIgnoreCase(user.getRole())) {
                        // Нет доступа — отправляем сообщение и завершаемся
                        messageSenderService.sendTextMessage(chatId, "У вас нет доступа к этой команде!");
                        log.warn("{} попытался воспользоваться командой 'Пропуски'", user.getUsername());
                        return CompletableFuture.completedFuture(null);
                    } else {
                        // Староста => получаем пропуски
                        return skipNotificationService.formatGroupAbsencesWithKeyboard(user.getGroupName())
                                .thenCompose(result -> {
                                    // result — объект MissedResponse
                                    SendMessage msg = SendMessage.builder()
                                            .chatId(chatId.toString())
                                            .text(result.getText())
                                            .parseMode("Markdown")
                                            .replyMarkup(result.getMarkup())
                                            .build();

                                    // Отправляем кнопочное сообщение
                                    return messageSenderService.sendButtonMessage(msg)
                                            // Когда сообщение отправлено, завершаем CompletableFuture<Void>
                                            .thenApply(apiResponse -> null);
                                });
                    }
                });
    }
}
