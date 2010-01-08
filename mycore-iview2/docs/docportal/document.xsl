<?xml version="1.0" encoding="UTF-8"?>

  <!-- ============================================== -->
  <!-- $Revision: 1.2 $ $Date: 2007-11-12 09:37:15 $ -->
  <!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mcr="http://www.mycore.org/" xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  exclude-result-prefixes="xlink mcr acl i18n xsl">

  <!-- Template for result list hit -->
  <xsl:template match="mcr:hit[contains(@id,'_document_')]">
    <xsl:param name="mcrobj" />
    <xsl:variable name="DESCRIPTION_LENGTH" select="100" />

    <xsl:variable name="host" select="@host" />
    <xsl:variable name="obj_id">
      <xsl:value-of select="@id" />
    </xsl:variable>
    <tr>
      <td class="resultTitle" width="60%">
        <xsl:call-template name="objectLink">
          <xsl:with-param name="mcrobj" select="$mcrobj" />
        </xsl:call-template>
      </td>
      <td class="resultTitle" width="10" />
      <td class="resultTitle">
        <xsl:choose>
          <xsl:when test="$mcrobj/metadata/creatorlinks/creatorlink">

            <xsl:call-template name="personlink">
              <xsl:with-param name="nodes" select="$mcrobj/metadata/creatorlinks/creatorlink" />
              <xsl:with-param name="host" select="$host" />
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:for-each select="$mcrobj/metadata/creators/creator">
              <xsl:if test="position() != 1">
                <br />
              </xsl:if>
              <xsl:value-of select="." />
            </xsl:for-each>
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </tr>
    <tr>
      <td colspan="3" class="description">
        <div class="description">
          <xsl:variable name="description">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="$mcrobj/metadata/descriptions/description" />
            </xsl:call-template>
          </xsl:variable>
          <xsl:call-template name="ShortenText">
            <xsl:with-param name="text" select="$description" />
            <xsl:with-param name="length" select="$DESCRIPTION_LENGTH" />
          </xsl:call-template>
        </div>
        <span>
          <!-- format -->
          <xsl:call-template name="printClass">
            <xsl:with-param name="nodes" select="$mcrobj/metadata/formats/format" />
            <xsl:with-param name="host" select="$host" />
            <xsl:with-param name="next" select="', '" />
          </xsl:call-template>
          

          <!-- type -->
          <xsl:call-template name="printClass">
            <xsl:with-param name="nodes" select="$mcrobj/metadata/types/type" />
            <xsl:with-param name="host" select="$host" />
            <xsl:with-param name="next" select="', '" />
          </xsl:call-template>
          ,

          <xsl:value-of select="$mcrobj/@ID" />
          ,

          <xsl:variable name="date">
            <xsl:call-template name="formatISODate">
              <xsl:with-param name="date" select="$mcrobj/service/servdates/servdate[@type='modifydate']" />
              <xsl:with-param name="format" select="i18n:translate('metaData.date')" />
            </xsl:call-template>
          </xsl:variable>
          <xsl:value-of select="i18n:translate('results.lastChanged',$date)" />
        </span>
        <xsl:apply-templates select="." mode="hitInFiles" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_document_')]" mode="resulttitle" priority="1">
    <xsl:choose>
      <xsl:when test="metadata/titles/title[contains(@type,'main')] and metadata/titles/title[contains(@type,'sub')]">
        <xsl:call-template name="printI18N">
          <xsl:with-param name="nodes" select="metadata/titles/title[contains(@type,'main')]" />
        </xsl:call-template>
        <xsl:value-of select="': '" />
        <xsl:call-template name="printI18N">
          <xsl:with-param name="nodes" select="metadata/titles/title[contains(@type,'sub')]" />
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="metadata/titles">
        <xsl:call-template name="printI18N">
          <xsl:with-param name="nodes" select="metadata/titles/title" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="@label" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="/mycoreobject[contains(@ID,'_document_')]" mode="title" priority="1">
    <xsl:apply-templates select="/mycoreobject/metadata/titles" />
  </xsl:template>
  <xsl:template match="/mycoreobject[contains(@ID,'_document_')]" mode="present" priority="1">
    <xsl:param name="obj_host" select="$objectHost" />
    <xsl:variable name="obj_id">
      <xsl:value-of select="/mycoreobject/@ID" />
    </xsl:variable>
    <xsl:variable name="objectBaseURL">
      <xsl:if test="$objectHost != 'local'">
        <xsl:value-of
          select="concat($hostfile/mcr:hosts/mcr:host[@alias=$objectHost]/@url,$hostfile/mcr:hosts/mcr:host[@alias=$objectHost]/@staticpath)" />
      </xsl:if>
      <xsl:if test="$objectHost = 'local'">
        <xsl:value-of select="concat($WebApplicationBaseURL,'receive/')" />
      </xsl:if>
    </xsl:variable>
    <xsl:variable name="staticURL">
      <xsl:value-of select="concat($objectBaseURL,$obj_id)" />
    </xsl:variable>

    <table id="metaData" cellpadding="0" cellspacing="0">

      <!-- DC 01 *** Subchapter ********************************************* -->
      <xsl:if test="./structure/children">
        <tr>
          <td class="metaname">
            <xsl:value-of select="i18n:translate('metaData.document.title')" />
          </td>
          <td class="metavalue">
            <xsl:apply-templates select="./structure/children" />
          </td>
        </tr>
      </xsl:if>

      <!-- DC 15 *** Identifier **************************************** -->

      <xsl:if test="./metadata/identifiers">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.identifier'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/identifiers/identifier" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 02 *** Creator ******************************************* -->
      <!--    03 *** CreatorLink *************************************** -->

      <xsl:choose>
        <xsl:when test="./metadata/creatorlinks">
          <tr>
            <td valign="top" class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.document.creator'),' :')" />
            </td>
            <td class="metavalue">
              <xsl:call-template name="personlink">
                <xsl:with-param name="nodes" select="./metadata/creatorlinks/creatorlink" />
                <xsl:with-param name="host" select="$obj_host" />
              </xsl:call-template>
            </td>
          </tr>
        </xsl:when>
        <xsl:when test="./metadata/creators">
          <tr>
            <td class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.document.creator'),' :')" />
            </td>
            <td class="metavalue">
              <xsl:call-template name="printI18N">
                <xsl:with-param name="nodes" select="./metadata/creators/creator" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </td>
          </tr>
        </xsl:when>
      </xsl:choose>

      <!-- DC 08 *** Publisher ***************************************** -->
      <!--    09 *** PublisherLink ************************************* -->

      <xsl:if test="./metadata/publishlinks or ./metadata/publishers">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.publisher'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:if test="./metadata/publishlinks">
              <xsl:call-template name="personlink">
                <xsl:with-param name="nodes" select="./metadata/publishlinks/publishlink" />
                <xsl:with-param name="host" select="$obj_host" />
              </xsl:call-template>
            </xsl:if>
            <xsl:if test="./metadata/publishlinks or ./metadata/publishers">
              <br />
            </xsl:if>
            <xsl:if test="./metadata/publishers">
              <xsl:call-template name="printI18N">
                <xsl:with-param name="nodes" select="./metadata/publishers/publisher" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </xsl:if>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 10 *** Contributor *************************************** -->
      <!-- DC 11 *** ContributorLink *********************************** -->

      <xsl:if test="./metadata/contriblinks or ./metadata/contributors">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.contributor'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:if test="./metadata/contriblinks">
              <xsl:call-template name="personlink">
                <xsl:with-param name="nodes" select="./metadata/contriblinks/contriblink" />
                <xsl:with-param name="host" select="$obj_host" />
              </xsl:call-template>
            </xsl:if>
            <xsl:if test="./metadata/contriblinks or ./metadata/contributors">
              <br />
            </xsl:if>
            <xsl:if test="./metadata/contributors">
              <xsl:call-template name="printI18N">
                <xsl:with-param name="nodes" select="./metadata/contributors/contributor" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </xsl:if>
          </td>
        </tr>
      </xsl:if>

      <!--    05 *** Origin ********************************************* -->

      <xsl:if test="./metadata/origins">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.origin'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printClass">
              <xsl:with-param name="nodes" select="./metadata/origins/origin" />
              <xsl:with-param name="host" select="$obj_host" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 12 *** Date ********************************************** -->

      <xsl:if test="./metadata/dates">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.date'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:variable name="selectLang">
              <xsl:choose>
                <xsl:when test="/mcr_result/mycoreobject/metadata/dates/date[lang($CurrentLang)]">
                  <xsl:value-of select="$CurrentLang" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="$DefaultLang" />
                </xsl:otherwise>
              </xsl:choose>
            </xsl:variable>
            <xsl:for-each select="./metadata/dates/date[lang($selectLang)]">
              <xsl:if test="position() != 1">
                ,
              </xsl:if>
              <xsl:choose>
                <xsl:when test="@type = 'create'">
                  <xsl:value-of select="concat(i18n:translate('metaData.createdAt'),' :')" />
                </xsl:when>
                <xsl:when test="@type = 'submit'">
                  <xsl:value-of select="concat(i18n:translate('metaData.submittedAt'),' :')" />
                </xsl:when>
                <xsl:when test="@type = 'accept'">
                  <xsl:value-of select="concat(i18n:translate('metaData.acceptedAt'),' :')" />
                </xsl:when>
                <xsl:when test="@type = 'decide'">
                  <xsl:value-of select="concat(i18n:translate('metaData.decidedAt'),' :')" />
                </xsl:when>
              </xsl:choose>
              <xsl:call-template name="formatISODate">
                <xsl:with-param name="date" select="." />
                <xsl:with-param name="format" select="i18n:translate('metaData.date')" />
              </xsl:call-template>
            </xsl:for-each>
          </td>
        </tr>
      </xsl:if>

      <!-- Derivate *** interne und externe Referenzen ******************* -->

      <xsl:if test="./structure/derobjects">
        <tr>
          <td class="metanone" colspan="2">&#160;</td>
        </tr>
        <tr>
          <td class="metaname" style="vertical-align:top;">
            <xsl:value-of select="concat(i18n:translate('metaData.document.derivate'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:if test="$objectHost != 'local'">
              <a href="{$staticURL}">nur auf original Server</a>
            </xsl:if>
            <xsl:if test="$objectHost = 'local'">
              <xsl:for-each select="./structure/derobjects/derobject">
                <table width="100%" border="0" cellspacing="0" cellpadding="0">
                  <xsl:variable name="deriv" select="@xlink:href" />
                  <!-- MCR IView2 ..start -->
                    <tr>
                      <td class="metanone" colspan="8">
                        <xsl:variable name="supportedMainFile">
                          <xsl:call-template name="iview2.getSupport">
                            <xsl:with-param name="derivID" select="$deriv" />
                          </xsl:call-template>
                        </xsl:variable>
                        <xsl:choose>
                          <xsl:when test="$supportedMainFile != ''">
                            <xsl:call-template name="iview2.init">
                              <xsl:with-param name="groupID" select="$deriv" />
                            </xsl:call-template>
                            <xsl:value-of select="concat(i18n:translate('metaData.document.derivate'),' :')" />
                            <xsl:call-template name="iview2.getThumbnail">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="parent" select="concat('viewerContainer',$deriv)" />
                            </xsl:call-template>
                            <xsl:call-template name="iview2.getChapter">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="parent" select="'viewer'" />
                            </xsl:call-template>
                            <!--<xsl:call-template name="iview2.getZoomBar">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="direction" select="'true'" />
                              <xsl:with-param name="horizontal" select="'true'" />
                              <xsl:with-param name="parent" select="concat('buttonSurface',$deriv)" />
                            </xsl:call-template>-->
                            <xsl:call-template name="iview2.getToolbar">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="optOut" select="'true'" />
                              <xsl:with-param name="create" select="'headerLeft,headerLCBorder,headerCenter,headerCRBorder,headerRight'" />
                            </xsl:call-template>
                            <xsl:call-template name="iview2.getViewer">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="zoomBar" select="'false'" />
                              <xsl:with-param name="chapter" select="'true'" />
                              <xsl:with-param name="cutOut" select="'true'" />
                              <xsl:with-param name="overview" select="'true'" />
                              <xsl:with-param name="style" select="'width:256px; height:256px;'"/>
                            </xsl:call-template>
                            <xsl:call-template name="iview2.start">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="style" select="'default'" />
                              <xsl:with-param name="startFile" select="$supportedMainFile"/>
                            </xsl:call-template>
                          </xsl:when>
                        </xsl:choose>
                      </td>
                    </tr>
                  <!-- MCR IView2 ..end -->
                  <tr>
                    <xsl:variable name="deriv" select="@xlink:href" />
                    <xsl:variable name="derivlink" select="concat('notnull:mcrobject:',$deriv)" />
                    <xsl:variable name="derivate" select="document($derivlink)" />
                    <td align="left" valign="top">
                      <div class="derivateBox">
                        <xsl:apply-templates select="$derivate/mycorederivate/derivate/internals" />
                        <xsl:apply-templates select="$derivate/mycorederivate/derivate/externals" />
                      </div>
                    </td>
                    <xsl:if test="acl:checkPermission($obj_id,'writedb')">
                      <td width="10" />
                      <td width="30" valign="top" align="center">
                        <form method="get">
                          <xsl:attribute name="action">
                          <xsl:value-of select="concat($WebApplicationBaseURL,'servlets/MCRStartEditorServlet',$JSessionID)" />
                        </xsl:attribute>
                          <input name="lang" type="hidden" value="{$CurrentLang}" />
                          <input name="se_mcrid" type="hidden">
                            <xsl:attribute name="value">
                            <xsl:value-of select="@xlink:href" />
                          </xsl:attribute>
                          </input>
                          <input name="te_mcrid" type="hidden">
                            <xsl:attribute name="value">
                            <xsl:value-of select="@xlink:href" />
                          </xsl:attribute>
                          </input>
                          <input name="re_mcrid" type="hidden">
                            <xsl:attribute name="value">
                            <xsl:value-of select="$obj_id" />
                          </xsl:attribute>
                          </input>
                          <xsl:variable name="type">
                            <xsl:copy-of select="substring-before(substring-after($obj_id,'_'),'_')" />
                          </xsl:variable>
                          <input name="type" type="hidden" value="{$type}" />
                          <input name="todo" type="hidden" value="saddfile" />
                          <input type="image" src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{i18n:translate('component.swf.derivate.addFile')}" />
                        </form>
                      </td>
                      <td width="30" valign="top" align="center">
                        <xsl:variable name="type">
                          <xsl:copy-of select="substring-before(substring-after($obj_id,'_'),'_')" />
                        </xsl:variable>
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={@xlink:href}&amp;re_mcrid={$obj_id}&amp;se_mcrid={@xlink:href}&amp;type={$type}&amp;todo=seditder">
                          <img src="{$WebApplicationBaseURL}images/workflow_deredit.gif" title="{i18n:translate('component.swf.derivate.editDerivate')}" />
                        </a>
                      </td>
                      <td width="30" valign="top" align="center">
                        <xsl:variable name="id" select="@xlink:href" />
                        <a
                          href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type=acl&amp;step=commit&amp;todo=seditacl">
                          <img src="{$WebApplicationBaseURL}images/workflow_acleditder.gif" title="{i18n:translate('component.swf.object.editACL')}" />
                        </a>
                      </td>
                      <td width="30" valign="top" align="center">
                        <xsl:if test="acl:checkPermission($obj_id,'deletedb')">
                          <xsl:variable name="type">
                            <xsl:copy-of select="substring-before(substring-after($obj_id,'_'),'_')" />
                          </xsl:variable>
                          <a
                            href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={@xlink:href}&amp;re_mcrid={$obj_id}&amp;se_mcrid={@xlink:href}&amp;type={$type}&amp;todo=sdelder">
                            <img src="{$WebApplicationBaseURL}images/workflow_derdelete.gif" title="{i18n:translate('component.swf.derivate.delDerivate')}" />
                          </a>
                        </xsl:if>
                      </td>
                      <td width="10" />
                    </xsl:if>
                  </tr>
                </table>
              </xsl:for-each>
            </xsl:if>
          </td>
        </tr>
      </xsl:if>

      <!-- Minor MetaData -->
      <tr>
        <td class="metanone" colspan="2">&#160;</td>
      </tr>

      <!-- DC 04 *** Subject ******************************************** -->
      <xsl:if test="./metadata/subjects">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.subject'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printClass">
              <xsl:with-param name="nodes" select="./metadata/subjects/subject" />
              <xsl:with-param name="host" select="$obj_host" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 13 *** Type *********************************************** -->

      <xsl:if test="./metadata/types">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.type'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printClass">
              <xsl:with-param name="nodes" select="./metadata/types/type" />
              <xsl:with-param name="host" select="$obj_host" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 14 *** Format  ******************************************** -->

      <xsl:if test="./metadata/formats">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.format'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printClass">
              <xsl:with-param name="nodes" select="./metadata/formats/format" />
              <xsl:with-param name="host" select="$obj_host" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 06 *** Description *************************************** -->
      <!--    07 *** DescriptionURL ************************************ -->

      <xsl:if test="./metadata/descriptions or ./metadata/descripturls">

        <xsl:if test="./metadata/descriptions/description[contains(@type,'abstract')]">
          <tr>
            <xsl:choose>
              <xsl:when test="position() = 1">
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.document.abstract'),' :')" />
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td valign="top" class="metaname" />
              </xsl:otherwise>
            </xsl:choose>
            <td class="metavalue">
              <xsl:call-template name="printI18N">
                <xsl:with-param name="nodes" select="./metadata/descriptions/description[contains(@type,'abstract')]" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </td>
          </tr>
        </xsl:if>

        <xsl:for-each select="./metadata/descripturls/decripturl[contains(@type,'abstract')]">
          <tr>
            <td valign="top" class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.document.abstract'),' :')" />
            </td>
            <td class="metavalue">
              <xsl:for-each select=".">
                <xsl:if test="position() != 1">
                  ,
                </xsl:if>
                <xsl:variable name="urlhref" select="@xlink:href" />
                <a href="{$urlhref}" target="_blank">
                  <xsl:value-of select="@xlink:title" />
                  &gt;&gt;
                </a>
              </xsl:for-each>
            </td>
          </tr>
        </xsl:for-each>

        <xsl:if test="./metadata/descriptions/description[contains(@type,'description')]">
          <tr>
            <xsl:choose>
              <xsl:when test="position() = 1">
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.document.description'),' :')" />
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td valign="top" class="metaname" />
              </xsl:otherwise>
            </xsl:choose>
            <td class="metavalue">
              <xsl:call-template name="printI18N">
                <xsl:with-param name="nodes" select="./metadata/descriptions/description[contains(@type,'description')]" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </td>
          </tr>
        </xsl:if>

        <xsl:if test="./metadata/descripturls/decripturl[contains(@type,'description')]">
          <tr>
            <td valign="top" class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.document.description'),' :')" />
            </td>
            <td class="metavalue">
              <xsl:call-template name="webLink">
                <xsl:with-param name="nodes" select="./metadata/descripturls/decripturl[contains(@type,'description')]" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </td>
          </tr>
        </xsl:if>

        <xsl:if test="./metadata/descriptions/description[contains(@type,'content')]">
          <tr>
            <xsl:choose>
              <xsl:when test="position() = 1">
                <td valign="top" class="metaname">
                  <xsl:value-of select="concat(i18n:translate('metaData.document.content'),' :')" />
                </td>
              </xsl:when>
              <xsl:otherwise>
                <td valign="top" class="metaname" />
              </xsl:otherwise>
            </xsl:choose>
            <td class="metavalue">
              <xsl:call-template name="printI18N">
                <xsl:with-param name="nodes" select="./metadata/descriptions/description[contains(@type,'content')]" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </td>
          </tr>
        </xsl:if>

        <xsl:if test="./metadata/descripturls/decripturl[contains(@type,'content')]">
          <tr>
            <td valign="top" class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.document.content'),' :')" />
            </td>
            <td class="metavalue">
              <xsl:call-template name="webLink">
                <xsl:with-param name="nodes" select="./metadata/descripturls/decripturl[contains(@type,'content')]" />
                <xsl:with-param name="next" select="'&lt;br /&gt;'" />
              </xsl:call-template>
            </td>
          </tr>
        </xsl:if>

      </xsl:if>

      <!-- DC 19 *** Keywords ****************************************** -->

      <xsl:if test="./metadata/keywords">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.keywords'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/keywords/keyword" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 16 *** Source ******************************************** -->

      <xsl:if test="./metadata/sources">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.source'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/sources/source" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 20 *** Coverages ***************************************** -->

      <xsl:if test="./metadata/coverages">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.coverage'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/coverages/coverage" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 23 *** Relation ****************************************** -->

      <xsl:if test="./metadata/relations">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.relation'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/relations/relation" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- DC 26 *** Rights ******************************************** -->

      <xsl:if test="./metadata/rights">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.rights'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/rights/right" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!--    28 *** Size ********************************************** -->

      <xsl:if test="./metadata/sizes">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.size'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/sizes/size" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!--    34 *** Notes ********************************************* -->

      <xsl:if test="./metadata/notes">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.notes'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/notes/note" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!--    35 *** Citation ****************************************** -->

      <xsl:if test="./metadata/citations">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.citation'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/citations/citation" />
              <xsl:with-param name="next" select="'&lt;br /&gt;'" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!--    31 *** URB(NBN) ****************************************** -->
      <xsl:if test="./metadata/nbns">
        <tr>
          <td valign="top" class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.document.nbn'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="printI18N">
              <xsl:with-param name="nodes" select="./metadata/nbns/nbn" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>

      <!-- Empty line ************************************************** -->

      <tr>
        <td class="metanone" colspan="2">&#160;</td>
      </tr>

      <!-- Created ***************************************************** -->
      <xsl:if test="./service/servdates/servdate[@type='createdate']">
        <tr>
          <td class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.createdAt'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="formatISODate">
              <xsl:with-param name="date" select="./service/servdates/servdate[@type='createdate']" />
              <xsl:with-param name="format" select="i18n:translate('metaData.dateTime')" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>
      <!-- Last Change ************************************************* -->
      <xsl:if test="./service/servdates/servdate[@type='modifydate']">
        <tr>
          <td class="metaname">
            <xsl:value-of select="concat(i18n:translate('metaData.lastChanged'),' :')" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="formatISODate">
              <xsl:with-param name="date" select="./service/servdates/servdate[@type='modifydate']" />
              <xsl:with-param name="format" select="i18n:translate('metaData.dateTime')" />
            </xsl:call-template>
          </td>
        </tr>
      </xsl:if>
      <!-- MyCoRe ID *************************************************** -->
      <tr>
        <td class="metaname">
          <xsl:value-of select="concat(i18n:translate('metaData.ID'),' :')" />
        </td>
        <td class="metavalue">
          <xsl:value-of select="./@ID" />
        </td>
      </tr>
      <!-- Static URL ************************************************** -->
      <tr>
        <td class="metaname">
          <xsl:value-of select="concat(i18n:translate('metaData.staticURL'),' :')" />
        </td>
        <td class="metavalue">
          <a>
            <xsl:attribute name="href">
              <xsl:copy-of select="$staticURL" />
            </xsl:attribute>
            <xsl:copy-of select="$staticURL" />
          </a>
        </td>
      </tr>
      <!-- Editor Buttons ********************************************** -->
      <xsl:variable name="accessnbn">
        <xsl:choose>
          <xsl:when test="./metadata/nbns">
            <xsl:value-of select="false()" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="true()" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <xsl:call-template name="editobject_with_der">
        <xsl:with-param name="id" select="./@ID" />
        <xsl:with-param name="accessnbn" select="$accessnbn" />
      </xsl:call-template>
      <!-- ************************************************************* -->
    </table>
  </xsl:template>

  <!-- Titles from a Document ************************************** -->
  <xsl:template match="titles">
    <xsl:choose>
      <xsl:when test="title[lang($CurrentLang) and @inherited = '0']">
        <xsl:for-each select="title[lang($CurrentLang) and @inherited = '0']">
          <xsl:if test="position() != 1">
            <br />
          </xsl:if>
          <xsl:value-of select="." />
        </xsl:for-each>
      </xsl:when>
      <xsl:when test="title[lang($DefaultLang) and @inherited = '0']">
        <xsl:for-each select="title[lang($DefaultLang) and @inherited = '0']">
          <xsl:if test="position() != 1">
            <br />
          </xsl:if>
          <xsl:value-of select="." />
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="title[@inherited = '0']">
          <xsl:if test="position() != 1">
            <br />
          </xsl:if>
          <xsl:value-of select="." />
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="personlink">
    <xsl:param name="nodes" />
    <xsl:for-each select="$nodes">
      <xsl:if test="position() != 1">
        ,
      </xsl:if>
      <xsl:call-template name="objectLink">
        <xsl:with-param name="obj_id" select="@xlink:href" />
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>