package com.uloaix.xiaolu_aicode.controller;

import com.uloaix.xiaolu_aicode.constant.AppConstant;
import com.uloaix.xiaolu_aicode.model.enums.CodeGenTypeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于浏览）
    private static final String PREVIEW_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            boolean isPreviewKey = false;
            for (CodeGenTypeEnum typeEnum : CodeGenTypeEnum.values()) {
                if (deployKey.startsWith(typeEnum.getValue() + "_")) {
                    isPreviewKey = true;
                    break;
                }
            }
            boolean isVuePreview = isPreviewKey && deployKey.startsWith(CodeGenTypeEnum.VUE_PROJECT.getValue() + "_");
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 兼容已携带 /dist 的访问路径（避免重复拼接）
            if (isVuePreview) {
                if ("/dist".equals(resourcePath) || "/dist/".equals(resourcePath)) {
                    resourcePath = "/";
                } else if (resourcePath.startsWith("/dist/")) {
                    resourcePath = resourcePath.substring("/dist".length());
                }
            }
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 构建基础目录，Vue 项目预览需要使用 dist 目录
            String baseDirPath;
            if (isPreviewKey) {
                baseDirPath = PREVIEW_ROOT_DIR + "/" + deployKey;
                if (isVuePreview) {
                    baseDirPath = baseDirPath + "/dist";
                }
            } else {
                baseDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + "/" + deployKey;
            }
            // 构建文件路径
            String filePath = baseDirPath + resourcePath;
            File file = new File(filePath);
            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
