package org.mycore.solr.cloud.collection;

import org.mycore.solr.MCRSolrIndex;

public interface MCRSolrCloudCollection extends MCRSolrIndex {

  Integer getNumShards();

  Integer getNumNrtReplicas();

  Integer getNumTlogReplicas();

  Integer getNumPullReplicas();

  String getConfigSetTemplate();
}
