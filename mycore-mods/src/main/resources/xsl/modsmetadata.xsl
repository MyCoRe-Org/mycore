<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3" exclude-result-prefixes="xlink mcr i18n acl mods"
  version="1.0">

  <xsl:template name="printMetaDate.mods">
    <!-- prints a table row for a given nodeset -->
    <xsl:param name="nodes" />
    <xsl:param name="label" select="i18n:translate(concat('metaData.mods.dictionary.',local-name($nodes[1])))" />
    <xsl:param name="sep" select="''" />
    <xsl:message>
      label:
      <xsl:value-of select="$label" />
    </xsl:message>
    <xsl:message>
      nodes:
      <xsl:value-of select="count($nodes)" />
    </xsl:message>
    <xsl:if test="$nodes">
      <tr>
        <td valign="top" class="metaname">
          <xsl:value-of select="concat($label,':')" />
        </td>
        <td class="metavalue">
          <xsl:variable name="selectPresentLang">
            <xsl:call-template name="selectPresentLang">
              <xsl:with-param name="nodes" select="$nodes" />
            </xsl:call-template>
          </xsl:variable>
          <xsl:for-each select="$nodes">
            <xsl:if test="position()!=1">
              <xsl:choose>
                <xsl:when test="string-length($sep)&gt;0">
                  <xsl:value-of select="$sep" />
                </xsl:when>
                <xsl:otherwise>
                  <br />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
            <xsl:if test="not(@xml:lang) or @xml:lang=$selectPresentLang">
              <xsl:value-of select="normalize-space(.)" />
            </xsl:if>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:extent" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.extent'),':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.page'),': ',mods:start,'-',mods:end)" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:name" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate(concat('metaData.mods.dictionary.',mods:role/mods:roleTerm[@authority='marcrelator'])),':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="concat(mods:namePart[@type='family'],', ',mods:namePart[@type='given'])" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='urn']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="'URN:'" />
      </td>
      <td class="metavalue">
        <xsl:variable name="urn" select="." />
        <a href="http://nbn-resolving.de/urn/resolver.pl?urn={$urn}">
          <xsl:value-of select="$urn" />
        </a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.report">
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
      <xsl:with-param name="sep" select="'; '" />
    </xsl:call-template>
    <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent" />
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.thesis">
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes"
        select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo[not(@type='translated')]/mods:title" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes"
        select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo[@type='translated']/mods:title" />
    </xsl:call-template>
    <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='personal']" />
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
      <xsl:with-param name="sep" select="'; '" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
    </xsl:call-template>
    <xsl:call-template name="printMetaDate.mods">
      <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
    </xsl:call-template>
    <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier[@type='urn']" />
  </xsl:template>

</xsl:stylesheet>