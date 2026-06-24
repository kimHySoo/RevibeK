import json
import logging
import os
from contextlib import contextmanager
from typing import List, Optional

import mysql.connector

logger = logging.getLogger(__name__)

# Spring Boot(application.properties)와 동일한 DB를 바라본다.
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_NAME = os.getenv("DB_NAME", "kpop_radio_2")
DB_USERNAME = os.getenv("DB_USERNAME", "SSAFY")
DB_PASSWORD = os.getenv("DB_PASSWORD", "SSAFY")

AUDIO_EMBEDDING_TYPE = "AUDIO_9D"


@contextmanager
def _connection():
    conn = mysql.connector.connect(
        host=DB_HOST,
        port=DB_PORT,
        database=DB_NAME,
        user=DB_USERNAME,
        password=DB_PASSWORD,
    )
    try:
        yield conn
    finally:
        conn.close()


def get_audio_embedding(youtube_video_id: str) -> Optional[List[float]]:
    """
    songs.youtube_id로 곡을 찾아 embedding_songs(AUDIO_9D)에 저장된 벡터를 반환한다.
    곡/벡터가 없거나 DB 연결에 실패하면 None을 반환해 분석을 다시 진행하게 한다
    (중복 분석은 비용 문제일 뿐 데이터 오류는 아님).
    """
    try:
        with _connection() as conn:
            cursor = conn.cursor()
            cursor.execute(
                "SELECT id FROM songs WHERE youtube_id = %s",
                (youtube_video_id,),
            )
            row = cursor.fetchone()
            if row is None:
                return None
            song_id = row[0]

            cursor.execute(
                "SELECT vector FROM embedding_songs WHERE song_id = %s AND embedding_type = %s LIMIT 1",
                (song_id, AUDIO_EMBEDDING_TYPE),
            )
            vector_row = cursor.fetchone()
            if vector_row is None:
                return None

            vector = vector_row[0]
            return json.loads(vector) if isinstance(vector, str) else vector
    except mysql.connector.Error as e:
        logger.warning("embedding_songs 조회 실패, 분석을 계속 진행합니다: %s", e)
        return None
