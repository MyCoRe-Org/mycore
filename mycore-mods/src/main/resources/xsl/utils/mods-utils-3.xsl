<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:fn="http://www.w3.org/2005/xpath-functions"
  xmlns:mods="http://www.loc.gov/mods/v3"
  xmlns:xlink="http://www.w3.org/1999/xlink" 
  xmlns:mcrstring="http://www.mycore.de/xslt/stringutils"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  
  exclude-result-prefixes="xlink mods fn">
  
  <xsl:param name="CurrentUser" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="MCR.MODS.Utils.shortenTitleLength" />
  <xsl:param name="MCR.MODS.Utils.addTermsOfAddressToDisplayForm" />

  <!--
    Static lists of translatable content and languages (supported by all translatable deed and legal codes) from
    https://creativecommons.org/publicdomain/ and https://creativecommons.org/licenses/.
    For cc_mark, there is no legal code. This is handled explicitly in the template.
  -->
  <xsl:param name="MCR.MODS.Utils.ccSupportedTranslations" select="'deed-1.0,deed-2.0,deed-2.1,deed-2.5,deed-3.0,deed-4.0,legalcode-4.0'" />
  <xsl:param name="MCR.MODS.Utils.ccSupportedLanguages" select="'ar,cs,de,el,en,es,eu,fi,fr,hr,id,it,ja,ko,lt,lv,nl,no,pl,pt,ro,ru,sl,sv,tr,uk'" />

  <!--
    What to link to from Creative Commons licenses; either: default, deed, legalcode
  -->
  <xsl:param name="MCR.MODS.Utils.ccLinkDestination" />

  <!--
    What text link to create additionally for Creative Commons licenses; either: none, text, description
  -->
  <xsl:param name="MCR.MODS.Utils.ccTextLink" />

  <xsl:import href="resource:xsl/functions/stringutils.xsl" />
  <xsl:import href="resource:xsl/functions/i18n.xsl" />

  <xsl:template mode="mods.type" match="mods:mods">
    <xsl:choose>
      <xsl:when
        test="substring-after(mods:genre[@type='intern']/@valueURI,'#')='article' or
              (mods:relatedItem/mods:genre='periodical' and mods:identifier/@type='doi')" >
        <xsl:value-of select="'article'" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring-after(mods:genre[@type='intern']/@valueURI,'#')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode="mods.datePublished" match="mods:mods">
    <xsl:value-of select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:dateIssued" />
  </xsl:template>


  <!--
   -    Prints mods title defined by 'type' [translated|alternative|abbreviated|uniform]. If no type is given
   -    returns main title (default). The parameter 'withSubtitle' [true|false] specifies, if the subtitle
   -    sould be shown.
   -    The parameter 'asHTML' [true|false] is used to define the output should in HTML form if one is given.
   -->
  <xsl:template mode="mods.title" match="mods:mods">
    <xsl:param name="type" select="''" />
    <xsl:param name="asHTML" select="false()" />
    <xsl:param name="withSubtitle" select="false()" />
    <xsl:param name="position" select="''" />

    <xsl:variable name="mods-type">
      <xsl:apply-templates select="." mode="mods.type" />
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="string-length($type) = 0 and ($mods-type='confpro' or $mods-type='proceedings')">
        <xsl:apply-templates select="." mode="mods.title.confpro">
          <xsl:with-param name="asHTML" select="$asHTML" />
          <xsl:with-param name="withSubtitle" select="$withSubtitle" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="string-length($type) = 0 and $mods-type='issue'">
        <xsl:apply-templates select="." mode="mods.title.issue">
          <xsl:with-param name="asHTML" select="$asHTML" />
          <xsl:with-param name="withSubtitle" select="$withSubtitle" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="mods:titleInfo/mods:title">
        <xsl:choose>
          <xsl:when test="string-length($type) &gt; 0 and string-length($position) = 0">
            <xsl:apply-templates select="mods:titleInfo[@type=$type]" mode="mods.printTitle">
              <xsl:with-param name="asHTML" select="$asHTML" />
              <xsl:with-param name="withSubtitle" select="$withSubtitle" />
            </xsl:apply-templates>
          </xsl:when>
          <xsl:when test="string-length($type) &gt; 0 and string-length($position) &gt; 0">
            <xsl:apply-templates select="mods:titleInfo[@type=$type][position()=$position]" mode="mods.printTitle">
              <xsl:with-param name="asHTML" select="$asHTML" />
              <xsl:with-param name="withSubtitle" select="$withSubtitle" />
            </xsl:apply-templates>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="mods:titleInfo[not(@type='uniform' or @type='abbreviated' or @type='alternative' or @type='translated')]"
              mode="mods.printTitle">
              <xsl:with-param name="asHTML" select="$asHTML" />
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
    <xsl:param name="asHTML" select="false()" />
    <xsl:param name="withSubtitle" select="false()" />

    <xsl:variable name="altRepGroup" select="@altRepGroup" />
    <xsl:variable name="hasAlternateFormat" select="count(..//mods:titleInfo[(@altRepGroup = $altRepGroup) and (string-length(@altFormat) &gt; 0)]) &gt; 0" />

    <xsl:choose>
      <xsl:when test="$asHTML and $hasAlternateFormat and (string-length(@altFormat) = 0)">
        <!-- ignore titleInfo -->
      </xsl:when>
      <xsl:when test="$asHTML and $hasAlternateFormat and (string-length(@altFormat) &gt; 0)">
        <xsl:variable name="alternateContent"
          select="document(..//mods:titleInfo[(@altRepGroup = $altRepGroup) and (string-length(@altFormat) &gt; 0)]/@altFormat)/titleInfo" />

        <xsl:if test="$alternateContent/nonSort">
          <xsl:apply-templates select="$alternateContent/nonSort/node()" mode="unescapeHtml" />
          <xsl:text> </xsl:text>
        </xsl:if>
        <xsl:apply-templates select="$alternateContent/title/node()" mode="unescapeHtml" />
        <xsl:if test="$withSubtitle and $alternateContent/subTitle">
          <span class="subtitle">
            <span class="delimiter">
              <xsl:text> : </xsl:text>
            </span>
            <xsl:apply-templates select="$alternateContent/subTitle/node()" mode="unescapeHtml" />
          </span>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="mods:nonSort">
          <xsl:value-of select="concat(mods:nonSort, ' ')" />
        </xsl:if>
        <xsl:value-of select="mods:title" />
        <xsl:if test="$withSubtitle and mods:subTitle">
          <span class="subtitle">
            <span class="delimiter">
              <xsl:text> : </xsl:text>
            </span>
            <xsl:value-of select="mods:subTitle" />
          </span>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode="mods.title.confpro" match="mods:mods">
    <xsl:param name="asHTML" select="false()" />
    <xsl:param name="withSubtitle" select="false()" />
    <xsl:choose>
      <xsl:when test="mods:titleInfo/mods:title">
        <xsl:apply-templates select="mods:titleInfo[not(@type='uniform' or @type='abbreviated' or @type='alternative' or @type='translated')]"
          mode="mods.printTitle"
        >
          <xsl:with-param name="asHTML" select="$asHTML" />
          <xsl:with-param name="withSubtitle" select="$withSubtitle" />
        </xsl:apply-templates>
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
        <xsl:value-of select="mcri18n:translate-with-params('component.mods.metaData.dictionary.proceedingOf',$completeTitle)" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates mode="mods.internalId" select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template mode="mods.title.issue" match="mods:mods">
    <xsl:param name="asHTML" select="false()" />
    <xsl:param name="withSubtitle" select="false()" />
    <xsl:choose>
      <xsl:when test="mods:titleInfo/mods:title">
        <xsl:apply-templates select="mods:titleInfo[not(@type='uniform' or @type='abbreviated' or @type='alternative' or @type='translated')]"
          mode="mods.printTitle"
        >
          <xsl:with-param name="asHTML" select="$asHTML" />
          <xsl:with-param name="withSubtitle" select="$withSubtitle" />
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="mods:relatedItem[@type='host']/mods:part/mods:detail[@type='volume']">
        <xsl:choose>
          <xsl:when test="mods:relatedItem[@type='host']/mods:part/mods:detail[@type='issue']">
            <xsl:value-of
              select="concat(mods:relatedItem[@type='host']/mods:part/mods:detail[@type='volume'],
                                         ', ',
                                         mcri18n:translate('component.mods.metaData.dictionary.issue'),
                                         ' ',
                                         mods:relatedItem[@type='host']/mods:part/mods:detail[@type='issue'])" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="mods:relatedItem[@type='host']/mods:part/mods:detail[@type='volume']" />
          </xsl:otherwise>
        </xsl:choose>
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

  <!--
   -   Prints alternate format of mods abstract, tableOfContents or accessCondition.
   -   The parameter 'asHTML' [true|false] is used to define the output should in HTML form if one is given.
   -   Use the parameter 'filtered' [true|false] with 'true' if do some pre filtering.
   -->
  <xsl:template mode="mods.printAlternateFormat" match="mods:abstract|mods:tableOfContents|mods:accessCondition">
    <xsl:param name="asHTML" select="false()" />
    <xsl:param name="filtered" select="false()" />

    <xsl:variable name="name" select="name(.)" />
    <xsl:variable name="localName" select="local-name(.)" />
    <xsl:variable name="altRepGroup" select="@altRepGroup" />
    <xsl:variable name="hasAlternateFormat" select="count(..//*[(name() = $name) and (@altRepGroup = $altRepGroup) and (string-length(@altFormat) &gt; 0)]) &gt; 0" />

    <xsl:choose>
      <xsl:when test="$asHTML and $hasAlternateFormat and not($filtered) and (string-length(@altFormat) = 0)">
        <!-- ignore -->
      </xsl:when>
      <xsl:when test="$asHTML and $hasAlternateFormat and ((string-length(@altFormat) &gt; 0) or ((string-length(@altFormat) = 0) and $filtered))">
        <xsl:variable name="alternateContent"
          select="document(..//*[(name() = $name) and (@altRepGroup = $altRepGroup) and (string-length(@altFormat) &gt; 0)]/@altFormat)/*[name() = $localName]" />
        <xsl:apply-templates select="$alternateContent/node()" mode="unescapeHtml" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- mycoreobject templates -->
  <xsl:template mode="mods-type" match="mycoreobject">
    <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods" mode="mods.type" />
  </xsl:template>

  <!-- RS: copied to mods-util-3.xsl -->
  <!--Template for generated link names and result titles: see mycoreobject.xsl, results.xsl, MyCoReLayout.xsl -->
  <xsl:template priority="1" mode="resulttitle" match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="completeTitle">
      <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods" mode="mods.title" >
        <xsl:with-param name="withSubtitle" select="true()"/>
      </xsl:apply-templates>
    </xsl:variable>
    <xsl:value-of select="mcrstring:shorten($completeTitle, $MCR.MODS.Utils.shortenTitleLength)" />
  </xsl:template>

  <!--Template for access conditions -->
  <xsl:template match="mods:accessCondition" mode="cc-logo">
    <!-- split category ID e.g "cc_zero_1.0", "cc_mark_1.0", "cc_by-nc-nd_3.0_de", "cc_by_4.0" -->
    <xsl:variable name="licenseId" select="substring-after(@xlink:href,'#')" />
    <xsl:variable name="license" select="document(concat('classification:metadata:0:children:mir_licenses:',$licenseId))//category[@ID=$licenseId]" />
    <xsl:variable name="ccDetails" select="substring-after($licenseId, 'cc_')" />
    <xsl:variable name="ccComponents" select="substring-before($ccDetails, '_')" />
    <xsl:variable name="ccVersionAndCountry" select="substring-after($ccDetails, '_')" />
    <xsl:variable name="ccVersion">
      <xsl:choose>
        <xsl:when test="contains($ccVersionAndCountry,'_')">
          <xsl:value-of select="substring-before($ccVersionAndCountry,'_')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$ccVersionAndCountry" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="ccCountry">
      <xsl:choose>
        <xsl:when test="contains($ccVersionAndCountry,'_')">
          <xsl:value-of select="substring-after($ccVersionAndCountry,'_')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="''" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="publicDomain" select="$ccComponents='zero' or $ccComponents='mark'" />
    <xsl:variable name="ccUrl">
      <xsl:value-of select="'//creativecommons.org/'" />
      <xsl:choose>
        <xsl:when test="$publicDomain">
          <xsl:value-of select="concat('publicdomain/',$ccComponents,'/',$ccVersion,'/')" />
        </xsl:when>
        <xsl:when test="string-length($ccCountry)&gt;0">
          <xsl:value-of select="concat('licenses/',$ccComponents,'/',$ccVersion,'/',$ccCountry,'/')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('licenses/',$ccComponents,'/',$ccVersion,'/')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="aHref">
      <xsl:value-of select="$ccUrl" />
      <xsl:variable name="ccLinkDestination" select="$MCR.MODS.Utils.ccLinkDestination" />
      <xsl:if test="$ccLinkDestination='deed' or $ccLinkDestination='legalcode'">
        <xsl:value-of select="$ccLinkDestination" />
        <xsl:variable name="translation" select="concat($ccLinkDestination,'-',$ccVersion)" />
        <xsl:variable name="ccSupportedTranslations" select="$MCR.MODS.Utils.ccSupportedTranslations" />
        <xsl:variable name="ccSupportedLanguages" select="$MCR.MODS.Utils.ccSupportedLanguages" />
        <xsl:if test="contains($ccSupportedTranslations,$translation) and contains($ccSupportedLanguages,$CurrentLang)">
          <xsl:if test="not($ccComponents='mark' and $ccLinkDestination='legalcode')">
            <xsl:value-of select="concat('.',$CurrentLang)" />
          </xsl:if>
        </xsl:if>
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="licenseLogoUrl" select="$license/label[lang('x-logo')]/@text" />
    <xsl:variable name="imgSrc">
      <xsl:choose>
        <xsl:when test="string-length($licenseLogoUrl)&gt;0">
          <xsl:value-of select="$licenseLogoUrl" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="'//licensebuttons.net/'" />
          <xsl:choose>
            <xsl:when test="$publicDomain">
              <xsl:value-of select="concat('p/',$ccComponents,'/',$ccVersion)" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat('l/',$ccComponents,'/',$ccVersion)" />
            </xsl:otherwise>
          </xsl:choose>
          <xsl:value-of select="'/88x31.png'" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <a class="cc-logo" rel="license" href="{$aHref}">
      <img src="{$imgSrc}" />
    </a>
    <xsl:variable name="ccTextLink" select="$MCR.MODS.Utils.ccTextLink" />
    <xsl:if test="$ccTextLink='text' or $ccTextLink='description'">
      <br />
      <a class="cc-text" rel="license" href="{$aHref}">
        <xsl:choose>
          <xsl:when test="$license/label[lang($CurrentLang)]">
            <xsl:value-of select="$license/label[lang($CurrentLang)]/@*[name()=$ccTextLink]" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$license/label[lang($DefaultLang)]/@*[name()=$ccTextLink]" />
          </xsl:otherwise>
        </xsl:choose>
      </a>
    </xsl:if>
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="oa-logo">
    <a rel="license" href="https://open-access.net/">
      <img src="//open-access.net/fileadmin/logos/OpenAccess_Logo.JPG" style="width: 25%;" />
    </a>
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="ogl-logo">
    <xsl:variable name="licenseVersion" select="substring-before(substring-after(@xlink:href, '#ogl_'), '.')" />
    <a rel="license" href="http://www.nationalarchives.gov.uk/doc/open-government-licence/version/{$licenseVersion}/">
      <img src="//upload.wikimedia.org/wikipedia/en/4/46/UKOpenGovernmentLicence.svg" style="width: 50px;" />
    </a>
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="cc-text">
    <xsl:variable name="trimmed" select="substring-after(normalize-space(@xlink:href),'#')" />
    <xsl:variable name="licenseURI" select="concat('classification:metadata:0:children:mir_licenses:',$trimmed)" />
    <xsl:variable name="licenseXML" select="document($licenseURI)" />
    <xsl:choose>
      <xsl:when test="$licenseXML//category/label[@xml:lang=$CurrentLang]/@text">
        <xsl:value-of select="$licenseXML//category/label[@xml:lang=$CurrentLang]/@text" />
      </xsl:when>
      <xsl:when test="$licenseXML//category/label[@xml:lang=$DefaultLang]/@text">
        <xsl:value-of select="$licenseXML//category/label[@xml:lang=$DefaultLang]/@text" />
      </xsl:when>
      <xsl:when test="$licenseXML//category/label[1]/@text">
        <xsl:value-of select="$licenseXML//category/label[1]/@text" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$trimmed" />
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="rights_reserved">
    <xsl:value-of select="mcri18n:translate('component.mods.metaData.dictionary.rightsReserved')" />
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="oa_nlz">
    <xsl:value-of select="mcri18n:translate('component.mods.metaData.dictionary.oa_nlz')" />
  </xsl:template>

  <xsl:template match="mods:name" mode="nameLink">
    <xsl:variable name="nameIds">
      <xsl:call-template name="getNameIdentifiers">
        <xsl:with-param name="entity" select="." />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="nameIdentifier" select="$nameIds/nameIdentifier[1]" />

    <!-- if user is in role editor or admin, show all; other users only gets their own and published publications -->
    <xsl:variable name="owner">
      <xsl:choose>
        <!-- old XSLT1: <xsl:when test="mcrxml:isCurrentUserInRole('admin') or mcrxml:isCurrentUserInRole('editor')"> -->
        <xsl:when test="document('solrwr:isCurrentUserInRole:admin')/text()='true' or document('solrwr:isCurrentUserInRole:editor')/text() = 'true'">
          <xsl:text>*</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$CurrentUser" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="query">
      <xsl:choose>
        <xsl:when test="count($nameIdentifier) &gt; 0">
          <xsl:value-of
            select="concat($ServletsBaseURL,'solr/mods_nameIdentifier?q=mods.nameIdentifier:', $nameIdentifier/@type, '%5C:', $nameIdentifier/@id, '&amp;owner=createdby:', $owner)" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($ServletsBaseURL,'solr/mods_nameIdentifier?q=', '+mods.name:&quot;')" />
          <xsl:apply-templates select="." mode="queryableNameString" />
          <xsl:value-of select="concat('&quot;', '&amp;owner=createdby:', $owner)" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <a href="{$query}">
      <span>
        <span>
          <xsl:apply-templates select="." mode="nameString" />
        </span>
        <xsl:if test="count($nameIdentifier) &gt; 0">
          <xsl:text>&#160;</xsl:text><!-- add whitespace here -->
          <a href="{$nameIdentifier/@uri}{$nameIdentifier/@id}" title="Link zu {$nameIdentifier/@label}">
            <sup>
              <xsl:value-of select="$nameIdentifier/@label" />
            </sup>
          </a>
        </xsl:if>
      </span>
    </a>
  </xsl:template>

  <xsl:variable name="nameIdentifiers" select="document(concat('classification:metadata:all:children:','nameIdentifier'))/mycoreclass/categories" />

  <xsl:template name="getNameIdentifiers">
    <xsl:param name="entity" />

    <xsl:for-each select="$nameIdentifiers/category">
      <xsl:sort select="x-order" data-type="number" />
      <xsl:variable name="categId" select="@ID" />
      <xsl:if test="not(label[@xml:lang='x-display']/@text='false') and (string-length(label[@xml:lang='x-uri']/@text) &gt; 0) and count($entity/mods:nameIdentifier[@type = $categId]) &gt; 0">
        <nameIdentifier>
          <xsl:attribute name="label">
            <xsl:value-of select="label[@xml:lang='de']/@text" />
          </xsl:attribute>
          <xsl:attribute name="uri">
            <xsl:value-of select="label[@xml:lang='x-uri']/@text" />
          </xsl:attribute>
          <xsl:attribute name="type">
            <xsl:value-of select="$categId" />
          </xsl:attribute>
          <xsl:attribute name="id">
            <xsl:value-of select="$entity/mods:nameIdentifier[@type = $categId]/text()" />
          </xsl:attribute>
        </nameIdentifier>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="mods:name" mode="queryableNameString">
    <xsl:apply-templates select="." mode="nameString">
      <xsl:with-param name="queryable" select="true()"/>
    </xsl:apply-templates>
  </xsl:template>

  <xsl:template match="mods:name" mode="nameString">
    <xsl:param name="queryable" select="false()"/>
    <xsl:variable name="name">
      <xsl:choose>
        <xsl:when test="mods:displayForm">
          <xsl:if test="$MCR.MODS.Utils.addTermsOfAddressToDisplayForm='true' and not($queryable) and mods:namePart[@type='termsOfAddress']">
            <xsl:value-of select="concat(mods:namePart[@type='termsOfAddress'], ' ')" />
          </xsl:if>
          <xsl:value-of select="mods:displayForm" />
        </xsl:when>
        <xsl:when test="mods:namePart[not(@type)]">
          <xsl:for-each select="mods:namePart[not(@type)]">
            <xsl:choose>
              <xsl:when test="position() = 1">
                <xsl:value-of select="text()" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat(' ', text())" />
              </xsl:otherwise>
            </xsl:choose>
          </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
          <xsl:if test="not($queryable) and mods:namePart[@type='termsOfAddress']">
            <xsl:value-of select="concat(mods:namePart[@type='termsOfAddress'], ' ')" />
          </xsl:if>
          <xsl:value-of select="mods:namePart[@type='family']" />
          <xsl:if test="mods:namePart[@type='given']">
            <xsl:value-of select="concat(', ',mods:namePart[@type='given'])" />
          </xsl:if>
          <xsl:if test="not($queryable) and mods:namePart[@type='date']">
            <xsl:value-of select="concat(' (',mods:namePart[@type='date'],')')" />
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:value-of select="normalize-space($name)" />
  </xsl:template>

  <xsl:template match="*" mode="copyNode">
    <xsl:copy-of select="node()" />
  </xsl:template>

  <xsl:template match="*" mode="unescapeHtml">
    <xsl:copy>
      <xsl:apply-templates select="@*" />
      <xsl:apply-templates mode="unescapeHtml" />
    </xsl:copy>
  </xsl:template>

  <xsl:template match="text()" mode="unescapeHtml">
    <xsl:value-of select="." disable-output-escaping="yes" />
  </xsl:template>

  <xsl:template name="mods.seperateName">
    <xsl:param name="displayForm" />
    <xsl:choose>
      <xsl:when test="contains($displayForm, ',')">
        <mods:namePart type="family">
          <xsl:value-of select="normalize-space(substring-before($displayForm, ','))" />
        </mods:namePart>
        <xsl:variable name="modsNames">
          <xsl:call-template name="mods.tokenizeName">
            <xsl:with-param name="name" select="normalize-space(substring-after($displayForm, ','))" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="modsNameParts" select="$modsNames" />
        <xsl:for-each select="$modsNameParts/namePart">
          <mods:namePart type="given">
            <xsl:value-of select="." />
          </mods:namePart>
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="contains($displayForm, ' ')">
        <xsl:variable name="modsNames">
          <xsl:call-template name="mods.tokenizeName">
            <xsl:with-param name="name" select="normalize-space($displayForm)" />
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="modsNameParts" select="$modsNames" />
        <xsl:for-each select="$modsNameParts/namePart">
          <mods:namePart>
            <xsl:choose>
              <xsl:when test="position()!=last()">
                <xsl:attribute name="type">
                  <xsl:value-of select="'given'" />
                </xsl:attribute>
              </xsl:when>
              <xsl:otherwise>
                <xsl:attribute name="type">
                  <xsl:value-of select="'family'" />
                </xsl:attribute>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="." />
          </mods:namePart>
        </xsl:for-each>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="mods.tokenizeName">
    <xsl:param name="name" />
    <xsl:param name="delimiter" select="' '" />
    <xsl:choose>
      <xsl:when test="$delimiter and contains($name, $delimiter)">
        <namePart>
          <xsl:value-of select="substring-before($name,$delimiter)" />
        </namePart>
        <xsl:call-template name="mods.tokenizeName">
          <xsl:with-param name="name" select="substring-after($name,$delimiter)" />
          <xsl:with-param name="delimiter" select="$delimiter" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <namePart>
          <xsl:value-of select="$name" />
        </namePart>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- TODO: Migration to XSLT3
       Wird das Template überhaupt aufgerufen?
       Wenn ja - Bekommen wir diese Infos ggf. auch aus den Servflags?
       XSLT3 kann inzwischen auch JSON verarbeiten! -->

  <!--
  <xsl:template mode="preferredPI" match="mods:mods">
    <xsl:param name="type" />
    <xsl:param name="prefix" select="''"/>
    <xsl:param name="preferManaged" select="false()" />
    <xsl:variable name="candidate"
                  select="mods:identifier[@type=$type and starts-with(text(), $prefix)]" />
    <xsl:variable name="preferred"
                  select="$candidate[$preferManaged=piUtil:isManagedPI(text(), /mycoreobject/@ID)]" />
    <xsl:choose>
      <xsl:when test="$preferred">
        <xsl:value-of select="$preferred[1]" />
      </xsl:when>
      <xsl:when test="$candidate">
        <xsl:value-of select="$candidate[1]" />
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  -->
  
</xsl:stylesheet>  