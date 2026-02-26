<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns="http://www.openarchives.org/OAI/2.0/"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:output method="xml" encoding="UTF-8" />

  <xsl:template match="/response">
    <ListSets>
      <xsl:apply-templates select="result/doc" />
    </ListSets>
  </xsl:template>

  <xsl:template match="doc">
    <set>
      <setSpec>
        <xsl:value-of select="str[@name='id']/text()" />
      </setSpec>
      <setName>
        <xsl:value-of select="str[@name='maintitle']/text()" />
      </setName>
    </set>
  </xsl:template>

</xsl:stylesheet>
