<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
        <!ENTITY html-output SYSTEM "xsl/xsl-output-html.fragment">
        ]>

<!-- ============================================== -->
<!-- $Revision: 1.4 $ $Date: 2009/03/20 10:42:33 $ -->
<!-- ============================================== -->

<!-- +
| This stylesheet controls the Web-Layout of the Login Servlet. The Login Servlet
| gathers information about the session, user ID, password and calling URL and
| then tries to login the user by delegating the login request to the user manager.
| Depending on whether the login was successful or not, the Login Servlet generates
| the following XML output stream:
|
| <mcr_user unknown_user="true|false"
|           user_disabled="true|false"
|           invalid_password="true|false">
|   <guest_id>...</guest_id>
|   <guest_pwd>...</guest_pwd>
|   <backto_url>...<backto_url>
| </mcr_user>
|
| The XML stream is sent to the Layout Servlet and finally handled by this stylesheet.
|
| Authors: Detlev Degenhardt, Thomas Scheffler, Kathleen Neumann
| Last changes: 2012-03-16
+ -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
                exclude-result-prefixes="xlink mcri18n"
>
    &html-output;

    <xsl:include href="MyCoReLayout-3.xsl" />

    <xsl:param name="FormTarget" select="concat($ServletsBaseURL,'MCRLoginServlet')" />

    <xsl:variable name="PageTitle" select="mcri18n:translate('component.user2.login.form.title')" />

    <xsl:template match="/login">
        <!-- +
        | There are three possible error-conditions: wrong password, unknown user and disabled
        | user. If one of these conditions occured, the corresponding information will be
        | presented at the top of the page.
        + -->
        <xsl:apply-templates select="." mode="userStatus" />
        <xsl:apply-templates select="." mode="userAction" />
    </xsl:template>

    <xsl:template match="login" mode="userAction">
        <xsl:apply-templates select="form" />
    </xsl:template>

    <xsl:template match="form">
        <form action="{@action}{$HttpSession}" method="post" role="form" class="form-login">
            <h2 class="form-login-heading">
                <xsl:value-of select="mcri18n:translate('component.user2.login.heading')" />
            </h2>
            <fieldset>
                <!-- Here come the input fields... -->
                <xsl:apply-templates select="input" />
            </fieldset>
            <div class="form-actions">
                <xsl:choose>
                    <xsl:when test="$direction = 'rtl' ">
                        <button class="btn btn-secondary" type="button" onClick="self.location.href='{../returnURL}'" tabindex="999">
                            <xsl:value-of select="mcri18n:translate('component.user2.button.cancel')" />
                        </button>
                        <button class="btn btn-primary ml-3" type="submit" name="LoginSubmit">
                            <xsl:value-of select="mcri18n:translate('component.user2.button.login')" />
                        </button>
                    </xsl:when>
                    <xsl:otherwise>
                        <button class="btn btn-primary" type="submit" name="LoginSubmit">
                            <xsl:value-of select="mcri18n:translate('component.user2.button.login')" />
                        </button>
                        <button class="btn btn-secondary ml-3" type="button" onClick="self.location.href='{../returnURL}'" tabindex="999">
                            <xsl:value-of select="mcri18n:translate('component.user2.button.cancel')" />
                        </button>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </form>
    </xsl:template>

    <xsl:template match="input">
        <xsl:choose>
            <xsl:when test="@isHidden='true'">
                <input type="hidden" name="{@name}" value="{@value}" />
            </xsl:when>
            <xsl:otherwise>
                <div>
                    <xsl:apply-templates select="." mode="controlGroupClass" />
                    <label class="control-label" for="{@name}">
                        <xsl:value-of select="concat(@label,' :')" />
                    </label>
                    <div class="controls">
                        <xsl:variable name="type">
                            <xsl:choose>
                                <xsl:when test="@isPassword='true'">
                                    <xsl:value-of select="'password'" />
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="'text'" />
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:variable>
                        <input type="{$type}" name="{@name}" class="form-control input-large" placeholder="{@placeholder}" title="{@label}" autocorrect="off"
                               autocapitalize="off" />
                    </div>
                </div>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="login" mode="userStatus">
        <xsl:if test="@loginFailed='true'">
            <div class="alert alert-danger" role="alert">
                <strong>
                    <xsl:value-of select="mcri18n:translate('component.user2.login.failed')" />
                </strong>
                <xsl:choose>
                    <xsl:when test="errorMessage">
                        <br />
                        <xsl:value-of select="mcri18n:translate-with-params('component.user2.login.failed.reason', errorMessage)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat(' ',mcri18n:translate('component.user2.login.invalidUserPwd'))" />
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </xsl:if>
    </xsl:template>

    <xsl:template match="input" mode="controlGroupClass">
        <xsl:attribute name="class">
            <xsl:value-of select="'form-group'" />
            <xsl:if test="@loginFailed='true'">
                <xsl:value-of select="' mcr-invalid'" />
            </xsl:if>
        </xsl:attribute>
    </xsl:template>

</xsl:stylesheet>
