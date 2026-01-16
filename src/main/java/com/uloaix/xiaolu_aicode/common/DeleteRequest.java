package com.uloaix.xiaolu_aicode.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求封装
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
