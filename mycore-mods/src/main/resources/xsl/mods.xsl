<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:mcr="http://www.mycore.org/"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:encoder="xalan://java.net.URLEncoder"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mcrurn="xalan://org.mycore.urn.MCRXMLFunctions" exclude-result-prefixes="xalan xlink mcr i18n acl mods mcrxsl mcrurn encoder" version="1.0">
  <xsl:param select="'local'" name="objectHost" />
  <xsl:param name="MCR.Users.Superuser.UserName" />
  <xsl:include href="mods-utils.xsl" />
  <xsl:include href="mods2html.xsl" />
  <xsl:include href="modsmetadata.xsl" />
  <xsl:include href="mods-highwire.xsl" />

  <xsl:include href="basket.xsl" />

  <xsl:include href="modshitlist-external.xsl" />  <!-- for external usage in application module -->
  <xsl:include href="modsdetails-external.xsl" />  <!-- for external usage in application module -->

  <xsl:variable name="head.additional">
    <xsl:if test="contains(/mycoreobject/@ID,'_mods_')">
      <!-- ==================== Highwire Press tags ==================== -->
      <xsl:apply-templates select="/mycoreobject/metadata/def.modsContainer/modsContainer/mods:mods" mode="highwire" />
    </xsl:if>
  </xsl:variable>

  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="basketContent">
    <xsl:call-template name="objectLink">
      <xsl:with-param select="." name="mcrobj" />
    </xsl:call-template>
    <div class="description">
      <xsl:for-each select="./metadata/def.modsContainer/modsContainer/*">
<!-- Link to presentation, ?pt -->
        <xsl:for-each select="mods:identifier[@type='uri']">
          <a href="{.}">
            <xsl:value-of select="." />
          </a>
          <br />
        </xsl:for-each>
<!-- Place, ?pt -->
        <xsl:for-each select="mods:originInfo[not(@eventType) or @eventType='publication']/mods:place/mods:placeTerm[@type='text']">
          <xsl:value-of select="." />
        </xsl:for-each>
<!-- Author -->
        <xsl:for-each select="mods:name[mods:role/mods:roleTerm/text()='aut']">
          <xsl:if test="position()!=1">
            <xsl:value-of select="'; '" />
          </xsl:if>
          <xsl:apply-templates select="." mode="printName" />
          <xsl:if test="position()=last()">
            <br />
          </xsl:if>
        </xsl:for-each>
<!-- Shelfmark -->
        <xsl:for-each select="mods:location/mods:shelfLocator">
          <xsl:value-of select="." />
          <br />
        </xsl:for-each>
<!-- URN -->
        <xsl:for-each select="mods:identifier[@type='urn']">
          <xsl:value-of select="." />
          <br />
        </xsl:for-each>
      </xsl:for-each>
    </div>
  </xsl:template>

  <!--Template for title in metadata view: see mycoreobject.xsl -->
  <xsl:template priority="1" mode="title" match="/mycoreobject[contains(@ID,'_mods_')]">
    <xsl:variable name="mods-type">
      <xsl:apply-templates select="." mode="mods-type" />
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$mods-type='confpro'">
        <xsl:apply-templates select="./metadata/def.modsContainer/modsContainer/mods:mods" mode="mods.title.confpro" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title">
            <xsl:variable name="text">
              <xsl:choose>
                <xsl:when test="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo[@transliteration]/mods:title">
                  <!-- TODO: if editor bug fixed -->
                  <xsl:value-of select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo[@transliteration]/mods:title" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="./metadata/def.modsContainer/modsContainer/mods:mods/mods:titleInfo/mods:title[1]" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:value-of select="$text" disable-output-escaping="yes" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@ID" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!--Template for metadata view: see mycoreobject.xsl -->
  <xsl:template priority="1" mode="present" match="/mycoreobject[contains(@ID,'_mods_')]">
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

      <!--1***modsContainer************************************* -->
    <xsl:variable name="mods-type">
      <xsl:apply-templates mode="mods-type" select="." />
    </xsl:variable>

    <xsl:variable name="rdfaTypeOf">
      <xsl:choose>
        <xsl:when test="$mods-type = 'article'">  <xsl:value-of select="'ScholarlyArticle'" /></xsl:when>
        <xsl:when test="$mods-type = 'book'">     <xsl:value-of select="'Book'" />            </xsl:when>
        <xsl:when test="$mods-type = 'confpro'">  <xsl:value-of select="'Book'" />            </xsl:when>
        <xsl:when test="$mods-type = 'confpub'">  <xsl:value-of select="'ScholarlyArticle'" /></xsl:when>
        <xsl:when test="$mods-type = 'journal'">  <xsl:value-of select="'Periodical'" />      </xsl:when>
        <xsl:when test="$mods-type = 'series'">   <xsl:value-of select="'Periodical'" />      </xsl:when>
        <xsl:when test="$mods-type = 'chapter'">  <xsl:value-of select="'CreativeWork'" />    </xsl:when>
        <xsl:when test="$mods-type = 'report'">   <xsl:value-of select="'CreativeWork'" />    </xsl:when>
        <xsl:when test="$mods-type = 'av-medium'"><xsl:value-of select="'MediaObject'" />     </xsl:when>
      </xsl:choose>
    </xsl:variable>

    <div id="detail_view" class="blockbox" typeOf="{$rdfaTypeOf}">
      <h3>
        <xsl:apply-templates select="." mode="resulttitle" /><!-- shorten plain text title (without html) -->
      </h3>
      <xsl:choose>
        <xsl:when test="$mods-type='series'">
          <xsl:apply-templates select="." mode="objectActions">
            <xsl:with-param name="layout"     select="'journal'" />
            <xsl:with-param name="mods-type"  select="$mods-type" />
          </xsl:apply-templates>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="." mode="objectActions">
            <xsl:with-param select="$mods-type" name="layout" />
            <xsl:with-param select="$mods-type" name="mods-type" />
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:choose>
        <!-- xsl:when cases are handled in modsmetadata.xsl -->
        <xsl:when test="$mods-type = 'report'">
          <xsl:apply-templates select="." mode="present.report" />
        </xsl:when>
        <xsl:when test="$mods-type = 'thesis'">
          <xsl:apply-templates select="." mode="present.thesis" />
        </xsl:when>
        <xsl:when test="$mods-type = 'confpro'">
          <xsl:apply-templates select="." mode="present.confpro" />
        </xsl:when>
        <xsl:when test="$mods-type = 'confpub'">
          <xsl:apply-templates select="." mode="present.confpub" />
        </xsl:when>
        <xsl:when test="$mods-type = 'book'">
          <xsl:apply-templates select="." mode="present.book" />
        </xsl:when>
        <xsl:when test="$mods-type = 'chapter'">
          <xsl:apply-templates select="." mode="present.chapter" />
        </xsl:when>
        <xsl:when test="$mods-type = 'journal'">
          <xsl:apply-templates select="." mode="present.journal" />
        </xsl:when>
        <xsl:when test="$mods-type = 'series'">
          <xsl:apply-templates select="." mode="present.series" />
        </xsl:when>
        <xsl:when test="$mods-type = 'article'">
          <xsl:apply-templates select="." mode="present.article" />
        </xsl:when>
        <xsl:when test="$mods-type = 'av'">
          <xsl:apply-templates select="." mode="present.av" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="." mode="present.modsDefaultType">
            <xsl:with-param name="mods-type" select="$mods-type" />
          </xsl:apply-templates>
        </xsl:otherwise>
      </xsl:choose>
      <!--*** Editor Buttons ************************************* -->
      <!-- put all derivates and display value in one variable -->
      <xsl:variable name="derivateDisplayList">
        <derivates>
        <xsl:for-each select="./structure/derobjects/derobject">
          <derivate>
            <id><xsl:value-of select="@xlink:href" /></id>
            <display><xsl:value-of select="mcrxsl:isDisplayedEnabledDerivate(@xlink:href)" /></display>
          </derivate>
        </xsl:for-each>
        </derivates>
      </xsl:variable>
      <xsl:variable name="isDisplayedEnabled" select="contains($derivateDisplayList, 'true')" />
      <xsl:if
        test="((./structure/children/child) and not($mods-type='series' or $mods-type='journal' or $mods-type='confpro' or $mods-type='book')) or (./structure/derobjects/derobject and ($isDisplayedEnabled or not(mcrxsl:isCurrentUserGuestUser())))">
        <div id="derivate_box" class="detailbox">
          <h4 id="derivate_switch" class="block_switch">
            <a name="derivate_box"></a>
            <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.derivatebox')" />
          </h4>
          <div id="derivate_content" class="block_content">
            <table class="metaData">
              <!--*** List children per object type ************************************* -->
              <!-- 1.) get a list of objectTypes of all child elements 2.) remove duplicates from this list 3.) for-each objectTyp id list child elements -->
              <xsl:variable name="objectTypes">
                <xsl:for-each select="./structure/children/child/@xlink:href">
                  <id>
                    <xsl:copy-of select="substring-before(substring-after(.,'_'),'_')" />
                  </id>
                </xsl:for-each>
              </xsl:variable>
              <xsl:variable select="xalan:nodeset($objectTypes)/id[not(.=following::id)]" name="unique-ids" />
              <!-- the for-each would iterate over <id> with root not beeing /mycoreobject so we save the current node in variable context to access
                needed nodes -->
              <xsl:variable select="." name="context" />
              <xsl:for-each select="$unique-ids">
                <xsl:variable select="." name="thisObjectType" />
                <xsl:variable name="label">
                  <xsl:choose>
                    <xsl:when test="count($context/structure/children/child[contains(@xlink:href,$thisObjectType)])=1">
                      <xsl:value-of select="i18n:translate(concat('component.',$thisObjectType,'.metaData.[singular]'))" />
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="i18n:translate(concat('component.',$thisObjectType,'.metaData.[plural]'))" />
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:variable>
                <xsl:call-template name="printMetaDate">
                  <xsl:with-param select="$context/structure/children/child[contains(@xlink:href, concat('_',$thisObjectType,'_'))]"
                    name="nodes" />
                  <xsl:with-param select="$label" name="label" />
                </xsl:call-template>
              </xsl:for-each>
              <xsl:apply-templates mode="printDerivates" select=".">
                <xsl:with-param select="$staticURL" name="staticURL" />
              </xsl:apply-templates>
            </table>
          </div>
        </div>
      </xsl:if>
      <!--*** Created ************************************* -->
      <xsl:if test="not(mcrxsl:isCurrentUserGuestUser())">
        <div id="system_box" class="detailbox">
          <h4 id="system_switch" class="block_switch">
            <xsl:value-of select="i18n:translate('component.mods.metaData.dictionary.systembox')" />
          </h4>
          <div id="system_content" class="block_content">
            <table class="metaData">
              <xsl:call-template name="printMetaDate">
                <xsl:with-param select="./service/servdates/servdate[@type='createdate']" name="nodes" />
                <xsl:with-param select="i18n:translate('metaData.createdAt')" name="label" />
              </xsl:call-template>
              <!--*** Last Modified ************************************* -->
              <xsl:call-template name="printMetaDate">
                <xsl:with-param select="./service/servdates/servdate[@type='modifydate']" name="nodes" />
                <xsl:with-param select="i18n:translate('metaData.lastChanged')" name="label" />
              </xsl:call-template>
              <!--*** MyCoRe-ID ************************************* -->
              <tr>
                <td class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.ID'),':')" />
                </td>
                <td class="metavalue">
                  <xsl:value-of select="./@ID" />
                </td>
              </tr>
              <tr>
                <td class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.versions'),' :')" />
                </td>
                <td class="metavalue">
                  <xsl:apply-templates select="." mode="versioninfo" />
                </td>
              </tr>
            </table>
          </div>
        </div>
      </xsl:if>

    </div>
  </xsl:template>

  <xsl:template mode="printDerivates" match="/mycoreobject[contains(@ID,'_mods_')]" priority="1">
    <xsl:param name="staticURL" />
    <xsl:param name="layout" />
    <xsl:param name="xmltempl" />
    <xsl:variable name="suffix">
      <xsl:if test="string-length($layout)&gt;0">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:if test="./structure/derobjects">
      <xsl:variable name="parentObjID" select="./@ID" />
      <tr>
        <td style="vertical-align:top;" class="metaname">
          <xsl:value-of select="concat(i18n:translate('component.mods.metaData.[derivates]'), ':')" />
        </td>
        <td class="metavalue">
          <xsl:if test="$objectHost != 'local'">
            <a href="{$staticURL}">nur auf original Server</a>
          </xsl:if>
          <xsl:if test="$objectHost = 'local'">
            <xsl:for-each select="./structure/derobjects/derobject">
              <xsl:variable select="@xlink:href" name="deriv" />

              <div class="derivateBox">
                <xsl:variable select="concat('mcrobject:',$deriv)" name="derivlink" />
                <xsl:variable select="document($derivlink)" name="derivate" />
                <xsl:variable name="derivateWithURN" select="mcrurn:hasURNDefined($deriv)" />

                <xsl:apply-templates select="$derivate/mycorederivate/derivate/internals" />
                <xsl:if test="$derivateWithURN">
                  <xsl:variable name="derivateURN" select="$derivate/mycorederivate/derivate/fileset/@urn" />
                  <a href="{concat('http://nbn-resolving.de/urn/resolver.pl?urn=',$derivateURN)}">
                    <xsl:value-of select="$derivateURN" />
                  </a>
                </xsl:if>
                <xsl:apply-templates select="$derivate/mycorederivate/derivate/externals" />

                <!-- MCR-IView ..start -->
                <xsl:call-template name="derivateView">
                  <xsl:with-param name="derivateID" select="$deriv" />
                </xsl:call-template>
                <!-- MCR - IView ..end -->

                <xsl:if test="acl:checkPermission(./@xlink:href,'writedb')">
                  <div class="derivate_options">
                    <!-- TODO: refacture! -->
                    <img class="button_options" src="{$WebApplicationBaseURL}images/icon_arrow_circled_red_down.png"
                         alt="show derivate options" title="{i18n:translate('component.mods.metaData.options')}" />
                    <div class="options">
                      <ul>
                        <xsl:if test="$derivateWithURN=false()">
                          <li>
                            <a href="{$ServletsBaseURL}derivate/update{$HttpSession}?objectid={../../../@ID}&amp;id={@xlink:href}{$suffix}">
                              <xsl:value-of select="i18n:translate('component.mods.derivate.addFile')" />
                            </a>
                          </li>
                        </xsl:if>
                        <li>
                          <xsl:if test="not($derivateWithURN=false() and mcrxsl:isAllowedObjectForURNAssignment($parentObjID))">
                            <xsl:attribute name="class">last</xsl:attribute>
                          </xsl:if>
                          <a href="{$ServletsBaseURL}derivate/update{$HttpSession}?id={@xlink:href}{$suffix}">
                            <xsl:value-of select="i18n:translate('component.mods.derivate.editDerivate')" />
                          </a>
                        </li>
                        <xsl:if test="$derivateWithURN=false() and mcrxsl:isAllowedObjectForURNAssignment($parentObjID)">
                          <xsl:variable name="apos">
                            <xsl:text>'</xsl:text>
                          </xsl:variable>
                          <li>
                            <xsl:if test="not(acl:checkPermission(./@xlink:href,'deletedb'))">
                              <xsl:attribute name="class">last</xsl:attribute>
                            </xsl:if>
                            <a href="{$ServletsBaseURL}MCRAddURNToObjectServlet{$HttpSession}?object={@xlink:href}" onclick="{concat('return confirm(',$apos, i18n:translate('component.mods.metaData.options.urn.confirm'), $apos, ');')}">
                              <xsl:value-of select="i18n:translate('component.mods.metaData.options.urn')" />
                            </a>
                          </li>
                        </xsl:if>
                        <xsl:if test="acl:checkPermission(./@xlink:href,'deletedb') and $derivateWithURN=false()">
                          <li class="last">
                            <a href="{$ServletsBaseURL}derivate/delete{$HttpSession}?id={@xlink:href}" class="confirm_derivate_deletion">
                              <xsl:value-of select="i18n:translate('component.mods.derivate.delDerivate')" />
                            </a>
                          </li>
                        </xsl:if>
                      </ul>
                    </div>
                  </div>
                </xsl:if>
              </div>
            </xsl:for-each>
          </xsl:if>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>
  <xsl:template match="/mycoreobject[contains(@ID,'_mods_')]" mode="objectActions" priority="1">
    <xsl:param name="id" select="./@ID" />
    <xsl:param name="accessedit" select="acl:checkPermission($id,'writedb')" />
    <xsl:param name="accessdelete" select="acl:checkPermission($id,'deletedb')" />
    <xsl:param name="accesscreate" select="acl:checkPermission('create-mods')" />
    <xsl:param name="hasURN" select="'false'" />
    <xsl:param name="displayAddDerivate" select="acl:checkPermission($id,'writedb')" />
    <xsl:param name="layout" select="'$'" />
    <xsl:param name="mods-type" select="'report'" />
    <xsl:variable name="layoutparam">
      <xsl:if test="$layout != '$'">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="editURL">
      <xsl:call-template name="mods.getObjectEditURL">
        <xsl:with-param name="id" select="$id" />
        <xsl:with-param name="layout" select="$layout" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="editURL_allMods">
      <xsl:call-template name="mods.getObjectEditURL">
        <xsl:with-param name="id" select="$id" />
        <xsl:with-param name="layout" select="'all'" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="hasDerivateWithURN">
      <xsl:for-each select="./structure/derobjects/derobject">
        <xsl:if test="mcrurn:hasURNDefined(@xlink:href)">
          <xsl:value-of select="true()" />
        </xsl:if>
      </xsl:for-each>
    </xsl:variable>

    <xsl:if test="$objectHost = 'local'">
      <xsl:choose>
        <xsl:when test="$accessedit or $accessdelete or $accesscreate">
          <xsl:variable name="type" select="substring-before(substring-after($id,'_'),'_')" />
          <xsl:variable name="child-layout">
            <xsl:choose>
              <xsl:when test="$mods-type = 'book'">
                <xsl:value-of select="'chapter'" />
              </xsl:when>
              <xsl:when test="$mods-type = 'confpro'">
                <xsl:value-of select="'confpub'" />
              </xsl:when>
              <xsl:when test="$mods-type = 'journal'">
                <xsl:value-of select="'article'" />
              </xsl:when>
              <xsl:when test="$mods-type = 'series'">
                <xsl:value-of select="'book|confpro'" />
              </xsl:when>
            </xsl:choose>
          </xsl:variable>

          <div class="document_options">
            <!-- TODO: refacture! -->
            <img class="button_options" src="{$WebApplicationBaseURL}images/icon_arrow_circled_red_down.png"
              alt="show document options" title="{i18n:translate('component.mods.metaData.options')}" />
            <div class="options">
              <ul>
                <xsl:if test="$accessedit or $accesscreate">
                  <li>
                    <xsl:if test="not($CurrentUser=$MCR.Users.Superuser.UserName) and
                                  $displayAddDerivate!='true' and
                                  $accessdelete and $hasDerivateWithURN and
                                  string-length($child-layout)=0 and not($accesscreate)">
                      <xsl:attribute name="class">last</xsl:attribute>
                    </xsl:if>
                    <xsl:choose>
                      <xsl:when test="string-length($editURL) &gt; 0">
                        <a href="{$editURL}">
                          <xsl:value-of select="i18n:translate('object.editObject')" />
                        </a>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="i18n:translate('object.locked')" />
                      </xsl:otherwise>
                    </xsl:choose>
                  </li>

                  <xsl:apply-templates select="." mode="externalObjectActions" />

                  <xsl:if test="$displayAddDerivate='true'">
                    <li>
                      <xsl:if test="not($CurrentUser=$MCR.Users.Superuser.UserName) and
                                    $accessdelete and $hasDerivateWithURN and
                                    string-length($child-layout)=0 and not(acl:checkPermission(./@ID,'writedb'))">
                        <xsl:attribute name="class">last</xsl:attribute>
                      </xsl:if>
                      <a href="{$ServletsBaseURL}derivate/create{$HttpSession}?id={$id}">
                        <xsl:value-of select="i18n:translate('derivate.addDerivate')" />
                      </a>
                    </li>
                  </xsl:if>
                </xsl:if>
                <xsl:if test="$accessdelete and string-length($hasDerivateWithURN)=0">
                  <li>
                    <xsl:if test="not($CurrentUser=$MCR.Users.Superuser.UserName) and string-length($child-layout)=0 and not(acl:checkPermission(./@ID,'writedb'))">
                      <xsl:attribute name="class">last</xsl:attribute>
                    </xsl:if>
                    <xsl:choose>
                      <xsl:when test="/mycoreobject/structure/children/child">
                        <xsl:value-of select="i18n:translate('object.hasChildren')" />
                      </xsl:when>
                      <xsl:otherwise>
                        <a href="{$ServletsBaseURL}object/delete{$HttpSession}?id={$id}" id="confirm_deletion">
                          <xsl:value-of select="i18n:translate('object.delObject')" />
                        </a>
                      </xsl:otherwise>
                    </xsl:choose>
                  </li>
                </xsl:if>
                <xsl:if test="string-length($child-layout) &gt; 0 and $accesscreate">
                  <xsl:choose>
                    <xsl:when test="$mods-type = 'series'">
                      <li>
                        <a href="{$ServletsBaseURL}object/create{$HttpSession}?type=mods&amp;layout=book&amp;parentId={./@ID}">
                          <xsl:value-of select="i18n:translate('component.mods.metaData.types.book')" />
                        </a>
                      </li>
                      <li>
                        <xsl:if test="not($CurrentUser=$MCR.Users.Superuser.UserName)">
                          <xsl:attribute name="class">last</xsl:attribute>
                        </xsl:if>
                        <a href="{$ServletsBaseURL}object/create{$HttpSession}?type=mods&amp;layout=confpro&amp;parentId={./@ID}">
                          <xsl:value-of select="i18n:translate('component.mods.metaData.types.confpro')" />
                        </a>
                      </li>
                    </xsl:when>
                    <xsl:otherwise>
                      <li>
                        <xsl:if test="not($CurrentUser=$MCR.Users.Superuser.UserName)">
                          <xsl:attribute name="class">last</xsl:attribute>
                        </xsl:if>
                        <a href="{$ServletsBaseURL}object/create{$HttpSession}?type=mods&amp;layout={$child-layout}&amp;parentId={./@ID}">
                          <xsl:value-of select="i18n:translate(concat('component.mods.metaData.types.',$child-layout))" />
                        </a>
                      </li>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:if>
                <xsl:if test="$CurrentUser=$MCR.Users.Superuser.UserName">
                  <li class="last">
                    <xsl:choose>
                      <xsl:when test="string-length($editURL_allMods) &gt; 0">
                        <a href="{$editURL_allMods}">
                          <xsl:value-of select="i18n:translate('component.mods.object.editAllModsXML')" />
                        </a>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="i18n:translate('object.locked')" />
                      </xsl:otherwise>
                    </xsl:choose>
                  </li>
                </xsl:if>
              </ul>
            </div>
          </div>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>

  <xsl:template name="mods.getObjectEditURL">
    <xsl:param name="id" />
    <xsl:param name="layout" select="'$'" />
    <xsl:param name="collection" select="''" />
    <xsl:choose>
      <xsl:when test="mcrxsl:resourceAvailable('actionmappings.xml')">
        <!-- URL mapping enabled -->
        <xsl:variable name="url">
          <xsl:choose>
            <xsl:when test="string-length($collection) &gt; 0">
              <xsl:choose>
                <xsl:when test="$layout = 'all'">
                  <xsl:value-of select="actionmapping:getURLforCollection('update-xml',$collection,true())" xmlns:actionmapping="xalan://org.mycore.wfc.actionmapping.MCRURLRetriever" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="actionmapping:getURLforCollection('update',$collection,true())" xmlns:actionmapping="xalan://org.mycore.wfc.actionmapping.MCRURLRetriever" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
              <xsl:choose>
                <xsl:when test="$layout = 'all'">
                  <xsl:value-of select="actionmapping:getURLforID('update-xml',$id,true())" xmlns:actionmapping="xalan://org.mycore.wfc.actionmapping.MCRURLRetriever" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="actionmapping:getURLforID('update',$id,true())" xmlns:actionmapping="xalan://org.mycore.wfc.actionmapping.MCRURLRetriever" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="string-length($url)=0" />
          <xsl:otherwise>
            <xsl:variable name="urlWithParam">
              <xsl:call-template name="UrlSetParam">
                <xsl:with-param name="url" select="$url"/>
                <xsl:with-param name="par" select="'id'"/>
                <xsl:with-param name="value" select="$id" />
              </xsl:call-template>
            </xsl:variable>
            <xsl:call-template name="UrlAddSession">
              <xsl:with-param name="url" select="$urlWithParam" />
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
      <!-- URL mapping disabled -->
        <xsl:variable name="layoutSuffix">
          <xsl:if test="$layout != '$'">
            <xsl:value-of select="concat('-',$layout)" />
          </xsl:if>
        </xsl:variable>
        <xsl:variable name="form" select="concat('editor_form_commit-mods',$layoutSuffix,'.xml')" />
        <xsl:value-of select="concat($WebApplicationBaseURL,$form,$HttpSession,'?id=',$id)" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
