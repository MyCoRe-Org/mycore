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
        <!-- unfortunately every generated page must contain this variable, we do not want a page title for now -->
        <xsl:value-of select="i18n:translate('component.solr.searchresult.resultList')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:template match="/response">
    <xsl:apply-templates select="result|lst[@name='grouped']/lst[@name='returnId']" />
  </xsl:template>

  <xsl:template match="/response/result|lst[@name='grouped']/lst[@name='returnId']">
    <xsl:variable name="ResultPages">
      <xsl:if test="$hits &gt; 0">
        <xsl:call-template name="solr.Pagination">
          <xsl:with-param name="size" select="$rows" />
          <xsl:with-param name="currentpage" select="$currentPage" />
          <xsl:with-param name="totalpage" select="$totalPages" />
        </xsl:call-template>
      </xsl:if>
    </xsl:variable>
    <h3>
      <xsl:choose>
        <xsl:when test="$hits=0">
          <xsl:value-of select="i18n:translate('results.noObject')" />
        </xsl:when>
        <xsl:when test="$hits=1">
          <xsl:value-of select="i18n:translate('results.oneObject')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="i18n:translate('results.nObjects',$hits)" />
        </xsl:otherwise>
      </xsl:choose>
    </h3>
    <xsl:variable name="searchMask" select="$params/str[@name='mask']" />
    <div class="resultHeader">
      <xsl:if test="$searchMask | doc | int[@name='matches' and not (text()='0')]">
        <div class="result_options">
          <div class="btn-group">
            <xsl:if test="$searchMask">
              <a class="btn btn-default" href="{$WebApplicationBaseURL}{$searchMask}{$HttpSession}">
                <i class="icon-search">
                  <xsl:value-of select="' '" />
                </i>
                <xsl:value-of select="i18n:translate('results.newSearch')" />
              </a>
            </xsl:if>
            <xsl:if test="doc|arr[@name='groups']/lst">
              <form action="{$ServletsBaseURL}MCRBasketServlet{$HttpSession}" method="post" class="basket_form">
                <input type="hidden" name="action" value="add" />
                <input type="hidden" name="redirect" value="referer" />
                <input type="hidden" name="type" value="objects" />
                <xsl:for-each select="doc">
                  <input type="hidden" name="id" value="{@id}" />
                  <input type="hidden" name="uri" value="{concat('mcrobject:',@id)}" />
                </xsl:for-each>
                <xsl:for-each select="arr[@name='groups']/lst/str[@name='groupValue']">
                  <input type="hidden" name="id" value="{.}" />
                  <input type="hidden" name="uri" value="{concat('mcrobject:',.)}" />
                </xsl:for-each>
              </form>
              <a class="btn btn-primary" href="" onclick="jQuery('form.basket_form').submit();return false;">
                <xsl:value-of select="i18n:translate('basket.add.searchpage')" />
              </a>
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
      <xsl:apply-templates select="doc|arr[@name='groups']/lst/str[@name='groupValue']" />
    </div>
    <xsl:comment>
      RESULT LIST END
    </xsl:comment>
    <xsl:copy-of select="$ResultPages" />
  </xsl:template>

  <xsl:template match="str[@name='groupValue']">
    <!-- should find matched 'doc' element in subresult 'groupOwner' -->
    <xsl:variable name="hitNumberOnPage" select="count(../preceding-sibling::*)+1" />
    <xsl:apply-templates select="key('groupOwner', .)">
      <xsl:with-param name="hitNumberOnPage" select="$hitNumberOnPage" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="doc">
    <xsl:param name="hitNumberOnPage" select="count(preceding-sibling::*[name()=name(.)])+1" />
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
          <xsl:with-param name="hitNumberOnPage" select="$hitNumberOnPage" />
          <xsl:with-param name="mcrobj" select="$mcrobj" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="." mode="resultList">
          <xsl:with-param name="hitNumberOnPage" select="$hitNumberOnPage" />
        </xsl:apply-templates>
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

    <article class="result clearfix" itemscope="" itemtype="http://schema.org/Book">
      <header class="top-head">
        <h3>
          <xsl:apply-templates select="." mode="linkTo" />
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
        <xsl:apply-templates select="." mode="hitInFiles" />
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
  </xsl:template>

</xsl:stylesheet>