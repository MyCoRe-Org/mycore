package org.mycore.datamodel.metadata.neo4jToJson;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public record Neo4JMetaData(
      @JsonProperty
      String title,

      @JsonProperty
      List<String> content
) {
}
