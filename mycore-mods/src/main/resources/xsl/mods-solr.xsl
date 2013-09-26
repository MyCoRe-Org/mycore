<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xalan="http://xml.apache.org/xalan" exclude-result-prefixes="xalan xlink mods mcrxsl">
  <xsl:import href="xslImport:solr-document:mods-solr.xsl" />
  <xsl:include href="mods-utils.xsl" />
  <xsl:include href="mods2mods.xsl" />
  <xsl:include href="xslInclude:mods" />
  
  <xsl:strip-space elements="mods:*"/>

  <xsl:template match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:apply-imports />
    <xsl:variable name="resolved">
      <xsl:apply-templates select="." mode="mods" />
    </xsl:variable>
    <xsl:variable name="fullyResolved" select="xalan:nodeset($resolved)" />
    
    <!-- fields from mycore-mods -->
    <xsl:apply-templates select="$fullyResolved/mods:mods" />
    <field name="mods.type">
      <xsl:apply-templates select="." mode="mods-type" />
    </field>
    <field name="search_result_link_text">
      <xsl:apply-templates select="." mode="resulttitle" />
    </field>
    <xsl:for-each select="structure/parents/parent">
      <xsl:variable name="parent" select="document(concat('mcrobject:',@xlink:href))/mycoreobject" />
      <field name="parentLinkText">
        <xsl:apply-templates select="$parent" mode="resulttitle" />
      </field>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="mods:mods">
    <xsl:for-each select="mods:titleInfo/descendant-or-self::*[text()]">
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
      select=".//mods:name[mods:role/mods:roleTerm[@authority='marcrelator' and (@type='text' and text()='author') or (@type='code' and text()='aut')]]">
      <field name="mods.author">
        <xsl:for-each select="mods:displayForm | mods:namePart | text()">
          <xsl:value-of select="concat(' ',.)" />
        </xsl:for-each>
      </field>
    </xsl:for-each>
    <xsl:for-each select=".//mods:originInfo/mods:place/mods:placeTerm[not(@type='code')]">
      <field name="mods.place">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each
      select=".//mods:originInfo/mods:publisher|.//mods:name[mods:role/mods:roleTerm[@authority='marcrelator' and (@type='text' and text()='publisher') or (@type='code' and text()='pbl')]]">
      <field name="mods.publisher">
        <xsl:for-each select="mods:displayForm | mods:namePart | text()">
          <xsl:value-of select="concat(' ',.)" />
        </xsl:for-each>
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:genre[text()]">
      <field name="mods.genre">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:identifier">
      <field name="mods.identifier">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:abstract[1]">
      <field name="mods.abstract">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select=".//mods:originInfo/mods:dateIssued">
      <xsl:sort data-type="number" select="count(ancestor::mods:originInfo)" />
      <xsl:if test="position()=1">
        <field name="mods.dateIssued">
          <xsl:value-of select="." />
        </field>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>