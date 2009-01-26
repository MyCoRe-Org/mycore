<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2008/12/01 13:34:24 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
>
<xsl:output method="xml" encoding="UTF-8" />

<xsl:param name="DefaultLang" />
<xsl:param name="CurrentLang" />

<xsl:template match="/mycoreobject">
 <item>
  <xsl:attribute name="ID">
   <xsl:value-of select="@ID" />
  </xsl:attribute>
  <!-- Title -->
  <label>
   <xsl:choose>
    <xsl:when test="@label">
     <xsl:value-of select="@label" />	   
   </xsl:when>
   <xsl:otherwise>
    <xsl:text>metadata object</xsl:text>
   </xsl:otherwise>
   </xsl:choose>
  </label>
  <!-- Create Date -->
  <xsl:if test="service/servdates/servdate">
   <data>
   <xsl:for-each select="service/servdates/servdate">
    <xsl:if test="@type = 'modifydate'">
     <xsl:value-of select="i18n:translate('component.swf.converter.modifydate')" /><xsl:value-of select="text()|*" />
    </xsl:if>
   </xsl:for-each>
   </data>
  </xsl:if>
 </item>
</xsl:template>

</xsl:stylesheet>

