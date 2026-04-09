package com.example.appbackend.entity;

import com.alibaba.excel.annotation.ExcelProperty;

/**
 * Excel/CSV 文件解析数据传输对象
 */
public class WordExcelDTO {

    @ExcelProperty(index = 0)
    private String spelling;

    @ExcelProperty(index = 1)
    private String phonetic;

    @ExcelProperty(index = 2)
    private Integer difficulty;

    @ExcelProperty(index = 3)
    private String commonMeaning;

    @ExcelProperty(index = 4)
    private String csMeaning;

    @ExcelProperty(index = 5)
    private String enExample;

    @ExcelProperty(index = 6)
    private String cnExample;

    public String getSpelling() { return spelling; }
    public void setSpelling(String spelling) { this.spelling = spelling; }
    public String getPhonetic() { return phonetic; }
    public void setPhonetic(String phonetic) { this.phonetic = phonetic; }
    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
    public String getCommonMeaning() { return commonMeaning; }
    public void setCommonMeaning(String commonMeaning) { this.commonMeaning = commonMeaning; }
    public String getCsMeaning() { return csMeaning; }
    public void setCsMeaning(String csMeaning) { this.csMeaning = csMeaning; }
    public String getEnExample() { return enExample; }
    public void setEnExample(String enExample) { this.enExample = enExample; }
    public String getCnExample() { return cnExample; }
    public void setCnExample(String cnExample) { this.cnExample = cnExample; }
}