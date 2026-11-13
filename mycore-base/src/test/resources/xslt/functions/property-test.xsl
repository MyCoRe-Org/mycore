<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:map="http://www.w3.org/2005/xpath-functions/map"
  xmlns:mcrproperty="http://www.mycore.de/xslt/property"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:template match="/">
    <results>

      <get-absent>
        <xsl:value-of select="mcrproperty:get('MCR.PropertyTest.Absent')"/>
      </get-absent>
      <get-absent-with-fallback>
        <xsl:value-of select="mcrproperty:get-or-else('MCR.PropertyTest.Absent', 'fallback')"/>
      </get-absent-with-fallback>

      <get-present>
        <xsl:value-of select="mcrproperty:get('MCR.PropertyTest.Present')"/>
      </get-present>
      <get-present-with-fallback>
        <xsl:value-of select="mcrproperty:get-or-else('MCR.PropertyTest.Present', 'fallback')"/>
      </get-present-with-fallback>

      <get-boolean-absent>
        <xsl:value-of select="mcrproperty:get-boolean('MCR.PropertyTest.Absent')"/>
      </get-boolean-absent>
      <get-boolean-present>
        <xsl:value-of select="mcrproperty:get-boolean('MCR.PropertyTest.Present')"/>
      </get-boolean-present>
      <get-boolean-false>
        <xsl:value-of select="mcrproperty:get-boolean('MCR.PropertyTest.False')"/>
      </get-boolean-false>
      <get-boolean-true>
        <xsl:value-of select="mcrproperty:get-boolean('MCR.PropertyTest.True')"/>
      </get-boolean-true>

      <sub>
        <xsl:variable name="sub" select="mcrproperty:get-subproperties('MCR.PropertyTest.Sub')"/>
        <xsl:for-each select="map:keys($sub)">
          <item key="{.}" value="{$sub(.)}"/>
        </xsl:for-each>
      </sub>

    </results>
  </xsl:template>

</xsl:stylesheet>
