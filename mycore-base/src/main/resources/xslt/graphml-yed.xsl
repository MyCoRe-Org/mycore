<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:g="http://graphml.graphdrawing.org/xmlns"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:y="http://www.yworks.com/xml/graphml"
  xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd"
  exclude-result-prefixes="g">
  <xsl:include href="copynodes.xsl" />
  <xsl:template match="g:graphml">
    <graphml xmlns="http://graphml.graphdrawing.org/xmlns" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:y="http://www.yworks.com/xml/graphml"
      xmlns:yed="http://www.yworks.com/xml/yed/3" xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd">
      <key for="node" id="ng" yfiles.type="nodegraphics" />
      <key for="edge" id="eg" yfiles.type="edgegraphics" />
      <xsl:apply-templates select="g:graph" />
    </graphml>
  </xsl:template>
  <xsl:template match="g:node">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <g:data key="ng">
        <y:ShapeNode>
          <y:Fill color="{normalize-space(g:data[@key='color'])}" />
          <y:NodeLabel>
            <xsl:value-of select="normalize-space(g:data[@key='label'])" />
          </y:NodeLabel>
          <y:Shape type="ellipse" />
        </y:ShapeNode>
      </g:data>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="g:edge">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <g:data key="eg">
        <y:PolyLineEdge>
          <y:EdgeLabel>
            <xsl:value-of select="normalize-space(g:data[@key='label'])" />
          </y:EdgeLabel>
          <y:Arrows source="none" target="standard" />
        </y:PolyLineEdge>
      </g:data>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>