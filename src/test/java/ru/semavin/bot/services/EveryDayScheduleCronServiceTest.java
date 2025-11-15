package ru.semavin.bot.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.semavin.bot.dto.UserDTO;
import ru.semavin.bot.service.MessageSenderService;
import ru.semavin.bot.service.groups.GroupService;
import ru.semavin.bot.service.schedules.EveryDayScheduleCronService;
import ru.semavin.bot.service.schedules.ScheduleService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EveryDayScheduleCronServiceTest {

    @Mock
    private ScheduleService scheduleService;
    @Mock
    private GroupService groupService;
    @Mock
    private MessageSenderService messageSenderService;

    @InjectMocks
    private EveryDayScheduleCronService everyDayScheduleCronService;

    private final String defaultGroup = "М3О-403С-22";

    @Test
    void dontSend_ifScheduleNotFound() {
        when(groupService.getStudentList(defaultGroup))
                .thenReturn(CompletableFuture.completedFuture(List.of(UserDTO.builder().build())));

        when(scheduleService.getForToday(defaultGroup))
                .thenReturn(CompletableFuture.completedFuture(" На 15.11.2025 занятий нет"));

        everyDayScheduleCronService.sendScheduleForToday();

        verify(messageSenderService, times(0)).sendButtonMessage(any());

    }
}
