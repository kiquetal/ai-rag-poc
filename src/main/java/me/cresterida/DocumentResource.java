package me.cresterida;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/documents")
public class DocumentResource {

    @Inject
    Ingestor ingestor;

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response ingest(String document) {
        ingestor.ingest(document);
        return Response.accepted().build();
    }
}