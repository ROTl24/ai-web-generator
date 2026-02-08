package com.uloaix.xiaolu_aicode.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.uloaix.xiaolu_aicode.core.builder.VueProjectBuilder;
import com.uloaix.xiaolu_aicode.exception.BusinessException;
import com.uloaix.xiaolu_aicode.exception.ErrorCode;
import com.uloaix.xiaolu_aicode.exception.ThrowUtils;
import com.uloaix.xiaolu_aicode.mapper.AppMapper;
import com.uloaix.xiaolu_aicode.mapper.AppVersionMapper;
import com.uloaix.xiaolu_aicode.model.entity.App;
import com.uloaix.xiaolu_aicode.model.entity.AppVersion;
import com.uloaix.xiaolu_aicode.model.enums.AppGenStatusEnum;
import com.uloaix.xiaolu_aicode.model.enums.AppVersionStatusEnum;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;
import com.uloaix.xiaolu_aicode.model.vo.AppVersionDiffVO;
import com.uloaix.xiaolu_aicode.model.vo.AppVersionFileDiffVO;
import com.uloaix.xiaolu_aicode.service.AppVersionService;
import com.uloaix.xiaolu_aicode.service.ScreenshotService;
import com.uloaix.xiaolu_aicode.utils.AppVersionPathUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用版本 服务层实现。
 */
@Service
@Slf4j
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion> implements AppVersionService {

    @Resource
    private AppMapper appMapper;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 生成中的版本缓存（用于工具写入定位版本目录）
     */
    private final Cache<Long, Integer> generatingVersionCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    /**
     * 当前版本缓存（减少 DB 读取）
     */
    private final Cache<Long, Integer> currentVersionCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppVersion createNewVersion(Long appId, CodeGenTypeEnum codeGenType, Long userId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户ID无效");

        int currentVersion = getCurrentVersion(appId);
        int nextVersion = currentVersion + 1;
        String versionDir = AppVersionPathUtils.buildVersionDir(codeGenType, appId, nextVersion);
        // Vue 项目：新版本默认继承上一版本代码，便于增量修改
        if (codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            String previousDir = currentVersion > 0
                    ? AppVersionPathUtils.buildVersionDir(codeGenType, appId, currentVersion)
                    : AppVersionPathUtils.buildBaseDir(codeGenType, appId);
            try {
                if (new File(previousDir).exists()) {
                    FileUtil.copyContent(new File(previousDir), new File(versionDir), true);
                }
            } catch (Exception e) {
                log.warn("复制上一版本代码失败，appId={}, from={}, to={}, err={}", appId, previousDir, versionDir, e.getMessage());
            }
        }

        AppVersion appVersion = AppVersion.builder()
                .appId(appId)
                .version(nextVersion)
                .codeGenType(codeGenType.getValue())
                .codeDir(versionDir)
                .status(AppVersionStatusEnum.GENERATING.getValue())
                .createdBy(userId)
                .build();
        boolean saved = this.save(appVersion);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建版本记录失败");

        App update = new App();
        update.setId(appId);
        update.setCurrentVersion(nextVersion);
        update.setGenStatus(AppGenStatusEnum.GENERATING.getValue());
        update.setEditTime(LocalDateTime.now());
        int updated = appMapper.update(update, true);
        ThrowUtils.throwIf(updated <= 0, ErrorCode.OPERATION_ERROR, "更新应用版本号失败");

        generatingVersionCache.put(appId, nextVersion);
        currentVersionCache.put(appId, nextVersion);
        return appVersion;
    }

    @Override
    public void markVersionReady(Long appId, Integer version) {
        if (appId == null || appId <= 0 || version == null || version <= 0) {
            return;
        }
        AppVersion update = new AppVersion();
        update.setStatus(AppVersionStatusEnum.READY.getValue());
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", appId)
                .eq("version", version);
        this.update(update, wrapper);
        App appUpdate = new App();
        appUpdate.setId(appId);
        appUpdate.setGenStatus(AppGenStatusEnum.READY.getValue());
        appMapper.update(appUpdate, true);
        generatingVersionCache.invalidate(appId);
    }

    @Override
    public void markVersionFailed(Long appId, Integer version, String reason) {
        if (appId == null || appId <= 0 || version == null || version <= 0) {
            return;
        }
        AppVersion update = new AppVersion();
        update.setStatus(AppVersionStatusEnum.FAILED.getValue());
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", appId)
                .eq("version", version);
        this.update(update, wrapper);
        App appUpdate = new App();
        appUpdate.setId(appId);
        appUpdate.setGenStatus(AppGenStatusEnum.FAILED.getValue());
        appMapper.update(appUpdate, true);
        generatingVersionCache.invalidate(appId);
        if (StrUtil.isNotBlank(reason)) {
            log.warn("版本生成失败 appId={}, version={}, reason={}", appId, version, reason);
        }
    }

    @Override
    public int getCurrentVersion(Long appId) {
        if (appId == null || appId <= 0) {
            return 0;
        }
        Integer cached = currentVersionCache.getIfPresent(appId);
        if (cached != null) {
            return cached;
        }
        App app = appMapper.selectOneById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        Integer version = app.getCurrentVersion();
        int resolved = version == null ? 0 : version;
        currentVersionCache.put(appId, resolved);
        return resolved;
    }

    @Override
    public int resolveActiveVersion(Long appId) {
        if (appId == null || appId <= 0) {
            return 0;
        }
        Integer generating = generatingVersionCache.getIfPresent(appId);
        if (generating != null) {
            return generating;
        }
        try {
            return getCurrentVersion(appId);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String buildVersionDir(CodeGenTypeEnum codeGenType, Long appId, int version) {
        return AppVersionPathUtils.buildVersionDir(codeGenType, appId, version);
    }

    @Override
    public String resolveActiveVersionDir(CodeGenTypeEnum codeGenType, Long appId) {
        int version = resolveActiveVersion(appId);
        return AppVersionPathUtils.buildVersionDir(codeGenType, appId, version);
    }

    @Override
    public boolean rollbackToVersion(Long appId, int version) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(version <= 0, ErrorCode.PARAMS_ERROR, "版本号无效");
        AppVersion target = this.getOne(QueryWrapper.create()
                .eq("appId", appId)
                .eq("version", version));
        ThrowUtils.throwIf(target == null, ErrorCode.NOT_FOUND_ERROR, "目标版本不存在");
        ThrowUtils.throwIf(AppVersionStatusEnum.FAILED.getValue().equals(target.getStatus()),
                ErrorCode.OPERATION_ERROR, "目标版本不可用");
        App update = new App();
        update.setId(appId);
        update.setCurrentVersion(version);
        update.setGenStatus(AppGenStatusEnum.READY.getValue());
        update.setEditTime(LocalDateTime.now());
        boolean updated = appMapper.update(update, true) > 0;
        if (updated) {
            currentVersionCache.put(appId, version);
        }
        return updated;
    }

    @Override
    public List<AppVersion> listVersions(Long appId) {
        if (appId == null || appId <= 0) {
            return List.of();
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", appId)
                .orderBy("version", false);
        return this.list(wrapper);
    }

    @Override
    public AppVersionDiffVO diffVersions(Long appId, int fromVersion, int toVersion, boolean withScreenshot) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID无效");
        ThrowUtils.throwIf(fromVersion <= 0 || toVersion <= 0, ErrorCode.PARAMS_ERROR, "版本号无效");

        App app = appMapper.selectOneById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        ThrowUtils.throwIf(codeGenType == null, ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");

        String fromDir = buildVersionDir(codeGenType, appId, fromVersion);
        String toDir = buildVersionDir(codeGenType, appId, toVersion);
        ThrowUtils.throwIf(!new File(fromDir).exists(), ErrorCode.NOT_FOUND_ERROR, "起始版本代码不存在");
        ThrowUtils.throwIf(!new File(toDir).exists(), ErrorCode.NOT_FOUND_ERROR, "目标版本代码不存在");

        AppVersionDiffVO diffVO = new AppVersionDiffVO();
        diffVO.setAppId(appId);
        diffVO.setFromVersion(fromVersion);
        diffVO.setToVersion(toVersion);
        diffVO.setCodeGenType(codeGenType.getValue());
        diffVO.setFromPreviewUrl(AppVersionPathUtils.buildPreviewUrl(codeGenType, appId, fromVersion));
        diffVO.setToPreviewUrl(AppVersionPathUtils.buildPreviewUrl(codeGenType, appId, toVersion));
        diffVO.setFileDiffs(diffFiles(fromDir, toDir));

        if (withScreenshot) {
            tryBuildVueProjectIfNeeded(codeGenType, fromDir);
            tryBuildVueProjectIfNeeded(codeGenType, toDir);
            try {
                diffVO.setFromSnapshotUrl(screenshotService.generateAndUploadScreenshot(diffVO.getFromPreviewUrl()));
            } catch (Exception e) {
                log.warn("生成起始版本截图失败: {}", e.getMessage());
            }
            try {
                diffVO.setToSnapshotUrl(screenshotService.generateAndUploadScreenshot(diffVO.getToPreviewUrl()));
            } catch (Exception e) {
                log.warn("生成目标版本截图失败: {}", e.getMessage());
            }
        }
        return diffVO;
    }

    @Override
    public List<AppVersionFileDiffVO> diffFiles(String baseDir, String targetDir) {
        Map<String, File> baseFiles = listFiles(baseDir);
        Map<String, File> targetFiles = listFiles(targetDir);

        Set<String> allPaths = new HashSet<>();
        allPaths.addAll(baseFiles.keySet());
        allPaths.addAll(targetFiles.keySet());

        List<AppVersionFileDiffVO> diffs = new ArrayList<>();
        for (String path : allPaths) {
            File baseFile = baseFiles.get(path);
            File targetFile = targetFiles.get(path);
            AppVersionFileDiffVO diffVO = new AppVersionFileDiffVO();
            diffVO.setPath(path);
            if (baseFile == null) {
                diffVO.setChangeType("added");
                fillFileInfo(diffVO, null, targetFile);
            } else if (targetFile == null) {
                diffVO.setChangeType("removed");
                fillFileInfo(diffVO, baseFile, null);
            } else {
                String baseSha = calcSha256(baseFile);
                String targetSha = calcSha256(targetFile);
                diffVO.setBeforeSha(baseSha);
                diffVO.setAfterSha(targetSha);
                if (Objects.equals(baseSha, targetSha)) {
                    diffVO.setChangeType("unchanged");
                } else {
                    diffVO.setChangeType("modified");
                }
                fillTextExcerpt(diffVO, baseFile, targetFile);
            }
            if (!"unchanged".equals(diffVO.getChangeType())) {
                diffs.add(diffVO);
            }
        }
        return diffs.stream()
                .sorted(Comparator.comparing(AppVersionFileDiffVO::getPath))
                .collect(Collectors.toList());
    }

    private void tryBuildVueProjectIfNeeded(CodeGenTypeEnum codeGenType, String projectDir) {
        if (codeGenType != CodeGenTypeEnum.VUE_PROJECT) {
            return;
        }
        File distDir = new File(projectDir, "dist");
        if (distDir.exists() && distDir.isDirectory()) {
            return;
        }
        boolean success = vueProjectBuilder.buildProject(projectDir);
        if (!success) {
            log.warn("Vue 版本构建失败，无法生成预览截图: {}", projectDir);
        }
    }

    private Map<String, File> listFiles(String rootDir) {
        if (StrUtil.isBlank(rootDir)) {
            return Map.of();
        }
        File root = new File(rootDir);
        if (!root.exists() || !root.isDirectory()) {
            return Map.of();
        }
        List<File> files = FileUtil.loopFiles(root, file -> file.isFile());
        Map<String, File> result = new HashMap<>();
        for (File file : files) {
            String relative = root.toPath().relativize(file.toPath()).toString().replace("\\", "/");
            if (shouldIgnorePath(relative)) {
                continue;
            }
            result.put(relative, file);
        }
        return result;
    }

    private boolean shouldIgnorePath(String relativePath) {
        return relativePath.startsWith("node_modules/")
                || relativePath.startsWith(".git/")
                || relativePath.startsWith(".idea/")
                || relativePath.startsWith(".vscode/")
                || relativePath.startsWith("dist/")
                || relativePath.endsWith(".log")
                || relativePath.endsWith(".tmp");
    }

    private void fillFileInfo(AppVersionFileDiffVO diffVO, File before, File after) {
        diffVO.setBeforeSha(before == null ? null : calcSha256(before));
        diffVO.setAfterSha(after == null ? null : calcSha256(after));
        fillTextExcerpt(diffVO, before, after);
    }

    private void fillTextExcerpt(AppVersionFileDiffVO diffVO, File before, File after) {
        if (before != null && isTextFile(before.getName())) {
            diffVO.setBeforeLines(countLines(before));
            diffVO.setBeforeExcerpt(readExcerpt(before, 2000));
        }
        if (after != null && isTextFile(after.getName())) {
            diffVO.setAfterLines(countLines(after));
            diffVO.setAfterExcerpt(readExcerpt(after, 2000));
        }
    }

    private boolean isTextFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".html")
                || lower.endsWith(".css")
                || lower.endsWith(".js")
                || lower.endsWith(".ts")
                || lower.endsWith(".vue")
                || lower.endsWith(".json")
                || lower.endsWith(".md")
                || lower.endsWith(".txt")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.endsWith(".xml")
                || lower.endsWith(".svg");
    }

    private String readExcerpt(File file, int maxChars) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[512];
            int total = 0;
            int read;
            while ((read = reader.read(buffer)) != -1 && total < maxChars) {
                int len = Math.min(read, maxChars - total);
                sb.append(buffer, 0, len);
                total += len;
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private int countLines(File file) {
        int lines = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                lines++;
            }
        } catch (Exception e) {
            return 0;
        }
        return lines;
    }

    private String calcSha256(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
