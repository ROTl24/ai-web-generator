package com.uloaix.xiaolu_aicode.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.uloaix.xiaolu_aicode.ai.model.message.*;
import com.uloaix.xiaolu_aicode.ai.tools.BaseTool;
import com.uloaix.xiaolu_aicode.ai.tools.ToolManager;
import com.uloaix.xiaolu_aicode.core.builder.VueProjectBuilder;
import com.uloaix.xiaolu_aicode.model.entity.User;
import com.uloaix.xiaolu_aicode.model.enums.ChatHistoryMessageTypeEnum;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;
import com.uloaix.xiaolu_aicode.service.AppVersionService;
import com.uloaix.xiaolu_aicode.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON 消息流处理器
 * 处理 VUE_PROJECT 类型的复杂流式响应，包含工具调用信息
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ToolManager toolManager;

    @Resource
    private AppVersionService appVersionService;
    /**
     * 处理 TokenStream（VUE_PROJECT）
     * 解析 JSON 消息并重组为完整的响应格式
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param appId              应用ID
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // 过滤空字串
                .doOnComplete(() -> {
                    // 流式响应完成后，添加 AI 消息到对话历史
                    // 注意：doOnComplete 里的异常会把“正常结束”变成 error，进而影响 SSE（触发全局异常处理器）。
                    try {
                        String aiResponse = chatHistoryStringBuilder.toString();
                        chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                        String projectPath = appVersionService.resolveActiveVersionDir(CodeGenTypeEnum.VUE_PROJECT, appId);
                        vueProjectBuilder.buildProjectAsync(projectPath);
                        int version = appVersionService.resolveActiveVersion(appId);
                        appVersionService.markVersionReady(appId, version);
                    } catch (Exception e) {
                        log.error("流式完成回调失败（已忽略），appId={}", appId, e);
                    }
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    try {
                        String errorMessage = "AI回复失败: " + error.getMessage();
                        chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                        int version = appVersionService.resolveActiveVersion(appId);
                        appVersionService.markVersionFailed(appId, version, errorMessage);
                    } catch (Exception e) {
                        log.error("流式错误回调失败（已忽略），appId={}", appId, e);
                    }
                });
    }

    /**
     * 解析并收集 TokenStream 数据
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // 解析 JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // 直接拼接响应
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 检查是否是第一次看到这个工具 ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // 第一次调用这个工具，记录 ID 并完整返回工具信息
                    seenToolIds.add(toolId);
                    BaseTool tool = toolManager.getTool(toolRequestMessage.getName());
                    if (tool == null) {
                        log.warn("收到未知工具请求: {}", toolRequestMessage.getName());
                        return String.format("\n\n[选择工具] 未知工具：%s\n\n", toolRequestMessage.getName());
                    }
                    return tool.generateToolRequestResponse();
                } else {
                    // 不是第一次调用这个工具，直接返回空
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                BaseTool tool = toolManager.getTool(toolExecutedMessage.getName());
                String result;
                if (tool == null) {
                    log.warn("收到未知工具执行结果: {}", toolExecutedMessage.getName());
                    result = String.format("[⚒️工具调用] 未知工具 %s\n参数：%s",
                            toolExecutedMessage.getName(), toolExecutedMessage.getArguments());
                } else {
                    JSONObject jsonObject;
                    try {
                        jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                    } catch (Exception e) {
                        log.warn("解析工具参数失败，toolName: {}, raw: {}", toolExecutedMessage.getName(), toolExecutedMessage.getArguments());
                        jsonObject = new JSONObject();
                    }
                    result = tool.generateToolExecutedResult(jsonObject);
                }
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}
