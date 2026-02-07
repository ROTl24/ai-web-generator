package com.uloaix.xiaolu_aicode.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import com.uloaix.xiaolu_aicode.core.progress.BuildProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentMap<String, CompletableFuture<Boolean>> buildTaskMap = new ConcurrentHashMap<>();

    @Resource
    private BuildProgressService buildProgressService;

    /**
     * 构建异步项目，不阻塞主流程
     * @param projectPath 项目路径
     */
    public void buildProjectAsync(String projectPath) {
        submitBuildTask(projectPath)
                .exceptionally(error -> {
                    log.error("异步构建Vue项目时发生异常：{}", error.getMessage(), error);
                    return false;
                });
    }

    /**
     * 构建 Vue 项目
     *
     * @param projectPath 项目根目录路径
     * @return 是否构建成功
     */
    public boolean buildProject(String projectPath) {
        try {
            return submitBuildTask(projectPath).get();
        } catch (Exception e) {
            log.error("构建Vue项目失败：{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 提交构建任务（同一项目路径复用任务）
     */
    private CompletableFuture<Boolean> submitBuildTask(String projectPath) {
        return buildTaskMap.compute(projectPath, (path, existing) -> {
            if (existing != null && !existing.isDone()) {
                log.info("检测到项目正在构建，复用任务: {}", path);
                return existing;
            }
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> doBuildProject(path), executor);
            future.whenComplete((result, error) -> buildTaskMap.remove(path, future));
            return future;
        });
    }

    /**
     * 实际执行构建流程
     */
    private boolean doBuildProject(String projectPath) {
        buildProgressService.startBuild(projectPath);
        try {
            File projectDir = new File(projectPath);
            if (!projectDir.exists() || !projectDir.isDirectory()) {
                log.error("项目目录不存在: {}", projectPath);
                buildProgressService.completeFailed(projectPath, "项目目录不存在，构建失败");
                return false;
            }
            // 检查 package.json 是否存在
            File packageJson = new File(projectDir, "package.json");
            if (!packageJson.exists()) {
                log.error("package.json 文件不存在: {}", packageJson.getAbsolutePath());
                buildProgressService.completeFailed(projectPath, "package.json 不存在，构建失败");
                return false;
            }
            log.info("开始构建 Vue 项目: {}", projectPath);
            // 执行 npm install
            buildProgressService.updateProgress(projectPath, 15, "install", "安装依赖中");
            if (!executeNpmInstall(projectDir)) {
                log.error("npm install 执行失败");
                buildProgressService.completeFailed(projectPath, "安装依赖失败");
                return false;
            }
            // 执行 npm run build
            buildProgressService.updateProgress(projectPath, 70, "build", "项目构建中");
            if (!executeNpmBuild(projectDir)) {
                log.error("npm run build 执行失败");
                buildProgressService.completeFailed(projectPath, "项目构建失败");
                return false;
            }
            // 验证 dist 目录是否生成
            buildProgressService.updateProgress(projectPath, 90, "verify", "校验构建产物");
            File distDir = new File(projectDir, "dist");
            if (!distDir.exists()) {
                log.error("构建完成但 dist 目录未生成: {}", distDir.getAbsolutePath());
                buildProgressService.completeFailed(projectPath, "未生成 dist 目录");
                return false;
            }
            log.info("Vue 项目构建成功，dist 目录: {}", distDir.getAbsolutePath());
            buildProgressService.completeSuccess(projectPath, "构建完成");
            return true;
        } catch (Exception e) {
            log.error("构建 Vue 项目异常: {}", e.getMessage(), e);
            buildProgressService.completeFailed(projectPath, "构建异常，请重试");
            return false;
        }
    }


    /**
     * 执行 npm install 命令
     */
    private boolean executeNpmInstall(File projectDir) {
        // 优化策略1：如果 node_modules 已存在，直接跳过安装
        // 这对于“修改提示词重新生成”的场景非常有效，能将时间从几分钟缩短到几秒
        File nodeModules = new File(projectDir, "node_modules");
        if (nodeModules.exists() && nodeModules.isDirectory()) {
            log.info("检测到 node_modules 已存在，跳过 npm install 以加速构建");
            return true;
        }

        log.info("执行 npm install...");
        // 优化策略2：建议在服务器安装 pnpm，并将此处改为 pnpm install，速度可提升 3-5 倍
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5分钟超时
    }

    /**
     * 执行 npm run build 命令
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("执行 npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180); // 3分钟超时
    }

    /**
     * 根据操作系统构造命令
     * @param baseCommand
     * @return
     */
    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }


    /**
     * 操作系统检测
     * @return
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }



    /**
     * 执行命令
     *
     * @param workingDir     工作目录
     * @param command        命令字符串
     * @param timeoutSeconds 超时时间（秒）
     * @return 是否执行成功
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
    }

}
