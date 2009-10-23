<?xml version="1.0" encoding="UTF-8"?>

  <!-- ============================================== -->
  <!-- $Revision: 1.61 $ $Date: 2007-12-05 16:11:02 $ -->
  <!-- ============================================== -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:xalan="http://xml.apache.org/xalan" xmlns:mcr="http://www.mycore.org/" xmlns:acl="xalan://org.mycore.access.MCRAccessManager"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:websiteWriteProtection="xalan://org.mycore.frontend.MCRWebsiteWriteProtection"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" exclude-result-prefixes="xlink mcr acl i18n">

  <xsl:include href="coreFunctions.xsl" />
  <xsl:include href="generatePage.xsl" />

  <xsl:param name="DocumentBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="RequestURL" />
  <xsl:param name="CurrentUser" />
  <xsl:param name="CurrentGroups" />
  <xsl:param name="MCRSessionID" />
  <!-- HttpSession is empty if cookies are enabled, else ";jsessionid=<id>" -->
  <xsl:param name="HttpSession" />
  <!-- JSessionID is alway like ";jsessionid=<id>" and good for internal calls -->
  <xsl:param name="JSessionID" />
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="DefaultLang" />
  <xsl:param name="CurrentLang" />
  <xsl:param name="Referer" />
  <xsl:param name="TypeMapping" />
  <xsl:param name="objectHost" select="'local'" />
  <xsl:variable name="hostfile" select="document('webapp:hosts.xml')" />

  <!-- website write protected ? -->
  <xsl:variable name="writeProtectedWebsite">
    <xsl:call-template name="get.writeProtectedWebsite" />
  </xsl:variable>
  <!-- get message, if write protected -->
  <xsl:variable name="writeProtectionMessage">
    <xsl:call-template name="get.writeProtectionMessage" />
  </xsl:variable>

  <xsl:variable name="direction">
    <xsl:choose>
      <xsl:when test="$CurrentLang = 'ar'">
        <xsl:text>rtl</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>ltr</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- ============================================================================================================= -->

  <xsl:template match="/">
    <xsl:call-template name="generatePage" />
  </xsl:template>

  <xsl:template match="/mycoreobject" mode="resulttitle">
    <!-- Overwrite this with either heigher priority or a more specific match -->
    <xsl:value-of select="@ID" />
  </xsl:template>

  <xsl:template name="printMetaDate">
    <!-- prints a table row for a given nodeset -->
    <xsl:param name="nodes" />
    <xsl:param name="label" select="local-name($nodes[1])" />
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
            <xsl:choose>
              <xsl:when test="../@class='MCRMetaClassification'">
                <xsl:call-template name="printClass">
                  <xsl:with-param name="nodes" select="." />
                  <xsl:with-param name="host" select="$objectHost" />
                  <xsl:with-param name="next" select="'&lt;br /&gt;'" />
                </xsl:call-template>
                <xsl:call-template name="printClassInfo">
                  <xsl:with-param name="nodes" select="." />
                  <xsl:with-param name="host" select="$objectHost" />
                  <xsl:with-param name="next" select="'&lt;br /&gt;'" />
                </xsl:call-template>
              </xsl:when>
              <xsl:when test="../@class='MCRMetaISO8601Date'">
                <xsl:variable name="format">
                  <xsl:choose>
                    <xsl:when test="string-length(normalize-space(.))=4">
                      <xsl:value-of select="i18n:translate('metaData.dateYear')" />
                    </xsl:when>
                    <xsl:when test="string-length(normalize-space(.))=7">
                      <xsl:value-of select="i18n:translate('metaData.dateYearMonth')" />
                    </xsl:when>
                    <xsl:when test="string-length(normalize-space(.))=10">
                      <xsl:value-of select="i18n:translate('metaData.dateYearMonthDay')" />
                    </xsl:when>
                    <xsl:otherwise>
                      <xsl:value-of select="i18n:translate('metaData.dateTime')" />
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:variable>
                <xsl:call-template name="formatISODate">
                  <xsl:with-param name="date" select="." />
                  <xsl:with-param name="format" select="$format" />
                </xsl:call-template>
              </xsl:when>
              <xsl:when test="../@class='MCRMetaHistoryDate'">
                <xsl:if test="not(@xml:lang) or @xml:lang=$selectPresentLang">
                  <xsl:call-template name="printHistoryDate">
                    <xsl:with-param name="nodes" select="." />
                    <xsl:with-param name="next" select="', '" />
                  </xsl:call-template>
                </xsl:if>
              </xsl:when>
              <xsl:when test="../@class='MCRMetaLinkID'">
                <xsl:call-template name="objectLink">
                  <xsl:with-param name="obj_id" select="@xlink:href" />
                </xsl:call-template>
              </xsl:when>
              <xsl:when test="@class='MCRMetaDerivateLink'">
                <xsl:call-template name="derivateLink" />
              </xsl:when>
              <xsl:when test="../@class='MCRMetaLink'">
                <xsl:call-template name="webLink">
                  <xsl:with-param name="nodes" select="$nodes" />
                  <xsl:with-param name="next" select="'&lt;br /&gt;'" />
                </xsl:call-template>
              </xsl:when>
              <xsl:otherwise>
                <xsl:if test="not(@xml:lang) or @xml:lang=$selectPresentLang">
                  <xsl:call-template name="printI18N">
                    <xsl:with-param name="nodes" select="." />
                    <xsl:with-param name="host" select="$objectHost" />
                    <xsl:with-param name="next" select="'&lt;br /&gt;'" />
                  </xsl:call-template>
                </xsl:if>
              </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="position()!=last()">
              <br />
            </xsl:if>
          </xsl:for-each>
        </td>
      </tr>
    </xsl:if>
  </xsl:template>

  <!-- ******************************************************** -->
  <!-- * Object Link                                          * -->
  <!-- ******************************************************** -->
  <xsl:template name="objectLink">
    <!-- specify either one of them -->
    <xsl:param name="obj_id" />
    <xsl:param name="mcrobj" />
    <xsl:choose>
      <xsl:when test="$mcrobj">
        <xsl:if test="$objectHost = 'local'">
          <xsl:variable name="obj_id" select="$mcrobj/@ID" />
          <xsl:choose>
            <xsl:when test="acl:checkPermission($obj_id,'read')">
              <a href="{$WebApplicationBaseURL}receive/{$obj_id}{$HttpSession}">
                <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <!-- Build Login URL for LoginServlet -->
              <xsl:variable xmlns:encoder="xalan://java.net.URLEncoder" name="LoginURL"
                select="concat( $ServletsBaseURL, 'MCRLoginServlet',$HttpSession,'?url=', encoder:encode( string( $RequestURL ) ) )" />
              <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
              &#160;
              <a href="{$LoginURL}">
                <img src="{concat($WebApplicationBaseURL,'images/paper_lock.gif')}" />
              </a>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
        <!-- 
          REMOTE REQUEST
        -->
        <xsl:if test="$objectHost != 'local'">
          <xsl:variable name="mcrobj"
            select="document(concat('mcrws:operation=MCRDoRetrieveObject&amp;host=',$objectHost,'&amp;ID=',$obj_id))/mycoreobject" />
          <a href="{$WebApplicationBaseURL}receive/{$obj_id}{$HttpSession}?host={@host}">
            <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
          </a>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <!-- handle old way which may cause a double parsing of mcrobject: -->
        <!-- 
          LOCAL REQUEST
        -->
        <xsl:if test="$objectHost = 'local'">
          <xsl:variable name="mcrobj" select="document(concat('mcrobject:',$obj_id))/mycoreobject" />
          <xsl:choose>
            <xsl:when test="acl:checkPermission($obj_id,'read')">
              <a href="{$WebApplicationBaseURL}receive/{$obj_id}{$HttpSession}">
                <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <!-- Build Login URL for LoginServlet -->
              <xsl:variable xmlns:encoder="xalan://java.net.URLEncoder" name="LoginURL"
                select="concat( $ServletsBaseURL, 'MCRLoginServlet',$HttpSession,'?url=', encoder:encode( string( $RequestURL ) ) )" />
              <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
              &#160;
              <a href="{$LoginURL}">
                <img src="{concat($WebApplicationBaseURL,'images/paper_lock.gif')}" />
              </a>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
        <!-- 
          REMOTE REQUEST
        -->
        <xsl:if test="$objectHost != 'local'">
          <xsl:variable name="mcrobj"
            select="document(concat('mcrws:operation=MCRDoRetrieveObject&amp;host=',$objectHost,'&amp;ID=',$obj_id))/mycoreobject" />
          <a href="{$WebApplicationBaseURL}receive/{$obj_id}{$HttpSession}?host={@host}">
            <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
          </a>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- ******************************************************** -->
  <!-- * Derivate Link                                        * -->
  <!-- ******************************************************** -->
  <xsl:template name="derivateLink">
    <xsl:param name="staticURL" />

    <xsl:if test="$objectHost != 'local'">
      <a href="{$staticURL}">nur auf original Server</a>
    </xsl:if>
    <xsl:if test="$objectHost = 'local'">
      <xsl:for-each select="derivateLink">
        <xsl:variable select="substring-before(@xlink:href, '/')" name="deriv" />
        <xsl:variable name="derivateWithURN" select="mcrxsl:hasURNDefined(@xlink:href)" />
        <xsl:choose>
          <xsl:when test="acl:checkPermissionForReadingDerivate($deriv)">
            <xsl:variable name="firstSupportedFile" select="concat('/', substring-after(@xlink:href, '/'))" />
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <td valign="top" align="left">
                  <!-- MCR IView2 ..start -->
                  <xsl:if test="$jai.available">
                    <tr>
                      <td class="metanone" colspan="8">
                        <xsl:choose>
                          <xsl:when test="$firstSupportedFile != ''">
                            <xsl:call-template name="iview2.init">
                              <xsl:with-param name="groupID" select="$deriv" />
                            </xsl:call-template>
                            <xsl:value-of select="concat(i18n:translate('metaData.document.derivate'),' :')" />
                            <xsl:call-template name="iview2.getThumbnail">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="parent" select="'viewer'" />
                            </xsl:call-template>
                            <xsl:call-template name="iview2.getChapter">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="parent" select="'here'" />
                            </xsl:call-template>
                            <xsl:call-template name="iview2.getZoomBar">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="direction" select="'true'" />
                              <xsl:with-param name="horizontal" select="'true'" />
                              <xsl:with-param name="parent" select="concat('buttonSurface',$deriv)" />
                            </xsl:call-template>
                            <xsl:call-template name="iview2.getToolbar">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="optOut" select="'true'" />
                              <xsl:with-param name="create" select="'headerLeft,headerLCBorder,headerCenter,headerCRBorder,headerRight'" />
                            </xsl:call-template>
                            <xsl:call-template name="iview2.getViewer">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="zoomBar" select="'true'" />
                              <xsl:with-param name="chapter" select="'true'" />
                              <xsl:with-param name="cutOut" select="'true'" />
                              <xsl:with-param name="overview" select="'true'" />
                            </xsl:call-template>
                            <xsl:call-template name="iview2.start">
                              <xsl:with-param name="groupID" select="$deriv" />
                              <xsl:with-param name="style" select="'default'" />
                            </xsl:call-template>
                          </xsl:when>
                        </xsl:choose>
                      </td>
                    </tr>
                  </xsl:if>
                  <!-- MCR IView2 ..end -->
                </td>
              </tr>
            </table>
          </xsl:when>
          <xsl:otherwise>
            <p>
              <!-- Zugriff auf 'Abbildung' gesperrt -->
              <xsl:variable select="substring-before(substring-after(/@ID,'_'),'_')" name="type" />
              <xsl:value-of select="i18n:translate('metaData.derivateLocked',i18n:translate(concat('metaData.',$type,'.[derivates]')))" />
            </p>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </xsl:if>
  </xsl:template>

  <xsl:template name="printClass">
    <xsl:param name="nodes" />
    <xsl:param name="host" select="$objectHost" />
    <xsl:param name="next" select="''" />
    <xsl:for-each select="$nodes">
      <xsl:if test="position() != 1">
        <xsl:value-of select="$next" disable-output-escaping="yes" />
      </xsl:if>
      <xsl:variable name="classlink">
        <xsl:call-template name="ClassCategLink">
          <xsl:with-param name="classid" select="@classid" />
          <xsl:with-param name="categid" select="@categid" />
          <xsl:with-param name="host" select="$host" />
        </xsl:call-template>
      </xsl:variable>
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
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="printClassInfo">
    <xsl:param name="nodes" />
    <xsl:param name="host" />
    <xsl:param name="next" />
    <xsl:for-each select="$nodes">
      <xsl:if test="position() != 1">
        <xsl:value-of select="$next" disable-output-escaping="yes" />
      </xsl:if>
      <xsl:variable name="classlink">
        <xsl:call-template name="ClassCategLink">
          <xsl:with-param name="classid" select="@classid" />
          <xsl:with-param name="categid" select="@categid" />
          <xsl:with-param name="host" select="$host" />
        </xsl:call-template>
      </xsl:variable>
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
        <xsl:for-each select="./label[lang($selectLang) and @description]">
          <xsl:choose>
            <xsl:when test="string-length($categurl) != 0">
              <a href="{$categurl}">
                <xsl:if test="$wcms.useTargets = 'yes'">
                  <xsl:attribute name="target">_blank</xsl:attribute>
                </xsl:if>
                <xsl:value-of select="concat('(',@description,')')" />
              </a>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat('(',@description,')')" />
            </xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </xsl:for-each>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="printI18N">
    <xsl:param name="nodes" />
    <xsl:param name="next" />
    <xsl:variable name="selectPresentLang">
      <xsl:call-template name="selectPresentLang">
        <xsl:with-param name="nodes" select="$nodes" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:for-each select="$nodes[lang($selectPresentLang)]">
      <xsl:if test="position() != 1">
        <xsl:value-of select="$next" disable-output-escaping="yes" />
      </xsl:if>
      <xsl:call-template name="lf2br">
        <xsl:with-param name="string" select="." />
      </xsl:call-template>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="lf2br">
    <xsl:param name="string" />
    <xsl:choose>
      <xsl:when test="contains($string,'&#xA;')">
        <xsl:value-of select="substring-before($string,'&#xA;')" />
        <!-- replace line break character by xhtml tag -->
        <br />
        <xsl:call-template name="lf2br">
          <xsl:with-param name="string" select="substring-after($string,'&#xA;')" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$string" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="webLink">
    <xsl:param name="nodes" />
    <xsl:param name="next" />
    <xsl:for-each select="$nodes">
      <xsl:if test="position() != 1">
        <xsl:value-of select="$next" disable-output-escaping="yes" />
      </xsl:if>
      <xsl:variable name="href" select="@xlink:href" />
      <xsl:variable name="title">
        <xsl:choose>
          <xsl:when test="@xlink:title">
            <xsl:value-of select="@xlink:title" />
          </xsl:when>
          <xsl:when test="@xlink:label">
            <xsl:value-of select="@xlink:label" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="@xlink:href" />
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>
      <a href="{@xlink:href}" target="_blank">
        <xsl:value-of select="$title" />
      </a>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="mailLink">
    <xsl:param name="nodes" />
    <xsl:param name="next" />
    <xsl:variable name="selectLang">
      <xsl:call-template name="selectLang">
        <xsl:with-param name="nodes" select="$nodes" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:for-each select="$nodes[lang($selectLang)]">
      <xsl:if test="position() != 1">
        <xsl:value-of select="$next" disable-output-escaping="yes" />
      </xsl:if>
      <xsl:variable name="email" select="." />
      <a href="mailto:{$email}">
        <xsl:value-of select="$email" />
      </a>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="printHistoryDate">
    <xsl:param name="nodes" />
    <xsl:param name="next" />
    <xsl:variable name="selectLang">
      <xsl:call-template name="selectLang">
        <xsl:with-param name="nodes" select="$nodes" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:for-each select="$nodes[lang($selectLang)]">
      <xsl:if test="position() != 1">
        <xsl:value-of select="$next" disable-output-escaping="yes" />
      </xsl:if>
      <xsl:value-of select="text" />
      <xsl:text> (</xsl:text>
      <xsl:value-of select="von" />
      <xsl:text> - </xsl:text>
      <xsl:value-of select="bis" />
      <xsl:text> )</xsl:text>
    </xsl:for-each>
  </xsl:template>

  <!-- Person name form LegalEntity ******************************** -->
  <xsl:template match="names">
    <xsl:variable name="name" select="./name[1]" />
    <xsl:choose>
      <xsl:when test="$name/fullname">
        <xsl:value-of select="$name/fullname" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$name/academic" />
        <xsl:text></xsl:text>
        <xsl:value-of select="$name/peerage" />
        <xsl:text></xsl:text>
        <xsl:value-of select="$name/callname" />
        <xsl:text></xsl:text>
        <xsl:value-of select="$name/prefix" />
        <xsl:text></xsl:text>
        <xsl:value-of select="$name/surname" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template match="printlatestobjects">
    <xsl:call-template name="printLatestObjects">
      <xsl:with-param name="objectType" select="@objecttype" />
      <xsl:with-param name="sortField" select="@sortfield" />
      <xsl:with-param name="mCRQLConditions" select="@mcrqlcond" />
      <xsl:with-param name="maxResults" select="@maxresults" />
      <xsl:with-param name="overwriteLayout" select="@overwritelayout" />
    </xsl:call-template>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template name="printLatestObjects">
    <xsl:param name="objectType" />
    <xsl:param name="sortField" />
    <xsl:param name="mCRQLConditions" />
    <xsl:param name="maxResults" />
    <xsl:param name="overwriteLayout" />
    <!-- build query term -->
    <xsl:variable name="objType" xmlns:encoder="xalan://java.net.URLEncoder">
      <xsl:value-of select="encoder:encode(concat('(objectType = ',$objectType,')') )" />
    </xsl:variable>
    <xsl:variable name="mCRQLConditions_encoded" xmlns:encoder="xalan://java.net.URLEncoder">
      <xsl:choose>
        <xsl:when test="$mCRQLConditions!=''">
          <xsl:value-of select="encoder:encode( concat(' and (',$mCRQLConditions,')') ) " />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="''" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="term">
      <xsl:value-of select="concat($objType,$mCRQLConditions_encoded)" />
    </xsl:variable>
    <xsl:variable name="queryURI">
      <xsl:value-of select="concat('query:term=',$term,'&amp;sortby=',$sortField,
            '&amp;order=descending&amp;maxResults=',$maxResults)" />
    </xsl:variable>
    <!-- do layout -->
    <xsl:choose>
      <xsl:when test="$overwriteLayout='true'">
        <xsl:for-each select="xalan:nodeset(document($queryURI))/mcr:results/mcr:hit">
          <xsl:variable name="mcrobj" select="document(concat('mcrobject:',@id))/mycoreobject" />
          <xsl:apply-templates select="." mode="latestObjects">
            <xsl:with-param name="mcrobj" select="$mcrobj" />
          </xsl:apply-templates>
        </xsl:for-each>
        <xsl:call-template name="printLatestObjects.all">
          <xsl:with-param name="query2" select="$term" />
          <xsl:with-param name="sortBy" select="$sortField" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <table id="resultList" cellpadding="0" cellspacing="0" xmlns:mcr="http://www.mycore.org/">
          <xsl:for-each select="xalan:nodeset(document($queryURI))/mcr:results/mcr:hit">
            <xsl:variable name="mcrobj" select="document(concat('mcrobject:',@id))/mycoreobject" />
            <xsl:apply-templates select=".">
              <xsl:with-param name="mcrobj" select="$mcrobj" />
            </xsl:apply-templates>
          </xsl:for-each>
          <tr>
            <td colspan="3" align="right">
              <xsl:call-template name="printLatestObjects.all">
                <xsl:with-param name="query2" select="$term" />
                <xsl:with-param name="sortBy" select="$sortField" />
              </xsl:call-template>
            </td>
          </tr>
        </table>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template name="printLatestObjects.all">
    <xsl:param name="query2" />
    <xsl:param name="sortBy" />
    <a
      href="{$ServletsBaseURL}MCRSearchServlet{$HttpSession}?query={$query2}&amp;{$sortBy}.sortField=descending&amp;numPerPage=10&amp;maxResults=0">
      <xsl:value-of select="i18n:translate('latestObjects.more')" />
    </a>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template name="printNotLoggedIn">
    <xsl:value-of select="i18n:translate('webpage.notLoggedIn')" />
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template name="userInfo">
    <xsl:value-of select="concat(i18n:translate('users.user'),': ')" />
    <xsl:choose>
      <xsl:when test="$CurrentUser='gast'">
        <xsl:value-of select="i18n:translate('users.error.notLoggedIn')" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:variable xmlns:encoder="xalan://java.net.URLEncoder" name="URL"
          select="concat( $ServletsBaseURL, 'MCRUserServlet',$HttpSession,'?mode=Select&amp;url=', encoder:encode( string( $RequestURL ) ) )" />
        <a href="{$URL}">
          <xsl:value-of select="$CurrentUser" />
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template name="get.writeProtectionMessage">
    <xsl:if test="$writeProtectedWebsite='true'">
      <xsl:copy-of select="websiteWriteProtection:getMessage()" />
    </xsl:if>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template name="print.writeProtectionMessage">
    <xsl:if test="$writeProtectedWebsite='true' and not(/website-ReadOnly)">
      <p style="color:#FF0000;">
        <b>
          <xsl:copy-of select="$writeProtectionMessage" />
        </b>
      </p>
    </xsl:if>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <xsl:template name="get.writeProtectedWebsite">
    <xsl:choose>
      <xsl:when test="$CurrentUser!='gast'">
        <xsl:value-of select="websiteWriteProtection:isActive()" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="false()" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- ============================================================================================================= -->
  <!-- The template write the icon line to edit an object with derivate -->
  <xsl:template name="editobject_with_der">
    <xsl:param name="accessnbn" />
    <xsl:param name="accessedit" />
    <xsl:param name="accessdelete" />
    <xsl:param name="id" />
    <xsl:param name="layout" select="'$'" />
    <xsl:variable name="layoutparam">
      <xsl:if test="$layout != '$'">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:if test="$objectHost = 'local'">
      <xsl:choose>
        <xsl:when test="acl:checkPermission($id,'writedb') or acl:checkPermission($id,'deletedb')">
          <xsl:variable name="type" select="substring-before(substring-after($id,'_'),'_')" />
          <tr>
            <td class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.edit'),' :')" />
            </td>
            <td class="metavalue">
              <xsl:if test="acl:checkPermission($id,'writedb')">
                <a
                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type={$type}{$layoutparam}&amp;step=commit&amp;todo=seditobj">
                  <img src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('component.swf.object.editObject')}" />
                </a>
                <a
                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type=acl&amp;step=commit&amp;todo=seditacl">
                  <img src="{$WebApplicationBaseURL}images/workflow_acledit.gif" title="{i18n:translate('component.swf.object.editACL')}" />
                </a>
                <xsl:if test="$accessnbn = 'true'">
                  <a
                    href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type={$type}{$layoutparam}&amp;step=commit&amp;todo=saddnbn">
                    <img src="{$WebApplicationBaseURL}images/workflow_addnbn.gif" title="{i18n:translate('component.swf.object.addNBN')}" />
                  </a>
                </xsl:if>
                <a
                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type={$type}&amp;step=commit&amp;todo=snewder">
                  <img src="{$WebApplicationBaseURL}images/workflow_deradd.gif" title="{i18n:translate('component.swf.derivate.addDerivate')}" />
                </a>
              </xsl:if>
              <xsl:if test="acl:checkPermission($id,'deletedb')">
                <a
                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type={$type}&amp;step=commit&amp;todo=sdelobj">
                  <img src="{$WebApplicationBaseURL}images/workflow_objdelete.gif" title="{i18n:translate('component.swf.object.delObject')}" />
                </a>
              </xsl:if>
            </td>
          </tr>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>
  <!-- The template write the icon line to edit an object -->
  <xsl:template name="editobject">
    <xsl:param name="accessedit" />
    <xsl:param name="accessdelete" />
    <xsl:param name="id" />
    <xsl:param name="layout" select="'$'" />
    <xsl:variable name="layoutparam">
      <xsl:if test="$layout != '$'">
        <xsl:value-of select="concat('&amp;layout=',$layout)" />
      </xsl:if>
    </xsl:variable>
    <xsl:if test="$objectHost = 'local'">
      <xsl:choose>
        <xsl:when test="acl:checkPermission($id,'writedb') or acl:checkPermission($id,'deletedb')">
          <xsl:variable name="type" select="substring-before(substring-after($id,'_'),'_')" />
          <tr>
            <td class="metaname">
              <xsl:value-of select="concat(i18n:translate('metaData.edit'),' :')" />
            </td>
            <td class="metavalue">
              <xsl:if test="acl:checkPermission($id,'writedb')">
                <a
                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type={$type}{$layoutparam}&amp;step=commit&amp;todo=seditobj">
                  <img src="{$WebApplicationBaseURL}images/workflow_objedit.gif" title="{i18n:translate('component.swf.object.editObject')}" />
                </a>
                <a
                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type=acl&amp;step=commit&amp;todo=seditacl">
                  <img src="{$WebApplicationBaseURL}images/workflow_acledit.gif" title="{i18n:translate('component.swf.object.editACL')}" />
                </a>
              </xsl:if>
              <xsl:if test="acl:checkPermission($id,'deletedb')">
                <a
                  href="{$ServletsBaseURL}MCRStartEditorServlet{$HttpSession}?tf_mcrid={$id}&amp;re_mcrid={$id}&amp;se_mcrid={$id}&amp;type={$type}&amp;step=commit&amp;todo=sdelobj">
                  <img src="{$WebApplicationBaseURL}images/workflow_objdelete.gif" title="{i18n:translate('component.swf.object.delObject')}" />
                </a>
              </xsl:if>
            </td>
          </tr>
        </xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>

  <!-- template that matches if not files where hit by query
       fixed bug #2545125
   -->
  <xsl:template match="mcr:hit" mode="hitInFiles" priority="0" />

</xsl:stylesheet>