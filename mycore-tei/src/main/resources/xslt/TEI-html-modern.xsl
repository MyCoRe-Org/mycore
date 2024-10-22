<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ This file is part of ***  M y C o R e  ***
  ~ See http://www.mycore.de/ for details.
  ~
  ~ MyCoRe is free software: you can redistribute it and/or modify
  ~ it under the terms of the GNU General Public License as published by
  ~ the Free Software Foundation, either version 3 of the License, or
  ~ (at your option) any later version.
  ~
  ~ MyCoRe is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~ MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~ GNU General Public License for more details.
  ~
  ~ You should have received a copy of the GNU General Public License
  ~ along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0"
                xmlns:mml="http://www.w3.org/1998/Math/MathML"
                version="3.0">

  <xsl:output method="html" doctype-system="about:legacy-compat" indent="yes" omit-xml-declaration="yes" media-type="text/html"/>
  <xsl:preserve-space elements="*"/>
  <xsl:param name="WebApplicationBaseURL"/>
  <xsl:param name="derivate"/>

  <xsl:template match="tei:TEI">
    <xsl:comment>Begin - template match="tei:TEI" (TEI-tei2html.xsl)</xsl:comment>

    <html>
      <head>
        <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
        <!-- include Junicode web font -->
        <link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}tei/css/webfont-junicode.css"/>
        <link rel="stylesheet" type="text/css" href="{$WebApplicationBaseURL}tei/css/tei.css"/>
        <title>
          <xsl:value-of select="tei:teiHeader/tei:fileDesc/tei:titleStmt/tei:title"/>
        </title>
      </head>

      <body class="transcription">
        <div id="content" style="font-family:junicode,Times New Roman,serif; font-size:14pt; line-height: 1.5;">
          <!-- Haupttext -->
          <xsl:apply-templates select="tei:text/tei:body/*"/>
        </div>

        <!-- Kommentarbereich  -->
        <div class="bg-light small">
          <xsl:if test=".//tei:note[@type='editorial'] | //tei:note[@place='foot']">
            <hr/>
          </xsl:if>

          <xsl:apply-templates select=".//tei:note[@place='foot']" mode="footnote"/>

          <xsl:if test=".//tei:note[@type='editorial']">
            <div style="font-size:14pt; font-family:Helvetica,Arial,sans-serif; color:black; font-weight:600; text-align:left;">Kommentar</div>
          </xsl:if>

          <div style="position:relative; margin-left:10px; margin-right:10px; margin-top:0px; margin-bottom:0px; font-family:Helvetica,Arial,sans-serif; font-size:12pt;">
            <xsl:for-each select="//tei:note[@type='editorial']">
              <div class="footnotes">
                <a>
                  <xsl:attribute name="name">
                    <xsl:text>fn</xsl:text>
                    <xsl:number level="any" count="tei:note[@type ='editorial']"/>
                  </xsl:attribute>
                  <a>
                    <xsl:attribute name="href">
                      <xsl:text>#fna</xsl:text>
                      <xsl:number level="any" count="tei:note[@type ='editorial']"/>
                    </xsl:attribute>
                    <span style="font-size:9pt;vertical-align:super;color:blue;">
                      <xsl:number level="any" count="tei:note[@type ='editorial']"/>
                    </span>
                  </a>
                  <xsl:text> </xsl:text>
                  <xsl:apply-templates/>
                </a>
              </div>
            </xsl:for-each>
          </div>
        </div>
      </body>
    </html>

    <xsl:comment>End - template match="tei:TEI" (TEI-tei2html.xsl)</xsl:comment>
  </xsl:template>

  <!-- Renders the footnote number appearing behind the word -->
  <xsl:template match="tei:note[@place='foot']">
    <xsl:comment>
      Start - template match="tei:note[@place='foot'] (TEI-tei2html.xsl)
    </xsl:comment>

    <a id="{@xml:id}" href="#{concat('fnttext_', @xml:id)}" class="text-info">
      <sup>
        <xsl:value-of select="@n"/>
      </sup>
    </a>

    <xsl:comment>
      End - template match="tei:note[@place='foot'] (TEI-tei2html.xsl)
    </xsl:comment>
  </xsl:template>

  <!-- Starts a footnote block at the bottom of the page -->
  <xsl:template match="tei:note[@place='foot']" mode="footnote">
    <xsl:comment>
      Start - template match="tei:note[@place='foot']" mode="footnote" (TEI-tei2html.xsl)
    </xsl:comment>

    <span class="d-block mb-3">
      <sup>
        <a id="{concat('fnttext_', @xml:id)}" href="#{@xml:id}" class="text-info font-weight-bold">
          <xsl:value-of select="@n"/>
        </a>
      </sup>
      <span class="d-block mb-1">
        <xsl:apply-templates select="child::*|child::text()"/>
      </span>
    </span>

    <xsl:comment>
      End - template match="tei:note[@place='foot']" mode="footnote" (TEI-tei2html.xsl)
    </xsl:comment>
  </xsl:template>

  <xsl:template match="tei:div">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="tei:head">
    <xsl:variable name="headerContent">
      <xsl:choose>
        <xsl:when test="p">
          <xsl:value-of select="p/text()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="text()"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <h1>
      <xsl:value-of select="$headerContent"/>
    </h1>
  </xsl:template>

  <xsl:template match="tei:ref">
    <xsl:choose>
      <xsl:when test="@type='editorial'">
        <xsl:element name="a">
          <xsl:attribute name="name">
            <xsl:text>fna</xsl:text>
            <xsl:value-of select="."/>
          </xsl:attribute>
          <xsl:attribute name="href">
            <xsl:value-of select="@target"/>
          </xsl:attribute>
          <span style="font-size:9pt;vertical-align:super;color:blue;">
            <xsl:value-of select="."/>
          </span>
        </xsl:element>
      </xsl:when>

      <xsl:otherwise>
        <a>
          <xsl:attribute name="href">
            <xsl:value-of select="@target"/>
          </xsl:attribute>
          <xsl:attribute name="target">_blank</xsl:attribute>
          <xsl:apply-templates/>
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="mml:math">
    <math>
      <xsl:apply-templates mode="nsRemove"/>
    </math>
  </xsl:template>

  <xsl:template match="tei:p">
    <xsl:element name="span">
      <xsl:attribute name="class">
        <xsl:value-of select="'teiLine '"/>
      </xsl:attribute>
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="*|text()"/>
    </xsl:element>
    <xsl:if test="following-sibling::tei:closer[1]">
      <xsl:value-of select="tei:p"/><xsl:text> </xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="tei:pb">
    <br/>
    <xsl:if test="@rend">
      <xsl:attribute name="style">
        <xsl:call-template name="getStyle">
          <xsl:with-param name="rend" select="@rend"/>
        </xsl:call-template>
      </xsl:attribute>
    </xsl:if>
    <xsl:if test="@facs">
      <span style="color:grey;font-family:Helvetica,Arial,sans-serif;">
        <xsl:text>[Blatt </xsl:text>
        <xsl:value-of select="@n"/>
        <xsl:text>:]</xsl:text>
      </span>
      <br/>
    </xsl:if>
    <xsl:apply-templates select="*|text()"/>
  </xsl:template>

  <xsl:template match="tei:fw">
    <span style="color:#666;">
      <xsl:choose>
        <xsl:when test="@place='top' or @place='bottom'">
          <span style="text-align: center; margin: 0 auto; display:block;">
            <xsl:apply-templates></xsl:apply-templates>
          </span>
        </xsl:when>

        <xsl:when test="@place='top-right' or @place='bottom-right'">
          <span style="float:right;">
            <xsl:apply-templates></xsl:apply-templates>
          </span>
          <br/>
        </xsl:when>

        <xsl:when test="@place='top-left' or @place='bottom-left'">
          <span style="float:left;">
            <xsl:apply-templates></xsl:apply-templates>
          </span>
          <br/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates></xsl:apply-templates>
          <br/>
        </xsl:otherwise>
      </xsl:choose>
    </span>
  </xsl:template>

  <xsl:template match="tei:signed">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:salute">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:dateline">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:seg">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:label">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:hi">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:hi[not(text()|*)]">
    <!-- remove empty -->
  </xsl:template>

  <xsl:template match="tei:addrLine">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:address">
    <span style="
                            display:table-cell;
                            border:1px dotted #D1C5A4;
                            padding:10px;">
      <xsl:element name="span">
        <xsl:if test="@rend">
          <xsl:attribute name="style">
            <xsl:call-template name="getStyle">
              <xsl:with-param name="rend" select="@rend"/>
            </xsl:call-template>
          </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates/>
      </xsl:element>
    </span>
  </xsl:template>

  <xsl:template match="tei:table">
    <div class="teiTable">
      <xsl:apply-templates select="tei:row"/>
    </div>
  </xsl:template>

  <xsl:template match="tei:row">
    <div class="teiRow">
      <xsl:apply-templates select="tei:cell"/>
    </div>
  </xsl:template>

  <xsl:template match="tei:bibl">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:del">
    <span title="Gelöscht">
      <xsl:element name="span">
        <xsl:if test="@rend">
          <xsl:attribute name="style">
            <xsl:call-template name="getStyle">
              <xsl:with-param name="rend" select="@rend"/>
            </xsl:call-template>
          </xsl:attribute>
        </xsl:if>
        <xsl:apply-templates/>
      </xsl:element>
    </span>
  </xsl:template>

  <xsl:template match="tei:note">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="@type='editorial'">
          <a>
            <xsl:attribute name="name">
              <xsl:text>fna</xsl:text>
              <xsl:number level="any" count="tei:note[@type ='editorial']"/>
            </xsl:attribute>
            <a>
              <xsl:attribute name="href">
                <xsl:text>#fn</xsl:text>
                <xsl:number level="any" count="tei:note[@type ='editorial']"/>
              </xsl:attribute>
              <xsl:attribute name="title">
                <xsl:copy-of select="normalize-space(.)"/>
              </xsl:attribute>
              <span style="font-size:9pt;vertical-align:super;color:blue;">
                <xsl:number level="any" count="tei:note[@type ='editorial']"/>
              </span>
            </a>
          </a>
        </xsl:when>

        <xsl:when test="@place='margin-left'">
          <span style="display:table-cell; width:110px; float:left; clear:left; margin-left:-125px; text-indent:0; padding:0 4px; border:1px dotted #D1C5A4; font-size:smaller;">
            <span style="color:grey; font-size:smaller; font-family:Helvetica,Arial,sans-serif;">
              <xsl:text>[Marginalie:]</xsl:text>
            </span>
            <br/>
            <span>
              <xsl:apply-templates/>
            </span>
          </span>
        </xsl:when>

        <xsl:when test="@place='margin-right'">
          <span style="display:table-cell; width:110px; float:right; clear:right; margin-right:-125px; text-indent:0; padding:0 4px; border:1px dotted #D1C5A4; font-size:smaller;">
            <span style="color:grey; font-size:smaller; font-family:Helvetica,Arial,sans-serif;">
              <xsl:text>[Marginalie:]</xsl:text>
            </span>
            <br/>
            <span>
              <xsl:apply-templates/>
            </span>
          </span>
        </xsl:when>
        <xsl:when test="@place='bottom-left' or @place='top-left'">
          <br/>
          <span style="margin-left:-125px; display:block;">
            <xsl:apply-templates/>
          </span>
        </xsl:when>
        <xsl:when test="@place='bottom-right' or @place='top-right'">
          <br/>
          <span style="display:block; text-align:right">
            <xsl:apply-templates/>
          </span>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:persName">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="@ref">
          <a>
            <xsl:attribute name="href">
              <xsl:value-of select="@ref"/>
            </xsl:attribute>
            <xsl:attribute name="target">_blank</xsl:attribute>
            <xsl:apply-templates/>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:placeName">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="@ref">
          <a>
            <xsl:attribute name="href">
              <xsl:value-of select="@ref"/>
            </xsl:attribute>
            <xsl:attribute name="target">_blank</xsl:attribute>
            <xsl:apply-templates/>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:orgName">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="@ref">
          <a>
            <xsl:attribute name="href">
              <xsl:value-of select="@ref"/>
            </xsl:attribute>
            <xsl:attribute name="target">_blank</xsl:attribute>
            <xsl:apply-templates/>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:rs">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="@type='person'">
          <xsl:choose>
            <xsl:when test="@ref">
              <a>
                <xsl:attribute name="href">
                  <xsl:value-of select="@ref"/>
                </xsl:attribute>
                <xsl:attribute name="target">_blank</xsl:attribute>
                <xsl:apply-templates/>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@type='place'">
          <xsl:choose>
            <xsl:when test="@ref">
              <a>
                <xsl:attribute name="href">
                  <xsl:value-of select="@ref"/>
                </xsl:attribute>
                <xsl:attribute name="target">_blank</xsl:attribute>
                <xsl:apply-templates/>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:when test="@type='organisation'">
          <xsl:choose>
            <xsl:when test="@ref">
              <a>
                <xsl:attribute name="href">
                  <xsl:value-of select="@ref"/>
                </xsl:attribute>
                <xsl:attribute name="target">_blank</xsl:attribute>
                <xsl:apply-templates/>
              </a>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:app">
    <xsl:variable name="href" select="concat('#',@n)"/>
    <span class="popupTrigger">
      <a>
        <xsl:apply-templates select="tei:lem"/>
      </a>
      <div class="popupBox" style="display: none;">
        <ul style="padding-left: 20px">
          <xsl:for-each select="tei:rdg">
            <li>
              <xsl:apply-templates/>
            </li>
          </xsl:for-each>
        </ul>
      </div>
    </span>
    <xsl:if test="contains(@rend,'note')">
      <a id="{concat('t',@n)}" href="{$href}" style="vertical-align:top; font-size:0.8em; line-height:100%;">
        <xsl:value-of select="@n"/>
      </a>
    </xsl:if>
  </xsl:template>

  <xsl:template match="tei:app[contains(@rend,'note')]" mode="footnotes">
    <dt>
      <a id="{@n}" href="{concat('#t',@n)}">
        <xsl:value-of select="@n"/>
      </a>
    </dt>
    <dd>
      <xsl:apply-templates select="tei:rdg" mode="footnotes"/>
      <br/>
    </dd>
  </xsl:template>

  <xsl:template match="tei:rdg" mode="footnotes">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="tei:lb">
    <br/>
  </xsl:template>

  <xsl:template match="tei:list">
    <ul>
      <xsl:apply-templates/>
    </ul>
  </xsl:template>

  <xsl:template match="tei:list[@type='ordered']">
    <ol>
      <xsl:apply-templates/>
    </ol>
  </xsl:template>

  <xsl:template match="tei:item">
    <li>
      <xsl:apply-templates/>
    </li>
  </xsl:template>

  <xsl:template match="tei:cell">
    <div class="teiCell">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="not(*|text())">
        <xsl:text>&#160;</xsl:text>
      </xsl:if>
      <xsl:apply-templates select="*|text()"/>
    </div>
  </xsl:template>

  <xsl:template match="tei:choice">
    <xsl:choose>
      <xsl:when test="./tei:reg">
        <span style="padding-bottom: 2px; border-bottom:dotted 2px #2d9700;">
          <xsl:element name="span">
            <xsl:attribute name="title">Original:
              <xsl:value-of select="tei:orig"/>
            </xsl:attribute>
            <xsl:apply-templates select="tei:reg"/>
          </xsl:element>
        </span>
      </xsl:when>
      <xsl:when test="./tei:abbr and not(ancestor::tei:subst)">
        <span style="padding-bottom: 1px; border-bottom:dotted 2px #1942a1;">
          <xsl:element name="span">
            <xsl:attribute name="title">Abkürzung im Original:
              <xsl:value-of select="tei:abbr"/>
            </xsl:attribute>
            <xsl:apply-templates select="tei:expan"/>
          </xsl:element>
        </span>
      </xsl:when>
      <xsl:when test="./tei:abbr and ancestor::tei:subst">
        <span style="padding-bottom: 1px; border-bottom:dotted 2px #1942a1;">
          <xsl:element name="span">
            <xsl:attribute name="title">Abkürzung im Original: <xsl:value-of select="tei:abbr"/>. Überschrieben:
              <xsl:value-of select="ancestor::tei:subst/tei:del"/>
            </xsl:attribute>
            <xsl:apply-templates select="tei:expan"/>
          </xsl:element>
        </span>
      </xsl:when>
      <xsl:when test="./tei:corr">
        <span style="padding-bottom: 3px; border-bottom:dotted 2px #97002d">
          <xsl:element name="span">
            <xsl:attribute name="title">Korrektur:
              <xsl:value-of select="tei:corr"/>
            </xsl:attribute>
            <xsl:apply-templates select="tei:sic"/>
          </xsl:element>
        </span>
      </xsl:when>
      <xsl:when test="tei:unclear">
        <span class="unclear">
          <xsl:attribute name="title">Alternative Lesung (mit niedrigerer Gewissheit):
            <xsl:value-of select="tei:unclear[@cert='low']"/>
          </xsl:attribute>
          <span style="color:green">
            <xsl:text>[</xsl:text>
          </span>
          <xsl:value-of select="tei:unclear[@cert='high']"/>
          <span style="color:green">
            <xsl:text>]</xsl:text>
          </span>
        </span>
      </xsl:when>

      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="tei:add">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="@place='superlinear' and not(parent::tei:subst)">
          <span style="padding-bottom: 4px; border-bottom:dotted 2px #FFBF00;">
            <xsl:element name="span">
              <xsl:attribute name="title">Einfügung über der Zeile</xsl:attribute>
              <xsl:apply-templates/>
            </xsl:element>
          </span>
        </xsl:when>
        <xsl:when test="@place='sublinear' and not(parent::tei:subst)">
          <span style="padding-bottom: 4px; border-bottom:dotted 2px #FFBF00;">
            <xsl:element name="span">
              <xsl:attribute name="title">Einfügung unter der Zeile</xsl:attribute>
              <xsl:apply-templates/>
            </xsl:element>
          </span>
        </xsl:when>
        <xsl:when test="@place='margin-left' or @place='margin-right' or @place='bottom'">
          <span style="padding-bottom: 4px; border-bottom:dotted 2px #FFBF00;" class="linebreak_none">
            <xsl:element name="span">
              <xsl:if test="@place='margin-left'">
                <xsl:attribute name="title">Einfügung links der Zeile</xsl:attribute>
              </xsl:if>
              <xsl:if test="@place='margin-right'">
                <xsl:attribute name="title">Einfügung rechts der Zeile</xsl:attribute>
              </xsl:if>
              <xsl:if test="@place='bottom'">
                <xsl:attribute name="title">Einfügung am unteren Rand</xsl:attribute>
              </xsl:if>
              <xsl:apply-templates/>
            </xsl:element>
          </span>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:subst">
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="./tei:add[@place='across']">
          <span style="border-bottom:dotted 2px #B40404;">
            <xsl:element name="span">

              <xsl:if test="descendant::tei:unclear">
                <xsl:attribute name="title">Überschrieben: [<xsl:value-of select="tei:del"/>]
                </xsl:attribute>
              </xsl:if>

              <xsl:if test="not(descendant::tei:unclear)">
                <xsl:attribute name="title">Überschrieben:
                  <xsl:value-of select="tei:del[@rend='overwritten' or @rend='erasure']"/>
                  <xsl:apply-templates select="tei:del/tei:gap"/>
                </xsl:attribute>
              </xsl:if>

              <xsl:apply-templates select="tei:add"/>
            </xsl:element>
          </span>
        </xsl:when>

        <xsl:when test="./tei:add[@place='superlinear']">
          <xsl:if test="./tei:del[@rend='strikethrough']">
            <span style="text-decoration: line-through;">
              <xsl:value-of select="tei:del[@rend='strikethrough']"/>
            </span>
            <span style="padding-bottom: 4px; border-bottom:dotted 2px #FFBF00;">
              <xsl:element name="span">
                <xsl:attribute name="title">Ersetzung über der Streichung</xsl:attribute>
                <xsl:apply-templates select="tei:add"/>
              </xsl:element>
            </span>
          </xsl:if>
          <xsl:if test="./tei:del[@rend='erasure']">
            <span style="color:#C6C6C6;">
              <xsl:value-of select="tei:del[@rend='erasure']"/>
            </span>
            <xsl:text> </xsl:text>
            <span style="padding-bottom: 4px; border-bottom:dotted 2px #FFBF00;">
              <xsl:element name="span">
                <xsl:attribute name="title">Ersetzung über der Löschung</xsl:attribute>
                <xsl:apply-templates select="tei:add"/>
              </xsl:element>
            </span>
          </xsl:if>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:gap'>

    <xsl:element name="span">
      <xsl:attribute name="title">
        <xsl:if test="@reason='lost'">verlorenes Material</xsl:if>
        <xsl:if test="@reason='insignificant'">irrelevantes Material</xsl:if>
        <xsl:if test="@reason='fm'">fremdsprachliches Material</xsl:if>
        <xsl:if test="@reason='illegible'">unleserliches Material</xsl:if>
        <xsl:if test="@reason='erased'">gelöschtes Material</xsl:if>
        <xsl:if test="@reason='incomplete'">unvollständiges Material</xsl:if>

        <xsl:if test="@unit">
          <xsl:text> – </xsl:text>
        </xsl:if>
        <xsl:choose>
          <xsl:when test="@unit">
            <xsl:if test="@quantity">
              <xsl:value-of select="@quantity"/><xsl:text> </xsl:text>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="@unit='pages' and @quantity!=1">Seiten</xsl:when>
              <xsl:when test="@unit='pages' and @quantity=1">Seite</xsl:when>
              <xsl:when test="@unit='lines' and @quantity!=1">Zeilen</xsl:when>
              <xsl:when test="@unit='lines' and @quantity=1">Zeile</xsl:when>
              <xsl:when test="@unit='words' and @quantity!=1">Wörter</xsl:when>
              <xsl:when test="@unit='words' and @quantity=1">Wort</xsl:when>
              <xsl:when test="@unit='chars'">Zeichen</xsl:when>
            </xsl:choose>
            <xsl:text> fehl</xsl:text>
            <xsl:if test="@quantity=1 or not(@quantity)">t</xsl:if>
            <xsl:if test="@quantity!=1">en</xsl:if>
          </xsl:when>
        </xsl:choose>
      </xsl:attribute>
      <span class="gap" style="color:green">
        <xsl:if test="@quantity=1 and @unit='chars'">
          <xsl:text>[#]</xsl:text>
        </xsl:if>

        <xsl:if test="@quantity=1 and @unit='words'">
          <xsl:text>[###]</xsl:text>
        </xsl:if>

        <xsl:if test="@quantity!=1 and @unit='chars'">
          <xsl:text>[###]</xsl:text>
        </xsl:if>

        <xsl:if test="@quantity!=1 and @unit='words'">
          <xsl:text>[### ###]</xsl:text>
        </xsl:if>

        <xsl:if test="not(@quantity) and not(@unit)">
          <xsl:text>[###]</xsl:text>
        </xsl:if>
      </span>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:supplied'>
    <span class="supplied">
      <xsl:attribute name="title">Rekonstruktion verlorenen Materials. Gewissheit:
        <xsl:if test="@cert='high'">hoch.</xsl:if>
        <xsl:if test="@cert='low'">niedrig.</xsl:if>
      </xsl:attribute>
      <span style="color:green">
        <xsl:text>[</xsl:text>
      </span>
      <xsl:apply-templates/>
      <span style="color:green">
        <xsl:text>]</xsl:text>
      </span>
    </span>
  </xsl:template>

  <xsl:template match='tei:unclear'>

    <xsl:if test="not(parent::tei:choice)">
      <span class="unclear">
        <xsl:choose>
          <xsl:when test="@reason='ambiguousType'">
            <xsl:attribute name="title">Uneindeutige Schriftart.
            </xsl:attribute>
            <span style="color:green">
              <xsl:text>[</xsl:text>
            </span>
            <xsl:apply-templates/>
            <span style="color:green">
              <xsl:text>]</xsl:text>
            </span>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="title">Alternative Lesung möglich. Grund:
              <xsl:if test="@reason='illegible'">unleserlich.</xsl:if>
              <xsl:if test="@reason='faded'">verblasst.</xsl:if>
              <xsl:if test="@reason='stain'">verschmutzt.</xsl:if>
              <xsl:if test="@reason='incomplete'">unvollständig.</xsl:if>
              <xsl:if test="@reason='border'">Seitenende.</xsl:if>
              <xsl:if test="@reason='erased'">gelöscht.</xsl:if>
              Gewissheit:
              <xsl:if test="@cert='high'">hoch.</xsl:if>
              <xsl:if test="@cert='low'">niedrig.</xsl:if>
            </xsl:attribute>
            <span style="color:green">
              <xsl:text>[</xsl:text>
            </span>
            <xsl:apply-templates/>
            <span style="color:green">
              <xsl:text>]</xsl:text>
            </span>
          </xsl:otherwise>
        </xsl:choose>
      </span>
    </xsl:if>
  </xsl:template>

  <xsl:template match='tei:space'>
    <xsl:choose>
      <xsl:when test="@dim='vertical'">
        <xsl:if test="@quantity='0.5'">
          <span style="line-height:50%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='1'">
          <br/>
        </xsl:if>
        <xsl:if test="@quantity='2'">
          <span style="line-height:200%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='3'">
          <span style="line-height:300%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='4'">
          <span style="line-height:400%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='5'">
          <span style="line-height:500%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='6'">
          <span style="line-height:600%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='7'">
          <span style="line-height:700%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='8'">
          <span style="line-height:800%;">
            <br/>
          </span>
        </xsl:if>
        <xsl:if test="@quantity='9'">
          <span style="line-height:900%;">
            <br/>
          </span>
        </xsl:if>
      </xsl:when>

      <xsl:when test="@dim='horizontal'">
        <xsl:text> </xsl:text>
      </xsl:when>

      <xsl:otherwise>
        <xsl:apply-templates select="*|text()"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:milestone'>
    <xsl:if test="@rend='diagonal-rule'">
      <xsl:text>&#x29F8;</xsl:text>
      <xsl:apply-templates/>
    </xsl:if>
    <xsl:if test="@rend='horizontal-rule'">
      <xsl:text>&#x23AF;&#x23AF;&#x23AF;&#x23AF;&#x23AF;</xsl:text>
      <xsl:apply-templates/>
    </xsl:if>
  </xsl:template>

  <xsl:template match='tei:figure'>
    <xsl:if test="@type='seal'">
      <span style="font-size:1.6em;" title="Siegel">
        <xsl:if test="@place='margin-left'">
          <span style="float:left; margin-left:-125px; text-indent:0; padding:0">
            <xsl:text>&#x24C8;</xsl:text>
          </span>
        </xsl:if>
        <xsl:if test="@place='left'">
          <span style="text-align: left; margin: 0 auto; display:block;">
            <xsl:text>&#x24C8;</xsl:text>
          </span>
        </xsl:if>
        <xsl:if test="@place='right'">
          <span style="text-align: right; margin: 0 auto; display:block;">
            <xsl:text>&#x24C8;</xsl:text>
          </span>
        </xsl:if>
        <xsl:if test="@place='margin-right'">
          <span style="float:right; margin-right:-125px; text-indent:0; padding:0">
            <xsl:text>&#x24C8;</xsl:text>
          </span>
        </xsl:if>
        <xsl:if test="@place='top' or @place='bottom'">
          <span style="text-align: center; margin: 0 auto; display:block;">
            <xsl:text>&#x24C8;</xsl:text>
          </span>
        </xsl:if>
      </span>
      <br/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="tei:anchor">
    <a>
      <xsl:attribute name="name">
        <xsl:value-of select="@xml:id"/>
      </xsl:attribute>
      <xsl:text> </xsl:text>
    </a>
  </xsl:template>

  <xsl:template name="getStyle">
    <xsl:param name="rend"/>

    <xsl:if test="contains($rend, 'italic')">
      <xsl:value-of select="'font-style: italic;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'bold')">
      <xsl:value-of select="'font-weight: bold;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'strikethrough')">
      <xsl:value-of select="'text-decoration: line-through;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'erasure')">
      <xsl:element name="span">
        <xsl:attribute name="title">gelöscht</xsl:attribute>
        <xsl:value-of select="'color:#C6C6C6;'"/>
      </xsl:element>
    </xsl:if>

    <xsl:if test="contains($rend, 'larger')">
      <xsl:value-of select="'font-size:1.5em;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'right')">
      <xsl:value-of select="'text-align: left;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'left')">
      <xsl:value-of select="'text-align: right;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'align-center')">
      <xsl:value-of select="'display:inline-block; width:100%; text-align:center;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'align-right')">
      <xsl:value-of select="'display:inline-block; width:100%; text-align:right;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'indent')">
      <xsl:value-of select="'display:inline-block; margin-left:2em; text-indent:0'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'justify')">
      <xsl:value-of select="'text-align: justify;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'kurrent')">
      <xsl:value-of select="'font-family: junicode;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'superscript')">
      <xsl:value-of select="'vertical-align: super; font-size:0.7em'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'subscript')">
      <xsl:value-of select="'vertical-align: sub; font-size:0.7em'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'spaced')">
      <xsl:value-of select="'letter-spacing: 0.1em'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'color') and not(contains($rend, 'underline')) and not(contains($rend, 'mark'))">
      <xsl:variable name="colorCode" select="substring-before(substring-after($rend, 'color('),')')"/>
      <xsl:variable name="firstChar" select="substring($colorCode,1,1)"/>
      <xsl:choose>
        <xsl:when test="$firstChar='#'">
          <xsl:value-of select="concat('color:',$colorCode,';')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('color:#',$colorCode,';')"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>

    <xsl:if test="contains($rend, 'underline')">
      <xsl:value-of select="'border-bottom:2px solid;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'underline') and contains($rend, 'color')">
      <xsl:variable name="colorCode" select="substring-before(substring-after($rend, 'color('),')')"/>
      <xsl:variable name="firstChar" select="substring($colorCode,1,1)"/>
      <xsl:choose>
        <xsl:when test="$firstChar='#'">
          <xsl:value-of select="concat('border-bottom:2px solid',$colorCode,';')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('border-bottom:2px solid',$colorCode,';')"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>

    <xsl:if test="contains($rend, 'mark')">
      <xsl:value-of select="'background-color:#CCCCCC;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'mark') and contains($rend, 'color(')">
      <xsl:variable name="colorCode" select="substring-before(substring-after($rend, 'color('),')')"/>
      <xsl:variable name="firstChar" select="substring($colorCode,1,1)"/>
      <xsl:choose>
        <xsl:when test="$firstChar='#'">
          <xsl:value-of select="concat('background-color:',$colorCode,';')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('background-color:#',$colorCode,';')"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>

    <xsl:if test="contains($rend, 'latin')">
      <xsl:value-of select="'font-family:helvetica,arial,sans-serif;'"/>
    </xsl:if>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>

</xsl:stylesheet>