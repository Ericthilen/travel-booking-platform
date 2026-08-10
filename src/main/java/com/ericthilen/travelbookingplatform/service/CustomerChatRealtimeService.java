package com.ericthilen.travelbookingplatform.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CustomerChatRealtimeService {

    private final Map<String, List<SseEmitter>> conversationEmitters =
            new ConcurrentHashMap<>();
    private final List<SseEmitter> dashboardEmitters =
            new CopyOnWriteArrayList<>();

    public SseEmitter subscribeToConversation(String publicId) {
        SseEmitter emitter = new SseEmitter(0L);
        conversationEmitters
                .computeIfAbsent(publicId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeConversationEmitter(publicId, emitter));
        emitter.onTimeout(() -> removeConversationEmitter(publicId, emitter));
        emitter.onError(error -> removeConversationEmitter(publicId, emitter));

        send(emitter, "refresh", new RefreshEvent(publicId));

        return emitter;
    }

    public SseEmitter subscribeToDashboard() {
        SseEmitter emitter = new SseEmitter(0L);
        dashboardEmitters.add(emitter);

        emitter.onCompletion(() -> dashboardEmitters.remove(emitter));
        emitter.onTimeout(() -> dashboardEmitters.remove(emitter));
        emitter.onError(error -> dashboardEmitters.remove(emitter));

        send(emitter, "refresh", new RefreshEvent("dashboard"));

        return emitter;
    }

    public void notifyConversation(String publicId) {
        broadcastConversation(publicId, "refresh", new RefreshEvent(publicId));
    }

    public void notifyDashboard() {
        broadcastDashboard("refresh", new RefreshEvent("dashboard"));
    }

    public void typing(
            String publicId,
            String actor,
            String name,
            boolean typing,
            String preview
    ) {
        broadcastConversation(
                publicId,
                "typing",
                new TypingEvent(
                        actor,
                        name,
                        typing,
                        cleanPreview(preview)
                )
        );
    }

    private String cleanPreview(String preview) {
        if (preview == null) {
            return "";
        }

        String cleanPreview = preview.trim();

        if (cleanPreview.length() <= 500) {
            return cleanPreview;
        }

        return cleanPreview.substring(0, 500);
    }

    private void broadcastConversation(
            String publicId,
            String eventName,
            Object data
    ) {
        List<SseEmitter> emitters = conversationEmitters.get(publicId);

        if (emitters == null) {
            return;
        }

        emitters.removeIf(emitter -> !send(emitter, eventName, data));
    }

    private void broadcastDashboard(String eventName, Object data) {
        dashboardEmitters.removeIf(emitter -> !send(emitter, eventName, data));
    }

    private boolean send(
            SseEmitter emitter,
            String eventName,
            Object data
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException exception) {
            emitter.complete();
            return false;
        }
    }

    private void removeConversationEmitter(
            String publicId,
            SseEmitter emitter
    ) {
        List<SseEmitter> emitters = conversationEmitters.get(publicId);

        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    public record RefreshEvent(String target) {
    }

    public record TypingEvent(
            String actor,
            String name,
            boolean typing,
            String preview
    ) {
    }
}
