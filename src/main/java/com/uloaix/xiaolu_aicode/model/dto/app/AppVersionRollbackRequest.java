package com.uloaix.xiaolu_aicode.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用版本回滚请求
 */
@Data
public class AppVersionRollbackRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 目标版本号
     */
    private Integer version;

    private static final long serialVersionUID = 1L;
}
