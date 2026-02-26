<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mcrlayoututils="http://www.mycore.de/xslt/layoututils"
  xmlns:mcrurl="http://www.mycore.de/xslt/url"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="fn xs">

  <xsl:function name="mcrlayoututils:read-access" as="xs:string">
        <xsl:param name="webpageID" as="xs:string"/>
        <xsl:param name="blockerWebpageID" as="xs:string?"/>
        <xsl:choose>
            <xsl:when test="empty($blockerWebpageID) or $blockerWebpageID=''">
                <xsl:value-of select="count(document(concat('layoutUtils:readAccess:', $webpageID))/true)&gt;0" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="count(document(concat('layoutUtils:readAccess:', $webpageID, ':split:', $blockerWebpageID))/true)&gt;0" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="mcrlayoututils:get-personal-navigation">
        <xsl:copy-of select="document('layoutUtils:personalNavigation')" />
    </xsl:function>

  <!--
    Function: get-browser-address
    Synopsis: Identifies the currently selected menu entry and the associated element item/@href.
    Usage:    mcrurl:get-browser-address(mcrlayoututils:get-personal-navigation(),.)
  -->
  <xsl:function name="mcrlayoututils:get-browser-address" as="xs:string">
    <xsl:param name="loaded_navigation_xml" as="element()" />
    <!-- Context node must be passed explicitly because XSLT functions lack an implicit context item -->
    <xsl:param name="context-node" as="node()?" />

    <!-- 1. remove last page parameter -->
    <xsl:variable name="request-url-last-page-del"
                  select="mcrurl:del-param($RequestURL, 'XSL.lastPage.SESSION')" as="xs:string" />

    <xsl:variable name="request-url-web-url-del"
                  select="concat('/', substring-after($request-url-last-page-del, $WebApplicationBaseURL))"
                  as="xs:string" />

    <!-- 2. remove lang parameter -->
    <xsl:variable name="clean-url" select="mcrurl:del-param($request-url-last-page-del, 'lang')" as="xs:string" />
    <xsl:variable name="clean-url2" select="mcrurl:del-param($request-url-web-url-del, 'lang')" as="xs:string" />

    <!-- 1st Case: test if navigation.xml contains the current browser address -->
    <xsl:variable name="browser-address-href"
                  select="string($loaded_navigation_xml//item[@href=$clean-url2 or @href=$clean-url][1]/@href)"
                  as="xs:string" />

    <xsl:choose>
      <xsl:when test="$browser-address-href != ''">
        <xsl:sequence select="$browser-address-href" />
      </xsl:when>

      <!-- 2nd Case: evaluate dynamicContentBinding -->
      <xsl:otherwise>
        <!-- safely get the name of the first child element -->
        <xsl:variable name="root-tag" select="name($context-node/*[1])" as="xs:string" />

        <!-- The original nested loops translate cleanly to this single XPath expression. -->
        <xsl:variable name="browser-address-dynamic"
                      select="string-join($loaded_navigation_xml//dynamicContentBinding/rootTag[. = $root-tag]/ancestor-or-self::*[@href][1]/@href, '')"
                      as="xs:string" />

        <xsl:sequence select="
            if ($browser-address-dynamic != '') then
                $browser-address-dynamic
            else
                string($loaded_navigation_xml/@hrefStartingPage)
        " />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

</xsl:stylesheet>
