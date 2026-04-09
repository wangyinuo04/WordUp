package com.example.appbackend.service;

import com.alibaba.excel.EasyExcel;
import com.example.appbackend.entity.Word;
import com.example.appbackend.entity.WordBook;
import com.example.appbackend.entity.WordExcelDTO;
import com.example.appbackend.mapper.WordBookMapper;
import com.example.appbackend.mapper.WordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 词书数据管理业务逻辑类
 */
@Service
public class WordBookService {

    private final WordBookMapper wordBookMapper;
    private final WordMapper wordMapper;

    public WordBookService(WordBookMapper wordBookMapper, WordMapper wordMapper) {
        this.wordBookMapper = wordBookMapper;
        this.wordMapper = wordMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void importBookData(String bookName, MultipartFile file) throws Exception {
        List<WordExcelDTO> dataList = EasyExcel.read(file.getInputStream())
                .head(WordExcelDTO.class)
                .sheet()
                .doReadSync();

        if (dataList == null || dataList.isEmpty()) {
            throw new RuntimeException("解析失败：上传的文件内容为空或格式不正确");
        }

        WordBook book = new WordBook();
        book.setBookName(bookName);
        book.setTotalWords(dataList.size());
        wordBookMapper.insert(book);
        Long bookId = book.getId();

        List<Word> words = new ArrayList<>();
        for (WordExcelDTO dto : dataList) {
            Word word = new Word();
            word.setBookId(bookId);
            word.setSpelling(dto.getSpelling() != null ? dto.getSpelling() : "");
            word.setPhonetic(dto.getPhonetic() != null ? dto.getPhonetic() : "");
            word.setTranslation(dto.getCommonMeaning() != null ? dto.getCommonMeaning() : "");
            word.setDifficulty(dto.getDifficulty() != null ? dto.getDifficulty() : 2);
            word.setCommonMeaning(dto.getCommonMeaning());
            word.setCsMeaning(dto.getCsMeaning());
            word.setEnExample(dto.getEnExample());
            word.setCnExample(dto.getCnExample());
            words.add(word);
        }
        wordMapper.insertBatch(words);
    }

    /**
     * 获取所有词书
     */
    public List<WordBook> getAllBooks() {
        return wordBookMapper.selectAll();
    }

    /**
     * 根据 ID 获取单个词书信息
     */
    public WordBook getBookById(Long id) {
        if (id == null) return null;
        return wordBookMapper.selectById(id);
    }
}