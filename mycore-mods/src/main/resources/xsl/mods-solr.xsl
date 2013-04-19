<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" exclude-result-prefixes="mods mcrxsl">
  <xsl:import href="xslImport:solr-document:mods-solr.xsl" />

  <xsl:template match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="hasImports" select="mcrxsl:hasNextImportStep('solr-document:mods-solr.xsl')" />
    <!-- fields from mycore-mods -->
    <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods">
      <xsl:with-param name="hasImports" select="$hasImports" />
    </xsl:apply-templates>
    <xsl:if test="$hasImports">
      <xsl:apply-imports />
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:mods">
    <xsl:param name="hasImports" />
    <xsl:for-each select="mods:titleInfo/descendant-or-self::*">
      <field name="mods.title">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select=".//descendant-or-self::*[contains(@valueURI,'http://d-nb.info/gnd/')]">
      <field name="mods.gnd">
        <xsl:value-of select="substring-after(@valueURI,'http://d-nb.info/gnd/')" />
      </field>
    </xsl:for-each>
    <xsl:for-each select=".//mods:name">
      <field name="mods.name">
        <xsl:for-each select="mods:displayForm | mods:namePart | text()">
          <xsl:value-of select="concat(' ',.)" />
        </xsl:for-each>
      </field>
    </xsl:for-each>
    <xsl:for-each
      select="mods:name[mods:role/mods:roleTerm[@authority='marcrelator' and (@type='text' and text()='author') or (@type='code' and text()='aut')]]">
      <field name="mods.author">
        <xsl:for-each select="mods:displayForm | mods:namePart | text()">
          <xsl:value-of select="concat(' ',.)" />
        </xsl:for-each>
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:genre">
      <field name="mods.genre">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:identifier">
      <field name="mods.identifer">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select=".//mods:originInfo/mods:dateIssued">
      <field name="mods.dateIssued">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:if test="$hasImports">
      <xsl:apply-imports />
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>