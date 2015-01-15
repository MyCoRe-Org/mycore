<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:tei="http://www.tei-c.org/ns/1.0">

  <xsl:output method="html" indent="yes"/>

  <xsl:template match="/tei:TEI">
    <xsl:comment>
      mcrtranscr-html.xsl
    </xsl:comment>
    <html>
      <body>
        <xsl:apply-templates select="tei:text/tei:body/*"/>
      </body>
    </html>
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
        <!--every p is a line -->
        <xsl:value-of select="'teiLine '"/>
        <!-- center text content -->

      </xsl:attribute>
      <xsl:value-of select="text()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:table">
    <div class="teiTable">
      <xsl:for-each select="tei:row">
        <div class="teiRow">
          <xsl:for-each select="tei:cell">
            <div class="teiCell">
              <xsl:apply-templates select="*"/>
            </div>
          </xsl:for-each>
        </div>
      </xsl:for-each>
    </div>
  </xsl:template>


</xsl:stylesheet>