package com.gitbitex.matchingengine.command;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;

@Slf4j
public class CommandSerializer implements Serializer<Command> {
    @Override
    public byte[] serialize(String topic, Command command) {
        try {
            byte[] jsonBytes = JSON.toJSONString(command).getBytes(StandardCharsets.UTF_8);
            byte[] messageBytes = new byte[jsonBytes.length + 1];
            messageBytes[0] = command.getType().getByteValue();
            System.arraycopy(jsonBytes, 0, messageBytes, 1, jsonBytes.length);
            return messageBytes;
        } catch (Exception e) {
            logger.error("Failed to serialize command: {}", command, e);
            throw new RuntimeException("serialize error", e);
        }
    }
}
