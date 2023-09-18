<?xml version="1.0" encoding="ISO-8859-1"?>

  <!--
    XSL to transform XML output from MCRClassificationBrowser servlet to
    HTML for client browser, which is loaded by AJAX. The browser sends
    data of all child categories of the requested node.
  -->

<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcrproperty="http://www.mycore.de/xslt/property"
                exclude-result-prefixes="mcrproperty">

  <xsl:output method="xml" omit-xml-declaration="yes" />

  <xsl:include href="default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:template match="/classificationBrowserData">
    <xsl:variable name="folder.closed" select="mcrproperty:one('MCR.classbrowser.folder.closed')" />
    <xsl:variable name="folder.open" select="mcrproperty:one('MCR.classbrowser.folder.open')" />
    <xsl:variable name="folder.leaf" select="mcrproperty:one('MCR.classbrowser.folder.leaf')" />
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
              <a href="#0" onclick="toggleClass('{@id}','{$folder.closed}','{$folder.open}');"><i class="{$folder.closed}" id="cbButton_{$id}"><!-- WebKit bugfix: no empty divs please --><xsl:comment/></i></a>
            </xsl:when>
            <xsl:otherwise>
              <i class="{$folder.leaf}" id="cbButton_{$id}"><!-- WebKit bugfix: no empty divs please --><xsl:comment/></i>
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
