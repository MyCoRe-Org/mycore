<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- XSL to display data of a login user -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" exclude-result-prefixes="xsl xalan i18n">

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:variable name="PageID" select="'show-user'" />

  <xsl:variable name="PageTitle" select="concat(i18n:translate('component.user2.admin.userDisplay'),/user/@name)" />

  <xsl:param name="step" />

  <xsl:variable name="uid">
    <xsl:value-of select="/user/@name" />
    <xsl:if test="not ( /user/realm/@id = 'local' )">
      <xsl:text>@</xsl:text>
      <xsl:value-of select="/user/realm/@id" />
    </xsl:if>
  </xsl:variable>

  <xsl:variable name="actions">

    <xsl:if test="(string-length($step) = 0) or ($step = 'cantDeleteLE') or ($step = 'changedPassword')">
      <xsl:choose>
        <xsl:when test="contains($CurrentGroups,'admins')">
          <action target="{$WebApplicationBaseURL}authorization/change-user.xml" label="{i18n:translate('component.user2.admin.changedata')}">
            <param name="action" value="save" />
            <param name="id" value="{/user/@id}" />
          </action>
        </xsl:when>
        <xsl:when test="$CurrentUser != $uid">
          <action target="{$WebApplicationBaseURL}authorization/change-read-user.xml" label="{i18n:translate('component.user2.admin.changedata')}">
            <param name="action" value="save" />
            <param name="id" value="{/user/@id}" />
          </action>
        </xsl:when>
      </xsl:choose>
      <xsl:if test="/user/realm/@id = 'local'">
        <action target="{$WebApplicationBaseURL}authorization/change-password.xml" label="{i18n:translate('component.user2.admin.changepw')}">
          <param name="action" value="password" />
          <param name="id" value="{/user/@id}" />
        </action>
      </xsl:if>
      <xsl:if test="contains($CurrentGroups,'admins') or ($CurrentUser != $uid)">
        <action target="UserServlet" label="{i18n:translate('component.user2.admin.userDeleteYes')}">
          <param name="action" value="show" />
          <param name="id" value="{/user/@id}" />
          <param name="XSL.step" value="confirmDelete" />
        </action>
      </xsl:if>
    </xsl:if>
  </xsl:variable>

  <xsl:template match="user">
    <xsl:if test="$step = 'confirmDelete'">
      <div class="section">
        <p>
          <strong>
            <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteRequest')" />
          </strong>
          <br />
          <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteExplain')" />
          <br />
          <xsl:if test="owns/user">
            <strong>
              <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteExplainRead1')" />
              <xsl:value-of select="count(owns/user)" />
              <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteExplainRead2')" />
            </strong>
          </xsl:if>
        </p>
        <form class="action" method="post" action="UserServlet">
          <input name="action" value="delete" type="hidden" />
          <input name="id" value="{/user/@id}" type="hidden" />
          <input name="XSL.step" value="deleted" type="hidden" />
          <input value="{i18n:translate('button.deleteYes')}" class="action" type="submit" />
        </form>
        <form class="action" method="get" action="UserServlet">
          <input name="action" value="show" type="hidden" />
          <input name="id" value="{/user/@id}" type="hidden" />
          <input value="{i18n:translate('button.cancelNo')}" class="action" type="submit" />
        </form>
      </div>
    </xsl:if>
    <xsl:if test="$step = 'deleted'">
      <div class="section">
        <p>
          <strong>
            <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteConfirm')" />
          </strong>
        </p>
      </div>
    </xsl:if>
    <xsl:if test="$step = 'changedPassword'">
      <div class="section">
        <p>
          <strong>
            <xsl:value-of select="i18n:translate('component.user2.admin.passwordChangeConfirm')" />
          </strong>
        </p>
      </div>
    </xsl:if>
    <xsl:if test="$step = 'cantDeleteLE'">
      <div class="section">
        <p>
          <strong>
            <xsl:value-of select="i18n:translate('legalEntity.deleteCant1')" />
            <xsl:for-each select="legalEntity">
              <a href="LegalEntityServlet?id={@id}">
                <xsl:value-of select="name" />
                <xsl:text> [</xsl:text>
                <xsl:value-of select="@id" />
                <xsl:text>]</xsl:text>
              </a>
            </xsl:for-each>
            <xsl:value-of select="i18n:translate('legalEntity.deleteCant2')" />
          </strong>
        </p>
      </div>
    </xsl:if>
    <div class="section" id="sectionlast">
      <table class="user">
        <tr>
          <th scope="row">
            <xsl:value-of select="i18n:translate('component.user2.admin.userAccount')" />
          </th>
          <td>
            <xsl:apply-templates select="." mode="name" />
          </td>
        </tr>
        <tr>
          <th scope="row">
            <xsl:value-of select="i18n:translate('component.user2.admin.passwordHint')" />
          </th>
          <td>
            <xsl:value-of select="hint" />
          </td>
        </tr>
        <tr>
          <th scope="row">
            <xsl:value-of select="i18n:translate('component.user2.admin.user.lastLogin')" />
          </th>
          <td>
            <xsl:call-template name="formatISODate">
              <xsl:with-param name="date" select="lastLogin" />
              <xsl:with-param name="format" select="i18n:translate('metaData.dateTime')" />
            </xsl:call-template>
          </td>
        </tr>
        <tr class="abstand">
          <th scope="row">Name:</th>
          <td>
            <xsl:value-of select="realName" />
          </td>
        </tr>
        <xsl:if test="eMail">
          <tr>
            <th scope="row">E-Mail:</th>
            <td>
              <a href="mailto:{eMail}">
                <xsl:value-of select="eMail" />
              </a>
            </td>
          </tr>
        </xsl:if>
        <xsl:if test="attributes">
          <tr>
            <th scope="row">
              <xsl:value-of select="i18n:translate('component.user2.admin.user.attributes')" />
            </th>
            <td>
              <dl>
                <xsl:for-each select="attributes/attribute">
                  <dt>
                    <xsl:value-of select="@name" />
                  </dt>
                  <dd>
                    <xsl:value-of select="@value" />
                  </dd>
                </xsl:for-each>
              </dl>
            </td>
          </tr>
        </xsl:if>
        <tr class="abstand">
          <th scope="row">
            <xsl:value-of select="i18n:translate('component.user2.admin.owner')" />
          </th>
          <td>
            <xsl:apply-templates select="owner/user" mode="link" />
            <xsl:if test="count(owner)=0">
              <xsl:value-of select="i18n:translate('component.user2.admin.userIndependent')" />
            </xsl:if>
          </td>
        </tr>
        <tr>
          <th scope="row">
            <xsl:value-of select="i18n:translate('component.user2.admin.groups')" />
          </th>
          <td>
            <xsl:for-each select="groups/group">
              <xsl:value-of select="@name" />
              <xsl:variable name="lang">
                <xsl:call-template name="selectPresentLang">
                  <xsl:with-param name="nodes" select="label" />
                </xsl:call-template>
              </xsl:variable>
              <xsl:value-of select="concat(' [',label[lang($lang)]/@text,']')" />
              <xsl:if test="position() != last()">
                <br />
              </xsl:if>
            </xsl:for-each>
          </td>
        </tr>
        <tr>
          <th scope="row">
            <xsl:value-of select="i18n:translate('component.user2.admin.userOwns')" />
          </th>
          <td>
            <xsl:apply-templates select="owns/user" mode="link" />
          </td>
        </tr>
      </table>
    </div>
  </xsl:template>

  <xsl:template match="user" mode="link">
    <a href="UserServlet?action=show&amp;id={@id}">
      <xsl:apply-templates select="." mode="name" />
    </a>
    <xsl:if test="position() != last()">
      <br />
    </xsl:if>
  </xsl:template>

  <xsl:template match="user" mode="name">
    <xsl:value-of select="@name" />
    <xsl:text> [</xsl:text>
    <xsl:value-of select="realm" />
    <xsl:text>]</xsl:text>
  </xsl:template>

</xsl:stylesheet>
