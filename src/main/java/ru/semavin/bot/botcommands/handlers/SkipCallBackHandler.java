package ru.semavin.bot.botcommands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.notification.SkipNotificationService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class SkipCallBackHandler {
    private final SkipNotificationService skipNotificationService;
    private final MessageSenderService messageSenderService;
    public void handleSkipCallback(CallbackQuery callbackQuery){
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (data.startsWith("DELETE_MISSED_")) {
            // data = "DELETE_MISSED_uuid"
            String[] parts = data.split("_");
            // 0:DELETE, 1:MISSED, 2:uuid
            String uuid = parts[2];

            // Узнаём у userService, к какой группе принадлежит username
            skipNotificationService.deleteSkip(uuid).thenAccept(skip -> {
                log.info("Удаление пропуска ");
                messageSenderService.editMessageText(
                        KeyboardUtils.createEditMessage(chatId.toString(), messageId, "✅ Пропуск  удалён.", null)
                );
            })
                    .exceptionally(e -> {
                            log.warn("⚠️ Не удалось найти этот пропуск.");
                            return null;
                    });
        }
    }
}
