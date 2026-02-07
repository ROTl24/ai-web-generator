package com.uloaix.xiaolu_aicode.config;

import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilder;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 流式对话模型配置（多例）
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private boolean logRequests;

    private boolean logResponses;

    /**
     * 流式模型
     * @return 创建流式对话模型
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {

        SpringRestClientBuilder httpClientBuilder = SpringRestClient.builder();

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .httpClientBuilder(httpClientBuilder) // ✅ 关键：显式指定，避免多 HTTP client 冲突
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses);

        // DeepSeek thinking/tool-calls：需要解析 reasoning_content 并在工具调用链路回传 reasoning_content
        boolean isDeepSeek = modelName != null && modelName.toLowerCase().startsWith("deepseek");
        if (isDeepSeek) {
            builder.returnThinking(true)
                    .sendThinking(true); // 默认字段名 "reasoning_content"
        }

        // DeepSeek thinking/reasoner 模型对参数更严格：temperature 可能会触发 400
        if (temperature != null && !"deepseek-reasoner".equalsIgnoreCase(modelName)) {
            builder.temperature(temperature);
        }

        return builder.build();
    }

}
