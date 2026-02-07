package com.uloaix.xiaolu_aicode.exception;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.uloaix.xiaolu_aicode.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Object businessExceptionHandler(BusinessException e, HttpServletRequest request) {
        log.error("BusinessException", e);
        if (isSseRequest(request)) {
            return sseError(e.getCode(), e.getMessage());
        }
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Object runtimeExceptionHandler(RuntimeException e, HttpServletRequest request) {
        log.error("RuntimeException", e);
        if (isSseRequest(request)) {
            return sseError(ErrorCode.SYSTEM_ERROR.getCode(), "系统错误");
        }
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }

    private static boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        if (StrUtil.isNotBlank(accept) && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            return true;
        }
        String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
        if (StrUtil.isNotBlank(contentType) && contentType.contains(MediaType.TEXT_EVENT_STREAM_VALUE)) {
            return true;
        }
        // 兜底：按路径规避（避免 SSE 端点异常时返回 BaseResponse 触发 converter 报错）
        String uri = request.getRequestURI();
        return StrUtil.isNotBlank(uri) && uri.contains("/chat/gen/code");
    }

    /**
     * 给 SSE 请求返回单条 error 事件（避免 BaseResponse 在 text/event-stream 下触发 converter 异常）。
     * <p>
     * 注意：如果响应已提交（stream 已开始写入），此处仍可能无能为力，所以更推荐在 Controller/Flux 内 onErrorResume。
     */
    private static ResponseEntity<String> sseError(int code, String message) {
        String msg = StrUtil.blankToDefault(message, "系统错误");
        String data = JSONUtil.toJsonStr(Map.of(
                "code", String.valueOf(code),
                "message", msg
        ));
        String body = "event: error\n" +
                "data: " + data + "\n\n";
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }
}
