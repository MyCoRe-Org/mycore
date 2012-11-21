<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xalan="http://xml.apache.org/xalan">

  <xsl:template match="*">
    <xsl:apply-templates />
  </xsl:template>

  <xsl:template match="/">
    <solr-document-container>
      <source>
        <xsl:copy-of select="." />
      </source>
      <user>
        <xsl:apply-templates />
      </user>
    </solr-document-container>
  </xsl:template>

  <xsl:template match="*[count(child::*) = 0]">

    <!-- index categids -->
    <xsl:if test="@classid">
      <xsl:variable name="notInherited" select="@inherited = 0" />

      <!-- get all parent nodes of the current categ id -->
      <xsl:variable name="classification" select="document(concat('classification:metadata:0:parents:', @classid, ':', @categid))/mycoreclass" />

      <xsl:for-each select="$classification/categories//category">
        <field name="{$classification/@ID}">
          <xsl:value-of select="@ID" />
        </field>

        <!-- index the category labels in any available language -->
        <xsl:for-each select="label">
          <field name="content">
            <xsl:value-of select="@text" />
          </field>
        </xsl:for-each>
      </xsl:for-each>

      <xsl:for-each select="$classification/categories//category/@ID">
        <xsl:if test="$notInherited">
          <field name="{concat($classification/@ID, '.top')}">
            <xsl:value-of select="." />
          </field>
        </xsl:if>
      </xsl:for-each>

    </xsl:if>
  </xsl:template>
</xsl:stylesheet>