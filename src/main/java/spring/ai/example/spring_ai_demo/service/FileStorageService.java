package spring.ai.example.spring_ai_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import spring.ai.example.spring_ai_demo.exception.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件存储服务（头像图片上传）
 *
 * 安全设计：
 * - 扩展名白名单 + Content-Type 必须是 image/*
 * - 文件名使用 UUID 重新生成，不使用用户原始文件名（防路径穿越/同名覆盖）
 * - 大小限制通过 file.avatar-max-size 配置
 * - 删除旧图时校验最终路径仍在托管目录内
 */
@Service
@Slf4j
public class FileStorageService {

    /** 头像对外访问URL前缀（与 WebMvcConfig 的 /uploads/** 资源映射对应） */
    public static final String AVATAR_URL_PREFIX = "/uploads/avatars/";

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /** 头像存放子目录 */
    private static final String AVATAR_SUB_DIR = "avatars";

    private final Path uploadRoot;
    private final DataSize avatarMaxSize;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir,
                              @Value("${file.avatar-max-size:2MB}") DataSize avatarMaxSize) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.avatarMaxSize = avatarMaxSize;
        log.info("文件存储根目录: {}, 头像大小上限: {}", uploadRoot, avatarMaxSize);
    }

    /**
     * 保存头像图片，返回可访问URL（如 /uploads/avatars/avatar_1_9a8b7c6d.png）
     */
    public String storeAvatar(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择图片文件");
        }
        if (file.getSize() > avatarMaxSize.toBytes()) {
            throw new BusinessException(400, "图片大小不能超过" + avatarMaxSize.toKilobytes() + "KB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(400, "仅能上传图片文件");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(400, "仅支持 jpg/jpeg/png/gif/webp/bmp 格式图片");
        }

        String filename = "avatar_" + userId + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + extension;
        Path target = uploadRoot.resolve(AVATAR_SUB_DIR).resolve(filename);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("头像保存失败: userId={}, target={}", userId, target, e);
            throw new BusinessException(500, "图片保存失败，请稍后重试");
        }

        log.info("头像上传成功: userId={}, file={}, size={}KB", userId, filename, file.getSize() / 1024);
        return AVATAR_URL_PREFIX + filename;
    }

    /**
     * 更换头像后清理旧头像文件
     * 仅删除本服务托管目录下的文件，默认头像/外链URL自动忽略；删除失败不影响业务
     */
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(AVATAR_URL_PREFIX)) {
            return;
        }
        Path file = uploadRoot.resolve(AVATAR_SUB_DIR)
                .resolve(avatarUrl.substring(AVATAR_URL_PREFIX.length())).normalize();
        // 防御：归一化后必须仍在托管目录内（防 ../ 路径穿越）
        if (!file.startsWith(uploadRoot)) {
            log.warn("拒绝删除目录外路径: {}", file);
            return;
        }
        try {
            Files.deleteIfExists(file);
            log.debug("旧头像已清理: {}", avatarUrl);
        } catch (IOException e) {
            log.warn("旧头像清理失败: {}", avatarUrl, e);
        }
    }

    /**
     * 从原始文件名中提取安全的扩展名
     */
    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
