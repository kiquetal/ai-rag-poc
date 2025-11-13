package me.cresterida;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;

/**
 * A class representing indexed document metadata.
 * This object is stored as the VALUE in the 'document-ids' cache.
 * The KEY for the cache entry is the 'docId' string itself.
 */
@Indexed
public class DocumentInfo {

    private final String docId;
    private final String title;

    public DocumentInfo(String docId, String title) {
        this.docId = docId;
        this.title = title;
    }

    /**
     * The unique ID of the document.
     * This field is also used as the KEY for the entry in the Infinispan cache.
     * Making it projectable and sortable allows for efficient queries.
     */
    @Basic(projectable = true, sortable = true)
    public String getDocId() {
        return docId;
    }

    /**
     * The title of the document.
     * This field is indexed for searching.
     */
    @Basic(projectable = true, sortable = true)
    public String getTitle() {
        return title;
    }
}
