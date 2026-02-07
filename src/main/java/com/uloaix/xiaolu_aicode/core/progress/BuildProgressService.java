package com.uloaix.xiaolu_aicode.core.progress;

import com.uloaix.xiaolu_aicode.model.dto.build.BuildProgressEvent;
import com.uloaix.xiaolu_aicode.model.enums.BuildProgressStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class BuildProgressService {

    private final ConcurrentMap<String, Sinks.Many<BuildProgressEvent>> sinkMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BuildProgressEvent> latestEventMap = new ConcurrentHashMap<>();

    public void markWaiting(String projectPath) {
        emit(projectPath, BuildProgressStatusEnum.WAITING, 0, "waiting", "等待构建开始");
    }

    public void startBuild(String projectPath) {
        emit(projectPath, BuildProgressStatusEnum.RUNNING, 0, "prepare", "开始构建");
    }

    public void updateProgress(String projectPath, int percent, String step, String message) {
        emit(projectPath, BuildProgressStatusEnum.RUNNING, percent, step, message);
    }

    public void completeSuccess(String projectPath, String message) {
        emit(projectPath, BuildProgressStatusEnum.SUCCESS, 100, "done", message);
    }

    public void completeFailed(String projectPath, String message) {
        emit(projectPath, BuildProgressStatusEnum.FAILED, 100, "failed", message);
    }

    public Flux<BuildProgressEvent> watch(String projectPath) {
        String key = normalizeKey(projectPath);
        Sinks.Many<BuildProgressEvent> sink = sinkMap.get(key);
        if (sink != null) {
            return sink.asFlux();
        }
        BuildProgressEvent latest = latestEventMap.get(key);
        if (latest != null) {
            return Flux.just(latest);
        }
        return sinkMap.computeIfAbsent(key, k -> Sinks.many().replay().latest()).asFlux();
    }

    private void emit(String projectPath,
                      BuildProgressStatusEnum status,
                      int percent,
                      String step,
                      String message) {
        String key = normalizeKey(projectPath);
        int safePercent = Math.max(0, Math.min(100, percent));
        BuildProgressEvent event = BuildProgressEvent.builder()
                .status(status.getValue())
                .step(step)
                .percent(safePercent)
                .message(message)
                .build();
        latestEventMap.put(key, event);
        Sinks.Many<BuildProgressEvent> sink = sinkMap.computeIfAbsent(key, k -> Sinks.many().replay().latest());
        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isFailure() && result != Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER) {
            log.debug("构建进度事件发送失败: {}, result={}", key, result);
        }
        if (BuildProgressStatusEnum.isFinished(status.getValue())) {
            sink.tryEmitComplete();
            sinkMap.remove(key, sink);
        }
    }

    private String normalizeKey(String projectPath) {
        if (projectPath == null) {
            return "";
        }
        try {
            Path path = Paths.get(projectPath);
            return path.toAbsolutePath().normalize().toString();
        } catch (InvalidPathException e) {
            return projectPath;
        }
    }
}
