package me.cresterida;

import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

@Path("/embed")
public class EmbeddingResource {

    @Inject
    EmbeddingModel embeddingModel;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Float> embed(String text)
    {
        float[] vectors =
                embeddingModel.embed(text).content().vector();


        return IntStream.range(0, vectors.length)
                .mapToObj(i -> vectors[i])
                .toList();
    }
}
