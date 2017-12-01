<?xml version="1.0" encoding="UTF-8"?>

<x:stylesheet version="1.0" 
  xmlns:x="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xsl="http://www.w3.org/1999/XSL/TransformAlias"
  xmlns:xalan="http://xml.apache.org/xalan"
  exclude-result-prefixes="xalan" 
>

  <x:namespace-alias stylesheet-prefix="xsl" result-prefix="x" />
  
  <x:output method="xml" indent="yes" xalan:indent-amount="2" />
  
  <x:variable name="suffix">_values_</x:variable>
  
  <x:template match="/">
    <x:text disable-output-escaping="yes">&#xa;&#xa;</x:text>
    <xsl:stylesheet>
      <x:call-template name="copyNamespaceNodes" />
  
      <x:text disable-output-escaping="yes">&#xa;</x:text>
      <xsl:template match="/">
        <xsl:apply-templates select="{name(*)}" />
      </xsl:template>
     
      <x:apply-templates select="*" />
      <x:call-template name="writeDefaultTemplates" />
    </xsl:stylesheet>
  </x:template>
  
  <x:template name="copyNamespaceNodes">
    <x:for-each select="descendant::*|descendant::*//@*">
      <x:choose>
        <x:when test="not(contains(name(),':'))" />
        <x:when test="starts-with(name(),'xml:')" />
        <x:when test="contains(name(),$suffix)" />
        <x:when test="ancestor::*[namespace-uri()=namespace-uri(current())]" />
        <x:when test="preceding::*[namespace-uri()=namespace-uri(current())]" />
        <x:when test="preceding::*/@*[namespace-uri()=namespace-uri(current())]" />
        <x:otherwise>
          <x:attribute name="{substring-before(name(),':')}:dummy" namespace="{namespace-uri(.)}" />
        </x:otherwise>
      </x:choose>
    </x:for-each>
  </x:template>

  <x:template name="writeDefaultTemplates">
    <x:text disable-output-escaping="yes">&#xa;&#xa;</x:text>
    <xsl:template match="@*" /> <x:comment>  remove unsupported attributes </x:comment>
    <xsl:template match="*|text()"> <x:comment> remove unsupported elements and text </x:comment>
      <xsl:comment>
        <xsl:copy-of select="." />
      </xsl:comment>
    </xsl:template>

    <x:text disable-output-escaping="yes">&#xa;&#xa;</x:text>
    <x:comment> this is called for all supported elements: </x:comment>
    <xsl:template name="copyAndApply">
      <xsl:copy>
        <xsl:apply-templates select="@*|node()" />
      </xsl:copy>  
    </xsl:template>
  </x:template>
  
  <x:template match="*">
    <x:text disable-output-escaping="yes">&#xa;</x:text>
    <xsl:template>
      <x:attribute name="match"> 
        <x:apply-templates select=".|parent::*|parent::*/parent::*" mode="nameWithPredicates" /> <!-- pa and grandpa only, can't use .. here -->
      </x:attribute>
      <xsl:call-template name="copyAndApply" />
    </xsl:template>
    <x:apply-templates select="@*" />
    <x:comment> ========== </x:comment>
    <x:apply-templates select="*" />
  </x:template>
  
  <x:template match="@*">
    <x:text disable-output-escaping="yes">&#xa;</x:text>
    <xsl:template>
      <x:attribute name="match"> 
        <x:apply-templates select=".|parent::*|parent::*/parent::*|parent::*/parent::*/parent::*" mode="nameWithPredicates" /> <!-- pa and grandpa and grandpas pa only, can't use .. here -->
      </x:attribute>
      <xsl:copy-of select="." />
    </xsl:template>
  </x:template>

  <x:template match="@*[contains(local-name(),$suffix)]" />

  <x:template match="*" mode="nameWithPredicates">
    <x:value-of select="name()" />
    <x:apply-templates select="@*" mode="predicate" />
    <x:if test="position() != last()">/</x:if>
  </x:template>

  <x:template match="@*" mode="nameWithPredicates">
    <x:text>@</x:text>
    <x:value-of select="name()" />
  </x:template>

  <x:template match="@*[string-length(.) = 0]" mode="predicate" />
  <x:template match="@*[contains(local-name(),$suffix)]" mode="predicate" />

  <x:template match="@*" mode="predicate">
    <x:variable name="set" select="concat(local-name(),$suffix)" />
    <x:if test="not(../@*[local-name()=$set])">
      <x:text>[@</x:text>
      <x:value-of select="name()" />
      <x:text>='</x:text>
      <x:value-of select="." />
      <x:text>']</x:text>
    </x:if>
  </x:template>

</x:stylesheet>
