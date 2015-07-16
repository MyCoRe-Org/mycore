<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mods="http://www.loc.gov/mods/v3"
  exclude-result-prefixes="xlink mcr i18n acl mods mcrmods" version="1.0">


  <!-- copy this stylesheet to your application and overwrite template matches with highest priority=1  -->

  <!-- xsl:template priority="1" mode="present" match="/mycoreobject[contains(@ID,'_mods_')]">

  </xsl:template -->


  <xsl:template mode="printDerivatesThumb" match="/mycoreobject[contains(@ID,'_mods_')]" priority="1">
    <!-- do nothing ... define this for your own application  -->
  </xsl:template>

  <xsl:template match="/mycoreobject" mode="breadCrumb" priority="1">

    <ul class="breadcrumb">
      <xsl:variable name="obj_host">
        <xsl:value-of select="$objectHost" />
      </xsl:variable>
      <xsl:if test="./structure/parents">
        <xsl:variable name="parent_genre">
          <xsl:apply-templates mode="mods-type" select="document(concat('mcrobject:',./structure/parents/parent/@xlink:href))/mycoreobject" />
        </xsl:variable>
        <li>
          <xsl:value-of select="i18n:translate(concat('component.mods.metaData.dictionary.', $parent_genre))" />
          <xsl:text>: </xsl:text>
          <xsl:apply-templates select="./structure/parents">
            <xsl:with-param name="obj_host" select="$obj_host" />
            <xsl:with-param name="obj_type" select="'this'" />
          </xsl:apply-templates>
          <xsl:apply-templates select="./structure/parents">
            <xsl:with-param name="obj_host" select="$obj_host" />
            <xsl:with-param name="obj_type" select="'before'" />
          </xsl:apply-templates>
          <xsl:apply-templates select="./structure/parents">
            <xsl:with-param name="obj_host" select="$obj_host" />
            <xsl:with-param name="obj_type" select="'after'" />
          </xsl:apply-templates>
        </li>
      </xsl:if>

      <xsl:variable name="internal_genre">
        <xsl:apply-templates mode="mods-type" select="." />
      </xsl:variable>
      <li>
        <xsl:value-of select="i18n:translate(concat('component.mods.metaData.dictionary.', $internal_genre))" />
      </li>
      <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
        <li>
          <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
            mode="printModsClassInfo" />
        </li>
      </xsl:if>
    </ul>

  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="externalObjectActions">
    <!-- add here your application specific edit menu entries, as <li />  -->
  </xsl:template>

</xsl:stylesheet>