<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor">
  <!-- Transforms output of "classification:editorComplete:*" URIs to xeditor compatible format -->
  <xsl:param name="MaxLengthVisible" />

  <xsl:variable name="editor.list.indent" select="'&#160;&#160;&#160;'" />
  <xsl:template match="items">
    <items>
      <xsl:apply-templates />
    </items>
  </xsl:template>
  <xsl:template match="item">
    <item value="{@value}">
      <xsl:for-each select="label">
        <xsl:apply-templates select="." />
      </xsl:for-each>
      <!-- ==== handle children ==== -->
      <xsl:apply-templates select="item" />
    </item>
  </xsl:template>

  <xsl:template match="label">
    <label xml:lang="{@xml:lang}">
      <xsl:choose>
        <xsl:when test="$MaxLengthVisible and (string-length(.) &gt; $MaxLengthVisible) ">
          <xsl:value-of select="concat(substring(., 0, $MaxLengthVisible), ' [...]')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="." />
        </xsl:otherwise>
      </xsl:choose>
    </label>
  </xsl:template>
</xsl:stylesheet>