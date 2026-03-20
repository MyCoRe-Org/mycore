<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcrclassification="http://www.mycore.de/xslt/classification"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  exclude-result-prefixes="#all">

  <!--
    Returns the MCR category entry for the given classification and category ID

    Parameters
      * classid: the classification ID
      * categid: the category ID
  -->
  <xsl:function name="mcrclassification:category" as="element()?">
    <xsl:param name="classid" as="xs:string" />
    <xsl:param name="categid" as="xs:string" />

    <xsl:sequence
      select="document(concat('classification:metadata:0:children:',$classid,':',$categid))//category" />
  </xsl:function>

  <!--
    Returns whether the given classification ID and category ID resolve to an existing category.

    Parameters
      * classid: the classification ID
      * categid: the category ID
  -->
  <xsl:function name="mcrclassification:is-category-id" as="xs:boolean">
    <xsl:param name="classid" as="xs:string" />
    <xsl:param name="categid" as="xs:string" />

    <xsl:sequence select="
      $classid != ''
      and $categid != ''
      and string-length($classid) le 32
      and exists(mcrclassification:category($classid, $categid))
    " />
  </xsl:function>

  <!--
    Returns the CurrentLang label element of the given classification.

    Parameters
      * class: the MCR category element
  -->
  <xsl:function name="mcrclassification:current-label" as="element()?">
    <xsl:param name="class" as="element()?" />

    <xsl:copy-of select="mcrclassification:label($CurrentLang, $class)"/>
  </xsl:function>

  <!--
    Returns the label element of the given classification

    Parameters
      * lang: the language of the label element
      * class: the MCR category element
  -->
  <xsl:function name="mcrclassification:label" as="element()?">
    <xsl:param name="lang" as="xs:string"/>
    <xsl:param name="class" as="element()?"/>

    <xsl:choose>
      <xsl:when test="$class[@classid and @categid]">
        <xsl:sequence select="mcrclassification:label($lang, document(concat('classification:metadata:0:children:',$class/@classid,':',$class/@categid))//category)" />
      </xsl:when>
      <xsl:when test="string-length($lang) > 0 and $class/label[lang($lang)]">
        <xsl:sequence select="$class/label[lang($lang)]" />
      </xsl:when>
      <xsl:when test="$class/label[lang($CurrentLang)]">
        <xsl:sequence select="$class/label[lang($CurrentLang)]" />
      </xsl:when>
      <xsl:when test="$class/label[lang($DefaultLang)]">
        <xsl:sequence select="$class/label[lang($DefaultLang)]" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="$class/label[1]" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <!--
    Returns the text of the CurrentLang label element of the given classification

    Parameters
      * class: the MCR category element
  -->
  <xsl:function name="mcrclassification:current-label-text" as="xs:string?">
    <xsl:param name="class" as="element()?" />

    <xsl:value-of select="mcrclassification:label-text($CurrentLang, $class)"/>
  </xsl:function>

  <!--
    Returns the text of the label element with the given language of the given classification.

    Parameters
      * lang: the language of the label
      * class: the MCR category element
  -->
  <xsl:function name="mcrclassification:label-text" as="xs:string?">
    <xsl:param name="lang" as="xs:string"/>
    <xsl:param name="class" as="element()?"/>

    <xsl:variable name="label" select="mcrclassification:label($lang, $class)"/>
    <xsl:choose>
      <xsl:when test="$label">
        <xsl:sequence select="$label/@text" />
      </xsl:when>
      <xsl:when test="$class/@ID">
        <xsl:sequence select="concat('??', $class/@ID, '@', $lang, '??')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="()" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>
</xsl:stylesheet>
