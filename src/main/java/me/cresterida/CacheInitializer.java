package me.cresterida;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.StringConfiguration;
// We inject your generated schema
import me.cresterida.DocumentInfoSchema;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CacheInitializer {

    private static final Logger LOGGER = Logger.getLogger(CacheInitializer.class);

    @Inject
    RemoteCacheManager remoteCacheManager;


    void onStart(@Observes StartupEvent ev)
    {
        LOGGER.info("Starting manual schema registration...");



        // 2. Get the special cache for storing protobuf schemas
        String cacheName = "document-ids";
        String cacheConfigXml = "<distributed-cache name=\"" + cacheName + "\">" +
                "<encoding media-type=\"application/x-protostream\"/>" +
                "<indexing enabled=\"true\" storage=\"local-heap\">" +
                "    <indexed-entities>" +
                "        <indexed-entity>me.cresterida.DocumentInfo</indexed-entity>" +
                "    </indexed-entities>" +
                "</indexing>" +
                "</distributed-cache>";

        LOGGER.info("Creating cache '" + cacheName + "'...");
        remoteCacheManager.administration()
                .getOrCreateCache(cacheName, new StringConfiguration(cacheConfigXml));

        LOGGER.info("Cache '" + cacheName + "' created successfully.");
    }
}
