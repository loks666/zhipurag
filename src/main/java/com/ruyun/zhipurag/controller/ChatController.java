package com.ruyun.zhipurag.controller;

import com.ruyun.zhipurag.entiy.Message;
import com.ruyun.zhipurag.service.StreamService;
import com.ruyun.zhipurag.service.ZhiPuAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/")
@Slf4j
public class ChatController {

    private final ZhiPuAiService zhiPuAiService;
    private final StreamService streamService;

    public ChatController(ZhiPuAiService zhiPuAiService, StreamService streamService) {
        this.zhiPuAiService = zhiPuAiService;
        this.streamService = streamService;
    }

    @GetMapping("/")
    public String index() {
        return "chat";
    }

    @GetMapping("/hello")
    public ResponseEntity<?> chat() {
        return ResponseEntity.ok("Hello World!");
    }

    @PostMapping("/chat")
    public ResponseEntity<Message> chat(@RequestBody Message message, @RequestParam(required = false) String requestId) {
        log.info("chat@用户输入: {}，requestId：{}", message, requestId);
        Message responseMessage = zhiPuAiService.chat(message.getUser(), requestId);
        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody Message message, @RequestParam(required = false) String requestId) {
        log.info("stream@用户输入: {}，requestId：{}", message, requestId);
        return streamService.streamChat(message.getUser(), requestId);
    }
}
