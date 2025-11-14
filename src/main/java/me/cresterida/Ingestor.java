package me.cresterida;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkus.infinispan.client.Remote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
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

    @Inject
    RemoteCacheManager remoteCacheManager; // Inject the main cache manager
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
        var documentIdsCache = remoteCacheManager.getCache("document-ids");

        return documentIdsCache.size();
    }

    public List<DocumentInfo> searchByDocumentTitle(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return Collections.emptyList();
        }

        // Get the cache
        RemoteCache<String, DocumentInfo> documentIdsCache = remoteCacheManager.getCache("document-ids");

        // The query is updated to check both 'title' AND 'content'
        String queryString = "FROM me.cresterida.DocumentInfo WHERE title : :searchTerm";

        Query<DocumentInfo> query = documentIdsCache.query(queryString);


        query.setParameter("searchTerm", searchTerm);

        return query.list();
    }
    public List<CombinedSearchResult> findSimilarContent(String searchTerm) {

        // 1. Convert the user's search term into a vector
        Embedding searchTermEmbedding = embeddingModel.embed(searchTerm).content();

        List<EmbeddingMatch<TextSegment>> relevantMatches = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(searchTermEmbedding)
                        .maxResults(10)
                        .build()).matches();

        // 3. Extract the segments from the matches
        return relevantMatches.stream()
                .map(match -> {
                    // Get the segment and its score
                    TextSegment segment = match.embedded();
                    double score = match.score();

                    // Get the document_id from the segment's metadata
                    String docId = segment.metadata().getString("document_id");

                    // NOW, use the docId to get the title from our *other* cache
                    DocumentInfo docInfo = documentIdsCache.get(docId);
                    String title = (docInfo != null) ? docInfo.title() : "Unknown Title";

                    // Create our nice, clean result object
                    return new CombinedSearchResult(score, segment.text(), docId, title);
                })
                .toList();


    }
    public record CombinedSearchResult(
            double score,
            String text,
            String documentId,
            String title
    ) {}
}
