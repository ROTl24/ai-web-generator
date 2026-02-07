package com.uloaix.xiaolu_aicode.langgraph4j.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import com.uloaix.xiaolu_aicode.utils.SpringContextUtil;

/**
 * 代码质量检查服务工厂
 */
@Slf4j
@Configuration
public class CodeQualityCheckServiceFactory {

    /**
     * 创建代码质量检查 AI 服务
     */
    @Bean
    @Scope("prototype")
    public CodeQualityCheckService createCodeQualityCheckService() {
        ChatModel chatModel = SpringContextUtil.getBean("chatModelPrototype", ChatModel.class);
        return AiServices.builder(CodeQualityCheckService.class)
                .chatModel(chatModel)
                .build();
    }
}
