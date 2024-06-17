package com.ruyun.zhipurag.entiy;

import lombok.Data;

@Data
public class Message {
    private String user;
    private String system;
    private String requestId;

    public Message(String user, String system, String requestId) {
        this.user = user;
        this.system = system;
        this.requestId = requestId;
    }
}

