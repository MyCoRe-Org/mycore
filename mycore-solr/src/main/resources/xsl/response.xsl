<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
  <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan i18n encoder">
  &html-output;
  <xsl:include href="MyCoReLayout.xsl" />
  <xsl:include href="response-utils.xsl" />
  <xsl:include href="xslInclude:solrResponse" />

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="MCR.Results.FetchHit" />
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

  <xsl:template match="/response">
    <xsl:apply-templates select="result" />
  </xsl:template>

  <xsl:template match="/response/result">
    <xsl:variable name="ResultPages">
      <xsl:call-template name="solr.Pagination">
        <xsl:with-param name="size" select="$rows" />
        <xsl:with-param name="currentpage" select="$currentPage" />
        <xsl:with-param name="totalpage" select="$totalPages" />
      </xsl:call-template>
    </xsl:variable>
    <h3>
      <xsl:choose>
        <xsl:when test="@numFound=0">
          <xsl:value-of select="i18n:translate('results.noObject')" />
        </xsl:when>
        <xsl:when test="@numFound=1">
          <xsl:value-of select="i18n:translate('results.oneObject')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="i18n:translate('results.nObjects',@numFound)" />
        </xsl:otherwise>
      </xsl:choose>
    </h3>
    <xsl:variable name="searchMask" select="$params/str[@name='mask']" />
    <div class="resultHeader">
      <xsl:if test="$searchMask | doc">
        <div class="result_options">
          <div class="btn-group">
            <xsl:if test="$searchMask">
              <a class="btn" href="{$WebApplicationBaseURL}{$searchMask}{$HttpSession}">
                <i class="icon-search">
                  <xsl:value-of select="' '" />
                </i>
                <xsl:value-of select="i18n:translate('results.newSearch')" />
              </a>
            </xsl:if>
            <xsl:if test="doc">
              <a class="btn btn-primary" href="" onclick="jQuery('form.basket_form').submit();return false;">
                <xsl:value-of select="i18n:translate('basket.add.searchpage')" />
              </a>
              <form action="{$ServletsBaseURL}MCRBasketServlet{$HttpSession}" method="post" class="basket_form">
                <input type="hidden" name="action" value="add" />
                <input type="hidden" name="redirect" value="referer" />
                <input type="hidden" name="type" value="objects" />
                <xsl:for-each select="doc">
                  <input type="hidden" name="id" value="{@id}" />
                  <input type="hidden" name="uri" value="{concat('mcrobject:',@id)}" />
                </xsl:for-each>
              </form>
            </xsl:if>
          </div>
        </div>
      </xsl:if>
      <xsl:copy-of select="$ResultPages" />
    </div>
    <xsl:comment>
      RESULT LIST START
    </xsl:comment>
    <div id="resultList">
      <xsl:apply-templates select="doc" />
    </div>
    <xsl:comment>
      RESULT LIST END
    </xsl:comment>
    <xsl:copy-of select="$ResultPages" />
  </xsl:template>

  <xsl:template match="doc">
    <xsl:comment>
      RESULT ITEM START
    </xsl:comment>
    <xsl:choose>
      <xsl:when test="$MCR.Results.FetchHit='true'">
        <!-- 
          LOCAL REQUEST
        -->
        <xsl:variable name="mcrobj" select="document(concat('mcrobject:',@id))/mycoreobject" />
        <xsl:apply-templates select="." mode="resultList">
          <xsl:with-param name="mcrobj" select="$mcrobj" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="." mode="resultList" />
      </xsl:otherwise>
    </xsl:choose>
    <xsl:comment>
      RESULT ITEM END
    </xsl:comment>
  </xsl:template>

  <xsl:template match="doc" mode="resultList">
    <!-- 
      Do not read MyCoRe object at this time
    -->
    <xsl:variable name="identifier" select="@id" />
    <xsl:variable name="mcrobj" select="." />
    <xsl:variable name="mods-type">
      <xsl:choose>
        <xsl:when test="str[@name='mods.type']">
          <xsl:value-of select="str[@name='mods.type']" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'article'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

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
    <article class="result clearfix" itemscope="" itemtype="http://schema.org/Book">
      <header class="top-head">
        <h3>
          <a href="{$linkTo}" itemprop="url">
            <span itemprop="name">
              <xsl:choose>
                <xsl:when test="./arr[@name='search_result_link_text']">
                  <xsl:value-of select="./arr[@name='search_result_link_text']/str[1]" />
                </xsl:when>
                <xsl:when test="./str[@name='fileName']">
                  <xsl:value-of select="./str[@name='fileName']" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$identifier" />
                </xsl:otherwise>
              </xsl:choose>
            </span>
          </a>
        </h3>
      </header>
      <footer class="date">
        <xsl:variable name="dateModified" select="date[@name='modified']" />
        <p>
          Zuletzt bearbeitet am :
          <time itemprop="dateModified" datetime="{$dateModified}">
            <xsl:call-template name="formatISODate">
              <xsl:with-param select="$dateModified" name="date" />
              <xsl:with-param select="i18n:translate('metaData.date')" name="format" />
            </xsl:call-template>
          </time>
        </p>
      </footer>
      <section>
        <ul class="actions">
          <li>
            <a href="#">
              <i class="icon-edit"></i>
              Bearbeiten
            </a>
          </li>
          <li>
            <a
              href="{$ServletsBaseURL}MCRBasketServlet{$HttpSession}?type=objects&amp;action=add&amp;id={$mcrobj/@ID}&amp;uri=mcrobject:{$mcrobj/@ID}&amp;redirect=referer">
              <i class="icon-plus"></i>
              <xsl:value-of select="i18n:translate('basket.add')" />
            </a>
          </li>
        </ul>
      </section>
    </article>
    <!-- 
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
     -->
  </xsl:template>

  <xsl:template name="iViewLinkPrev">
    <xsl:param name="derivate" />
    <xsl:param name="mcrid" />
    <xsl:param name="fileName" />
    <xsl:param name="derivateLink" />

    <xsl:param name="derId">
      <xsl:choose>
        <xsl:when test="$derivate">
          <xsl:value-of select="$derivate" />
        </xsl:when>
        <xsl:when test="$derivateLink">
          <xsl:value-of select="substring-before($derivateLink , '/')" />
        </xsl:when>
      </xsl:choose>
    </xsl:param>
    <xsl:if test="string-length($derId) &gt; 0 and $mcrid">
      <xsl:variable name="pageToDisplay">
        <xsl:choose>
          <xsl:when test="$fileName">
            <xsl:value-of select="$fileName" />
          </xsl:when>
          <xsl:when test="$derivateLink">
            <xsl:value-of select="concat('/', substring-after($derivateLink, '/'))" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="iview2.getSupport">
              <xsl:with-param select="$derivate" name="derivID" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:if test="$pageToDisplay != ''">
        <a
          href="{concat($WebApplicationBaseURL, 'receive/', $mcrid, '?jumpback=true&amp;maximized=true&amp;page=',$pageToDisplay,'&amp;derivate=', $derId)}"
          tile="{i18n:translate('metaData.iView')}">
          <xsl:call-template name="iview2.getImageElement">
            <xsl:with-param select="$derId" name="derivate" />
            <xsl:with-param select="$pageToDisplay" name="imagePath" />
          </xsl:call-template>
        </a>
      </xsl:if>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>