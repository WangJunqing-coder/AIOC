package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.ImageGeneration;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 图片生成Mapper接口
 */
@Mapper
public interface ImageGenerationMapper {
    
    /**
     * 插入图片生成记录
     */
    @Insert("INSERT INTO image_generation (user_id, prompt, style, size, image_url, thumbnail_url, " +
            "generation_time, status, error_message) VALUES " +
            "(#{userId}, #{prompt}, #{style}, #{size}, #{imageUrl}, #{thumbnailUrl}, " +
            "#{generationTime}, #{status}, #{errorMessage})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ImageGeneration record);
    
    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM image_generation WHERE id = #{id}")
    ImageGeneration selectById(Long id);
    
    /**
     * 更新生成结果
     */
    @Update("UPDATE image_generation SET image_url = #{imageUrl}, thumbnail_url = #{thumbnailUrl}, " +
            "generation_time = #{generationTime}, status = #{status}, error_message = #{errorMessage} " +
            "WHERE id = #{id}")
    int updateResult(ImageGeneration record);
    
    /**
     * 根据用户ID查询生成记录
     */
    @Select("SELECT * FROM image_generation WHERE user_id = #{userId} " +
            "ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<ImageGeneration> selectByUserId(@Param("userId") Long userId, 
                                        @Param("offset") Integer offset, 
                                        @Param("size") Integer size);

    /**
     * 根据用户ID查询生成记录（配合 PageHelper，不带 LIMIT）
     */
    @Select("SELECT * FROM image_generation WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ImageGeneration> selectByUserIdPaged(@Param("userId") Long userId);
    
    /**
     * 查询用户生成记录总数
     */
    @Select("SELECT COUNT(*) FROM image_generation WHERE user_id = #{userId}")
    Long countByUserId(Long userId);
    
    /**
     * 根据状态查询记录
     */
    @Select("SELECT * FROM image_generation WHERE status = #{status} " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<ImageGeneration> selectByStatus(@Param("status") Integer status, 
                                        @Param("limit") Integer limit);
    
    /**
     * 删除记录
     */
    @Delete("DELETE FROM image_generation WHERE id = #{id}")
    int deleteById(Long id);
}