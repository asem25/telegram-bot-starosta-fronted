package ru.semavin.bot.service.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.semavin.bot.dto.ScheduleChangeDTO;
import ru.semavin.bot.dto.ScheduleChangeForEveryDayCheckDTO;
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

    @Cacheable(value = "schedule_day", key = "'day:date:' + #groupName + ':' + #date")
    public CompletableFuture<List<ScheduleDTO>> getForSomeDate(String date, String groupName) {
        return CompletableFuture.supplyAsync(() ->
                        restClient.get()
                                .uri(urlApi + "/schedule/day?groupName={groupName}&date={date}", groupName, date)
                                .retrieve()
                                .body(new ParameterizedTypeReference<List<ScheduleDTO>>() {
                                }),
                executorService
        );
    }

    /**
     * Отправляет запрос на обновление расписания (PUT-запрос).
     *
     * @param groupName Имя группы.
     * @param dto       Данные изменения в расписании.
     * @return CompletableFuture со строковым ответом API.
     */
    @Caching(evict = {
            @CacheEvict(value = "schedule_day", key = "'day:date:' + #groupName + ':' + #dto.oldLessonDate.format(T(java.time.format.DateTimeFormatter).ofPattern('dd.MM.yyyy'))"),
            @CacheEvict(value = "schedule_week", key = "'week:current:' + #groupName")
    })
    public CompletableFuture<String> updateScheduleChange(String groupName, ScheduleChangeDTO dto) {
        String url = urlApi + "/schedule/change?groupName=" + groupName;
        return CompletableFuture.supplyAsync(() -> restClient.put()
                .uri(url)
                .body(dto)
                .retrieve()
                .body(String.class), executorService);
    }

    /**
     * Отправляет запрос на удаление пары (DELETE-запрос) с телом запроса.
     * <p>
     * Так как у метода restClient.delete() нет метода body,
     * используется RequestEntity с HTTP-методом DELETE и метод exchange().
     *
     * @param groupName Имя группы.
     * @param dto       Данные удаляемой пары.
     * @return CompletableFuture со строковым ответом API.
     */
    @Caching(evict = {
            @CacheEvict(value = "schedule_day", key = "'day:date:' + #groupName + ':' + #dto.oldLessonDate.format(T(java.time.format.DateTimeFormatter).ofPattern('dd.MM.yyyy'))"),
            @CacheEvict(value = "schedule_week", key = "'week:current:' + #groupName")
    })
    public CompletableFuture<String> deleteScheduleChange(String groupName, ScheduleChangeDTO dto) {
        String url = urlApi + "/schedule/change?groupName=" + groupName;
        return CompletableFuture.supplyAsync(() -> restClient.method(HttpMethod.DELETE)
                .uri(url)
                .body((dto))
                .retrieve()
                .body(String.class), executorService);
    }

    public CompletableFuture<ScheduleChangeForEveryDayCheckDTO> getScheduleChange(String date, String groupName) {
        String url = urlApi + "/schedule/change?groupName=" + groupName + "&date=" + date;
        return CompletableFuture.supplyAsync(() -> restClient.method(HttpMethod.GET)
                .uri(url)
                .retrieve()
                .body(ScheduleChangeForEveryDayCheckDTO.class), executorService);
    }

    public CompletableFuture<ScheduleDTO> findLesson(String groupName, String date, String startTime) {
        String url = urlApi + "/schedule/lesson?groupName=" + groupName + "&date=" + date + "&startTime=" + startTime;
        return CompletableFuture.supplyAsync(() -> restClient.method(HttpMethod.GET)
                .uri(url)
                .retrieve()
                .body(ScheduleDTO.class), executorService);
    }
}
