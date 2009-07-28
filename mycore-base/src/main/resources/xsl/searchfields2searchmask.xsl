<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet generates a search mask from searchfields.xml configuration file  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                              xmlns:mcr="http://www.mycore.org/" 
                              xmlns:xalan="http://xml.apache.org/xalan"
                              exclude-result-prefixes="mcr xalan">

<xsl:output indent="yes" method="xml" encoding="UTF-8" xalan:indent-amount="2"/>

<!-- ==================================================== -->
<!--                  XSL Parameters                      -->
<!-- ==================================================== -->

<!-- Build a webpage file or the included editor file? -->
<xsl:param name="mode" select="'editor'" />

<!-- Filename of the editor definition file -->
<xsl:param name="filename.editor" />

<!-- Filename of the webpage that contains the search mask -->
<xsl:param name="filename.webpage" />

<!-- Title of webpage in german -->
<xsl:param name="title.de" />

<!-- Title of webpage in english -->
<xsl:param name="title.en" />

<!-- i18n key of search mask headline -->
<xsl:param name="headline.i18n" />

<!-- ID of the search index(es) to include, multiple indexes separated by blanks -->
<!-- If this property is set, by default all fields in those indexes are included -->
<xsl:param name="search.indexes" />

<!-- Name(s) of the search fields to skip, separated by blanks. -->
<!-- If this property and search.indexes is set, the listed fields are NOT included in search mask -->
<xsl:param name="skip.fields" />

<!-- Name(s) of the search fields to include, separated by blanks. -->
<!-- If this property is set, ONLY these search fields will be included in search mask -->
<xsl:param name="search.fields" />

<!-- Optional restriction (hidden condition) for search -->
<!-- Syntax: "field operator value", separated by blanks -->
<xsl:param name="restriction" />

<!-- List of fields to include as sort criteria of results -->
<!-- If empty, panel is not displayed -->
<xsl:param name="sort.fields" />

<!-- If true, include a panel to select hosts to query -->
<xsl:param name="include.hostsSelectionPanel" select="'true'" />

<!-- Layout of search mask: simple or advanced -->
<!-- simple: one line per field, default operator, combine conditions with and -->
<!-- advanced: choose fields and operators from drop-down list, choose and/or -->
<xsl:param name="layout" select="'simple'" />

<!-- ==================================================== -->
<!--                 Global variables                     -->
<!-- ==================================================== -->

<xsl:variable name="fieldtypes" select="document('fieldtypes.xml',.)/mcr:fieldtypes" />

<xsl:variable name="list.of.fields">
  <xsl:choose>
    <xsl:when test="string-length(normalize-space($search.fields)) &gt; 0">
      <xsl:value-of select="$search.fields" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:for-each select="mcr:index[contains(concat(' ',$search.indexes,' '),concat(' ',@id,' '))]/mcr:field[@source != 'searcherHitMetadata']">
        <xsl:choose>
          <xsl:when test="string-length(normalize-space($skip.fields)) = 0">
            <xsl:value-of select="@name" />
            <xsl:text> </xsl:text>
          </xsl:when>
          <xsl:when test="contains(concat(' ',$skip.fields,' '),concat(' ',@name,' '))" />
          <xsl:otherwise>
            <xsl:value-of select="@name" />
            <xsl:text> </xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </xsl:otherwise>
  </xsl:choose>
</xsl:variable>

<!-- ==================================================== -->
<!--                   Transformation                     -->
<!-- ==================================================== -->

<xsl:template match="/">
  <xsl:if test="$mode = 'editor'">
    <xsl:apply-templates select="mcr:searchfields" />
  </xsl:if>
  <xsl:if test="$mode = 'webpage'">
    <xsl:call-template name="webpage" />
  </xsl:if>
</xsl:template>

<!-- ==================================================== -->
<!--                   Build webpage                      -->
<!-- ==================================================== -->

<xsl:template name="webpage">
  <MyCoReWebPage>
    <section title="{$title.de}" xml:lang="de">
      <editor id="searchmask-de">
        <include uri="webapp:editor/{$filename.editor}"/>
      </editor>
    </section>
    <section title="{$title.en}" xml:lang="en">
      <editor id="searchmask-en">
        <include uri="webapp:editor/{$filename.editor}"/>
      </editor>
    </section>
  </MyCoReWebPage>
</xsl:template>

<!-- ==================================================== -->
<!--                    Build editor                      -->
<!-- ==================================================== -->

<xsl:template match="mcr:searchfields">
  <editor id="searchmask">

    <source>
      <xsl:attribute name="uri">request:servlets/MCRSearchServlet?mode=load&amp;id={id}</xsl:attribute>
    </source>
    <target type="servlet" name="MCRSearchServlet" method="post" format="xml" />

    <components root="root" var="/query">
      <headline anchor="WEST">
        <text i18n="{$headline.i18n}"/>
      </headline>

      <panel id="root">
      
        <hidden var="@mask" default="{$filename.webpage}" />
        <hidden var="conditions/@format" default="xml" />
        <hidden var="conditions/boolean/@operator" default="and" />

        <xsl:if test="$layout = 'simple'">
          <xsl:call-template name="build.from.list">
            <xsl:with-param name="fields" select="$list.of.fields" /> 
          </xsl:call-template>
          <xsl:call-template name="spacer" />
        </xsl:if>
        <xsl:if test="$layout = 'advanced'">
          <xsl:call-template name="build.advanced" />
          <xsl:call-template name="spacer" />
          <xsl:call-template name="choose.and.or" />
        </xsl:if>
        
        <xsl:if test="string-length(normalize-space($restriction)) &gt; 0">
          <xsl:call-template name="restriction" />
        </xsl:if>
        
        <xsl:if test="$include.hostsSelectionPanel = 'true'">
          <xsl:call-template name="hosts" />
        </xsl:if>
        
        <xsl:if test="string-length(normalize-space($sort.fields)) &gt; 0">
          <xsl:call-template name="sortBy" />
        </xsl:if>
        
        <xsl:call-template name="maxResultsNumPerPage" />

        <cell row="99" col="1" colspan="2" anchor="EAST">
          <submitButton i18n="editor.search.search" width="150px" />
        </cell>
        
      </panel>
 
      <xsl:call-template name="includes" />      
     
    </components>
  </editor>
</xsl:template>

<!-- ==================================================== -->
<!--            Build advanced search mask                -->
<!-- ==================================================== -->

<xsl:template name="build.advanced">
  <xsl:variable name="used.types">
    <xsl:call-template name="build.used.types.list">
      <xsl:with-param name="list" select="$list.of.fields" />
    </xsl:call-template>
  </xsl:variable>
  
  <xsl:call-template name="build.type.selectors">
    <xsl:with-param name="list" select="$used.types" />
  </xsl:call-template>
</xsl:template>

<xsl:template name="build.used.types.list">
  <xsl:param name="list" />
  <xsl:param name="used" />
  
  <xsl:if test="string-length(normalize-space($list)) &gt; 0">
    <xsl:variable name="tmp" select="concat(normalize-space($list),' ')" />
    <xsl:variable name="field" select="mcr:index/mcr:field[@name=substring-before($tmp,' ')]" />
    <xsl:variable name="type">
      <xsl:choose>
        <xsl:when test="$field/@type='identifier' and $field/@source='objectCategory'">
          <xsl:value-of select="concat('@',$field/@classification)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$field/@type" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:if test="not(contains(concat(' ',$used,' '),concat(' ',$type,' ')))">
      <xsl:value-of select="$type" />
      <xsl:text> </xsl:text>
    </xsl:if>
    
    <xsl:call-template name="build.used.types.list">
      <xsl:with-param name="list" select="substring-after($tmp,' ')" />
      <xsl:with-param name="used" select="concat($used,' ',$type)" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<xsl:template name="build.type.selectors">
  <xsl:param name="list" />
  <xsl:param name="pos" select="1" />
  
  <xsl:if test="string-length(normalize-space($list)) &gt; 0">
    <xsl:variable name="tmp" select="concat(normalize-space($list),' ')" />

    <xsl:variable name="type" select="normalize-space(substring-before($tmp,' '))" />    

    <xsl:comment> Search for fields of type <xsl:value-of select="$type" /><xsl:text> </xsl:text></xsl:comment>

    <xsl:variable name="fields.for.type">
      <xsl:call-template name="build.fields.for.type">
        <xsl:with-param name="list" select="$list.of.fields" />
        <xsl:with-param name="type" select="$type" />
      </xsl:call-template>        
    </xsl:variable>
    
    <xsl:if test="starts-with($type,'@')">
      <hidden var="conditions/boolean/boolean/condition{$pos}/@operator" default="=" /> 
    </xsl:if>
    <xsl:if test="not(contains(normalize-space($fields.for.type),' '))">
      <hidden var="conditions/boolean/boolean/condition{$pos}/@field" default="{normalize-space($fields.for.type)}" /> 
    </xsl:if>
    
    <cell row="{number($pos)*2}" col="1" colspan="2" anchor="NORTHWEST" var="conditions/boolean/boolean/condition{$pos}">
      <repeater min="1" max="10" arrows="false">
        <panel>
        
          <cell row="1" col="1" anchor="EAST" width="200px" var="@field">
            <xsl:choose>
              <xsl:when test="contains(normalize-space($fields.for.type),' ')">
                <list type="dropdown">
                  <xsl:call-template name="build.field.items">
                    <xsl:with-param name="list" select="$fields.for.type" />
                  </xsl:call-template>        
                </list>
              </xsl:when>
              <xsl:otherwise>
                <text i18n="{mcr:index/mcr:field[@name=normalize-space($fields.for.type)]/@i18n}" />
              </xsl:otherwise>
            </xsl:choose>
            <space width="200px" height="0px" />
          </cell>    
          <xsl:choose>
            <xsl:when test="starts-with($type,'@')">
              <cell row="1" col="2" anchor="WEST" var="@value"> 
                <list type="dropdown">
                  <item value="" i18n="editor.search.choose" />
                  <include uri="classification:editor[TextCounter]:2:children:{substring-after($type,'@')}" cacheable="false" />
                </list>
              </cell>
            </xsl:when>
            <xsl:otherwise>
              <cell row="1" col="2" anchor="WEST" var="@operator" ref="operators.{$type}" />
              <cell row="1" col="3" anchor="WEST" var="@value" ref="input.{$type}" />
            </xsl:otherwise>
          </xsl:choose>
    
        </panel>
      </repeater>
    </cell>    
    
    <xsl:call-template name="build.type.selectors">
      <xsl:with-param name="list" select="substring-after($tmp,' ')" />
      <xsl:with-param name="pos" select="$pos + 1" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<xsl:template name="build.fields.for.type">
  <xsl:param name="list" />
  <xsl:param name="type" />
  
  <xsl:if test="string-length(normalize-space($list)) &gt; 0">
    <xsl:variable name="tmp" select="concat(normalize-space($list),' ')" />
    <xsl:variable name="fname" select="substring-before($tmp,' ')" />
    <xsl:variable name="field" select="mcr:index/mcr:field[@name=$fname]" />
    <xsl:variable name="myType">
      <xsl:choose>
        <xsl:when test="$field/@type='identifier' and $field/@source='objectCategory'">
          <xsl:value-of select="concat('@',$field/@classification)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$field/@type" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    
    <xsl:if test="$myType=$type">
      <xsl:value-of select="concat($field/@name,' ')" />
    </xsl:if>
    
    <xsl:call-template name="build.fields.for.type">
      <xsl:with-param name="list" select="substring-after($tmp,' ')" />
      <xsl:with-param name="type" select="$type" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<xsl:template name="build.field.items">
  <xsl:param name="list" />
  
  <xsl:if test="string-length(normalize-space($list)) &gt; 0">
    <xsl:variable name="tmp" select="concat(normalize-space($list),' ')" />
    <xsl:variable name="fname" select="substring-before($tmp,' ')" />
    <xsl:variable name="field" select="mcr:index/mcr:field[@name=$fname]" />

    <item value="{$field/@name}" i18n="{$field/@i18n}" />
    
    <xsl:call-template name="build.field.items">
      <xsl:with-param name="list" select="substring-after($tmp,' ')" />
    </xsl:call-template>
  </xsl:if>
</xsl:template>

<!-- ==================================================== -->
<!--     Build hidden search condition as restriction     -->
<!-- ==================================================== -->

<xsl:template name="restriction">
  <xsl:comment> Search only for <xsl:value-of select="$restriction" />
    <xsl:text> </xsl:text>
  </xsl:comment>
  
  <hidden var="conditions/boolean/condition94/@field"    
    default="{normalize-space(substring-before(normalize-space($restriction),' '))}" />
  <hidden var="conditions/boolean/condition94/@operator" 
    default="{normalize-space(substring-before(normalize-space(substring-after(normalize-space($restriction),' ')),' '))}" />
  <hidden var="conditions/boolean/condition94/@value"
    default="{normalize-space(substring-after(normalize-space(substring-after(normalize-space($restriction),' ')),' '))}" />
</xsl:template>

<!-- ==================================================== -->
<!--   Input elements included depending on field type    -->
<!-- ==================================================== -->

<xsl:template name="includes">
  <xsl:comment> Input elements included depending on field type </xsl:comment>
  
  <textfield width="40" id="input.text" />
  <textfield width="30" id="input.name" />
  <textfield width="20" id="input.identifier" />
  <textfield width="10" id="input.date" />
  <textfield width="8"  id="input.time" />
  <textfield width="18" id="input.timestamp" />
  <list type="checkbox" rows="1" default="" id="input.boolean">
    <item i18n="editor.search.choose" value="" />
    <item i18n="editor.search.true"   value="true" />
    <item i18n="editor.search.false"  value="false" />
  </list>
  <textfield width="10" id="input.decimal" />
  <textfield width="10" id="input.integer" />

  <xsl:if test="$layout='advanced'">
    <xsl:for-each select="$fieldtypes/mcr:type">
      <list type="dropdown" rows="1" default="{@default}" id="operators.{@name}">
        <xsl:for-each select="mcr:operator">
          <item value="{@token}">
            <xsl:choose>
              <xsl:when test="string-length(translate(@token,'&lt;&gt;=','')) = 0">
                <xsl:attribute name="label"><xsl:value-of select="@token" /></xsl:attribute>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="i18n">editor.search.<xsl:value-of select="@token" /></xsl:attribute>
              </xsl:otherwise>
            </xsl:choose>
          </item>
        </xsl:for-each>
      </list>
    </xsl:for-each>
  </xsl:if>
</xsl:template>

<!-- ==================================================== -->
<!--             Selector for hosts to query              -->
<!-- ==================================================== -->

<xsl:template name="hosts">
  <xsl:comment> Select hosts to query </xsl:comment>

  <cell row="95" col="1" anchor="SOUTHEAST" height="50px">
    <text i18n="editor.search.searchon"/>
  </cell>
  <cell row="95" col="2" anchor="SOUTHWEST" var="hosts/@target">
    <list type="radio" default="local">
      <item value="local" i18n="editor.search.searchthis"/>
      <item value="all" i18n="editor.search.searchall"/>
      <item value="selected" i18n="editor.search.searchboth"/>
    </list>
  </cell>
      
  <cell row="96" col="2" anchor="NORTHWEST" var="hosts/host">
    <list type="checkbox" cols="2">
      <include uri="xslStyle:hosts:webapp:hosts.xml" />
    </list>
  </cell>
</xsl:template>

<!-- ==================================================== -->
<!--      Choose AND or OR for combining conditions       -->
<!-- ==================================================== -->

<xsl:template name="choose.and.or">
  <cell row="93" col="1" anchor="EAST">
    <text i18n="editor.search.connect" />
  </cell>
  <cell row="93" col="2" anchor="WEST" var="conditions/boolean/boolean/@operator">
    <list type="dropdown" default="and">
      <item value="and" i18n="editor.search.and" />
      <item value="or" i18n="editor.search.or" />
    </list>
  </cell>
</xsl:template>

<!-- ==================================================== -->
<!--        Selector for sort criteria of results         -->
<!-- ==================================================== -->

<xsl:template name="sortBy">
  <xsl:comment> Select sort order of results </xsl:comment>
  <cell row="97" col="1" anchor="NORTHEAST">
    <text i18n="editor.search.sortby" />
  </cell>
  <cell row="97" col="2" anchor="NORTHWEST" var="sortBy/field">
    <repeater min="1" max="3">
      <panel>
        <cell row="1" col="1" anchor="WEST" var="@name">
          <list type="dropdown">
            <item value="" i18n="editor.search.choose" />
            <xsl:for-each select="mcr:index/mcr:field[@sortable='true' and contains(concat(' ',$sort.fields,' '),concat(' ',@name,' '))]">
              <item value="{@name}" i18n="{@i18n}" />
            </xsl:for-each>
          </list>
        </cell>
        <cell row="1" col="2" anchor="WEST" var="@order">
          <list type="dropdown" default="ascending">
            <item value="ascending" i18n="editor.search.ascending" />
            <item value="descending" i18n="editor.search.descending" />
          </list>
        </cell>
      </panel>
    </repeater>
  </cell>
</xsl:template>

<!-- ==================================================== -->
<!--      Selector for maxResults and numPerPage          -->
<!-- ==================================================== -->

<xsl:template name="maxResultsNumPerPage">
  <xsl:comment> Select maximum number of results and num per page </xsl:comment>

  <cell row="98" col="1" colspan="2" anchor="SOUTHEAST" height="50px">
    <panel>
      <cell row="1" col="1" anchor="WEST">
        <text  i18n="editor.search.max" />
      </cell>
      <cell row="1" col="2" anchor="WEST" var="@maxResults">
        <list type="dropdown" default="100">
          <item value="20"  label="20"  />
          <item value="100" label="100" />
          <item value="500" label="500" />            
          <item value="0"   i18n="editor.search.all" />
        </list>
      </cell>
      <cell row="1" col="3" anchor="WEST">
        <text i18n="editor.search.label" />
      </cell>
      <cell row="1" col="4" anchor="WEST" var="@numPerPage">
        <list type="dropdown" default="10">
          <item value="10" label="10" />
          <item value="20" label="20" />
          <item value="50" label="50" />
          <item value="0"  i18n="editor.search.all" />
        </list>
      </cell>
      <cell row="1" col="5" anchor="WEST">
        <text i18n="editor.search.perpage" />
      </cell>
    </panel>
  </cell>
</xsl:template>

<!-- ==================================================== -->
<!--                  Some vertical space                 -->
<!-- ==================================================== -->

<xsl:template name="spacer">
  <cell row="92" col="1" colspan="2" anchor="WEST">
    <space height="20px" />
  </cell>
</xsl:template>

<!-- ==================================================== -->
<!--      Input element for a single search field         -->
<!-- ==================================================== -->

<xsl:template name="build.from.list">
  <xsl:param name="fields" />
  <xsl:param name="pos" select="1" />
  
  <xsl:if test="string-length(normalize-space($fields)) &gt; 0">
    <xsl:choose>
      <xsl:when test="contains(normalize-space($fields),' ')">
        <xsl:for-each select="mcr:index/mcr:field[@name=substring-before(normalize-space($fields),' ')]">
          <xsl:call-template name="build.search">
            <xsl:with-param name="pos" select="$pos" />
          </xsl:call-template>
        </xsl:for-each>
        <xsl:call-template name="build.from.list">
          <xsl:with-param name="fields" select="substring-after(normalize-space($fields),' ')" />
          <xsl:with-param name="pos" select="$pos + 1" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="mcr:index/mcr:field[@name=normalize-space($fields)]">
          <xsl:call-template name="build.search">
            <xsl:with-param name="pos" select="$pos" />
          </xsl:call-template>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>  
 
<xsl:template name="build.search">
  <xsl:param name="pos" select="position()" />

  <xsl:comment> Search in field '<xsl:value-of select="@name" />
    <xsl:text>' with operator '</xsl:text>
    <xsl:value-of select="$fieldtypes/mcr:type[@name=current()/@type]/@default" />
    <xsl:text>' </xsl:text>
  </xsl:comment>

  <hidden var="conditions/boolean/condition{$pos}/@field" default="{@name}" />
  <hidden var="conditions/boolean/condition{$pos}/@operator" default="{$fieldtypes/mcr:type[@name=current()/@type]/@default}" />
  <cell row="{number($pos)*2}" col="1" anchor="EAST">
    <text i18n="{@i18n}" />
  </cell>
  <cell row="{number($pos)*2}" col="2" anchor="WEST" var="conditions/boolean/condition{$pos}/@value">
    <xsl:choose>
      <xsl:when test="@classification and @source='objectCategory'">
        <list type="dropdown">
          <item value="" i18n="editor.search.choose" />
          <include uri="classification:editor[TextCounter]:2:children:{@classification}" cacheable="false" />
        </list>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="ref">input.<xsl:value-of select="@type" /></xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
  </cell>
</xsl:template>

</xsl:stylesheet>
