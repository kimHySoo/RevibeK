package com.ssafy.revibek.qdrant;

import com.ssafy.revibek.song.dto.SongDto;
import com.ssafy.revibek.song.service.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qdrant")
@Tag(name = "Qdrant", description = "벡터 DB 관리 API")
@RequiredArgsConstructor
public class QdrantController {

    private final QdrantService qdrantService;
    private final SongService songService;

    @PostMapping("/embed")
    @Operation(summary = "전체 곡 벡터 저장", description = "songs 테이블 전체를 Qdrant에 upsert")
    public ResponseEntity<String> embedAll() {
        qdrantService.createCollectionIfNotExists();
        List<SongDto> songs = songService.getAllSongs();
        qdrantService.upsertSongs(songs);
        return ResponseEntity.ok(songs.size() + "곡 Qdrant 저장 완료");
    }

    @GetMapping("/similar/{songId}")
    @Operation(summary = "유사곡 조회", description = "특정 곡과 유사한 곡 N개 반환")
    public ResponseEntity<List<SongDto>> getSimilar(
            @PathVariable String songId,
            @RequestParam(defaultValue = "10") int limit) {

        List<String> similarIds = qdrantService.searchSimilar(songId, limit);
        List<SongDto> songs = similarIds.stream()
            .map(songService::getSongById)
            .toList();
        return ResponseEntity.ok(songs);
    }
}
