<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan encoder">

  <xsl:template match="/response">
    <xsl:copy>
      <xsl:copy-of select="@*|node()" />
      <xsl:if test="result/doc">
        <xsl:variable name="orChain">
          <xsl:apply-templates mode="query" select="result/doc/@id" />
        </xsl:variable>
        <xsl:variable name="query">
          <xsl:value-of select="'+objectType:derivate +returnId:('" />
          <xsl:value-of select="substring-after($orChain, 'OR ')" />
          <xsl:value-of select="')'" />
        </xsl:variable>
        <xsl:apply-templates select="document(concat('solr:fl=id,returnId,maindoc,iviewFile&amp;rows=',(count(result/doc)*10),'&amp;q=', encoder:encode($query)))" mode="derivate"/>
      </xsl:if>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="response" mode="derivate">
    <xsl:copy>
      <xsl:attribute name="subresult">
        <xsl:value-of select="'derivate'" />
      </xsl:attribute>
      <xsl:copy-of select="@*|node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template mode="query" match="@id">
    <xsl:value-of select="concat(' OR ',.)" />
  </xsl:template>

</xsl:stylesheet>