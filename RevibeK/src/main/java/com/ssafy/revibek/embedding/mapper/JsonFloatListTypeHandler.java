package com.ssafy.revibek.embedding.mapper;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * embedding_songs.vector(JSON 컬럼)을 List&lt;Float&gt;로 직렬화/역직렬화한다.
 * answer16.md 1장 - 벡터 본체를 파일이 아닌 MySQL에 직접 저장하도록 재설계한 결과.
 */
@MappedTypes(List.class)
public class JsonFloatListTypeHandler extends BaseTypeHandler<List<Float>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Float> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("벡터(List<Float>) 직렬화 실패", e);
        }
    }

    @Override
    public List<Float> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<Float> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<Float> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<Float> parse(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<Float>>() {
            });
        } catch (JsonProcessingException e) {
            throw new SQLException("벡터(List<Float>) 역직렬화 실패", e);
        }
    }
}
