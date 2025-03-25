package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.semavin.bot.dto.ScheduleDTO;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleApiService {
    private final WebClient webClient;

    @Value("${api.url}")
    private String url;

    @Cacheable(value = "schedule_week", key = "'week:current:' + #groupName")
    public Mono<List<ScheduleDTO>> getForCurrentWeek(String groupName) {
        return webClient.get()
                .uri(url + "/schedule/week?groupName=" + groupName)
                .retrieve()
                .bodyToFlux(ScheduleDTO.class)
                .collectList();
    }

    @Cacheable(value = "schedule_day", key = "'day:today:' + #groupName")
    public Mono<List<ScheduleDTO>> getForToday(String groupName) {
        return webClient.get()
                .uri(url + "/schedule/currentDay?groupName=" + groupName)
                .retrieve()
                .bodyToFlux(ScheduleDTO.class)
                .collectList();
    }

    @Cacheable(value = "schedule_week", key = "'week:number:' + #groupName + ':' + #week")
    public Mono<List<ScheduleDTO>> getForSomeWeek(String groupName, int week) {
        return webClient.get()
                .uri(url + "/schedule/week?groupName=" + groupName+"&week="+week)
                .retrieve()
                .bodyToFlux(ScheduleDTO.class)
                .collectList();
    }

    @Cacheable(value = "schedule_day", key = "'day:date:' + #groupName + ':' + #date")
    public Mono<List<ScheduleDTO>> getForSomeDate(String date, String groupName) {
        return webClient.get()
                .uri(url + "/schedule/day?groupName=" + groupName+"&date="+date)
                .retrieve()
                .bodyToFlux(ScheduleDTO.class)
                .collectList();
    }
}