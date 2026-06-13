package com.ssafy.revibek.embedding.service;

public interface SongEmbeddingService {

    /**
     * 임베딩 파일이 없는 곡에 대해 GMS 임베딩 API를 호출해
     * song_embeddings/{songId}.json 으로 저장한다.
     *
     * @return 새로 생성된 임베딩 파일 수
     */
    int generateEmbeddings();
}
