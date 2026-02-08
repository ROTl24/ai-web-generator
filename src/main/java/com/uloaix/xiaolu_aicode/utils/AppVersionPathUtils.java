package com.uloaix.xiaolu_aicode.utils;

import com.uloaix.xiaolu_aicode.constant.AppConstant;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.file.Paths;

/**
 * 应用版本路径工具类
 */
public final class AppVersionPathUtils {

    private AppVersionPathUtils() {
    }

    /**
     * 获取应用基础目录（不含版本）
     */
    public static String buildBaseDir(CodeGenTypeEnum codeGenType, Long appId) {
        String dirName = codeGenType.getValue() + "_" + appId;
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, dirName).toString();
    }

    /**
     * 获取指定版本目录（v{version}）
     */
    public static String buildVersionDir(CodeGenTypeEnum codeGenType, Long appId, int version) {
        if (version <= 0) {
            return buildBaseDir(codeGenType, appId);
        }
        return Paths.get(buildBaseDir(codeGenType, appId), "v" + version).toString();
    }

    /**
     * 获取预览 URL（基于静态资源服务）
     */
    public static String buildPreviewUrl(CodeGenTypeEnum codeGenType, Long appId, int version) {
        String previewKey = codeGenType.getValue() + "_" + appId;
        return String.format("%s/%s/?version=%d", AppConstant.CODE_DEPLOY_HOST, previewKey, version);
    }

    /**
     * 判断版本目录是否存在
     */
    public static boolean versionDirExists(String versionDir) {
        return new File(versionDir).exists();
    }
}
