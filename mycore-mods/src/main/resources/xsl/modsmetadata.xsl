<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:mcrmods="xalan://org.mycore.mods.MCRMODSClassificationSupport"
  xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" exclude-result-prefixes="xlink mcr i18n acl mods mcrmods" version="1.0">
  <xsl:param name="MCR.Handle.Resolver.MasterURL" />

  <xsl:template name="printMetaDate.mods">
    <!-- prints a table row for a given nodeset -->
    <xsl:param name="nodes" />
    <xsl:param name="label" select="i18n:translate(concat('metaData.mods.dictionary.',local-name($nodes[1])))" />
    <xsl:param name="sep" select="''" />
    <xsl:message>
      <xsl:value-of select="concat('label: ',$label)" />
    </xsl:message>
    <xsl:message>
      <xsl:value-of select="concat('nodes: ',count($nodes))" />
    </xsl:message>
    <xsl:if test="$nodes">
      <tr>
        <td valign="top" class="metaname">
          <xsl:value-of select="concat($label,':')" />
        </td>
        <td class="metavalue">
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
              <xsl:value-of select="normalize-space(.)" />
            </xsl:if>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*" mode="printModsClassInfo">
    <xsl:variable name="classlink" select="mcrmods:getClassCategLink(.)" />
    <xsl:choose>
      <xsl:when test="string-length($classlink) &gt; 0">
        <xsl:for-each select="document($classlink)/mycoreclass/categories/category">
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
    <tr>
      <td valign="top" class="metaname">
        <xsl:variable name="title">
          <xsl:choose>
            <xsl:when test="@type='translated'">
              <xsl:value-of select="concat(' (',mods:title/@lang,') ')" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="' '" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.title'),$title,':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="mods:title" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:abstract" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.abstract'),' (' ,@lang,'):')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:extent" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.extent'),':')" />
      </td>
      <td class="metavalue">
        <xsl:choose>
          <xsl:when test="count(mods:start) &gt; 0">
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.page'),': ',mods:start,'-',mods:end)" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(mods:total,' ',i18n:translate('metaData.mods.dictionary.pages'))" />
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:extension[@displayLabel='referenced']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate(concat('metaData.mods.dictionary.',@displayLabel)),':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="i18n:translate(concat('metaData.mods.dictionary.',.))" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:extension" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate(concat('metaData.mods.dictionary.',@displayLabel)),':')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:name" mode="printName">
    <xsl:choose>
      <xsl:when test="mods:displayForm">
        <xsl:choose>
          <xsl:when test="@valueURI">
            <a href="{@valueURI}" class="extern">
              <xsl:value-of select="mods:displayForm" />
            </a>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="mods:displayForm" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="@valueURI">
        <xsl:apply-templates select="." mode="printModsClassInfo" />
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
      <xsl:otherwise>
        <xsl:value-of select="." />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mods:name" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:choose>
          <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']">
            <xsl:apply-templates select="mods:role/mods:roleTerm[@authority='marcrelator' and @type='code']" mode="printModsClassInfo" />
            <xsl:value-of select="':'" />
          </xsl:when>
          <xsl:when test="mods:role/mods:roleTerm[@authority='marcrelator']">
            <xsl:value-of
              select="concat(i18n:translate(concat('metaData.mods.dictionary.',mods:role/mods:roleTerm[@authority='marcrelator'])),':')" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.name'),':')" />
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
        <xsl:value-of select="'Handle:'" />
      </td>
      <td class="metavalue">
        <xsl:variable name="hdl" select="." />
        <a href="{$MCR.Handle.Resolver.MasterURL}{$hdl}">
          <xsl:value-of select="$hdl" />
        </a>
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:identifier[@type!='urn']" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.identifier'),'(',@type,') :')" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="." />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="mods:classification" mode="present">
    <tr>
      <td valign="top" class="metaname">
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.classification'), ':')" />
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
        <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.language'), ':')" />
      </td>
      <td class="metavalue">
        <xsl:for-each select="mods:languageTerm">
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
        </xsl:for-each>
      </td>
    </tr>
  </xsl:template>


  <!--  -->
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.report">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">Report</h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='personal']" />
          <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
            <tr>
              <td valign="top" class="metaname">
                <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.genre.kindof'),':')" />
              </td>
              <td class="metavalue">
                <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
                  mode="printModsClassInfo" />
              </td>
            </tr>
          </xsl:if>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateCreated" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type!='personal']" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
            <xsl:with-param name="sep" select="'; '" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.thesis">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.thesisbox')" />
      </h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='personal']" />
        </table>
      </div>
    </div>

    <div id="abstract_box" class="detailbox">
      <h4 id="abstract_switch" class="block_switch">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.abstractbox')" />
      </h4>
      <div id="abstract_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
        </table>
      </div>
    </div>

    <div id="category_box" class="detailbox">
      <h4 id="category_switch" class="block_switch">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.categorybox')" />
      </h4>
      <div id="category_content" class="block_content">
        <table class="metaData">
          <xsl:if test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']">
            <tr>
              <td valign="top" class="metaname">
                <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.genre.kindof'),':')" />
              </td>
              <td class="metavalue">
                <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:genre[@type='kindof']"
                  mode="printModsClassInfo" />
              </td>
            </tr>
          </xsl:if>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
            <xsl:with-param name="sep" select="'; '" />
          </xsl:call-template>
        </table>
      </div>
    </div>

    <div id="institution_box" class="detailbox">
      <h4 id="institution_switch" class="block_switch">
        <xsl:value-of select="i18n:translate('metaData.mods.dictionary.institutionbox')" />
      </h4>
      <div id="institution_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='corporate']" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="title.cproceeding">
    <xsl:choose>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title">
        <xsl:call-template name="ShortenText">
          <xsl:with-param name="text" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1]" />
          <xsl:with-param name="length" select="70" />
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
        <xsl:variable name="completeTitle">
          <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
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
        <xsl:call-template name="ShortenText">
          <xsl:with-param name="text" select="i18n:translate('metaData.mods.dictionary.proceedingOf',$completeTitle)" />
          <xsl:with-param name="length" select="70" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@ID" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.cproceeding">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">Konferenzband</h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[@type='conference']">
            <td valign="top" class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.conference'),':')" />
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
          </xsl:for-each>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther" />
            <xsl:with-param name="sep" select="'; '" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:apply-templates mode="present"
            select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name[not(@type='conference')]" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.cpublication">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">Konferenzbeitrag</h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.book">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">Buch</h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:publisher" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:edition" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateOther" />
            <xsl:with-param name="sep" select="'; '" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:place/mods:placeTerm" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:originInfo/mods:dateIssued" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:detail/mods:number" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:part/mods:extent" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.book-chapter">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">Buchkapitel</h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/modsrelatedItem[not(@type='isReferencedBy')]/mods:titleInfo/mods:title" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:apply-templates mode="present"
            select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:part/mods:extent" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo[@type='isReferencedBy']/mods:title" />
            <xsl:with-param name="label" select="i18n:translate('metaData.mods.dictionary.authority')" />
          </xsl:call-template>

          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:subject/mods:topic" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.journal">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">Zeitschrift</h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
            <xsl:with-param name="label" select="'ISSN'" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:extension" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="present.article">
    <div id="title_box" class="detailbox">
      <h4 id="title_switch" class="block_switch open">Zeitschriftenaufsatz</h4>
      <div id="title_content" class="block_content">
        <table class="metaData">
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:name" />
          <xsl:for-each select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem[@type='host']">
            <tr>
              <td valign="top" class="metaname">
                <xsl:value-of select="concat(i18n:translate('metaData.mods.dictionary.articleIn'),':')" />
              </td>
              <td class="metavalue">
                <!-- Journal -->
                <!-- Issue -->
                <xsl:value-of
                  select="concat(mods:part/mods:detail[@type='issue']/mods:caption,' ',mods:part/mods:detail[@type='issue']/mods:number)" />
                <xsl:if test="mods:part/mods:date">
                  <xsl:value-of select="concat('/',mods:part/mods:date,' ')" />
                </xsl:if>
                <!-- Volume -->
                <xsl:if test="mods:part/mods:detail[@type='volume']/mods:number">
                  <xsl:value-of
                    select="concat('(',i18n:translate('metaData.mods.dictionary.volume.article'),': ',mods:part/mods:detail[@type='volume']/mods:number,')')" />
                </xsl:if>
                <!-- Pages -->
                <xsl:for-each select="mods:part/mods:extent[@unit='pages']">
                  <xsl:value-of select="concat(', ', i18n:translate('metaData.mods.dictionary.page.abbr'),' ')" />
                  <xsl:choose>
                    <xsl:when test="mods:start = mods:end">
                      <xsl:value-of select="mods:start" />
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="concat(mods:start,'-',mods:end)" />
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:for-each>
                <!-- date issued -->
              </td>
            </tr>
          </xsl:for-each>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:originInfo/mods:dateIssued" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:identifier" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:abstract" />
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:language" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:note" />
          </xsl:call-template>
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:location/mods:physicalLocation" />
          </xsl:call-template>
          <xsl:apply-templates mode="present" select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:classification" />
          <xsl:call-template name="printMetaDate.mods">
            <xsl:with-param name="nodes"
              select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:relatedItem/mods:titleInfo/mods:title" />
          </xsl:call-template>
        </table>
      </div>
    </div>
  </xsl:template>

</xsl:stylesheet>