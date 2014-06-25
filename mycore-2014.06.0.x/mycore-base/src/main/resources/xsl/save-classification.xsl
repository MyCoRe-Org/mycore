<?xml version="1.0" encoding="UTF-8"?>

<!-- ============================================== -->
<!-- $Revision: 1.1 $ $Date: 2008/07/23 05:25:54 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
> 

<xsl:output method="xml" encoding="UTF-8" indent="yes"/>

<xsl:variable name="newline">
<xsl:text>
</xsl:text>
</xsl:variable>

<xsl:template match="/">
  <mycoreclass>
    <xsl:copy-of select="mycoreclass/@ID"/>
    <xsl:copy-of select="mycoreclass/@xsi:noNamespaceSchemaLocation"/>
	<xsl:value-of select="$newline" />
	<xsl:for-each select="mycoreclass/label">
	  <xsl:copy-of select="." />		
	  <xsl:value-of select="$newline" />
	</xsl:for-each>
	<categories>
      <xsl:for-each select="mycoreclass/categories/category">
        <xsl:apply-templates select="." />
	    <xsl:value-of select="$newline" />
	  </xsl:for-each>
	</categories>
	<xsl:value-of select="$newline" />
    <service>
	  <xsl:value-of select="$newline" />
	  <xsl:copy-of select="mycoreclass/service/*"/>
	  <!-- include acl if available -->
	  <xsl:value-of select="$newline" />
	  <xsl:variable name="acl" select="document(concat('access:action=all&amp;object=',mycoreclass/@ID))"/>
      <xsl:if test="$acl/*/*">
	    <xsl:copy-of select="$acl"/>
	  </xsl:if>
	</service>
  </mycoreclass>
</xsl:template>

<xsl:template match="category">
  <category>
    <xsl:attribute name="ID">
      <xsl:value-of select="@ID" />
    </xsl:attribute>
	<xsl:value-of select="$newline" />
    <xsl:for-each select="label">
      <xsl:copy-of select="." />
	  <xsl:value-of select="$newline" />
	</xsl:for-each>
    <xsl:for-each select="url">
      <xsl:copy-of select="." />
	  <xsl:value-of select="$newline" />
	</xsl:for-each>
	<xsl:if test="category">
        <xsl:apply-templates select="category" />
	    <xsl:value-of select="$newline" />		
	</xsl:if>
  </category>
  <xsl:value-of select="$newline" />		
</xsl:template>
	
</xsl:stylesheet>

