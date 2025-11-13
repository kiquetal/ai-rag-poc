package me.cresterida;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkus.infinispan.client.Remote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCache;

import java.util.UUID;

@ApplicationScoped
public class Ingestor {

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    /**
     * A dedicated cache to store and track the IDs of ingested documents.
     * This provides a reliable way to count unique documents.
     */
    @Inject
    @Remote("document-ids")
    RemoteCache<String, String> documentIdsCache;

    public void ingest(String text) {
        // 1. Create a unique ID for the new document first.
        String docId = UUID.randomUUID().toString();

        // 2. Create the Metadata object using the correct static factory method.
        Metadata metadata = Metadata.from("document_id", docId);

        // 3. Create the Document using the correct static factory method.
        Document document = Document.from(text, metadata);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(100, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        // 4. Store the ID we created in our tracking cache.
        documentIdsCache.put(docId, docId);
    }

    /**
     * Retrieves the total number of unique documents that have been ingested.
     *
     * @return The total number of unique documents.
     */
    public long getDocumentCount() {
        return documentIdsCache.size();
    }
}
