package com.uloaix.xiaolu_aicode.service;

import com.mybatisflex.core.service.IService;
import com.uloaix.xiaolu_aicode.model.entity.AppVersion;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;
import com.uloaix.xiaolu_aicode.model.vo.AppVersionDiffVO;
import com.uloaix.xiaolu_aicode.model.vo.AppVersionFileDiffVO;

import java.util.List;

/**
 * 应用版本 服务层。
 */
public interface AppVersionService extends IService<AppVersion> {

    /**
     * 创建新版本（生成前调用）
     */
    AppVersion createNewVersion(Long appId, CodeGenTypeEnum codeGenType, Long userId);

    /**
     * 标记版本生成成功
     */
    void markVersionReady(Long appId, Integer version);

    /**
     * 标记版本生成失败
     */
    void markVersionFailed(Long appId, Integer version, String reason);

    /**
     * 获取应用当前版本号（无版本返回 0）
     */
    int getCurrentVersion(Long appId);

    /**
     * 解析当前生效版本号（优先使用生成中的版本）
     */
    int resolveActiveVersion(Long appId);

    /**
     * 获取版本代码目录（不保证存在）
     */
    String buildVersionDir(CodeGenTypeEnum codeGenType, Long appId, int version);

    /**
     * 获取当前生效的代码目录（优先生成中的版本）
     */
    String resolveActiveVersionDir(CodeGenTypeEnum codeGenType, Long appId);

    /**
     * 回滚到指定版本
     */
    boolean rollbackToVersion(Long appId, int version);

    /**
     * 获取版本列表
     */
    List<AppVersion> listVersions(Long appId);

    /**
     * 版本对比（文件级）
     */
    AppVersionDiffVO diffVersions(Long appId, int fromVersion, int toVersion, boolean withScreenshot);

    /**
     * 计算文件差异
     */
    List<AppVersionFileDiffVO> diffFiles(String baseDir, String targetDir);
}
