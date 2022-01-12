<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" 
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mcrmods="http://www.mycore.de/xslt/mods">
  
  <xsl:import href="xslImport:solr-document-3:solr/indexing/mods-dynamicfields-3.xsl" />

  <xsl:import href="resource:xsl/functions/mods.xsl" />

  <xsl:param name="MCR.Solr.DynamicFields" select="'false'" />
  <xsl:param name="MCR.Solr.DynamicFields.excludes" select="''" />

  <xsl:template match="mycoreobject">
    <xsl:apply-imports />
    <xsl:variable name="isExcluded">
      <!-- defined in mycore-solr -->
      <xsl:call-template name="check.excludes" />
    </xsl:variable>
    <xsl:if test="$MCR.Solr.DynamicFields='true' and $isExcluded = 'false'">
      <xsl:comment>
        Start of dynamic fields (for mods):
        Set 'MCR.Solr.DynamicFields=false' to exclude these:
      </xsl:comment>
      <!-- dynamic field for mods -->
      <xsl:for-each select="metadata//mods:*[@authority or @authorityURI]">
        <xsl:if test="mcrmods:is-supported(.)">
          <xsl:variable name="class" select="mcrmods:to-mycoreclass(., 'parent')" />
          <xsl:variable name="classid" select="$class/@ID" />
          <xsl:variable name="classTree" select="$class/categories//category" />
          <xsl:variable name="withTopField" select="not(ancestor::mods:relatedItem)" />
          <xsl:for-each select="$classTree">
            <!-- classid as fieldname -->
            <field name="{$classid}">
              <!-- categid as value -->
              <xsl:value-of select="@ID" />
            </field>
            <xsl:for-each select="label">
              <field name="{$classid}_Label">
                <xsl:value-of select="@text" />
              </field>
              <field name="{$classid}_Label.{@xml:lang}">
                <xsl:value-of select="@text" />
              </field>
            </xsl:for-each>
            <xsl:if test="$withTopField">
              <field name="{$classid}.top">
                <xsl:value-of select="@ID" />
              </field>
            </xsl:if>
          </xsl:for-each>
        </xsl:if>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>
</xsl:stylesheet>
