<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xed="http://www.mycore.de/xeditor"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:transformer="xalan://org.mycore.frontend.xeditor.MCRXEditorTransformer"
  exclude-result-prefixes="xsl xed xalan transformer">

  <xsl:strip-space elements="xed:*" />

  <xsl:include href="copynodes.xsl" />

  <xsl:param name="XEditorTransformerKey" />
  <xsl:param name="ServletsBaseURL" />

  <xsl:variable name="transformer" select="transformer:getTransformer($XEditorTransformerKey)" />

  <!-- ========== <xed:form /> ========== -->

  <xsl:template match="xed:form">
    <form>
      <xsl:apply-templates select="@*" mode="xeditor" />
      <xsl:attribute name="action">
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor')" />
      </xsl:attribute>
      <input type="hidden" name="_xed_session" value="{transformer:getEditorSessionID($transformer)}" />
      <xsl:for-each select="transformer:getRequestParameters($transformer)">
        <input type="hidden" name="{@name}" value="{text()}" />
      </xsl:for-each>
      <xsl:apply-templates select="node()" mode="xeditor" />
    </form>
  </xsl:template>

  <!-- ========== <xed:source /> ========== -->

  <xsl:template match="xed:source" mode="xeditor">
    <xsl:value-of select="transformer:readSourceXML($transformer,@uri)" />
  </xsl:template>

  <!-- ========== <xed:cancel /> ========== -->

  <xsl:template match="xed:cancel" mode="xeditor">
    <xsl:value-of select="transformer:setCancelURL($transformer,@url)" />
  </xsl:template>

  <!-- ========== <xed:include /> ========== -->

  <xsl:template match="xed:include[@uri and @ref]" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="document($uri)/*/descendant::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@uri and not(@ref)]" mode="xeditor">
    <xsl:variable name="uri" select="transformer:replaceParameters($transformer,@uri)" />
    <xsl:apply-templates select="document($uri)/*" mode="included" />
  </xsl:template>

  <xsl:template match="xed:include[@ref and not(@uri)]" mode="xeditor">
    <xsl:variable name="ref" select="transformer:replaceParameters($transformer,@ref)" />
    <xsl:apply-templates select="/*/descendant-or-self::*[@id=$ref]" mode="included" />
  </xsl:template>

  <xsl:template match="*|text()" mode="included">
    <xsl:apply-templates mode="xeditor" />
  </xsl:template>

  <!-- ========== Text ========== -->

  <xsl:template match="@*" mode="xeditor">
    <xsl:attribute name="{name()}">
      <xsl:value-of select="transformer:replaceXPaths($transformer,.)" />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="text()" mode="xeditor">
    <xsl:value-of select="transformer:replaceXPaths($transformer,.)" />
  </xsl:template>

  <!-- ========== <xed:bind /> ========== -->

  <xsl:template match="xed:bind" mode="xeditor">
    <xsl:value-of select="transformer:bind($transformer,@xpath,@name)" />
    <xsl:apply-templates select="*" mode="xeditor" />
    <xsl:value-of select="transformer:unbind($transformer)" />
  </xsl:template>

  <xsl:template match="@xed:*|xed:*" mode="xeditor" />

  <xsl:template match="*" mode="xeditor">
    <xsl:copy>
      <xsl:apply-templates select="." mode="add-attributes" />
      <xsl:apply-templates select="@*|node()" mode="xeditor" />
      <xsl:apply-templates select="." mode="add-content" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="node()" mode="add-attributes" />

  <xsl:template match="node()" mode="add-content" />

  <!-- ========== <input /> ========== -->

  <xsl:template match="input[contains('submit image',@type)]" mode="add-attributes">
    <xsl:attribute name="name">
      <xsl:text>_xed_submit_</xsl:text>
      <xsl:value-of select="@xed:target" />
    </xsl:attribute>
  </xsl:template>

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

  <xsl:template match="xed:repeat" mode="xeditor">
    <xsl:variable name="xed_repeat" select="." />

    <xsl:for-each select="xalan:tokenize(transformer:repeat($transformer,@xpath,@min,@max))">
      <xsl:value-of select="transformer:bindRepeatPosition($transformer)" />
      <xsl:apply-templates select="$xed_repeat/node()" mode="xeditor" />
    </xsl:for-each>
    <xsl:value-of select="transformer:endRepeat($transformer)" />  
  </xsl:template>

  <!-- ========== <xed:controls /> ========== -->

  <xsl:template match="xed:controls" mode="xeditor">
    <xsl:variable name="pos" select="transformer:getRepeatPosition($transformer)" />
    <xsl:variable name="num" select="transformer:getNumRepeats($transformer)" />
    <xsl:variable name="max" select="transformer:getMaxRepeats($transformer)" />
    
    <xsl:variable name="controls">
      <xsl:if test="string-length(.) = 0">insert remove up down</xsl:if>
      <xsl:value-of select="." />
    </xsl:variable>
    
    <xsl:for-each select="xalan:tokenize($controls)">
      <xsl:choose>
        <xsl:when test="(. = 'append') and ($pos &lt; $num)" />
        <xsl:when test="(. = 'up') and ($pos = 1)" />
        <xsl:when test="(. = 'down') and ($pos = $num)" />
        <xsl:when test="(. = 'insert') and ($max = $num)" />
        <xsl:when test="(. = 'append') and ($max = $num)" />
        <xsl:otherwise>
          <input type="submit" value="{.}" name="_xed_submit_{.}_{transformer:getControlsParameter($transformer)}" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
