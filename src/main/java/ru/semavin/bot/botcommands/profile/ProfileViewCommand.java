package ru.semavin.bot.botcommands.profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.semavin.bot.botcommands.BotCommand;
import ru.semavin.bot.service.users.profile.ProfileService;

import java.util.concurrent.CompletableFuture;
@Slf4j
@RequiredArgsConstructor
@Component
public class ProfileViewCommand implements BotCommand {
    private final ProfileService profileService;
    /**
     * Проверяет, подходит ли данное сообщение для обработки данной командой.
     *
     * @param message входящее сообщение от Telegram
     * @return true, если команда может обработать сообщение
     */
    @Override
    public boolean canHandle(Message message) {
        return "Посмотреть данные".equalsIgnoreCase(message.getText().trim());
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
        // Вызываем сервис, который отправляет данные профиля пользователю
        profileService.viewProfile(chatId, username);
        log.info("Команда 'Посмотреть данные' выполнена для пользователя {}", username);
        // Завершаем цепочку, возвращая CompletableFuture с результатом null (тип Void)
        return CompletableFuture.completedFuture(null);
    }
}
