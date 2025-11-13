package me.cresterida;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;

/**
 * A class representing indexed document metadata.
 * This object is stored in the 'document-ids' cache.
 */
@Indexed
public class DocumentInfo {

    private final String docId;
    private final String title;

    public DocumentInfo(String docId, String title) {
        this.docId = docId;
        this.title = title;
    }

    // The docId will be the primary key, but we make it searchable as well.
    @Basic(projectable = true, sortable = true)
    public String getDocId() {
        return docId;
    }

    // The title of the document, which we can use for full-text search.
    @Basic(projectable = true, sortable = true)
    public String getTitle() {
        return title;
    }
}
