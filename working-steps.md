# How Infinispan Remote Embedding Store Works

## Overview
This RAG (Retrieval-Augmented Generation) application uses **Infinispan** as a remote distributed cache to store:
1. **Vector embeddings** for semantic search (via LangChain4j's Infinispan integration)
2. **Document metadata** (document IDs and titles) in a custom indexed cache

---

## Architecture Components

### 1. **Infinispan Server**
- Running as a Docker container (port 11222)
- Credentials: admin/password
- Acts as a remote in-memory data grid
- Provides:
  - Key-value storage
  - Vector similarity search
  - Full-text search via indexing
  - Protobuf-based serialization

### 2. **Two Separate Caches**

#### Cache A: Vector Embeddings (Managed by LangChain4j)
- **Purpose**: Store text segments and their vector embeddings
- **Managed by**: `quarkus-langchain4j-infinispan` extension
- **Configuration**: 
  ```properties
  quarkus.langchain4j.infinispan.dimension=384
  ```
- **Content**: TextSegment objects with 384-dimensional vectors
- **Created automatically** by the LangChain4j extension

#### Cache B: Document Metadata (`document-ids`)
- **Purpose**: Store document IDs and titles for metadata lookup
- **Managed by**: Manual creation in `CacheInitializer.java`
- **Content**: `DocumentInfo` records (docId, title)
- **Uses**: Protobuf serialization and Infinispan Query indexing

---

## Step-by-Step Flow

### Initialization Phase (Application Startup)

```
1. Application starts → CacheInitializer.onStart() is triggered
   ↓
2. Creates "document-ids" cache with XML configuration:
   - Encoding: application/x-protostream (Protobuf)
   - Indexing: ENABLED (local-heap storage)
   - Indexed entity: me.cresterida.DocumentInfo
   ↓
3. Protobuf schema is automatically generated from:
   - DocumentInfo.java (@Proto annotation)
   - DocumentInfoSchema.java (@ProtoSchema interface)
   ↓
4. LangChain4j extension automatically creates its vector embedding cache
   - Uses Infinispan's vector search capabilities
   - Configured for 384-dimensional embeddings
```

**Key Files:**
- `CacheInitializer.java` - Cache creation logic
- `DocumentInfo.java` - Data model with indexing annotations
- `DocumentInfoSchema.java` - Protobuf schema generator

---

### Document Ingestion Flow

When a user sends a POST request to `/documents`:

```
1. DocumentResource.ingest(title, text) receives request
   ↓
2. Ingestor.ingest(title, text) is called
   ↓
3. Generate unique document ID: UUID.randomUUID()
   ↓
4. Create metadata: Metadata.from("document_id", docId)
   ↓
5. Create LangChain4j Document: Document.from(text, metadata)
   ↓
6. EmbeddingStoreIngestor processes the document:
   a) Split text into chunks (100 chars, 0 overlap)
      → DocumentSplitters.recursive(100, 0)
   b) Generate embeddings for each chunk
      → embeddingModel.embed(chunk) 
      → Uses BgeSmallEnQuantizedEmbeddingModel (384 dimensions)
   c) Store chunks + embeddings in Infinispan
      → embeddingStore.add(embedding, textSegment)
      → Each segment includes metadata with document_id
   ↓
7. Store document metadata separately:
   documentIdsCache.put(docId, new DocumentInfo(docId, title))
   → Stored in "document-ids" cache
```

**Data Stored:**

**In Vector Cache (automatic):**
```
{
  "id": "generated-segment-id-1",
  "embedding": [0.123, -0.456, 0.789, ...], // 384 floats
  "text": "chunk of original text...",
  "metadata": {
    "document_id": "abc-123-uuid"
  }
}
```

**In document-ids Cache (manual):**
```
Key: "abc-123-uuid"
Value: DocumentInfo {
  docId: "abc-123-uuid",
  title: "My Document Title"
}
```

---

### Search Flow (Semantic/Vector Search)

When a user searches via `/documents/search?term=query`:

```
1. DocumentResource.search(term) → Ingestor.findSimilarContent(term)
   ↓
2. Convert search term to embedding vector:
   Embedding searchEmbedding = embeddingModel.embed(searchTerm)
   → Returns 384-dimensional vector
   ↓
3. Perform vector similarity search in Infinispan:
   embeddingStore.search(
     EmbeddingSearchRequest.builder()
       .queryEmbedding(searchEmbedding)
       .maxResults(10)
       .build()
   )
   ↓
4. Infinispan returns top 10 most similar segments:
   - Each match contains:
     * TextSegment (the chunk of text)
     * Score (similarity score, e.g., 0.85)
     * Metadata (including document_id)
   ↓
5. For each match, enrich with document title:
   a) Extract docId from segment.metadata
   b) Lookup title: documentIdsCache.get(docId)
   c) Create CombinedSearchResult(score, text, docId, title)
   ↓
6. Remove duplicates and return results
```

**How Vector Search Works in Infinispan:**
- Uses cosine similarity or Euclidean distance
- Implements HNSW (Hierarchical Navigable Small World) indexing
- Searches through all stored embedding vectors
- Returns top-K nearest neighbors

---

### Title Search Flow (Full-Text Search)

Via `Ingestor.searchByDocumentTitle(searchTerm)`:

```
1. Get document-ids cache
   ↓
2. Execute Ickle query (Infinispan Query Language):
   "FROM me.cresterida.DocumentInfo WHERE title : :searchTerm"
   → ":" operator performs full-text search (not exact match)
   ↓
3. Infinispan's Hibernate Search integration:
   - Tokenizes and analyzes the title field (@Text annotation)
   - Searches the inverted index
   - Returns matching DocumentInfo records
   ↓
4. Return list of matching documents
```

**Why Two Search Types?**
- **Vector search**: Finds semantically similar content (even if words differ)
- **Title search**: Fast exact/fuzzy text matching on indexed fields

---

## Key Technologies Explained

### Protobuf Serialization
- **What**: Google's efficient binary serialization format
- **Why**: Infinispan requires it for indexing and querying
- **How**: 
  - `@Proto` annotation on DocumentInfo
  - `@ProtoSchema` interface auto-generates schema at compile time
  - Schema uploaded to Infinispan at startup

### Infinispan Indexing
- **@Indexed**: Marks entity as searchable
- **@Keyword**: Exact-match indexing (for IDs, sorting)
  - `projectable = true`: Can retrieve in query results
  - `sortable = true`: Can sort by this field
- **@Text**: Full-text search indexing
  - Analyzes and tokenizes content
  - Allows word-level searches

### LangChain4j Integration
- **EmbeddingModel**: Converts text → vectors
- **EmbeddingStore**: Stores vectors in Infinispan
- **DocumentSplitters**: Chunks large documents
- **EmbeddingStoreIngestor**: Orchestrates the pipeline

---

## Configuration Breakdown

### application.properties

```properties
# Vector dimension for embeddings
quarkus.langchain4j.infinispan.dimension=384

# Embedding model (local, no API needed)
quarkus.langchain4j.embedding-model.provider=dev.langchain4j.model.embedding.onnx.bgesmallenq.BgeSmallEnQuantizedEmbeddingModel

# Production Infinispan connection
%prod.quarkus.infinispan-client.hosts=${INFINISPAN_HOSTS}
%prod.quarkus.infinispan-client.username=${INFINISPAN_USER:admin}
%prod.quarkus.infinispan-client.password=${INFINISPAN_PASS:password}
%prod.quarkus.infinispan-client.sasl.mechanism=SCRAM-SHA-512
```

### docker-compose.yml

```yaml
infinispan:
  image: infinispan/server:15.1
  ports:
    - "11222:11222"
  environment:
    - USER=admin
    - PASS=password
```

---

## Data Model

### DocumentInfo (Stored in Infinispan)
```java
@Proto
@Indexed
public record DocumentInfo(
    @Keyword(projectable = true, sortable = true)
    String docId,
    
    @Text(projectable = true)
    String title
) {}
```

### TextSegment (Stored by LangChain4j)
- Internal LangChain4j class
- Contains:
  - Text content
  - Metadata map
  - Embedding vector (stored separately in Infinispan)

---

## Common Operations

### 1. Ingest a Document
```
POST /documents
{
  "title": "Introduction to AI",
  "text": "Artificial intelligence is..."
}
```
→ Creates embeddings + stores metadata

### 2. Search by Semantic Similarity
```
GET /documents/search?term=machine learning concepts
```
→ Returns relevant text chunks with titles

### 3. Get Document Count
```
GET /documents/count
```
→ Returns size of document-ids cache

---

## Why This Architecture?

### Advantages
1. **Distributed**: Infinispan can scale across multiple nodes
2. **Fast**: In-memory storage with indexing
3. **Flexible**: Supports both vector and text search
4. **Persistent**: Can configure disk persistence if needed
5. **Queryable**: Rich query capabilities via Ickle

### Separation of Concerns
- **Vector cache**: Optimized for similarity search (high-dimensional data)
- **Metadata cache**: Optimized for structured queries (low-latency lookups)

---

## How to Access Infinispan Locally

When running `mvn quarkus:dev`, Infinispan runs in a Docker container with a **dynamic port mapping**.

### Find the Infinispan Port

```bash
# Check running Infinispan containers
docker ps | grep infinispan

# Get the exact port mapping (look for 11222/tcp -> 0.0.0.0:XXXXX)
docker ps | grep infinispan | grep -oP '0.0.0.0:\K\d+(?=->11222)'

# Or use docker port command (replace CONTAINER_ID with actual ID)
docker port <CONTAINER_ID> 11222
```

**Example output:**
```
11222/tcp -> 0.0.0.0:32771
```

This means Infinispan's port 11222 is mapped to **localhost:32771** on your machine.

### Access Infinispan Console

Once you have the port, you can:

1. **Web Console:** Open in browser
   ```
   http://localhost:32771/console/
   ```
   - Default credentials: `admin` / `password` (see docker-compose.yml)

2. **REST API:** Check caches
   ```bash
   curl http://localhost:32771/rest/v2/caches
   ```

3. **View specific cache:**
   ```bash
   # List all caches
   curl http://localhost:32771/rest/v2/caches
   
   # Get cache statistics
   curl http://localhost:32771/rest/v2/caches/document-ids?action=stats
   ```

### Quick Commands

```bash
# Get Infinispan port
INFINISPAN_PORT=$(docker port $(docker ps -q -f ancestor=quay.io/infinispan/server) 11222 | cut -d':' -f2)
echo "Infinispan is running on: http://localhost:$INFINISPAN_PORT"

# Open console
xdg-open "http://localhost:$INFINISPAN_PORT/console/"  # Linux
# or
open "http://localhost:$INFINISPAN_PORT/console/"      # macOS
```

---

## Troubleshooting Tips

### If embeddings aren't found:
1. Check cache exists: `curl http://localhost:<PORT>/rest/v2/caches` (replace <PORT> with actual port)
2. Verify embedding dimension matches (384)
3. Check Infinispan logs for schema errors

### If title search fails:
1. Ensure DocumentInfo is properly indexed (@Indexed annotation)
2. Check Protobuf schema was registered
3. Verify cache configuration has indexing enabled

### Connection issues:
1. Check Infinispan is running: `docker ps`
2. Verify connection string: `INFINISPAN_HOSTS=infinispan:11222`
3. Test credentials: admin/password
4. Check SASL mechanism matches server config

---

## Summary

This application demonstrates a **hybrid search** approach:
- **Vector embeddings** for semantic understanding
- **Metadata indexing** for structured queries
- **Infinispan** as the unified storage layer

The remote embedding store pattern allows the application to scale horizontally while maintaining fast, intelligent search capabilities across large document collections.

