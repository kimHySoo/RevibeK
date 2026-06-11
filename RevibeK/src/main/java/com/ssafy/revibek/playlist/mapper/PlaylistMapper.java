package com.ssafy.revibek.playlist.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.revibek.playlist.dto.PlaylistDto;
import com.ssafy.revibek.playlist.dto.PlaylistItemDto;

@Mapper
public interface PlaylistMapper {

    int insertPlaylist(PlaylistDto playlist);

    List<PlaylistDto> selectPlaylistsByUserId(@Param("userId") String userId);

    PlaylistDto selectPlaylistByIdAndUserId(@Param("playlistId") String playlistId,
                                            @Param("userId") String userId);

    int deletePlaylist(@Param("playlistId") String playlistId,
                       @Param("userId") String userId);

    int countPlaylistItem(@Param("playlistId") String playlistId,
                          @Param("songId") String songId);

    int selectNextOrderNum(@Param("playlistId") String playlistId);

    int insertPlaylistItem(@Param("playlistId") String playlistId,
                           @Param("songId") String songId,
                           @Param("orderNum") int orderNum);

    List<PlaylistItemDto> selectPlaylistItems(@Param("playlistId") String playlistId);

    int deletePlaylistItem(@Param("playlistId") String playlistId,
                           @Param("itemId") String itemId);
}
