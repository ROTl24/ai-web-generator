package com.uloaix.xiaolu_aicode.model.enums;

import lombok.Getter;

@Getter
public enum BuildProgressStatusEnum {
    WAITING("waiting"),
    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed");

    private final String value;

    BuildProgressStatusEnum(String value) {
        this.value = value;
    }

    public static boolean isFinished(String status) {
        return SUCCESS.value.equals(status) || FAILED.value.equals(status);
    }
}
