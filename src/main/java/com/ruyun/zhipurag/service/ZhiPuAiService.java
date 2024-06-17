package com.ruyun.zhipurag.service;

import com.ruyun.zhipurag.entiy.Message;
import com.ruyun.zhipurag.entiy.LogInfo;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ZhiPuAiService {

    private final String knowledgeId;
    private final String promptTemplate;
    private final ClientV4 client;
    private static LogInfo logInfo = new LogInfo();


    public ZhiPuAiService(@Value("${zhipu.key}") String apiKey,
                          @Value("${zhipu.knowledge_id}") String knowledgeId,
                          @Value("${zhipu.prompt}") String promptTemplate) {
        this.knowledgeId = knowledgeId;
        this.promptTemplate = promptTemplate;
        this.client = new ClientV4.Builder(apiKey).build();
    }

    public Message chat(String userMessage, String requestId) {
        List<ChatMessage> messages = List.of(new ChatMessage(ChatMessageRole.USER.value(), userMessage));
        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .messages(messages)
                .requestId(requestId == null ? null : String.format("ruyun-%d", System.currentTimeMillis()))
                .tools(getChatTools())
                .toolChoice("auto")
                .build();

        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        StringBuilder responseBuilder = new StringBuilder();

        if (sseModelApiResp.isSuccess()) {
            ChatMessageAccumulator messageInfo = mapStreamToAccumulator(sseModelApiResp.getFlowable())
                    .doOnNext(accumulator -> {
                        if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                            responseBuilder.append(accumulator.getDelta().getContent());
                        }
                    })
                    .doOnComplete(() -> log.info("消息读取完成."))
                    .lastElement()
                    .blockingGet();
            logInfo.setId(messageInfo.getId());
            logInfo.setCreated(messageInfo.getCreated());
            Usage usage = messageInfo.getUsage();
            if (usage != null) {
                logInfo.setCompletionTokens(usage.getCompletionTokens());
                logInfo.setPromptTokens(usage.getPromptTokens());
                logInfo.setTotalTokens(usage.getTotalTokens());
            }
        } else {
            String errorMessage = "模型请求失败: " + sseModelApiResp.getMsg();
            log.error(errorMessage);
            responseBuilder.append(errorMessage);
        }
        String message = responseBuilder.toString();
        log.info("Id：{},创建时间：{},token用量：[补全：{},提示词：{}，总用量：{}]\n内容：{}\n", logInfo.getId(), logInfo.getFormattedCreated(), logInfo.getCompletionTokens(), logInfo.getPromptTokens(), logInfo.getTotalTokens(), message);
        logInfo = new LogInfo();
        return new Message(userMessage, message, requestId);
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

    public Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId()));
    }
}

