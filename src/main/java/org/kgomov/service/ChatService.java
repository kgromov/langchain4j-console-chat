package org.kgomov.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static java.util.stream.Collectors.joining;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatModel;
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;

    public String answer(String question) {
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        // Find relevant embeddings in embedding store by semantic similarity
        // You can play with parameters below to find a sweet spot for your specific use case
        int maxResults = 3;
        double minScore = 0.5;
        var relevantEmbeddings = embeddingStore.findRelevant(questionEmbedding, maxResults, minScore);
        // Create a prompt for the model that includes question and relevant embeddings
        PromptTemplate promptTemplate = PromptTemplate.from(
                """
                        You are a helpful assistant, conversing with a user about the subjects contained in a set of documents.
                        Use the information from the DOCUMENTS section to provide accurate answers.
                        If unsure or if the answer isn't found in the DOCUMENTS section, simply state that you don't know the answer.

                        QUESTION:
                        {{question}}

                        DOCUMENTS:
                        {{documents}}
                        """
        );
        String documentsContent = relevantEmbeddings.stream()
                .map(match -> match.embedded().text())
                .collect(joining("\n"));
        Map<String, Object> promptParams = Map.of(
                "question", question,
                "documents", documentsContent
        );
        Prompt prompt = promptTemplate.apply(promptParams);
        // Send the prompt to the OpenAI chat model
        AiMessage aiMessage = chatModel.generate(prompt.toUserMessage()).content();
        return aiMessage.text();
    }
}
