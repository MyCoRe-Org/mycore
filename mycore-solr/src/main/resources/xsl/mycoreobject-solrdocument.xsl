<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xalan="http://xml.apache.org/xalan" xmlns:iview2="xalan://org.mycore.iview2.frontend.MCRIView2XSLFunctions"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" exclude-result-prefixes="mcrxsl xalan mods xlink iview2">

  <xsl:import href="xslImport:solr-document" />

  <xsl:template match="/">
    <add>
      <xsl:apply-templates />
    </add>
  </xsl:template>

  <xsl:template match="text()" />

  <xsl:template match="mycoreobject">
    <xsl:variable name="hasImports" select="mcrxsl:hasNextImportStep('solr-document')" />
    <doc>
      <xsl:call-template name="baseFields" />
      <xsl:for-each select="structure/parents/parent">
        <field name="parent">
          <xsl:value-of select="@xlink:href" />
        </field>
      </xsl:for-each>
      <xsl:variable name="derobject" select="structure/derobjects/derobject" />
      <field name="derCount">
        <xsl:value-of select="count($derobject)" />
      </field>
      <xsl:for-each select="$derobject">
        <field name="derivates">
          <xsl:value-of select="@xlink:href" />
        </field>
      </xsl:for-each>
      <xsl:for-each select="./descendant::*[@classid and @categid]">
        <xsl:variable name="classid" select="@classid" />
        <xsl:variable name="uri" select="concat('classification:metadata:0:parents:',@classid,':',@categid)" />
        <xsl:apply-templates select="document($uri)//category" mode="category">
          <xsl:with-param name="classid" select="$classid" />
          <xsl:with-param name="withTopField" select="@inherited = '0'" />
        </xsl:apply-templates>
      </xsl:for-each>
      <xsl:for-each select="metadata//mods:*[@authority or @authorityURI]">
        <xsl:variable name="uri" xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport" select="mcrmods:getClassCategParentLink(.)" />
        <xsl:if test="string-length($uri) &gt; 0">
          <xsl:variable name="classdoc" select="document($uri)" />
          <xsl:variable name="classid" select="$classdoc/mycoreclass/@ID" />
          <xsl:apply-templates select="$classdoc//category" mode="category">
            <xsl:with-param name="classid" select="$classid" />
          <!-- TODO: Currently we do not have to think of releatedItem[@type='host'] here -->
            <xsl:with-param name="withTopField" select="true()" />
          </xsl:apply-templates>
        </xsl:if>
      </xsl:for-each>
      <xsl:for-each select="metadata/*//*[@xlink:title|text()]">
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
    </doc>
  </xsl:template>

  <xsl:template match="mycorederivate">
    <xsl:variable name="hasImports" select="mcrxsl:hasNextImportStep('solr-document')" />
    <doc>
      <xsl:call-template name="baseFields" />
      <xsl:for-each select="derivate/fileset/file/urn | derivate/fileset/@urn">
        <field name="derivateURN">
          <xsl:value-of select="." />
        </field>
      </xsl:for-each>
      <xsl:variable name="iviewMainFile" select="iview2:getSupportedMainFile(@ID)" />
      <xsl:if test="string-length($iviewMainFile) &gt; 0">
        <field name="iviewFile">
          <xsl:value-of select="concat(@xlink:href,$iviewMainFile)" />
        </field>
      </xsl:if>
      <field name="maindoc">
        <xsl:value-of select="derivate/internals/internal/@maindoc"/>
      </field>
    </doc>
  </xsl:template>

  <xsl:template name="baseFields">
    <field name="id">
      <xsl:value-of select="@ID" />
    </field>
    <field name="returnId">
      <xsl:choose>
        <xsl:when test="contains(@ID, '_derivate_')">
          <xsl:value-of select="derivate/linkmetas/linkmeta/@xlink:href" />
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
  </xsl:template>

  <xsl:template match="category" mode="category">
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
      <xsl:apply-templates select="@text|@description" mode="category" />
    </xsl:for-each>
  </xsl:template>
  <xsl:template match="@text|@description" mode="category">
    <field name="allMeta">
      <xsl:value-of select="." />
    </field>
  </xsl:template>

</xsl:stylesheet>
