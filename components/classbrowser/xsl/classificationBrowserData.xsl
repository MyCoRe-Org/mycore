<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.9 $ $Date: 2007-10-15 09:58:16 $ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
>

<xsl:output method="xml" omit-xml-declaration="yes" />

<xsl:template match="/classificationBrowserData">
  <ul class="cbList">
    <xsl:for-each select="category">
      <xsl:variable name="id" select="concat(../@classification,'_',@id)" />
      <li>
        <xsl:if test="@children = 'true'">
          <input id="cbButton_{$id}" type="button" value="+" onclick="toogle('{../@classification}','{@id}');" />
        </xsl:if>
        <span class="cbID"><xsl:value-of select="@id" /></span>
        <span class="cbLabel"><xsl:value-of select="@label" /></span>
        <xsl:if test="@children = 'true'">
          <div id="cbChildren_{$id}" class="cbHidden" />
        </xsl:if>
      </li>
    </xsl:for-each>
  </ul>
</xsl:template>

</xsl:stylesheet>
