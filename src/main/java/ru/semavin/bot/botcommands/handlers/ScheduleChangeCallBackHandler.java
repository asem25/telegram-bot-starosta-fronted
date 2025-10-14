package ru.semavin.bot.botcommands.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.semavin.bot.dto.ScheduleChangeDTO;
import ru.semavin.bot.dto.ScheduleDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.schedules.ScheduleApiService;
import ru.semavin.bot.service.schedules.ScheduleChangeEditingContextService;
import ru.semavin.bot.service.schedules.ScheduleChangeNotificationService;
import ru.semavin.bot.service.schedules.ScheduleService;
import ru.semavin.bot.service.users.UserService;
import ru.semavin.bot.util.KeyboardUtils;
import ru.semavin.bot.util.calendar.CalendarUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleChangeCallBackHandler {

    private final MessageSenderService messageSenderService;
    private final ScheduleChangeEditingContextService scheduleChangeEditingContextService;
    private final ScheduleApiService scheduleApiService;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final ScheduleChangeNotificationService scheduleChangeNotificationService;
    /**
     * Обрабатывает callback-события для редактирования и удаления изменений в расписании.
     *
     * Ожидаемые callback data:
     * - "SCHEDULE_CHANGE_EDIT": инициировать ввод новых данных для изменения пары.
     * - "SCHEDULE_CHANGE_DELETE": отправить запрос на удаление пары.
     * - "UPDATE_SCHEDULE_CHANGE_FIELD_{field}": указание на изменение конкретного поля (например, description).
     * - "SCHEDULE_CHANGE_CONFIRM": подтверждение и отправка обновлённых данных (PUT-запрос в API).
     */
    public void handleScheduleChangeCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String telegramUser = callbackQuery.getFrom().getUserName();
        Long userId = callbackQuery.getFrom().getId();
        // Обработка выбора даты (например, "CALENDAR_DATE_2025-03-15")
        if (data.startsWith("CALENDAR_CHANGE_")) {
            LocalDate selectedDate = CalendarUtils.parseDateFromCallback(data);
            userService.getUserForTelegramTag(telegramUser).thenAccept(user -> {
                String groupName = user.getGroupName();
                scheduleService.getScheduleSomeDateWithOutText(groupName, selectedDate)
                        .thenAccept(lessons -> {
                            // Преобразуем JSON-строку в List<ScheduleDTO>

                            InlineKeyboardMarkup lessonsMarkup = KeyboardUtils.createLessonsMarkup(lessons);
                            String text = "Выберите занятие для внесения изменений:";
                            messageSenderService.editMessageText(
                                    KeyboardUtils.createEditMessage(chatId.toString(), messageId, text, lessonsMarkup)
                            );
                        })
                        .exceptionally(ex -> {
                            log.error("Ошибка при получении расписания {}", ex.getMessage());
                            messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на выбранную дату.");
                            return null;
                        });
            });
            return;
        }

        if (data.startsWith("LESSON_SELECT_")) {
            String payload = data.replace("LESSON_SELECT_", "");
            String[] parts = payload.split("\\|");
            String groupName = parts[0];
            LocalDate lessonDate = LocalDate.parse(parts[1]);
            String startTime = parts[2];

            // Получаем данные занятия асинхронно
            scheduleService.findLesson(groupName, lessonDate, startTime)
                    .thenAccept(lesson -> {
                        // Формируем ScheduleChangeDTO на основе полученного ScheduleDTO
                        ScheduleChangeDTO dto = ScheduleChangeDTO.builder()
                                .subjectName(lesson.getSubjectName())
                                .lessonType(lesson.getLessonType())
                                .teacherName(lesson.getTeacherName())
                                .classroom(lesson.getClassroom())
                                .oldLessonDate(lesson.getLessonDate())
                                .oldStartTime(lesson.getStartTime())
                                .oldEndTime(lesson.getEndTime())
                                .build();
                        // Сохраняем объект в контексте редактирования для пользователя
                        scheduleChangeEditingContextService.initScheduleChange(userId, dto);

                        // Формируем текст уведомления на основе старых значений
                        String text = String.format("Изменения для занятия:\n%s, %s–%s\nПредмет: %s\nАудитория: %s",
                                dto.getOldLessonDate(), dto.getOldStartTime(), dto.getOldEndTime(),
                                dto.getSubjectName(), dto.getClassroom());

                        // Получаем клавиатуру для редактирования изменений
                        InlineKeyboardMarkup editMarkup = KeyboardUtils.createScheduleChangeEditMarkup();

                        // Отправляем сообщение с формой редактирования
                        messageSenderService.editMessageText(
                                KeyboardUtils.createEditMessage(chatId.toString(), messageId, text, editMarkup)
                        );
                    })
                    .exceptionally(ex -> {
                        messageSenderService.sendTextMessage(chatId, "Ошибка при получении данных занятия.");
                        return null;
                    });
            return;
        }
        if ("SCHEDULE_CHANGE_EDIT".equals(data)) {
            // Запрос новых данных для редактирования пары
            messageSenderService.sendTextMessage(chatId, "Введите новые данные для изменения пары в формате:\n" +
                    "newLessonDate (yyyy-MM-dd), newStartTime (HH:mm), newEndTime (HH:mm), аудитория, комментарий");
            // Установить состояние ожидания ввода для данного пользователя можно здесь
            return;
        }

        if ("SCHEDULE_CHANGE_DELETE".equals(data)) {
            scheduleChangeEditingContextService.getScheduleChange(userId).ifPresentOrElse(dto -> {
                userService.getUserForTelegramTag(telegramUser).thenAccept(user -> {
                    scheduleApiService.deleteScheduleChange(user.getGroupName(), dto)
                            .thenAccept(response -> {
                                scheduleChangeNotificationService.notifyStudents(user.getGroupName(), dto, true);
                                scheduleChangeEditingContextService.clearScheduleChange(userId);
                                messageSenderService.sendTextMessage(chatId, "✅ Пара отмечена как удалённая.");
                            })
                            .exceptionally(ex -> {
                                log.error("Ошибка при удалении пары: {}", ex.getMessage());
                                messageSenderService.sendTextMessage(chatId, "Ошибка при удалении пары.");
                                return null;
                            });
                });
            }, () -> messageSenderService.sendTextMessage(chatId, "Нет данных для удаления, попробуйте снова."));
            return;
        }

        if (data.startsWith("UPDATE_SCHEDULE_CHANGE_FIELD_")) {
            String field = data.replace("UPDATE_SCHEDULE_CHANGE_FIELD_", "");
            scheduleChangeEditingContextService.setEditingField(userId, field);
            messageSenderService.sendTextMessage(chatId, "Введите новое значение для поля " + field + ":");
            return;
        }


        if ("SCHEDULE_CHANGE_CONFIRM".equals(data)) {
            scheduleChangeEditingContextService.getScheduleChange(userId).ifPresentOrElse(dto -> {

                userService.getUserForTelegramTag(telegramUser).thenAccept(user -> {
                    scheduleApiService.updateScheduleChange(user.getGroupName(), dto)
                            .thenAccept(response -> {
                                scheduleChangeEditingContextService.clearScheduleChange(userId);
                                scheduleChangeNotificationService.notifyStudents(user.getGroupName(), dto, false);
                                messageSenderService.sendTextMessage(chatId, "✅ Изменения успешно отправлены.");
                            })
                            .exceptionally(ex -> {
                                log.error("Ошибка при обновлении пары: {}", ex.getMessage());
                                messageSenderService.sendTextMessage(chatId, "Ошибка при обновлении пары.");
                                return null;
                            });
                });
            }, () -> messageSenderService.sendTextMessage(chatId, "Нет данных для обновления, попробуйте снова."));
            return;
        }
        if (data.equals("SCHEDULE_CHANGE_CANCEL_LESSON")) {
            scheduleChangeEditingContextService.getScheduleChange(userId).ifPresentOrElse(dto -> {
                // Получаем актуальное groupName из userService (как делали ранее)
                userService.getUserForTelegramTag(telegramUser).thenAccept(user -> {
                    // Здесь можно выполнить вызов API для отмены пары.
                    // Например, можно использовать метод deleteScheduleChange или создать отдельный cancelLesson.
                    scheduleApiService.deleteScheduleChange(user.getGroupName(), dto)
                            .thenAccept(response -> {
                                scheduleChangeEditingContextService.clearScheduleChange(userId);
                                scheduleChangeNotificationService.notifyStudents(user.getGroupName(), dto, true);
                                messageSenderService.sendTextMessage(chatId, "✅ Пара отменена.");
                            })
                            .exceptionally(ex -> {
                                log.error("Ошибка при отмене пары: {}", ex.getMessage());
                                messageSenderService.sendTextMessage(chatId, "Ошибка при отмене пары.");
                                return null;
                            });
                });
            }, () -> messageSenderService.sendTextMessage(chatId, "Нет данных для отмены, попробуйте снова."));
            return;
        }
        // Если неизвестная команда
        messageSenderService.sendTextMessage(chatId, "Неизвестная команда для изменения расписания: " + data);
    }
}
