<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns="http://www.openarchives.org/OAI/2.0/"
  xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
  xmlns:dc="http://purl.org/dc/elements/1.1/">
  
<xsl:output method="xml" encoding="UTF-8"/>
  
<xsl:template match="/mycoreclass">
  <ListSets>
    <xsl:apply-templates select="categories/category">
      <xsl:with-param name="prefix" select="@ID" />
    </xsl:apply-templates>
  </ListSets>
</xsl:template>

<xsl:template match="category">
  <xsl:param name="prefix" />
  
  <xsl:variable name="id" select="concat($prefix,':',@ID)" />
  
  <set>
    <setSpec>
      <xsl:value-of select="$id" />
    </setSpec>
    <setName>
      <xsl:value-of select="label[lang('de')]/@text" />
    </setName>
  </set>

  <xsl:apply-templates select="category">
    <xsl:with-param name="prefix" select="$id" />
  </xsl:apply-templates>
</xsl:template>

</xsl:stylesheet>
