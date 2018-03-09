<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xalan="http://xml.apache.org/xalan" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" exclude-result-prefixes="xalan mcrxsl">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" select="''" />
  <xsl:variable name="relacode" select="document('resource:relacode.xml')/relacode" />
  <xsl:key name="relacode" match="code" use="@key" />

  <xsl:variable name="ifsTemp">
    <xsl:for-each select="mycoreobject/structure/derobjects/derobject[mcrxsl:isDisplayedEnabledDerivate(@xlink:href)]">
      <der id="{@xlink:href}">
        <xsl:copy-of select="document(concat('xslStyle:mcr_directory-recursive:ifs:',@xlink:href,'/'))" />
      </der>
    </xsl:for-each>
  </xsl:variable>
  <xsl:variable name="ifs" select="xalan:nodeset($ifsTemp)" />

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
      <xsl:variable name="dernumber" select="count($ifs/der)" />
      <xsl:if test="string-length($WebApplicationBaseURL)&gt;0 or $dernumber&gt;0">
        <mods:location>
          <xsl:if test="not(mods:location/mods:url[@access='object in context'])">
              <mods:url access="object in context">
                <xsl:value-of select="concat($WebApplicationBaseURL,'receive/',$mycoreobject/@ID)" />
              </mods:url>
          </xsl:if>
          <xsl:if test="not(mods:location/mods:url[@access='raw object']) and $dernumber&gt;0">
            <xsl:variable name="ddbfilenumber" select="count($ifs/der/mcr_directory/children//child[@type='file'])" />
            <mods:url access="raw object">
              <xsl:choose>
                <xsl:when test="$ddbfilenumber = 1">
                  <xsl:variable name="uri" select="$ifs/der/mcr_directory/children//child[@type='file']/uri" />
                  <xsl:variable name="derId" select="substring-before(substring-after($uri,':/'), ':')" />
                  <xsl:variable name="filePath" select="substring-after(substring-after($uri, ':'), ':')" />
                  <xsl:value-of select="concat($ServletsBaseURL,'MCRFileNodeServlet/',$derId,$filePath)" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:choose>
                    <xsl:when test="$dernumber = 1">
                      <xsl:value-of select="concat($ServletsBaseURL,'MCRZipServlet/',$ifs/der/@id)" />
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="concat($ServletsBaseURL,'MCRZipServlet/',/mycoreobject/@ID)" />
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:otherwise>
              </xsl:choose>
            </mods:url>
          </xsl:if>
          <xsl:if test="not(mods:location/mods:url[@access='preview']) and $dernumber&gt;0">
            <mods:url access="preview">
              <xsl:value-of select="concat($WebApplicationBaseURL,'rsc/thumbnail/',$mycoreobject/@ID,'.png')" />
            </mods:url>
          </xsl:if>
        <xsl:apply-templates select="mods:location/node()" mode="mods2mods"/>
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
