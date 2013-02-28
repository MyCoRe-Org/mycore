<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xalan="http://xml.apache.org/xalan">

  <xsl:include href="xslInclude:solr-export" />

  <xsl:template match="*">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="text()">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/">
    <solr-document-container>
      <xsl:apply-templates mode="mcr2Source" select="//mycoreobject|//mycorederivate|//file[not(parent::fileset)]" />
    </solr-document-container>
  </xsl:template>

  <xsl:template mode="mcr2Source" match="mycoreobject|mycorederivate|file">
    <source>
      <xsl:copy-of select="." />
      <user>
        <xsl:apply-templates />
        <xsl:apply-templates mode="user-application" />
      </user>
    </source>
  </xsl:template>

  <xsl:template match="*[@class='MCRMetaClassification']/*">
    <xsl:variable name="classTree" select="document(concat('classification:metadata:0:parents:', @classid, ':', @categid))/mycoreclass/categories//category" />

    <xsl:apply-templates mode="classi2fields" select="$classTree" />

    <!-- if mycore object is not a child -->
    <xsl:if test="@inherited = '0'">
      <xsl:apply-templates mode="classi2fieldsTop" select="$classTree" />
    </xsl:if>

  </xsl:template>

  <xsl:template mode="classi2fields" match="category">
    <xsl:variable name="classid" select="ancestor-or-self::node()[last()]/mycoreclass/@ID" />

    <!-- classid as fieldname -->
    <field name="{$classid}">
      <!-- categid as value -->
      <xsl:value-of select="@ID" />
    </field>

    <field name="category">
      <xsl:value-of select="concat($classid, ':', @ID)" />
    </field>

    <xsl:apply-templates mode="labels" select="label" />
  </xsl:template>

  <xsl:template mode="classi2fieldsTop" match="category">
    <field name="{concat(ancestor-or-self::node()[last()]/mycoreclass/@ID, '.top')}">
      <xsl:value-of select="@ID" />
    </field>
  </xsl:template>

  <xsl:template mode="labels" match="label">
    <field name="content">
      <xsl:value-of select="@text" />
    </field>
  </xsl:template>

  <!-- overwrite this in your application -->
  <xsl:template match="node()" mode="user-application" />

</xsl:stylesheet>
