FROM python:3.13-slim

# yt-dlp가 다운로드한 오디오를 WAV로 추출하는 데 ffmpeg가 필요 (requirements.txt에는 없음)
# yt-dlp가 유튜브 서명 해독(JS 챌린지)에 JS 런타임을 요구하므로 nodejs도 설치
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

# downloads/, song_embeddings/는 compose에서 볼륨으로 덮어씀 — 빈 디렉터리만 보장
RUN mkdir -p downloads song_embeddings

EXPOSE 8000

# --workers 2: analyze_music()이 동기 I/O를 블로킹 실행하므로 프로세스 병렬화로 동시 요청을 완화
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
