package com.example.appbackend.mapper;

import com.example.appbackend.entity.Word;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 单词字典表数据访问接口
 */
@Mapper
public interface WordMapper {

    @Insert({
            "<script>",
            "INSERT INTO word (book_id, spelling, phonetic, translation, difficulty, common_meaning, cs_meaning, en_example, cn_example) VALUES ",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.bookId}, #{item.spelling}, #{item.phonetic}, #{item.translation}, #{item.difficulty}, #{item.commonMeaning}, #{item.csMeaning}, #{item.enExample}, #{item.cnExample})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<Word> list);

    // [新增]：根据词书ID获取全量单词
    @Select("SELECT * FROM word WHERE book_id = #{bookId}")
    List<Word> selectByBookId(@Param("bookId") Long bookId);
}