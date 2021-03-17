<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink" 

  exclude-result-prefixes="xlink mods fn">
  
  <xsl:template mode="mods.type" match="mods:mods">
    <xsl:choose>
      <xsl:when
        test="substring-after(mods:genre[@type='intern']/@valueURI,'#')='article' or
                (mods:relatedItem/mods:genre='periodical' and mods:identifier/@type='doi')">
        <xsl:value-of select="'article'" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring-after(mods:genre[@type='intern']/@valueURI,'#')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
</xsl:stylesheet>  