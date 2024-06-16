package com.ruyun.zhipurag.service;

import com.ruyun.zhipurag.entiy.Message;
import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
    AtomicBoolean complete = new AtomicBoolean(false);

    if (sseModelApiResp.isSuccess()) {
        mapStreamToAccumulator(sseModelApiResp.getFlowable())
                .doOnNext(accumulator -> {
                    if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                        responseBuilder.append(accumulator.getDelta().getContent());
                    }
//                    if ("stop".equals(accumulator.getFinishReason())) {
//                        complete.set(true);
//                    }
                })
                .doOnComplete(() -> log.info("Model API invocation completed."))
                .lastElement()
                .blockingGet();

        // 提取API响应中的信息
        ModelData modelData = sseModelApiResp.getData();
        if (modelData != null && modelData.getChoices() != null && !modelData.getChoices().isEmpty()) {
            String content = modelData.getChoices().get(0).getMessage().getContent().toString();
            int completionTokens = modelData.getUsage().getCompletionTokens();
            int promptTokens = modelData.getUsage().getPromptTokens();
            int totalTokens = modelData.getUsage().getTotalTokens();
            String model = modelData.getModel();
            long timestamp = modelData.getCreated();

            // 构建Message对象
            Message responseMessage = new Message(userMessage, responseBuilder.toString().replace("\\n", "\n"), complete.get());

            // 在日志中记录完整的返回信息
            String formattedTimestamp = new SimpleDateFormat("yyyy-MM-dd").format(new Date(timestamp * 1000));
            String logMessage = String.format("request_id: %s, 时间: %s, token用量: {补全: %d, 提示词: %d, 总用量: %d}, 模型: %s, 内容: %s",
                    requestId,
                    formattedTimestamp,
                    completionTokens,
                    promptTokens,
                    totalTokens,
                    model,
                    content
            );
//                log.info(logMessage);
            System.out.println(logMessage);
            return responseMessage;
        }
    } else {
        responseBuilder.append("Model invocation failed: ").append(sseModelApiResp.getMsg());
        log.error("Model invocation failed: {}", sseModelApiResp.getMsg());
    }

    return new Message(userMessage, responseBuilder.toString(), false);
}



    private @NotNull List<ChatTool> getChatTools() {
        List<ChatTool> chatToolList = new ArrayList<>();

        // 配置web_search工具
        ChatTool webSearchTool = new ChatTool();
        webSearchTool.setType(ChatToolType.WEB_SEARCH.value());
        WebSearch webSearch = new WebSearch();
        webSearch.setEnable(true);
        webSearchTool.setWeb_search(webSearch);
        chatToolList.add(webSearchTool);

        // 配置retrieval工具
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
