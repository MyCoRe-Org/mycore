<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:mcr="http://www.mycore.org/" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:gnd="http://d-nb.info/gnd/" xmlns:java="http://xml.apache.org/xalan/java"
  xmlns:char="character" exclude-result-prefixes="gnd rdf mcrmods mcr xalan java" version="1.0">

  <char:char ent="lt">&lt;</char:char>
  <char:char ent="gt">&gt;</char:char>
  <char:char ent="amp">&amp;</char:char>
  <char:char ent="apos">&apos;</char:char>
  <char:char ent="quot">&quot;</char:char>


  <xsl:output
    method="xml"
    version="1.0"
    indent="yes"
    encoding="UTF-8" />

  <xsl:template match="/">
    <xsl:call-template name="parse">
     <xsl:with-param name="str" select="."/>
  </xsl:call-template>
  </xsl:template>

  <xsl:template name="parse">
    <xsl:param name="str" select="." />
    <xsl:choose>
      <xsl:when test="contains($str,'&lt;')">
        <xsl:variable name="tag"
          select="substring-before(substring-after($str,'&lt;'),'&gt;')" />
        <xsl:variable name="endTag">
          <xsl:choose>
            <xsl:when test="contains($tag,' ')">
              <xsl:value-of select="substring-before($tag,' ')" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$tag" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="parse">
          <xsl:with-param name="str"
            select="substring-before($str,concat('&lt;',$tag,'&gt;'))" />
        </xsl:call-template>
        <xsl:call-template name="parseTag">
          <xsl:with-param name="tag" select="$tag" />
          <xsl:with-param name="endTag" select="normalize-space($endTag)" />
          <xsl:with-param name="value"
            select="substring-before(substring-after($str,concat('&lt;',$tag,'&gt;')),concat('&lt;/',normalize-space($endTag),'&gt;'))" />
        </xsl:call-template>
        <xsl:choose>
          <xsl:when test="substring($tag,string-length($tag))='/'">
            <xsl:call-template name="parse">
              <xsl:with-param name="str"
                select="substring-after($str,concat('&lt;',$tag,'&gt;'))" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="parse">
              <xsl:with-param name="str"
                select="substring-after($str,concat('&lt;/',normalize-space($endTag),'&gt;'))" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <!-- xsl:value-of select="$str" / -->
        <xsl:call-template name="normalize-text">
          <xsl:with-param name="text" select="$str" />
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="parseTag">
    <xsl:param name="tag" select="''" />
    <xsl:param name="endTag" select="''" />
    <xsl:param name="value" select="''" />
    <xsl:element name="{translate($endTag,'/','')}">
      <xsl:call-template name="attribs">
        <xsl:with-param name="attrlist"
          select="substring-after(normalize-space($tag),' ')" />
      </xsl:call-template>
      <xsl:call-template name="parse">
        <xsl:with-param name="str" select="$value" />
      </xsl:call-template>
    </xsl:element>
  </xsl:template>
  <xsl:template name="attribs">
    <xsl:param name="attrlist" select="''" />
    <xsl:variable name="name"
      select="normalize-space(substring-before($attrlist,'='))" />
    <xsl:if test="$name">
      <xsl:variable name="value">
        <xsl:choose>
          <xsl:when test="substring-before($attrlist,'=&quot;')">
            <xsl:value-of
              select="substring-before(substring-after($attrlist,'=&quot;'),'&quot;')" />
          </xsl:when>
          <xsl:when test="substring-before($attrlist,'= &quot;')">
            <xsl:value-of
              select="substring-before(substring-after($attrlist,'= &quot;'),'&quot;')" />
          </xsl:when>
          <xsl:when test="substring-before($attrlist,&quot;=&apos;&quot;)">
            <xsl:value-of
              select="substring-before(substring-after($attrlist,&quot;=&apos;&quot;),&quot;&apos;&quot;)" />
          </xsl:when>
          <xsl:when test="substring-before($attrlist,&quot;= &apos;&quot;)">
            <xsl:value-of
              select="substring-before(substring-after($attrlist,&quot;=&apos;&quot;),&quot;&apos;&quot;)" />
          </xsl:when>
        </xsl:choose>
      </xsl:variable>
      <xsl:attribute name="{$name}">
        <xsl:value-of select="$value" />
      </xsl:attribute>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="contains($attrlist,' ')">
        <xsl:call-template name="attribs">
          <xsl:with-param name="attrlist"
            select="substring-after($attrlist,' ')" />
        </xsl:call-template>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template name="normalize-text">
    <xsl:param name="text" />
      <xsl:choose>
        <xsl:when test="contains($text,'&amp;')">
          <xsl:variable name="vAfter" select="substring-after($text,'&amp;')" />
          <xsl:value-of
            select="concat(substring-before($text,'&amp;'),
                                                   document('')/*/char:*
                                                   [@ent =
                                                   substring-before($vAfter,';')])"
            disable-output-escaping="yes" />
          <xsl:call-template name="normalize-text">
            <xsl:with-param name="text" select="substring-after($vAfter,';')" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$text" />
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>

</xsl:stylesheet>