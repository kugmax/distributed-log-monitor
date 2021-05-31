package com.course.bff.books.db;

import com.course.bff.books.responses.BookResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.annotation.NewSpan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisClient {
    private final static Logger logger = LoggerFactory.getLogger(RedisClient.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${redis.topic}")
    private String redisTopic;

    @NewSpan("redis-convertAndSend")
    public void sendPushNotification(BookResponse bookResponse) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            redisTemplate.convertAndSend(redisTopic, gson.toJson(bookResponse));
        } catch (Exception e) {
            logger.error("Push Notification Error", e);
        }
    }
}
