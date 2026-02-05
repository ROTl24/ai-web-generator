package com.uloaix.xiaolu_aicode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Vue 工程创建 AI Service
 * <p>
 * 仅用于“从零创建项目”的首次生成场景，配合最小化工具集以降低工具调用幻觉。
 */
public interface AiVueProjectCreateService {

    /**
     * 从零创建 Vue3 工程（流式）
     *
     * @param appId       应用 id（用于隔离对话记忆与工具作用域）
     * @param userMessage 用户需求描述
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-create-system-prompt.txt")
    TokenStream createVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}

