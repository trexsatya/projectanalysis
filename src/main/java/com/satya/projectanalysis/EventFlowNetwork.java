package com.satya.projectanalysis;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

@Slf4j
public class EventFlowNetwork {
    @Builder
    @Value
    static class Consumer {
        MethodProcessor.MethodDetail methodDetail;
    }

    @Builder
    @Value
    static class Producer {
        MethodInvocationProcessor.InvocationDetail invocationDetail;
    }

    @Builder(toBuilder = true)
    @Getter
    @Setter
    static class Event {
        String name;
        @Builder.Default
        Set<Producer> producers = new HashSet<>();
        @Builder.Default
        Set<Consumer> consumers  = new HashSet<>();
    }

    private static final Map<String, Event> events = new HashMap<>();

    public static void add(String eventName, MethodInvocationProcessor.InvocationDetail invocationDetail) {
        if(invocationDetail == null) {
            log.warn("eventName invocationDetail -> null");
            return;
        }
        events.computeIfAbsent(eventName, k -> Event.builder().build()).producers
                .add(Producer.builder().invocationDetail(invocationDetail
                ).build());
    }

    public static void add(String eventName, MethodProcessor.MethodDetail methodDetail) {
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
