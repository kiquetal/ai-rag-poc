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
import org.infinispan.commons.api.query.Query;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class Ingestor {

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    @Remote("document-ids")
    RemoteCache<String, DocumentInfo> documentIdsCache;

    public void ingest(String title, String text) {
        String docId = UUID.randomUUID().toString();
        Metadata metadata = Metadata.from("document_id", docId);
        Document document = Document.from(text, metadata);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(100, 0))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(document);

        documentIdsCache.put(docId, new DocumentInfo(docId, title));
    }

    public long getDocumentCount() {
        return documentIdsCache.size();
    }

    public List<DocumentInfo> searchByTitle(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return Collections.emptyList();
        }

        // Correctly use the .query() method directly on the RemoteCache.
        Query<DocumentInfo> query = documentIdsCache.query("FROM me.cresterida.DocumentInfo WHERE title LIKE :searchTerm");
        query.setParameter("searchTerm", "%" + searchTerm + "%");
        return query.list();
    }
}
