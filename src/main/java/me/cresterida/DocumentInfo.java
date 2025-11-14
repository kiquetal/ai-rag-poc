package me.cresterida;

import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Keyword;
import org.infinispan.api.annotations.indexing.Text;
import org.infinispan.protostream.annotations.Proto;

@Proto
@Indexed
public record DocumentInfo(

        /**
         * @Keyword - Indexes this field as a single, exact-match token.
         *            Ideal for IDs, status codes, or fields used
         *            for sorting, aggregation, or faceting.
         *            'projectable = true' allows retrieving just this
         *            field from a query.
         */
        @Keyword(projectable = true, sortable = true)
        String docId,

        /**
         * @Text - Indexes this field for full-text search.
         *         The content will be analyzed, tokenized,
         *         and lowercased, allowing searches for
         *         individual words within the title.
         */
        @Text(projectable = true)
        String title
) {
    // With Java 16+ Records, @Proto is sufficient.
    // For a traditional POJO, you would use @ProtoMessage on the class
    // and @ProtoField(number = N) on each getter or field. [9, 11]
}
