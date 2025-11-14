package me.cresterida;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.StringConfiguration;

@ApplicationScoped
public class CacheInitializer {

    @Inject
    RemoteCacheManager remoteCacheManager; // Inject the main cache manager

    /**
     * This method is automatically called by Quarkus when the application starts.
     * It ensures our cache is created with the correct configuration.
     */
    void onStart(@Observes StartupEvent ev)
    {
        String cacheName = "document-ids";

        // 1. Define the cache configuration as an XML string.
        // This is the same logic from the properties file, but now in Java.
        String cacheConfigXml = "<distributed-cache name=\"" + cacheName + "\">" +
                "<encoding media-type=\"application/x-protostream\"/>" +
                "<indexing enabled=\"true\" storage=\"local-heap\"/>" +
                "</distributed-cache>";

        // 2. Use the administration API to create (or get) the cache
        // with this specific configuration.
        // This is an idempotent call, so it's safe to run every time.
        remoteCacheManager.administration()
                .getOrCreateCache(cacheName, new StringConfiguration(cacheConfigXml));

    }
}
