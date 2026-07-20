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
);

-- 文章表
CREATE TABLE IF NOT EXISTS blog_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content CLOB NOT NULL,
    summary VARCHAR(500),
    cover_image VARCHAR(255),
    author_id BIGINT NOT NULL,
    category VARCHAR(50),
    tags VARCHAR(200),
    view_count BIGINT DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    status INT DEFAULT 1,
    is_top INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0
);

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
    deleted INT DEFAULT 0
);

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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_article_author ON blog_article(author_id);
CREATE INDEX IF NOT EXISTS idx_article_category ON blog_article(category);
CREATE INDEX IF NOT EXISTS idx_article_create_time ON blog_article(create_time);
CREATE INDEX IF NOT EXISTS idx_comment_article ON blog_comment(article_id);
CREATE INDEX IF NOT EXISTS idx_comment_user ON blog_comment(user_id);
CREATE INDEX IF NOT EXISTS idx_operation_log_time ON blog_operation_log(create_time);
