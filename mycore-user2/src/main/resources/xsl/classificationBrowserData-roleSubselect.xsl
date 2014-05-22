<?xml version="1.0" encoding="UTF-8"?>

<!-- 
  XSL to transform XML output from MCRClassificationBrowser servlet
  to HTML for client browser, which is loaded by AJAX. The browser
  sends data of all child categories of the requested node.
 -->

<xsl:stylesheet version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:const="xalan://org.mycore.user2.MCRUser2Constants"
  exclude-result-prefixes="xsl xalan encoder const"
>

<xsl:output method="xml" omit-xml-declaration="yes" />

<xsl:include href="coreFunctions.xsl" />

<xsl:param name="ServletsBaseURL" />
<xsl:param name="WebApplicationBaseURL" />

<!-- ========== Subselect Parameters ========== -->
<xsl:param name="subselect.session" />
<xsl:param name="subselect.varpath" />
<xsl:param name="subselect.webpage" />

<!-- ========== XED Subselect detection ========== -->
<xsl:variable name="xedSession">
  <xsl:call-template name="UrlGetParam">
    <xsl:with-param name="url" select="/classificationBrowserData/@webpage" />
    <xsl:with-param name="par" select="'_xed_subselect_session'" />
  </xsl:call-template>
</xsl:variable>

<xsl:variable name="folder.closed" select="concat($WebApplicationBaseURL,'images/folder_closed_in_use.gif')" />
<xsl:variable name="folder.open" select="concat($WebApplicationBaseURL,'images/folder_open.gif')" />
<xsl:variable name="folder.leaf" select="concat($WebApplicationBaseURL,'images/folder_closed_empty.gif')" />

<xsl:template match="/classificationBrowserData">
  <ul class="cbList">
    <xsl:for-each select="category">
      <xsl:variable name="id" select="translate(concat(../@classification,'_',@id),'+/()[]','ABCDEF')" />
      <li>
        <xsl:choose>
          <xsl:when test="@children = 'true'">
            <input type="image" id="cbButton_{$id}" src="{$folder.closed}" onclick="toogle('{@id}','{$folder.closed}','{$folder.open}');" />
          </xsl:when>
          <xsl:otherwise>
            <img src="{$folder.leaf}" />
          </xsl:otherwise>
        </xsl:choose>
        <a>
          <xsl:attribute name="href">
            <xsl:variable name="groupName">
              <xsl:choose>
                <xsl:when test="../@classification=const:getRoleRootId()">
                  <xsl:value-of select="@id"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat(../@classification,':',@id)"/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            
            <xsl:choose>
              <xsl:when test="string-length($xedSession) &gt; 0">
                <xsl:value-of select="concat($ServletsBaseURL,'XEditor?_xed_submit_return= ')" />
                <xsl:value-of select="concat('&amp;_xed_session=',encoder:encode($xedSession,'UTF-8'))" />
                <xsl:value-of select="concat('&amp;@name=',encoder:encode($groupName,'UTF-8'))" />
                <xsl:value-of select="concat('&amp;label/@text=',encoder:encode(label,'UTF-8'))" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat($ServletsBaseURL,'XMLEditor?_action=end.subselect')" />
                <xsl:value-of select="concat('&amp;subselect.session=',encoder:encode($subselect.session,'UTF-8'))" />
                <xsl:value-of select="concat('&amp;subselect.varpath=',encoder:encode($subselect.varpath,'UTF-8'))" />
                <xsl:value-of select="concat('&amp;subselect.webpage=',encoder:encode($subselect.webpage,'UTF-8'))" />
                <xsl:value-of select="concat('&amp;_var_@name=',encoder:encode($groupName,'UTF-8'))" />
                <xsl:value-of select="concat('&amp;_var_label/@text=',encoder:encode(label,'UTF-8'))" />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:value-of select="label" />
        </a>
        <xsl:if test="description">
          <p class="cbDescription"><xsl:value-of select="description" /></p>
        </xsl:if>
        <xsl:if test="@children = 'true'">
          <div id="cbChildren_{$id}" class="cbHidden"><!-- WebKit bugfix: no empty divs please --><xsl:comment/></div>
        </xsl:if>
      </li>
    </xsl:for-each>
  </ul>
</xsl:template>

</xsl:stylesheet>
