package com.function.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventGridConfig {
    
    @Value("${eventgrid.topic.endpoint}")
    private String topicEndpoint;
    
    @Value("${eventgrid.topic.key}")
    private String topicKey;
    
    public String getTopicEndpoint() {
        return topicEndpoint;
    }
    
    public String getTopicKey() {
        return topicKey;
    }
}