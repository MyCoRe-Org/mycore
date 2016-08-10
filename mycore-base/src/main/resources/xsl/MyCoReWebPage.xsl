<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>

<!-- ============================================== -->
<!-- $Revision: 1.25 $ $Date: 2007-12-06 16:17:45 $ -->
<!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" exclude-result-prefixes="xlink xalan i18n">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="xslInclude:MyCoReWebPage" />

  <xsl:variable name="PageID" select="/MyCoReWebPage/@id" />

  <xsl:variable name="PageTitle">
    <xsl:choose>
      <xsl:when test="/MyCoReWebPage/section/@i18n">
        <xsl:value-of select="i18n:translate(/MyCoReWebPage/section/@i18n)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="/MyCoReWebPage/section[ lang($CurrentLang)]/@title != '' ">
            <xsl:value-of select="/MyCoReWebPage/section[lang($CurrentLang)]/@title" />
          </xsl:when>
          <xsl:when test="/MyCoReWebPage/section[@alt and contains(@alt,$CurrentLang)]/@title != '' ">
            <xsl:value-of select="/MyCoReWebPage/section[contains(@alt,$CurrentLang)]/@title" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="/MyCoReWebPage/section[lang($DefaultLang)]/@title" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="Servlet" select="'undefined'" />

  <!-- =============================================================================== -->

  <xsl:template match="/MyCoReWebPage">
    <xsl:choose>
      <xsl:when test="section[@direction = $direction]">
        <xsl:apply-templates select="section[@direction = $direction]" />
      </xsl:when>
      <xsl:when test="section[lang($CurrentLang)]">
        <xsl:apply-templates select="section[lang($CurrentLang) or lang('all') or contains(@alt,$CurrentLang)]" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="section[lang($DefaultLang) or lang('all') or contains(@alt,$DefaultLang)]" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- =============================================================================== -->
  <xsl:template match="selectBox">
    <xsl:if test="@classification">
      <xsl:variable name="maxLengthVisible">
        <xsl:if test="@maxLengthVisible">
          <xsl:value-of select="concat('?MaxLengthVisible=' ,@maxLengthVisible)" />
        </xsl:if>
      </xsl:variable>

      <xsl:variable name="classDocument"
        select="document(concat('xslStyle:items2options', $maxLengthVisible, ':classification:editor:-1:children:', @classification))" />

      <xsl:variable name="style">
        <xsl:choose>
          <xsl:when test="@style">
            <xsl:value-of select="@style" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="'editorList'" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:variable name="field">
        <xsl:choose>
          <xsl:when test="@field">
            <xsl:value-of select="@field" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@classification" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <select name="{$field}" class="{$style}">
        <option value="" selected="selected">
          <xsl:value-of select="i18n:translate('editor.search.choose')" />
        </option>
        <xsl:copy-of select="$classDocument/select/*" />
      </select>
    </xsl:if>
  </xsl:template>

  <xsl:template name="generateSelectBoxOptionValues">
    <xsl:param name="classDocument" />
    <xsl:param name="depth" select="'&#160;'" />

    <xsl:for-each select="$classDocument/category">
      <option value="{@ID}">
        <xsl:value-of select="concat($depth, label[@xml:lang=$CurrentLang]/@text)" />
      </option>

      <xsl:call-template name="generateSelectBoxOptionValues">
        <xsl:with-param name="classDocument" select="." />
        <xsl:with-param name="depth" select="concat($depth, ' &#160;')" />
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <!-- =============================================================================== -->

  <xsl:template match='@*|node()'>
    <xsl:copy>
      <xsl:apply-templates select='@*|node()' />
    </xsl:copy>
  </xsl:template>

  <!-- =============================================================================== -->

  <xsl:template match="/MyCoReWebPage/section">
    <xsl:for-each select="node()">
      <xsl:apply-templates select="." />
    </xsl:for-each>
  </xsl:template>

  <!-- =============================================================================== -->

  <xsl:template match="i18n">
    <xsl:value-of select="i18n:translate(@key)" />
  </xsl:template>

  <!-- =============================================================================== -->

</xsl:stylesheet>