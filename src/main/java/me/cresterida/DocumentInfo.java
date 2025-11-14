package me.cresterida;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Text;
import org.infinispan.protostream.annotations.Proto;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * A class representing indexed document metadata.
 * This object is stored as the VALUE in the 'document-ids' cache.
 * The KEY for the cache entry is the 'docId' string itself.
 *
 * The @Proto annotations are required to automatically generate
 * the marshaller that converts this object to/from the Protobuf format
 * used by the remote Infinispan server.
 */
@Indexed
@Proto
public record DocumentInfo(
        @Text
        String docId,
        @Text
           String title){

}


