<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
                exclude-result-prefixes="fn xs">

    <xsl:function name="mcri18n:translate" as="xs:string">
        <xsl:param name="key" as="xs:string"/>
        <xsl:variable name="transDoc"  select="fn:document(concat('i18n:', $key))" />
        <xsl:value-of select="$transDoc/i18n/text()" />
    </xsl:function>

    <xsl:function name="mcri18n:translate-with-params" as="xs:string">
        <xsl:param name="key" as="xs:string"/>
        <xsl:param name="arguments" as="xs:string*"/>
        <xsl:variable name="translation" select="mcri18n:translate($key)" />
        <xsl:choose>
            <xsl:when test="count($arguments) = 0">
                <xsl:value-of select="$translation" />
            </xsl:when>
            <xsl:otherwise>
                <!-- replace {N} in $translation with {N+1} element of $arguments -->
                <xsl:iterate select="$arguments">
                    <xsl:param name="replTrans" select="$translation" as="xs:string" />
                    <xsl:param name="repl" select="0" as="xs:integer" />
                    <xsl:variable name="replaced" select="fn:replace($replTrans, concat('\{',$repl,'\}'), .)" />
                    <xsl:on-completion>
                        <xsl:value-of select="$replaced" />
                    </xsl:on-completion>
                    <xsl:next-iteration>
                        <xsl:with-param name="replTrans" select="$replaced" />
                        <xsl:with-param name="repl" select="($repl + 1)" />
                    </xsl:next-iteration>
                </xsl:iterate>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>
