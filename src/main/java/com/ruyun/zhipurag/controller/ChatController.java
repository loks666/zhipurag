package com.ruyun.zhipurag.controller;

import com.ruyun.zhipurag.entiy.Message;
import com.ruyun.zhipurag.service.ZhiPuAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
@Slf4j
public class ChatController {

    private final ZhiPuAiService zhiPuAiService;

    public ChatController(ZhiPuAiService zhiPuAiService) {
        this.zhiPuAiService = zhiPuAiService;
    }

    @GetMapping("/")
    public String index() {
        return "chat";
    }

    @GetMapping("/hello")
    @ResponseBody
    public ResponseEntity<?> chat() {
        return ResponseEntity.ok("Hello World!");
    }

    @PostMapping("/chat")
    public ResponseEntity<Message> chat(@RequestBody Message message, @RequestParam(required = false) String requestId) {
        log.info("用户输入: {}，requestId：{}", message, requestId);
        Message responseMessage = zhiPuAiService.chat(message.getUser(), requestId);
        return ResponseEntity.ok(responseMessage);
    }
}
