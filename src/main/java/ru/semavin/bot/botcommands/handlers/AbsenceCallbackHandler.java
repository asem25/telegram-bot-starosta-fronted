package ru.semavin.bot.botcommands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.dto.SkipNotificationDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.calendar.AbsenceCalendarContextService;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static ru.semavin.bot.util.KeyboardUtils.buildCalendarWithConfirm;

/**
 * Обработчик всех callback'ов, связанных с пропуском занятий:
 * - ABSENCE_DATE_... (выбор даты)
 * - NAV_ABSENCE_... (переключение календаря)
 * - ABSENCE_CONFIRM / ABSENCE_CANCEL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AbsenceCallbackHandler {

    private final UserService userService;
    private final MessageSenderService messageSenderService;
    private final AbsenceCalendarContextService absenceCalendarContextService;

    public void handleAbsenceCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String username = callbackQuery.getFrom().getUserName();
        Long userId = callbackQuery.getFrom().getId();

        // 1) Переход между месяцами для пропусков
        if (data.startsWith("NAV_ABSENCE_")) {
            handleAbsenceNav(data, chatId, messageId);
            return;
        }

        // 2) Выбор даты
        if (data.startsWith("ABSENCE_DATE_")) {
            handleAbsenceDate(data, chatId, messageId, userId, username);
            return;
        }

        // 3) Подтверждение
        if (data.equals("ABSENCE_CONFIRM")) {
            handleAbsenceConfirm(chatId, userId);
            return;
        }

        // 4) Отмена
        if (data.equals("ABSENCE_CANCEL")) {
            handleAbsenceCancel(chatId, userId);
            return;
        }
    }

    private void handleAbsenceNav(String data, Long chatId, Integer messageId) {
        // Пример: NAV_ABSENCE_2025_04
        String[] parts = data.split("_"); // [NAV, ABSENCE, YYYY, MM]
        int year = Integer.parseInt(parts[2]);
        int month = Integer.parseInt(parts[3]);

        InlineKeyboardMarkup calendar = CalendarUtils.buildAbsenceCalendar(year, month);
        messageSenderService.editCalendarMarkup(chatId, messageId, calendar)
                .exceptionally(e -> {
                    log.error("Ошибка при навигации absenceCalendar: {}", e.getMessage());
                    return null;
                });
    }

    private void handleAbsenceDate(String data, Long chatId, Integer messageId, Long userId, String username) {
        LocalDate selectedDate = CalendarUtils.parseDateFromCallback(data);


        userService.getUserForTelegramTag(username).thenAccept(user -> {
            // Устанавливаем username, groupName в контексте
            absenceCalendarContextService.setUserContext(userId, user.getUsername(), user.getGroupName());

            // Текущий draft
            Optional<SkipNotificationDTO> optDraft = absenceCalendarContextService.getDraft(userId);
            SkipNotificationDTO draft = optDraft.orElseGet(() -> {
                SkipNotificationDTO d = new SkipNotificationDTO();
                absenceCalendarContextService.setDraft(userId, d);
                return d;
            });

            // Заполняем from/to
            LocalDate from = draft.getFromDate();
            LocalDate to   = draft.getToDate();

            // Если from нет -> это первая дата
            if (from == null) {
                from = selectedDate;
                to   = selectedDate;
            } else {
                // Если уже есть from, возможно нет to или хотим обновить to
                // Упорядочим диапазон
                LocalDate min = from.isBefore(selectedDate) ? from : selectedDate;
                LocalDate max = from.isBefore(selectedDate) ? selectedDate : from;
                from = min;
                to   = max;
            }

            // Сохраняем
            draft.setFromDate(from);
            draft.setToDate(to);

            // Текст: «Выбран диапазон c ... по ...» (или если from == to, значит 1 день)
            String text = String.format("""
                🗓 Текущий пропуск:
                С %s по %s

                Можете выбрать другую дату в календаре или подтвердить.
                """,
                    from, to
            );

            // Делаем inline-календарь для текущего месяца (или того, где находится `to`):
            YearMonth ym = YearMonth.from(to);
            InlineKeyboardMarkup calendarWithConfirm = buildCalendarWithConfirm(ym, from, to);

            // Редактируем текущее сообщение
            messageSenderService.editMessageText(
                    KeyboardUtils.createEditMessage(
                            chatId.toString(),
                            messageId,
                            text,
                            calendarWithConfirm
                    )
            ).exceptionally(ex -> {
                log.error("Ошибка при редактировании: {}", ex.getMessage());
                return null;
            });
        });
        return;
    }

    private void handleAbsenceConfirm(Long chatId, Long userId) {
        if (absenceCalendarContextService.isReadyToConfirm(userId)) {
            messageSenderService.sendTextMessage(chatId,
                    "✏️ Что именно вы пропускаете?");
        } else {
            messageSenderService.sendTextMessage(chatId,
                    "⚠️ Выбор неполный. Нажмите 'Пропущу', чтобы начать заново.");
            absenceCalendarContextService.clear(userId);
        }
    }

    private void handleAbsenceCancel(Long chatId, Long userId) {
        absenceCalendarContextService.clear(userId);
        messageSenderService.sendTextMessage(chatId,
                "❌ Выбор отменён. Нажмите 'Пропущу', чтобы начать заново.");
    }
}