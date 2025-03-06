package com.gitbitex.feed;

import com.alibaba.fastjson.JSON;
import com.gitbitex.feed.message.Request;
import com.gitbitex.feed.message.SubscribeRequest;
import com.gitbitex.feed.message.UnsubscribeRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class FeedTextWebSocketHandler extends TextWebSocketHandler {
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(FeedTextWebSocketHandler.class);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionManager.removeSession(session);
    }

    @Override
    @SneakyThrows
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("Received message: {}", message.getPayload());
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String type = jsonNode.get("type").asText();
            
            if ("subscribe".equals(type)) {
                handleSubscribe(session, jsonNode);
            } else if ("unsubscribe".equals(type)) {
                handleUnsubscribe(session, jsonNode);
            } else if ("ping".equals(type)) {
                sessionManager.sendPong(session);
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", message.getPayload(), e);
        }
    }

    private void handleSubscribe(WebSocketSession session, JsonNode message) {
        log.info("Processing subscription: {}", message);
        SubscribeRequest subscribeRequest = JSON.parseObject(message.toString(), SubscribeRequest.class);
        sessionManager.subOrUnSub(session, subscribeRequest.getProductIds(), subscribeRequest.getCurrencyIds(),
                subscribeRequest.getChannels(), true);
    }

    private void handleUnsubscribe(WebSocketSession session, JsonNode message) {
        log.info("Processing unsubscribe: {}", message);
        UnsubscribeRequest unsubscribeRequest = JSON.parseObject(message.toString(), UnsubscribeRequest.class);
        sessionManager.subOrUnSub(session, unsubscribeRequest.getProductIds(),
                unsubscribeRequest.getCurrencyIds(),
                unsubscribeRequest.getChannels(), false);
    }
}
