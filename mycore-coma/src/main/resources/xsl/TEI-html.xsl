<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0">

  <xsl:output method="html" indent="yes"/>

  <xsl:template match="/tei:TEI">
    <xsl:comment>
      mcrtranscr-html.xsl
    </xsl:comment>
    <div>
      <xsl:apply-templates select="tei:text/tei:body/*"/>
      <xsl:if test="//tei:note">
        <hr/>
        <dl>
          <xsl:apply-templates select="//tei:note" mode="footnotes"/>
        </dl>
      </xsl:if>
    </div>
  </xsl:template>


  <xsl:template match="tei:div">
    <xsl:comment>tei:div</xsl:comment>
    <xsl:apply-templates select="*"/>
  </xsl:template>

  <!-- header -> h1 -->
  <xsl:template match="tei:head">
    <xsl:comment>tei:head</xsl:comment>
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


  <!-- p -> span[@line]-->
  <xsl:template match="tei:p">
    <xsl:comment>tei:p</xsl:comment>
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
  </xsl:template>

  <xsl:template match="tei:hi">
    <xsl:comment>tei:hi</xsl:comment>
    <xsl:element name="span">
      <xsl:if test="@rend">
        <xsl:attribute name="style">
          <xsl:call-template name="getStyle">
            <xsl:with-param name="rend" select="@rend"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="*|text()"/>
    </xsl:element>
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

  <xsl:template match="tei:note[@place='foot']">
    <a href="{concat('#fn', @n)}" style="vertical-align:top; font-size:0.8em; line-height:100%;">
      <xsl:value-of select="@n"/>
    </a>
  </xsl:template>

  <xsl:template match="tei:note[@place='foot']" mode="footnotes">
    <dt id="{concat('fn', @n)}">
      <xsl:value-of select="@n"/>
    </dt>
    <dd>
      <xsl:value-of select="."/>
    </dd>
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

    <xsl:if test="contains($rend, 'underline')">
      <xsl:value-of select="'text-decoration: underline;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'color(')">
      <xsl:variable name="colorCode" select="substring-before(substring-after($rend, 'color('),')')"/>
      <xsl:value-of select="concat('color:',$colorCode,';')"/>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>