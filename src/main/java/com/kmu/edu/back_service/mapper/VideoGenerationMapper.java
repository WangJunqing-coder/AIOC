package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.VideoGeneration;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VideoGenerationMapper {

    @Insert("INSERT INTO video_generation (user_id, prompt, source_type, source_image_url, duration, style, " +
            "video_url, thumbnail_url, generation_time, progress, status, error_message) VALUES " +
            "(#{userId}, #{prompt}, #{sourceType}, #{sourceImageUrl}, #{duration}, #{style}, " +
            "#{videoUrl}, #{thumbnailUrl}, #{generationTime}, #{progress}, #{status}, #{errorMessage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VideoGeneration record);

    @Select("SELECT * FROM video_generation WHERE id = #{id}")
    VideoGeneration selectById(Long id);

    @Update("UPDATE video_generation SET video_url = #{videoUrl}, thumbnail_url = #{thumbnailUrl}, " +
            "generation_time = #{generationTime}, progress = #{progress}, status = #{status}, error_message = #{errorMessage} " +
            "WHERE id = #{id}")
    int updateResult(VideoGeneration record);

    @Select("SELECT * FROM video_generation WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<VideoGeneration> selectByUserId(@Param("userId") Long userId,
                                         @Param("offset") Integer offset,
                                         @Param("size") Integer size);

    @Delete("DELETE FROM video_generation WHERE id = #{id}")
    int deleteById(Long id);
}
