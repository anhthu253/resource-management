package com.example.springboot.service;

import com.example.springboot.dto.StatusUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Slf4j
@Service
public class StatusUpdateService {
    private final Map<Long, Sinks.Many<StatusUpdateDto>> sinks ;

    public StatusUpdateService(Map<Long, Sinks.Many<StatusUpdateDto>> sinks) {
        this.sinks = sinks;
    }

    public Flux<StatusUpdateDto> getStatusStream(long id) {
        return getOrCreateSink(id).asFlux();
    }
    public void notifyUpdate(StatusUpdateDto update) {
        Sinks.Many<StatusUpdateDto> sink = getOrCreateSink(update.id());
        if (sink != null) {
            sink.tryEmitNext(update);
        }
    }

    private Sinks.Many<StatusUpdateDto> getOrCreateSink(long id) {
        return sinks.computeIfAbsent(id,
                key -> Sinks.many().replay().limit(10));
    }
    public void complete(long id) {
        Sinks.Many<StatusUpdateDto> sink = sinks.remove(id);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
