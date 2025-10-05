package com.function;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventGridPublisher {
    private static final ObjectMapper M = new ObjectMapper();

    // topicEndpoint: https://<tu-topic>.<region>-1.eventgrid.azure.net/api/events
    // key: key del topic (get from portal)
    public static void publish(String topicEndpoint, String key, String eventType, Map<String,Object> data) {
        try {
            List<Map<String,Object>> events = new ArrayList<>();
            Map<String,Object> ev = new HashMap<>();
            ev.put("id", UUID.randomUUID().toString());
            ev.put("eventType", eventType);
            ev.put("subject", "/inventario/" + eventType);
            ev.put("eventTime", java.time.OffsetDateTime.now().toString());
            ev.put("data", data);
            ev.put("dataVersion", "1.0");
            events.add(ev);

            byte[] payload = M.writeValueAsBytes(events);
            URL url = new URL(topicEndpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            // key header
            con.setRequestProperty("aeg-sas-key", key);
            con.setDoOutput(true);
            try(OutputStream os = con.getOutputStream()) {
                os.write(payload);
            }
            int code = con.getResponseCode();
            // optionally read response for logging
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}