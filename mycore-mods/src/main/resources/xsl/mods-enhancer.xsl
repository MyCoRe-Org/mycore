<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xalan="http://xml.apache.org/xalan" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="xalan">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:variable name="relacode" select="document('resource:relacode.xml')/relacode" />
  <xsl:key name="relacode" match="code" use="@key" />

  <xsl:template match="mycoreobject[contains(@ID,'_mods_')]" mode="mods">
    <xsl:apply-templates mode="mods2mods" />
  </xsl:template>
  <xsl:template match="@*|node()" mode="mods2mods">
    <xsl:apply-templates mode="mods2mods" />
  </xsl:template>
  <xsl:template match="mods:*|text()[namespace-uri(..)='http://www.loc.gov/mods/v3']" mode="mods2mods">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates mode="mods2mods" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:mods" mode="mods2mods">
    <xsl:variable name="mycoreobject" select="ancestor::mycoreobject" />
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:if test="not(@ID)">
        <xsl:attribute name="ID">
          <xsl:value-of select="$mycoreobject/@ID" />
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="string-length($WebApplicationBaseURL)&gt;0">
        <mods:location>
          <mods:url access="object in context">
            <xsl:value-of select="concat($WebApplicationBaseURL,'receive/',$mycoreobject/@ID)" />
          </mods:url>
        </mods:location>
      </xsl:if>
      <xsl:apply-templates mode="mods2mods" />
      <xsl:if test="not(mods:relatedItem[@type='host']) and $mycoreobject/structure/parents/parent">
        <xsl:variable name="parentObject" select="document(concat('mcrobject:',$mycoreobject/structure/parents/parent/@xlink:href))" />
        <mods:relatedItem type="host">
          <xsl:apply-templates select="$parentObject/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods/*"
            mode="mods2mods" />
        </mods:relatedItem>
      </xsl:if>
      <mods:identifier type="citekey">
        <xsl:value-of select="$mycoreobject/@ID" />
      </mods:identifier>
      <xsl:if test="not(mods:identifier[@type='uri']) and string-length($WebApplicationBaseURL)&gt;0">
        <mods:identifier type="uri">
          <xsl:value-of select="concat($WebApplicationBaseURL,'receive/',$mycoreobject/@ID)" />
        </mods:identifier>
      </xsl:if>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:location" mode="mods2mods">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates select="*[not(local-name()='url' and @access='object in context')]" mode="mods2mods" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:genre[@type='intern']" mode="mods2mods">
    <xsl:choose>
      <xsl:when test="contains(@valueURI,'#journal')">
      <!-- additional journals data -->
        <mods:originInfo eventType="publication">
          <mods:issuance>continuing</mods:issuance>
        </mods:originInfo>
        <mods:genre authority="marcgt">journal</mods:genre>
      </xsl:when>
      <xsl:when test="contains(@valueURI,'#article')">
      <!-- additional journals data -->
        <mods:genre authority="marcgt">article</mods:genre>
      </xsl:when>
      <xsl:when test="contains(@valueURI,'#book')">
      <!-- additional journals data -->
        <mods:genre authority="marcgt">book</mods:genre>
      </xsl:when>
      <xsl:when
        test="contains(@valueURI,'thesis') or contains(@valueURI,'#dissertation') or contains(@valueURI,'#habilitation') or contains(@valueURI,'#student_resarch_project')">
      <!-- additional journals data -->
        <mods:genre authority="marcgt">thesis</mods:genre>
      </xsl:when>
      <xsl:when test="contains(@valueURI,'#confpub')">
      <!-- additional journals data -->
        <mods:genre authority="marcgt">conference publication</mods:genre>
      </xsl:when>
    </xsl:choose>
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates mode="mods2mods" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:role" mode="mods2mods">
    <xsl:copy>
      <xsl:copy-of select="@*" />
      <xsl:apply-templates mode="mods2mods" />
      <xsl:variable name="marcrelatorCode" select="mods:roleTerm[@authority='marcrelator' and @type='code']/text()" />
      <xsl:if test="not(mods:roleTerm[@authority='marcrelator' and @type='text'] and string-length($marcrelatorCode) &gt; 0)">
        <mods:roleTerm authority="marcrelator" type="text">
          <xsl:for-each select="$relacode">
            <xsl:value-of select="key('relacode', $marcrelatorCode)/@value" />
          </xsl:for-each>
        </mods:roleTerm>
      </xsl:if>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>