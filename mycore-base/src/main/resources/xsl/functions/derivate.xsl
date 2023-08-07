<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrderivate="http://www.mycore.de/xslt/derivate"
                exclude-result-prefixes="fn xs">

    <xsl:function name="mcrderivate:getMainFile" as="xs:string">
        <xsl:param name="derivID" as="xs:string"/>
        <xsl:variable name="derivate" select="document(concat('mcrobject:', $derivID))" />
        <xsl:variable name="mainFile" select="$derivate/mycorederivate/derivate/internals/internal/@maindoc" />
        <xsl:value-of select="$mainFile" />
    </xsl:function>

    <xsl:function name="mcrderivate:getContentType" as="xs:string">
        <xsl:param name="derivID" as="xs:string"/>
        <xsl:param name="path" as="xs:string"/>
        <xsl:variable name="directory">
            <xsl:choose>
                <xsl:when test="fn:ends-with($path, '/')">
                    <xsl:value-of select="substring($path, 1, string-length($path) - 1)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="substring-before($path, '/')" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="file" select="substring-after($path, $directory)" />
        <xsl:variable name="derivate" select="document(concat('ifs:', $derivID, '/', $directory))" />
        <xsl:variable name="contentType" select="$derivate/mcr_directory/children/child[@type='file' and name/text()=$file]/contentType" />
        <xsl:value-of select="$contentType" />
    </xsl:function>


</xsl:stylesheet>
