package com.itheima.aicodehelper.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 面试鸭网站获取面试题工具类
 */
@Slf4j
@Component
public class InterviewQuestionTool {

    @Tool(name = "interviewQuestionSearch", value = """
            Retrieves relevant interview questions from mianshiya.com based on a keyword.
            Use this tool when the user asks for interview questions about specific technologies,
            programming concepts, or job-related topics. The input should be a clear search term,
            for example "redis", "java 多线程" or "spring".
            The tool returns the matching question titles, one per line.
            """)
    public String searchInterviewQuestions(@P(value = "the keyword to search") String keyword) {

        // 构建搜索 URL（参数编码防止中文乱码）
        String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = "https://www.mianshiya.com/search/all?searchText=" + encodedKeyword;

        // 发送请求并解析页面
        Document doc;
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();
        } catch (Exception e) {
            log.error("获取面试鸭搜索结果失败, url: {}", url, e);
            return "获取失败: " + e.getMessage();
        }

        // 解析页面：只取结果表格每行第 2 列的题目链接，避免把标签也抓进来
        Elements questionElements = doc.select("tbody tr.ant-table-row td:nth-child(2) a");
        List<String> questions = new ArrayList<>();
        questionElements.forEach(el -> questions.add(el.text().trim()));
        return String.join("\n", questions);
    }
}
