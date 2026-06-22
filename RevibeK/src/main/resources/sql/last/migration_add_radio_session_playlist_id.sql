ALTER TABLE radio_sessions
  ADD COLUMN playlist_id CHAR(36) NULL;

ALTER TABLE radio_sessions
  ADD CONSTRAINT fk_radio_sessions_playlist
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE SET NULL;
