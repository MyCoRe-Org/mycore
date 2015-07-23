<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to display login options as defined in realms.xml -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:param name="MCR.NameOfProject" select="'MyCoRe'" />

  <xsl:variable name="PageTitle" select="i18n:translate('component.user2.login.realms')" />
  <xsl:variable name="PageID" select="'login'" />

  <xsl:template match="realms">
    <div class="user-realms">
      <div class="section">
        <p>
          <xsl:variable name="currentAccount">
            <xsl:value-of select="'&lt;strong&gt;'" />
            <xsl:choose>
              <xsl:when test="@guest='true'">
                <xsl:value-of select="i18n:translate('component.user2.login.guest')" />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="concat(@user,' [',@realm,']')" />
              </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="'&lt;/strong&gt;'" />
          </xsl:variable>
          <xsl:value-of select="i18n:translate('component.user2.login.currentAccount', $currentAccount)" disable-output-escaping="yes" />
        </p>
      </div>
      <div class="section" id="sectionlast">
        <p>
          <strong>
            <xsl:value-of select="i18n:translate('component.user2.login.select')" />
          </strong>
          <br />
          <xsl:if test="@guest != 'true'">
            <dl class="realms">
              <dd>
                <ul>
                  <li>
                    <a href="{$ServletsBaseURL}logout">
                      <xsl:value-of select="i18n:translate('component.user2.login.logout')" />
                    </a>
                    <div>
                      <xsl:value-of select="i18n:translate('component.user2.login.openAccess')" />
                    </div>
                  </li>
                </ul>
              </dd>
            </dl>
          </xsl:if>
          <dl class="realms">
            <xsl:apply-templates select="realm[string-length(login/@url) &gt; 0]" />
          </dl>
        </p>
        <p>
          <form method="get" action="{$ServletsBaseURL}MCRLoginServlet" class="action">
            <input value="cancel" name="action" type="hidden" />
            <input value="{i18n:translate('component.user2.button.cancel')}" class="btn btn-default" type="submit" />
          </form>
        </p>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="*[label]" mode="printLabel">
    <xsl:variable name="lang">
      <xsl:call-template name="selectPresentLang">
        <xsl:with-param name="nodes" select="label" />
      </xsl:call-template>
    </xsl:variable>
    <xsl:value-of select="label[lang($lang)]" disable-output-escaping="yes" />
  </xsl:template>

  <xsl:template match="realm">
    <dt>
      <xsl:apply-templates select="." mode="printLabel" />
    </dt>
    <dd>
      <ul>
        <xsl:apply-templates select="login" />
        <xsl:apply-templates select="create" />
      </ul>
    </dd>
  </xsl:template>

  <xsl:template match="login|create">
    <li>
      <a href="{@url}">
        <xsl:apply-templates select="." mode="printLabel" />
      </a>
      <xsl:apply-templates select="info" />
    </li>
  </xsl:template>

  <xsl:template match="info">
    <div>
      <xsl:apply-templates select="." mode="printLabel" />
    </div>
  </xsl:template>

</xsl:stylesheet>
