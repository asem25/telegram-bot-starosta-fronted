package ru.semavin.bot.enums;

public enum EditStep {
    NONE,            // Когда редактирование еще не начато
    ENTER_FIRSTNAME, // Шаг ввода имени
    ENTER_LASTNAME,  // Шаг ввода фамилии
    ENTER_GROUP,     // Шаг ввода группы
    FINISHED
}
