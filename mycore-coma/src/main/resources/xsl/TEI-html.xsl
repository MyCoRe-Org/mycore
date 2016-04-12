<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0"
                xmlns:mml="http://www.w3.org/1998/Math/MathML"
                version="1.0">

  <xsl:output method="html" indent="no" />

  <xsl:template match="*" mode="nsRemove">
    <xsl:element name="{local-name()}">
      <xsl:apply-templates select="@* | node()" mode="nsRemove" />
    </xsl:element>
  </xsl:template>

  <xsl:template match="@*" mode="nsRemove">
    <xsl:copy />
  </xsl:template>

  <xsl:template match="tei:TEI">
    <div>
      <xsl:apply-templates select="tei:text/tei:body/*"/>
      <xsl:if test="//tei:note">
        <hr/>
        <dl>
          <xsl:apply-templates select="//tei:note[string(number(@n)) != 'NaN']" mode="footnotes">
            <xsl:sort data-type="number" select="number(@n)"/>
          </xsl:apply-templates>
          <xsl:apply-templates select="//tei:note[string(number(@n)) = 'NaN']" mode="footnotes">
            <xsl:sort select="@n"/>
          </xsl:apply-templates>
        </dl>
      </xsl:if>
      <xsl:if test="//tei:app[contains(@rend,'note')]">
        <dl>
          <xsl:apply-templates select="//tei:app[contains(@rend,'note')]" mode="footnotes"/>
        </dl>
      </xsl:if>
    </div>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="translate(., '&#10;&#13;', '')" />
  </xsl:template>

  <xsl:template match="tei:div">
    <xsl:apply-templates select="*"/>
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

  <xsl:template match="mml:math">
    <math>
      <xsl:apply-templates mode="nsRemove" />
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
      <xsl:apply-templates select="*|text()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:hi[not(text()|*)]">
    <!-- remove empty -->
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
    <span id="{concat('fnt',@n)}" class="popupTrigger">
      <a style="vertical-align:top; font-size:0.8em; line-height:100%;">
        <xsl:value-of select="@n"/>
      </a>
      <div class="popupBox" style="display:none">
        <xsl:apply-templates/>
      </div>
    </span>
  </xsl:template>

  <xsl:template match="tei:note[@place='foot']" mode="footnotes">
    <dt id="{concat('fnb', @n)}">
      <a href="{concat('#fnt', @n)}">
        <xsl:value-of select="@n"/>
      </a>
    </dt>
    <dd>
      <xsl:apply-templates/>
      <br/>
    </dd>
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

    <xsl:if test="contains($rend, 'start')">
      <xsl:value-of select="'text-align: left;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'end')">
      <xsl:value-of select="'text-align: right;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'center')">
      <xsl:value-of select="'text-align: center;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'justify')">
      <xsl:value-of select="'text-align: justify;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'sup')">
      <xsl:value-of select="'vertical-align: super;'"/>
    </xsl:if>

    <xsl:if test="contains($rend, 'color(')">
      <xsl:variable name="colorCode" select="substring-before(substring-after($rend, 'color('),')')"/>
      <xsl:variable name="firstChar" select="substring($colorCode,1,1)" />
      <xsl:choose>
        <xsl:when test="$firstChar='#'">
          <xsl:value-of select="concat('color:',$colorCode,';')" />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('color:#',$colorCode,';')" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:if>

  </xsl:template>

</xsl:stylesheet>