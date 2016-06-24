<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:mcr="http://www.mycore.org/" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport"
  xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:gndo="http://d-nb.info/standards/elementset/gnd#" xmlns:java="http://xml.apache.org/xalan/java"
  exclude-result-prefixes="gndo rdf mcrmods mcr xalan java" version="1.0" xmlns:ex="http://exslt.org/dates-and-times"
    extension-element-prefixes="ex">

  <xsl:include href="copynodes.xsl" />
  <xsl:include href="editor2mods-external.xsl" />

  <xsl:template match="mods:mods">
    <xsl:copy>
      <xsl:apply-templates select='@*' />
      <xsl:if test="not(mods:typeOfResource)">
        <xsl:apply-templates select="." mode="typeOfResource" />
      </xsl:if>
      <xsl:apply-templates select='node()' />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:mods" mode="typeOfResource">
    <mods:typeOfResource>
      <xsl:value-of select="'text'"/>
    </mods:typeOfResource>
  </xsl:template>

  <xsl:template match="mods:titleInfo">
    <!-- copy only if subelement has text nodes -->
    <xsl:if test="string-length(*/text())&gt;0">
      <xsl:copy>
        <xsl:apply-templates select='@*|node()' />
      </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:name">
    <!-- only copy mods:name where a name is given -->
    <xsl:param name="ID" />
    <xsl:if test="@mcr:categId">
      <xsl:apply-templates select="." mode="handleClassification">
        <xsl:with-param name="ID" select="$ID" />
      </xsl:apply-templates>
    </xsl:if>
    <xsl:if test="mods:displayForm | mods:namePart">
      <xsl:copy>
        <xsl:apply-templates select='@*|node()' />
      </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:namePartDate">
    <mods:namePart type="date">
      <xsl:apply-templates select="@*|node()" />
    </mods:namePart>
  </xsl:template>

  <xsl:template match="mods:identifier|mods:abstract">
    <!-- copy only if element has text node -->
    <xsl:if test="string-length(text())&gt;0">
      <xsl:copy>
        <xsl:apply-templates select='@*|node()' />
      </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:language">
    <!-- only copy mods:language where a language is given -->
    <xsl:if test="mods:languageTerm/@mcr:categId or normalize-space(mods:languageTerm) != ''">
      <xsl:copy>
        <xsl:apply-templates select='@*|node()' />
      </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="@editor.output" />
  <xsl:template match="@editor.parentName" />
  <!-- ignore @classId and @categId but transform it to @authority|@authorityURI and @valueURI -->
  <xsl:template match="@mcr:categId" />
  <xsl:template match="*[@mcr:categId]">
    <xsl:param name="ID" />
    <xsl:apply-templates select="." mode="handleClassification">
      <xsl:with-param name="ID" select="$ID" />
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="*[@mcr:categId]" mode="handleClassification">
    <xsl:param name="ID" />
    <xsl:copy>
      <xsl:variable name="classNodes" select="mcrmods:getClassNodes(.)" />
      <xsl:if test="string-length($ID)&gt;0">
        <xsl:attribute name="ID">
          <xsl:value-of select="$ID" />
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select='$classNodes/@*|@*|node()|$classNodes/node()' />
    </xsl:copy>
  </xsl:template>

  <!-- BMELV specific - move to externals -->
  <xsl:template match="noteLocationCorp">
    <xsl:variable name="repeaterId" select="generate-id(.)" />
    <xsl:apply-templates select="mods:name">
      <xsl:with-param name="ID" select="$repeaterId" />
    </xsl:apply-templates>
    <xsl:for-each select="mods:note">
      <xsl:copy>
        <xsl:attribute name="xlink:href" namespace="http://www.w3.org/1999/xlink">
          <xsl:value-of select="concat('#',$repeaterId)" />
        </xsl:attribute>
        <xsl:apply-templates select='@*|node()' />
      </xsl:copy>
    </xsl:for-each>
    <xsl:for-each select="mods:location">
      <xsl:copy>
        <xsl:for-each select="mods:physicalLocation">
          <xsl:copy>
            <xsl:attribute name="xlink:href" namespace="http://www.w3.org/1999/xlink">
          <xsl:value-of select="concat('#',$repeaterId)" />
        </xsl:attribute>
            <xsl:apply-templates select='@*|node()' />
          </xsl:copy>
        </xsl:for-each>
      </xsl:copy>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="*[nameOrPND]">
    <xsl:copy>
      <xsl:apply-templates select="@*" />

      <!-- get name from PND or from editor input -->
      <xsl:variable name="trimmedValue" select="java:trim(string(nameOrPND))" />
      <xsl:choose>
        <xsl:when test="string-length($trimmedValue)&lt;11 and string(number(translate($trimmedValue,'-Xx','')))!='NaN'">
          <xsl:variable name="gndURL" select="concat('http://d-nb.info/gnd/',$trimmedValue,'/about/lds.rdf')"/>
          <!-- PND -->
          <xsl:message>
            <xsl:value-of select="concat($gndURL,' ',ex:date-time())"/>
          </xsl:message>
          <xsl:variable name="gndEntry" select="document($gndURL)" />
          <xsl:message>
            <xsl:value-of select="ex:date-time()"/>
            <xsl:value-of select="count($gndEntry/*)"/>
          </xsl:message>
          <xsl:variable name="gndType"  select="substring-after($gndEntry//rdf:type/@rdf:resource, '#')" />
          <xsl:attribute name="authorityURI">
            <xsl:value-of select="'http://d-nb.info/'" />
          </xsl:attribute>
          <xsl:attribute name="valueURI">
            <xsl:value-of select="concat('http://d-nb.info/gnd/',$trimmedValue)" />
          </xsl:attribute>
          <mods:displayForm>
            <xsl:choose>
              <xsl:when test="$gndType='CorporateBody'">
                <xsl:value-of select="$gndEntry//gndo:preferredNameForTheCorporateBody" />
              </xsl:when>
              <xsl:when test="$gndType='ConferenceOrEvent'">
                <xsl:value-of select="$gndEntry//gndo:preferredNameForTheConferenceOrEvent" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="$gndEntry//gndo:preferredNameForThePerson[not(@rdf:parseType)]"/>
              </xsl:otherwise>
            </xsl:choose>
          </mods:displayForm>
        </xsl:when>
        <xsl:otherwise>
          <mods:displayForm>
            <xsl:value-of select="$trimmedValue" />
          </mods:displayForm>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:apply-templates select="*[not(local-name()='nameOrPND')]" />
    </xsl:copy>
  </xsl:template>

  <!-- workaround for bug #2595855 -->
  <xsl:template match="@lang">
    <xsl:attribute name="xml:lang">
      <xsl:value-of select="." />
    </xsl:attribute>
  </xsl:template>

  <!-- workaround for bug #3479633 -->
  <xsl:template match="@transliteration">
    <xsl:attribute name="transliteration">
      <xsl:value-of select="'text/html'"/>
    </xsl:attribute>
</xsl:template>

</xsl:stylesheet>