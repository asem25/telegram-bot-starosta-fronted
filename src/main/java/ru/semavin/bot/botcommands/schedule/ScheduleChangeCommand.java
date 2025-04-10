package ru.semavin.bot.botcommands.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ScheduleChangeCommand implements BotCommand {
    private final MessageSenderService messageSenderService;


    @Override
    public boolean canHandle(Message message) {
        return message.getText().equalsIgnoreCase("Изменить расписание");
    }

    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        // Отправляем сообщение с календарём для выбора даты занятия
        LocalDate localDate = LocalDate.now();
        return messageSenderService.sendButtonMessage(
                KeyboardUtils.createMessageWithInlineCalendarWithChange(chatId, localDate.getYear(), localDate.getMonthValue())
        ).thenAccept(response -> {});
    }
}
