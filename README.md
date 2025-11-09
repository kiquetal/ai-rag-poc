# ai-rag-poc

Small Quarkus project POC. Added a simple REST resource for testing:

- GET /hello -> returns a plain text greeting

Quick run (dev mode):

```bash
./mvnw quarkus:dev
```

Or build and run:

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

Then open http://localhost:8081/hello

