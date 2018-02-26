<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:mcrxml="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xalan="http://xml.apache.org/xalan" xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="i18n mcrxml xalan xlink"
>

  <xsl:param name="CurrentUser" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="MCR.MODS.Utils.shortenTitleLength" />

  <xsl:template mode="mods.type" match="mods:mods">
    <xsl:choose>
      <xsl:when
        test="substring-after(mods:genre[@type='intern']/@valueURI,'#')='article' or
              (mods:relatedItem/mods:genre='periodical' and mods:identifier/@type='doi')"
      >
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
              mode="mods.printTitle"
            >
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
            <xsl:text> : </xsl:text>
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
            <xsl:text> : </xsl:text>
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
        <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.proceedingOf',$completeTitle)" />
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
                                         i18n:translate('component.mods.metaData.dictionary.issue'),
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

  <!--Template for generated link names and result titles: see mycoreobject.xsl, results.xsl, MyCoReLayout.xsl -->
  <xsl:template priority="1" mode="resulttitle" match="mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="completeTitle">
      <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods" mode="mods.title" />
    </xsl:variable>
    <xsl:value-of select="mcrxml:shortenText($completeTitle,$MCR.MODS.Utils.shortenTitleLength)" />
  </xsl:template>

  <!--Template for access conditions -->
  <xsl:template match="mods:accessCondition" mode="cc-logo">
    <!-- split category ID e.g "cc_by_4.0" -->
    <xsl:variable name="licenseVersion" select="substring-after(substring-after(@xlink:href, '#cc_'), '_')" />
    <xsl:variable name="licenseString" select="substring-before(substring-after(@xlink:href, '#cc_'), '_')" />
    <xsl:choose>
      <!-- public domain -->
      <xsl:when test="$licenseString='zero' or $licenseString='mark'">
        <a rel="license" href="http://creativecommons.org/publicdomain/{$licenseString}/{$licenseVersion}/">
          <img src="//i.creativecommons.org/p/{$licenseString}/{$licenseVersion}/88x31.png" />
        </a>
      </xsl:when>
      <xsl:otherwise>
        <a rel="license" href="http://creativecommons.org/licenses/{$licenseString}/{$licenseVersion}/">
          <img src="//i.creativecommons.org/l/{$licenseString}/{$licenseVersion}/88x31.png" />
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="oa-logo">
    <a rel="license" href="https://open-access.net/">
      <img src="http://open-access.net/fileadmin/logos/OpenAccess_Logo.JPG" style="width: 25%;" />
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
    <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.rightsReserved')" />
  </xsl:template>

  <xsl:template match="mods:accessCondition" mode="oa_nlz">
    <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.oa_nlz')" />
  </xsl:template>

  <xsl:template match="mods:name" mode="nameLink">
    <xsl:variable name="nameIds">
      <xsl:call-template name="getNameIdentifiers">
        <xsl:with-param name="entity" select="." />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="nameIdentifier" select="xalan:nodeset($nameIds)/nameIdentifier[1]" />

    <!-- if user is in role editor or admin, show all; other users only gets their own and published publications -->
    <xsl:variable name="owner">
      <xsl:choose>
        <xsl:when test="mcrxml:isCurrentUserInRole('admin') or mcrxml:isCurrentUserInRole('editor')">
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
          <xsl:apply-templates select="." mode="nameString" />
          <xsl:value-of select="concat('&quot;', '&amp;owner=createdby:', $owner)" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <a itemprop="creator" href="{$query}">
      <span itemscope="itemscope" itemtype="http://schema.org/Person">
        <span itemprop="name">
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
      <xsl:if test="(string-length(label[@xml:lang='x-uri']/@text) &gt; 0) and count($entity/mods:nameIdentifier[@type = $categId]) &gt; 0">
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

  <xsl:template match="mods:name" mode="nameString">
    <xsl:variable name="name">
      <xsl:choose>
        <xsl:when test="mods:displayForm">
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
          <xsl:value-of select="mods:namePart[@type='family']" />
          <xsl:if test="mods:namePart[@type='given']">
            <xsl:value-of select="concat(', ',mods:namePart[@type='given'])" />
          </xsl:if>
          <xsl:if test="mods:namePart[@type='date']">
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
        <xsl:variable name="modsNameParts" select="xalan:nodeset($modsNames)" />
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
        <xsl:variable name="modsNameParts" select="xalan:nodeset($modsNames)" />
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

</xsl:stylesheet>