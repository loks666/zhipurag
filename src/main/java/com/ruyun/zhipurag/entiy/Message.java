package com.ruyun.zhipurag.entiy;

import lombok.Data;

@Data
public class Message {
    private String user;
    private String system;

    public Message(String user, String system) {
        this.user = user;
        this.system = system;
    }

}

