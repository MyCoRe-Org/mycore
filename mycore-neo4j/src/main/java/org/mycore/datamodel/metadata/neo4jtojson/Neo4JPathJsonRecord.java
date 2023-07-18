package org.mycore.datamodel.metadata.neo4jtojson;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonRootName("p")
public record Neo4JPathJsonRecord(
      @JsonProperty("nodes")
      List<Neo4JNodeJsonRecord> nodes,

      @JsonProperty("relationships")
      List<Neo4JRelationShipJsonRecord> relationships
      // List<Neo4JSegment> segments
) {
}
