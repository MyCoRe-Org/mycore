<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xed="http://www.mycore.de/xeditor">
  <!-- Transforms output of "classification:editorComplete:*" URIs to xeditor compatible format -->
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:variable name="editor.list.indent" select="'&#160;&#160;&#160;'" />
  <xsl:template match="items">
    <select>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
    </select>
  </xsl:template>
  <xsl:template match="item">
    <xsl:param name="indent" select="''" />
    <xsl:variable name="toolTip">
      <xsl:apply-templates select="." mode="toolTip" />
    </xsl:variable>

    <option title="{$toolTip}">
      <xsl:copy-of select="@*" />
      <xsl:value-of select="$indent" disable-output-escaping="yes" />
      <xsl:apply-templates select="." mode="label" />
    </option>

    <xsl:apply-templates select="item">
      <xsl:with-param name="indent" select="concat($editor.list.indent,$indent)" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="item" mode="label">
    <xsl:variable name="onDisplay">
      <xsl:choose>
        <xsl:when test="label[lang($CurrentLang)]">
          <xsl:value-of select="label[lang($CurrentLang)]" />
        </xsl:when>
        <xsl:when test="label[lang($DefaultLang)]">
          <xsl:value-of select="label[lang($DefaultLang)]" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="." />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="string-length($onDisplay) &gt; 75 ">
        <xsl:value-of select="concat(substring($onDisplay, 0, 75), ' [...]')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$onDisplay" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="item" mode="toolTip">
    <xsl:choose>
      <xsl:when test="label[lang($CurrentLang)]">
        <xsl:value-of select="label[lang($CurrentLang)]" />
      </xsl:when>
      <xsl:when test="label[lang($DefaultLang)]">
        <xsl:value-of select="label[lang($DefaultLang)]" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>