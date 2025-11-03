-- -----------------------------------------------------
-- Schema for the Weibo-like personal blog system
-- -----------------------------------------------------

CREATE DATABASE IF NOT EXISTS weiboblog
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE weiboblog;

-- -----------------------------------------------------
-- Table `users`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    bio VARCHAR(280),
    signature VARCHAR(280),
    location VARCHAR(120),
    avatar_url VARCHAR(255),
    privacy_setting VARCHAR(32) NOT NULL DEFAULT 'PUBLIC',
    is_admin TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `topics`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS topics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(280),
    owner_id BIGINT NOT NULL,
    heat BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_topics_name UNIQUE (name),
    KEY idx_topics_owner_id (owner_id),
    KEY idx_topics_heat (heat)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `topic_members`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS topic_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_topic_members UNIQUE (topic_id, user_id),
    KEY idx_topic_members_topic_id (topic_id),
    KEY idx_topic_members_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `posts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT NOT NULL,
    topic_id BIGINT NULL,
    content VARCHAR(500) NOT NULL,
    visibility VARCHAR(32) NOT NULL DEFAULT 'PUBLIC',
    heat BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    KEY idx_posts_author_id (author_id),
    KEY idx_posts_topic_id (topic_id),
    KEY idx_posts_created_at (created_at),
    KEY idx_posts_heat (heat)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `post_media`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS post_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    media_url VARCHAR(255) NOT NULL,
    KEY idx_post_media_post_id (post_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `post_visibility_allow`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS post_visibility_allow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    allowed_user_id BIGINT NOT NULL,
    KEY idx_post_visibility_post_id (post_id),
    KEY idx_post_visibility_user_id (allowed_user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `comments`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(280) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY idx_comments_post_id_created_at (post_id, created_at),
    KEY idx_comments_author_id (author_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `post_likes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_post_likes_post_user
        UNIQUE (post_id, user_id),
    KEY idx_post_likes_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `follows`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_follows_follower_followee
        UNIQUE (follower_id, followee_id),
    CONSTRAINT chk_follows_not_self
        CHECK (follower_id <> followee_id),
    KEY idx_follows_followee_id (followee_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
