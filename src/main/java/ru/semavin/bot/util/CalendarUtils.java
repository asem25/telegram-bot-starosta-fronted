package ru.semavin.bot.util;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class CalendarUtils {
    //TODO логи + оптимизация
    private static final LocalDate MIN_DATE = LocalDate.of(2025, 2, 10);
    private static final LocalDate MAX_DATE = LocalDate.of(2025, 6, 30);

    private static final Map<YearMonth, InlineKeyboardMarkup> calendarCache = new ConcurrentHashMap<>();
    private static final Map<Long, YearMonth> userMonthContext = new ConcurrentHashMap<>();

    static {
        preloadAllCalendars();
    }

    /**
     * Предварительно создаёт и кэширует календарь для всех месяцев в допустимом диапазоне
     */
    public static void preloadAllCalendars() {
        YearMonth start = YearMonth.from(MIN_DATE);
        YearMonth end = YearMonth.from(MAX_DATE);
        YearMonth current = start;
        while (!current.isAfter(end)) {
            calendarCache.putIfAbsent(current, generateCalendar(current.getYear(), current.getMonthValue()));
            current = current.plusMonths(1);
        }
    }

    public InlineKeyboardMarkup buildCalendarKeyboard(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return calendarCache.computeIfAbsent(ym, key -> generateCalendar(year, month));
    }

    private InlineKeyboardMarkup generateCalendar(int year, int month) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardRow navRow = new InlineKeyboardRow();
        LocalDate firstDayCurrent = LocalDate.of(year, month, 1);
        LocalDate firstDayPrev = firstDayCurrent.minusMonths(1);

        if (!firstDayPrev.isBefore(MIN_DATE.withDayOfMonth(1))) {
            navRow.add(InlineKeyboardButton.builder()
                    .text("<")
                    .callbackData("CALENDAR_NAV_" + firstDayPrev.getYear() + "_" + firstDayPrev.getMonthValue())
                    .build());
        } else {
            navRow.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
        }

        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        navRow.add(InlineKeyboardButton.builder()
                .text(monthName + " " + year)
                .callbackData("IGNORE")
                .build());

        LocalDate firstDayNext = firstDayCurrent.plusMonths(1);
        if (!firstDayNext.isAfter(MAX_DATE.withDayOfMonth(1))) {
            navRow.add(InlineKeyboardButton.builder()
                    .text(">")
                    .callbackData("CALENDAR_NAV_" + firstDayNext.getYear() + "_" + firstDayNext.getMonthValue())
                    .build());
        } else {
            navRow.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
        }

        rows.add(navRow);

        InlineKeyboardRow daysOfWeekRow = new InlineKeyboardRow();
        String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        for (String d : daysOfWeek) {
            daysOfWeekRow.add(InlineKeyboardButton.builder().text(d).callbackData("IGNORE").build());
        }
        rows.add(daysOfWeekRow);

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int dayOfWeekValue = firstDay.getDayOfWeek().getValue();

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        for (int i = 1; i < dayOfWeekValue; i++) {
            currentRow.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = LocalDate.of(year, month, day);
            if (currentDate.isBefore(MIN_DATE) || currentDate.isAfter(MAX_DATE)) {
                currentRow.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
            } else {
                currentRow.add(InlineKeyboardButton.builder()
                        .text(String.valueOf(day))
                        .callbackData("CALENDAR_DATE_" + currentDate)
                        .build());
            }
            if (currentRow.size() == 7) {
                rows.add(currentRow);
                currentRow = new InlineKeyboardRow();
            }
        }

        if (!currentRow.isEmpty()) {
            while (currentRow.size() < 7) {
                currentRow.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
            }
            rows.add(currentRow);
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public LocalDate parseDateFromCallback(String callbackData) {
        if (callbackData != null && callbackData.startsWith("CALENDAR_DATE_")) {
            String dateStr = callbackData.replace("CALENDAR_DATE_", "");
            return LocalDate.parse(dateStr);
        }
        return null;
    }

    public void rememberMonthForUser(Long userId, YearMonth month) {
        userMonthContext.put(userId, month);
    }

    public YearMonth getUserMonthOrDefault(Long userId) {
        return userMonthContext.getOrDefault(userId, YearMonth.now());
    }
}
