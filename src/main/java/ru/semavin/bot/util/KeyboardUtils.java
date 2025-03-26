package ru.semavin.bot.util;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

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
        row1.add(KeyboardButton.builder()
                .text("Староста")
                .build());
        // Вторая строка с одной кнопкой
        return getMainRowsMainMenu(row1);

    }

    private static ReplyKeyboardMarkup getMainRowsMainMenu(KeyboardRow row1) {
        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder()
                .text("Дедлайны")
                .build());
        row2.add(KeyboardButton.builder()
                .text("Оповещения")
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
                .text("Заявки")
                .build());
        row1.add(KeyboardButton.builder()
                .text("Дедлайны")
                .build());

        KeyboardRow row2 = new KeyboardRow();
        row2.add(KeyboardButton.builder()
                .text("Оповещение")
                .build());
        row2.add(KeyboardButton.builder()
                .text("Настройки")
                .build());
        KeyboardRow row3 = new KeyboardRow();
        row2.add(KeyboardButton.builder()
                .text("Назад")
                .build());
        return ReplyKeyboardMarkup.builder()
                .keyboard(List.of(row1, row2, row3))
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
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
}
