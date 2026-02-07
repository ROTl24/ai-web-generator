package com.uloaix.xiaolu_aicode.model.dto.build;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildProgressEvent {

    /**
     * 状态：waiting / running / success / failed
     */
    private String status;

    /**
     * 阶段标识（可选）
     */
    private String step;

    /**
     * 进度百分比（0-100）
     */
    private Integer percent;

    /**
     * 用户可读的提示信息
     */
    private String message;
}
