package ru.semavin.bot.botcommands.deadlines;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.deadline.DeadLineCreationService;

import java.util.concurrent.CompletableFuture;
@RequiredArgsConstructor
@Component
@Slf4j
public class AddDeadlineCommand implements BotCommand {
    private final DeadLineCreationService stateService;
    private final MessageSenderService messageSenderService;

    @Override
    public boolean canHandle(Message message) {
        return message.getText().equalsIgnoreCase("Добавить дедлайн");
    }

    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        String username = message.getFrom().getUserName();

        stateService.start(chatId, username);

        return messageSenderService.sendMessage(chatId, "Введите название дедлайна:")
                .thenAccept(response -> {})
                .exceptionally(e -> {
                    messageSenderService.sendTextMessage(chatId, "Ошибка при запуске создания дедлайна.");
                    return null;
                });
    }
}
