package com.ruyun.zhipurag;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.Constants;
import com.zhipu.oapi.service.v4.model.*;
import io.reactivex.Flowable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZhiPuAiStreamTest {
    private static final String API_KEY = "afa11a1014b7cf92395f9506c6132f54.2wHlRDiYn921lRDy";
    private static final ClientV4 client = new ClientV4.Builder(API_KEY).build();

    public static void main(String[] args) {
        testSseInvoke();
    }

    private static void testSseInvoke() {
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage(ChatMessageRole.USER.value(), "刮宫终止妊娠");
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
        if (sseModelApiResp.isSuccess()) {
            AtomicBoolean isFirst = new AtomicBoolean(true);
            mapStreamToAccumulator(sseModelApiResp.getFlowable())
                    .doOnNext(accumulator -> {
                        if (isFirst.getAndSet(false)) {
                            System.out.print("Response: ");
                        }
                        if (accumulator.getDelta() != null && accumulator.getDelta().getContent() != null) {
                            System.out.print(accumulator.getDelta().getContent());
                        }
                    })
                    .doOnComplete(System.out::println)
                    .lastElement()
                    .blockingGet();
        } else {
            System.out.println("Model invocation failed: " + sseModelApiResp.getMsg());
        }
    }

    private static @NotNull List<ChatTool> getChatTools() {
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
        retrieval.setKnowledge_id("1776965889154023424");
        retrieval.setPrompt_template("从文档\n\"\"\"\n{{knowledge}}\n\"\"\"\n分别寻找中找\n\"\"\"\n{{question}}\n\"\"\"\n当中以逗号分隔的文本所表达语义相同、相似或有直接逻辑关系的文本，并直接返回找到的文本片段。");
        retrievalTool.setRetrieval(retrieval);
        chatToolList.add(retrievalTool);
        return chatToolList;
    }

    public static Flowable<ChatMessageAccumulator> mapStreamToAccumulator(Flowable<ModelData> flowable) {
        return flowable.map(chunk -> new ChatMessageAccumulator(chunk.getChoices().get(0).getDelta(), null, chunk.getChoices().get(0), chunk.getUsage(), chunk.getCreated(), chunk.getId()));
    }
}
