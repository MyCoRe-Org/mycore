package org.mycore.datamodel.metadata.neo4jToJson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Neo4JRelationShipJsonRecord(

      //Long start,

      String startElementId,

      //Long end,

      String endElementId,

      String type,

      Long id,

      String elementId,

      Map<String, String> properties
) {

   @Override public boolean equals(Object o) {
      if (this == o) {
         return true;
      }
      if (o == null || getClass() != o.getClass()) {
         return false;
      }
      Neo4JRelationShipJsonRecord that = (Neo4JRelationShipJsonRecord) o;
      return Objects.equals(startElementId, that.startElementId)
            && Objects.equals(endElementId, that.endElementId)
            && Objects.equals(type, that.type) && Objects.equals(id, that.id)
            && Objects.equals(elementId, that.elementId);
   }

   @Override public int hashCode() {
      return Objects.hash( startElementId, endElementId, type, id, elementId);
   }
}
