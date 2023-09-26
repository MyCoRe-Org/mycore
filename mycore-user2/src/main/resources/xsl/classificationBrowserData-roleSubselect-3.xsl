<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to transform XML output from MCRClassificationBrowser servlet to HTML for client browser, which is loaded by AJAX. The browser sends data of all child categories 
  of the requested node. -->

<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcrurl="http://www.mycore.de/xslt/url"
                exclude-result-prefixes="xsl mcrurl"
>

  <xsl:output method="xml" omit-xml-declaration="yes" />

  <xsl:include href="default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <!-- ========== XED Subselect detection ========== -->
  <xsl:variable name="xedSession" select="mcrurl:get-param(/classificationBrowserData/@webpage, '_xed_subselect_session')" />

  <xsl:variable name="folder.closed" select="'far fa-lg fa-fw fa-plus-square'" />
  <xsl:variable name="folder.open" select="'far fa-lg fa-fw fa-minus-square'" />
  <xsl:variable name="folder.leaf" select="'far fa-lg fa-fw fa-square'" />

  <xsl:template match="/classificationBrowserData">
    <ul class="cbList">
      <xsl:for-each select="category">
        <xsl:variable name="id" select="translate(concat(../@classification,'_',@id),'+/()[]','ABCDEF')" />
        <li>
          <xsl:choose>
            <xsl:when test="@children = 'true'">
              <a id="f{$id}" href="#" onclick="toggleClass('{@id}','{$folder.closed}','{$folder.open}');">
                <i class="{$folder.closed}" id="cbButton_{$id}"><xsl:comment> </xsl:comment></i>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <i class="{$folder.leaf}" id="cbButton_{$id}" ><xsl:comment> </xsl:comment></i>
            </xsl:otherwise>
          </xsl:choose>
          <a id="r{$id}">
            <xsl:attribute name="href">
              <xsl:variable name="groupName">
                <xsl:choose>
                  <xsl:when test="../@classification='mcr-roles'">
                    <xsl:value-of select="@id" />
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="concat(../@classification,':',@id)" />
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:variable>
            
              <xsl:value-of select="concat($ServletsBaseURL,'XEditor?_xed_submit_return= ')" />
              <xsl:value-of select="concat('&amp;_xed_session=',encode-for-uri($xedSession))" />
              <xsl:value-of select="concat('&amp;@name=',encode-for-uri($groupName))" />
              <xsl:value-of select="concat('&amp;label/@text=',encode-for-uri(label))" />
            </xsl:attribute>
            <xsl:value-of select="label" />
          </a>
          <xsl:if test="description">
            <p class="cbDescription">
              <xsl:value-of select="description" />
            </p>
          </xsl:if>
          <xsl:if test="@children = 'true'">
            <div id="cbChildren_{$id}" class="cbHidden"><!-- WebKit bugfix: no empty divs please -->
              <xsl:comment />
            </div>
          </xsl:if>
        </li>
      </xsl:for-each>
    </ul>
  </xsl:template>

</xsl:stylesheet>
