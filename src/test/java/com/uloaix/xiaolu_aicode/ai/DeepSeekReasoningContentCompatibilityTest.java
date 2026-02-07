package com.uloaix.xiaolu_aicode.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.openai.internal.OpenAiUtils;
import dev.langchain4j.model.openai.internal.chat.ChatCompletionRequest;
import dev.langchain4j.model.openai.internal.chat.Message;

/**
 * DeepSeek thinking/tool-calls 兼容性回归测试：
 * DeepSeek 在 tool_calls 链路里要求 assistant message 必须显式包含 reasoning_content 字段，
 * 否则会报错：Missing reasoning_content field...
 */
class DeepSeekReasoningContentCompatibilityTest {

    @Test
    void assistantMessageShouldContainReasoningContentFieldWhenToolCallsPresent() throws Exception {
        ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                .id("call_1")
                .name("writeFile")
                .arguments("{\"relativeFilePath\":\"src/main.js\",\"content\":\"console.log(1)\",\"appId\":1}")
                .build();

        // 关键：LangChain4j 1.11.x 会把 DeepSeek 的 reasoning_content 解析进 AiMessage.thinking()（returnThinking=true）。
        // sendThinking=true 时，会在后续 tool_calls 链路把该 thinking 回传给 DeepSeek（否则可能 400: Missing reasoning_content field）。
        AiMessage aiMessage = AiMessage.builder()
                .toolExecutionRequests(List.of(toolExecutionRequest))
                .thinking("test-thinking")
                .build();
        // LangChain4j 1.11.x：是否在 assistant message 中发送 reasoning_content 由 sendThinking 开关控制
        Message openAiMessage = OpenAiUtils.toOpenAiMessage(aiMessage, true, "reasoning_content");

        ChatCompletionRequest req = ChatCompletionRequest.builder()
                .model("deepseek-reasoner")
                .messages(openAiMessage)
                .build();

        String json = new ObjectMapper().writeValueAsString(req);
        assertTrue(json.contains("\"reasoning_content\":\"test-thinking\""), json);
    }
}

