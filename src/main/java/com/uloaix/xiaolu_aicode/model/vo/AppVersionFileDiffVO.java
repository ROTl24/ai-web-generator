package com.uloaix.xiaolu_aicode.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 版本文件差异视图对象
 */
@Data
public class AppVersionFileDiffVO implements Serializable {

    private String path;
    private String changeType; // added/removed/modified/unchanged
    private String beforeSha;
    private String afterSha;
    private Integer beforeLines;
    private Integer afterLines;
    private String beforeExcerpt;
    private String afterExcerpt;

    private static final long serialVersionUID = 1L;
}
