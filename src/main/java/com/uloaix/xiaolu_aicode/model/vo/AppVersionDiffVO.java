package com.uloaix.xiaolu_aicode.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 版本对比视图对象
 */
@Data
public class AppVersionDiffVO implements Serializable {

    private Long appId;
    private Integer fromVersion;
    private Integer toVersion;
    private String codeGenType;
    private String fromPreviewUrl;
    private String toPreviewUrl;
    private String fromSnapshotUrl;
    private String toSnapshotUrl;
    private List<AppVersionFileDiffVO> fileDiffs;

    private static final long serialVersionUID = 1L;
}
