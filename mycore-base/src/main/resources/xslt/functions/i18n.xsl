<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

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
                    <xsl:on-completion>
                        <xsl:value-of select="$replTrans" />
                    </xsl:on-completion>
                    <xsl:next-iteration>
                        <xsl:with-param name="replTrans" select="fn:replace($replTrans, concat('\{',$repl,'\}'), .)" />
                        <xsl:with-param name="repl" select="($repl + 1)" />
                    </xsl:next-iteration>
                </xsl:iterate>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="mcri18n:text-direction" as="xs:string">
        <xsl:param name="language" as="xs:string"/>

        <xsl:variable name="langXML" select="fn:document(concat('language:', $language))" />
        <xsl:choose>
            <xsl:when test="$langXML/language/@rtl = 'true'">rtl</xsl:when>
            <xsl:otherwise>ltr</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <!--
      Function: mcri18n:select-lang
      synopsis: returns $CurrentLang if $elements[lang($CurrentLang)] is not empty, else $DefaultLang

      parameters:
      - $elements: the elements to check
    -->
    <xsl:function name="mcri18n:select-lang" as="xs:string">
        <xsl:param name="elements" as="element()*"/>

        <xsl:sequence select="
      if (exists($elements[lang($CurrentLang)])) then
        $CurrentLang
      else
        $DefaultLang
    "/>
    </xsl:function>

    <!--
      Function: mcri18n:select-present-lang
      synopsis: returns the result of select-lang if elements for that language are present,
                else returns a language for which elements are present

      parameters:
      - $elements: the elements to check
    -->
    <xsl:function name="mcri18n:select-present-lang" as="xs:string?">
        <xsl:param name="elements" as="element()*"/>

        <xsl:variable name="check" as="xs:string"
                      select="mcri18n:select-lang($elements)"/>

        <xsl:sequence select="
      if (exists($elements[lang($check)])) then
        $check
      else
        ($elements/@xml:lang/string())[1]
    "/>
    </xsl:function>

</xsl:stylesheet>
