package org.kgomov;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import lombok.extern.slf4j.Slf4j;
import org.kgomov.config.EmbeddingStoreSettings;
import org.kgomov.config.OpenAISetting;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StopWatch;

import java.util.Scanner;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties({EmbeddingStoreSettings.class, OpenAISetting.class})
public class LangchainPdfServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangchainPdfServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner interactiveChatRunner(ConversationalRetrievalChain retrievalChain) {
        return args -> {
            log.info("Spin up chat");
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.print("User: ");
                    String userMessage = scanner.nextLine();

                    if ("exit".equalsIgnoreCase(userMessage)) {
                        break;
                    }
                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start("Answer question");
                    try {
                        String agentMessage = retrievalChain.execute(userMessage);
                        System.out.println("Agent: " + agentMessage);
                    } finally {
                        stopWatch.stop();
                        var taskInfo = stopWatch.lastTaskInfo();
                        log.info("Time to {} = {} ms", taskInfo.getTaskName(), taskInfo.getTimeMillis());
                    }
                }
            }
        };
    }
}
