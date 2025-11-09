# ai-rag-poc

Lightweight Quarkus proof-of-concept that demonstrates a minimal REST resource and integrates with langchain4j components.

Inferred project details (from `pom.xml`):

- Framework: Quarkus (platform BOM version 3.29.2)
- Java: target/release set to Java 21
- Key dependencies:
  - `quarkus-websockets-next` (websocket support)
  - `quarkus-arc` (CDI / dependency injection)
  - `quarkiverse.langchain4j` modules (Infinispan and AI Gemini integrations)
  - `langchain4j-embeddings-bge-small-en-q` (embedding model used by langchain4j)
- Build plugins:
  - `quarkus-maven-plugin` (generates code and supports native image workflows)
  - `maven-compiler-plugin` and surefire/failsafe configured for tests

What I added for quick testing

- A minimal REST resource at `GET /hello` implemented in `src/main/java/me/cresterida/GreetingResource.java`.
- `src/main/resources/application.properties` sets `quarkus.http.port=8081` so the app listens on port 8081 by default.

Run / build instructions

- Development mode (recommended while developing):

```bash
./mvnw quarkus:dev
```

The dev server will start and you can open:

http://localhost:8081/hello

- Build and run the JVM artifact:

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

- Build a native executable (on a machine configured for GraalVM/native-image):

```bash
# This activates the `native` profile (see `pom.xml`), and will run the native build
./mvnw -Pnative package
```

Notes and helpful hints

- Tests: The project includes `quarkus-junit5` in `pom.xml`. The current quick build used `-DskipTests` to speed up verification; run `./mvnw test` to execute unit tests.
- The `quarkus-maven-plugin` in the `pom.xml` contains goals such as `generate-code` and `native-image-agent` used for native-image preparation and Quarkus codegen.
- `skipITs` property is present in the `pom.xml` to control integration tests; the `native` profile sets `skipITs=false` to run integration tests for native builds.
- If you plan to expose API docs, consider adding `quarkus-smallrye-openapi` and `quarkus-smallrye-swagger-ui` to the dependencies for automatic OpenAPI generation and Swagger UI.

Next steps (optional)

- Add a small JUnit test that exercises `/hello` (I can add this for you).
- Add OpenAPI and Swagger UI for API exploration.
- Add a JSON response DTO and example client code.

---

`/hello` quickcheck

- After starting the app, visit: http://localhost:8081/hello
- Expected response: plain text `Hello from ai-rag-poc`
