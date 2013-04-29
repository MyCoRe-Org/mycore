<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions"
  xmlns:mods="http://www.loc.gov/mods/v3">
  <xsl:template mode="mods-type" match="mycoreobject">
    <xsl:choose>
      <xsl:when
        test="substring-after(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='intern']/@valueURI,'#')='article' or
                      (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:genre='periodical' and
                       ./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier/@type='doi')">
        <xsl:value-of select="'article'" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring-after(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='intern']/@valueURI,'#')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--Template for generated link names and result titles: see mycoreobject.xsl, results.xsl, MyCoReLayout.xsl -->
  <xsl:template priority="1" mode="resulttitle" match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="mods-type">
      <xsl:apply-templates select="." mode="mods-type" />
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$mods-type='confpro'">
        <xsl:apply-templates select="." mode="title.confpro" />
      </xsl:when>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title">
        <xsl:value-of select="mcrxml:shortenText(./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1],70)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@ID" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mycoreobject[contains(@ID,'_mods_')]" mode="title.confpro">
    <xsl:choose>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title">
        <xsl:value-of select="mcrxml:shortenText(./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1],70)" />
      </xsl:when>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
        <xsl:variable name="completeTitle">
          <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
            <xsl:for-each select="mods:namePart[not(@type)]">
              <xsl:choose>
                <xsl:when test="position()=1">
                  <xsl:value-of select="." />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat(' – ',.)" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
            <xsl:if test="mods:namePart[@type='date']">
              <xsl:value-of select="', '" />
              <xsl:value-of select="mods:namePart[@type='date']" />
            </xsl:if>
            <xsl:for-each select="mods:affiliation">
              <xsl:value-of select="concat(', ',.)" />
            </xsl:for-each>
          </xsl:for-each>
        </xsl:variable>
        <xsl:value-of select="mcrxml:shortenText(i18n:translate('component.mods.metaData.dictionary.proceedingOf',$completeTitle),70)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@ID" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>