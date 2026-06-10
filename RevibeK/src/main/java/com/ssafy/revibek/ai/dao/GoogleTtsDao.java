package com.ssafy.revibek.ai.dao;

import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeRequestDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsSynthesizeResponseDto;
import com.ssafy.revibek.ai.dto.external.GoogleTtsVoicesResponseDto;

public interface GoogleTtsDao {

    GoogleTtsSynthesizeResponseDto synthesize(GoogleTtsSynthesizeRequestDto requestDto);

    GoogleTtsVoicesResponseDto listVoices(String languageCode);
}
