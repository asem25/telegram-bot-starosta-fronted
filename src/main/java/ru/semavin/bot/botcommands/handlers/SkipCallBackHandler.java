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
    private final UserService userService;
    private final MessageSenderService messageSenderService;
    public void handleSkipCallback(CallbackQuery callbackQuery){
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if (data.startsWith("DELETE_MISSED_")) {
            // data = "DELETE_MISSED_username_2025-04-01_2025-04-03"
            String[] parts = data.split("_");
            // 0:DELETE, 1:MISSED, 2:username, 3:fromDate, 4:toDate
            String username = parts[2];
            LocalDate from = LocalDate.parse(parts[3]);
            LocalDate to   = LocalDate.parse(parts[4]);

            // Узнаём у userService, к какой группе принадлежит username
            userService.getUserForTelegramTag(username).thenAccept(user -> {
                boolean deleted = skipNotificationService.deleteSkip(user.getGroupName(), username, from, to);

                String text = deleted
                        ? "✅ Пропуск с " + from + " по " + to + " для @" + username + " удалён."
                        : "⚠️ Не удалось найти этот пропуск.";

                messageSenderService.editMessageText(
                        KeyboardUtils.createEditMessage(chatId.toString(), messageId, text, null)
                );
            });
            return;
        }
    }
}
