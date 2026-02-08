package com.uloaix.xiaolu_aicode.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.uloaix.xiaolu_aicode.constant.AppConstant;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;
import com.uloaix.xiaolu_aicode.service.AppVersionService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件。
 * <p>
 * 内置 <b>重复写入拦截</b> 机制：基于 appId 跟踪已写入文件，
 * 同一文件第二次写入时直接拦截并返回已写入文件清单，防止 AI 遗忘导致的死循环。
 */
@Slf4j
@Component
public class FileWriteTool extends BaseTool {

    @jakarta.annotation.Resource
    private AppVersionService appVersionService;

    /**
     * 每个 appId 对应一组已写入的文件路径（标准化后）。
     * <p>
     * 缓存策略：写入后 30 分钟 / 最后访问后 10 分钟自动过期，避免内存泄漏。
     */
    private final Cache<String, Set<String>> writtenFilesTracker = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    @Tool("写入文件到指定路径。注意：同一文件只能写入一次，重复写入会被拦截。")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        // 标准化路径用于去重比较
        String normalizedPath = normalizeRelativePath(relativeFilePath);
        if (normalizedPath.isEmpty()) {
            return "错误：文件路径为空";
        }

        int activeVersion = appVersionService.resolveActiveVersion(appId);
        String trackerKey = buildTrackerKey(appId, activeVersion);
        // 获取或创建当前 appId + 版本 的已写入文件集合
        Set<String> writtenFiles = writtenFilesTracker.get(trackerKey, k -> ConcurrentHashMap.newKeySet());

        // ============ 重复写入拦截 ============
        if (writtenFiles.contains(normalizedPath)) {
            log.warn("拦截重复写入文件: {} (appId: {}, version: {}), 已写入文件数: {}", relativeFilePath, appId, activeVersion, writtenFiles.size());
            String fileList = writtenFiles.stream().sorted().collect(Collectors.joining(", "));
            return String.format(
                    "⚠️ 文件 [%s] 已在本次会话中写入过，重复写入已被拦截。" +
                    "已写入的全部文件列表: [%s]（共 %d 个，version=%d）。" +
                    "请不要再次写入已有的文件，继续创建其他尚未写入的文件；如果所有文件均已完成，请直接输出生成完毕提示。",
                    relativeFilePath, fileList, writtenFiles.size(), activeVersion
            );
        }

        try {
            Path path = Paths.get(normalizedPath);
            if (!path.isAbsolute()) {
                // 相对路径处理，基于 appId + 版本的项目目录
                String projectRootDir = appVersionService.resolveActiveVersionDir(CodeGenTypeEnum.VUE_PROJECT, appId);
                if (projectRootDir == null) {
                    String projectDirName = "vue_project_" + appId;
                    projectRootDir = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName).toString();
                }
                Path projectRoot = Paths.get(projectRootDir);
                path = projectRoot.resolve(normalizedPath);
            }
            // 创建父目录（如果不存在）
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            // 记录已写入文件
            writtenFiles.add(normalizedPath);

            log.info("成功写入文件: {} (appId: {}, version: {}, 已写入 {} 个文件)", path.toAbsolutePath(), appId, activeVersion, writtenFiles.size());
            // 返回写入成功信息 + 已写入文件清单，为 AI 提供外部记忆
            String fileList = writtenFiles.stream().sorted().collect(Collectors.joining(", "));
            return String.format("文件写入成功: %s（本次已写入 %d 个文件: [%s]，version=%d）",
                    normalizedPath, writtenFiles.size(), fileList, activeVersion);
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 清除指定 appId 的文件写入记录。
     * <p>
     * 可在需要重新生成整个项目时调用（如用户明确要求"重新生成"）。
     *
     * @param appId 应用 ID
     */
    public void clearWrittenFiles(Long appId) {
        if (appId == null || appId <= 0) {
            return;
        }
        int activeVersion = appVersionService.resolveActiveVersion(appId);
        String trackerKey = buildTrackerKey(appId, activeVersion);
        writtenFilesTracker.invalidate(trackerKey);
        log.info("已清除 appId={} version={} 的文件写入记录", appId, activeVersion);
    }

    /**
     * 获取指定 appId 已写入的文件集合（用于外部查询/调试）
     *
     * @param appId 应用 ID
     * @return 已写入文件路径集合，若无记录则返回空集合
     */
    public Set<String> getWrittenFiles(Long appId) {
        if (appId == null || appId <= 0) {
            return Set.of();
        }
        int activeVersion = appVersionService.resolveActiveVersion(appId);
        String trackerKey = buildTrackerKey(appId, activeVersion);
        Set<String> files = writtenFilesTracker.getIfPresent(trackerKey);
        return files != null ? Set.copyOf(files) : Set.of();
    }

    /**
     * 标准化文件路径，确保相同文件的不同写法可以正确匹配。
     * <p>
     * 例如 "./index.html"、"index.html"、".\index.html" 统一为 "index.html"
     */
    private String normalizePath(String path) {
        return normalizeRelativePath(path);
    }

    private String buildTrackerKey(Long appId, int version) {
        return appId + ":" + Math.max(version, 0);
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        String normalizedPath = normalizeRelativePath(relativeFilePath);
        return String.format("""
                        [⚒️工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(),
                normalizedPath.isEmpty() ? relativeFilePath : normalizedPath,
                suffix, content);
    }
}



