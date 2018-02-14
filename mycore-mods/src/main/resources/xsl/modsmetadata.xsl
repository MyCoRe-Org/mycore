<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrmods="xalan://org.mycore.mods.classification.MCRMODSClassificationSupport"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mcr="http://www.mycore.org/"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  exclude-result-prefixes="xalan xlink mcr mcrxsl i18n acl mods mcrmods rdf"
  version="1.0">
  <xsl:param name="MCR.Handle.Resolver.MasterURL" />
  <xsl:param name="MCR.DOI.Resolver.MasterURL" />
  <xsl:param name="MCR.Mods.SherpaRomeo.ApiKey" select="''" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="wcms.useTargets" select="'no'" /><!-- TODO: refacture! -->

  <xsl:key use="mods:role/mods:roleTerm" name="name-by-role" match="mods:mods/mods:name" />

  <xsl:template name="printMetaDate.mods">
    <!-- prints a table row for a given nodeset -->
    <xsl:param name="nodes" />
    <xsl:param name="label" select="i18n:translate(concat('component.mods.metaData.dictionary.',local-name($nodes[1])))" />
    <xsl:param name="sep" select="''" />
    <xsl:param name="property" select="''" />
    <xsl:if test="$nodes">
      <tr>
        <td valign="top" class="metaname">
          <xsl:value-of select="concat($label,':')" />
        </td>
        <td class="metavalue">
          <xsl:if test="$property != ''">
            <xsl:attribute name="property">
              <xsl:value-of select="$property" />
            </xsl:attribute>
          </xsl:if>
          <xsl:variable name="selectPresentLang">
            <xsl:call-template name="selectPresentLang">
              <xsl:with-param name="nodes" select="$nodes" />
            </xsl:call-template>
          </xsl:variable>
          <xsl:for-each select="$nodes">
            <xsl:if test="position()!=1">
              <xsl:choose>
                <xsl:when test="string-length($sep)&gt;0">
                  <xsl:value-of select="$sep" />
                </xsl:when>
                <xsl:otherwise>
                  <br />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:if>
            <xsl:if test="not(@xml:lang) or @xml:lang=$selectPresentLang">
              <xsl:call-template name="lf2br">
                <xsl:with-param name="string" select="normalize-space(.)" />
              </xsl:call-template>
              <xsl:if test="@authority='gnd' and @valueURI">
                <xsl:apply-templates select="." mode="gnd"/>
              </xsl:if>
            </xsl:if>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:topic[@authority='gnd']" mode="gnd">
    <a href="{@valueURI}" title="Link zu GND"><sup>GND</sup></a>
  </xsl:template>

  <xsl:template match="mods:geographic[@authority='gnd']" mode="gnd">
    <a href="{@valueURI}" title="Link zu GND"><sup>GND</sup></a>
  </xsl:template>

  <xsl:template match="mods:dateCreated|mods:dateOther|mods:dateIssued|mods:dateCaptured|mods:dateModified" mode="present">
    <xsl:param name="label">
      <xsl:choose>
        <xsl:when test="(@point='start' or @point='end') and i18n:exists(concat('component.mods.metaData.dictionary.',local-name(), '.range'))">
          <xsl:value-of select="i18n:translate(concat('component.mods.metaData.dictionary.',local-name(), '.range'))"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="i18n:translate(concat('component.mods.metaData.dictionary.',local-name()))"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:param>
    <xsl:if test="not(@point='end' and (preceding-sibling::*[name(current())=name()][@point='start']  or following-sibling::*[name(current())=name()][@point='start']))">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat($label,':')" />
      </td>
      <td class="metavalue">
        <xsl:if test="local-name()='dateIssued'">
          <meta property="datePublished">
            <xsl:attribute name="content">
                    <xsl:value-of select="." />
                  </xsl:attribute>
          </meta>
        </xsl:if>
        <xsl:apply-templates select="." mode="rangeDate"/>
      </td>
    </tr>
  </xsl:if>
  </xsl:template>

  <xsl:template match="mods:dateCreated|mods:dateIssued|mods:dateCaptured|mods:dateModified" mode="rangeDate">
  <xsl:choose>
    <xsl:when
    test="@point='start' and (following-sibling::*[name(current())=name()][@point='end'] or preceding-sibling::*[name(current())=name()][@point='end'])">
      <xsl:apply-templates select="." mode="formatDate" />
      <xsl:value-of select="' - '" />
      <xsl:apply-templates select="preceding-sibling::*[name(current())=name()][@point='end']" mode="formatDate" />
      <xsl:apply-templates select="following-sibling::*[name(current())=name()][@point='end']" mode="formatDate" />
    </xsl:when>
    <xsl:when
    test="@point='start' and not(following-sibling::*[name(current())=name()][@point='end'] or preceding-sibling::*[name(current())=name()][@point='end'])">
      <xsl:apply-templates select="." mode="formatDate" />
      <xsl:value-of select="' - '" />
    </xsl:when>
    <xsl:when
    test="@point='end' and not(following-sibling::*[name(current())=name()][@point='start'] or preceding-sibling::*[name(current())=name()][@point='start'])">
      <xsl:value-of select="' - '" />
      <xsl:apply-templates select="." mode="formatDate" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="." mode="formatDate" />
    </xsl:otherwise>
  </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:dateOther" mode="rangeDate">
    <xsl:choose>
    <xsl:when
    test="@point='start' and (following-sibling::*[name(current())=name()][@point='end'][@type=current()/@type] or preceding-sibling::*[name(current())=name()][@point='end'][@type=current()/@type])">
      <xsl:apply-templates select="." mode="formatDate" />
      <xsl:value-of select="' - '" />
      <xsl:apply-templates select="preceding-sibling::*[name(current())=name()][@point='end'][@type=current()/@type]" mode="formatDate" />
      <xsl:apply-templates select="following-sibling::*[name(current())=name()][@point='end'][@type=current()/@type]" mode="formatDate" />
    </xsl:when>
    <xsl:when
    test="@point='start' and not(following-sibling::*[name(current())=name()][@point='end'][@type=current()/@type] or preceding-sibling::*[name(current())=name()][@point='end'][@type=current()/@type])">
      <xsl:apply-templates select="." mode="formatDate" />
      <xsl:value-of select="' - '" />
    </xsl:when>
    <xsl:when
    test="@point='end' and not(following-sibling::*[name(current())=name()][@point='start'][@type=current()/@type] or preceding-sibling::*[name(current())=name()][@point='start'][@type=current()/@type])">
      <xsl:value-of select="' - '" />
      <xsl:apply-templates select="." mode="formatDate" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="." mode="formatDate" />
    </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:dateCreated|mods:dateOther|mods:dateIssued|mods:dateCaptured|mods:dateModified" mode="formatDate">
    <xsl:variable name="dateFormat">
      <xsl:choose>
        <xsl:when test="string-length(normalize-space(.))=4">
          <xsl:value-of select="'yyyy'" />
        </xsl:when>
        <xsl:when test="string-length(normalize-space(.))=7">
          <xsl:value-of select="'MM.yyyy'" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'dd.MM.yyyy'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="formatted">
      <xsl:call-template name="formatISODate">
        <xsl:with-param name="date" select="." />
        <xsl:with-param name="format" select="$dateFormat" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when
        test="string-length($formatted)&gt;2
                      and starts-with($formatted, '?')
                      and substring($formatted,string-length($formatted),1)='?'">
        <xsl:value-of select="translate($formatted, '?', '')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$formatted" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="category" mode="printModsClassInfo">
    <xsl:variable name="categurl">
      <xsl:if test="url">
        <xsl:choose>
            <!-- MCRObjectID should not contain a ':' so it must be an external link then -->
          <xsl:when test="contains(url/@xlink:href,':')">
            <xsl:value-of select="url/@xlink:href" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat($WebApplicationBaseURL,'receive/',url/@xlink:href,$HttpSession)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:if>
    </xsl:variable>

    <xsl:variable name="selectLang">
      <xsl:call-template name="selectLang">
        <xsl:with-param name="nodes" select="./label" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:for-each select="./label[lang($selectLang)]">
      <xsl:choose>
        <xsl:when test="string-length($categurl) != 0">
          <a href="{$categurl}">
            <xsl:if test="$wcms.useTargets = 'yes'">
              <xsl:attribute name="target">_blank</xsl:attribute>
            </xsl:if>
            <xsl:value-of select="@text" />
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@text" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="*" mode="printModsClassInfo">
    <xsl:variable name="classlink" select="mcrmods:getClassCategLink(.)" />
    <xsl:choose>
      <xsl:when test="string-length($classlink) &gt; 0">
        <xsl:for-each select="document($classlink)/mycoreclass/categories/category">
          <xsl:apply-templates select="." mode="printModsClassInfo" />
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="@valueURI">
            <xsl:apply-templates select="." mode="hrefLink" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="text()" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*[@valueURI]" mode="hrefLink">
    <a href="{@valueURI}">
      <xsl:choose>
        <xsl:when test="mods:displayForm">
          <xsl:value-of select="mods:displayForm" />
        </xsl:when>
        <xsl:when test="@displayLabel">
          <xsl:value-of select="@displayLabel" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@valueURI" />
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template match="mods:titleInfo" mode="present">
    <xsl:if test="not(@transliteration='text/html')">
      <xsl:for-each select="mods:title">
        <tr>
          <td valign="top" class="metaname">
            <xsl:choose>
              <xsl:when test="./../@type='translated'">
                <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.title'),' (',./../@xml:lang,'):')" />
              </xsl:when>
              <xsl:when test="./../@type='alternative' and ./../@displayLabel='Short form of the title'">
                <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.shorttitle'),':')" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.title'),':')" />
              </xsl:otherwise>
            </xsl:choose>
          </td>
          <td class="metavalue" property="name">
            <span>
              <xsl:attribute name="property">
                <xsl:choose>
                  <xsl:when test="./../@type='translated' or ./../@type='alternative'">
                    <xsl:text>alternativeHeadline</xsl:text>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:text>headline</xsl:text>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:attribute>
              <xsl:choose>
                <xsl:when test="not(./../@type='translated' or ./../@type='alternative') and //mods:titleInfo[@transliteration='text/html']">
                  <xsl:value-of select="//mods:titleInfo[@transliteration='text/html']/mods:title" disable-output-escaping="yes" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:call-template name="lf2br">
                    <xsl:with-param name="string" select="." />
                  </xsl:call-template>
                </xsl:otherwise>
              </xsl:choose>
            </span>
          </td>
        </tr>
      </xsl:for-each>
      <xsl:if test="mods:subTitle">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.subtitle'),':')" />
          </td>
          <td class="metavalue subTitle">
            <span property="alternativeHeadline">
              <xsl:value-of select="mods:subTitle" />
            </span>
          </td>
        </tr>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.titleContent">
    <xsl:variable name="modsType">
      <xsl:choose>
        <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
          <xsl:value-of select="substring-after(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']/@valueURI,'#')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates mode="mods-type" select="." />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <div id="title_content" class="block_content">
      <div class="subcolumns">
        <div class="c85l">
          <table class="metaData">
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />

            <!-- mods:name grouped by mods:role/mods:roleTerm -->
            <xsl:for-each
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@ID) and count(. | key('name-by-role',mods:role/mods:roleTerm)[1])=1]">
              <!-- for every role -->
              <tr>
                <td valign="top" class="metaname">
                  <xsl:choose>
                    <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']">
                      <xsl:apply-templates select="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']"
                        mode="printModsClassInfo" />
                      <xsl:value-of select="':'" />
                    </xsl:when>
                    <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator']">
                      <xsl:value-of
                        select="concat(i18n:translate(concat('component.mods.metaData.dictionary.',mods:role/mods:roleTerm[@authority='marcrelator'])),':')" />
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.name'),':')" />
                    </xsl:otherwise>
                  </xsl:choose>
                </td>
                <td class="metavalue">
                  <xsl:for-each select="key('name-by-role',mods:role/mods:roleTerm)">
                    <xsl:if test="position()!=1">
                      <xsl:value-of select="'; '" />
                    </xsl:if>
                    <xsl:apply-templates select="." mode="printName" />
                  </xsl:for-each>
                </td>
              </tr>
            </xsl:for-each>
            <xsl:if test="./structure/children/child">
              <xsl:apply-templates mode="printChildren" select="./structure/children">
                <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.contains')" />
              </xsl:apply-templates>
            </xsl:if>
          </table>
        </div>
        <div class="c15r">
          <xsl:if test="./structure/derobjects">
            <xsl:variable name="objectBaseURL">
              <xsl:if test="$objectHost != 'local'">
                <xsl:value-of select="document('webapp:hosts.xml')/mcr:hosts/mcr:host[@alias=$objectHost]/mcr:url[@type='object']/@href" />
              </xsl:if>
              <xsl:if test="$objectHost = 'local'">
                <xsl:value-of select="concat($WebApplicationBaseURL,'receive/')" />
              </xsl:if>
            </xsl:variable>
            <xsl:variable name="staticURL">
              <xsl:value-of select="concat($objectBaseURL,@ID)" />
            </xsl:variable>
            <xsl:apply-templates mode="printDerivatesThumb" select=".">
              <xsl:with-param select="$staticURL" name="staticURL" />
              <xsl:with-param select="$modsType" name="modsType" />
            </xsl:apply-templates>
          </xsl:if>
        </div>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="mods:abstract" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.abstract'),' (' ,@xml:lang,') :')" />
      </td>
      <td class="metavalue">
        <xsl:call-template name="lf2br">
          <xsl:with-param name="string" select="." />
        </xsl:call-template>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.abstractContent">
    <div id="abstract_box" class="detailbox">
      <h4 id="abstract_switch" class="block_switch">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.abstractbox')" />
      </h4>
      <div id="abstract_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="mods:extent" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.extent'),':')" />
      </td>
      <td class="metavalue">
        <xsl:call-template name="printMetaDate.mods.extent" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.extent">
    <xsl:choose>
      <xsl:when test="count(mods:start) &gt; 0">
        <xsl:choose>
          <xsl:when test="count(mods:end) &gt; 0">
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.page.abbr'),' ',mods:start,'-',mods:end)" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.page.abbr'),' ',mods:start)" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="mods:total">
        <xsl:value-of select="concat(mods:total,' ',i18n:translate('component.mods.metaData.dictionary.pages'))" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:extension[@displayLabel='characteristics']" mode="present">
    <xsl:if test="not(mcrxsl:isCurrentUserGuestUser())">
      <tr>
        <td valign="top" class="metaname">
          <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.characteristics'),':')" />
        </td>
        <td class="metavalue">
          <table class="table table-condensed">
            <tr>
              <th>
                <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.year')" />
              </th>
              <th>
                <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.impact')" />
              </th>
              <th>
                <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.refereed')" />
              </th>
            </tr>
            <xsl:for-each select="chars">
              <tr>
                <td>
                  <xsl:value-of select="@year" />
                </td>
                <td>
                  <xsl:value-of select="@factor" />
                </td>
                <td>
                  <xsl:if test="@refereed">
                    <xsl:value-of select="i18n:translate(concat('component.mods.metaData.dictionary.refereed.',@refereed))" />
                  </xsl:if>
                </td>
              </tr>
            </xsl:for-each>
          </table>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:extension" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate(concat('component.mods.metaData.dictionary.',@displayLabel)),':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:name" mode="printName">
    <xsl:variable name="personName">
      <xsl:choose>
        <xsl:when test="@valueURI">
          <!-- derived from printModsClassInfo template -->
          <xsl:variable name="classlink" select="mcrmods:getClassCategParentLink(.)" />
          <xsl:choose>
            <xsl:when test="string-length($classlink) &gt; 0">
              <xsl:for-each select="document($classlink)/mycoreclass//category[position()=1 or position()=last()]">
                <xsl:if test="position() > 1">
                  <xsl:value-of select="', '" />
                </xsl:if>
                <xsl:apply-templates select="." mode="printModsClassInfo" />
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="." mode="hrefLink" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="mods:namePart">
          <xsl:choose>
            <xsl:when test="mods:namePart[@type='given'] and mods:namePart[@type='family']">
              <xsl:value-of select="concat(mods:namePart[@type='family'], ', ',mods:namePart[@type='given'])" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="mods:namePart" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="mods:displayForm">
          <xsl:value-of select="mods:displayForm" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="." />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
     <xsl:variable name="nameIdentifier">
      <xsl:if test="mods:nameIdentifier/@type">
        <xsl:value-of select="mods:nameIdentifier" />
      </xsl:if>
    </xsl:variable>
  <xsl:variable name="nameIdentifierType">
  <xsl:if test="string-length($nameIdentifier)&gt;0"></xsl:if>
    <xsl:value-of select="mods:nameIdentifier/@type" />
  </xsl:variable>

    <xsl:choose>
      <xsl:when test="mods:role/mods:roleTerm='aut'">
        <xsl:variable name="propType">
          <xsl:choose>
            <xsl:when test="@type='corporate'"><xsl:text>Organisation</xsl:text></xsl:when>
            <xsl:otherwise><xsl:text>Person</xsl:text></xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <span property="author" typeof="{$propType}">
          <xsl:value-of select="$personName" />
          <meta property="name" content="{$personName}"/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$personName" />
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="string-length($nameIdentifier)&gt;0">
      <xsl:variable name="classi" select="document(concat('classification:metadata:all:children:','nameIdentifier',':',$nameIdentifierType))/mycoreclass/categories/category[@ID=$nameIdentifierType]" />
    <xsl:variable name="uri" select="$classi/label[@xml:lang='x-uri']/@text" />
    <xsl:variable name="idType" select="$classi/label[@xml:lang='de']/@text" />
      <xsl:text>&#160;</xsl:text><!-- add whitespace here -->
      <a href="{$uri}{$nameIdentifier}" title="Link zu {$idType}">
        <sup>
          <xsl:value-of select="$idType" />
        </sup>
      </a>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:name" mode="present"><!-- ToDo: all authors, rev ... in one column -->
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']">
            <xsl:apply-templates select="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']" mode="printModsClassInfo" />
            <xsl:value-of select="':'" />
          </xsl:when>
          <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator']">
            <xsl:value-of
              select="concat(i18n:translate(concat('component.mods.metaData.dictionary.',mods:role/mods:roleTerm[@authority='marcrelator'])),':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.name'),':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:apply-templates select="." mode="printName" />
      </td>
    </tr>
  </xsl:template>

  <!-- TODO: OpenAgrar specific solution -->
  <xsl:template match="mods:name[@type='corporate' and @ID]" mode="present">
    <xsl:variable name="id" select="concat('#', @ID)" />
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.institution.label'),':')" />
      </td>
      <td class="metavalue">
        <xsl:apply-templates select="." mode="printName" />
      </td>
    </tr>
    <xsl:if
      test="(not(mcrxsl:isCurrentUserGuestUser()) and ./../mods:note[@xlink:href=$id]) or (./../mods:location/mods:physicalLocation[@xlink:href=$id])">
      <tr>
        <td colspan="2">
          <table class="metaData">
            <xsl:if test="not(mcrxsl:isCurrentUserGuestUser())">
              <xsl:call-template name="printMetaDate.mods">
                <xsl:with-param name="nodes" select="./../mods:note[@xlink:href=$id]" />
              </xsl:call-template>
            </xsl:if>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./../mods:location/mods:physicalLocation[@xlink:href=$id]" />
            </xsl:call-template>
          </table>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:name[@type='corporate' and not(@ID)]" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="mods:role/mods:roleTerm[@type='code' and @authority='marcrelator']">
            <xsl:value-of select="concat(i18n:translate(concat('component.mods.metaData.dictionary.institution.', mods:role/mods:roleTerm[@type='code' and @authority='marcrelator'], '.label')),':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.institution.label'),':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:apply-templates select="." mode="printName" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='hdl']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.identifier.hdl'),':')" />
      </td>
      <td class="metavalue">
        <xsl:variable name="hdl" select="." />
        <a href="{$MCR.Handle.Resolver.MasterURL}{$hdl}">
          <xsl:value-of select="$hdl" />
        </a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:identifier" mode="present">
     <xsl:variable name="identifier" select="document('classification:metadata:-1:children:identifier')" />
    <xsl:variable name="type" select="./@type" />
    <tr>
      <td valign="top" class="metaname">
      <xsl:choose>
        <xsl:when test="not($identifier//category[@ID=$type])">
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.identifier.other', $type), ':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat($identifier//category[@ID=$type]/label[lang($CurrentLang)]/@text, ':')" />
          </xsl:otherwise>
      </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='intern_old']" mode="present">
    <xsl:if test="not(mcrxsl:isCurrentUserGuestUser())">
      <tr>
        <td valign="top" class="metaname">
          <xsl:value-of select="concat(i18n:translate(concat('component.mods.metaData.dictionary.identifier.',@type)),':')" />
        </td>
        <td class="metavalue">
          <xsl:value-of select="." />
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:identifier[@type='uri' or @type='doi' or @type='urn']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="contains(.,'ppn') or contains(.,'PPN')">
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.identifier.ppn'),':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate(concat('component.mods.metaData.dictionary.identifier.',@type)),':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:variable name="link" select="." />
        <xsl:choose>
          <xsl:when test="contains($link,'ppn') or contains($link,'PPN')">
            <a class="ppn" href="{$link}">
              <xsl:choose>
                <xsl:when test="contains($link, 'PPN=')">
                  <xsl:value-of select="substring-after($link, 'PPN=')" />
                </xsl:when>
                <xsl:when test="contains($link, ':ppn:')">
                  <xsl:value-of select="substring-after($link, ':ppn:')"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$link"/>
                </xsl:otherwise>
              </xsl:choose>
            </a>
          </xsl:when>
          <xsl:when test="@type='doi' and not(contains($link,'http'))">
            <a href="{$MCR.DOI.Resolver.MasterURL}{$link}">
              <xsl:value-of select="$link" />
            </a>
          </xsl:when>
          <xsl:when test="@type='urn' and not(contains($link,'http'))">
            <a href="http://nbn-resolving.de/{$link}">
              <xsl:value-of select="$link" />
            </a>
          </xsl:when>
          <xsl:otherwise>
            <a href="{$link}">
              <xsl:value-of select="$link" />
            </a>
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:classification" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="@authority='sdnb'">
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.sdnb'), ':')" />
          </xsl:when>
          <xsl:when test="@displayLabel='status'">
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.status'), ':')" />
          </xsl:when>
          <xsl:when test="not(contains(i18n:translate(concat('component.mods.metaData.dictionary.', @displayLabel)), 'component.mods.metaData.dictionary.'))">
            <xsl:value-of select="concat(i18n:translate(concat('component.mods.metaData.dictionary.', @displayLabel)), ':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.classification'), ':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:apply-templates select="." mode="printModsClassInfo" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:language" mode="present">
    <xsl:param name="sep" select="''" />
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.language'), ':')" />
      </td>
      <td class="metavalue">
        <xsl:for-each select="mods:languageTerm[@authority='rfc4646']">
          <xsl:if test="position()!=1">
            <xsl:choose>
              <xsl:when test="string-length($sep)&gt;0">
                <xsl:value-of select="$sep" />
              </xsl:when>
              <xsl:otherwise>
                <br />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
          <xsl:apply-templates select="." mode="printModsClassInfo" />
          <meta property="inLanguage">
            <xsl:attribute name="content">
              <xsl:value-of select="." />
            </xsl:attribute>
          </meta>
        </xsl:for-each>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:url" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="@access">
            <xsl:value-of select="concat(i18n:translate(concat('component.mods.metaData.dictionary.url.',mcrxsl:regexp(@access,' ','_'))),':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.url'),':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <a>
          <xsl:attribute name="href"><xsl:value-of select="." /></xsl:attribute>
          <xsl:choose>
            <xsl:when test="string-length(@displayLabel)&gt;0">
              <xsl:value-of select="@displayLabel" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="." />
            </xsl:otherwise>
          </xsl:choose>

        </a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="present"><!-- ToDo: show cc icon and more information ... -->
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="@type">
            <xsl:value-of
              select="concat(i18n:translate(concat('component.mods.metaData.dictionary.accessCondition.',mcrxsl:regexp(@type,' ','_'))),':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.accessCondition'),':')" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
      <td class="metavalue">
        <xsl:choose>
          <xsl:when test="@type='use and reproduction'">
            <xsl:variable name="trimmed" select="normalize-space(.)" />
            <xsl:choose>
              <xsl:when test="contains($trimmed, 'cc_by')">
                <xsl:apply-templates select="." mode="cc-logo" />
              </xsl:when>
              <xsl:when test="contains($trimmed, 'rights_reserved')">
                <xsl:apply-templates select="." mode="rights_reserved" />
              </xsl:when>
              <xsl:when test="contains($trimmed, 'oa_nlz')">
                <xsl:apply-templates select="." mode="oa_nlz" />
              </xsl:when>
              <xsl:when test="contains($trimmed, 'oa')">
                <xsl:apply-templates select="." mode="oa-logo" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="." />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="." />
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.relatedItem">
    <xsl:param name="parentID" />
    <xsl:param name="label" />

    <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@xlink:href=$parentID]">
    <xsl:call-template name="printMetaDate.mods.relatedItems">
      <xsl:with-param name="parentID" select="$parentID"/>
      <xsl:with-param name="label" select="$label"></xsl:with-param>
    </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.relatedItems">
    <xsl:param name="parentID" />
    <xsl:param name="label" />
  <tr>
    <td valign="top" class="metaname">
      <xsl:value-of select="concat($label,':')" />
    </td>
    <td class="metavalue">
      <!-- Parent/Host -->
      <xsl:choose>
        <xsl:when test="string-length($parentID)!=0">
          <xsl:call-template name="objectLink">
            <xsl:with-param select="$parentID" name="obj_id" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="mods:titleInfo/mods:title" />
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text disable-output-escaping="yes">&lt;br /></xsl:text>
      <xsl:variable name="dateIssued">
        <xsl:choose>
          <xsl:when test="../../mods:originInfo[@eventType='publication']/mods:dateIssued">
            <xsl:apply-templates select="../../mods:originInfo[@eventType='publication']/mods:dateIssued" mode="formatDate"/>
          </xsl:when>
          <xsl:when test="../mods:originInfo[@eventType='publication']/mods:dateIssued">
            <xsl:apply-templates select="../mods:originInfo[@eventType='publication']/mods:dateIssued" mode="formatDate"/>
          </xsl:when>
          <xsl:when test="mods:originInfo[@eventType='publication']/mods:dateIssued">
            <xsl:apply-templates select="mods:originInfo[@eventType='publication']/mods:dateIssued" mode="formatDate"/>
          </xsl:when>
          <xsl:when test="mods:part/mods:date">
            <xsl:apply-templates select="mods:part/mods:date" mode="formatDate"/>
          </xsl:when>
        </xsl:choose>
      </xsl:variable>
      <!-- Volume -->
      <xsl:if test="mods:part/mods:detail[@type='volume']/mods:number">
        <xsl:value-of
          select="concat('Vol. ',mods:part/mods:detail[@type='volume']/mods:number)" />
        <xsl:if test="mods:part/mods:detail[@type='issue']/mods:number">
          <xsl:text>, </xsl:text>
        </xsl:if>
      </xsl:if>
      <!-- Issue -->
      <xsl:if test="mods:part/mods:detail[@type='issue']/mods:number">
        <xsl:value-of
          select="concat('H. ',mods:part/mods:detail[@type='issue']/mods:number)" />
      </xsl:if>
      <xsl:if test="mods:part/mods:detail[@type='issue']/mods:number and string-length($dateIssued) &gt; 0">
        <xsl:text> </xsl:text>
      </xsl:if>
      <xsl:if test="string-length($dateIssued) &gt; 0">
        <xsl:text>(</xsl:text>
        <xsl:value-of select="$dateIssued" />
        <xsl:text>)</xsl:text>
      </xsl:if>
      <!-- Pages -->
      <xsl:if test="mods:part/mods:extent[@unit='pages']">
        <xsl:text>, </xsl:text>
        <xsl:for-each select="mods:part/mods:extent[@unit='pages']">
          <xsl:call-template name="printMetaDate.mods.extent" />
        </xsl:for-each>
      </xsl:if>
    </td>
  </tr>
  </xsl:template>

  <xsl:template match="children" mode="printChildren">
    <xsl:param name="label" select="i18n:translate('component.mods.metaData.dictionary.contains')" />
    <!--*** List children per object type ************************************* -->
    <!-- 1.) get a list of objectTypes of all child elements 2.) remove duplicates from this list 3.) for-each objectTyp id list child elements -->
    <xsl:variable name="objectTypes">
      <xsl:for-each select="child/@xlink:href">
        <id>
          <xsl:copy-of select="substring-before(substring-after(.,'_'),'_')" />
        </id>
      </xsl:for-each>
    </xsl:variable>
    <xsl:variable select="xalan:nodeset($objectTypes)/id[not(.=following::id)]" name="unique-ids" />
    <!-- the for-each would iterate over <id> with root not beeing /mycoreobject so we save the current node in variable context to access
      needed nodes -->
    <xsl:variable select="/mycoreobject" name="context" />
    <xsl:for-each select="$unique-ids">
      <xsl:variable select="." name="thisObjectType" />
      <xsl:variable name="children" select="$context/structure/children/child[contains(@xlink:href, concat('_',$thisObjectType,'_'))]" />
      <xsl:variable name="maxElements" select="20" />
      <xsl:variable name="positionMin">
        <xsl:choose>
          <xsl:when test="count($children) &gt; $maxElements">
            <xsl:value-of select="count($children) - $maxElements + 1" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="0" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:choose>
        <xsl:when test="$positionMin != 0">
          <!-- display recent $maxElements only -->
          <tr>
            <td valign="top" class="metaname">
              <xsl:value-of select="concat($label,':')" />
            </td>
            <td class="metavalue">
              <p>
                <xsl:choose>
                  <xsl:when test="not(mcrxsl:isCurrentUserGuestUser())">
                    <a href="{$ServletsBaseURL}solr/parent?q={$context/@ID}&amp;fq=">
                      <xsl:value-of select="i18n:translate('component.mods.metaData.displayAll')" />
                    </a>
                  </xsl:when>
                  <xsl:otherwise>
                    <a href="{$ServletsBaseURL}solr/parent?q={$context/@ID}">
                      <xsl:value-of select="i18n:translate('component.mods.metaData.displayAll')" />
                    </a>
                  </xsl:otherwise>
                </xsl:choose>
              </p>
              <xsl:for-each select="$children[position() &gt;= $positionMin]">
                <xsl:call-template name="objectLink">
                  <xsl:with-param name="obj_id" select="@xlink:href" />
                </xsl:call-template>
                <xsl:if test="position()!=last()">
                  <br />
                </xsl:if>
              </xsl:for-each>
            </td>
          </tr>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="printMetaDate">
            <xsl:with-param select="$children" name="nodes" />
            <xsl:with-param select="$label" name="label" />
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.permalink">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.permalink'),':')" />
      </td>
      <td class="metavalue">
        <a href="{$WebApplicationBaseURL}receive/{@ID}">
          <xsl:value-of select="concat($WebApplicationBaseURL,'receive/',@ID)" />
        </a>
<!--         <xsl:text> | </xsl:text>
        <xsl:call-template name="shareButton">
          <xsl:with-param name="linkURL" select="concat($ServletsBaseURL,'receive/',@ID)" />
          <xsl:with-param name="linkTitle" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1]" />
        </xsl:call-template> -->
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="printMetaDate.mods.categoryContent">
    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateCreated) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='submitted']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther[@type='accepted']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch open">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
              <tr>
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.genre.kindof'),':')" />
                </td>
                <td class="metavalue">
                  <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
                    mode="printModsClassInfo" />
                </td>
              </tr>
            </xsl:if>
            <xsl:if test="./structure/parents/parent/@xlink:href">
              <xsl:call-template name="printMetaDate.mods.relatedItem">
                <xsl:with-param name="parentID" select="./structure/parents/parent/@xlink:href" />
                <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.confpubIn')" />
              </xsl:call-template>
            </xsl:if>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher" />
            </xsl:call-template>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateCreated"
              mode="present" />
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateOther[@type='submitted']"
              mode="present">
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.dateSubmitted')" />
            </xsl:apply-templates>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateOther[@type='accepted']"
              mode="present">
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.dateAccepted')" />
            </xsl:apply-templates>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued"
              mode="present" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
              <xsl:with-param name="property" select="'keyword'" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>

  <!-- view default metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.modsDefaultType">
    <xsl:param name="mods-type" select="'report'" />
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate(concat('component.mods.metaData.dictionary.', $mods-type))" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:call-template name="printMetaDate.mods.categoryContent" />
  </xsl:template>
  <!-- END: view default metadata -->

  <!-- view report metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.report">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.report')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:call-template name="printMetaDate.mods.categoryContent" />
  </xsl:template>
  <!-- END: view report metadata -->

  <!-- view thesis metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.thesis">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.thesis'), ' - ')" />
        <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
          mode="printModsClassInfo" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:call-template name="printMetaDate.mods.categoryContent" />
  </xsl:template>
  <!-- END: view thesis metadata -->

  <!-- view conference proceeding metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.confpro">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.confpro')" />
      </h4>
      <div id="title_content" class="block_content">
        <div class="subcolumns">
          <div class="c85l">
            <table class="metaData">
              <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
                <tr>
                  <td valign="top" class="metaname">
                    <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.conference.title'),':')" />
                  </td>
                  <td class="metavalue">
                    <strong>
                      <xsl:for-each select="mods:namePart[not(@type)]">
                        <xsl:choose>
                          <xsl:when test="position()=1">
                            <xsl:value-of select="." />
                          </xsl:when>
                          <xsl:otherwise>
                            <em>
                              <xsl:value-of select="concat(' – ',.)" />
                            </em>
                          </xsl:otherwise>
                        </xsl:choose>
                      </xsl:for-each>
                    </strong>
                    <xsl:if test="mods:namePart[@type='date']">
                      <em>
                        <xsl:value-of select="', '" />
                        <xsl:value-of select="mods:namePart[@type='date']" />
                      </em>
                    </xsl:if>
                    <xsl:for-each select="mods:affiliation">
                      <xsl:value-of select="concat(', ',.)" />
                    </xsl:for-each>
                  </td>
                </tr>
              </xsl:for-each>
              <xsl:if test="not(./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo)">
                <tr>
                  <td valign="top" class="metaname">
                    <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.title'),':')" />
                  </td>
                  <td class="metavalue">
                    <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.conference.notReleased')" />
                  </td>
                </tr>
              </xsl:if>
              <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
              <xsl:apply-templates mode="present"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@ID) and not(@type='conference')]" />
              <xsl:if test="./structure/children/child">
                <xsl:apply-templates mode="printChildren" select="./structure/children">
                  <xsl:with-param name="label" select="'Konferenzbeiträge'" />
                </xsl:apply-templates>
              </xsl:if>
            </table>
          </div>
          <div class="c15r">
            <xsl:if test="./structure/derobjects">
              <xsl:variable name="objectBaseURL">
                <xsl:if test="$objectHost != 'local'">
                  <xsl:value-of select="document('webapp:hosts.xml')/mcr:hosts/mcr:host[@alias=$objectHost]/mcr:url[@type='object']/@href" />
                </xsl:if>
                <xsl:if test="$objectHost = 'local'">
                  <xsl:value-of select="concat($WebApplicationBaseURL,'receive/')" />
                </xsl:if>
              </xsl:variable>
              <xsl:variable name="staticURL">
                <xsl:value-of select="concat($objectBaseURL,@ID)" />
              </xsl:variable>
              <xsl:apply-templates mode="printDerivatesThumb" select=".">
                <xsl:with-param select="$staticURL" name="staticURL" />
                <xsl:with-param select="'confpro'" name="modsType" />
              </xsl:apply-templates>
            </xsl:if>
          </div>
        </div>
      </div>
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:edition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@type='conference')]) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch open">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued" mode="present" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:edition" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.volume.article')" />
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']/mods:part/mods:detail[@type='volume']" />
            </xsl:call-template>
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
              <xsl:with-param name="property" select="'keyword'" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view conference proceeding metadata -->

  <!-- view conference publication metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.confpub">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.confpub')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@ID)]) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='isReferencedBy']/mods:titleInfo/mods:title) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch open">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:if test="./structure/parents/parent/@xlink:href or ./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']">
              <xsl:call-template name="printMetaDate.mods.relatedItem">
                <xsl:with-param name="parentID" select="./structure/parents/parent/@xlink:href" />
                <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.confpubIn')" />
              </xsl:call-template>
            </xsl:if>
            <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
              <tr>
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('component.mods.metaData.dictionary.genre.kindof'),':')" />
                </td>
                <td class="metavalue">
                  <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
                    mode="printModsClassInfo" />
                </td>
              </tr>
            </xsl:if>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='isReferencedBy']/mods:titleInfo/mods:title" />
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.authority')" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
              <xsl:with-param name="property" select="'keyword'" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view conference publication metadata -->

  <!-- view book metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.book">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.book')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:edition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch open">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:if test="./structure/parents/parent/@xlink:href or ./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']">
              <xsl:call-template name="printMetaDate.mods.relatedItem">
                <xsl:with-param name="parentID" select="./structure/parents/parent/@xlink:href" />
                <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.confpubIn')" />
              </xsl:call-template>
            </xsl:if>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:edition" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateOther" />
              <xsl:with-param name="sep" select="'; '" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm" />
            </xsl:call-template>
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued" mode="present" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.volume.article')" />
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']/mods:part/mods:detail[@type='volume']" />
            </xsl:call-template>
            <xsl:apply-templates mode="present"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:physicalDescription/mods:extent" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
              <xsl:with-param name="property" select="'keyword'" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view book metadata -->

  <!-- view book chapter metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.chapter">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.chapter')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:part/mods:extent) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@ID)]) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='isReferencedBy']/mods:titleInfo/mods:title) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch open">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <table class="metaData">
            <xsl:if test="./structure/parents/parent/@xlink:href or ./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']">
              <xsl:call-template name="printMetaDate.mods.relatedItem">
                <xsl:with-param name="parentID" select="./structure/parents/parent/@xlink:href" />
                <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.chapterIn')" />
              </xsl:call-template>
            </xsl:if>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:choose>
              <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued">
                <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued" mode="present" />
              </xsl:when>
              <xsl:when test="./structure/parents/parent/@xlink:href">
                <xsl:variable name="parent" select="document(concat('mcrobject:',./structure/parents/parent/@xlink:href))/mycoreobject"/>
                <xsl:apply-templates select="$parent//mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued" mode="present" />
              </xsl:when>
            </xsl:choose>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='isReferencedBy']/mods:titleInfo/mods:title" />
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.authority')" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
              <xsl:with-param name="property" select="'keyword'" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>

  </xsl:template>
  <!-- END: view book chapter metadata -->

  <!-- view journal metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.journal">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.journal')" />
      </h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:if test="./structure/children/child">
            <xsl:apply-templates mode="printChildren" select="./structure/children" />
          </xsl:if>
          <xsl:variable name="identifier" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:apply-templates mode="present" select="$identifier" />
          <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier[@type='issn']">
            <xsl:variable name="sherpa_issn" select="." />
            <xsl:for-each select="document(concat('http://www.sherpa.ac.uk/romeo/api29.php?ak=', $MCR.Mods.SherpaRomeo.ApiKey, '&amp;issn=', $sherpa_issn))//publishers/publisher">
              <tr>
                <td class="metaname" valign="top">SHERPA/RoMEO:</td>
                <td class="metavalue">
                  <a href="http://www.sherpa.ac.uk/romeo/search.php?issn={$sherpa_issn}">RoMEO <xsl:value-of select="romeocolour" /> Journal</a>
                </td>
              </tr>
            </xsl:for-each>
          </xsl:for-each>
          <xsl:call-template name="printMetaDate.mods.permalink" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:extension" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>
  <!-- END: view journal metadata -->

  <!-- view series metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.series">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.series')" />
      </h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:if test="./structure/children/child">
            <xsl:apply-templates mode="printChildren" select="./structure/children" />
          </xsl:if>
          <xsl:variable name="identifier" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:apply-templates mode="present" select="$identifier" />
          <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier[@type='issn']">
            <xsl:if test="document(concat('http://www.sherpa.ac.uk/romeo/api29.php?issn=', .))//numhits &gt; 0">
              <xsl:variable name="sherpa_issn" select="." />
              <tr>
                <td class="metaname" valign="top">SHERPA/RoMEO:</td>
                <td class="metavalue">
                  <a href="http://www.sherpa.ac.uk/romeo/search.php?issn={$sherpa_issn}">RoMEO <xsl:value-of select="document(concat('http://www.sherpa.ac.uk/romeo/api29.php?issn=', $sherpa_issn))//romeocolour" /> Journal</a>
                </td>
              </tr>
            </xsl:if>
          </xsl:for-each>
          <xsl:call-template name="printMetaDate.mods.permalink" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:extension" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:publisher" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm" />
          </xsl:call-template>
        </table>
      </div>
    </div>

  </xsl:template>
  <!-- END: view series metadata -->

  <!-- view article metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.article">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.article')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:if
      test="(./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:language) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='isReferencedBy']/mods:titleInfo/mods:title) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition) or
                  (./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID])">
      <div id="category_box" class="detailbox">
        <h4 id="category_switch" class="block_switch open">
          <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.categorybox')" />
        </h4>
        <div id="category_content" class="block_content">
          <xsl:variable name="parentID" select="./structure/parents/parent/@xlink:href" />
          <table class="metaData">
            <xsl:call-template name="printMetaDate.mods.relatedItem">
              <xsl:with-param name="parentID" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']/@xlink:href" />
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.articleIn')" />
            </xsl:call-template>
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
              <xsl:with-param name="sep" select="'; '" />
              <xsl:with-param name="property" select="'keyword'" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:call-template name="printMetaDate.mods.permalink" />
            <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued" mode="present" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
            <xsl:call-template name="printMetaDate.mods">
              <xsl:with-param name="nodes"
                select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='isReferencedBy']/mods:titleInfo/mods:title" />
              <xsl:with-param name="label" select="i18n:translate('component.mods.metaData.dictionary.2ndSource')" />
            </xsl:call-template>
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:url" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:accessCondition" />
            <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@ID]" />
          </table>
        </div>
      </div>
    </xsl:if>
  </xsl:template>
  <!-- END: view article metadata -->

  <!-- view av media metadata -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.av">
    <div id="title_box" class="detailbox floatbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.av')" />
      </h4>
      <xsl:call-template name="printMetaDate.mods.titleContent" />
    </div>

    <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract">
      <xsl:call-template name="printMetaDate.mods.abstractContent" />
    </xsl:if>

    <xsl:call-template name="printMetaDate.mods.categoryContent" />

  </xsl:template>
  <!-- END: view av nedia metadata -->

</xsl:stylesheet>
