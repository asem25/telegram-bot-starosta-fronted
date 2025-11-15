package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.ScheduleChangeForEveryDayCheckDTO;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EveryDayScheduleCheckChangesCronService {

    private final ScheduleService scheduleService;
    private final String defaultGroup = "М3О-403С-22";
    private final MessageSenderService messageSenderService;
    private final GroupService groupService;

    @Scheduled(cron = "0 0 21 * * MON-SAT", zone = "Europe/Moscow")
    public void checkChanges() {
        ZoneId ZONE = ZoneId.of("Europe/Moscow");

        LocalDate tomorrow = LocalDate.now(ZONE).plusDays(1);

        var schdListFutures = scheduleService.getChangeForDay(tomorrow, defaultGroup)
                .exceptionally(ex -> {
                    messageSenderService.sendTextErrorMessage(ex);
                    log.error("Ошибка при получении изменений расписания {}", ex.getMessage(), ex);
                    return null;
                });

        var stdListFutures = groupService.getStudentList(defaultGroup)
                .exceptionally(ex -> {
                    messageSenderService.sendTextErrorMessage(ex);
                    log.error("Ошибка при получении студентов группы {}", ex.getMessage(), ex);
                    return null;
                });

        var schdList = schdListFutures.join();
        var stdList = stdListFutures.join();

        if (schdList.getScheduleChangeEntityList().isEmpty()) {
            log.info("Не найдено изменений расписания на {}", tomorrow);
            return;
        }

        log.info("Найдены изменения в расписание на {}", tomorrow);

        processChangesSchedule(stdList, schdList.getScheduleChangeEntityList(),
                tomorrow);
    }

    private void processChangesSchedule(List<UserDTO> stdList,
                                        List<ScheduleChangeForEveryDayCheckDTO.ScheduleChangeForFrontDTO>
                                                scheduleChangeEntityList,
                                        LocalDate tomorrow) {
        if (stdList == null || stdList.isEmpty()) {
            log.warn("Для группы {} не найдено студентов – уведомление не отправлено", defaultGroup);
            return;
        }

        stdList.forEach(student -> messageSenderService
                .sendTextMessage(student.getTelegramId(),
                        buildChangeForTomorrow(scheduleChangeEntityList,
                                tomorrow))
        );
    }

    private String buildChangeForTomorrow(
            List<ScheduleChangeForEveryDayCheckDTO.ScheduleChangeForFrontDTO> changes,
            LocalDate date
    ) {
        String formattedDate = DateTimeFormatter.ofPattern("dd.MM.yyyy").format(date);

        StringBuilder sb = new StringBuilder("⚠️ Изменения на ").append(formattedDate).append(":\n\n");

        for (var c : changes) {
            String subject = nz(c.getSubjectName(), "Предмет не указан");
            String teacher = nz(c.getTeacherName(), "Преподаватель не указан");

            sb.append("📘 ")
                    .append(subject)
                    .append("(")
                    .append(getStringForLessonType(c.getLessonType()))
                    .append(")")
                    .append("\n")
                    .append("👨‍🏫 ")
                    .append(shortTeacher(teacher))
                    .append("\n");

            if (c.isDeleted()) {
                sb.append("❌ Пара отменена.\n\n");
                continue;
            }

            // Перенос на другую дату
            if (c.getNewLessonDate() != null) {
                sb.append("📅 Перенесена на дату: ").append(c.getNewLessonDate()).append("\n");
            }

            // Изменение времени
            var ns = c.getNewStartTime();
            var ne = c.getNewEndTime();
            if (ns != null && ne != null) {
                sb.append("⏰ Новое время: ").append(ns).append(" – ").append(ne).append("\n");
            } else if (ns != null) {
                sb.append("⏱ Новое время начала: ").append(ns).append("\n");
            } else if (ne != null) {
                sb.append("⏱ Новое время окончания: ").append(ne).append("\n");
            }

            sb.append("\n"); // пустая строка между пунктами
        }

        return sb.toString().trim();
    }

    private static String nz(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private static String shortTeacher(String teacher) {
        if (teacher == null) return "Преподаватель не указан";
        if (teacher.contains("Не указан")) {
            return teacher.length() > 9 ? teacher.substring(0, 9) : teacher;
        }
        return teacher;
    }

    private String getStringForLessonType(String lessonType){
        return switch (lessonType) {
            case "LECTURE" -> "ЛК";
            case "PRACTICAL" -> "ПЗ";
            case "EXAM" -> "Экзамен";
            case "LAB" -> "ЛР";
            default -> lessonType;
        };
    }

}
