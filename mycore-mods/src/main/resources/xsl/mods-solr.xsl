<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xalan="http://xml.apache.org/xalan"
  exclude-result-prefixes="xalan xlink mods mcrxsl">
  <xsl:import href="xslImport:solr-document:mods-solr.xsl" />
  <xsl:include href="mods-utils.xsl" />
  <xsl:include href="mods2mods.xsl" />
  <xsl:include href="xslInclude:mods" />

  <xsl:strip-space elements="mods:*" />

  <xsl:template match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:apply-imports />
    <!-- classification fields from mycore-mods -->
    <xsl:apply-templates select="metadata//mods:*[@authority or @authorityURI]" />
    <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" />
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

  <xsl:template match="mods:*[@authority or @authorityURI]">
    <xsl:variable name="uri" xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport" select="mcrmods:getClassCategParentLink(.)" />
    <xsl:if test="string-length($uri) &gt; 0">
      <xsl:variable name="topField" select="not(ancestor::mods:relatedItem)" />
      <xsl:variable name="classdoc" select="document($uri)" />
      <xsl:variable name="classid" select="$classdoc/mycoreclass/@ID" />
      <xsl:apply-templates select="$classdoc//category" mode="category">
        <xsl:with-param name="classid" select="$classid" />
        <xsl:with-param name="withTopField" select="$topField" />
      </xsl:apply-templates>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:mods">
    <xsl:for-each select="mods:titleInfo/descendant-or-self::*[text()]">
      <field name="mods.title">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <field name="mods.title.main">
      <xsl:apply-templates mode="mods.title" select="." />
    </field>
    <field name="mods.title.subtitle">
      <xsl:apply-templates mode="mods.subtitle" select="." />
    </field>
    <xsl:for-each select=".//descendant-or-self::mods:nameIdentifier">
      <field name="mods.nameIdentifier">
        <xsl:value-of select="concat(current()/@type, ':', .)" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:name/descendant-or-self::mods:nameIdentifier">
      <field name="mods.nameIdentifier.top">
        <xsl:value-of select="concat(current()/@type, ':', .)" />
      </field>
    </xsl:for-each>
    <xsl:apply-templates select=".//mods:name" mode="childdoc" />
    <!-- keep mods:name fields for legacy reasons -->
    <xsl:for-each select=".//mods:name">
      <field name="mods.name">
        <xsl:for-each select="mods:displayForm | mods:namePart[@type!='date'] | text()">
          <xsl:value-of select="concat(' ',mcrxsl:normalizeUnicode(.))" />
        </xsl:for-each>
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:name">
      <field name="mods.name.top">
        <xsl:for-each select="mods:displayForm | mods:namePart[@type!='date'] | text()">
          <xsl:value-of select="concat(' ',mcrxsl:normalizeUnicode(.))" />
        </xsl:for-each>
      </field>
    </xsl:for-each>
    <xsl:for-each
      select=".//mods:name[mods:role/mods:roleTerm[@authority='marcrelator' and (@type='text' and text()='author') or (@type='code' and text()='aut')]]">
      <field name="mods.author">
        <xsl:for-each select="mods:displayForm | mods:namePart | text()">
          <xsl:value-of select="concat(' ',mcrxsl:normalizeUnicode(.))" />
        </xsl:for-each>
      </field>
    </xsl:for-each>
    <xsl:for-each select=".//mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm[not(@type='code')]">
      <field name="mods.place">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each
      select=".//mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher">
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
    <xsl:for-each select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued">
      <xsl:sort data-type="number" select="count(ancestor::mods:originInfo[not(@eventType) or @eventType='publication'])" />
      <xsl:if test="position()=1">
        <field name="mods.dateIssued">
          <xsl:value-of select="." />
        </field>
      </xsl:if>
    </xsl:for-each>
    <!-- add allMeta from parent -->
    <xsl:for-each select="mods:relatedItem">
      <xsl:variable name="type" select="@type" />
      <xsl:for-each select="mods:titleInfo/descendant-or-self::*[text()]">
        <field name="mods.title.{$type}">
          <xsl:value-of select="text()" />
        </field>
      </xsl:for-each>
      <xsl:for-each select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued">
        <xsl:sort data-type="number" select="count(ancestor::mods:originInfo[not(@eventType) or @eventType='publication'])" />
        <xsl:if test="position()=1">
          <field name="mods.dateIssued.{$type}">
            <xsl:value-of select="." />
          </field>
        </xsl:if>
      </xsl:for-each>
      <xsl:for-each select=".//*[@xlink:title|text()]">
        <xsl:for-each select="text()|@xlink:title">
          <xsl:variable name="trimmed" select="normalize-space(.)" />
          <xsl:if test="string-length($trimmed) &gt; 0">
            <field name="allMeta">
              <xsl:value-of select="$trimmed" />
            </field>
          </xsl:if>
        </xsl:for-each>
      </xsl:for-each>
      <xsl:for-each select="mods:identifier">
        <field name="mods.identifier.{$type}">
          <xsl:value-of select="text()" />
        </field>
      </xsl:for-each>
      <!-- START MCR-888 -->
      <xsl:variable name="relatedID">
        <xsl:choose>
          <xsl:when test="contains(@xlink:href,'_mods_')">
            <xsl:value-of select="@xlink:href" />
          </xsl:when>
          <xsl:when test="@type='host'">
            <xsl:value-of select="ancestor-or-self::mycoreobject/structure/parents/parent/@xlink:href" />
          </xsl:when>
        </xsl:choose>
      </xsl:variable>
      <xsl:if test="$relatedID">
        <field name="mods.relatedItem">
          <xsl:value-of select="$relatedID" />
        </field>
        <field name="mods.relatedItem.{$type}">
          <xsl:value-of select="$relatedID" />
        </field>
        <field name="mods.relatedItem.{$relatedID}">
          <xsl:value-of select="$type" />
        </field>
      </xsl:if>
      <!-- END MCR-888 -->
    </xsl:for-each>
    <xsl:for-each select="mods:accessCondition[@type='embargo']">
      <field name="mods.embargo">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:accessCondition[@type='use and reproduction']">
      <field name="mods.rights">
        <xsl:variable name="trimmed" select="normalize-space(.)" />
        <xsl:choose>
          <xsl:when test="contains($trimmed, 'cc_by')">
            <xsl:apply-templates select="." mode="cc-text" />
          </xsl:when>
          <xsl:when test="contains($trimmed, 'rights_reserved')">
            <xsl:apply-templates select="." mode="rights_reserved" />
          </xsl:when>
          <xsl:when test="contains($trimmed, 'oa_nlz')">
            <xsl:apply-templates select="." mode="oa_nlz" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="." />
          </xsl:otherwise>
        </xsl:choose>
      </field>
    </xsl:for-each>
  </xsl:template>
  <xsl:template match="mods:name" mode="childdoc">
    <xsl:variable name="topField" select="not(ancestor::mods:relatedItem)" />
    <doc>
      <field name="id">
        <xsl:value-of select="concat(ancestor::mycoreobject/@ID,'-',generate-id(.))" />
      </field>
      <xsl:apply-templates select=".//mods:*[@authority or @authorityURI]" />
      <xsl:for-each select=".//descendant-or-self::mods:nameIdentifier">
        <field name="mods.nameIdentifier">
          <xsl:value-of select="concat(current()/@type, ':', .)" />
        </field>
      </xsl:for-each>
      <field name="mods.name">
        <xsl:for-each select="mods:displayForm | mods:namePart[@type!='date'] | text()">
          <xsl:value-of select="concat(' ',.)" />
        </xsl:for-each>
      </field>
      <xsl:if test="$topField">
        <xsl:for-each select="mods:name/descendant-or-self::mods:nameIdentifier">
          <field name="mods.nameIdentifier.top">
            <xsl:value-of select="concat(current()/@type, ':', .)" />
          </field>
        </xsl:for-each>
        <field name="mods.name.top">
          <xsl:for-each select="mods:displayForm | mods:namePart[@type!='date'] | text()">
            <xsl:value-of select="concat(' ',.)" />
          </xsl:for-each>
        </field>
      </xsl:if>
    </doc>
  </xsl:template>
</xsl:stylesheet>