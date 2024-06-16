package com.ruyun.zhipurag.controller;

import com.ruyun.zhipurag.entiy.Message;
import com.ruyun.zhipurag.service.ZhiPuAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/zhipu")
public class ChatController {

    private final ZhiPuAiService zhiPuAiService;

    public ChatController(ZhiPuAiService zhiPuAiService) {
        this.zhiPuAiService = zhiPuAiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Message> chat(@RequestBody Message message) {
        Message responseMessage = zhiPuAiService.chat(message.getUser());
        return ResponseEntity.ok(responseMessage);
    }

    @GetMapping("/hello")
    public String chat() {
        return "Hello World!";
    }
}
