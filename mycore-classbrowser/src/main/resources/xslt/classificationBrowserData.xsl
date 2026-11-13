<?xml version="1.0" encoding="UTF-8"?>

<!-- 
  XSL to transform XML output from MCRClassificationBrowser servlet
  to HTML for client browser, which is loaded by AJAX. The browser
  sends data of all child categories of the requested node.
 -->

<xsl:stylesheet version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  exclude-result-prefixes="#all">

<xsl:output method="xml" omit-xml-declaration="yes" />

  <xsl:include href="default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:template match="/classificationBrowserData">
    <xsl:variable name="folder.closed" select="mcrproperty:get('MCR.classbrowser.folder.closed')" />
    <xsl:variable name="folder.open" select="mcrproperty:get('MCR.classbrowser.folder.open')" />
    <xsl:variable name="folder.leaf" select="mcrproperty:get('MCR.classbrowser.folder.leaf')" />
    <xsl:variable name="maxLinks">
      <xsl:value-of select="category[not(@numLinks &lt; following-sibling::category/@numLinks)]/@numLinks" />
    </xsl:variable>
    <xsl:variable name="maxResults">
      <xsl:value-of select="category[not(@numResults &lt; following-sibling::category/@numResults)]/@numResults" />
    </xsl:variable>

    <ul class="cbList">
      <xsl:for-each select="category">
        <xsl:variable name="id" select="translate(concat(../@classification,'_',@id),'+/()[]','ABCDEF')" />
        <li>
          <xsl:choose>
            <xsl:when test="@children = 'true'">
              <xsl:variable name="btnLabel">
                <xsl:choose>
                  <xsl:when test="label">
                    <xsl:value-of select="mcri18n:translate('component.mods.metaData.displayAll')" />
                  </xsl:when>
                  <xsl:otherwise>Show subcategories</xsl:otherwise>
                </xsl:choose>
              </xsl:variable>

              <button
                type="button"
                class="cbToggleBtn"
                id="cbButton_{$id}"
                aria-label="{$btnLabel}"
                aria-expanded="false"
                aria-controls="cbChildren_{$id}"
                onclick="toggleClass('{@id}','{$folder.closed}','{$folder.open}');">
                  <i class="{$folder.closed}" aria-hidden="true"><xsl:comment/></i>
              </button>
            </xsl:when>
            <xsl:otherwise>
              <i class="{$folder.leaf}" id="cbButton_{$id}" aria-hidden="true"><xsl:comment/></i>
            </xsl:otherwise>
          </xsl:choose>
          
          <xsl:apply-templates select="@numResults" mode="formatCount">
            <xsl:with-param name="maxCount" select="$maxResults" />
          </xsl:apply-templates>
          <xsl:apply-templates select="@numLinks" mode="formatCount">
            <xsl:with-param name="maxCount" select="$maxLinks" />
          </xsl:apply-templates>
          
          <a onclick="return startSearch('{$ServletsBaseURL}solr/select?','{@query}','{../@webpage}','{../@parameters}');" href="{$ServletsBaseURL}solr/select?{@query}&amp;mask={../@webpage}&amp;{../@parameters}">
            <xsl:value-of select="label" />
          </a>
          <xsl:if test="uri">
            <xsl:text> </xsl:text>
            <a href="{uri}" class="cbURI">
              <xsl:value-of select="uri" />
            </a>
          </xsl:if>
          <xsl:if test="description">
            <p class="cbDescription">
              <xsl:value-of select="description" />
            </p>
          </xsl:if>
          <xsl:if test="@children = 'true'">
            <div id="cbChildren_{$id}" class="cbHidden"><!-- WebKit bugfix: no empty divs please --><xsl:comment/></div>
          </xsl:if>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>


</xsl:stylesheet>
