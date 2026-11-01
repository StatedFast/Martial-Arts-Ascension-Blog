-- ==================== 表结构（MySQL 8.x，幂等脚本） ====================

-- 用户表
CREATE TABLE IF NOT EXISTS blog_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    avatar VARCHAR(255) DEFAULT '/images/default-avatar.png',
    bio VARCHAR(500),
    role VARCHAR(20) DEFAULT 'USER',
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文章表
CREATE TABLE IF NOT EXISTS blog_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT NOT NULL,
    summary VARCHAR(500),
    cover_image VARCHAR(255),
    author_id BIGINT NOT NULL,
    category VARCHAR(50),
    tags VARCHAR(200),
    view_count BIGINT DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    status INT DEFAULT 1,
    is_top INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_article_author (author_id),
    KEY idx_article_category (category),
    KEY idx_article_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 评论表
CREATE TABLE IF NOT EXISTS blog_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    reply_to_user_id BIGINT,
    content VARCHAR(1000) NOT NULL,
    like_count BIGINT DEFAULT 0,
    status INT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    KEY idx_comment_article (article_id),
    KEY idx_comment_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 操作日志表
CREATE TABLE IF NOT EXISTS blog_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(100),
    operation VARCHAR(100),
    method VARCHAR(10),
    url VARCHAR(500),
    ip VARCHAR(50),
    params VARCHAR(1000),
    result VARCHAR(2000),
    duration BIGINT,
    status INT DEFAULT 1,
    error_msg VARCHAR(1000),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 互动关系表（点赞 / 收藏 / 评论点赞） ====================
-- 说明：这三张表刻意不加 deleted 逻辑删除字段。
-- 取消点赞/收藏走物理删除，否则逻辑删除的残留行会被唯一索引挡住，导致“取消后再点赞”失败。

-- 文章点赞：唯一索引既是幂等保障，也是防刷手段
CREATE TABLE IF NOT EXISTS blog_article_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_like_article_user (article_id, user_id),
    KEY idx_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 文章收藏
CREATE TABLE IF NOT EXISTS blog_article_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_fav_article_user (article_id, user_id),
    KEY idx_fav_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 评论点赞
CREATE TABLE IF NOT EXISTS blog_comment_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_like_user (comment_id, user_id),
    KEY idx_comment_like_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 站内通知 ====================
-- content 存摘要快照，通知列表页无需回表关联文章/评论，避免 N+1
CREATE TABLE IF NOT EXISTS blog_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收者',
    sender_id BIGINT COMMENT '触发者，SYSTEM 类型为空',
    type VARCHAR(20) NOT NULL COMMENT 'LIKE/FAVORITE/COMMENT/REPLY/MENTION/SYSTEM',
    article_id BIGINT,
    comment_id BIGINT,
    content VARCHAR(300),
    is_read TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notify_unread (user_id, is_read, create_time),
    KEY idx_notify_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 系统配置中心（k-v） ====================
-- 后台「配置中心」读写，兼作故障演练开关（如内存泄漏注入）。默认全部关闭。
CREATE TABLE IF NOT EXISTS blog_sys_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value VARCHAR(500),
    description VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==================== 存量库升级：blog_article 增加 favorite_count ====================
-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS，这里用 information_schema 判断 + 动态 SQL 保证幂等
SET @fav_col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'blog_article'
      AND COLUMN_NAME = 'favorite_count'
);
SET @fav_ddl := IF(@fav_col_exists = 0,
    'ALTER TABLE blog_article ADD COLUMN favorite_count BIGINT DEFAULT 0',
    'SELECT 1');
PREPARE fav_stmt FROM @fav_ddl;
EXECUTE fav_stmt;
DEALLOCATE PREPARE fav_stmt;

-- 文章列表按 status + is_top + create_time 排序的复合索引（原索引只有单列）
SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'blog_article'
      AND INDEX_NAME = 'idx_article_list'
);
SET @idx_ddl := IF(@idx_exists = 0,
    'ALTER TABLE blog_article ADD INDEX idx_article_list (status, is_top, create_time)',
    'SELECT 1');
PREPARE idx_stmt FROM @idx_ddl;
EXECUTE idx_stmt;
DEALLOCATE PREPARE idx_stmt;
