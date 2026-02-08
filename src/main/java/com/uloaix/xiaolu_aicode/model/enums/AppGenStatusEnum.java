package com.uloaix.xiaolu_aicode.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用生成状态枚举
 */
@Getter
public enum AppGenStatusEnum {

    NOT_GENERATED("未生成", "not_generated"),
    GENERATING("生成中", "generating"),
    READY("已完成", "ready"),
    FAILED("失败", "failed");

    private final String text;
    private final String value;

    AppGenStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static AppGenStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppGenStatusEnum anEnum : AppGenStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
