package com.uloaix.xiaolu_aicode.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.uloaix.xiaolu_aicode.ai.guardrail.PromptSafetyInputGuardrail;
import com.uloaix.xiaolu_aicode.ai.guardrail.RetryOutputGuardrail;
import com.uloaix.xiaolu_aicode.ai.tools.ToolManager;
import com.uloaix.xiaolu_aicode.exception.BusinessException;
import com.uloaix.xiaolu_aicode.exception.ErrorCode;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;
import com.uloaix.xiaolu_aicode.service.ChatHistoryService;
import com.uloaix.xiaolu_aicode.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

/**
 * AI服务创建工厂
 */

@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * Vue 工程“创建”服务缓存
     */
    private final Cache<Long, AiVueProjectCreateService> vueCreateServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> log.debug("Vue 创建服务实例被移除，appId: {}, 原因: {}", key, cause))
            .build();

    /**
     * Vue 工程“修改”服务缓存
     */
    private final Cache<Long, AiVueProjectModifyService> vueModifyServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> log.debug("Vue 修改服务实例被移除，appId: {}, 原因: {}", key, cause))
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     *  @param appId 应用ID
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务（带缓存）
     *  @param appId 应用ID
     *  @param codeGenType 服务类型
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId,CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey,key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 获取 Vue 工程创建服务（带缓存）
     */
    public AiVueProjectCreateService getAiVueProjectCreateService(long appId) {
        return vueCreateServiceCache.get(appId, key -> createAiVueProjectCreateService(appId));
    }

    /**
     * 获取 Vue 工程修改服务（带缓存）
     */
    public AiVueProjectModifyService getAiVueProjectModifyService(long appId) {
        return vueModifyServiceCache.get(appId, key -> createAiVueProjectModifyService(appId));
    }


    /**
     * 创建新的 AI 服务实例
     * @param appId 应用ID
     * @param codeGenType 服务类型
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        MessageWindowChatMemory chatMemory = buildChatMemory(appId, 20);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            case VUE_PROJECT -> {

                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools((Object[]) toolManager.getAllTools())
                        .inputGuardrails(new PromptSafetyInputGuardrail())  // 输入护轨
                        .outputGuardrails(new RetryOutputGuardrail())// 输出护轨
                        .maxSequentialToolsInvocations(20) //最多连续调用 20次工具
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();
            }
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                // 使用多例模式的 ChatModel，避免共享单例导致并发串行化
                ChatModel chatModel = SpringContextUtil.getBean("chatModelPrototype", ChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())  // 输入护轨
                        .outputGuardrails(new RetryOutputGuardrail()) // 输出护轨
                        .maxSequentialToolsInvocations(20) //最多连续调用 20次工具
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };

    }

    /**
     * 创建 Vue 工程创建服务实例（最小工具集）
     */
    private AiVueProjectCreateService createAiVueProjectCreateService(long appId) {
        MessageWindowChatMemory chatMemory = buildChatMemory(appId, 20);
        Object[] tools = requireTools("writeFile");
        // 使用多例模式的 StreamingChatModel 解决并发问题
        StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiVueProjectCreateService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(tools)
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }

    /**
     * 创建 Vue 工程修改服务实例（按修改场景提供工具集合）
     */
    private AiVueProjectModifyService createAiVueProjectModifyService(long appId) {
        MessageWindowChatMemory chatMemory = buildChatMemory(appId, 20);
        Object[] tools = requireTools("readDir", "readFile", "modifyFile", "writeFile", "deleteFile");
        // 使用多例模式的 StreamingChatModel 解决并发问题
        StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiVueProjectModifyService.class)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools(tools)
                .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                ))
                .build();
    }
    /**
     * 创建AI代码生成器服务
     * @return
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 根据 appId 构建独立的对话记忆，并从数据库加载历史
     */
    private MessageWindowChatMemory buildChatMemory(long appId, int maxMessages) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(maxMessages)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, maxMessages);
        return chatMemory;
    }

    /**
     * 只传递需要的工具：按名称获取，并确保工具齐全
     */
    private Object[] requireTools(String... toolNames) {
        Object[] tools = toolManager.getTools(toolNames);
        if (tools.length != toolNames.length) {
            throw new IllegalStateException("工具注册不完整，期望: " + Arrays.toString(toolNames) + "，实际数量: " + tools.length);
        }
        return tools;
    }

    /**
     * 构造缓存键
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

}
