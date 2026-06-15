# RevibeK 백엔드 수정 파일 전체 코드 재출력 요청

이전 작업에서 `docs/answer/answer13.md` 또는 `docs/answer/answer19_BE_radio_only.md`에 백엔드 수정 결과를 정리했지만, 일부 파일이 전체 코드가 아니라 핵심 코드만 제공되었습니다.

이번 작업에서는 코드를 새로 수정하지 말고, 이전 작업에서 실제로 수정된 파일들의 “현재 최종 전체 코드”를 그대로 읽어서 문서로 저장해주세요.

## 작업 원칙

```text
1. 코드를 새로 수정하지 마세요.
2. 프론트엔드 코드를 만들거나 수정하지 마세요.
3. Song search 관련 코드를 수정하지 마세요.
4. 현재 프로젝트에 존재하는 최종 파일 내용을 그대로 읽어주세요.
5. 수정된 파일은 전체 코드를 생략하지 말고 출력해주세요.
6. 너무 길다는 이유로 핵심 코드만 요약하지 마세요.
```

## 반드시 전체 코드로 보여줄 파일

아래 파일은 반드시 “파일 전체 코드”를 보여주세요.

```text
src/main/java/com/ssafy/revibek/radio/dto/RadioResponseDto.java
src/main/java/com/ssafy/revibek/radio/dto/RadioCreateResponseDto.java
src/main/java/com/ssafy/revibek/radio/dto/RadioCreateRequestDto.java
src/main/java/com/ssafy/revibek/radio/mapper/RadioMapper.java
src/main/resources/mapper/RadioMapper.xml
src/main/java/com/ssafy/revibek/radio/service/RadioService.java
src/main/java/com/ssafy/revibek/common/GlobalExceptionHandler.java
```

## 새로 추가된 예외 파일 찾기

아래 파일은 실제 위치를 먼저 찾아주세요.

```text
RadioNotFoundException.java
```

가능한 위치 예시:

```text
src/main/java/com/ssafy/revibek/radio/exception/RadioNotFoundException.java
src/main/java/com/ssafy/revibek/common/exception/RadioNotFoundException.java
src/main/java/com/ssafy/revibek/radio/RadioNotFoundException.java
```

찾은 뒤 전체 코드를 출력해주세요.

## SQL 파일 전체 코드

아래 SQL 파일도 전체 내용을 보여주세요.

```text
src/main/resources/sql/kpop_radio_schema.sql
src/main/resources/sql/migration_add_radio_session_playlist_id.sql
```

만약 실제 경로가 다르면 파일명으로 검색해서 찾은 뒤 전체 내용을 보여주세요.

## 확인만 할 파일

아래 파일은 수정하지 않았더라도, 관련 메서드 전체를 보여주세요.

```text
src/main/java/com/ssafy/revibek/radio/controller/RadioController.java
src/main/java/com/ssafy/revibek/playlist/service/PlaylistService.java
src/main/java/com/ssafy/revibek/playlist/controller/PlaylistController.java
```

특히 `PlaylistService`에서는 아래 메서드 전체를 보여주세요.

```text
createPlaylist
addItem
```

이유는 `RadioService.createRadioPlaylist()`에서 `PlaylistService.createPlaylist()`와 `PlaylistService.addItem()`을 호출하기 때문에, 메서드 시그니처가 맞는지 확인해야 하기 때문입니다.

## 추가 검증

아래 항목을 코드 기준으로 확인해주세요.

```text
1. RadioService에서 createRadioPlaylist()가 실제로 존재하는가?
2. createRadioPlaylist() 안에서 PlaylistService.createPlaylist() 호출 방식이 실제 메서드 시그니처와 맞는가?
3. createRadioPlaylist() 안에서 PlaylistService.addItem() 호출 방식이 실제 메서드 시그니처와 맞는가?
4. RadioMapper.xml의 updateRadioSessionPlaylistId id가 RadioMapper.java 메서드명과 정확히 일치하는가?
5. RadioMapper.xml SELECT에 playlist_id가 포함되어 있는가?
6. RadioResponseDto에 playlistId 필드가 있는가?
7. GlobalExceptionHandler에 RadioNotFoundException 처리 메서드가 있는가?
8. Maven compile 또는 test를 실행하지 못했다면 그 이유를 다시 정리해주세요.
```

## 결과 저장

결과는 아래 파일에 저장해주세요.

```text
docs/answer/answer13_fullcode.md
```

반드시 UTF-8 인코딩으로 저장해주세요.

## answer13_fullcode.md 형식

아래 형식으로 작성해주세요.

```markdown
# RevibeK 백엔드 수정 파일 전체 코드 확인 결과

## 1. 전체 결론

## 2. 실제 수정된 파일 목록

## 3. RadioResponseDto.java 전체 코드

## 4. RadioCreateResponseDto.java 전체 코드

## 5. RadioCreateRequestDto.java 전체 코드

## 6. RadioMapper.java 전체 코드

## 7. RadioMapper.xml 전체 코드

## 8. RadioService.java 전체 코드

## 9. RadioNotFoundException.java 전체 코드

## 10. GlobalExceptionHandler.java 전체 코드

## 11. kpop_radio_schema.sql 전체 코드 또는 radio_sessions 관련 전체 DDL

## 12. migration_add_radio_session_playlist_id.sql 전체 코드

## 13. RadioController 관련 메서드 전체

## 14. PlaylistService createPlaylist/addItem 메서드 전체

## 15. PlaylistController 관련 메서드 전체

## 16. 메서드 시그니처 일치 여부 검증

## 17. 아직 위험한 부분

## 18. 다음에 내가 직접 확인할 체크리스트
```

## 최종 지시

이번 작업은 “전체 코드 재출력 및 검증” 작업입니다.

코드를 새로 수정하지 말고, 현재 프로젝트 파일을 읽어서 전체 코드를 문서에 정리해주세요.

절대 핵심 코드만 요약하지 말고, 지정한 파일은 전체 코드를 보여주세요.

결과는 반드시 `docs/answer/answer13_fullcode.md`에 저장해주세요.
