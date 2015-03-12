<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to transform XML output from MCRClassificationBrowser servlet to HTML for client browser, which is loaded by AJAX. The browser sends data of all child categories 
  of the requested node. -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:const="xalan://org.mycore.user2.MCRUser2Constants" exclude-result-prefixes="xsl xalan encoder const"
>

  <xsl:output method="xml" omit-xml-declaration="yes" />

  <xsl:include href="coreFunctions.xsl" />

  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="template" />

  <!-- ========== XED Subselect detection ========== -->
  <xsl:variable name="xedSession">
    <xsl:call-template name="UrlGetParam">
      <xsl:with-param name="url" select="/classificationBrowserData/@webpage" />
      <xsl:with-param name="par" select="'_xed_subselect_session'" />
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="folder.closed" select="'fa fa-lg fa-fw fa-plus-square-o'" />
  <xsl:variable name="folder.open" select="'fa fa-lg fa-fw fa-minus-square-o'" />
  <xsl:variable name="folder.leaf" select="'fa fa-lg fa-fw fa-square-o'" />

  <xsl:template match="/classificationBrowserData">
    <ul class="cbList">
      <xsl:for-each select="category">
        <xsl:variable name="id" select="translate(concat(../@classification,'_',@id),'+/()[]','ABCDEF')" />
        <li>
          <xsl:choose>
            <xsl:when test="@children = 'true'">
              <a id="f{$id}" href="#" onclick="toggleClass('{@id}','{$folder.closed}','{$folder.open}');">
                <i class="{$folder.closed}" id="cbButton_{$id}" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <i class="{$folder.leaf}" id="cbButton_{$id}" />
            </xsl:otherwise>
          </xsl:choose>
          <a id="r{$id}">
            <xsl:attribute name="href">
              <xsl:variable name="groupName">
                <xsl:choose>
                  <xsl:when test="../@classification=const:getRoleRootId()">
                    <xsl:value-of select="@id" />
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:value-of select="concat(../@classification,':',@id)" />
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:variable>
            
              <xsl:value-of select="concat($ServletsBaseURL,'XEditor?_xed_submit_return= ')" />
              <xsl:value-of select="concat('&amp;_xed_session=',encoder:encode($xedSession,'UTF-8'))" />
              <xsl:value-of select="concat('&amp;@name=',encoder:encode($groupName,'UTF-8'))" />
              <xsl:value-of select="concat('&amp;label/@text=',encoder:encode(label,'UTF-8'))" />
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
