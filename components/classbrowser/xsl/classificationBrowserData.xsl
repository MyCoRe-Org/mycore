<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- 
  XSL to transform XML output from MCRClassificationBrowser servlet
  to HTML for client browser, which is loaded by AJAX. The browser
  sends data of all child categories of the requested node.
 -->

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:encoder="xalan://java.net.URLEncoder">

<xsl:output method="xml" omit-xml-declaration="yes" />

<xsl:param name="ServletsBaseURL" />

<xsl:template match="/classificationBrowserData">
  <ul class="cbList">
    <xsl:for-each select="category">
      <xsl:variable name="id" select="concat(../@classification,'_',@id)" />
      <li>
        <xsl:if test="@children = 'true'">
          <input id="cbButton_{$id}" type="button" value="+" onclick="toogle('{@id}');" />
        </xsl:if>
        <xsl:text> </xsl:text>
        <span class="cbID"><xsl:value-of select="@id" /></span>
        <xsl:text> </xsl:text>
        <xsl:if test="@numResults">
          <span class="cbNum"><xsl:value-of select="@numResults" /></span>
        </xsl:if>
        <xsl:if test="@numLinks">
          <span class="cbNum"><xsl:value-of select="@numLinks" /></span>
        </xsl:if>
        <a href="{$ServletsBaseURL}MCRSearchServlet?query={encoder:encode(@query)}&amp;{../@parameters}">
          <xsl:value-of select="label" />
        </a>
        <xsl:if test="uri">
          <a href="{uri}" class="cbURI"><xsl:value-of select="uri" /></a>
        </xsl:if>
        <xsl:if test="description">
          <p class="cbDescription"><xsl:value-of select="description" /></p>
        </xsl:if>
        <xsl:if test="@children = 'true'">
          <div id="cbChildren_{$id}" class="cbHidden" />
        </xsl:if>
      </li>
    </xsl:for-each>
  </ul>
</xsl:template>

</xsl:stylesheet>
