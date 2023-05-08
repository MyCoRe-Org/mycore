<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    exclude-result-prefixes="xsl">

    <xsl:output method="html" encoding="UTF-8" media-type="text/html" indent="yes"/>

    <xsl:param name="CurrentLang"/>
    <xsl:param name="WebApplicationBaseURL"/>

    <xsl:template match="/">
        <html lang="{$CurrentLang}">
            <head>
                <script type="text/javascript" src="{$WebApplicationBaseURL}modules/orcid2/js/orcid-popup.js"></script>
            </head>
            <body>
                <main>
                    <article>
                        <xsl:apply-templates/>
                    </article>
                </main>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="ORCIDOAuthAccessTokenResponse">
        <img src="{$WebApplicationBaseURL}modules/orcid2/images/orcid_member.svg" alt="ORCID logo"/>
        <h2>
            <span><xsl:value-of select="concat(' ', document('i18n:component.orcid2.oauth.message.thanks'), ' ')"/></span>
            <img style="height:.8em;vertical-align:baseline" src="{$WebApplicationBaseURL}modules/orcid2/images/orcid_icon.svg" alt="ORCID logo"/>
        </h2>
        <p><xsl:value-of select="document('i18n:component.orcid2.oauth.message.confirmation')"/></p>
        <ul>
            <xsl:if test="contains(scope/text(), '/authenticate')">
                <li><xsl:value-of select="document('i18n:component.orcid2.oauth.message.authenticate')"/></li>
            </xsl:if>
            <xsl:if test="contains(scope/text(), '/read-limited')">
                <li><xsl:value-of select="document('i18n:component.orcid2.oauth.message.read-limited')"/></li>
            </xsl:if>
            <xsl:if test="contains(scope/text(), '/activities/update')">
                <li><xsl:value-of select="document('i18n:component.orcid2.oauth.message.activities_update')"/></li>
            </xsl:if>
            <xsl:if test="contains(scope/text(), '/person/update')">
                <li><xsl:value-of select="document('i18n:component.orcid2.oauth.message.person_update')"/></li>
            </xsl:if>
        </ul>
        <p><xsl:value-of select="document('i18n:component.orcid2.oauth.message.revoke.pt1')"/>
            <a href="https://sandbox.orcid.org/trusted-parties" target="_blank">
                <xsl:value-of select="document('i18n:component.orcid2.oauth.message.profile')"/>
                <img src="{$WebApplicationBaseURL}modules/orcid2/images/orcid_icon.svg" style="height:.8em;vertical-align:baseline" alt="ORCID logo"/>
            </a>
            <xsl:value-of select="concat(document('i18n:component.orcid2.oauth.message.revoke.pt2'), '.')"/>
        </p>
        <button onclick="window.close()">
            <xsl:value-of select="document('i18n:component.orcid2.oauth.button.window.close')"/>
        </button>
    </xsl:template>

    <xsl:template match="ORCIDOAuthErrorResponse">
        <img src="{$WebApplicationBaseURL}modules/orcid2/images/orcid_logo.svg" alt="ORCID logo"/>
        <h2>
            <span><xsl:value-of select="concat(' ', document('i18n:component.orcid2.oauth.message.rejected'))"/></span>
        </h2>
        <xsl:choose>
            <xsl:when test="error/text()='access_denied'">
                <p><xsl:value-of select="concat(document('i18n:component.orcid2.oauth.message.access_denied'), '.')"/></p>
            </xsl:when>
            <xsl:otherwise>
                <p><xsl:value-of select="concat(document('i18n:component.orcid2.oauth.message.error'), ':')"/></p>
                <pre><xsl:value-of select="concat(error/text(), ': ', errorDescription/text())"/></pre>
            </xsl:otherwise>
        </xsl:choose>
        <button onclick="window.close()">
            <xsl:value-of select="document('i18n:component.orcid2.oauth.button.window.close')"/>
        </button>
    </xsl:template>

</xsl:stylesheet>
