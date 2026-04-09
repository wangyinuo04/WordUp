package com.example.appbackend.mapper;

import com.example.appbackend.entity.WordBook;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordBookMapper {
    /**
     * 插入词书信息，并返回数据库生成的自增 ID 给入参对象
     */
    @Insert("INSERT INTO word_book (book_name, total_words) VALUES (#{bookName}, #{totalWords})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WordBook wordBook);

    /**
     * 查询所有词书列表
     */
    @Select("SELECT * FROM word_book")
    List<WordBook> selectAll();

    /**
     * 根据 ID 查询词书信息
     */
    @Select("SELECT * FROM word_book WHERE id = #{id}")
    WordBook selectById(Long id);
}