<?xml version="1.0" encoding="iso-8859-1"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2005-01-04 10:45:23 $ -->
<!-- ============================================== -->

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  >

<xsl:output 
  method="xml" 
  encoding="UTF-8"
  />

<xsl:template match="/root">
<mcr_search_results>
 <xsl:for-each select="mycoreobject" >
  <mcr_search_result>
   <xsl:attribute name="ID">
    <xsl:value-of select="@ID" />
   </xsl:attribute>
  </mcr_search_result>
 </xsl:for-each>
</mcr_search_results>
</xsl:template>

</xsl:stylesheet>

