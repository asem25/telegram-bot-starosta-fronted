package ru.semavin.bot.botcommands.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Component
public class ScheduleDayOnDateCommand implements BotCommand {
    private final MessageSenderService messageSenderService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "День по дате".equalsIgnoreCase(message.getText().trim());
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
        LocalDate now = LocalDate.now();
        return messageSenderService.sendButtonMessage(
                KeyboardUtils.createMessageWithInlineCalendar(chatId, now.getYear(), now.getMonthValue())
        ).thenAccept(msg ->{
            log.info("Команда 'На неделю' выполнена для пользователя {}", message.getFrom().getUserName());
        });
    }
}
