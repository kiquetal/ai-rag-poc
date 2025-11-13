package me.cresterida;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.List;


import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

@Singleton
@Startup
public class Ingestion
{
    private Logger LOGGER = Logger.getLogger(Ingestion.class);

    public Ingestion(EmbeddingStore<TextSegment> store, EmbeddingModel embeddingModel)
    {
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .documentSplitter(recursive(1024,0))
                .build();


        Path dir = Path.of("documents");
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(dir);
        LOGGER.info("Loaded " + documents.size() + " documents");
        ingestor.ingest(documents);
        LOGGER.info("Documents ingested");
    }
}
