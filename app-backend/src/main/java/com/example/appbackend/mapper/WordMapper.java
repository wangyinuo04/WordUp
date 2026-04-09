package com.example.appbackend.mapper;

import com.example.appbackend.entity.Word;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 单词字典表数据访问接口
 */
@Mapper
public interface WordMapper {
    /**
     * 批量插入单词数据，包含新增的扩展字段
     */
    @Insert({
            "<script>",
            "INSERT INTO word (book_id, spelling, phonetic, translation, difficulty, common_meaning, cs_meaning, en_example, cn_example) VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.bookId}, #{item.spelling}, #{item.phonetic}, #{item.translation}, #{item.difficulty}, #{item.commonMeaning}, #{item.csMeaning}, #{item.enExample}, #{item.cnExample})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<Word> list);
}