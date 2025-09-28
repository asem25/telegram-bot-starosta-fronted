package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.bot.dto.ScheduleDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleApiService scheduleApiService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public CompletableFuture<String> getForToday(String groupName) {
        String formattedDate = LocalDate.now().format(formatter);
        return scheduleApiService.getForSomeDate(formattedDate, groupName)
                .thenApply(schedule -> buildScheduleTextForDay(schedule, formattedDate))
                .exceptionally(e -> {
                    log.error("Ошибка при получении текста расписания на дату {}: {}", formattedDate, e.getMessage());
                    return "⚠ Ошибка при получении расписания на дату " + formattedDate;
                });
    }

    public CompletableFuture<String> getForCurrentWeek(String groupName) {
        String formattedDate = LocalDate.now().format(formatter);
        return scheduleApiService.getForCurrentWeek(groupName)
                .thenApply(schedule -> buildScheduleTextForWeek(schedule, formattedDate, formattedDate))
                .exceptionally(e -> {
                    log.error("Ошибка при получении текста расписания на неделю {}: {}", formattedDate, e.getMessage());
                    return "⚠ Ошибка при получении расписания на неделю от " + formattedDate;
                });
    }

    public CompletableFuture<String> getForSomeWeek(String groupName, int week) {
        String formattedDate = LocalDate.now().format(formatter);
        return scheduleApiService.getForSomeWeek(groupName, week)
                .thenApply(schedule -> buildScheduleTextForWeek(schedule, formattedDate, formattedDate))
                .exceptionally(e -> {
                    log.error("Ошибка при получении текста расписания на неделю {}: {}", formattedDate, e.getMessage());
                    return "⚠ Ошибка при получении расписания на неделю от " + formattedDate;
                });
    }

    public CompletableFuture<ScheduleDTO> findLesson(String groupName, LocalDate date, String startTime){
        String formattedDate = date.format(formatter);
        return scheduleApiService.findLesson(groupName, formattedDate, startTime);
    }

    public CompletableFuture<String> getScheduleSomeDate(String groupName, LocalDate date) {
        String formattedDate = date.format(formatter);
        return scheduleApiService.getForSomeDate(formattedDate, groupName)
                .thenApply(schedule -> buildScheduleTextForDay(schedule, formattedDate))
                .exceptionally(e -> {
                    log.error("Ошибка при получении текста расписания на дату {}: {}", formattedDate, e.getMessage());
                    return "⚠ Ошибка при получении расписания на дату " + formattedDate;
                });
    }

    public CompletableFuture<List<ScheduleDTO>> getScheduleSomeDateWithOutText(String groupName, LocalDate date) {
        String formattedDate = date.format(formatter);
        return scheduleApiService.getForSomeDate(formattedDate, groupName);
    }

    private String buildScheduleTextForDay(List<ScheduleDTO> schedule, String formattedDate) {
        if (schedule == null || schedule.isEmpty()) {
            return "📅 На " + formattedDate + " занятий нет";
        }
        StringBuilder sb = new StringBuilder("📅 Расписание на " + formattedDate + ":\n\n");
        for (ScheduleDTO dto : schedule) {
            sb.append(String.format("📘 %s (%s)\n%s – %s | Ауд: %s\n👨‍🏫 %s\n%s\n",
                    dto.getSubjectName(),
                    getStringForLessonType(dto.getLessonType()),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getClassroom(),
                    (dto.getTeacherName().contains("Не указан") ? dto.getTeacherName().substring(0, 9) : dto.getTeacherName()),
                    (dto.getDescription() != null && !dto.getDescription().isEmpty()) ? String.format("❓ Комментарий: %s\n", dto.getDescription()) :
                    ""));
        }
        return sb.toString();
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
    private String buildScheduleTextForWeek(List<ScheduleDTO> schedule,
                                            String localDateStartWeek,
                                            String localDateEndWeek) {

        StringBuilder sb = new StringBuilder("📅 Расписание на неделю: " + localDateStartWeek + " - " + localDateEndWeek + " :\n\n");
        for (ScheduleDTO dto : schedule) {
            sb.append(String.format("%s \n📘 %s (%s)\n%s – %s | Ауд: %s\n👨‍🏫 %s\n\n",
                    dto.getLessonDate().getDayOfWeek(),
                    dto.getSubjectName(),
                    dto.getLessonType(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getClassroom(),
                    dto.getTeacherName()));
        }
        return sb.toString();
    }
}
