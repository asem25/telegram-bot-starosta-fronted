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
        userService.getUserForTelegramTag(message.getFrom().getUserName()).thenAccept(user -> {
            if (!"STAROSTA".equalsIgnoreCase(user.getRole())) {
                messageSenderService.sendTextMessage(chatId, "У вас нет доступа к этой команде!");
                log.warn("{} попытался воспользоваться командой 'Пропуски'", user.getUsername());
            } else {
                SkipNotificationService.MissedResponse result = skipNotificationService.formatGroupAbsencesWithKeyboard(user.getGroupName());


                messageSenderService.sendButtonMessage(
                        SendMessage.builder()
                                .chatId(chatId.toString())
                                .text(result.getText())
                                .parseMode("Markdown")
                                .replyMarkup(result.getMarkup())
                                .build()
                );
            }
        });
        return CompletableFuture.completedFuture(null);
    }
}
