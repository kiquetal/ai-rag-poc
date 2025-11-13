package me.cresterida;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/documents")
public class DocumentResource {

    public static class IngestRequest {
        public String title;
        public String text;
    }

    @Inject
    Ingestor ingestor;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response ingest(IngestRequest ingestRequest) {
        ingestor.ingest(ingestRequest.title, ingestRequest.text);
        return Response.accepted().build();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    public long count() {
        return ingestor.getDocumentCount();
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DocumentInfo> search(@QueryParam("title") String title) {
        return ingestor.searchByTitle(title);
    }
}
