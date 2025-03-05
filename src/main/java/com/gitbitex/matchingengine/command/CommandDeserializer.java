package com.gitbitex.matchingengine.command;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CommandDeserializer implements Deserializer<Command> {
    @Override
    public Command deserialize(String topic, byte[] bytes) {
        try {
            CommandType commandType = CommandType.valueOfByte(bytes[0]);
            return switch (commandType) {
                case PUT_PRODUCT ->
                        JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8, PutProductCommand.class);
                case DEPOSIT ->
                        JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8, DepositCommand.class);
                case PLACE_ORDER -> JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                        PlaceOrderCommand.class);
                case CANCEL_ORDER -> JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8,
                        CancelOrderCommand.class);
                default -> {
                    logger.warn("Unhandled order message type: {}", commandType);
                    yield JSON.parseObject(bytes, 1, bytes.length - 1, StandardCharsets.UTF_8, Command.class);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("deserialize error: " + new String(bytes), e);
        }
    }
}

