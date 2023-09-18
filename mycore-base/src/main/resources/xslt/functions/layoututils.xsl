<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrlayoututils="http://www.mycore.de/xslt/layoututils"
                exclude-result-prefixes="fn xs">



    <xsl:function name="mcrlayoututils:read-access" as="xs:string">
        <xsl:param name="webpageID" as="xs:string"/>
        <xsl:param name="blockerWebpageID" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="fn:string-length($blockerWebpageID)&gt;0">
                <xsl:value-of select="count(document(concat('layoutUtils:readAccess:', $webpageID, ':split:', $blockerWebpageID))/true)&gt;0" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="count(document(concat('layoutUtils:readAccess:', $webpageID))/true)&gt;0" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="mcrlayoututils:get-personal-navigation">
        <xsl:copy-of select="document('layoutUtils:personalNavigation')" />
    </xsl:function>


</xsl:stylesheet>
