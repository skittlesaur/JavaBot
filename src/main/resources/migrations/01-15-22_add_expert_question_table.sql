CREATE TABLE expert_questions (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 guild_id BIGINT NOT NULL,
 text VARCHAR(2048) NOT NULL
);