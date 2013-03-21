<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:transformer="xalan://org.mycore.frontend.xeditor.MCRXEditorTransformer"
  exclude-result-prefixes="xsl xed xalan transformer">

  <xsl:strip-space elements="xed:*" />

  <xsl:param name="XEditorTransformerKey" />
  <xsl:param name="ServletsBaseURL" />

  <xsl:variable name="transformer" select="transformer:getTransformer($XEditorTransformerKey)" />

  <!-- ========== <xed:form /> ========== -->

  <xsl:template match="xed:form">
    <form>
      <xsl:apply-templates select="@*" />
      <xsl:attribute name="action">
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor')" />
      </xsl:attribute>
      <input type="hidden" name="XEditorSessionID" value="{transformer:getEditorSessionID($transformer)}" />
      <xsl:apply-templates select="node()" />
    </form>
  </xsl:template>

  <!-- ========== <xed:source /> ========== -->

  <xsl:template match="xed:source[@uri]">
    <xsl:value-of select="transformer:readSourceXML($transformer,@uri)" />
  </xsl:template>

  <xsl:template match="xed:source[@root]">
    <xsl:value-of select="transformer:readSourceXML($transformer,concat('buildxml:_rootName_=',@root))" />
  </xsl:template>

  <!-- ========== <xed:include /> ========== -->

  <xsl:template match="xed:include[@uri and @ref]">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="document($uri)/*/descendant::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@uri and not(@ref)]">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:apply-templates select="document($uri)/*" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@ref and not(@uri)]">
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="/*/descendant-or-self::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="*|text()" mode="included">
    <xsl:apply-templates />
  </xsl:template>

  <!-- ========== <xed:bind /> ========== -->

  <xsl:template match="xed:bind">
    <xsl:value-of select="transformer:bind($transformer,@xpath,@name)" />
    <xsl:apply-templates select="*" />
    <xsl:value-of select="transformer:unbind($transformer)" />
  </xsl:template>

  <xsl:template match="@*">
    <xsl:copy />
  </xsl:template>

  <xsl:template match="node()">
    <xsl:copy>
      <xsl:apply-templates select="." mode="add-attributes" />
      <xsl:apply-templates select="@*|node()" />
      <xsl:apply-templates select="." mode="add-content" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="node()" mode="add-attributes" />

  <xsl:template match="node()" mode="add-content" />

  <!-- ========== <input /> ========== -->

  <xsl:template
    match="input[contains('text,password,hidden,file,color,date,datetime,datetime-local,email,month,number,range,search,tel,time,url,week',@type)]"
    mode="add-attributes">
    <xsl:attribute name="name">
    <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
  </xsl:attribute>
    <xsl:attribute name="value">
    <xsl:value-of select="transformer:getValue($transformer)" />
  </xsl:attribute>
  </xsl:template>

  <xsl:template match="input[contains('checkbox,radio',@type)]" mode="add-attributes">
    <xsl:attribute name="name">
    <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
  </xsl:attribute>
    <xsl:if test="transformer:hasValue($transformer,@value)">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template match="option[ancestor::select]" mode="add-attributes">
    <xsl:choose>
      <xsl:when test="@value and (string-length(@value) &gt; 0)">
        <xsl:if test="transformer:hasValue($transformer,@value)">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:when test="string-length(text()) &gt; 0">
        <xsl:if test="transformer:hasValue($transformer,text())">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="textarea|select" mode="add-attributes">
    <xsl:attribute name="name">
    <xsl:value-of select="transformer:getAbsoluteXPath($transformer)" />
  </xsl:attribute>
  </xsl:template>

  <xsl:template match="textarea" mode="add-content">
    <xsl:value-of select="transformer:getValue($transformer)" />
  </xsl:template>

  <!-- ========== <xed:repeat /> ========== -->
  
  <xsl:template match="xed:repeat">
    <xsl:variable name="currentName" select="transformer:bindingName($transformer)" />
    <xsl:variable name="currentXPath" select="transformer:bindingXPath($transformer)" />

    <!-- Number of repeats to make -->
    <xsl:variable name="repeats" select="xalan:tokenize(transformer:numRepeats($transformer,@min))" />

    <!-- Unbind current binding to bind each node separately -->
    <xsl:value-of select="transformer:unbind($transformer)" />

    <xsl:variable name="currentNodeSet" select="*" />
    <xsl:for-each select="$repeats">
      <xsl:variable name="xPath" select="concat($currentXPath,'[',position(),']')" />
      <xsl:value-of select="transformer:bind($transformer,$xPath,'')" />
      <xsl:apply-templates select="$currentNodeSet" />
      <xsl:value-of select="transformer:unbind($transformer)" />
    </xsl:for-each>

    <!-- Restore original binding -->
    <xsl:value-of select="transformer:bind($transformer,$currentXPath,$currentName)" />
  </xsl:template>
  
</xsl:stylesheet>
