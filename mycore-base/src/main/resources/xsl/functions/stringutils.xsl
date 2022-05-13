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
    <xsl:param name="input" as="xs:string" />
    <xsl:param name="length" as="xs:integer" />
    <xsl:choose>
      <xsl:when test="string-length($input) &lt;=$length">
        <xsl:value-of select="$input" />
      </xsl:when>
      <xsl:when test="substring($input, $length+1, 1) = ' '">
        <xsl:value-of select="substring($input, 1, $length)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="t" select="tokenize(substring($input, 1, $length),' ')" />
        <xsl:value-of select="if (count($t) &lt;= 1) 
                              then (substring($input, 1, $length)) 
                              else (string-join(remove($t, count($t)), ' '))" />
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
  
  <xsl:function name="mcrstring:pretty-filesize" as="xs:string">
    <xsl:param name="size" as="xs:integer" />
    <xsl:variable name="suffixes" select="['bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']" />
    <xsl:for-each select="min((1 to array:size($suffixes))[$size &lt; math:pow(1024, .)])">
      <xsl:variable name="out" select="$size div math:pow(1024, . - 1)" />
      <xsl:choose>
        <xsl:when test="$out &gt;= 100 or (round($out * 100) = $out * 100) ">
          <xsl:value-of select="concat(format-number($out, '###'),' ', array:get($suffixes, .))" />
        </xsl:when>
        <xsl:when test="$out &gt;= 10">
          <xsl:value-of select="concat(format-number($out, '##.#'),' ', array:get($suffixes, .))" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat(format-number($out, '#.##'),' ', array:get($suffixes, .))" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:function>
    
</xsl:stylesheet>
