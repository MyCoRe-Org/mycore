<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport" exclude-result-prefixes="mcrmods" version="1.0">

  <xsl:include href="copynodes.xsl" />

  <xsl:template match="*[@authority or @authorityURI]">
    <xsl:copy>
      <xsl:variable name="classNodes" select="mcrmods:getMCRClassNodes(.)" />
      <xsl:apply-templates select='$classNodes/@*|@*|node()' />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:mods">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates
        select="*[not( (local-name()='name' and @ID and @type='corporate') or (starts-with(@xlink:href,'#')) or (starts-with(mods:physicalLocation/@xlink:href,'#')) )]" />
      <xsl:for-each select="mods:name[@ID and @type='corporate']">
        <noteLocationCorp>
          <xsl:variable name="ID" select="@ID" />
          <xsl:apply-templates select=".|../*[@xlink:href=concat('#',$ID) or mods:physicalLocation/@xlink:href=concat('#',$ID)]" />
        </noteLocationCorp>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="mods:name[@type='personal']">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:if test="@valueURI">
        <xsl:attribute name="editor.output">
          <xsl:choose>
            <xsl:when test="mods:namePart[@type='family'] and mods:namePart[@type='given']">
              <xsl:value-of select="concat(mods:namePart[@type='family'],', ',mods:namePart[@type='given'])" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="*" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <xsl:message>
        count: <xsl:value-of select="count(*[not((local-name()='namePart'))])"/>
      </xsl:message>
      <nameOrPND>
        <xsl:choose>
          <xsl:when test="mods:namePart[@type='family'] and mods:namePart[@type='given']">
            <xsl:value-of select="concat(mods:namePart[@type='family'],', ',mods:namePart[@type='given'])" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="*" />
          </xsl:otherwise>
        </xsl:choose>
      </nameOrPND>
      <xsl:apply-templates select="*[not((local-name()='namePart'))]" />
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>