<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision$ $Date$ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
      <xsl:output method="xml" encoding="UTF-8" />
      <xsl:template match="/">
            <items>
                  <xsl:apply-templates select="mcr_results/mcr_result" >
				<xsl:sort select="mycoreobject/metadata/names/name/surname" order="ascending" />  	                        		                        
                  </xsl:apply-templates>
            </items>
      </xsl:template>
      <xsl:template match="mycoreobject">
            <item value="{@ID}">
                  <xsl:apply-templates select="metadata/names/name" />
            </item>
      </xsl:template>
      <xsl:template match="name">
            <label>
                  <xsl:copy-of select="@xml:lang" />
                  <xsl:value-of select="concat(surname,', ',firstname)" />
            </label>
      </xsl:template>
</xsl:stylesheet>