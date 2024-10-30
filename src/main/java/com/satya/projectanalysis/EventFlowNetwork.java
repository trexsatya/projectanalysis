package com.satya.projectanalysis;

import com.satya.projectanalysis.processors.writes.MethodDetail;
import com.satya.projectanalysis.processors.writes.MethodInvocationProcessor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class EventFlowNetwork {
    @Builder
    @Value
    static class Consumer {
        MethodDetail methodDetail;
    }

    @Builder
    @Value
    static class Producer {
        MethodInvocationProcessor.MethodInvocationDetail methodInvocationDetail;
    }

    @Builder(toBuilder = true)
    @Getter
    @Setter
    @Data
    static class Event {
        String name;
        @Builder.Default
        Set<Producer> producers = new HashSet<>();
        @Builder.Default
        Set<Consumer> consumers  = new HashSet<>();
    }

    private static final Map<String, Event> events = new HashMap<>();

    public static void add(String eventName, MethodInvocationProcessor.MethodInvocationDetail methodInvocationDetail) {
        if(methodInvocationDetail == null) {
            log.warn("eventName invocationDetail -> null");
            return;
        }
        events.computeIfAbsent(eventName, k -> Event.builder().build()).producers
                .add(Producer.builder().methodInvocationDetail(methodInvocationDetail).build());
    }

    public static void add(String eventName, MethodDetail methodDetail) {
        if(methodDetail == null) {
            log.warn("eventName methodDetail -> null");
            return;
        }
        events.computeIfAbsent(eventName, k -> Event.builder().build()).consumers
                .add(Consumer.builder().methodDetail(methodDetail).build());
    }

    public static Map<String, Event> getEvents() {
        return events;
    }
}
