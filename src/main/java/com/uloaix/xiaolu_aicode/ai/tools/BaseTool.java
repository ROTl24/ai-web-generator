package com.uloaix.xiaolu_aicode.ai.tools;

import cn.hutool.json.JSONObject;

/**
 * 工具基类
 * 定义所有工具的通用接口
 */
public abstract class BaseTool {

    /**
     * 获取工具的英文名称（对应方法名）
     *
     * @return 工具英文名称
     */
    public abstract String getToolName();

    /**
     * 获取工具的中文显示名称
     *
     * @return 工具中文名称
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return 工具请求显示内容
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 规范化工具输入路径，避免出现以 "/" 开头导致的绝对路径误判。
     * 仅保留 Windows 盘符绝对路径，其它情况按相对路径处理。
     */
    protected String normalizeRelativePath(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (hasWindowsDrivePrefix(trimmed)) {
            return trimmed;
        }
        String normalized = trimmed.replace("\\", "/");
        normalized = normalized.replaceFirst("^\\./", "");
        normalized = normalized.replaceFirst("^/+", "");
        return normalized;
    }

    /**
     * 判断是否为 Windows 盘符绝对路径（如 C:\path 或 D:/path）。
     */
    protected boolean hasWindowsDrivePrefix(String path) {
        return path != null && path.matches("^[a-zA-Z]:[\\\\/].*");
    }

    /**
     * 生成工具执行结果格式（保存到数据库）
     *
     * @param arguments 工具执行参数
     * @return 格式化的工具执行结果
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);
}
