package ru.semavin.bot.util.calendar;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.semavin.bot.util.DateService;
import ru.semavin.bot.util.KeyboardUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@UtilityClass
public class CalendarUtils {

    private LocalDate MIN_DATE = LocalDate.of(2025, 9, 1);
    private LocalDate MAX_DATE = LocalDate.of(2026, 1, 30);

    private static final AtomicReference<InlineKeyboardMarkup> cachedMonthsMarkup = new AtomicReference<>();
    private static final Map<Integer, InlineKeyboardMarkup> weekScheduleCache = new ConcurrentHashMap<>();
    private static final Map<YearMonth, InlineKeyboardMarkup> calendarCache = new ConcurrentHashMap<>();
    private static final Map<YearMonth, InlineKeyboardMarkup> absenceCalendarCache = new ConcurrentHashMap<>();
    private static final Map<Long, YearMonth> userMonthContext = new ConcurrentHashMap<>();
    private static final Map<YearMonth, InlineKeyboardMarkup> monthWeeksCache = new ConcurrentHashMap<>();
    private static final Map<YearMonth, InlineKeyboardMarkup> changeCalendarCache = new ConcurrentHashMap<>();
    private static final Locale LOCALE = new Locale("ru");

    static {
        preloadAllCalendars();
        preloadAllWeeks();
        preLoadMothsMarkup();
    }

    public static void preloadAllCalendars() {
        YearMonth current = YearMonth.from(MIN_DATE);
        YearMonth end = YearMonth.from(MAX_DATE);
        while (!current.isAfter(end)) {
            calendarCache.putIfAbsent(current, generateCalendar(current, "CALENDAR_DATE_"));
            absenceCalendarCache.putIfAbsent(current, generateCalendar(current, "ABSENCE_DATE_"));
            changeCalendarCache.putIfAbsent(current, generateCalendar(current, "CALENDAR_CHANGE_"));
            current = current.plusMonths(1);
        }
    }

    public static void preloadAllWeeks() {
        LocalDate current = MIN_DATE.with(DayOfWeek.MONDAY);
        int weekIndex = 1;
        while (!current.isAfter(MAX_DATE)) {
            List<LocalDate> weekDates = new ArrayList<>();
            for (int j = 0; j < 6; j++) {
                LocalDate day = current.plusDays(j);
                if (!day.isAfter(MAX_DATE)) {
                    weekDates.add(day);
                }
            }
            weekScheduleCache.put(weekIndex++, KeyboardUtils.createScheduleWeekMarkup(weekDates, null));
            current = current.plusWeeks(1);
        }
    }

    public void preLoadMothsMarkup() {
        cachedMonthsMarkup.set(generateMonthsMarkup());
    }

    private static String getMonthDisplayName(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, LOCALE) + " " + month.getYear();
    }

    private static LocalDate getCorrectFirstWeekStart(YearMonth month) {
        LocalDate firstWeekStart = month.atDay(1).with(DayOfWeek.MONDAY);
        return firstWeekStart.isBefore(MIN_DATE) ? MIN_DATE : firstWeekStart;
    }

    private static InlineKeyboardMarkup generateMonthsMarkup() {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        YearMonth current = YearMonth.from(MIN_DATE);
        YearMonth end = YearMonth.from(MAX_DATE);

        while (!current.isAfter(end)) {
            rows.add(new InlineKeyboardRow(
                    InlineKeyboardButton.builder()
                            .text(getMonthDisplayName(current))
                            .callbackData("MONTH_" + current)
                            .build()
            ));
            current = current.plusMonths(1);
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup buildCalendarKeyboard(int year, int month) {
        return calendarCache.computeIfAbsent(YearMonth.of(year, month), ym -> generateCalendar(ym, "CALENDAR_DATE_"));
    }

    public InlineKeyboardMarkup buildAbsenceCalendar(int year, int month) {
        return absenceCalendarCache.computeIfAbsent(YearMonth.of(year, month), ym -> generateCalendar(ym, "ABSENCE_DATE_"));
    }

    public static InlineKeyboardMarkup buildCalendarForChange(int year, int month) {
        return changeCalendarCache.computeIfAbsent(YearMonth.of(year, month), ym -> generateCalendar(ym, "CALENDAR_CHANGE_"));
    }

    private static InlineKeyboardMarkup generateCalendar(YearMonth ym, String prefix) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        rows.add(buildNavigationRow(ym, prefix, false));

        String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
        InlineKeyboardRow headerRow = new InlineKeyboardRow();
        Arrays.stream(daysOfWeek).forEach(d -> headerRow.add(InlineKeyboardButton.builder().text(d).callbackData("IGNORE").build()));
        rows.add(headerRow);

        int daysInMonth = ym.lengthOfMonth();
        LocalDate firstDay = ym.atDay(1);
        int startDay = firstDay.getDayOfWeek().getValue();

        InlineKeyboardRow currentRow = new InlineKeyboardRow();
        for (int i = 1; i < startDay; i++) {
            currentRow.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
        }

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = ym.atDay(day);
            if (currentDate.isBefore(MIN_DATE) || currentDate.isAfter(MAX_DATE)) {
                currentRow.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
            } else {
                currentRow.add(InlineKeyboardButton.builder()
                        .text(String.valueOf(day))
                        .callbackData(prefix + currentDate)
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


    /**
     * @param ym            текущий YearMonth
     * @param prefix        'CALENDAR_DATE_' или 'ABSENCE_DATE_'
     * @param isMonthMarkup если true – значит пользователь находится в режиме "Выбор месяца" (MONTH_…)
     */
    private static InlineKeyboardRow buildNavigationRow(YearMonth ym, String prefix, boolean isMonthMarkup) {
        InlineKeyboardRow row = new InlineKeyboardRow();

        YearMonth prev = ym.minusMonths(1);
        YearMonth next = ym.plusMonths(1);

        // Если isMonthMarkup == true, значит для левой/правой кнопки используем data вида "MONTH_YYYY-MM"
        if (isMonthMarkup) {
            if (!prev.atEndOfMonth().isBefore(MIN_DATE)) {
                row.add(InlineKeyboardButton.builder()
                        .text("<")
                        .callbackData("MONTH_" + prev)
                        .build());
            } else {
                row.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
            }

            row.add(InlineKeyboardButton.builder()
                    .text(getMonthDisplayName(ym))
                    .callbackData("IGNORE")
                    .build());

            if (!next.atDay(1).isAfter(MAX_DATE)) {
                row.add(InlineKeyboardButton.builder()
                        .text(">")
                        .callbackData("MONTH_" + next)
                        .build());
            } else {
                row.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
            }

        } else {
            // Здесь используем логику для "NAV_CALENDAR_" или "NAV_ABSENCE_"
            // Смотрим prefix: "CALENDAR_DATE_" => shortPrefix = "CALENDAR"
            //                "ABSENCE_DATE_"  => shortPrefix = "ABSENCE"
            String shortPrefix = prefix.startsWith("ABSENCE") ? "ABSENCE" : "CALENDAR";

            // Левая кнопка
            if (isValidMonth(prev)) {
                String cbLeft = "NAV_" + shortPrefix + "_" + prev.getYear() + "_" + prev.getMonthValue();
                row.add(InlineKeyboardButton.builder().text("<").callbackData(cbLeft).build());
            } else {
                row.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
            }

            // Текст месяца
            row.add(InlineKeyboardButton.builder()
                    .text(getMonthDisplayName(ym))
                    .callbackData("IGNORE")
                    .build());

            // Правая кнопка
            if (isValidMonth(next)) {
                String cbRight = "NAV_" + shortPrefix + "_" + next.getYear() + "_" + next.getMonthValue();
                row.add(InlineKeyboardButton.builder().text(">").callbackData(cbRight).build());
            } else {
                row.add(InlineKeyboardButton.builder().text(" ").callbackData("IGNORE").build());
            }
        }

        return row;
    }

    private static boolean isValidMonth(YearMonth month) {
        // Если весь месяц за пределами [MIN_DATE, MAX_DATE] – не даём переход
        if (month.atEndOfMonth().isBefore(MIN_DATE)) return false;
        return !month.atDay(1).isAfter(MAX_DATE);
    }

    public static InlineKeyboardMarkup buildWeekMessage(int neededWeek) {
        return weekScheduleCache.get(neededWeek);
    }

    public static InlineKeyboardMarkup buildWeekMessageWithBack(int neededWeek) {
        InlineKeyboardMarkup fromCache = weekScheduleCache.get(neededWeek);
        List<InlineKeyboardRow> newKeyBoard = new ArrayList<>(fromCache.getKeyboard());
        newKeyBoard.add(new InlineKeyboardRow(KeyboardUtils.createBackToWeeksMarkup()));
        return new InlineKeyboardMarkup(newKeyBoard);
    }

    public static InlineKeyboardMarkup buildMonthsMarkup() {
        return cachedMonthsMarkup.get();
    }

    public static InlineKeyboardMarkup buildWeeksMarkupForMonth(YearMonth month) {
        //TODO В июне есть неделя из мая
        return monthWeeksCache.computeIfAbsent(month, m -> {
            List<InlineKeyboardRow> rows = new ArrayList<>();
            LocalDate firstWeekStart = getCorrectFirstWeekStart(m);
            LocalDate lastDayOfMonth = m.atEndOfMonth();
            int currentWeek = getRelativeWeekNumber(LocalDate.now());

            while (!firstWeekStart.isAfter(lastDayOfMonth)) {
                int weekNumber = getRelativeWeekNumber(firstWeekStart);
                LocalDate weekEnd = firstWeekStart.plusDays(5).isAfter(MAX_DATE) ? MAX_DATE : firstWeekStart.plusDays(5);

                String text = (weekNumber == currentWeek ? "⭐ " : "") +
                        "Неделя " + weekNumber + ": " +
                        firstWeekStart.format(DateTimeFormatter.ofPattern("dd.MM")) +
                        " - " +
                        weekEnd.format(DateTimeFormatter.ofPattern("dd.MM"));

                rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(text)
                        .callbackData("WEEK_" + weekNumber)
                        .build()));

                firstWeekStart = firstWeekStart.plusWeeks(1);
            }

            rows.add(buildNavigationRow(m, null, true));

            return InlineKeyboardMarkup.builder().keyboard(rows).build();
        });
    }

    public LocalDate parseDateFromCallback(String callbackData) {
        log.debug("Пришла data: {}", callbackData);
        if (callbackData != null && (callbackData.contains("_DATE_") || callbackData.contains("_CHANGE_"))) {
            String dateStr = callbackData.substring(callbackData.lastIndexOf("_") + 1);
            log.info("Полученная data, {}", dateStr);
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

    public static int getRelativeWeekNumber(LocalDate date) {
        long daysBetween = ChronoUnit.DAYS.between(MIN_DATE, date);
        int weekNumber = (int) (daysBetween / 7) + 1;
        return date.getDayOfWeek() == DayOfWeek.SUNDAY ? weekNumber + 1 : weekNumber;
    }
}
