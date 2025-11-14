# Project Roadmap: From Retrieval to Production RAG

This document outlines the next steps to evolve this project from a retrieval service into a full-fledged, production-ready Retrieval-Augmented Generation (RAG) application.

---

## Phase 1: Implement the Full RAG Pattern (The "G" in RAG)

The current system retrieves relevant context. This phase adds the "Generation" component to provide a synthesized, natural language answer.

### [ ] Integrate a Chat Language Model
- Add the `quarkus-langchain4j-openai` dependency.
- Inject `ChatLanguageModel` or `StreamingChatLanguageModel`.
- Configure `application.properties` to point to a local model server.

### [ ] Use Podman AI Lab for Local Testing
- Install Podman Desktop and the Podman AI Lab extension.
- Download and run a local LLM (e.g., `granite-7b` or `mistral`).
- Update `quarkus.langchain4j.openai.base-url` to the local model's API endpoint (e.g., `http://localhost:8080/v1`).
- Set a dummy API key and increase the timeout (`quarkus.langchain4j.openai.timeout=60s`).

### [ ] Create a RAG "Chat" Endpoint
- Create a new JAX-RS endpoint (e.g., `/chat`) that takes a user query.
- Inside the endpoint:
  1. **Retrieve**: Call `findSimilarContent(query)` to get relevant chunks.
  2. **Augment**: Combine the chunks into a single context string.
  3. **Generate**: Create a prompt (system message + context + user query) and send it to the `ChatLanguageModel`.
  4. Return the LLM's response.

### [ ] (Senior Feature) Implement Streaming Responses
- Refactor the chat endpoint to use `StreamingChatLanguageModel`.
- Change the endpoint's return type to `Multi<String>` (RESTEasy Reactive).
- This will stream the response token-by-token (as Server-Sent Events) for a much-improved user experience.

---

## Phase 2: Build a Resilient, Asynchronous Ingestion System

Make the document ingestion process robust and non-blocking, capable of handling large files and different formats.

### [ ] Create an Asynchronous Ingestion Endpoint
- Add `quarkus-smallrye-reactive-messaging-kafka` (or RabbitMQ/AMQP) dependency.
- Create a new `/ingest` endpoint that only accepts the document (e.g., as a file upload).
- The endpoint's only job is to put the document (and its title) onto a message queue (e.g., a Kafka topic named `document-ingestion`).

### [ ] Build an Ingestion Service
- Create a new service (e.g., `IngestionConsumer.java`).
- Use `@Incoming("document-ingestion")` to listen for new documents from the queue.
- This service will perform the heavy lifting:
  - Parse the document.
  - Split it into chunks (your existing logic).
  - Generate embeddings.
  - Store in Infinispan (`EmbeddingStore` and `documentIdsCache`).

### [ ] (Senior Feature) Add Multi-Format Support
- Add the **Apache Tika** library (`tika-parser-DefaultParser`) to your ingestion service.
- Use Tika to parse various file types (PDF, .docx, .txt, .md) into plain text before splitting.

---

## Phase 3: Add Production-Grade Observability

A RAG system fails silently by giving bad answers. Add tracing to see why.

### [ ] Integrate OpenTelemetry
- Add the `quarkus-opentelemetry` extension.
- Configure an exporter to send traces to a local collector (e.g., Jaeger or Grafana Tempo).

### [ ] Add Custom Spans
- Inject a `Tracer` into your RAG service.
- Add custom spans to your chat method to trace key steps:
  - `span("1. retrieve_context")` -> Wraps the call to `findSimilarContent`.
  - `span("2. augment_prompt")` -> Wraps the prompt-building logic.
  - `span("3. generate_response")` -> Wraps the call to the `ChatLanguageModel`.
- **Goal**: Be able to look at a single trace and see: "The user asked 'X', we retrieved context 'Y', and we sent prompt 'Z' to the LLM."

---

## Phase 4: Advanced Retrieval & Data Lifecycle

Improve the quality of retrieval and add essential data management features.

### [ ] (Senior Feature) Implement Re-Ranking
- Modify the retrieval logic:
  1. **Retrieve**: Fetch a larger number of chunks (e.g., top 10) from Infinispan.
  2. **Re-Rank**: (Optional, very advanced) Use a more powerful model (like a cross-encoder) to re-rank these 10 chunks based on their relevance to the specific query.
  3. **Augment**: Use the new top 3-5 re-ranked chunks for the prompt.

### [ ] Implement Document Deletion (Lifecycle Management)
- Create a `DELETE /document/{documentId}` endpoint.
- This endpoint must:
  - Remove the metadata from the `documentIdsCache`: `documentIdsCache.remove(documentId)`.
  - Remove all associated vectors from the `EmbeddingStore`. This requires a query/delete operation on the `EmbeddingStore` implementation to find and remove all segments where `metadata.document_id == documentId`.
