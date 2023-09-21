<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mcrsolr="http://www.mycore.de/xslt/solr">
  
  <xsl:import href="xslImport:solr-document:solr/indexing/mycoreobject-dynamicfields.xsl" />

  <xsl:import href="resource:xslt/functions/solr.xsl" />

  <xsl:param name="MCR.Solr.DynamicFields" select="'false'" />
  <xsl:param name="MCR.Solr.DynamicFields.excludes" select="''" />

  <xsl:template name="check.excludes">
    <xsl:param name="excludes" select="concat(normalize-space($MCR.Solr.DynamicFields.excludes), ',')" />
    <xsl:variable name="exclude" select="substring-before($excludes, ',')" />
    <xsl:variable name="otherExcludes" select="substring-after($excludes, ',')" />

    <xsl:choose>
      <xsl:when test="string-length(normalize-space($exclude))=0">
        <xsl:text>false</xsl:text>
      </xsl:when>
      <xsl:when test="contains(@ID, $exclude)">
        <xsl:text>true</xsl:text>
      </xsl:when>
      <xsl:when test="string-length(normalize-space($otherExcludes))=0">
        <xsl:text>false</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="check.excludes">
          <xsl:with-param name="excludes" select="$otherExcludes" />
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mycoreobject">
    <xsl:apply-imports />
    <xsl:variable name="isExcluded">
      <xsl:call-template name="check.excludes" />
    </xsl:variable>
    <xsl:if test="$MCR.Solr.DynamicFields='true' and $isExcluded = 'false'">
      <xsl:comment>
        Start of dynamic fields:
        Set 'MCR.Solr.DynamicFields=false' to exclude these:
      </xsl:comment>
      <!-- dynamic field for leaf nodes -->
      <xsl:for-each select=".//*[not(*)]">
        <xsl:variable name="element" select="local-name(.)" />

        <xsl:choose>
          <xsl:when test="(($element = 'von') or ($element = 'bis'))">
            <field name="{$element}">
              <xsl:if test="$element = 'von'">
              <xsl:value-of select="mcrsolr:historydate-julian-day-to-date-string(../ivon)" />
              </xsl:if>
              <xsl:if test="$element = 'bis'">
              <xsl:value-of select="mcrsolr:historydate-julian-day-to-date-string(../ibis)" />
              </xsl:if>
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
          select="document(concat('classification:metadata:0:parents:', @classid, ':', fn:encode-for-uri(@categid)))/mycoreclass/categories//category" />
        <xsl:variable name="classid" select="@classid" />
        <xsl:variable name="notInherited" select="@inherited = '0'" />

        <field name="{$classid}.leaf">
          <!-- categid as value -->
          <xsl:value-of select="@categid" />
        </field>

        <xsl:for-each select=" $classTree ">
          <xsl:if test="position() = 1">
            <field name="{$classid}.root">
              <!-- categid as value -->
              <xsl:value-of select="@ID" />
            </field>
          </xsl:if>

          <field name="{$classid}.id.in.position.{position()}">
            <xsl:value-of select="@ID" />
          </field>

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
