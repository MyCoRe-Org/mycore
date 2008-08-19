<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2008/07/23 05:25:54 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
> 

<xsl:output method="xml" encoding="UTF-8"/>

<xsl:variable name="newline">
<xsl:text>
</xsl:text>
</xsl:variable>

<xsl:attribute-set name="tag">
  <xsl:attribute name="class">
    <xsl:value-of select="./@class" />
  </xsl:attribute>
  <xsl:attribute name="heritable">
    <xsl:value-of select="./@heritable" />
  </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="subtag">
  <xsl:attribute name="sourcepath">
    <xsl:value-of select="/mycorederivate/@ID"/>
   </xsl:attribute>
   <xsl:attribute name="maindoc">
     <xsl:value-of select="@maindoc"/>
   </xsl:attribute>
   <xsl:attribute name="ifsid">
     <xsl:value-of select="@ifsid"/>
   </xsl:attribute>
</xsl:attribute-set>

<xsl:template match="/">
  <mycorederivate>
    <xsl:copy-of select="mycorederivate/@ID"/>
    <xsl:copy-of select="mycorederivate/@label"/>
    <xsl:copy-of select="mycoreobject/@version"/>
    <xsl:copy-of select="mycorederivate/@xsi:noNamespaceSchemaLocation"/>
    <derivate>
      <xsl:if test="mycorederivate/derivate/linkmetas">
        <xsl:copy-of select="mycorederivate/derivate/linkmetas"/>
	  </xsl:if>
      <xsl:if test="mycorederivate/derivate/titles">
        <xsl:copy-of select="mycorederivate/derivate/titles"/>
	  </xsl:if>
      <xsl:if test="mycorederivate/derivate/externals">
        <xsl:copy-of select="mycorederivate/derivate/externals"/>
	  </xsl:if>
      <xsl:for-each select="mycorederivate/derivate/internals">
        <xsl:copy use-attribute-sets="tag">
          <xsl:for-each select="internal">
            <xsl:copy use-attribute-sets="subtag" />
          </xsl:for-each>
        </xsl:copy>
      </xsl:for-each>
    </derivate>
    <service>
	  <xsl:copy-of select="mycoreobject/service/*"/>
	  <!-- include acl if available -->
	  <xsl:variable name="acl" select="document(concat('access:action=all&amp;object=',mycorederivate/@ID))"/>
      <xsl:if test="$acl/*/*">
	    <!-- acl are available -->
	    <xsl:copy-of select="$acl"/>
	  </xsl:if>
	</service>
  </mycorederivate>
</xsl:template>

</xsl:stylesheet>