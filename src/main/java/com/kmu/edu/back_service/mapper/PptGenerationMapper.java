package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.PptGeneration;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * PPT生成记录Mapper（基于注解的SQL映射）
 */
@Mapper
public interface PptGenerationMapper {

    /** 插入PPT生成记录（仅插入基础字段，其他生成完成后更新） */
    @Insert("INSERT INTO ppt_generation (user_id, title, prompt, template_id, slide_count, status) " +
            "VALUES (#{userId}, #{title}, #{prompt}, #{templateId}, #{slideCount}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PptGeneration pptGeneration);

    /** 根据ID查询 */
    @Select("SELECT * FROM ppt_generation WHERE id = #{id}")
    PptGeneration selectById(@Param("id") Long id);

    /** 根据用户ID分页查询 */
    @Select("SELECT * FROM ppt_generation WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<PptGeneration> selectByUserId(@Param("userId") Long userId,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    /** 统计用户记录数（可选） */
    @Select("SELECT COUNT(*) FROM ppt_generation WHERE user_id = #{userId}")
    Long countByUserId(@Param("userId") Long userId);

    /** 更新生成结果字段 */
    @Update("UPDATE ppt_generation SET ppt_url = #{pptUrl}, pdf_url = #{pdfUrl}, thumbnail_url = #{thumbnailUrl}, " +
            "generation_time = #{generationTime}, status = #{status}, error_message = #{errorMessage} WHERE id = #{id}")
    int updateById(PptGeneration pptGeneration);

    /** 根据ID删除 */
    @Delete("DELETE FROM ppt_generation WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /** 查询全部（分页，可选） */
    @Select("SELECT * FROM ppt_generation ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<PptGeneration> selectAll(@Param("offset") Integer offset,
                                  @Param("limit") Integer limit);

    /** 统计总数（可选） */
    @Select("SELECT COUNT(*) FROM ppt_generation")
    Long countAll();
}