<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xalan="http://xml.apache.org/xalan" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  xmlns:ex="http://exslt.org/dates-and-times" exclude-result-prefixes="mcrxsl xalan mods xlink ex">
  <!-- should really be last stylesheet to be imported -->
  <xsl:import href="xslImport:solr-document:solr-basetemplate.xsl" />
  <xsl:template match="text()" />

  <xsl:template match="mycoreobject">
    <xsl:apply-templates select="." mode="baseFields" />
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
    <field name="worldReadable">
      <xsl:value-of select="mcrxsl:isWorldReadable(@ID)" />
    </field>
    <field name="worldReadableComplete">
      <xsl:value-of select="mcrxsl:isWorldReadableComplete(@ID)" />
    </field>
    <xsl:for-each select="./descendant::*[@classid and @categid]">
      <xsl:variable name="classid" select="@classid" />
      <xsl:variable name="uri" select="concat('classification:metadata:0:parents:', @classid, ':', mcrxsl:encodeURIPath(@categid))" />
      <xsl:apply-templates select="document($uri)//category" mode="category">
        <xsl:with-param name="classid" select="$classid" />
        <xsl:with-param name="withTopField" select="@inherited = '0'" />
      </xsl:apply-templates>
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
  </xsl:template>

  <xsl:template match="mycorederivate">
    <xsl:apply-templates select="." mode="baseFields" />
    <xsl:for-each select="derivate/fileset/file/urn | derivate/fileset/@urn">
      <field name="derivateURN">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <field name="derivateDisplay">
      <xsl:value-of select="not(derivate/@display='false')" />
    </field>
    <field name="maindoc">
      <xsl:value-of select="derivate/internals/internal/@maindoc" />
    </field>
  </xsl:template>

  <xsl:template match="mycoreobject|mycorederivate" mode="baseFields">
    <field name="objectKind">
      <xsl:value-of select="local-name(.)" />
    </field>
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
      <xsl:if test="../@class = 'MCRMetaDerivateLink'">
        <field name="derivateLink">
          <xsl:value-of select="@xlink:href" />
        </field>
      </xsl:if>
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
    <xsl:for-each select="service/servflags/servflag[@type='modifiedby']">
      <field name="modifiedby">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="service/servflags/servflag[@type='createdby']">
      <field name="createdby">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="service/servstates/servstate">
      <field name="state">
        <xsl:value-of select="@categid" />
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
      <xsl:apply-templates select="@text" mode="category" />
    </xsl:for-each>
  </xsl:template>
  <xsl:template match="@text" mode="category">
    <field name="allMeta">
      <xsl:value-of select="." />
    </field>
  </xsl:template>

</xsl:stylesheet>
