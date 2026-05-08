<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:array="http://www.w3.org/2005/xpath-functions/array"
  xmlns:math="http://www.w3.org/2005/xpath-functions/math"
  xmlns:mcrstringutils="http://www.mycore.de/xslt/stringutils"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:function name="mcrstringutils:shorten" as="xs:string">
    <xsl:param name="input" as="xs:string" />
    <xsl:param name="length" as="xs:integer" />
    <xsl:value-of select="mcrstringutils:shorten($input, $length, '…')" />
  </xsl:function>

  <!-- Function shortens the text to the desired length and then adds the ellipsis. Thus, you might need to substract
    the length of the ellipsis from the length parameter to match the actual desired length.
   -->
  <xsl:function name="mcrstringutils:shorten" as="xs:string">
    <xsl:param name="input" as="xs:string" />
    <xsl:param name="length" as="xs:integer" />
    <xsl:param name="ellipsis" as="xs:string" />
    <xsl:choose>
      <xsl:when test="string-length($input) &lt;=$length">
        <xsl:value-of select="$input" />
      </xsl:when>
      <xsl:when test="substring($input, $length+1, 1) = ' '">
        <xsl:value-of select="concat(substring($input, 1, $length), $ellipsis)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="t" select="tokenize(substring($input, 1, $length),' ')" />
        <xsl:value-of select="if (count($t) &lt;= 1) 
                              then (concat(substring($input, 1, $length), $ellipsis)) 
                              else (concat(string-join(remove($t, count($t)), ' '), $ellipsis))" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:function name="mcrstringutils:abbreviate-center" as="xs:string">
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
  
  <xsl:function name="mcrstringutils:pretty-filesize" as="xs:string">
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

  <xsl:function name="mcrstringutils:trim" as="xs:string">
    <xsl:param name="input" as="xs:string" />
    <xsl:value-of select="replace($input, '^\s+|\s+$', '')" />
  </xsl:function>

  <xsl:function name="mcrstringutils:capitalize" as="xs:string?">
    <xsl:param name="s" as="xs:string?"/>
    <xsl:sequence select="
    if (empty($s) or $s = '') then $s
    else concat(upper-case(substring($s, 1, 1)), substring($s, 2))
  "/>
  </xsl:function>
    
</xsl:stylesheet>
