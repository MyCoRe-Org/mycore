<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" exclude-result-prefixes="xlink mcr i18n acl mods mcrmods" version="1.0">


  <!-- copy this stylesheet to your application and overwrite template matches with highest priority=2  -->

  <!-- xsl:template match="mcr:hit[contains(@id,'_mods_')]" priority="2">

  </xsl:template -->

</xsl:stylesheet>