package com.kmu.edu.back_service.mapper;

import com.kmu.edu.back_service.entity.PptTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * PPT模板Mapper（基于注解SQL）
 */
@Mapper
public interface PptTemplateMapper {

    @Insert("INSERT INTO ppt_template (template_name, template_desc, template_url, thumbnail_url, category, sort_order, status) " +
            "VALUES (#{templateName}, #{templateDesc}, #{templateUrl}, #{thumbnailUrl}, #{category}, #{sortOrder}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PptTemplate pptTemplate);

    @Select("SELECT * FROM ppt_template WHERE id = #{id}")
    PptTemplate selectById(@Param("id") Long id);

    @Select("SELECT * FROM ppt_template WHERE status = 1 ORDER BY sort_order ASC, create_time DESC")
    List<PptTemplate> selectEnabled();

    @Select("SELECT * FROM ppt_template WHERE category = #{category} AND status = 1 ORDER BY sort_order ASC, create_time DESC")
    List<PptTemplate> selectByCategory(@Param("category") String category);

    @Select("SELECT * FROM ppt_template ORDER BY sort_order ASC, create_time DESC LIMIT #{offset}, #{limit}")
    List<PptTemplate> selectAll(@Param("offset") Integer offset,
                               @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM ppt_template")
    Long countAll();

    @Update("UPDATE ppt_template SET template_name = #{templateName}, template_desc = #{templateDesc}, template_url = #{templateUrl}, " +
            "thumbnail_url = #{thumbnailUrl}, category = #{category}, sort_order = #{sortOrder}, status = #{status} WHERE id = #{id}")
    int updateById(PptTemplate pptTemplate);

    @Delete("DELETE FROM ppt_template WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}