package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.ScheduleDTO;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleApiService {
    @Value("${api.url}")
    private String urlApi;
    private final RestClient restClient;
    private final ExecutorService executorService;

    @Cacheable(value = "schedule_day", key = "'day:today:' + #groupName")
    public CompletableFuture<List<ScheduleDTO>> getForToday(String groupName) {
        return CompletableFuture.supplyAsync(() ->
                        restClient.get()
                                .uri(urlApi + "/schedule/currentDay?groupName={groupName}", groupName)
                                .retrieve()
                               .body(new ParameterizedTypeReference<List<ScheduleDTO>>() {}),
                executorService
        );
    }

    @Cacheable(value = "schedule_week", key = "'week:current:' + #groupName")
    public CompletableFuture<List<ScheduleDTO>> getForCurrentWeek(String groupName) {
        return CompletableFuture.supplyAsync(() ->
                        restClient.get()
                                .uri(urlApi + "/schedule/week?groupName={groupName}", groupName)
                                .retrieve()
                                .body(new ParameterizedTypeReference<List<ScheduleDTO>>() {}),
                executorService
        );
    }

    @Cacheable(value = "schedule_week", key = "'week:number:' + #groupName + ':' + #week")
    public CompletableFuture<List<ScheduleDTO>> getForSomeWeek(String groupName, int week) {
        return CompletableFuture.supplyAsync(() ->
                        restClient.get()
                                .uri(urlApi + "/schedule/week?groupName={groupName}&week={week}", groupName, week)
                                .retrieve()
                                .body(new ParameterizedTypeReference<List<ScheduleDTO>>() {}),
                executorService
        );
    }

    @Cacheable(value = "schedule_day", key = "'day:date:' + #groupName + ':' + #date")
    public CompletableFuture<List<ScheduleDTO>> getForSomeDate(String date, String groupName) {
        return CompletableFuture.supplyAsync(() ->
                        restClient.get()
                                .uri(urlApi + "/schedule/day?groupName={groupName}&date={date}", groupName, date)
                                .retrieve()
                                .body(new ParameterizedTypeReference<List<ScheduleDTO>>() {}),
                executorService
        );
    }
}
