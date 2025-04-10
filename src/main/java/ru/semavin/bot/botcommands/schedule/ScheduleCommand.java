package ru.semavin.bot.botcommands.schedule;

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
public class ScheduleCommand implements BotCommand {

    private final MessageSenderService messageSenderService;
    private final UserService userService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "Расписание".equalsIgnoreCase(message.getText().trim());
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
        return userService.getUserForTelegramTag(message.getFrom().getUserName()).thenAccept(userDTO -> {
            if (userService.isStarosta(userDTO)){
                messageSenderService.sendButtonMessage(KeyboardUtils.createMessageScheduleMenuForStarosta(chatId))
                        .thenAccept(msg ->
                        {
                            log.info("Команда 'Расписание' успешно выполнена для чата {}", chatId);
                        });
            }else {
                messageSenderService.sendButtonMessage(KeyboardUtils.createMessageScheduleMenu(chatId))
                        .thenAccept(msg ->
                        {
                            log.info("Команда 'Расписание' успешно выполнена для чата {}", chatId);
                        });
            };
        });
    }
}
