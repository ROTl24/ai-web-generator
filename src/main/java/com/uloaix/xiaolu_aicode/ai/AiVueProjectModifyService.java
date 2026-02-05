package com.uloaix.xiaolu_aicode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * Vue 工程修改 AI Service
 * <p>
 * 用于“在已有项目上做增量修改”的迭代场景，要求先读再改、最小改动。
 */
public interface AiVueProjectModifyService {

    /**
     * 修改已有 Vue3 工程（流式）
     *
     * @param appId       应用 id（用于隔离对话记忆与工具作用域）
     * @param userMessage 用户修改需求
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-modify-system-prompt.txt")
    TokenStream modifyVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);
}

