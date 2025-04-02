package ru.semavin.bot.botcommands.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.schedules.ScheduleService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Component
public class ScheduleTodayCommand implements BotCommand {
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final MessageSenderService messageSenderService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "Сегодня".equalsIgnoreCase(message.getText().trim());
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
        return userService.getUserForTelegramTag(message.getFrom().getUserName())
                .thenCompose(user -> scheduleService.getForToday(user.getGroupName()))
                .thenCompose(schedule -> messageSenderService.sendButtonMessage(
                        SendMessage.builder()
                                .chatId(chatId)
                                .text(schedule)
                                .replyMarkup(KeyboardUtils.createMarkupWithTomorrow(LocalDate.now()))
                                .build()
                ))
                .exceptionally(e -> {
                    log.error("Ошибка при получении расписания на сегодня: {}", e.getMessage());
                    messageSenderService.sendTextMessage(chatId, "Ошибка получения расписания.");
                    return null;
                })
                .thenApply(messageId -> {
                    log.info("Команда 'Сегодня' выполнена для пользователя {}", message.getFrom().getUserName());
                    return null;
                });
    }
}
