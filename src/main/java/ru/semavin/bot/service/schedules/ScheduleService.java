package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.semavin.bot.dto.ScheduleDTO;
import ru.semavin.bot.service.MessageSenderService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleApiService scheduleApiService;
    private final MessageSenderService messageSenderService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public Mono<String> getForToday(String groupName) {
        String formattedDate = LocalDate.now().format(formatter);
        return scheduleApiService.getForSomeDate(formattedDate, groupName)
                .map(schedule -> buildScheduleTextForDay(schedule, formattedDate))
                .onErrorResume(e -> {
                    log.error("Ошибка при получении текста расписания на дату {}: {}", formattedDate, e.getMessage());
                    return Mono.just("⚠ Ошибка при получении расписания на дату " + formattedDate);
                });
    }

    public Mono<String> getForCurrentWeek(String groupName) {
        String formattedDate = LocalDate.now().format(formatter);
        return scheduleApiService.getForCurrentWeek(groupName)
                .map(schedule -> buildScheduleTextForWeek(schedule, formattedDate, formattedDate))
                .onErrorResume(e -> {
                    log.error("Ошибка при получении текста расписания на дату {}: {}", formattedDate, e.getMessage());
                    return Mono.just("⚠ Ошибка при получении расписания на дату " + formattedDate);
                });
    }

    public Mono<String> getForSomeWeek(String groupName, int week) {
        String formattedDate = LocalDate.now().format(formatter);
        return scheduleApiService.getForSomeWeek(groupName, week)
                .map(schedule -> buildScheduleTextForWeek(schedule, formattedDate, formattedDate))
                .onErrorResume(e -> {
                    log.error("Ошибка при получении текста расписания на дату {}: {}", formattedDate, e.getMessage());
                    return Mono.just("⚠ Ошибка при получении расписания на дату " + formattedDate);
                });
    }

    //TODO Вызывается из API(не должно быть так)
    public Mono<String> getScheduleSomeDate(String groupName, LocalDate date) {
        String formattedDate = date.format(formatter);
        return scheduleApiService.getForSomeDate(formattedDate, groupName)
                .map(schedule -> buildScheduleTextForDay(schedule, formattedDate))
                .onErrorResume(e -> {
                    log.error("Ошибка при получении текста расписания на дату {}: {}", formattedDate, e.getMessage());
                    return Mono.just("⚠ Ошибка при получении расписания на дату " + formattedDate);
                });
    }

    private String buildScheduleTextForDay(List<ScheduleDTO> schedule, String formattedDate) {
        if (schedule == null || schedule.isEmpty()) {
            return "📅 На " + formattedDate + " занятий нет";
        }
        StringBuilder sb = new StringBuilder("📅 Расписание на " + formattedDate + ":\n\n");
        for (ScheduleDTO dto : schedule) {
            sb.append(String.format("📘 %s (%s)\n%s – %s | Ауд: %s\n👨‍🏫 %s\n\n",
                    dto.getSubjectName(),
                    dto.getLessonType(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getClassroom(),
                    dto.getTeacherName()));
        }
        return sb.toString();
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