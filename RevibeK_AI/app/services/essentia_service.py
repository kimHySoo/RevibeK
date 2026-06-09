import os
import librosa
import numpy as np


# Krumhansl-Kessler 키 프로파일 (조성 탐지)
_MAJOR_PROFILE = np.array([6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88])
_MINOR_PROFILE = np.array([6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17])
_KEY_NAMES = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']


def _detect_key(chroma_mean: np.ndarray) -> tuple[str, str, float]:
    """크로마그램 평균으로 키, 스케일, 강도를 반환한다."""
    major_corrs = [np.corrcoef(np.roll(chroma_mean, -i), _MAJOR_PROFILE)[0, 1] for i in range(12)]
    minor_corrs = [np.corrcoef(np.roll(chroma_mean, -i), _MINOR_PROFILE)[0, 1] for i in range(12)]

    best_major_idx = int(np.argmax(major_corrs))
    best_minor_idx = int(np.argmax(minor_corrs))

    if major_corrs[best_major_idx] >= minor_corrs[best_minor_idx]:
        return _KEY_NAMES[best_major_idx], "major", float(major_corrs[best_major_idx])
    else:
        return _KEY_NAMES[best_minor_idx], "minor", float(minor_corrs[best_minor_idx])


def analyze_audio(audio_path: str) -> dict:
    """
    오디오 파일을 librosa로 분석해 음악 feature를 반환한다.

    반환 키:
        duration_seconds (int)   : 실제 재생 시간(초)
        bpm              (float) : 템포 (BPM)
        energy           (float) : 에너지 [0, 1]
        danceability     (float) : 댄서빌리티 [0, 1] (tempo + beat strength 추정)
        loudness         (float) : 라우드니스 (dB, 음수)
        musical_key      (str)   : 음악 키 (예: "C")
        musical_scale    (str)   : 음악 스케일 ("major" | "minor")
        essentia_features (dict) : 전체 분석 결과
    """
    if not os.path.exists(audio_path):
        raise RuntimeError(f"오디오 파일이 존재하지 않습니다: {audio_path}")

    try:
        # ── 1. 오디오 로드 (44100 Hz 모노) ────────────────────────────────────
        audio, sr = librosa.load(audio_path, sr=44100, mono=True)
        duration = librosa.get_duration(y=audio, sr=sr)

        # ── 2. BPM / 비트 분석 ────────────────────────────────────────────────
        tempo, beats = librosa.beat.beat_track(y=audio, sr=sr)
        bpm = float(tempo[0] if hasattr(tempo, '__len__') else tempo)
        beats_count = int(len(beats))

        onset_env = librosa.onset.onset_strength(y=audio, sr=sr)
        beats_confidence = float(
            np.mean(onset_env[beats]) / (np.max(onset_env) + 1e-8)
        ) if beats_count > 0 else 0.0

        # ── 3. 키 / 스케일 분석 ───────────────────────────────────────────────
        chroma = librosa.feature.chroma_cqt(y=audio, sr=sr)
        musical_key, musical_scale, key_strength = _detect_key(np.mean(chroma, axis=1))

        # ── 4. 에너지 (mean squared amplitude) ───────────────────────────────
        energy = float(min(np.mean(audio ** 2), 1.0))

        # ── 5. 라우드니스 (dB) ────────────────────────────────────────────────
        rms = float(np.sqrt(np.mean(audio ** 2)))
        loudness = float(librosa.amplitude_to_db(np.array([rms]), ref=1.0)[0])

        # ── 6. 댄서빌리티 (tempo + beat strength 기반 추정) ──────────────────
        tempo_norm = min(bpm / 200.0, 1.0)
        danceability = round((tempo_norm + beats_confidence) / 2.0, 4)

        # ── 7. 스펙트럴 센트로이드 ────────────────────────────────────────────
        spectral_centroid = float(np.mean(librosa.feature.spectral_centroid(y=audio, sr=sr)))

        # ── 8. 영교차율 ───────────────────────────────────────────────────────
        zero_crossing_rate = float(np.mean(librosa.feature.zero_crossing_rate(audio)))

        essentia_features = {
            "duration": float(duration),
            "bpm": round(bpm, 4),
            "beats_count": beats_count,
            "beats_confidence": round(beats_confidence, 4),
            "key": musical_key,
            "scale": musical_scale,
            "key_strength": round(key_strength, 4),
            "energy": round(energy, 4),
            "loudness_db": round(loudness, 4),
            "danceability": round(danceability, 4),
            "spectral_centroid": round(spectral_centroid, 4),
            "zero_crossing_rate": round(zero_crossing_rate, 6),
        }

        return {
            "duration_seconds": int(duration),
            "bpm": round(bpm, 4),
            "energy": round(energy, 4),
            "danceability": round(danceability, 4),
            "loudness": round(loudness, 4),
            "musical_key": musical_key,
            "musical_scale": musical_scale,
            "essentia_features": essentia_features,
        }

    except RuntimeError:
        raise
    except Exception as e:
        raise RuntimeError(f"librosa 분석 중 오류 발생 ({audio_path}): {e}")
