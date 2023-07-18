package org.mycore.datamodel.metadata.neo4jtojson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Neo4JNodeJsonRecord(

      List<String> type,

      String id,

      // String elementId,

      String mcrid,

      List<Neo4JMetaData> metadata
) {
   @Override public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      Neo4JNodeJsonRecord neo4JNode = (Neo4JNodeJsonRecord) o;
      return Objects.equals(id, neo4JNode.id); // && Objects.equals(elementId, neo4JNode.elementId);
   }

   @Override public int hashCode() {
      return Objects.hash(id);
   }
}
