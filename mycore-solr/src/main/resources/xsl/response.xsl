<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan i18n encoder">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="xslInclude:solrResponse" />

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="DisplaySearchForm" select="'false'" />

  <xsl:variable name="PageTitle">
    <xsl:choose>
      <xsl:when test="$DisplaySearchForm = 'true'">
        <xsl:value-of select="''" />
      </xsl:when>
      <xsl:otherwise>
        <!-- unfortunately every generated page must conain this variable, we do not want a page title for now -->
        <xsl:value-of select="i18n:translate('component.solr.searchresult.resultList')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="hits" select="./response/result/@numFound" />
  <xsl:variable name="start" select="./response/lst[@name='responseHeader']/lst[@name='params']/str[@name='start']" />
  <xsl:variable name="rows" select="./response/lst[@name='responseHeader']/lst[@name='params']/str[@name='rows']" />
  <xsl:variable name="currentPage" select="ceiling((($start + 1) - $rows) div $rows)+1" />
  <xsl:variable name="query" select="./response/lst[@name='responseHeader']/lst[@name='params']/str[@name='q']" />

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

  <!-- retain the original query parameters, for attaching them to a url -->
  <xsl:variable name="params">
    <xsl:for-each select="./response/lst[@name='responseHeader']/lst[@name='params']/str[not(@name='start' or @name='rows')]">
      <!-- parameterName=parameterValue -->
      <xsl:value-of select="concat(@name,'=', encoder:encode(., 'UTF-8'))" />
      <xsl:if test="not (position() = last())">
        <xsl:value-of select="'&amp;'" />
      </xsl:if>
    </xsl:for-each>
  </xsl:variable>

  <xsl:template match="doc">
    <xsl:variable name="identifier" select="str[@name='id']" />

    <xsl:variable name="linkTo">
      <xsl:choose>
        <xsl:when test="str[@name='objectType'] = 'data_file'">
          <xsl:value-of select="concat($WebApplicationBaseURL, 'receive/', str[@name='returnId'])" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($WebApplicationBaseURL, 'receive/',$identifier)" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <tr>
      <td class="resultTitle" colspan="2">
        <a href="{$linkTo}" target="_self">
          <xsl:choose>
            <xsl:when test="./arr[@name='search_result_link_text']">
              <xsl:value-of select="./arr[@name='search_result_link_text']/str[position() = 1]" />
            </xsl:when>
            <xsl:when test="./str[@name='fileName']">
              <xsl:value-of select="./str[@name='fileName']" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$identifier" />
            </xsl:otherwise>
          </xsl:choose>
        </a>
      </td>
      <td rowspan="2" class="preview">
        <xsl:choose>
          <xsl:when test="str[@name='objectType'] = 'data_file'">
            <xsl:call-template name="iViewLinkPrev">
              <xsl:with-param name="derivates" select="./str[@name='DerivateID']" />
              <xsl:with-param name="mcrid" select="./str[@name='returnId']" />
              <xsl:with-param name="fileName" select="./str[@name='filePath']" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="iViewLinkPrev">
              <xsl:with-param name="derivates" select="./arr[@name='derivates']/str" />
              <xsl:with-param name="mcrid" select="$identifier" />
              <xsl:with-param name="derivateLinks" select="./arr[@name='derivateLink']/str" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
    <tr>
      <td class="description" colspan="2">
        <xsl:if test="./str[@name='shelfmark.type.actual']">
          <xsl:value-of select="./str[@name='shelfmark.type.actual']" />
          <br />
        </xsl:if>
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
        <span class="lastChangeDate">
          <xsl:value-of select="i18n:translate('results.lastChanged', $date)" />
        </span>
        <br />
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="iViewLinkPrev">
    <xsl:param name="derivates" />
    <xsl:param name="mcrid" />
    <xsl:param name="fileName" />
    <xsl:param name="derivateLinks" />

    <xsl:for-each select="$derivates">
      <xsl:variable name="firstSupportedFile">
        <xsl:choose>
          <xsl:when test="$fileName">
            <xsl:value-of select="$fileName" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="iview2.getSupport">
              <xsl:with-param select="." name="derivID" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
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

    <!-- display linked images -->
    <xsl:if test="$derivateLinks">
      <xsl:for-each select="$derivateLinks[string-length(.) &gt; 0]">
        <xsl:variable name="derivate" select="substring-before(. , '/')" />
        <xsl:variable name="pageToDisplay" select="concat('/', substring-after(., '/'))" />
        <a>
          <xsl:attribute name="href">
            <xsl:value-of
            select="concat($WebApplicationBaseURL,'receive/',$mcrid,'?jumpback=true&amp;maximized=true&amp;page=',$pageToDisplay,'&amp;derivate=', $derivate)" />
          </xsl:attribute>
          <xsl:attribute name="title">
            <xsl:value-of select="i18n:translate('metaData.iView')" />
          </xsl:attribute>
          <xsl:call-template name="iview2.getImageElement">
            <xsl:with-param select="$derivate" name="derivate" />
            <xsl:with-param select="$pageToDisplay" name="imagePath" />
          </xsl:call-template>
        </a>
      </xsl:for-each>
    </xsl:if>

  </xsl:template>

  <xsl:template match="/response">
    <xsl:if test="$DisplaySearchForm = 'true'">
      <div id="solrSearchInputSlotFormContainer">
        <form action="{concat($WebApplicationBaseURL,'servlets/SolrSelectProxy')}" method="get">
          <xsl:for-each select="lst[@name='responseHeader']/lst[@name='params']/str[not(@name='start' or @name='rows' or @name='q')]">
            <input type="hidden" name="{@name}" value="{.}" />
          </xsl:for-each>

          <input type="hidden" name="start" value="0" />
          <input type="hidden" name="rows" value="5" />
          <input id="solrSearchInputSlot" type="text" name="q" value="{$query}" />
          <input id="solrSearchInputSubmit" type="submit" value="" />
        </form>
      </div>
      <h1 id="resultListHeading">
        <xsl:value-of select="i18n:translate('component.solr.searchresult.resultList')" />
      </h1>
    </xsl:if>

    <!-- table header -->
    <table class="resultHeader" cellspacing="0" cellpadding="0">
      <tr>
        <td class="resultPages">
          <xsl:value-of select="concat(i18n:translate('searchResults.resultPage'), ': ', $currentPage ,'/', $pageTotal )" />
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
      <table>
        <tr>
          <xsl:if test="($start - $rows) &gt;= 0">
            <xsl:variable name="startRecordPrevPage">
              <xsl:value-of select="$start - $rows" />
            </xsl:variable>
            <td>
              <a id="linkToPreviousPage" title="{i18n:translate('searchResults.prevPage')}"
                href="{concat($WebApplicationBaseURL,'servlets/SolrSelectProxy?', $params, '&amp;start=', $startRecordPrevPage, '&amp;rows=', $rows)}">
                <xsl:value-of select="'↩'" />
              </a>
            </td>
          </xsl:if>

          <xsl:variable name="maxNumClickablePages" select="10" />
          <xsl:variable name="lookAhead" select="5" />
          <xsl:variable name="lastPageNumberToDisplay">
            <xsl:choose>
              <xsl:when test="$currentPage + $lookAhead &gt; $pageTotal">
                <xsl:value-of select="$pageTotal" />
              </xsl:when>

              <xsl:when test="$currentPage + $lookAhead &lt; $maxNumClickablePages">
                <xsl:choose>
                  <xsl:when test="$pageTotal &gt;= $maxNumClickablePages">
                    <xsl:value-of select="$maxNumClickablePages" />
                  </xsl:when>
                  <xsl:when test="$pageTotal &lt; $maxNumClickablePages">
                    <xsl:value-of select="$pageTotal" />
                  </xsl:when>
                </xsl:choose>
              </xsl:when>

              <xsl:otherwise>
                <xsl:value-of select="$currentPage + $lookAhead" />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>

          <xsl:variable name="startPage">
            <xsl:choose>
              <xsl:when test="$currentPage - $lookAhead &lt; 0">
                <xsl:value-of select="0" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="$currentPage - $lookAhead" />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>

          <td>
            <xsl:call-template name="displayPageNavigation">
              <xsl:with-param name="i" select="$startPage" />
              <xsl:with-param name="lastPageNumberToDisplay" select="$lastPageNumberToDisplay" />
            </xsl:call-template>
          </td>

          <xsl:variable name="startRecordNextPage">
            <xsl:value-of select="$start + $rows" />
          </xsl:variable>
          <xsl:if test="$startRecordNextPage &lt; $hits">
            <td>
              <a id="linkToNextPage" title="{i18n:translate('searchResults.nextPage')}"
                href="{concat($WebApplicationBaseURL,'servlets/SolrSelectProxy?', $params, '&amp;start=', $start + $rows, '&amp;rows=', $rows)}">
                <xsl:value-of select="'↪'" />
              </a>
            </td>
          </xsl:if>
        </tr>
      </table>
    </div>
  </xsl:template>

  <xsl:template name="displayPageNavigation">
    <xsl:param name="i" />
    <xsl:param name="lastPageNumberToDisplay" />

    <xsl:if test="$i &lt; $lastPageNumberToDisplay">
      <xsl:variable name="s" select="$i * $rows" />

      <xsl:element name="a">
        <xsl:variable name="idValue">
          <xsl:choose>
            <xsl:when test="$s = $start">
              <xsl:value-of select="'selectedResultPage'" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="'unselectedResultPage'" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <xsl:attribute name="id">
          <xsl:value-of select="$idValue" />
        </xsl:attribute>

        <xsl:attribute name="href">
          <xsl:value-of select="concat($WebApplicationBaseURL,'servlets/SolrSelectProxy?', $params, '&amp;start=', $s, '&amp;rows=', $rows)" />
        </xsl:attribute>

        <xsl:value-of select="$i + 1" />
      </xsl:element>

      <!-- display next page -->
      <xsl:call-template name="displayPageNavigation">
        <xsl:with-param name="i" select="$i + 1" />
        <xsl:with-param name="lastPageNumberToDisplay" select="$lastPageNumberToDisplay" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>