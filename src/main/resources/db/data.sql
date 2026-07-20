-- 初始化测试数据
-- 密码: 启动时自动迁移为BCrypt，默认登录密码: Abc@1234
INSERT INTO blog_user (username, password, nickname, email, role, status) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', 'admin@blog.com', 'ADMIN', 1),
('zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '张三', 'zhangsan@blog.com', 'USER', 1),
('lisi', 'e10adc3949ba59abbe56e057f20f883e', '李四', 'lisi@blog.com', 'USER', 1);

-- 初始化文章
INSERT INTO blog_article (title, content, summary, author_id, category, tags, view_count, comment_count, like_count, status) VALUES
('Spring Boot 4.0 新特性详解', 'Spring Boot 4.0 带来了许多激动人心的新特性。本文将详细介绍最重要的更新内容，包括对 Java 21 虚拟线程的支持、全新的可观测性API、以及改进的GraalVM原生镜像支持等。首先，虚拟线程（Project Loom）的支持让Spring应用在高并发场景下的性能得到了质的飞跃。...', 'Spring Boot 4.0 带来了虚拟线程、可观测性API等重磅新特性', 1, 'Spring', 'SpringBoot,Java,后端', 1250, 15, 88, 1),

('JWT认证在Spring项目中的最佳实践', 'JSON Web Token（JWT）已经成为现代Web应用中主流的认证方案。本文将分享在Spring Boot项目中集成JWT的最佳实践，包括Token的生成与验证、刷新Token机制、黑名单处理等。我们将从安全性、性能和易用性三个维度来设计一个完善的JWT认证方案。...', '全面讲解JWT在Spring Boot中的集成与最佳实践', 1, 'Spring', 'JWT,安全,认证', 980, 12, 56, 1),

('Redis缓存策略：从入门到精通', 'Redis作为最流行的缓存中间件，合理使用可以极大提升系统性能。本文将从缓存穿透、缓存击穿、缓存雪崩三大经典问题入手，深入剖析各种缓存策略的原理与实现，包括旁路缓存、读写穿透、异步写回等模式。同时介绍Redis在博客系统中的实际应用场景。...', '深入探讨Redis缓存的各种策略与高并发场景下的应用', 2, '数据库', 'Redis,缓存,高并发', 2100, 25, 120, 1),

('MyBatis-Plus 高效开发指南', 'MyBatis-Plus 是一款优秀的MyBatis增强工具，在其基础上只做增强不做改变，为简化开发、提高效率而生。本文详细介绍其核心功能：通用CRUD、条件构造器、分页插件、逻辑删除、自动填充等。...', '掌握MyBatis-Plus核心特性，提升开发效率', 2, '数据库', 'MyBatis,ORM,Java', 760, 8, 45, 1),

('构建高并发博客系统架构设计', '如何设计一个能够支撑高并发的博客系统？本文从整体架构出发，涵盖负载均衡、数据库优化、缓存设计、异步处理、限流降级等多个方面。采用Spring Boot + MyBatis-Plus + Redis的技术栈，配合合理的架构设计，轻松应对十万级并发访问。...', '从架构设计到代码实现，全方位构建高并发博客系统', 3, '架构', '架构,高并发,性能优化', 3100, 35, 200, 1);

-- 初始化评论
INSERT INTO blog_comment (article_id, user_id, parent_id, content, status) VALUES
(1, 2, 0, '写得太好了，非常详细！', 1),
(1, 3, 0, 'Spring Boot 4.0 的虚拟线程确实是一个亮点', 1),
(1, 1, 2, '感谢支持，后续会出更多Spring相关文章', 1),
(3, 1, 0, 'Redis缓存策略讲得很透彻，收获很大', 1),
(5, 2, 0, '架构设计思路很清晰，学习了', 1),
(5, 3, 0, '高并发这块确实需要多实践', 1);
