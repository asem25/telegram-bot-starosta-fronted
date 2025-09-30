package ru.semavin.bot.botcommands.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.starosta.StarostaChangeServiceState;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ScheduleChangeCommand implements BotCommand {
    private final MessageSenderService messageSenderService;
    private final StarostaChangeServiceState state;


    @Override
    public boolean canHandle(Message message) {
        return message.getText().equalsIgnoreCase("Изменить расписание");
    }

    @Override
    public CompletableFuture<Void> execute(Message message) {
        Long chatId = message.getChatId();
        LocalDate localDate = LocalDate.now();
        state.setState(chatId);
        return messageSenderService.sendButtonMessage(
                KeyboardUtils.createMessageWithInlineCalendarWithChange(chatId, localDate.getYear(), localDate.getMonthValue())
        ).thenAccept(response -> {});
    }
}
