<?xml version="1.0" encoding="UTF-8"?>

<!-- XSL to search for users and display the list users found -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
  xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xsl xalan i18n encoder"
>

  <xsl:include href="MyCoReLayout.xsl" />

  <xsl:variable name="PageID" select="'users'" />

  <xsl:variable name="PageTitle" select="i18n:translate('component.user2.admin.users.title')" />
  
  <xsl:param name="MCR.Ajax.LoadingImage" />

  <xsl:template match="/users" mode="headAdditional" />

  <xsl:template match="/users">
    <div class="user-list-users">
      <xsl:if test="@num">
        <div class="section">
          <form class="form-inline" action="{$ServletsBaseURL}MCRUserServlet" onsubmit="document.getElementById('indicator').style.display='inline';">
            <div class="form-group">
              <label class="control-label" for="name">
                <xsl:value-of select="i18n:translate('component.user2.admin.search')" />
                <xsl:text>&#160;</xsl:text>
              </label>
              <input class="form-control" type="text" name="search" value="{@search}" />
              <img id="indicator" style="display:none" src="{$WebApplicationBaseURL}{$MCR.Ajax.LoadingImage}" />
            </div>
          </form>
          <xsl:apply-templates select="." mode="headAdditional" />
        </div>
      </xsl:if>
      <div class="section" id="sectionlast">
        <xsl:choose>
          <xsl:when test="user">
            <strong>
              <xsl:value-of select="i18n:translate('component.user2.admin.search.found', count(user))" />
            </strong>
            <div class="table-responsive">
              <table class="user table table-striped">
                <thead>
                  <tr>
                    <th scope="col">
                      <xsl:value-of select="i18n:translate('component.user2.admin.userAccount')" />
                    </th>
                    <xsl:choose>
                      <xsl:when test="@num">
                        <th scope="col">
                          <xsl:value-of select="i18n:translate('component.user2.admin.user.origin')" />
                        </th>
                        <th scope="col">
                          <xsl:value-of select="i18n:translate('component.user2.admin.user.name')" />
                        </th>
                        <th scope="col">
                          <xsl:value-of select="i18n:translate('component.user2.admin.user.email')" />
                        </th>
                      </xsl:when>
                      <xsl:otherwise>
                        <th scope="col">
                          <xsl:value-of select="i18n:translate('component.user2.admin.user.description')" />
                        </th>
                      </xsl:otherwise>
                    </xsl:choose>
                  </tr>
                </thead>
                <tbody>
                  <xsl:apply-templates select="user" />
                </tbody>
              </table>
            </div>
          </xsl:when>
          <xsl:when test="string-length(@num) = 0">
            <br />
            <div class="alert alert-danger">
              <xsl:value-of select="i18n:translate('component.user2.admin.search.error.noRight')" />
            </div>
          </xsl:when>
          <xsl:when test="@num = 0">
            <br />
            <div class="alert alert-warning">
              <xsl:value-of select="i18n:translate('component.user2.admin.search.error.noUserFound')" />
            </div>
          </xsl:when>
          <xsl:when test="number(@num) &gt; number(@max)">
            <br />
            <div class="alert alert-warning">
              <xsl:value-of select="i18n:translate('component.user2.admin.search.error.tooManyUsers', concat(@num,';',@max))" />
            </div>
          </xsl:when>
        </xsl:choose>
      </div>
    </div>
  </xsl:template>

  <xsl:template match="user">
    <tr>
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
