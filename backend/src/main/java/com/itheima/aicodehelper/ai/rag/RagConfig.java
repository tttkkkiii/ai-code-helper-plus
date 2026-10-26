package com.itheima.aicodehelper.ai.rag;


import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

@Configuration
public class RagConfig {

    @Resource
    private EmbeddingModel qwenEmbeddingModel;

    // ===== 向量库连接配置，统一来自 application.yml =====

    @Value("${pgvector.host}")
    private String pgHost;

    @Value("${pgvector.port}")
    private int pgPort;

    @Value("${pgvector.database}")
    private String pgDatabase;

    @Value("${pgvector.user}")
    private String pgUser;

    @Value("${pgvector.password}")
    private String pgPassword;

    @Value("${pgvector.table}")
    private String pgTable;

    @Value("${pgvector.dimension}")
    private int pgDimension;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return PgVectorEmbeddingStore.builder()
                .host(pgHost)
                .port(pgPort)
                .database(pgDatabase)
                .user(pgUser)
                .password(pgPassword)
                .table(pgTable)
                .dimension(pgDimension)   // 千问 text-embedding-v4 的维度
                .createTable(true)        // 第一次会自动建表
                .build();
    }

    @Bean
    public ContentRetriever contentRetriever() {
        List<Document> documents = FileSystemDocumentLoader.loadDocuments("src/main/resources/docs");
        DocumentByParagraphSplitter documentByParagraphSplitter = new DocumentByParagraphSplitter(1000, 200);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentByParagraphSplitter)
                .textSegmentTransformer(textSegment -> TextSegment.from(textSegment.metadata().getString("file_name") + "\n" + textSegment.text(), textSegment.metadata()))
                .embeddingModel(qwenEmbeddingModel)
                .embeddingStore(embeddingStore()).build();

        // 向量库已有数据就跳过灌库，避免每次启动重复向量化 + 数据重复堆积
        if (countVectors() == 0) {
            ingestor.ingest(documents);
        }

        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore())
                .embeddingModel(qwenEmbeddingModel)
                .maxResults(5)
                .minScore(0.75).build();

        return contentRetriever;
    }

    /**
     * 统计向量表已有的条数；表不存在或连不上时按 0 处理（走首次灌库）。
     */
    private long countVectors() {
        String url = "jdbc:postgresql://%s:%d/%s".formatted(pgHost, pgPort, pgDatabase);
        try (Connection conn = DriverManager.getConnection(url, pgUser, pgPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM " + pgTable)) {
            rs.next();
            return rs.getLong(1);
        } catch (Exception e) {
            return 0;
        }
    }
}
