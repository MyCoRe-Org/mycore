<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xalan="http://xml.apache.org/xalan" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  exclude-result-prefixes="mcrxsl xalan mods xlink">

  <xsl:import href="xslImport:solr-document" />

  <xsl:template match="/">
    <doc>
      <xsl:apply-templates />
    </doc>
  </xsl:template>

  <xsl:template match="/*[@ID]">
    <xsl:variable name="hasImports" select="mcrxsl:hasNextImportStep('solr-document')" />
    <field name="id">
      <xsl:value-of select="@ID" />
    </field>
    <field name="returnId">
      <xsl:choose>
        <xsl:when test="contains(@ID, '_derivate_')">
          <xsl:value-of select="/mycorederivate/derivate/linkmetas/linkmeta/@xlink:href" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@ID" />
        </xsl:otherwise>
      </xsl:choose>
    </field>
    <field name="objectProject">
      <xsl:value-of select="substring-before(@ID,'_')" />
    </field>
    <field name="objectType">
      <xsl:value-of select="substring-before(substring-after(@ID,'_'),'_')" />
    </field>
    <xsl:for-each select="descendant::*[@xlink:href]">
      <field name="link">
        <xsl:value-of select="@xlink:href" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="/mycoreobject/structure/parents/parent">
      <field name="parent">
        <xsl:value-of select="@xlink:href" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="service/servdates/servdate[@type='modifydate']">
      <field name="modified">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="service/servdates/servdate[@type='createdate']">
      <field name="created">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="/mycorederivate/derivate/fileset/file/urn | /mycorederivate/derivate/fileset/@urn">
      <field name="derivateURN">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="/mycoreobject/descendant::*[@classid and @categid]">
      <xsl:variable name="classid" select="@classid" />
      <xsl:variable name="uri" select="concat('classification:metadata:0:parents:',@classid,':',@categid)" />
      <xsl:apply-templates select="document($uri)//category">
        <xsl:with-param name="classid" select="$classid" />
        <xsl:with-param name="withTopField" select="@inherited = '0'" />
      </xsl:apply-templates>
    </xsl:for-each>
    <xsl:for-each select="/mycoreobject/metadata//mods:*[@authority or @authorityURI]">
      <xsl:variable name="uri" xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport" select="mcrmods:getClassCategParentLink(.)" />
      <xsl:if test="string-length($uri) &gt; 0">
        <xsl:variable name="classdoc" select="document($uri)" />
        <xsl:variable name="classid" select="$classdoc/mycoreclass/@ID" />
        <xsl:apply-templates select="$classdoc//category">
          <xsl:with-param name="classid" select="$classid" />
          <!-- TODO: Currently we do not have to think of releatedItem[@type='host'] here -->
          <xsl:with-param name="withTopField" select="true()" />
        </xsl:apply-templates>
      </xsl:if>
    </xsl:for-each>
    <xsl:for-each select="/mycoreobject/metadata/*//*[@xlink:title|text()]">
      <xsl:for-each select="text()|@xlink:title">
        <xsl:variable name="trimmed" select="normalize-space(.)" />
        <xsl:if test="string-length($trimmed) &gt; 0">
          <field name="allMeta">
            <xsl:value-of select="$trimmed" />
          </field>
        </xsl:if>
      </xsl:for-each>
    </xsl:for-each>
    <xsl:if test="$hasImports">
      <xsl:apply-imports />
    </xsl:if>
  </xsl:template>

  <xsl:template match="/mycoreclass//category">
    <xsl:param name="withTopField" select="true()" />
    <xsl:param name="classid" select="/mycoreclass/@ID" />
    <field name="category">
      <xsl:value-of select="concat($classid,':',@ID)" />
    </field>
    <xsl:if test="$withTopField">
      <field name="category.top">
        <xsl:value-of select="concat($classid,':',@ID)" />
      </field>
    </xsl:if>
    <xsl:for-each select="label">
      <xsl:if test="@text">
        <field name="allMeta">
          <xsl:value-of select="@text" />
        </field>
      </xsl:if>
      <xsl:if test="@description">
        <field name="allMeta">
          <xsl:value-of select="@description" />
        </field>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
