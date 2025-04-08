package ru.semavin.bot.botcommands.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.YearMonth;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ScheduleChangeCommand implements BotCommand {
    private final MessageSenderService messageSenderService;


    @Override
    public boolean canHandle(Message message) {
        return message.getText().equalsIgnoreCase("Изменения расписания");
    }

    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        // Отправляем сообщение с календарём для выбора даты занятия
        YearMonth current = YearMonth.now();
        int year = current.getYear();
        InlineKeyboardMarkup calendarMarkup = CalendarUtils.buildCalendarForChange(current.getMonthValue(), year);
        String text = "🗓 Выберите дату занятия для внесения изменений:";
        return messageSenderService.sendButtonMessage(
                SendMessage.builder()
                        .chatId(chatId.toString())
                        .text(text)
                        .replyMarkup(calendarMarkup)
                        .build()
        ).thenAccept(response -> {});
    }
}
