package com.uloaix.xiaolu_aicode.manager;

import com.uloaix.xiaolu_aicode.utils.WebScreenshotUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 截图任务管理器：单线程队列串行执行
 */
@Component
@Slf4j
public class ScreenshotManager {

    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

    private final WebDriver webDriver = WebScreenshotUtils.initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("screenshot-worker");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 提交截图任务并返回异步结果
     *
     * @param url 目标页面地址
     * @return 截图路径（失败返回 null）
     */
    public CompletableFuture<String> takeScreenshot(String url) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return WebScreenshotUtils.saveWebPageScreenshot(webDriver, url);
            } catch (Exception e) {
                log.error("截图任务执行失败: {}", e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    @PreDestroy
    public void destroy() {
        try {
            webDriver.quit();
        } catch (Exception e) {
            log.warn("关闭 WebDriver 失败: {}", e.getMessage());
        }
        executor.shutdown();
    }
}
