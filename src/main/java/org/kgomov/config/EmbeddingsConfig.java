package org.kgomov.config;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.time.Duration;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiModelName.GPT_4;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmbeddingsConfig {
    private final OpenAISetting openai;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public ChatLanguageModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(openai.apiKey())
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    public InMemoryEmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(InMemoryEmbeddingStore<TextSegment> embeddingStore,
                                                         EmbeddingModel embeddingModel,
                                                         @Value("classpath:${settings.input.source}") Resource resource) {
        var ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(
                        300,
                        0,
                        new OpenAiTokenizer(GPT_4)
                ))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        this.loadDataFromResource(resource, ingestor);
        return ingestor;
    }

    @Bean
    public ConversationalRetrievalChain conversationalRetrievalChain(InMemoryEmbeddingStore<TextSegment> embeddingStore,
                                                                     EmbeddingModel embeddingModel) {
        return ConversationalRetrievalChain.builder()
                .chatLanguageModel(OpenAiChatModel.withApiKey(openai.apiKey()))
                .chatMemory(TokenWindowChatMemory.withMaxTokens(300, new OpenAiTokenizer(GPT_4)))
//                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
//                .retriever(EmbeddingStoreRetriever.from(embeddingStore, embeddingModel))
                .contentRetriever(EmbeddingStoreContentRetriever.from(embeddingStore))
                .build();
    }

    @SneakyThrows
    private void loadDataFromResource(Resource resource, EmbeddingStoreIngestor ingestor) {
        log.info("Load document data");
        Document document = loadDocument(
                resource.getFile().toPath(),
                new ApacheTikaDocumentParser()
//                new ApachePdfBoxDocumentParser()
        );
        ingestor.ingest(document);
    }
}
