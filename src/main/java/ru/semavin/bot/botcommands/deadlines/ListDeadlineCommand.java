package ru.semavin.bot.botcommands.deadlines;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.deadline.DeadlineService;
import ru.semavin.bot.service.users.UserService;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class ListDeadlineCommand implements BotCommand {
    private final UserService userService;
    private final MessageSenderService messageSenderService;
    private final DeadlineService deadlineService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return message.getText().equalsIgnoreCase("Дедлайны");
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

        return userService.getUserForTelegramTag(username)
                .thenCompose(user -> sendDeadlines(chatId, user));
    }

    private CompletableFuture<Void> sendDeadlines(Long chatId, UserDTO user) {
        boolean isStarosta = "STAROSTA".equalsIgnoreCase(user.getRole());

        var messages = deadlineService.buildDeadlineMessages(user.getGroupName(), chatId, isStarosta);
        CompletableFuture<Void> all = CompletableFuture.completedFuture(null);

        for (var msg : messages) {
            all = all.thenCompose(v -> messageSenderService.sendButtonMessage(msg).thenApply(resp -> null));
        }

        return all;
    }
}
