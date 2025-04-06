package ru.semavin.bot.util;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.semavin.bot.dto.SkipNotificationDTO;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Утилитный класс для создания inline‑клавиатур для Telegram‑бота.
 * Содержит методы для формирования клавиатуры с кнопкой регистрации
 * и для выбора роли пользователя.
 */
public class KeyboardUtils {


    /**
     * Создает inline‑клавиатуру с одной кнопкой «Регистрация».
     * При нажатии на кнопку будет отправлено CallbackQuery с данными "REG_START".
     *
     * @return InlineKeyboardMarkup с кнопкой регистрации.
     */
    public static InlineKeyboardMarkup registrationButton() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text("Регистрация")
                        .callbackData("REG_START")
                        .build()))
                .build();
    }

    /**
     * Создает ReplyKeyboardMarkup с одной кнопкой "Регистрация".
     * Эта клавиатура будет отображаться в области ввода сообщения.
     *
     * @return ReplyKeyboardMarkup с кнопкой "Регистрация".
     */
    public static ReplyKeyboardMarkup mainMenu() {
        // Первая строка с двумя кнопками
        KeyboardRow row1 = new KeyboardRow();
        row1.add(KeyboardButton.builder()
                .text("Профиль")
                .build());
        row1.add(KeyboardButton.builder()
                .text("Расписание")
                .build());

        // Вторая строка с одной кнопкой
        return getMainRowsMainMenu(row1);

    }
    /**
     * Создает обычную (reply) клавиатуру с кнопками «Посмотреть данные» и «Изменить данные».
     */
    public static ReplyKeyboardMarkup profileMenu() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(KeyboardButton.builder().text("Посмотреть данные").build());
        row1.add(KeyboardButton.builder().text("Изменить данные").build());

        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder().text("Назад").build());
        row2.add(KeyboardButton.builder().text("Настройки").build());
        List<KeyboardRow> keyboard = new ArrayList<>();
        keyboard.add(row1);
        keyboard.add(row2);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboard)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();
    }
    public static ReplyKeyboardMarkup starostaMainMenu(){
        // Первая строка с двумя кнопками
        KeyboardRow row1 = new KeyboardRow();
        row1.add(KeyboardButton.builder()
                .text("Профиль")
                .build());
        row1.add(KeyboardButton.builder()
                .text("Расписание")
                .build());

        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder()
                .text("Дедлайны")
                .build());
        row2.add(KeyboardButton.builder()
                .text("Староста")
                .build());


        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .resizeKeyboard(true)    // Автоматически подгоняет размер клавиатуры под экран
                .oneTimeKeyboard(false)    // Клавиатура остается видимой, пока пользователь ее не скроет
                .selective(true)           // Показывает клавиатуру только нужным пользователям в групповом чате
                .build();

    }
    public static InlineKeyboardMarkup buildCalendarWithConfirm(YearMonth ym, LocalDate from, LocalDate to) {
        // 1) Получаем готовую клавиатуру календаря
        InlineKeyboardMarkup normalCalendar = CalendarUtils.buildAbsenceCalendar(ym.getYear(), ym.getMonthValue());
        // 2) Копируем её строки
        //    В официальной бибилиотеке TelegramBots getKeyboard() возвращает List<List<InlineKeyboardButton>>
        List<InlineKeyboardRow> rows = new ArrayList<>(normalCalendar.getKeyboard());

        // 3) Создаём новую «строку» (List<InlineKeyboardButton>) для кнопок Подтвердить/Отменить
        InlineKeyboardRow confirmRow = new InlineKeyboardRow();
        confirmRow.add(
                InlineKeyboardButton.builder()
                        .text("✅ Подтвердить")
                        .callbackData("ABSENCE_CONFIRM")
                        .build()
        );
        confirmRow.add(
                InlineKeyboardButton.builder()
                        .text("❌ Отменить")
                        .callbackData("ABSENCE_CANCEL")
                        .build()
        );

        // 4) Добавляем новую строку с кнопками в общий список
        rows.add(confirmRow);

        // 5) Возвращаем обновлённый InlineKeyboardMarkup
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }
    /**
     * На каждый пропуск – одна кнопка \"Удалить\"
     * callbackData будет вида: DELETE_MISSED_username_2025-04-01_2025-04-02
     */
    public static InlineKeyboardMarkup buildMissedInlineKeyboard(List<SkipNotificationDTO> absences) {
        InlineKeyboardRow rows = new InlineKeyboardRow();

        for (SkipNotificationDTO dto : absences) {
            //TODO сделать клавишу удаления поменьше
            UUID uuid = dto.getUuid();
            // Пример: DELETE_MISSED_petya_uuid
            String callbackData = "DELETE_MISSED_" +  uuid;

            // Текст кнопки: \"Удалить пропуск (1.04 — 2.04)\", либо короче
            InlineKeyboardButton deleteButton = InlineKeyboardButton.builder()
                    .text("del (" + dto.getUsername() + " - " + absences.indexOf(dto) + ")")
                    .callbackData(callbackData)
                    .build();

            rows.add(deleteButton);
        }

        return InlineKeyboardMarkup.builder().keyboard(List.of(rows)).build();
    }
    private static ReplyKeyboardMarkup getMainRowsMainMenu(KeyboardRow row1) {
        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder()
                .text("Дедлайны")
                .build());
        row2.add(KeyboardButton.builder()
                .text("Пропущу")
                .build());

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .resizeKeyboard(true)    // Автоматически подгоняет размер клавиатуры под экран
                .oneTimeKeyboard(false)    // Клавиатура остается видимой, пока пользователь ее не скроет
                .selective(true)           // Показывает клавиатуру только нужным пользователям в групповом чате
                .build();
    }
    public static ReplyKeyboardMarkup scheduleMainMenu(){
        KeyboardRow row1 = new KeyboardRow();

        row1.add(KeyboardButton.builder()
                .text("Сегодня")
                .build());
        row1.add(KeyboardButton.builder()
                .text("На неделю")
                .build());
        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder()
                .text("День по дате")
                .build());
        row2.add(KeyboardButton.builder()
                .text("Неделя по номеру")
                .build());
        KeyboardRow row3 = new KeyboardRow();
        row3.add(KeyboardButton.builder()
                .text("Назад")
                .build());
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2,row3))
                .resizeKeyboard(true)    // Автоматически подгоняет размер клавиатуры под экран
                .oneTimeKeyboard(false)    // Клавиатура остается видимой, пока пользователь ее не скроет
                .selective(true)           // Показывает клавиатуру только нужным пользователям в групповом чате
                .build();
    }

    public static ReplyKeyboardMarkup starostaMenu(){
        KeyboardRow row1 = new KeyboardRow();
        row1.add(KeyboardButton.builder()
                .text("Пропуски")
                .build());

        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder()
                .text("Добавить оповещение")
                .build());
        row2.add(KeyboardButton.builder()
                .text("Добавить дедлайн")
                .build());


        KeyboardRow row3 = new KeyboardRow();
        row3.add(KeyboardButton.builder()
                .text("Я не староста")
                .build());
        KeyboardRow row4 = new KeyboardRow();
        row4.add(KeyboardButton.builder()
                .text("Назад")
                .build());
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3, row4))
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();

    }
    public static InlineKeyboardMarkup buildEditDeleteButtons(UUID deadlineId) {

        InlineKeyboardButton delete = InlineKeyboardButton.builder()
                .text("❌ Удалить")
                .callbackData("DELETE_DEADLINE_" + deadlineId)
                .build();

        InlineKeyboardRow row = new InlineKeyboardRow(List.of( delete));

        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(row))
                .build();
    }
    public static SendMessage createMessageMainMenu(Long chaId){
        return SendMessage.builder()
                .chatId(chaId)
                .text("Переход в профиль")
                .replyMarkup(profileMenu()) // Меню профиля
                .build();
    }
    public static SendMessage createMessageStarostaMainMenu(Long chaId){
        return SendMessage.builder()
                .chatId(chaId)
                .text("Переход в профиль")
                .replyMarkup(starostaMainMenu()) // Меню профиля
                .build();
    }
    public static SendMessage createMessageBackMenu(Long chaId){
        return SendMessage.builder()
                .chatId(chaId)
                .text("Вернулись в главное меню")
                .replyMarkup(KeyboardUtils.mainMenu())
                .build();
    }

    public static SendMessage createHelloMessageAndStarostaMainMenu(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Добро пожаловать! Выберите нужный пункт меню.")
                .replyMarkup(KeyboardUtils.starostaMainMenu()) // Главное меню старосты
                .build();
    }

    public static SendMessage createHelloMessageAndMainMenu(Long chatId) {
    return SendMessage.builder()
            .chatId(chatId.toString())
            .text("Добро пожаловать! Выберите нужный пункт меню.")
            .replyMarkup(KeyboardUtils.mainMenu()) // Главное меню
            .build();
    }

    public static SendMessage createMessageAfterRegistration(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите нужный пункт меню.")
                .replyMarkup(KeyboardUtils.mainMenu()) // Главное меню
                .build();
    }
    public static SendMessage createMessageWithStarostaMenu(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите нужный пункт меню.")
                .replyMarkup(KeyboardUtils.starostaMenu()) // Главное меню
                .build();
    }

    public static SendMessage createMessageScheduleMenu(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите нужный пункт.")
                .replyMarkup(KeyboardUtils.scheduleMainMenu()) // Главное расписания меню
                .build();
    }
    public static SendMessage createMessageWithInlineCalendar(Long chatId, int year, int month){
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Выберите дату")
                .replyMarkup(CalendarUtils.buildCalendarKeyboard(year, month))
                .build();
    }

        public static EditMessageText createEditMessage(String chatId, Integer messageId, String text, InlineKeyboardMarkup markup) {
            return EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(text)
                    .replyMarkup(markup)
                    .build();
        }

    public static InlineKeyboardButton createInlineBackToCalendarButton() {
        return InlineKeyboardButton.builder()
                .text("🔙 Назад к календарю")
                .callbackData("CALENDAR_BACK")
                .build();
    }

    public static InlineKeyboardMarkup createMarkupWithBackToCalendarButton() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(new InlineKeyboardRow(createInlineBackToCalendarButton())))
                .build();
    }
    /**
     * Создает inline-кнопки для расписания недели с возможностью раскрывать день.
     * @param datesOfWeek Список дат недели
     * @param expandedDate Дата раскрытого дня (может быть null, если ни один день не раскрыт)
     * @return InlineKeyboardMarkup с inline-кнопками
     */
    public static InlineKeyboardMarkup createScheduleWeekMarkup(List<LocalDate> datesOfWeek, LocalDate expandedDate) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (LocalDate date : datesOfWeek) {
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
            String prefix = date.equals(expandedDate) ? "▲ " : "▼ ";

            String dayNameToButton = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);

            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(prefix + dayNameToButton + " - " + (date.getDayOfMonth() + "." + (date.getMonthValue() / 10 == 0? "0" + date.getMonthValue() : date.getMonthValue() )))
                    .callbackData("SHOW_DAY_" + date)
                    .build();

            rows.add(new InlineKeyboardRow(button));
        }

        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    /**
     * Создает inline-кнопку "Назад к неделе"
     * @param weekStartDate Дата начала недели
     * @return InlineKeyboardMarkup с кнопкой назад
     */
    public static InlineKeyboardMarkup createBackToWeekMarkup(LocalDate weekStartDate) {
        InlineKeyboardRow row = new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text("🔙 Назад к неделе")
                .callbackData("BACK_WEEK_" + weekStartDate)
                .build());

        return InlineKeyboardMarkup.builder().keyboard(List.of(row)).build();
    }
    /**
     * Создает inline-кнопку "Назад к неделям"
     * @return InlineKeyboardMarkup с кнопкой назад
     */
    public static InlineKeyboardButton createBackToWeeksMarkup() {
        return InlineKeyboardButton.builder()
                .text("🔙 Назад к неделям")
                .callbackData("BACK_WEEKS")
                .build();
    }
    public static InlineKeyboardMarkup createMarkupWithTomorrow(LocalDate currentDate) {
        InlineKeyboardRow row = new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text("Завтра ➡️")
                .callbackData("TOMORROW_" + currentDate.plusDays(1))
                .build());

        return InlineKeyboardMarkup.builder().keyboard(List.of(row)).build();
    }

    public static InlineKeyboardMarkup createMarkupWithBackToToday(LocalDate todayDate) {
        InlineKeyboardRow row = new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text("⬅️ Вернуться к текущему дню")
                .callbackData("BACK_TO_TODAY_" + todayDate)
                .build());

        return InlineKeyboardMarkup.builder().keyboard(List.of(row)).build();
    }

    public static ReplyKeyboard ActionsMenu() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(KeyboardButton.builder().text("Я староста").build());
        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder().text("Назад").build());

        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2))
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();
    }

    public static InlineKeyboardMarkup createYesOrNoInlineMarkup() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(List.of(
                                InlineKeyboardButton.builder().text("✅ Да").callbackData("ABSENCE_CONFIRM").build(),
                                InlineKeyboardButton.builder().text("❌ Нет").callbackData("ABSENCE_CANCEL").build()
                        ))
                ))
                .build();
    }
}
