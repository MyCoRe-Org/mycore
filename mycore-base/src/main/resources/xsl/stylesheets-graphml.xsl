<?xml version="1.0" encoding="ISO-8859-1"?>

<!--Transforms output of MCRXSLInfoServlet to build a GraphML graph of all *.xsl stylesheets and their dependencies -->

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="http://graphml.graphdrawing.org/xmlns" 
  exclude-result-prefixes="xsl">

<xsl:output method="xml" />

<xsl:template match="/">
  <graphml xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd">
    <key id="label" for="node" attr.name="label" attr.type="string"/>
    <key id="label" for="edge" attr.name="label" attr.type="string"/>
    <key id="color" for="node" attr.name="color" attr.type="string"/>
    <xsl:apply-templates select="stylesheets" />
  </graphml>
</xsl:template>

<xsl:template match="stylesheets">
  <graph edgedefault="directed">
    <xsl:apply-templates select="stylesheet" />
    <xsl:apply-templates select="stylesheet/includes|stylesheet/imports" />
  </graph>
</xsl:template>

<xsl:template match="stylesheet">
  <node id="{generate-id(.)}">
    <data key="label">
      <xsl:value-of select="@name" />
    </data>
    <data key="color">
      <xsl:choose>
        <xsl:when test="origin[contains(text(),'classes/xsl')]">#4CC417</xsl:when> <!-- green -->
        <xsl:when test="origin[contains(text(),'.jar')]">#82CAFF</xsl:when> <!-- blue -->
        <xsl:when test="origin[contains(text(),'URIResolver')]">#FFFF00</xsl:when> <!-- yellow -->
        <xsl:when test="not(origin)">#FF0000</xsl:when> <!-- red -->
      </xsl:choose>
    </data>
  </node>
</xsl:template>

<xsl:template match="includes|imports">
  <edge id="{generate-id(.)}" source="{generate-id(..)}" target="{generate-id(/stylesheets/stylesheet[@name=current()])}">
    <data key="label">
      <xsl:value-of select="name()" />
    </data>
  </edge>
</xsl:template>

</xsl:stylesheet>
