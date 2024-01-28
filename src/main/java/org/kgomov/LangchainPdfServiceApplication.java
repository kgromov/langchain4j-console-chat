package org.kgomov;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.junit.jupiter.api.Order;
import org.kgomov.config.EmbeddingStoreSettings;
import org.kgomov.config.OpenAISetting;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;

import java.util.Scanner;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

@SpringBootApplication
@EnableConfigurationProperties({EmbeddingStoreSettings.class, OpenAISetting.class})
public class LangchainPdfServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangchainPdfServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner loadDocumentRunner(EmbeddingStoreIngestor ingestor, ResourceLoader resourceLoader) {
        return args -> {
            System.out.println("Load htmlcheatsheet data");
            Document document = loadDocument(
                    resourceLoader.getResource("classpath:input-sources/htmlcheatsheet.pdf").getFile().toPath(),
                    new ApachePdfBoxDocumentParser()
            );
            ingestor.ingest(document);
        };
    }

    @Bean
    @DependsOn("loadDocumentRunner")
    ApplicationRunner interactiveChatRunner(ConversationalRetrievalChain retrievalChain) {
        return args -> {
            System.out.println("Spin up chat");
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("User: ");
                String userMessage = scanner.nextLine();

                if ("exit".equalsIgnoreCase(userMessage)) {
                    break;
                }

                String agentMessage = retrievalChain.execute(userMessage);
                System.out.println("Agent: " + agentMessage);
            }

            scanner.close();
        };
    }
}
