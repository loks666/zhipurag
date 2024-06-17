package com.ruyun.zhipurag.entiy;

import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
public class LogInfo {
    String id;
    Long created;
    int completionTokens;
    int promptTokens;
    int totalTokens;

    public String getFormattedCreated() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochSecond(created));
    }
}
