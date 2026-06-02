DROP TABLE IF EXISTS youtube_videos_raw;
DROP TABLE IF EXISTS youtube_channels;

CREATE TABLE youtube_channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_id VARCHAR(100) NOT NULL UNIQUE,
    channel_name VARCHAR(255) NOT NULL,
    channel_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_checked_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE youtube_videos_raw (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    youtube_channel_id BIGINT NOT NULL,
    video_id VARCHAR(100) NOT NULL UNIQUE,
    video_url VARCHAR(500) NOT NULL,
    video_title VARCHAR(500) NOT NULL,
    duration_seconds INT,
    published_at DATETIME,
    collect_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_youtube_videos_raw_channel
        FOREIGN KEY (youtube_channel_id)
        REFERENCES youtube_channels(id)
        ON DELETE CASCADE
);