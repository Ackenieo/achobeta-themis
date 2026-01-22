package com.achobeta.themis.common.util;

import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class IKPreprocessorUtil {
    /**
     * 对文本进行IK分词处理（会过滤停用词）
     * 注意：IK分词器默认会过滤停用词（如：你、我、是、的等），可能导致某些短语分词后为空
     * 如果需要保留所有词，请使用 segmentWithoutStopWords 方法
     * @param text 待分词的文本
     * @param useSmart 是否使用智能分词模式
     * @return 分词后的文本，词语之间用空格分隔
     * @throws Exception 如果分词过程中发生错误
     */
    public static String segment(String text, boolean useSmart) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        StringReader reader = new StringReader(text);
        IKSegmenter segmenter = new IKSegmenter(reader, useSmart);
        List<String> words = new ArrayList<>();
        Lexeme lexeme;
        while ((lexeme = segmenter.next()) != null) {
            words.add(lexeme.getLexemeText());
        }
        // 如果分词结果为空（全是停用词），返回原文
        if (words.isEmpty()) {
            return text;
        }
        return String.join(" ", words);
    }
    
    /**
     * 对文本进行IK分词处理（不过滤停用词，保留所有分词结果）
     * 适用于需要保留完整语义的场景，如问题分类、相似度匹配等
     * @param text 待分词的文本
     * @param useSmart 是否使用智能分词模式
     * @return 分词后的文本，词语之间用空格分隔
     * @throws Exception 如果分词过程中发生错误
     */
    public static String segmentWithoutStopWords(String text, boolean useSmart) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        // 使用非智能模式可以减少停用词过滤的影响
        StringReader reader = new StringReader(text);
        IKSegmenter segmenter = new IKSegmenter(reader, false); // 使用非智能模式
        List<String> words = new ArrayList<>();
        Lexeme lexeme;
        while ((lexeme = segmenter.next()) != null) {
            words.add(lexeme.getLexemeText());
        }
        // 如果还是为空，直接返回原文
        if (words.isEmpty()) {
            return text;
        }
        return String.join(" ", words);
    }

    /**
     * 分词方法：保留复合词（非智能模式，避免拆分），返回空格分隔的整词
     * @param text 待分词文本
     * @param useSmart 是否使用智能模式（建议传false，保留复合词）
     * @return 空格分隔的分词结果
     * @throws Exception 分词异常
     */
    public static String stopWordSegment(String text, boolean useSmart) throws Exception {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        StringReader reader = new StringReader(text);
        IKSegmenter segmenter = new IKSegmenter(reader, useSmart);
        List<String> words = new ArrayList<>();
        Lexeme lexeme;
        while ((lexeme = segmenter.next()) != null) {
            String word = lexeme.getLexemeText();
            if (!isStopWord(word)) {
                words.add(word);
            }
        }
        return String.join(" ", words);
    }

    /**
     * 停用词过滤（可选，进一步减少干扰词）
     */
    private static boolean isStopWord(String word) {
        // 可配置停用词表，这里示例过滤常见无意义词
        List<String> stopWords = List.of("对于", "来说", "双方", "《", "》", "呢", "谁", "劳动");
        return stopWords.contains(word);
    }


}
