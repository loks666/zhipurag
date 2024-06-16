package com.ruyun.zhipurag.entiy;

import lombok.Data;

@Data
public class Message {
    private String user;
    private String system;
    private boolean complete;

    public Message(String user, String system, boolean complete) {
        this.user = user;
        this.system = system;
        this.complete = complete;
    }

}

