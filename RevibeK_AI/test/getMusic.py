import mysql.connector
import yt_dlp
from pathlib import Path

# 저장 폴더 생성
SAVE_DIR = Path("downloads")
SAVE_DIR.mkdir(exist_ok=True)

# DB 연결
conn = mysql.connector.connect(
    host="127.0.0.1",
    port=3306,
    user="ssafy",
    password="ssafy",
    database="kpop_radio",
    use_pure=True
)

cursor = conn.cursor(dictionary=True)

# video_id 3개 조회
cursor.execute("""
SELECT video_id
FROM youtube_videos_raw
WHERE video_id IS NOT NULL
LIMIT 3
""")

rows = cursor.fetchall()

print("조회된 개수:", len(rows))

for row in rows:
    video_id = row["video_id"]

    url = f"https://www.youtube.com/watch?v={video_id}"

    print(f"\n다운로드 시작: {url}")

    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": str(SAVE_DIR / f"{video_id}.%(ext)s"),
        "noplaylist": True,

        # wav 변환
        "postprocessors": [{
            "key": "FFmpegExtractAudio",
            "preferredcodec": "wav",
        }],
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            ydl.download([url])

        print(f"다운로드 완료: {video_id}")

    except Exception as e:
        print(f"다운로드 실패: {video_id}")
        print(e)

cursor.close()
conn.close()

print("\n모든 작업 완료")
print("저장 위치:", SAVE_DIR.resolve())