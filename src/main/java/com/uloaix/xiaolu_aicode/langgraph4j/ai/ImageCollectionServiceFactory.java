package com.uloaix.xiaolu_aicode.langgraph4j.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.uloaix.xiaolu_aicode.langgraph4j.tools.ImageSearchTool;
import com.uloaix.xiaolu_aicode.langgraph4j.tools.LogoGeneratorTool;
import com.uloaix.xiaolu_aicode.langgraph4j.tools.MermaidDiagramTool;
import com.uloaix.xiaolu_aicode.langgraph4j.tools.UndrawIllustrationTool;
import com.uloaix.xiaolu_aicode.utils.SpringContextUtil;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class ImageCollectionServiceFactory {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    /**
     * 创建图片收集 AI 服务
     */
    @Bean
    @Scope("prototype")
    public ImageCollectionService createImageCollectionService() {
        ChatModel chatModel = SpringContextUtil.getBean("chatModelPrototype", ChatModel.class);
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(
                        imageSearchTool,
                        undrawIllustrationTool,
                        mermaidDiagramTool,
                        logoGeneratorTool
                )
                .build();
    }
}
