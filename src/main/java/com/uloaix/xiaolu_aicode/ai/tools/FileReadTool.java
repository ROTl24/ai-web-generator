package com.uloaix.xiaolu_aicode.ai.tools;

import cn.hutool.json.JSONObject;
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

/**
 * 文件读取工具
 * 支持 AI 通过工具调用的方式读取文件内容
 */
@Slf4j
@Component
public class FileReadTool extends BaseTool{

    @jakarta.annotation.Resource
    private AppVersionService appVersionService;

    @Tool("读取指定路径的文件内容")
    public String readFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @ToolMemoryId Long appId
    ) {
        try {
            String normalizedPath = normalizeRelativePath(relativeFilePath);
            if (normalizedPath.isEmpty()) {
                return "错误：文件路径为空";
            }
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
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件 - " + normalizedPath;
            }
            return Files.readString(path);
        } catch (IOException e) {
            String errorMessage = "读取文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String normalizedPath = normalizeRelativePath(relativeFilePath);
        return String.format("[⚒️工具调用] %s %s", getDisplayName(),
                normalizedPath.isEmpty() ? relativeFilePath : normalizedPath);
    }
}


