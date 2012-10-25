<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:variable name="PageTitle" select="'Suchergebnisse'" />

  <xsl:template match="doc">
    <xsl:variable name="identifier" select="str[@name='id']" />
    <tr>
      <td class="resultTitle" colspan="2">
        <a href="{concat($WebApplicationBaseURL, 'receive/',$identifier)}" target="_self">
          <xsl:choose>
            <xsl:when test="./arr[@name='search_result_link_text']">
              <xsl:value-of select="./arr[@name='search_result_link_text']/str[position() = 1]" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$identifier" />
            </xsl:otherwise>
          </xsl:choose>
        </a>
      </td>
      <td rowspan="2" class="preview">
        <xsl:call-template name="iViewLinkPrev">
          <xsl:with-param name="derivates" select="./arr[@name='derivates']/str" />
          <xsl:with-param name="mcrid" select="$identifier" />
        </xsl:call-template>
      </td>
    </tr>
    <tr>
      <td class="description" colspan="2">
        <xsl:if test="./arr[@name='placeOfActivity']">
          <xsl:value-of select="./arr[@name='placeOfActivity']/str[position() = 1]" />
          <br />
        </xsl:if>
        <xsl:if test="./str[@name='pnd']">
          <xsl:value-of select="concat('PND:', ./str[@name='pnd'])" />
          <br />
        </xsl:if>
        <xsl:variable name="date">
          <xsl:call-template name="formatISODate">
            <xsl:with-param select="./date[@name='modifydate']" name="date" />
            <xsl:with-param select="i18n:translate('metaData.date')" name="format" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:value-of select="i18n:translate('results.lastChanged',$date)" />
        <br />
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="iViewLinkPrev">
    <xsl:param name="derivates" />
    <xsl:param name="mcrid" />

    <xsl:for-each select="$derivates">
      <xsl:variable name="firstSupportedFile">
        <xsl:call-template name="iview2.getSupport">
          <xsl:with-param select="." name="derivID" />
        </xsl:call-template>
      </xsl:variable>

      <!-- MCR-IView ..start -->
      <xsl:if test="$firstSupportedFile != ''">
        <a>
          <xsl:attribute name="href">
          <xsl:value-of
            select="concat($WebApplicationBaseURL, 'receive/', $mcrid, '?jumpback=true&amp;maximized=true&amp;page=',$firstSupportedFile,'&amp;derivate=', .)" />
        </xsl:attribute>
          <xsl:attribute name="title">
          <xsl:value-of select="i18n:translate('metaData.iView')" />
        </xsl:attribute>
          <xsl:call-template name="iview2.getImageElement">
            <xsl:with-param select="." name="derivate" />
            <xsl:with-param select="$firstSupportedFile" name="imagePath" />
          </xsl:call-template>
        </a>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>


  <xsl:template match="/response">
    <xsl:variable name="hits" select="result/@numFound" />
    <xsl:variable name="start" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='start']" />
    <xsl:variable name="rows" select="lst[@name='responseHeader']/lst[@name='params']/str[@name='rows']" />
    <xsl:variable name="query" select="encoder:encode(lst[@name='responseHeader']/lst[@name='params']/str[@name='q'])" />

    <xsl:variable name="pageTotal">
      <xsl:choose>
        <xsl:when test="ceiling($hits div $rows) = 0">
          <xsl:value-of select="1" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="ceiling($hits div $rows )" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <!-- table header -->
    <table class="resultHeader" cellspacing="0" cellpadding="0">
      <tr>
        <td class="resultPages">
          <xsl:value-of
            select="concat(i18n:translate('searchResults.resultPage'), ': ', ceiling((($start + 1) - $rows) div $rows)+1 ,'/', $pageTotal )" />
        </td>
        <td class="resultCount">
          <strong>
            <xsl:value-of select="concat('Es wurden ', $hits,' Objekte gefunden')" />
          </strong>
        </td>
      </tr>
    </table>

    <!-- results -->
    <table id="resultList" cellspacing="0" cellpadding="0">
      <xsl:apply-templates select="result/doc" />
    </table>

    <!-- table footer -->
    <div id="pageSelection">
      <tr>
        <xsl:if test="($start - $rows) &gt;= 0">
          <xsl:variable name="startRecordPrevPage">
            <xsl:value-of select="$start - $rows" />
          </xsl:variable>
          <td>
            <a title="{i18n:translate('searchResults.prevPage')}"
              href="{concat($WebApplicationBaseURL,'servlets/search?q=', $query, '&amp;start=', $startRecordPrevPage, '&amp;rows=', $rows)}">&lt;</a>
          </td>
        </xsl:if>

        <xsl:variable name="startRecordNextPage">
          <xsl:value-of select="$start + $rows" />
        </xsl:variable>
        <xsl:if test="$startRecordNextPage &lt; $hits">
          <td>
            <a title="{i18n:translate('searchResults.nextPage')}" href="{concat($WebApplicationBaseURL,'servlets/search?q=', $query, '&amp;start=', $start + $rows, '&amp;rows=', $rows)}">&gt;</a>
          </td>
        </xsl:if>
      </tr>
    </div>
  </xsl:template>
</xsl:stylesheet>