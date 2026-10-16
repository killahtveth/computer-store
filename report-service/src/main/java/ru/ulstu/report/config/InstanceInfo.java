package ru.ulstu.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstanceInfo {

    private final String instanceId;

    public InstanceInfo(
            @Value("${HOSTNAME:unknown}") String hostname,
            @Value("${server.port:8082}") int port) {
        this.instanceId = hostname + ":" + port;
    }

    public String getInstanceId() {
        return instanceId;
    }
}