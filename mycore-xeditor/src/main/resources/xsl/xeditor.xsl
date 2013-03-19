<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xed="http://www.mycore.de/xeditor"
  xmlns:xalan="http://xml.apache.org/xalan" 
  xmlns:store="xalan://org.mycore.frontend.xeditor.MCREditorSessionStore"
  xmlns:session="xalan://org.mycore.frontend.xeditor.MCREditorSession"
  exclude-result-prefixes="xsl xed xalan store session">

  <xsl:strip-space elements="xed:*" />

  <xsl:param name="XEditorSessionID" />
  <xsl:param name="ServletsBaseURL" />
  
  <xsl:variable name="session" select="store:getFromSession($XEditorSessionID)" />

  <xsl:template match="xed:xeditor">
    <xsl:apply-templates select="*" />
  </xsl:template>

  <xsl:template match="xed:source[@uri]">
    <xsl:value-of select="session:readSourceXML($session,@uri)" />
  </xsl:template>

  <xsl:template match="xed:source[@root]">
    <xsl:value-of select="session:setRootElement($session,@root)" />
  </xsl:template>

  <!-- implements <xed:include uri="..." /> -->
  <xsl:template match="xed:include">
    <xsl:apply-templates select="document(@uri)/*/*" />
  </xsl:template>

  <!-- implements <xed:bind xpath="..." /> -->
  <xsl:template match="xed:bind">
    <xsl:value-of select="session:bind($session,@xpath,@name)" />
    <xsl:apply-templates select="*" />
    <xsl:value-of select="session:unbind($session)" />
  </xsl:template>

  <xsl:template
    match="input[contains('text,password,hidden,file,color,date,datetime,datetime-local,email,month,number,range,search,tel,time,url,week',@type)]"
    mode="xeditor-attribute">
    <xsl:attribute name="name">
      <xsl:value-of select="session:getAbsoluteXPath($session)" />
    </xsl:attribute>
    <xsl:attribute name="value">
      <xsl:value-of select="session:getValue($session)" />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="input[contains('checkbox,radio',@type)]" mode="xeditor-attribute">
    <xsl:attribute name="name">
      <xsl:value-of select="session:getAbsoluteXPath($session)" />
    </xsl:attribute>
    <xsl:if test="session:hasValue($session,@value)">
      <xsl:attribute name="checked">checked</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template match="option[ancestor::select]" mode="xeditor-attribute">
    <xsl:choose>
      <xsl:when test="@value and (string-length(@value) &gt; 0)">
        <xsl:if test="session:hasValue($session,@value)">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:when test="string-length(text()) &gt; 0">
        <xsl:if test="session:hasValue($session,text())">
          <xsl:attribute name="selected">selected</xsl:attribute>
        </xsl:if>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="textarea|select" mode="xeditor-attribute">
    <xsl:attribute name="name">
      <xsl:value-of select="session:getAbsoluteXPath($session)" />
    </xsl:attribute>
  </xsl:template>

  <xsl:template match="textarea" mode="xeditor-after">
    <xsl:value-of select="session:getValue($session)" />
  </xsl:template>

  <xsl:template match="form">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:attribute name="action">
        <xsl:value-of select="concat($ServletsBaseURL,'XEditor')" />
      </xsl:attribute>
      <input type="hidden" name="XEditorSessionID" value="{$XEditorSessionID}" />
      <xsl:apply-templates select="node()" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="." mode="xeditor-attribute" />
      <xsl:apply-templates select="@*|node()" />
      <xsl:apply-templates select="." mode="xeditor-after" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="@*|node()" mode="xeditor-after" />

  <xsl:template match="@*|node()" mode="xeditor-attribute" />

</xsl:stylesheet>
