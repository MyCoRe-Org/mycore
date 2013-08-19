<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to display data of a login user -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:mcrxsl="xalan://org.mycore.common.xml.MCRXMLFunctions" xmlns:acl="xalan://org.mycore.access.MCRAccessManager" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:const="xalan://org.mycore.user2.MCRUser2Constants" exclude-result-prefixes="xsl xalan i18n acl const mcrxsl">

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:variable name="PageID" select="'show-user'" />

  <xsl:variable name="PageTitle" select="concat(i18n:translate('component.user2.admin.userDisplay'),/user/@name)" />

  <xsl:param name="step" />

  <xsl:variable name="uid">
    <xsl:value-of select="/user/@name" />
    <xsl:if test="not ( /user/@realm = 'local' )">
      <xsl:text>@</xsl:text>
      <xsl:value-of select="/user/@realm" />
    </xsl:if>
  </xsl:variable>
  <xsl:variable name="owns" select="document(concat('user:getOwnedUsers:',$uid))/owns" />

  <xsl:template match="user" mode="actions">
    <xsl:variable name="isCurrentUser" select="$CurrentUser = /user/@name"/>
    
    <xsl:if test="(string-length($step) = 0) or ($step = 'changedPassword')">
      <xsl:choose>
        <xsl:when test="acl:checkPermission(const:getUserAdminPermission())">
          <form action="{$WebApplicationBaseURL}authorization/change-user.xml" method="get">
            <input type="hidden" name="action" value="save" />
            <input type="hidden" name="id" value="{$uid}" />
            <input type="submit" class="action" value="{i18n:translate('component.user2.admin.changedata')}" />
          </form>
        </xsl:when>
        <xsl:when test="not($isCurrentUser)">
          <form action="{$WebApplicationBaseURL}authorization/change-read-user.xml" method="get">
            <input type="hidden" name="action" value="save" />
            <input type="hidden" name="id" value="{$uid}" />
            <input type="submit" class="action" value="{i18n:translate('component.user2.admin.changedata')}" />
          </form>
        </xsl:when>
        <xsl:when test="$isCurrentUser and not(/user/@locked = 'true')">
          <form action="{$WebApplicationBaseURL}authorization/change-current-user.xml" method="get">
            <input type="hidden" name="action" value="saveCurrentUser" />
            <input type="submit" class="action" value="{i18n:translate('component.user2.admin.changedata')}" />
          </form>
        </xsl:when>
      </xsl:choose>
      <xsl:if test="/user/@realm = 'local' and (not($isCurrentUser) or not(/user/@locked = 'true'))">
        <form action="{$WebApplicationBaseURL}authorization/change-password.xml" method="get">
          <input type="hidden" name="action" value="password" />
          <input type="hidden" name="id" value="{$uid}" />
          <input type="submit" class="action" value="{i18n:translate('component.user2.admin.changepw')}" />
        </form>
      </xsl:if>
      <xsl:if test="mcrxsl:isCurrentUserInRole('admin') and not($isCurrentUser)">
        <form action="MCRUserServlet" method="get">
          <input type="hidden" name="action" value="show" />
          <input type="hidden" name="id" value="{$uid}" />
          <input type="hidden" name="XSL.step" value="confirmDelete" />
          <input type="submit" class="action" value="{i18n:translate('component.user2.admin.userDeleteYes')}" />
        </form>
      </xsl:if>
    </xsl:if>
  </xsl:template>

  <xsl:template match="user">
    <div class="user-details">
      <div id="buttons">
        <xsl:apply-templates select="." mode="actions" />
      </div>
      <xsl:if test="$step = 'confirmDelete'">
        <div class="section">
          <p>
            <strong>
              <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteRequest')" />
            </strong>
            <br />
            <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteExplain')" />
            <br />
            <xsl:if test="$owns/user">
              <strong>
                <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteExplainRead1')" />
                <xsl:value-of select="count($owns/user)" />
                <xsl:value-of select="i18n:translate('component.user2.admin.userDeleteExplainRead2')" />
              </strong>
            </xsl:if>
          </p>
          <form class="action" method="post" action="MCRUserServlet">
            <input name="action" value="delete" type="hidden" />
            <input name="id" value="{$uid}" type="hidden" />
            <input name="XSL.step" value="deleted" type="hidden" />
            <input value="{i18n:translate('component.user2.button.deleteYes')}" class="action" type="submit" />
          </form>
          <form class="action" method="get" action="MCRUserServlet">
            <input name="action" value="show" type="hidden" />
            <input name="id" value="{$uid}" type="hidden" />
            <input value="{i18n:translate('component.user2.button.cancelNo')}" class="action" type="submit" />
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
              <xsl:value-of select="password/@hint" />
            </td>
          </tr>
          <tr>
            <th scope="row">
              <xsl:value-of select="i18n:translate('component.user2.admin.user.lastLogin')" />
            </th>
            <td>
              <xsl:call-template name="formatISODate">
                <xsl:with-param name="date" select="lastLogin" />
                <xsl:with-param name="format" select="i18n:translate('component.user2.metaData.dateTime')" />
              </xsl:call-template>
            </td>
          </tr>
          <tr>
            <th scope="row">
              <xsl:value-of select="i18n:translate('component.user2.admin.user.validUntil')" />
            </th>
            <td>
              <xsl:call-template name="formatISODate">
                <xsl:with-param name="date" select="validUntil" />
                <xsl:with-param name="format" select="i18n:translate('component.user2.metaData.dateTime')" />
              </xsl:call-template>
            </td>
          </tr>
          <tr class="abstand">
            <th scope="row">
              <xsl:value-of select="i18n:translate('component.user2.admin.user.name')" />
            </th>
            <td>
              <xsl:value-of select="realName" />
            </td>
          </tr>
          <xsl:if test="eMail">
            <tr>
              <th scope="row">
                <xsl:value-of select="i18n:translate('component.user2.admin.user.email')" />
              </th>
              <td>
                <a href="mailto:{eMail}">
                  <xsl:value-of select="eMail" />
                </a>
              </td>
            </tr>
          </xsl:if>
          <tr>
            <th scope="row">
              <xsl:value-of select="i18n:translate('component.user2.admin.user.locked')" />
            </th>
            <td>
              <xsl:choose>
                <xsl:when test="@locked='true'">
                  <xsl:value-of select="i18n:translate('component.user2.admin.user.locked.true')" />
                </xsl:when>
                <xsl:otherwise>
                  <xsl:value-of select="i18n:translate('component.user2.admin.user.locked.false')" />
                </xsl:otherwise>
              </xsl:choose>
            </td>
          </tr>
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
              <xsl:apply-templates select="owner" mode="link" />
              <xsl:if test="count(owner)=0">
                <xsl:value-of select="i18n:translate('component.user2.admin.userIndependent')" />
              </xsl:if>
            </td>
          </tr>
          <tr>
            <th scope="row">
              <xsl:value-of select="i18n:translate('component.user2.admin.roles')" />
            </th>
            <td>
              <xsl:for-each select="roles/role">
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
              <xsl:for-each select="$owns/user">
                <xsl:apply-templates select="." mode="link" />
                <xsl:if test="position() != last()">
                  <br />
                </xsl:if>
              </xsl:for-each>
            </td>
          </tr>
        </table>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="user|owner" mode="link">
    <xsl:variable name="uid">
      <xsl:value-of select="@name" />
      <xsl:if test="not ( @realm = 'local' )">
        <xsl:text>@</xsl:text>
        <xsl:value-of select="@realm" />
      </xsl:if>
    </xsl:variable>
    <a href="MCRUserServlet?action=show&amp;id={$uid}">
      <xsl:apply-templates select="." mode="name" />
    </a>
  </xsl:template>

  <xsl:template match="user|owner" mode="name">
    <xsl:value-of select="@name" />
    <xsl:text> [</xsl:text>
    <xsl:value-of select="@realm" />
    <xsl:text>]</xsl:text>
  </xsl:template>

</xsl:stylesheet>
