package com.ssafy.revibek.analysis.service;

import com.ssafy.revibek.analysis.dto.AnalyzeResponseDto;
import com.ssafy.revibek.song.dto.SongDto;

public interface AnalysisService {
    AnalyzeResponseDto analyze(SongDto song);
    void analyzeAndSave(SongDto song);
}