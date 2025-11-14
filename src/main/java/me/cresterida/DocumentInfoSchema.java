package me.cresterida;

import jakarta.inject.Singleton;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;

@ProtoSchema(includeClasses =  DocumentInfo.class,schemaPackageName = "me.cresterida")
public interface DocumentInfoSchema extends GeneratedSchema {
}
