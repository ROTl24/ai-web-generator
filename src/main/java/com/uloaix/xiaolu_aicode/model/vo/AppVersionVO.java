package com.uloaix.xiaolu_aicode.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用版本视图对象
 */
@Data
public class AppVersionVO implements Serializable {

    private Long id;
    private Long appId;
    private Integer version;
    private String codeGenType;
    private String codeDir;
    private String status;
    private String snapshotUrl;
    private Long createdBy;
    private LocalDateTime createTime;

    private static final long serialVersionUID = 1L;
}
