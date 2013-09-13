<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor">
<!-- Transforms output of "classification:editorComplete:*" URIs to xeditor compatible format -->
  <xsl:variable name="editor.list.indent" select="'&#160;&#160;&#160;'" />
  <xsl:template match="items">
    <select>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </select>
  </xsl:template>
  <xsl:template match="item">
    <xsl:param name="indent" select="''" />
    <option>
      <xsl:copy-of select="@*" />
      <xsl:value-of select="$indent" disable-output-escaping="yes" />
      <xed:multi-lang>
        <xsl:apply-templates select="label" />
      </xed:multi-lang>
    </option>
    <xsl:apply-templates select="item">
      <xsl:with-param name="indent" select="concat($editor.list.indent,$indent)" />
    </xsl:apply-templates>
  </xsl:template>
  <xsl:template match="label">
    <xed:lang>
      <xsl:copy-of select="@*" />
      <xsl:value-of select="." />
    </xed:lang>
  </xsl:template>
</xsl:stylesheet>