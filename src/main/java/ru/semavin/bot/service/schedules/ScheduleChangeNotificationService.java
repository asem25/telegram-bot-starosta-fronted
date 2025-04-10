package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.ScheduleChangeDTO;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleChangeNotificationService {

    private final MessageSenderService messageSenderService;
    private final GroupService groupService;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Оповещает студентов о внесённых изменениях в расписание.
     *
     * @param groupName название группы, для которой осуществляется оповещение
     * @param dto       объект ScheduleChangeDTO с данными об изменениях
     * @return CompletableFuture, завершённое после рассылки уведомлений
     */
    public CompletableFuture<Void> notifyStudents(String groupName, ScheduleChangeDTO dto, boolean deleted) {
        return groupService.getStudentList(groupName)
                .thenAccept((List<UserDTO> students) -> {
                    String message = deleted ? buildNotificationMessageWithDeleted(dto, groupName):buildNotificationMessage(dto, groupName);
                    if (students == null || students.isEmpty()) {
                        log.warn("Для группы {} не найдено студентов – уведомление не отправлено", groupName);
                        return;
                    }
                    for (UserDTO student : students) {
                        if (student.getTelegramId() != null) {
                            messageSenderService.sendMessageWithMarkDown(student.getTelegramId(), message);
                        }
                    }
                    log.info("Уведомление отправлено студентам группы {}", groupName);
                })
                .exceptionally(ex -> {
                    log.error("Ошибка при оповещении студентов: {}", ex.getMessage());
                    return null;
                });
    }

    private String buildNotificationMessageWithDeleted(ScheduleChangeDTO dto, String groupName) {
        // Если новые значения не заданы, подставляем старые
        String formattedDate = dto.getNewLessonDate() != null
                ? dto.getNewLessonDate().format(dateFormatter)
                : dto.getOldLessonDate().format(dateFormatter);
        String formattedStartTime = dto.getNewStartTime() != null
                ? dto.getNewStartTime().format(timeFormatter)
                : dto.getOldStartTime().format(timeFormatter);
        String formattedEndTime = dto.getNewEndTime() != null
                ? dto.getNewEndTime().format(timeFormatter)
                : dto.getOldEndTime().format(timeFormatter);

        return String.format(
                "*Пары не будет!*\n\n" +
                        "Группа: %s\n" +
                        "Предмет: %s\n" +
                        "Дата: %s\n" +
                        "Время: %s – %s\n" +
                        "Аудитория: %s\n" +
                        "Комментарий: %s",
                groupName,
                dto.getSubjectName(),
                formattedDate,
                formattedStartTime,
                formattedEndTime,
                dto.getClassroom(),
                dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription() : "Без комментария"
        );
    }

    /**
     * Формирует текст уведомления об изменениях в расписании.
     *
     * @param dto       объект ScheduleChangeDTO с данными об изменениях
     * @param groupName название группы, для которой происходит оповещение
     * @return отформатированный текст уведомления
     */
    private String buildNotificationMessage(ScheduleChangeDTO dto, String groupName) {
        // Если новые значения не заданы, подставляем старые
        String formattedDate = dto.getNewLessonDate() != null
                ? dto.getNewLessonDate().format(dateFormatter)
                : dto.getOldLessonDate().format(dateFormatter);
        String formattedStartTime = dto.getNewStartTime() != null
                ? dto.getNewStartTime().format(timeFormatter)
                : dto.getOldStartTime().format(timeFormatter);
        String formattedEndTime = dto.getNewEndTime() != null
                ? dto.getNewEndTime().format(timeFormatter)
                : dto.getOldEndTime().format(timeFormatter);

        return String.format(
                "*Изменения в расписании*\n\n" +
                        "Группа: %s\n" +
                        "Предмет: %s\n" +
                        "Дата: %s\n" +
                        "Время: %s – %s\n" +
                        "Аудитория: %s\n" +
                        "Комментарий: %s",
                groupName,
                dto.getSubjectName(),
                formattedDate,
                formattedStartTime,
                formattedEndTime,
                dto.getClassroom(),
                dto.getDescription() != null && !dto.getDescription().isBlank() ? dto.getDescription() : "Без комментария"
        );
    }
}
