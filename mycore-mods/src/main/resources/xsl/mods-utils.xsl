<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mods="http://www.loc.gov/mods/v3">
  <xsl:template mode="mods.type" match="mods:mods">
    <xsl:choose>
      <xsl:when
        test="substring-after(mods:genre[@type='intern']/@valueURI,'#')='article' or
              (mods:relatedItem/mods:genre='periodical' and mods:identifier/@type='doi')">
        <xsl:value-of select="'article'" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring-after(mods:genre[@type='intern']/@valueURI,'#')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode="mods.datePublished" match="mods:mods">
    <xsl:choose>
      <xsl:when test="mods:originInfo/mods:dateIssued">
        <xsl:value-of select="mods:originInfo/mods:dateIssued" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable name="created" select="ancestor::mycoreobject/service/servdates/servdate[@type='createdate']" />
        <xsl:if test="string-length($created)&gt;0">
          <xsl:value-of select="substring($created, 1,10)" />
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!--
   -    Prints mods title defined by 'type' [translated|alternative|abbreviated|uniform]. If no type is given
   -    returns main title (default). The parameter 'withSubtitle' [true|false] specifies, if the subtitle
   -    sould be shown.
   -->
  <xsl:template mode="mods.title" match="mods:mods">
    <xsl:param name="type" select="''" />
    <xsl:param name="withSubtitle" select="false()" />

    <xsl:variable name="mods-type">
      <xsl:apply-templates select="." mode="mods.type" />
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$mods-type='confpro'">
        <xsl:apply-templates select="." mode="mods.title.confpro" />
      </xsl:when>
      <xsl:when test="mods:titleInfo/mods:title">
        <xsl:choose>
          <xsl:when test="not($type='')">
            <xsl:apply-templates select="mods:titleInfo[@type=$type]" mode="mods.printTitle">
              <xsl:with-param name="withSubtitle" select="$withSubtitle" />
            </xsl:apply-templates>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="mods:titleInfo[not(@type='uniform' or @type='abbreviated' or @type='alternative' or @type='translated')]" mode="mods.printTitle">
              <xsl:with-param name="withSubtitle" select="$withSubtitle" />
            </xsl:apply-templates>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates mode="mods.internalId" select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode="mods.printTitle" match="mods:titleInfo">
    <xsl:param name="withSubtitle" select="false()" />
    <xsl:if test="mods:nonSort">
      <xsl:value-of select="concat(mods:nonSort, ' ')" />
    </xsl:if>
    <xsl:value-of select="mods:title" />
    <xsl:if test="$withSubtitle and mods:subTitle">
      <span class="subtitle">
        <xsl:text> : </xsl:text>
        <xsl:value-of select="mods:subTitle" />
      </span>
    </xsl:if>
  </xsl:template>

  <xsl:template mode="mods.title.confpro" match="mods:mods">
    <xsl:choose>
      <xsl:when test="mods:titleInfo/mods:title">
        <xsl:value-of select="mods:titleInfo/mods:title[1]" />
      </xsl:when>
      <xsl:when test="mods:name[@type='conference']">
        <xsl:variable name="completeTitle">
          <xsl:for-each select="mods:name[@type='conference']">
            <xsl:for-each select="mods:namePart[not(@type)]">
              <xsl:choose>
                <xsl:when test="position()=1">
                  <xsl:value-of select="." />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat(' – ',.)" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:for-each>
            <xsl:if test="mods:namePart[@type='date']">
              <xsl:value-of select="', '" />
              <xsl:value-of select="mods:namePart[@type='date']" />
            </xsl:if>
            <xsl:for-each select="mods:affiliation">
              <xsl:value-of select="concat(', ',.)" />
            </xsl:for-each>
          </xsl:for-each>
        </xsl:variable>
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.proceedingOf',$completeTitle)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates mode="mods.internalId" select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode="mods.subtitle" match="mods:mods">
    <xsl:param name="type" select="''" />
    <xsl:choose>
      <xsl:when test="not($type='')">
        <xsl:value-of select="mods:titleInfo[@type=$type]/mods:subTitle" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="mods:titleInfo[not(@type='uniform' or @type='abbreviated' or @type='alternative' or @type='translated')]/mods:subTitle" />
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template mode="mods.internalId" match="mods:mods">
    <xsl:choose>
      <xsl:when test="../../../../@ID">
        <xsl:value-of select="../../../../@ID" />
      </xsl:when>
      <xsl:when test="mods:recordInfo/mods:recordIdentifier">
        <xsl:value-of select="mods:recordInfo/mods:recordIdentifier" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="'unidentified MODS document'" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- mycoreobject templates -->
  <xsl:template mode="mods-type" match="mycoreobject">
    <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods" mode="mods.type" />
  </xsl:template>

  <!--Template for generated link names and result titles: see mycoreobject.xsl, results.xsl, MyCoReLayout.xsl -->
  <xsl:template priority="1" mode="resulttitle" match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="completeTitle">
      <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods" mode="mods.title" />
    </xsl:variable>
    <xsl:value-of select="mcrxml:shortenText($completeTitle,70)" />
  </xsl:template>

  <!--Template for access conditions -->
  <xsl:template match="mods:accessCondition" mode="cc-logo">
    <xsl:variable name="licenseVersion" select="'3.0'" />
    <!-- like cc_by-nc-sa: remove the 'cc_' -->
    <xsl:variable name="licenseString" select="substring-after(normalize-space(.),'cc_')" />
    <a rel="license" href="http://creativecommons.org/licenses/{$licenseString}/{$licenseVersion}/">
      <img src="//i.creativecommons.org/l/{$licenseString}/{$licenseVersion}/88x31.png" />
    </a>
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="cc-text">
    <!-- like cc_by-nc-sa: remove the 'cc_' -->
    <xsl:variable name="licenseString" select="substring-after(normalize-space(.),'cc_')" />
    <xsl:value-of select="i18n:translate(concat('component.mods.metaData.dictionary.cc.30.', $licenseString))" />
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="rights_reserved">
    <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.rightsReserved')" />
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="oa_nlz">
    <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.oa_nlz')" />
  </xsl:template>

  <xsl:template match="mods:name" mode="nameLink">
    <xsl:variable name="gnd">
      <xsl:if test="starts-with(@valueURI, 'http://d-nb.info/gnd/')">
        <xsl:value-of select="substring-after(@valueURI, 'http://d-nb.info/gnd/')" />
      </xsl:if>
    </xsl:variable>
    <!-- if user is in role editor or admin, show all; other users only gets their own and published publications -->
    <xsl:variable name="filter_query">
      <xsl:choose>
        <xsl:when test="mcrxml:isCurrentUserInRole('admin') or mcrxml:isCurrentUserInRole('editor')">state:*</xsl:when>
        <xsl:otherwise>state:published OR createdby:<xsl:value-of select="$CurrentUser" /></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="query">
      <xsl:choose>
        <xsl:when test="string-length($gnd)&gt;0">
          <xsl:value-of select="concat($ServletsBaseURL,'solr/select?q=mods.gnd:', $gnd, ' AND (', $filter_query, ')')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($ServletsBaseURL,'solr/select?q=')" />
          <xsl:value-of select="concat('+mods.author:&quot;',mods:displayForm,'&quot;')" />
          <xsl:value-of select="concat(' AND (', $filter_query, ')')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <a itemprop="creator" href="{$query}">
      <span itemscope="itemscope" itemtype="http://schema.org/Person">
        <span itemprop="name">
          <xsl:value-of select="mods:displayForm" />
        </span>
        <xsl:if test="string-length($gnd)&gt;0">
          <xsl:text>&#160;</xsl:text><!-- add whitespace here -->
          <a href="http://d-nb.info/gnd/{$gnd}" title="Link zur GND">
            <sup>GND</sup>
          </a>
        </xsl:if>
      </span>
    </a>
  </xsl:template>

</xsl:stylesheet>