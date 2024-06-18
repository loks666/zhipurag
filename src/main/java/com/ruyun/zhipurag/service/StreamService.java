package com.ruyun.zhipurag.service;

import com.ruyun.zhipurag.entiy.LogInfo;
import com.ruyun.zhipurag.entiy.Message;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
// 其他导入省略

@Slf4j
@Service
public class StreamService {

    private static final LogInfo logInfo = new LogInfo();
    private final String knowledgeId;
    private final String promptTemplate;
    private final ClientV4 client;

    public StreamService(@Value("${zhipu.key}") String apiKey,
                         @Value("${zhipu.knowledge_id}") String knowledgeId,
                         @Value("${zhipu.prompt}") String promptTemplate) {
        this.knowledgeId = knowledgeId;
        this.promptTemplate = promptTemplate;
        this.client = new ClientV4.Builder(apiKey).build();
    }

    public SseEmitter streamChat(String userMessage, String requestId) {
        final SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 长时间运行的连接
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 确保 requestId 是 final，这样可以在 lambda 表达式中使用
        final String finalRequestId;
        if (requestId == null) {
            finalRequestId = String.format("ruyun-%d", System.currentTimeMillis());
        } else {
            finalRequestId = requestId;
        }

        executor.execute(() -> {
            try {
                List<ChatMessage> messages = List.of(new ChatMessage(ChatMessageRole.USER.value(), userMessage));
                ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                        .model(Constants.ModelChatGLM4)
                        .stream(Boolean.TRUE)
                        .messages(messages)
                        .requestId(finalRequestId)
                        .tools(getChatTools())
                        .toolChoice("auto")
                        .build();

                ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);

                if (sseModelApiResp.isSuccess()) {
                    sseModelApiResp.getFlowable().map(chunk -> {
                        try {
                            emitter.send(chunk.getChoices().get(0).getDelta().getContent());
                        } catch (IOException e) {
                            log.error("Error sending event to client", e);
                        }
                        return chunk;
                    }).blockingSubscribe();
                } else {
                    log.error("模型请求失败: {}", sseModelApiResp.getMsg());
                    emitter.send("模型请求失败: " + sseModelApiResp.getMsg());
                }
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        executor.shutdown();
        return emitter;
    }

    private List<ChatTool> getChatTools() {
        ChatTool webSearchTool = new ChatTool();
        webSearchTool.setType(ChatToolType.WEB_SEARCH.value());
        WebSearch webSearch = new WebSearch();
        webSearch.setEnable(true);
        webSearchTool.setWeb_search(webSearch);

        ChatTool retrievalTool = new ChatTool();
        retrievalTool.setType(ChatToolType.RETRIEVAL.value());
        Retrieval retrieval = new Retrieval();
        retrieval.setKnowledge_id(knowledgeId);
        retrieval.setPrompt_template(promptTemplate);
        retrievalTool.setRetrieval(retrieval);

        return List.of(webSearchTool, retrievalTool);
    }

    private Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId()));
    }
}
