package com.uloaix.xiaolu_aicode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用版本状态枚举
 */
@Getter
public enum AppVersionStatusEnum {

    GENERATING("生成中", "generating"),
    READY("可用", "ready"),
    FAILED("失败", "failed");

    private final String text;
    private final String value;

    AppVersionStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static AppVersionStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppVersionStatusEnum anEnum : AppVersionStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
