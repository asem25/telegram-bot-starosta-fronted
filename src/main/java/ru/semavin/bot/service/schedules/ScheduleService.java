package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.semavin.bot.dto.ScheduleDTO;
import ru.semavin.bot.service.MessageSenderService;

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

    public Mono<Void> getForToday(Long chatId, String groupName) {
        return scheduleApiService.getForToday(groupName)
                .flatMap(schedule -> sendSchedule(chatId, schedule, "на сегодня"))
                .doOnError(e -> {
                    log.error("Ошибка при получении расписания на сегодня: {}", e.getMessage());
                    messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на сегодня");
                })
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> getForCurrentWeek(Long chatId, String groupName) {
        return scheduleApiService.getForCurrentWeek(groupName)
                .flatMap(schedule -> sendSchedule(chatId, schedule, "на эту неделю"))
                .doOnError(e -> {
                    log.error("Ошибка при получении расписания на эту неделю: {}", e.getMessage());
                    messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на эту неделю");
                })
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> getForSomeWeek(Long chatId, String groupName, int week) {
        return scheduleApiService.getForSomeWeek(groupName, week)
                .flatMap(schedule -> sendSchedule(chatId, schedule, "на неделю №" + week))
                .doOnError(e -> {
                    log.error("Ошибка при получении расписания на неделю №{}: {}", week, e.getMessage());
                    messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на неделю №" + week);
                })
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> getForSomeDate(Long chatId, String groupName, LocalDate date) {
        String formattedDate = date.format(formatter);
        return scheduleApiService.getForSomeDate(formattedDate, groupName)
                .flatMap(schedule -> sendSchedule(chatId, schedule, "на дату " + formattedDate))
                .doOnError(e -> {
                    log.error("Ошибка при получении расписания на дату {}: {}", formattedDate, e.getMessage());
                    messageSenderService.sendTextMessage(chatId, "Ошибка при получении расписания на дату " + formattedDate);
                })
                .onErrorResume(e -> Mono.empty());
    }

    private Mono<Void> sendSchedule(Long chatId, List<ScheduleDTO> schedule, String label) {
        if (schedule == null || schedule.isEmpty()) {
            return messageSenderService.sendMessage(chatId, "На " + label + " занятий нет").then();
        }
        StringBuilder sb = new StringBuilder("📅 Расписание " + label + ":\n\n");
        for (ScheduleDTO dto : schedule) {
            sb.append(String.format("📘 %s (%s)\n%s – %s | Ауд: %s\n👨‍🏫 %s\n\n",
                    dto.getSubjectName(),
                    dto.getLessonType(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getClassroom(),
                    dto.getTeacherName()));
        }
        return messageSenderService.sendMessage(chatId, sb.toString()).then();
    }
}