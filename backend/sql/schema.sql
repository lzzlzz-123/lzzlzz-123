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
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    bio VARCHAR(280),
    signature VARCHAR(280),
    location VARCHAR(120),
    avatar_url VARCHAR(255),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `posts`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    author_id BIGINT UNSIGNED NOT NULL,
    content VARCHAR(500) NOT NULL,
    heat BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_posts_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE CASCADE,
    KEY idx_posts_author_id (author_id),
    KEY idx_posts_created_at (created_at),
    KEY idx_posts_heat (heat)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `post_media`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS post_media (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT UNSIGNED NOT NULL,
    media_url VARCHAR(255) NOT NULL,
    CONSTRAINT fk_post_media_post
        FOREIGN KEY (post_id) REFERENCES posts (id)
        ON DELETE CASCADE,
    KEY idx_post_media_post_id (post_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `comments`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT UNSIGNED NOT NULL,
    author_id BIGINT UNSIGNED NOT NULL,
    content VARCHAR(280) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_comments_post
        FOREIGN KEY (post_id) REFERENCES posts (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_comments_author
        FOREIGN KEY (author_id) REFERENCES users (id)
        ON DELETE CASCADE,
    KEY idx_comments_post_id_created_at (post_id, created_at),
    KEY idx_comments_author_id (author_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `post_likes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS post_likes (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_post_likes_post_user
        UNIQUE (post_id, user_id),
    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id) REFERENCES posts (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_post_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    KEY idx_post_likes_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- -----------------------------------------------------
-- Table `follows`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS follows (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT UNSIGNED NOT NULL,
    followee_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_follows_follower_followee
        UNIQUE (follower_id, followee_id),
    CONSTRAINT fk_follows_follower
        FOREIGN KEY (follower_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_follows_followee
        FOREIGN KEY (followee_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_follows_not_self
        CHECK (follower_id <> followee_id),
    KEY idx_follows_followee_id (followee_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
