<?xml version="1.0" encoding="ISO-8859-1"?>

  <!--
    XSL to transform XML output from MCRClassificationBrowser servlet to
    HTML for client browser, which is loaded by AJAX. The browser sends
    data of all child categories of the requested node.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan i18n">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="template" />

  <xsl:param name="MCR.classbrowser.folder.closed" select="'glyphicon glyphicon-expand'" />
  <xsl:param name="MCR.classbrowser.folder.open" select="'glyphicon glyphicon-collapse-up'" />
  <xsl:param name="MCR.classbrowser.folder.leaf" select="'glyphicon glyphicon-unchecked'" />

  <xsl:output method="xml" omit-xml-declaration="yes" />

  <xsl:template match="/classificationBrowserData">
    <xsl:variable name="folder.closed" select="$MCR.classbrowser.folder.closed" />
    <xsl:variable name="folder.open" select="$MCR.classbrowser.folder.open" />
    <xsl:variable name="folder.leaf" select="$MCR.classbrowser.folder.leaf" />
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
              <a href="#" onclick="toggleClass('{@id}','{$folder.closed}','{$folder.open}');"><i class="{$folder.closed}" id="cbButton_{$id}" /></a>
            </xsl:when>
            <xsl:otherwise>
              <i class="{$folder.leaf}" id="cbButton_{$id}" />
            </xsl:otherwise>
          </xsl:choose>
          <xsl:apply-templates select="@numResults" mode="formatCount">
            <xsl:with-param name="maxCount" select="$maxResults" />
          </xsl:apply-templates>
          <xsl:apply-templates select="@numLinks" mode="formatCount">
            <xsl:with-param name="maxCount" select="$maxLinks" />
          </xsl:apply-templates>
          <a onclick="return startSearch('{$ServletsBaseURL}SolrSelectProxy?','{@query}','{../@webpage}','{../@parameters}');" href="{$ServletsBaseURL}SolrSelectProxy?{@query}&amp;mask={../@webpage}&amp;{../@parameters}">
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
            <div id="cbChildren_{$id}" class="cbHidden" />
          </xsl:if>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>

  <xsl:template match="@numResults|@numLinks" mode="formatCount">
    <xsl:param name="maxCount" />
    <!-- placeholder for numberOfFiles: 10 times &nbsp; -->
    <xsl:variable name="numberMask" select="'&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;&#160;'" />
    <xsl:variable name="maxLength" select="string-length($maxCount)" />
    <xsl:variable name="curLength" select="string-length(.)" />
    <xsl:variable name="lengthDiff" select="$maxLength - $curLength" />
    <xsl:variable name="cntString">
      <xsl:if test="$lengthDiff &gt; 0">
        <xsl:value-of select="substring($numberMask, 1, $lengthDiff)" />
      </xsl:if>
      <xsl:value-of select="." />
    </xsl:variable>
    <span class="cbNum">
      <xsl:value-of select="concat('[',$cntString,']')" />
    </span>
  </xsl:template>

</xsl:stylesheet>
