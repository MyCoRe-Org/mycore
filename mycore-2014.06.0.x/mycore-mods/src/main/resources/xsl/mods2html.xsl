<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" xmlns:xlink="http://www.w3.org/1999/xlink"
  xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" exclude-result-prefixes="mods i18n">
  <!-- MODS2 records to html ntra added 4th child level 4/2/04 -->

  <xsl:template match="mods:modsCollection">
    <xsl:apply-templates select="mods:mods" />
  </xsl:template>

  <xsl:template match="mods:mods">
    <table>
      <xsl:apply-templates/>
    </table>
  </xsl:template>

  <xsl:template match="mods:*">
    <xsl:choose>
      <xsl:when test="child::*">
        <tr>
          <td colspan="2">
            <b>
              <xsl:call-template name="longName">
                <xsl:with-param name="name">
                  <xsl:value-of select="local-name()" />
                </xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="attr" />
            </b>
          </td>
        </tr>
        <xsl:apply-templates mode="level2" />
      </xsl:when>

      <xsl:otherwise>
        <tr>
          <td width="300pt" class="metaname">
            <b>
              <xsl:call-template name="longName">
                <xsl:with-param name="name">
                  <xsl:value-of select="local-name()" />
                </xsl:with-param>
              </xsl:call-template>
              <xsl:call-template name="attr" />
            </b>
          </td>
          <td class="metavalue">
            <xsl:call-template name="formatValue" />
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="formatValue">
    <xsl:choose>
      <xsl:when test="@type='uri'">
        <a href="{text()}">
          <xsl:value-of select="text()" />
        </a>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="text()" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*" mode="level2">
    <xsl:choose>
      <xsl:when test="child::*">
        <tr>
          <td colspan="2" style="padding: 0 1em">
            <xsl:call-template name="longName">
              <xsl:with-param name="name">
                <xsl:value-of select="local-name()" />
              </xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="attr" />
          </td>
        </tr>
        <xsl:apply-templates mode="level3" />
      </xsl:when>
      <xsl:otherwise>
        <tr>
          <td class="metaname" style="padding: 0 1em">
            <xsl:call-template name="longName">
              <xsl:with-param name="name">
                <xsl:value-of select="local-name()" />
              </xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="attr" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="formatValue" />
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*" mode="level3">
    <xsl:choose>
      <xsl:when test="child::*">
        <tr>
          <td colspan="2" style="padding: 0 2em">
            <xsl:call-template name="longName">
              <xsl:with-param name="name">
                <xsl:value-of select="local-name()" />
              </xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="attr" />
          </td>
        </tr>
        <xsl:apply-templates mode="level4" />
      </xsl:when>
      <xsl:otherwise>
        <tr>
          <td class="metaname" style="padding: 0 2em">
            <xsl:call-template name="longName">
              <xsl:with-param name="name">
                <xsl:value-of select="local-name()" />
              </xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="attr" />
          </td>
          <td class="metavalue">
            <xsl:call-template name="formatValue" />
          </td>
        </tr>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*" mode="level4">
    <tr>
      <td class="metaname" style="padding: 0 3em">
        <xsl:call-template name="longName">
          <xsl:with-param name="name">
            <xsl:value-of select="local-name()" />
          </xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="attr" />
      </td>
      <td class="metavalue">
        <xsl:value-of select="text()" />
      </td>
    </tr>
  </xsl:template>

  <xsl:template name="longName">
    <xsl:param name="name" />
    <xsl:variable name="value" select="i18n:translate(concat('component.mods.metaData.dictionary.',$name))" />
    <xsl:choose>
      <xsl:when test="contains($value,'???')">
        <xsl:value-of select="$name" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$value" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="attr">
    <xsl:for-each select="@type|@point">
      <xsl:text>:</xsl:text>
      <xsl:call-template name="longName">
        <xsl:with-param name="name">
          <xsl:value-of select="." />
        </xsl:with-param>
      </xsl:call-template>
    </xsl:for-each>

    <xsl:if test="@authority or @edition">
      <xsl:for-each select="@authority">
        <xsl:text>(</xsl:text>
        <xsl:call-template name="longName">
          <xsl:with-param name="name">
            <xsl:value-of select="." />
          </xsl:with-param>
        </xsl:call-template>
      </xsl:for-each>

      <xsl:if test="@edition">
        Edition
        <xsl:value-of select="@edition" />
      </xsl:if>
      <xsl:text>)</xsl:text>
    </xsl:if>

    <xsl:variable name="attrStr">
      <xsl:for-each select="@*[local-name()!='edition' and local-name()!='type' and local-name()!='authority' and local-name()!='point']">
        <xsl:value-of select="concat(local-name(),'=&quot;',.,'&quot;,')" />
      </xsl:for-each>
    </xsl:variable>

    <xsl:variable name="nattrStr" select="normalize-space($attrStr)" />

    <xsl:if test="string-length($nattrStr)">
      <xsl:value-of select="concat('(',substring($nattrStr,1,string-length($nattrStr)-1),')')" />
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>