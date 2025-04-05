package ru.semavin.bot.botcommands.missed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissedCommand implements BotCommand {

    private final MessageSenderService messageSenderService;

    @Override
    public boolean canHandle(Message message) {
        return message.getText().equalsIgnoreCase("Пропущу");
    }

    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        LocalDate now = LocalDate.now();
        log.info("Запуск выбора пропуска для пользователя {}", message.getFrom().getUserName());

        return messageSenderService.sendButtonMessage(
                SendMessage.builder()
                        .chatId(chatId.toString())
                        .text("\uD83D\uDCC5 Выберите дату начала пропуска:")
                        .replyMarkup(CalendarUtils.buildAbsenceCalendar(now.getYear(), now.getMonthValue()))
                        .build()
        ).thenAccept(res -> {});
    }
}