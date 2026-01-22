SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 表结构: users (用户信息表)
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                        `name` VARCHAR(50) NOT NULL COMMENT '用户昵称',
                        `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '用户邮箱，用于登录',
                        `password_hash` VARCHAR(255) NOT NULL COMMENT '哈希后的密码',
                        `avatar_url` VARCHAR(512) DEFAULT NULL COMMENT '用户头像URL',
                        `role` ENUM('user', 'admin') NOT NULL DEFAULT 'user' COMMENT '用户角色: user-普通用户, admin-管理员',
                        `status` ENUM('active', 'disabled') NOT NULL DEFAULT 'active' COMMENT '账户状态: active-活跃, disabled-禁用',
                        `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        INDEX `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- ----------------------------
-- 表结构: categories (分类表)
-- ----------------------------
DROP TABLE IF EXISTS `categorie`;
CREATE TABLE `categorie` (
                             `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分类ID',
                             `name` VARCHAR(100) NOT NULL UNIQUE COMMENT '分类名称',
                             `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
                             `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点评分类表';

-- ----------------------------
-- 表结构: items (点评项表)
-- ----------------------------
DROP TABLE IF EXISTS `item`;
CREATE TABLE `item` (
                        `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点评项ID',
                        `category_id` BIGINT UNSIGNED NOT NULL COMMENT '所属分类ID (逻辑外键)',
                        `name` VARCHAR(255) NOT NULL COMMENT '点评项名称',
                        `image_url` VARCHAR(512) DEFAULT NULL COMMENT '图片URL',
                        `description` TEXT COMMENT '详细描述',
                        `metadata` JSON DEFAULT NULL COMMENT '其他元数据 (例如: {"teacher": "李教授", "credits": 3})',
                        `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        INDEX `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点评项信息表';

-- ----------------------------
-- 表结构: reviews (评论表)
-- ----------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
                          `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
                          `user_id` BIGINT UNSIGNED NOT NULL COMMENT '发表评论的用户ID (逻辑外键)',
                          `item_id` BIGINT UNSIGNED NOT NULL COMMENT '被评论的点评项ID (逻辑外键)',
                          `score` TINYINT UNSIGNED NOT NULL COMMENT '评分 (1-10)',
                          `content` TEXT COMMENT '评论内容',
                          `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`id`),
                          INDEX `idx_user_id` (`user_id`),
                          INDEX `idx_item_id` (`item_id`),
                          UNIQUE KEY `uk_user_item` (`user_id`, `item_id`) COMMENT '确保一个用户对一个项目只能评论一次',
                          CONSTRAINT `chk_score` CHECK (`score` >= 1 AND `score` <= 10)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户评论表';