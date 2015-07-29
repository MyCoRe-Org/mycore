<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xalan="http://xml.apache.org/xalan" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="xalan">
  <!-- standard copy template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:name[@type='personal']">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates />
      <xsl:if test="not(mods:namePart[type='family']) and mods:displayForm">
        <xsl:call-template name="mods.seperateName">
          <xsl:with-param name="displayForm" select="mods:displayForm" />
        </xsl:call-template>
      </xsl:if>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:detail/mods:caption">
  </xsl:template>

  <xsl:template match="mods:relatedItem/mods:genre[@authority='marcgt']">
    <xsl:choose>
      <!-- bibutils does not detect article on RIS or Endnote export -->
      <xsl:when test="text()='journal'">
        <mods:genre authority="marcgt">periodical</mods:genre>
        <mods:genre>academic journal</mods:genre>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:apply-templates select="@*" />
          <xsl:apply-templates />
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:mods/mods:genre">
    <xsl:choose>
      <!-- bibutils does not like genre in article, this is implicit via relatedItem -->
      <xsl:when test="text()='article' or contains(@authorityURI, 'genres')">
        <!-- do nothing -->
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy>
          <xsl:apply-templates select="@*" />
          <xsl:apply-templates />
        </xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="mods.seperateName">
    <xsl:param name="displayForm" />
    <xsl:choose>
      <xsl:when test="contains($displayForm, ',')">
        <mods:namePart type="family">
          <xsl:value-of select="normalize-space(substring-before($displayForm, ','))" />
        </mods:namePart>
        <xsl:variable name="modsNames">
          <xsl:call-template name="mods.tokenizeName">
            <xsl:with-param name="name" select="normalize-space(substring-after($displayForm, ','))" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="modsNameParts" select="xalan:nodeset($modsNames)" />
        <xsl:for-each select="$modsNameParts/namePart">
          <mods:namePart type="given">
            <xsl:value-of select="." />
          </mods:namePart>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="concat($displayForm, ' ')">
        <xsl:variable name="modsNames">
          <xsl:call-template name="mods.tokenizeName">
            <xsl:with-param name="name" select="normalize-space(substring-after($displayForm, ','))" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="modsNameParts" select="xalan:nodeset($modsNames)" />
        <xsl:for-each select="$modsNameParts/namePart">
          <mods:namePart>
            <xsl:choose>
              <xsl:when test="position()!=last()">
                <xsl:attribute name="type">
                  <xsl:value-of select="'given'" />
                </xsl:attribute>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="type">
                  <xsl:value-of select="'family'" />
                </xsl:attribute>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="." />
          </mods:namePart>
        </xsl:for-each>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  <xsl:template name="mods.tokenizeName">
    <xsl:param name="name" />
    <xsl:param name="delimiter" select="' '" />
    <xsl:choose>
      <xsl:when test="$delimiter and contains($name, $delimiter)">
        <namePart>
          <xsl:value-of select="substring-before($name,$delimiter)" />
        </namePart>
        <xsl:call-template name="mods.tokenizeName">
          <xsl:with-param name="name" select="substring-after($name,$delimiter)" />
          <xsl:with-param name="delimiter" select="$delimiter" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <namePart>
          <xsl:value-of select="$name" />
        </namePart>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
</xsl:stylesheet>