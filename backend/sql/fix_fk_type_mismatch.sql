USE weiboblog;

-- Ensure topics.owner_id column uses BIGINT to match users.id
SET @fk_topics_owner := (
    SELECT constraint_name
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'topics'
      AND referenced_table_name = 'users'
    LIMIT 1
);

SET @sql := IF(@fk_topics_owner IS NOT NULL,
               CONCAT('ALTER TABLE topics DROP FOREIGN KEY ', @fk_topics_owner),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE topics
    MODIFY owner_id BIGINT NOT NULL;

ALTER TABLE topics
    ADD CONSTRAINT fk_topics_owner
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE CASCADE;

-- Ensure topic_members.user_id column uses BIGINT to match users.id
SET @fk_topic_members_user := (
    SELECT constraint_name
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND table_name = 'topic_members'
      AND referenced_table_name = 'users'
    LIMIT 1
);

SET @sql := IF(@fk_topic_members_user IS NOT NULL,
               CONCAT('ALTER TABLE topic_members DROP FOREIGN KEY ', @fk_topic_members_user),
               'DO 0');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE topic_members
    MODIFY user_id BIGINT NOT NULL;

ALTER TABLE topic_members
    ADD CONSTRAINT fk_topic_members_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE;
