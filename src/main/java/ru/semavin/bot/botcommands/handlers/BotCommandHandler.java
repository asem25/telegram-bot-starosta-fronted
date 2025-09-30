package ru.semavin.bot.botcommands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.botcommands.schedule.ScheduleChangeCommand;
import ru.semavin.bot.service.users.starosta.StarostaChangeServiceState;

import java.util.List;
import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Component
public class BotCommandHandler {
    private final List<BotCommand> commands;
    private final StarostaChangeServiceState state;

    /**
     * Перебирает все команды и, если какая-либо может обработать сообщение, вызывает её execute.
     * Если ни одна команда не подошла, можно отправить сообщение об ошибке или игнорировать запрос.
     *
     * @param message входящее сообщение
     * @return CompletableFuture завершения обработки
     */
    public CompletableFuture<Void> handle(Message message) {
        for (BotCommand command : commands) {
            if (command.canHandle(message)) {
                if (!(command instanceof ScheduleChangeCommand)){
                    if (state.getState(message.getChatId())) {
                        state.clearState(message.getChatId());
                    }
                }
                return command.execute(message);
            }
        }
        // Если команда не найдена, отправляем стандартное сообщение
        // (или можно оставить пустым, если неизвестные команды обрабатываются отдельно)
        log.warn("Неизвестная команда: {}", message.getText());
        return CompletableFuture.completedFuture(null);
    }
}
