package com.ruyun.zhipurag.service;

import com.ruyun.zhipurag.entiy.Message;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ZhiPuAiService {

    private final String knowledgeId;
    private final String promptTemplate;
    private final ClientV4 client;

    public ZhiPuAiService(@Value("${zhipu.key}") String apiKey,
                          @Value("${zhipu.knowledge_id}") String knowledgeId,
                          @Value("${zhipu.prompt}") String promptTemplate) {
        this.knowledgeId = knowledgeId;
        this.promptTemplate = promptTemplate;
        this.client = new ClientV4.Builder(apiKey).build();
    }

    public Message chat(String userMessage) {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), userMessage);
        messages.add(chatMessage);
        String requestId = String.format("ruyun-%d", System.currentTimeMillis());

        // 配置工具
        List<ChatTool> chatToolList = getChatTools();

        ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest.builder()
                .model(Constants.ModelChatGLM4)
                .stream(Boolean.TRUE)
                .messages(messages)
                .requestId(requestId)
                .tools(chatToolList)
                .toolChoice("auto")
                .build();

        ModelApiResponse sseModelApiResp = client.invokeModelApi(chatCompletionRequest);
        StringBuilder responseBuilder = new StringBuilder();

        if (sseModelApiResp.isSuccess()) {
            mapStreamToAccumulator(sseModelApiResp.getFlowable())
                    .doOnNext(accumulator -> {
                        if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                            responseBuilder.append(accumulator.getDelta().getContent());
                        }
                    })
                    .doOnComplete(() -> responseBuilder.append("\n"))
                    .lastElement()
                    .blockingGet();
        } else {
            responseBuilder.append("Model invocation failed: ").append(sseModelApiResp.getMsg());
        }

        // 构建Message对象
        return new Message(userMessage, responseBuilder.toString().replace("\\n", "\n"), true);
    }

    private List<ChatTool> getChatTools() {
        List<ChatTool> chatToolList = new ArrayList<>();
        ChatTool webSearchTool = new ChatTool();
        webSearchTool.setType(ChatToolType.WEB_SEARCH.value());
        WebSearch webSearch = new WebSearch();
        webSearch.setEnable(true);
        webSearchTool.setWeb_search(webSearch);
        chatToolList.add(webSearchTool);

        ChatTool retrievalTool = new ChatTool();
        retrievalTool.setType(ChatToolType.RETRIEVAL.value());
        Retrieval retrieval = new Retrieval();
        retrieval.setKnowledge_id(knowledgeId);
        retrieval.setPrompt_template(promptTemplate);
        retrievalTool.setRetrieval(retrieval);
        chatToolList.add(retrievalTool);
        return chatToolList;
    }

    public Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId()));
    }
}
