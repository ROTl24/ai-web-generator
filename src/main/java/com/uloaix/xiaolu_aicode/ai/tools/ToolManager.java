package com.uloaix.xiaolu_aicode.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工具管理器
 * 统一管理所有工具，提供根据名称获取工具的功能
 */
@Slf4j
@Component
public class ToolManager {

    /**
     * 工具名称到工具实例的映射
     */
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    /**
     * 自动注入所有工具
     */
    @Resource
    private BaseTool[] tools;

    /**
     * 初始化工具映射
     */
    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("注册工具: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("工具管理器初始化完成，共注册 {} 个工具", toolMap.size());
    }

    /**
     * 根据工具名称获取工具实例
     *
     * @param toolName 工具英文名称
     * @return 工具实例
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 按工具英文名称（方法名）获取工具集合（保持入参顺序）
     * <p>
     * 用于按场景最小化传递给 AI 的工具集合，减少工具调用幻觉。
     *
     * @param toolNames 工具英文名称列表（如 readFile/writeFile）
     * @return 工具实例集合（不存在的工具会被忽略并记录日志）
     */
    public BaseTool[] getTools(String... toolNames) {
        if (toolNames == null || toolNames.length == 0) {
            return new BaseTool[0];
        }
        List<BaseTool> result = new ArrayList<>(toolNames.length);
        for (String toolName : toolNames) {
            BaseTool tool = toolMap.get(toolName);
            if (tool == null) {
                log.warn("未找到工具: {}", toolName);
                continue;
            }
            result.add(tool);
        }
        return result.stream().filter(Objects::nonNull).toArray(BaseTool[]::new);
    }

    /**
     * 获取已注册的工具集合
     *
     * @return 工具实例集合
     */
    public BaseTool[] getAllTools() {
        return tools;
    }
}
