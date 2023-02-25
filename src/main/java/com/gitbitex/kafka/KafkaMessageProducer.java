package com.gitbitex.kafka;

import java.util.Properties;
import java.util.concurrent.Future;

import com.alibaba.fastjson.JSON;

import com.gitbitex.AppProperties;
import com.gitbitex.matchingengine.command.MatchingEngineCommand;
import com.gitbitex.matchingengine.log.Log;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@Slf4j
public class KafkaMessageProducer extends KafkaProducer<String, String> {
    private final AppProperties appProperties;

    public KafkaMessageProducer(Properties kafkaProperties, AppProperties appProperties) {
        super(kafkaProperties);
        this.appProperties = appProperties;
    }




    public Future<RecordMetadata> sendToMatchingEngine(String productId, MatchingEngineCommand orderMessage, Callback callback) {
        String topic =  appProperties.getOrderBookCommandTopic();
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, productId, JSON.toJSONString(orderMessage));

        return super.send(record, (metadata, exception) -> {
            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }

    public Future<RecordMetadata> sendOrderBookLog(Log log, Callback callback) {

        ProducerRecord<String, String> record = new ProducerRecord<>(appProperties.getOrderBookLogTopic(), "all", JSON.toJSONString(log));


        return super.send(record, (metadata, exception) -> {
            if (callback != null) {
                callback.onCompletion(metadata, exception);
            }
        });
    }
}
