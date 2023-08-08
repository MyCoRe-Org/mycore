<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrversion="http://www.mycore.de/xslt/mcrversion"
                exclude-result-prefixes="fn xs">

    <xsl:function name="mcrversion:get-complete-version" as="xs:string">
        <xsl:variable name="versionDoc" select="fn:document('version:completeVersion')"/>
        <xsl:value-of select="$versionDoc/version/text()"/>
    </xsl:function>

    <xsl:function name="mcrversion:revision" as="xs:string">
        <xsl:variable name="versionDoc" select="fn:document('version:revision')"/>
        <xsl:value-of select="$versionDoc/version/text()"/>
    </xsl:function>

    <xsl:function name="mcrversion:version" as="xs:string">
        <xsl:variable name="versionDoc" select="fn:document('version:version')"/>
        <xsl:value-of select="$versionDoc/version/text()"/>
    </xsl:function>

    <xsl:function name="mcrversion:branch" as="xs:string">
         <xsl:variable name="versionDoc" select="fn:document('version:branch')"/>
         <xsl:value-of select="$versionDoc/version/text()"/>
    </xsl:function>

    <xsl:function name="mcrversion:abbrev" as="xs:string">
        <xsl:variable name="versionDoc" select="fn:document('version:abbrev')"/>
        <xsl:value-of select="$versionDoc/version/text()"/>
    </xsl:function>

    <xsl:function name="mcrversion:git-describe" as="xs:string">
        <xsl:variable name="versionDoc" select="fn:document('version:gitDescribe')"/>
        <xsl:value-of select="$versionDoc/version/text()"/>
    </xsl:function>

</xsl:stylesheet>