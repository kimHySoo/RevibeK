package com.ssafy.revibek.song.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ssafy.revibek.song.mapper.SongDao;

class SongServiceImplTest {

    private SongDao songDao;
    private SongServiceImpl songService;

    @BeforeEach
    void setUp() {
        songDao = mock(SongDao.class);
        songService = new SongServiceImpl();
        ReflectionTestUtils.setField(songService, "songDao", songDao);
    }

    @Test
    void 감정코드_2개까지는_정상적으로_동기화된다() {
        songService.syncMoods("song-1", List.of("EXCITED", "ENERGETIC"));

        verify(songDao).deleteSongMoodsNotIn("song-1", List.of("EXCITED", "ENERGETIC"));
        verify(songDao).insertSongMoodsIgnore("song-1", List.of("EXCITED", "ENERGETIC"));
    }

    @Test
    void 감정코드_3개_이상이면_예외가_발생하고_DB에_쓰지않는다() {
        assertThatThrownBy(() -> songService.syncMoods("song-1", List.of("EXCITED", "ENERGETIC", "CONFIDENT")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(songDao, never()).deleteSongMoodsNotIn(eq("song-1"), anyList());
        verify(songDao, never()).insertSongMoodsIgnore(eq("song-1"), anyList());
    }

    @Test
    void 표준코드가_아닌_값은_거부한다() {
        assertThatThrownBy(() -> songService.syncMoods("song-1", List.of("환상")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 대소문자와_중복은_정규화된다() {
        songService.syncMoods("song-1", List.of("excited", "EXCITED", "Excited"));

        verify(songDao).deleteSongMoodsNotIn("song-1", List.of("EXCITED"));
        verify(songDao).insertSongMoodsIgnore("song-1", List.of("EXCITED"));
    }

    @Test
    void 빈목록이면_전체삭제만하고_삽입은_호출하지않는다() {
        songService.syncMoods("song-1", List.of());

        verify(songDao).deleteSongMoodsNotIn("song-1", List.of());
        verify(songDao, never()).insertSongMoodsIgnore(eq("song-1"), anyList());
    }
}
