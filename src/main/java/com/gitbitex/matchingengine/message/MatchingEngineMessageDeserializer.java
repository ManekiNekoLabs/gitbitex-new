package com.gitbitex.matchingengine.message;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;

import java.nio.charset.StandardCharsets;

@Slf4j
public class MatchingEngineMessageDeserializer implements Deserializer<Message> {
    @Override
    public Message deserialize(String topic, byte[] bytes) {
        try {
            MessageType messageType = MessageType.valueOfByte(bytes[0]);
            switch (messageType) {
                case COMMAND_START:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                            CommandStartMessage.class);
                case COMMAND_END:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                            CommandEndMessage.class);
                case ACCOUNT:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                            AccountMessage.class);
                case PRODUCT:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                            ProductMessage.class);
                case ORDER:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                            OrderMessage.class);
                case TRADE:
                    return JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                            TradeMessage.class);
                default:
                    logger.warn("Unhandled message type: {}", messageType);
                    return JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                            Message.class);
            }
        } catch (Exception e) {
            String message = new String(bytes, StandardCharsets.UTF_8);
            logger.error("Failed to deserialize message: {}", message, e);
            throw new RuntimeException("deserialize error: " + message, e);
        }
    }
}

