package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewYearScheduler {

    private final MessageSenderService messageSenderService;
    private final GroupService groupService;
    private final String defaultGroup = "М3О-403С-22";
    private final String NEW_YEAR_MESSAGE = """
            Доброе утро!☀️ Хотя подозреваю, что это сообщение вы прочитаете явно не утром…\s
            Поздравляю с наступившим 2026 годом!
            Желаю, чтоб в этом году учиться было еще легче, преподы были добрее, а пары почаще отменялись — тогда у вас будет побольше уведомлений от меня😉
            
            Ну что, начнем отсчет еще одного года до выпуска?
            """;

//    @Scheduled(cron = "0 0 7 01 01 *", zone = "Europe/Moscow")
    @Scheduled(fixedDelay = 1000, zone = "Europe/Moscow")
    public void updateDailySchedules() {
        var stlistFutures = groupService.getStudentList(defaultGroup)
                .exceptionally(ex -> {
                    log.error("Ошибка при получении студентов группы {}", ex.getMessage(), ex);
                    return null;
                }).join();

        stlistFutures.forEach(student -> messageSenderService
                .sendTextMessage(student.getTelegramId(), NEW_YEAR_MESSAGE));
    }
}
