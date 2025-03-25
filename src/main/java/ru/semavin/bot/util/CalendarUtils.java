package ru.semavin.bot.util;

import io.netty.handler.codec.DateFormatter;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Утилитный класс для построения inline-календаря,
 * который ограничен датами 10.02.2025 – 30.06.2025.
 *
 * Важно:
 *  1) Возвращаем именно InlineKeyboardMarkup (кнопки внутри сообщения).
 *  2) Используем List<List<InlineKeyboardButton>> для rows.
 *  3) Не смешиваем с ReplyKeyboardMarkup/KeyboardButton/KeyboardRow.
 */
public class CalendarUtils {

    private static final LocalDate MIN_DATE = LocalDate.of(2025, 2, 10); // Начало семестра
    private static final LocalDate MAX_DATE = LocalDate.of(2025, 6, 30); // Конец семестра

    /**
     * Строит inline-календарь на указанный год/месяц.
     * Если месяц выходит за диапазон, кнопки навигации "<" или ">" не создаются.
     */
    public static InlineKeyboardMarkup buildCalendarKeyboard(int year, int month) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        // --------------------------
        // 1) Строка навигации: <  [Month Year]  >
        // --------------------------
        InlineKeyboardRow navRow = new InlineKeyboardRow();

        // Предыдущий месяц
        LocalDate firstDayCurrent = LocalDate.of(year, month, 1);
        LocalDate firstDayPrev = firstDayCurrent.minusMonths(1);
        if (!firstDayPrev.isBefore(MIN_DATE.withDayOfMonth(1))) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("<")
                    .callbackData("CALENDAR_NAV_" + firstDayPrev.getYear() + "_" + firstDayPrev.getMonthValue())
                    .build());
        } else {
            // пустая кнопка (или вообще не добавлять)
            navRow.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("IGNORE")
                    .build());
        }

        // Название месяца
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        navRow.add(InlineKeyboardButton.builder()
                .text(monthName + " " + year)
                .callbackData("IGNORE")
                .build());

        // Следующий месяц
        LocalDate firstDayNext = firstDayCurrent.plusMonths(1);
        if (!firstDayNext.isAfter(MAX_DATE.withDayOfMonth(1))) {
            navRow.add(InlineKeyboardButton.builder()
                    .text(">")
                    .callbackData("CALENDAR_NAV_" + firstDayNext.getYear() + "_" + firstDayNext.getMonthValue())
                    .build());
        } else {
            navRow.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("IGNORE")
                    .build());
        }

        rows.add(navRow);

        // --------------------------
        // 2) Строка с днями недели: Пн, Вт, Ср, ...
        // --------------------------
        InlineKeyboardRow daysOfWeekRow = new InlineKeyboardRow();
        String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String d : daysOfWeek) {
            daysOfWeekRow.add(InlineKeyboardButton.builder()
                    .text(d)
                    .callbackData("IGNORE")
                    .build());
        }
        rows.add(daysOfWeekRow);

        // --------------------------
        // 3) Генерация дней месяца
        // --------------------------
        YearMonth ym = YearMonth.of(year, month);
        int daysInMonth = ym.lengthOfMonth(); // сколько дней в месяце
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int dayOfWeekValue = firstDay.getDayOfWeek().getValue();
        // (1=Понедельник, 7=Воскресенье)

        // Текущая "строка" календаря
        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        // Добавляем пустые ячейки перед первым днём (если не понедельник)
        for (int i = 1; i < dayOfWeekValue; i++) {
            currentRow.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("IGNORE")
                    .build());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = LocalDate.of(year, month, day);
            if (currentDate.isBefore(MIN_DATE) || currentDate.isAfter(MAX_DATE)) {
                // Вне диапазона — пустая кнопка
                currentRow.add(InlineKeyboardButton.builder()
                        .text(" ")
                        .callbackData("IGNORE")
                        .build());
            } else {
                // Нормальная кнопка даты
                currentRow.add(InlineKeyboardButton.builder()
                        .text(String.valueOf(day))
                        .callbackData("CALENDAR_DATE_" + currentDate) // "CALENDAR_DATE_2025-02-15"
                        .build());
            }
            // Если набрали 7 кнопок (полная неделя) — перенос строки
            if (currentRow.size() == 7) {
                rows.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }
        // Добавляем остаток, если есть
        if (!currentRow.isEmpty()) {
            while (currentRow.size() < 7) {
                currentRow.add(InlineKeyboardButton.builder()
                        .text(" ")
                        .callbackData("IGNORE")
                        .build());
            }
            rows.add(currentRow);
        }

        // --------------------------
        // 4) Создаём InlineKeyboardMarkup через builder
        // --------------------------
        return InlineKeyboardMarkup.builder()
                .keyboard(rows)  // rows = List<List<InlineKeyboardButton>>
                .build();
    }

    /**
     * Парсит дату формата "CALENDAR_DATE_YYYY-MM-DD".
     * Возвращает LocalDate или null, если формат неверен.
     */
    public static LocalDate parseDateFromCallback(String callbackData) {
        if (callbackData != null && callbackData.startsWith("CALENDAR_DATE_")) {
            String dateStr = callbackData.replace("CALENDAR_DATE_", "");
            return LocalDate.parse(dateStr);
        }
        return null;
    }
    public static String parseDateForRequest(LocalDate date){
        return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
