<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
  xmlns:mcracl="http://www.mycore.de/xslt/acl"
  xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
  xmlns:mcrobject="http://www.mycore.de/xslt/object"
  xmlns:mcrurl="http://www.mycore.de/xslt/url"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  exclude-result-prefixes="#all">

  <xsl:output
    method="html"
    doctype-system="about:legacy-compat"
    indent="yes"
    omit-xml-declaration="yes"
    media-type="text/html"
    version="5" />

  <xsl:include href="resource:xslt/default-parameters.xsl" />
  <xsl:include href="xslInclude:functions" />

  <xsl:template match="*[@ID]">
    <xsl:if test="mcracl:check-permission(@ID,'view-history')">
      <xsl:variable name="version-info" select="mcrobject:get-version-info(@ID)" />

      <ol class="versioninfo">
        <xsl:for-each select="reverse($version-info/versions/version)">
          <li>
            <xsl:if test="@r">
              <span class="rev">
                <xsl:choose>
                  <xsl:when test="@action='D'">
                    <xsl:value-of select="@r" />
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:variable name="href" select="
                      mcrurl:set-param(mcrurl:del-param($RequestURL, 'XSL.Style'), 'r', @r)
                    " />
                    <a href="{$href}">
                      <xsl:value-of select="@r" />
                    </a>
                  </xsl:otherwise>
                </xsl:choose>
              </span>
              <xsl:text> </xsl:text>
            </xsl:if>
            <xsl:if test="@action">
              <span class="action">
                <xsl:value-of select="mcri18n:translate(concat('metaData.versions.action.', @action))" />
              </span>
              <xsl:text> </xsl:text>
            </xsl:if>
            <span class="date">
              <xsl:value-of select="format-dateTime(@date, mcri18n:translate('metaData.dateTime.xsl3'))" />
            </span>
            <xsl:text> </xsl:text>
            <xsl:if test="@user">
              <span class="user">
                <xsl:value-of select="@user" />
              </span>
            </xsl:if>
          </li>
        </xsl:for-each>
      </ol>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
