<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink" 
  xmlns:mcrmods="http://www.mycore.de/xslt/mods"

  exclude-result-prefixes="xlink mods fn mcrmods">
  
  <xsl:import href="xslImport:solr-document:solr/indexing/mods-solr.xsl" />
  <!-- already imported earlier in chain -->
  <!-- <xsl:import href="resource:xsl/functions/mods.xsl" /> -->
  <xsl:include href="resource:xslt/utils/mods-utils.xsl" />
  <xsl:include href="resource:xslt/utils/mods-enhancer.xsl" />
  
  <!--siehe: mir/mods2mods-classmapping.xsl (XSLT3-compatible)-->
  <xsl:include href="xslInclude:mods" />
  
  <xsl:strip-space elements="mods:*" />

  <xsl:template match="mycoreobject[./metadata/def.modsContainer/modsContainer/mods:mods]">
    <xsl:apply-imports />
    <!-- classification fields from mycore-mods -->
    <xsl:apply-templates select="metadata//mods:*[@authority or @authorityURI]|metadata//mods:mods/mods:typeOfResource|metadata//mods:mods/mods:accessCondition" />
    <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" />
    <field name="mods.type">
      <xsl:apply-templates select="metadata/def.modsContainer/modsContainer/mods:mods" mode="mods.type" />
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

  <xsl:template match="mods:*[@authority or @authorityURI]|mods:typeOfResource|mods:accessCondition">
    <xsl:variable name="classdoc" select="mcrmods:to-mycoreclass(., 'parent')" />    
    <xsl:if test="$classdoc">
      <xsl:variable name="topField" select="not(ancestor::mods:relatedItem)" />
      <xsl:variable name="classid" select="$classdoc/@ID" />
      <xsl:apply-templates select="$classdoc//category" mode="category">
        <xsl:with-param name="classid" select="$classid" />
        <xsl:with-param name="withTopField" select="$topField" />
      </xsl:apply-templates>
    </xsl:if>
  </xsl:template>

  <!-- make sure that only one mods:mods element is matched -->
  <xsl:template match="mods:mods[. = ../../modsContainer[1]/mods:mods]">
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
        <xsl:call-template name="printModsName" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:name">
      <field name="mods.name.top">
        <xsl:call-template name="printModsName" />
      </field>
    </xsl:for-each>
    <xsl:for-each
      select=".//mods:name[mods:role/mods:roleTerm[@authority='marcrelator' and (@type='text' and text()='author') or (@type='code' and text()='aut')]]">
      <field name="mods.author">
        <xsl:call-template name="printModsName" />
      </field>
    </xsl:for-each>
    <xsl:for-each select=".//mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm[not(@type='code')]">
      <field name="mods.place">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <!-- publisher is either specified in current metadata or inherited from parent or grandparent-->
    <xsl:choose>
      <xsl:when test="mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher">
        <xsl:apply-templates select="mods:originInfo[@eventType='publication' or (not(@eventType) and not (../mods:originInfo[@eventType='publication']))]" mode="print_publisher_field" />
      </xsl:when>
      <xsl:when test="mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher">
        <xsl:apply-templates select="mods:relatedItem[@type='host']/mods:originInfo[@eventType='publication' or (not(@eventType) and not (../mods:originInfo[@eventType='publication']))]/mods:publisher" mode="print_publisher_field" />
      </xsl:when>
      <xsl:when test=".//mods:relatedItem[@type='host']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher">
        <xsl:apply-templates select=".//mods:relatedItem[@type='host']/mods:originInfo[@eventType='publication' or (not(@eventType) and not (../mods:originInfo[@eventType='publication']))]/mods:publisher" mode="print_publisher_field" />
      </xsl:when>
      <xsl:when test="mods:relatedItem[@type='series']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher">
        <xsl:apply-templates select="mods:relatedItem[@type='series']/mods:originInfo[@eventType='publication' or (not(@eventType) and not (../mods:originInfo[@eventType='publication']))]/mods:publisher" mode="print_publisher_field" />
      </xsl:when>
      <xsl:when test=".//mods:relatedItem[@type='series']/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher">
        <xsl:apply-templates select=".//mods:relatedItem[@type='series']/mods:originInfo[@eventType='publication' or (not(@eventType) and not (../mods:originInfo[@eventType='publication']))]/mods:publisher" mode="print_publisher_field" />
      </xsl:when>
    </xsl:choose>
    <xsl:for-each select="mods:genre[@type='intern']">
      <field name="mods.genre">
        <xsl:value-of select="substring-after(@valueURI,'#')" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:identifier">
      <field name="mods.identifier">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:subject/mods:topic">
      <field name="mods.subject">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:abstract">
      <field name="mods.abstract">
        <xsl:value-of select="text()" />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='w3cdtf']">
      <xsl:sort data-type="number" select="count(ancestor::mods:originInfo[not(@eventType) or @eventType='publication'])" />
      <xsl:if test="position()=1">
        <field name="mods.dateIssued">
          <xsl:value-of select="." />
        </field>
        <xsl:variable name="yearIssued">
          <xsl:choose>
            <xsl:when test="contains(.,'-')">
              <xsl:value-of select="substring-before(.,'-')"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="."/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:if test="not (string(number($yearIssued)) = 'NaN')">
          <field name="mods.yearIssued">
            <xsl:value-of select="$yearIssued" />
          </field>
        </xsl:if>
      </xsl:if>
    </xsl:for-each>
    <xsl:for-each select="mods:originInfo/mods:edition">
      <field name="mods.edition">
        <xsl:value-of select="text()" />
      </field>
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
          <field name="mods.yearIssued.{$type}">
            <xsl:value-of select="substring(.,1,4)" />
          </field>
	  <xsl:if test="$type='host' and not(ancestor::mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued[@encoding='w3cdtf'])">
            <field name="mods.dateIssued">
              <xsl:value-of select="." />
            </field>
            <field name="mods.yearIssued">
              <xsl:value-of select="substring(.,1,4)" />
            </field>
	  </xsl:if>
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
        <xsl:if test="string-length($type) &gt; 0">
          <field name="mods.relatedItem.{$type}">
            <xsl:value-of select="$relatedID"/>
          </field>
        </xsl:if>
      </xsl:if>
      <!-- END MCR-888 -->
    </xsl:for-each>
    <xsl:for-each select="mods:accessCondition[@type='embargo']">
      <field name="mods.embargo">
        <xsl:value-of select="." />
      </field>
      <field name="mods.embargo.date">
        <xsl:value-of select="." />
      </field>
    </xsl:for-each>
    <xsl:for-each select="mods:note">
      <xsl:variable name="type" select="@type" />
      <xsl:choose>
        <xsl:when test="string-length($type) &gt; 0">
          <field name="mods.note.{$type}">
            <xsl:value-of select="text()" />
          </field>
        </xsl:when>
        <xsl:otherwise>
          <field name="mods.note">
            <xsl:value-of select="text()" />
          </field>
        </xsl:otherwise>
      </xsl:choose>
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
        <xsl:call-template name="printModsName" />
      </field>
      <xsl:if test="$topField">
        <xsl:for-each select="mods:name/descendant-or-self::mods:nameIdentifier">
          <field name="mods.nameIdentifier.top">
            <xsl:value-of select="concat(current()/@type, ':', .)" />
          </field>
        </xsl:for-each>
        <field name="mods.name.top">
          <xsl:call-template name="printModsName" />
        </field>
      </xsl:if>
    </doc>
  </xsl:template>


  <xsl:template match="mods:publisher" mode="print_publisher_field">
    <field name="mods.publisher">
      <xsl:call-template name="printModsName" />
    </field>
  </xsl:template>

  <xsl:template name="printModsName">
    <xsl:choose>
      <xsl:when test="mods:displayForm">
        <xsl:value-of select="fn:string-join(fn:normalize-unicode(mods:displayForm), ' ')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="mods:namePart[@type!='date'] | mods:namePart[not(@type)] | text()">
          <xsl:value-of select="concat(' ',fn:normalize-unicode(.))" />
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
