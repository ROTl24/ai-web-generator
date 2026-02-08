package com.uloaix.xiaolu_aicode.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.uloaix.xiaolu_aicode.constant.AppConstant;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;
import com.uloaix.xiaolu_aicode.service.AppVersionService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

/**
 * 文件目录读取工具
 * 使用 Hutool 简化文件操作
 */
@Slf4j
@Component
public class FileDirReadTool extends BaseTool {

    @jakarta.annotation.Resource
    private AppVersionService appVersionService;

    /**
     * 需要忽略的文件和目录
     */
    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules", ".git", "dist", "build", ".DS_Store",
            ".env", "target", ".mvn", ".idea", ".vscode", "coverage"
    );

    /**
     * 需要忽略的文件扩展名
     */
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log", ".tmp", ".cache", ".lock"
    );

    @Tool("读取目录结构，获取指定目录下的所有文件和子目录信息")
    public String readDir(
            @P("目录的相对路径，为空则读取整个项目结构")
            String relativeDirPath,
            @ToolMemoryId Long appId
    ) {
        try {
            String normalizedPath = normalizeRelativePath(relativeDirPath);
            Path path = Paths.get(normalizedPath);
            if (!path.isAbsolute()) {
                String projectRootDir = appVersionService.resolveActiveVersionDir(CodeGenTypeEnum.VUE_PROJECT, appId);
                if (projectRootDir == null) {
                    String projectDirName = "vue_project_" + appId;
                    projectRootDir = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName).toString();
                }
                Path projectRoot = Paths.get(projectRootDir);
                path = projectRoot.resolve(normalizedPath);
            }
            File targetDir = path.toFile();
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                return "错误：目录不存在或不是目录 - " + (normalizedPath.isEmpty() ? "根目录" : normalizedPath);
            }
            StringBuilder structure = new StringBuilder();
            structure.append("项目目录结构:\n");
            // 使用 Hutool 递归获取所有文件
            List<File> allFiles = FileUtil.loopFiles(targetDir, file -> !shouldIgnore(file.getName()));
            // 按相对路径排序显示（更利于大模型定位文件）
            allFiles.stream()
                    .map(file -> {
                        Path relativePath = targetDir.toPath().relativize(file.toPath());
                        // 统一使用 /，避免 Windows 路径分隔符干扰
                        return relativePath.toString().replace("\\", "/");
                    })
                    .sorted()
                    .forEach(file -> {
                        structure.append("- ").append(file).append("\n");
                    });
            return structure.toString();

        } catch (Exception e) {
            String errorMessage = "读取目录结构失败: " + relativeDirPath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * 判断是否应该忽略该文件或目录
     */
    private boolean shouldIgnore(String fileName) {
        // 检查是否在忽略名称列表中
        if (IGNORED_NAMES.contains(fileName)) {
            return true;
        }

        // 检查文件扩展名
        return IGNORED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }

    @Override
    public String getToolName() {
        return "readDir";
    }

    @Override
    public String getDisplayName() {
        return "读取目录";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeDirPath = arguments.getStr("relativeDirPath");
        String normalizedPath = normalizeRelativePath(relativeDirPath);
        if (StrUtil.isEmpty(normalizedPath)) {
            normalizedPath = "根目录";
        }
        return String.format(" [⚒️工具调用] %s %s", getDisplayName(), normalizedPath);
    }
}
