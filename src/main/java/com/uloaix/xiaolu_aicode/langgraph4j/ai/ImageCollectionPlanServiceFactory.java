package com.uloaix.xiaolu_aicode.langgraph4j.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import com.uloaix.xiaolu_aicode.utils.SpringContextUtil;

/**
 * 图片收集计划工厂
 */
@Configuration
public class ImageCollectionPlanServiceFactory {

    @Bean
    @Scope("prototype")
    public ImageCollectionPlanService createImageCollectionPlanService() {
        ChatModel chatModel = SpringContextUtil.getBean("chatModelPrototype", ChatModel.class);
        return AiServices.builder(ImageCollectionPlanService.class)
                .chatModel(chatModel)
                .build();
    }
}
