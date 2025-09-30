package ru.semavin.bot.service.users.starosta;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StarostaChangeServiceState {
    private final Map<Long, Boolean> state = new ConcurrentHashMap<>();

    public void setState(Long id) {
        state.put(id, true);
    }

    public boolean getState(Long id) {
        return state.getOrDefault(id, false);
    }

    public void clearState(Long id) {
        state.remove(id);
    }
}
