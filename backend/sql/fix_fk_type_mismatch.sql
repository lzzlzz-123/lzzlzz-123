USE weiboblog;

-- Convert physical foreign keys to logical references while ensuring column types remain aligned.

-- Drop foreign key on topics.owner_id referencing users.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'topics'
      AND column_name = 'owner_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE topics DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE topics
    MODIFY owner_id BIGINT NOT NULL;

-- Drop foreign key on topic_members.topic_id referencing topics.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'topic_members'
      AND column_name = 'topic_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE topic_members DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on topic_members.user_id referencing users.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'topic_members'
      AND column_name = 'user_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE topic_members DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE topic_members
    MODIFY topic_id BIGINT NOT NULL,
    MODIFY user_id BIGINT NOT NULL;

-- Drop foreign key on posts.author_id referencing users.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'posts'
      AND column_name = 'author_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE posts DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on posts.topic_id referencing topics.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'posts'
      AND column_name = 'topic_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE posts DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on post_media.post_id referencing posts.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'post_media'
      AND column_name = 'post_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE post_media DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on comments.post_id referencing posts.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comments'
      AND column_name = 'post_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE comments DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on comments.author_id referencing users.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'comments'
      AND column_name = 'author_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE comments DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on post_likes.post_id referencing posts.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'post_likes'
      AND column_name = 'post_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE post_likes DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on post_likes.user_id referencing users.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'post_likes'
      AND column_name = 'user_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE post_likes DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on follows.follower_id referencing users.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'follows'
      AND column_name = 'follower_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE follows DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Drop foreign key on follows.followee_id referencing users.id
SET @fk_name := (
    SELECT constraint_name
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE constraint_schema = DATABASE()
      AND table_name = 'follows'
      AND column_name = 'followee_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);
SET @sql := IF(@fk_name IS NOT NULL,
               CONCAT('ALTER TABLE follows DROP FOREIGN KEY ', @fk_name),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
