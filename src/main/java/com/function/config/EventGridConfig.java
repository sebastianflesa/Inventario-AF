package com.function.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventGridConfig {
    
    @Value("${eventgrid.topic.endpoint:#{null}}")
    private String topicEndpoint;
    
    @Value("${eventgrid.topic.key:#{null}}")
    private String topicKey;
    
    public String getTopicEndpoint() {
        // Fall back to environment variable if Spring property is not set
        if (topicEndpoint == null || topicEndpoint.isEmpty()) {
            return System.getenv("EVENTGRID_TOPIC_ENDPOINT");
        }
        return topicEndpoint;
    }
    
    public String getTopicKey() {
        // Fall back to environment variable if Spring property is not set
        if (topicKey == null || topicKey.isEmpty()) {
            return System.getenv("EVENTGRID_KEY");
        }
        return topicKey;
    }
}