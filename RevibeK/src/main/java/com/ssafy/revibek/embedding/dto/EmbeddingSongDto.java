package com.ssafy.revibek.embedding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingSongDto {

    private String id;
    private String songId;
    private String embeddingType;
    private String modelName;
    private int dimension;
    private String sourceText;
    private List<Float> vector;
    private String qdrantCollection;
    private String qdrantPointId;
    private boolean isIndexed;
    private LocalDateTime indexedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
