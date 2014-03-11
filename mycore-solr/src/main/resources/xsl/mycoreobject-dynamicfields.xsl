<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xalan="http://xml.apache.org/xalan" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions"
  exclude-result-prefixes="xalan xlink mods mcrxsl">

  <xsl:import href="xslImport:solr-document:mycoreobject-dynamicfields.xsl" />
  <xsl:param name="MCR.Module-solr.DynamicFields" select="'true'" />

  <xsl:template match="mycoreobject">
    <xsl:apply-imports />
    <xsl:if test="$MCR.Module-solr.DynamicFields='true'">
      <xsl:comment>
        Start of dynamic fields:
        Set 'MCR.Module-solr.DynamicFields=false' to exclude these:
      </xsl:comment>
      <!-- dynamic field for leaf nodes -->
      <xsl:for-each select=".//*[not(*)]">
        <xsl:variable name="element" select="local-name(.)" />

        <xsl:choose>
          <xsl:when test="(($element = 'von') or ($element = 'bis'))">
            <field name="{$element}">
              <xsl:value-of select="mcrxsl:getISODateFromMCRHistoryDate(.)" />
            </field>
            <xsl:for-each select="./@*">
              <!-- <elementName>.<attribute.name>.<attrVal> -->
              <field name="{concat($element, '.', local-name(.), '.', translate(.,'|$?#[]{}&amp; ','__________'))}">
                <xsl:value-of select="../." />
              </field>
            </xsl:for-each>
          </xsl:when>
          <xsl:when test="string-length(.) &gt; 0">
            <field name="{$element}">
              <xsl:value-of select="." />
            </field>
            <xsl:for-each select="./@*">
              <!-- <elementName>.<attribute.name>.<attrVal> -->
              <field name="{concat($element, '.', local-name(.), '.', translate(.,'|$?#[]{}&amp; ','__________'))}">
                <xsl:value-of select="../." />
              </field>
            </xsl:for-each>
          </xsl:when>
        </xsl:choose>


        <xsl:for-each select="./@*">
          <field name="{concat($element, '.', local-name(.))}">
            <xsl:value-of select="." />
          </field>
        </xsl:for-each>
      </xsl:for-each>
      <!-- dynamic class fields -->
      <xsl:for-each select="metadata/*[@class='MCRMetaClassification']/*">
        <xsl:variable name="classTree"
          select="document(concat('classification:metadata:0:parents:', @classid, ':', @categid))/mycoreclass/categories//category" />
        <xsl:variable name="classid" select="@classid" />
        <xsl:variable name="notInherited" select="@inherited = '0'" />

        <xsl:for-each select="$classTree">
          <xsl:if test="position() = 1">
            <field name="{$classid}.root">
              <!-- categid as value -->
              <xsl:value-of select="@ID" />
            </field>
          </xsl:if>

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
          <xsl:if test="$notInherited">
            <field name="{$classid}.top">
              <xsl:value-of select="@ID" />
            </field>
          </xsl:if>
        </xsl:for-each>
      </xsl:for-each>
      <!-- and once again for mods -->
      <xsl:for-each select="metadata//mods:*[@authority or @authorityURI]">
        <xsl:variable name="uri" xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport" select="mcrmods:getClassCategParentLink(.)" />
        <xsl:if test="string-length($uri) &gt; 0">
          <xsl:variable name="class" select="document($uri)" />
          <xsl:variable name="classid" select="document($uri)/mycoreclass/@ID" />
          <xsl:variable name="classTree" select="$class/mycoreclass/categories//category" />
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
            <!-- TODO: Currently we do not have to think of releatedItem[@type='host'] here -->
            <field name="{$classid}.top">
              <xsl:value-of select="@ID" />
            </field>
          </xsl:for-each>
        </xsl:if>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <!-- Allows to dynamically create fields from remote XML e.g. GND -->
  <xsl:template name="DynamicFieldsFromRemoteXMLContent">
    <xsl:param name="url" />
    <xsl:if test="$url">
      <xsl:variable name="xml" select="document($url)" />

      <xsl:for-each select="$xml//*[not(*)]">
        <xsl:variable name="element" select="local-name(.)" />

        <xsl:choose>
          <xsl:when test="string-length(.) &gt; 0">
            <field name="{local-name(.)}">
              <xsl:value-of select="." />
            </field>

            <xsl:for-each select="./@*">
              <!-- <elementName>.<attribute.name>.<attrVal> -->
              <field name="{concat($element, '.', local-name(.), '.', translate(.,'|$?#[]{}&amp; ','__________'))}">
                <xsl:value-of select="../." />
              </field>
            </xsl:for-each>
          </xsl:when>
        </xsl:choose>

        <!-- every attribute -->
        <xsl:for-each select="./@*">
          <field name="{concat($element, '.', local-name(.))}">
            <xsl:value-of select="." />
          </field>
        </xsl:for-each>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
