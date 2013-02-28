<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- XSL to search for users and display the list users found -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xsl xalan i18n encoder">

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:variable name="PageID" select="'select-user'" />

  <xsl:variable name="PageTitle" select="'Nutzer auswählen'" />

<!-- ========== Subselect Parameter ========== -->
  <xsl:param name="subselect.session" />
  <xsl:param name="subselect.varpath" />
  <xsl:param name="subselect.webpage" />
  <xsl:param name="MCR.Ajax.LoadingImage" />

  <xsl:template match="/users" mode="headAdditional" />

  <xsl:template match="/users">
    <div class="user-list-users">
      <xsl:if test="@num">
        <div class="section">
          <form action="{$ServletsBaseURL}MCRUserServlet" onsubmit="document.getElementById('indicator').style.display='inline';">
            <p>
              Suche nach Nutzerkennung oder Name:
              <input type="text" name="search" value="{@search}" />
              <img id="indicator" style="display:none" src="{$WebApplicationBaseURL}{$MCR.Ajax.LoadingImage}" />
            </p>
          </form>
          <xsl:apply-templates select="." mode="headAdditional" />
        </div>
      </xsl:if>
      <div class="section" id="sectionlast">
        <xsl:choose>
          <xsl:when test="user">
            <table class="user">
              <caption>
                <xsl:value-of select="count(user)" />
                <xsl:text> Nutzer gefunden:</xsl:text>
              </caption>
              <tr>
                <th scope="col">Nutzerkennung:</th>
                <xsl:choose>
                  <xsl:when test="@num">
                    <th scope="col">Herkunft:</th>
                    <th scope="col">Name:</th>
                    <th scope="col">E-Mail:</th>
                  </xsl:when>
                  <xsl:otherwise>
                    <th scope="col">Bezeichnung:</th>
                  </xsl:otherwise>
                </xsl:choose>
              </tr>
              <xsl:apply-templates select="user" />
            </table>
          </xsl:when>
          <xsl:when test="string-length(@num) = 0">
            <p>
              Sie besitzen keine Lesenutzer.
            </p>
          </xsl:when>
          <xsl:when test="@num = 0">
            <p>
              Keine Nutzer gefunden.
              Bitte passen Sie Ihre Suchkriterien an.
            </p>
          </xsl:when>
          <xsl:when test="number(@num) &gt; number(@max)">
            <p>
              Zu viele (
              <xsl:value-of select="@num" />
              ) Nutzer, um alle anzuzeigen.
              Bitte passen Sie Ihre Suchkriterien an.
              Es werden maximal
              <xsl:value-of select="@max" />
              Nutzer angezeigt.
            </p>
          </xsl:when>
        </xsl:choose>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="user">
    <tr>
      <xsl:attribute name="class">
      <xsl:choose>
        <xsl:when test="position() mod 2">background1</xsl:when>  
        <xsl:otherwise>background2</xsl:otherwise>  
      </xsl:choose>
    </xsl:attribute>
      <td>
        <xsl:apply-templates select="." mode="link" />
      </td>
      <xsl:if test="/users/@num">
        <td>
          <xsl:value-of select="@realm" />
        </td>
      </xsl:if>
      <td>
        <xsl:value-of select="realName" />
      </td>
      <xsl:if test="/users/@num">
        <td>
          <xsl:if test="eMail">
            <a href="mailto:{eMail}">
              <xsl:value-of select="eMail" />
            </a>
          </xsl:if>
        </td>
      </xsl:if>
    </tr>
  </xsl:template>

  <xsl:template match="user" mode="link">
    <a>
      <xsl:attribute name="href">
        <xsl:variable name="uid">
          <xsl:value-of select="@name" />
          <xsl:if test="not ( @realm = 'local' )">
            <xsl:text>@</xsl:text>
            <xsl:value-of select="@realm" />
          </xsl:if>
        </xsl:variable>
        <xsl:value-of select="concat('MCRUserServlet?action=show&amp;id=',$uid)" />
      </xsl:attribute>
      <xsl:value-of select="@name" />
    </a>
  </xsl:template>

</xsl:stylesheet>
