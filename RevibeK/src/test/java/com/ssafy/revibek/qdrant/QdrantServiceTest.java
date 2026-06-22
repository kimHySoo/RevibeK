package com.ssafy.revibek.qdrant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.google.common.util.concurrent.Futures;
import com.ssafy.revibek.song.mapper.SongDao;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QdrantServiceTest {

    private QdrantClient qdrantClient;
    private SongDao songDao;
    private QdrantService qdrantService;

    private static final String SONG_ID = "9ebacc70-6beb-11f1-ad3a-80ca5293d4d8";

    @BeforeEach
    void setUp() {
        qdrantClient = mock(QdrantClient.class);
        songDao = mock(SongDao.class);
        qdrantService = new QdrantService(qdrantClient, songDao);
        ReflectionTestUtils.setField(qdrantService, "enabled", true);
        ReflectionTestUtils.setField(qdrantService, "collection", "revibek_songs");
    }

    @Test
    void point가없으면_예외를던지지않고_빈결과를반환한다() {
        when(qdrantClient.queryAsync(any(QueryPoints.class)))
            .thenReturn(Futures.immediateFailedFuture(
                new RuntimeException("NOT_FOUND: Not found: No point with id " + SONG_ID + " found")));

        List<String> result = qdrantService.searchSimilar(SONG_ID, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void 실제연결오류가발생해도_예외를던지지않고_빈결과를반환한다() {
        when(qdrantClient.queryAsync(any(QueryPoints.class)))
            .thenReturn(Futures.immediateFailedFuture(
                new RuntimeException("UNAVAILABLE: io exception, channel not ready")));

        List<String> result = qdrantService.searchSimilar(SONG_ID, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void 정상응답이면_자기자신을제외한유사곡ID목록을반환한다() {
        ScoredPoint self = ScoredPoint.newBuilder()
            .setId(io.qdrant.client.PointIdFactory.id(java.util.UUID.fromString(SONG_ID)))
            .build();
        String similarId = "11111111-1111-1111-1111-111111111111";
        ScoredPoint similar = ScoredPoint.newBuilder()
            .setId(io.qdrant.client.PointIdFactory.id(java.util.UUID.fromString(similarId)))
            .build();
        when(qdrantClient.queryAsync(any(QueryPoints.class)))
            .thenReturn(Futures.immediateFuture(List.of(self, similar)));

        List<String> result = qdrantService.searchSimilar(SONG_ID, 5);

        assertThat(result).containsExactly(similarId);
    }

    @Test
    void point누락시_동일요청에서_재조회를_반복하지않고_한번만호출한다() {
        when(qdrantClient.queryAsync(any(QueryPoints.class)))
            .thenReturn(Futures.immediateFailedFuture(
                new RuntimeException("NOT_FOUND: Not found: No point with id " + SONG_ID + " found")));

        qdrantService.searchSimilar(SONG_ID, 5);

        verify(qdrantClient, times(1)).queryAsync(any(QueryPoints.class));
    }

    @Test
    void qdrant가비활성화면_클라이언트를호출하지않고_빈결과를반환한다() {
        ReflectionTestUtils.setField(qdrantService, "enabled", false);

        List<String> result = qdrantService.searchSimilar(SONG_ID, 5);

        assertThat(result).isEmpty();
        verify(qdrantClient, times(0)).queryAsync(any(QueryPoints.class));
    }
}
