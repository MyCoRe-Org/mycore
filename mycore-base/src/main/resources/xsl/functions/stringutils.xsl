<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:array="http://www.w3.org/2005/xpath-functions/array"
                xmlns:math="http://www.w3.org/2005/xpath-functions/math"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrstring="http://www.mycore.de/xslt/stringutils"
                
                exclude-result-prefixes="fn xs">

    <xsl:function name="mcrstring:shorten" as="xs:string">
        <xsl:param name="input" as="xs:string"/>
        <xsl:param name="length" as="xs:integer"/>
        <xsl:choose>
          <xsl:when test="string-length($input) &lt;=$length">
            <xsl:value-of select="$input" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(substring($input, 1, $length),substring-before(substring($input,$length+1), ' '), '...')" />
          </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

  <xsl:function name="mcrstring:abbreviate-center" as="xs:string">
    <xsl:param name="input" as="xs:string" />
    <xsl:param name="length" as="xs:integer" />
    <xsl:choose>
      <xsl:when test="string-length($input) &lt;=$length">
        <xsl:value-of select="$input" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of
          select="concat(substring($input, 1, $length div 2),'…', substring($input, string-length($input) - ($length div 2)))" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

</xsl:stylesheet>
