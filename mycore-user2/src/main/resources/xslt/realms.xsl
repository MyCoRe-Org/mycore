<?xml version="1.0" encoding="UTF-8"?>
<!-- XSL to display login options as defined in realms.xml -->
<xsl:stylesheet version="3.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  exclude-result-prefixes="#all">

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:variable name="PageTitle" select="mcri18n:translate('component.user2.login.realms')" />

  <xsl:template match="realms">
    <div class="user-realms">
      <div class="section">
        <p>
          <xsl:variable name="currentAccount">
            <strong>
              <xsl:choose>
                <xsl:when test="@guest='true'">
                  <xsl:value-of select="mcri18n:translate('component.user2.login.guest')" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="concat(@user,' [',@realm,']')" />
                </xsl:otherwise>
              </xsl:choose>
            </strong>
          </xsl:variable>
          <xsl:copy-of
            select="parse-xml-fragment(mcri18n:translate-with-params('component.user2.login.currentAccount', serialize($currentAccount)))/node()" />
        </p>
      </div>
      <div class="section" id="sectionlast">
        <p>
          <strong>
            <xsl:value-of select="mcri18n:translate('component.user2.login.select')" />
          </strong>
          <br />
          <xsl:if test="@guest != 'true'">
            <dl class="realms">
              <dd>
                <ul>
                  <li>
                    <a href="{$ServletsBaseURL}logout">
                      <xsl:value-of select="mcri18n:translate('component.user2.login.logout')" />
                    </a>
                    <div>
                      <xsl:value-of select="mcri18n:translate('component.user2.login.openAccess')" />
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
            <input value="{mcri18n:translate('component.user2.button.cancel')}" class="btn btn-primary" type="submit" />
          </form>
        </p>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="*[label]" mode="printLabel">
    <xsl:variable name="lang" select="mcri18n:select-present-lang(label)" />
    <xsl:variable name="selectedLabel" select="label[lang($lang)][1]" />
    <xsl:choose>
      <xsl:when test="$selectedLabel/*">
        <xsl:copy-of select="$selectedLabel/node()" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:try>
          <xsl:copy-of select="parse-xml-fragment(string($selectedLabel))/node()" />
          <xsl:catch>
            <xsl:value-of select="$selectedLabel" />
          </xsl:catch>
        </xsl:try>
      </xsl:otherwise>
    </xsl:choose>
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
