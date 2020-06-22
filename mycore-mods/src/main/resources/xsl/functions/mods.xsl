<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrmods="http://www.mycore.de/xslt/mods"
                exclude-result-prefixes="mods fn xs">
  <xsl:variable name="supportedElements" select="map{'accessCondition':(), 'area':(), 'cartographics':(), 'city':(),
        'citySection':(), 'classification':(), 'continent':(), 'country':(), 'county':(), 'descriptionStandard':(),
        'extraTerrestrialArea':(), 'form':(), 'frequency':(), 'genre':(), 'geographic':(), 'geographicCode':(), 'hierarchicalGeographic':(),
        'island':(), 'languageTerm':(), 'name':(), 'occupation':(), 'physicalLocation':(), 'placeTerm':(), 'publisher':(),
        'recordContentSource':(), 'region':(), 'roleTerm':(), 'scriptTerm':(), 'state':(), 'subject':(), 'targetAudience':(), 'temporal':(),
        'territory':(), 'titleInfo':(), 'topic':(), 'typeOfResource':()}"/>

  <xsl:function name="mcrmods:to-uri" as="xs:anyURI">
    <xsl:param name="node" as="element()"/>
    <xsl:choose>
      <xsl:when test="mcrmods:is-supported($node)">
        <xsl:choose>
          <xsl:when test="$node/@authorityURI">
            <xsl:sequence
                select="xs:anyURI(concat('modsclass:/uri/',fn:encode-for-uri($node/@authorityURI),'/',fn:encode-for-uri($node/@valueURI)))"/>
          </xsl:when>
          <xsl:when test="$node/@authority">
            <xsl:sequence
                select="xs:anyURI(concat('modsclass:/authority/',fn:encode-for-uri($node/@authority),'/',fn:encode-for-uri($node/text())))"/>
          </xsl:when>
          <xsl:when test="$node[fn:local-name()='accessCondition' and @xlink:href]">
            <xsl:sequence
                select="xs:anyURI(concat('modsclass:/accessCondition/',fn:encode-for-uri($node/@xlink:href)))"/>
          </xsl:when>
          <xsl:when test="$node[fn:local-name()='typeOfResource']">
            <xsl:sequence select="xs:anyURI(concat('modsclass:/typeOfResource/',fn:encode-for-uri($node)))"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:sequence select="()"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:function name="mcrmods:to-mycoreclass" as="element()?">
    <xsl:param name="node" as="element()"/>
    <xsl:param name="mode" as="xs:string"/>
    <xsl:choose>
      <xsl:when test="$mode = 'parent' or $mode = 'single'">
        <xsl:variable name="uri" select="mcrmods:to-uri($node)"/>
        <xsl:choose>
          <xsl:when test="fn:string-length($uri) &gt; 0">
            <xsl:variable name="class" select="fn:document($uri)"/>
            <xsl:choose>
              <xsl:when test="$class/mycoreclass">
                <!-- Yeah -->
                <xsl:choose>
                  <xsl:when test="$mode = 'parent'">
                    <xsl:sequence select="$class/mycoreclass"/>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:variable name="reduced">
                      <xsl:apply-templates select="$class/mycoreclass" mode="remove-parents" />
                    </xsl:variable>
                    <xsl:sequence select="$reduced/mycoreclass" />
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:when>
              <xsl:otherwise>
                <xsl:sequence select="()"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <xsl:sequence select="()"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="fn:error(xs:QName('mcrmods:MODEUNKNOWN'), concat('mode ',$mode,' is unsupported'))"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:function name="mcrmods:is-supported" as="xs:boolean">
    <xsl:param name="node" as="element()"/>
    <xsl:sequence
        select="fn:namespace-uri($node) = xs:anyURI('http://www.loc.gov/mods/v3') and map:contains($supportedElements, fn:local-name($node))"/>
  </xsl:function>

  <xsl:template match="mycoreclass" mode="remove-parents">
    <xsl:copy>
      <xsl:copy-of select="@*|*[fn:local-name() != 'categories']"/>
      <categories>
        <xsl:copy-of select="categories//category[not(category)]"/>
      </categories>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
