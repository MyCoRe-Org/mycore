<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:acl="xalan://org.mycore.access.MCRAccessManager"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:mcrurn="xalan://org.mycore.urn.MCRXMLFunctions"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:websiteWriteProtection="xalan://org.mycore.frontend.MCRWebsiteWriteProtection"
  exclude-result-prefixes="acl i18n xlink mcrxsl mcrurn websiteWriteProtection">
  <xsl:param name="WebApplicationBaseURL" />
  <xsl:param name="ServletsBaseURL" />
  <xsl:param name="RequestURL" />
  <xsl:param name="HttpSession" />
  <xsl:param name="JSessionID" />
  <xsl:param name="CurrentUser" />
  <!-- TODO: remove $objectHost, printMetaDate, derivateLink and maybe others -->
  <xsl:param name="objectHost" select="'local'" />
  <xsl:include href="coreFunctions.xsl" />
  <xsl:include href="xslInclude:components" />
  <!-- website write protected ? -->
  <xsl:variable name="writeProtectedWebsite" select="not(mcrxsl:isCurrentUserGuestUser()) and websiteWriteProtection:isActive()" />
  <xsl:template name="printNotLoggedIn">
    <div class="alert alert-danger">
      <xsl:value-of select="i18n:translate('webpage.notLoggedIn')" disable-output-escaping="yes" />
    </div>
  </xsl:template>
  <xsl:template name="print.writeProtectionMessage">
    <xsl:if test="$writeProtectedWebsite">
      <div class="alert alert-warning alert-dismissable">
        <button type="button" class="close" data-dismiss="alert" aria-hidden="true">
          <xsl:value-of select="'&#215;'" />
        </button>
        <strong>
          <xsl:copy-of select="websiteWriteProtection:getMessage()" />
        </strong>
      </div>
    </xsl:if>
  </xsl:template>

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

  <xsl:template name="printI18N">
    <xsl:param name="nodes" />
    <xsl:param name="next" />
    <xsl:variable name="selectPresentLang">
      <xsl:call-template name="selectPresentLang">
        <xsl:with-param name="nodes" select="$nodes" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="string-length($selectPresentLang)">
        <xsl:for-each select="$nodes[lang($selectPresentLang)]">
          <xsl:if test="position() != 1">
            <xsl:value-of select="$next" disable-output-escaping="yes" />
          </xsl:if>
          <xsl:call-template name="lf2br">
            <xsl:with-param name="string" select="." />
          </xsl:call-template>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="$nodes">
          <xsl:if test="position() != 1">
            <xsl:value-of select="$next" disable-output-escaping="yes" />
          </xsl:if>
          <xsl:call-template name="lf2br">
            <xsl:with-param name="string" select="." />
          </xsl:call-template>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
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
  <!-- ******************************************************** -->
  <!-- * Object Link * -->
  <!-- ******************************************************** -->
  <xsl:template name="objectLink">
    <!-- specify either one of them -->
    <xsl:param name="obj_id" />
    <xsl:param name="mcrobj" />
    <xsl:choose>
      <xsl:when test="$mcrobj">
        <xsl:choose>
          <xsl:when test="$objectHost != 'local' and string-length($objectHost) &gt; 0">
            <!-- REMOTE REQUEST -->
            <xsl:variable name="mcrobj"
              select="document(concat('mcrws:operation=MCRDoRetrieveObject&amp;host=',$objectHost,'&amp;ID=',$obj_id))/mycoreobject" />
            <a href="{$WebApplicationBaseURL}receive/{$obj_id}{$HttpSession}?host={@host}">
              <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
            </a>
          </xsl:when>
          <xsl:otherwise>
            <xsl:variable name="obj_id" select="$mcrobj/@ID" />
            <xsl:choose>
              <xsl:when test="acl:checkPermission($obj_id,'read')">
                <a href="{$WebApplicationBaseURL}receive/{$obj_id}{$HttpSession}">
                  <xsl:attribute name="title"><xsl:apply-templates select="$mcrobj" mode="fulltitle" /></xsl:attribute>
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
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:when test="string-length($obj_id)&gt;0">
        <!-- handle old way which may cause a double parsing of mcrobject: -->
        <xsl:choose>
          <xsl:when test="$objectHost != 'local' and string-length($objectHost) &gt; 0">
            <!-- REMOTE REQUEST -->
            <xsl:variable name="mcrobj"
              select="document(concat('mcrws:operation=MCRDoRetrieveObject&amp;host=',$objectHost,'&amp;ID=',$obj_id))/mycoreobject" />
            <a href="{$WebApplicationBaseURL}receive/{$obj_id}{$HttpSession}?host={@host}">
              <xsl:apply-templates select="$mcrobj" mode="resulttitle" />
            </a>
          </xsl:when>
          <xsl:otherwise>
            <!-- LOCAL REQUEST -->
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
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
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
      <a href="{@xlink:href}">
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

  <xsl:template name="printMetaDate">
    <!-- prints a table row for a given nodeset -->
    <xsl:param name="nodes" />
    <xsl:param name="label" select="local-name($nodes[1])" />

    <xsl:if test="$nodes">
      <tr id="metadata_{local-name($nodes[1])}" class="metadata_{substring-before(substring-after(@ID,'_'),'_')}_{local-name($nodes[1])}">
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
  <!-- * Derivate Link * -->
  <!-- ******************************************************** -->
  <xsl:template name="derivateLink">
    <xsl:param name="staticURL" />

    <xsl:if test="$objectHost != 'local'">
      <a href="{$staticURL}">nur auf original Server</a>
    </xsl:if>
    <xsl:if test="$objectHost = 'local'">
      <xsl:for-each select="derivateLink">
        <xsl:variable select="substring-before(@xlink:href, '/')" name="deriv" />
        <xsl:variable name="derivateWithURN" select="mcrurn:hasURNDefined(@xlink:href)" />
        <xsl:choose>
          <xsl:when test="acl:checkPermissionForReadingDerivate($deriv)">
            <xsl:variable name="firstSupportedFile" select="concat('/', substring-after(@xlink:href, '/'))" />
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
              <tr>
                <xsl:if test="annotation">
                  <xsl:value-of select="annotation" />
                  <br />
                </xsl:if>
              </tr>
              <tr>
                <td valign="top" align="left">
                  <!-- MCR-IView ..start -->
                  <xsl:call-template name="derivateLinkView">
                    <xsl:with-param name="derivateID" select="$deriv" />
                    <xsl:with-param name="file" select="$firstSupportedFile" />
                  </xsl:call-template>
                  <!-- MCR - IView ..end -->
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

</xsl:stylesheet>