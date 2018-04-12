<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:tei="http://www.tei-c.org/ns/1.0"
                xmlns:encoder="xalan://java.net.URLEncoder"
                xmlns:xalan="http://xml.apache.org/xalan"
                exclude-result-prefixes="tei"
                version="1.0">

  <xsl:output method="html"/>
  <xsl:template match="tei:TEI">
    <div>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:teiHeader'/>

  <xsl:template match='tei:cb'>
    <span class="dta-cb">
      <xsl:choose>
        <xsl:when test="@type='start'">[Beginn Spaltensatz]</xsl:when>
        <xsl:when test="@type='end'">[Ende Spaltensatz]</xsl:when>
        <xsl:otherwise>[Spaltenumbruch]</xsl:otherwise>
      </xsl:choose>
    </span>
  </xsl:template>

  <xsl:template match='tei:text[not(descendant::tei:text)]'>
    <xsl:apply-templates/>
    <xsl:for-each select="//tei:note[@place='foot' and string-length(@prev) > 0][not(./following::tei:pb)]">
      <xsl:apply-templates select="." mode="footnotes"/>
    </xsl:for-each>
    <xsl:for-each select="//tei:note[@place='foot' and string-length(@prev) = 0][not(./following::tei:pb)]">
      <xsl:apply-templates select="." mode="footnotes"/>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="tei:choice">
    <xsl:choose>
      <xsl:when test="./tei:reg">
        <xsl:element name="span">
          <xsl:attribute name="title">Original:
            <xsl:value-of select="tei:orig"/>
          </xsl:attribute>
          <xsl:attribute name="class">dta-reg</xsl:attribute>
          <xsl:apply-templates select="tei:reg"/>
        </xsl:element>
      </xsl:when>
      <xsl:when test="./tei:abbr">
        <xsl:element name="span">
          <xsl:attribute name="title">
            <xsl:value-of select="tei:abbr"/>
          </xsl:attribute>
          <xsl:attribute name="class">dta-abbr</xsl:attribute>
          <xsl:apply-templates select="tei:expan"/>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="span">
          <xsl:attribute name="title">Schreibfehler:
            <xsl:value-of select="tei:sic"/>
          </xsl:attribute>
          <xsl:attribute name="class">dta-corr</xsl:attribute>
          <xsl:apply-templates select="tei:corr"/>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="tei:corr">
    <xsl:choose>
      <xsl:when test="not(string(.))">
        <xsl:text>[&#8230;]</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="tei:del">
    <xsl:element name="span">
      <xsl:attribute name="class">dta-del</xsl:attribute>
      <xsl:attribute name="title">Streichung
        <xsl:if test="@rendition">(<xsl:value-of select="@rendition"/>)
        </xsl:if>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:add">
    <xsl:element name="span">
      <xsl:attribute name="class">dta-add</xsl:attribute>
      <xsl:attribute name="title">Hinzufügung
        <xsl:if test="@place">(<xsl:value-of select="@place"/>)
        </xsl:if>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:fw[@place="top"]'>
    <div>
      <xsl:attribute name="class">fw-top fw-<xsl:value-of select="@type"/>
      </xsl:attribute>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:fw[@place="bottom"][not(./ancestor::tei:note)]'>
    <xsl:if test="not(@type='page number')">
      <xsl:element name="div">
        <xsl:attribute name="class">
          <xsl:choose>
            <xsl:when test='@type="sig"'>
              fw-bottom-sig
            </xsl:when>
            <xsl:when test='@type="catch"'>
              fw-bottom-catch
            </xsl:when>
          </xsl:choose>
        </xsl:attribute>
        <xsl:apply-templates/>
      </xsl:element>
    </xsl:if>
  </xsl:template>

  <xsl:template match='tei:fw[@type="page number"]'/>

  <xsl:template match='tei:milestone'>
    <xsl:if test="contains(@rendition, '#hrRed') or contains(@rendition, '#hrBlue') or contains(@rendition, '#hr')">
      <xsl:element name="hr">
        <xsl:choose>
          <xsl:when test="contains(@rendition, '#red') or contains(@rendition, '#hrRed')">
            <xsl:attribute name="class">red</xsl:attribute>
          </xsl:when>
          <xsl:when test="contains(@rendition, '#blue') or contains(@rendition, '#hrBlue')">
            <xsl:attribute name="class">blue</xsl:attribute>
          </xsl:when>
        </xsl:choose>
      </xsl:element>
    </xsl:if>
  </xsl:template>

  <!-- place holders -->
  <xsl:template match='tei:formula'>
    <xsl:choose>
      <xsl:when test="@notation='TeX'">
        <xsl:element name="span">
          <xsl:attribute name="class">formula</xsl:attribute>
          <xsl:if test="@rendition='#c'">
            <xsl:attribute name="style">display:block; text-align:center</xsl:attribute>
          </xsl:if>
          <xsl:element name="img">
            <xsl:attribute name="style">vertical-align:middle; -moz-transform:scale(0.7); -webkit-transform:scale(0.7);
              transform:scale(0.7)
            </xsl:attribute>
            <xsl:attribute name="src">
              <xsl:text>http://dinglr.de/formula/</xsl:text><xsl:value-of select="encoder:encode(.)"/>
            </xsl:attribute>
          </xsl:element>
        </xsl:element>
      </xsl:when>
      <xsl:when test="string-length(.) &gt; 0">
        <xsl:apply-templates/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="span">
          <xsl:attribute name="class">ph formula-<xsl:value-of select="count(preceding::tei:formula)+1"/>
          </xsl:attribute>
          <xsl:attribute name="onclick">editFormula(<xsl:value-of select="count(preceding::tei:formula)+1"/>)
          </xsl:attribute>
          <xsl:attribute name="style">cursor:pointer</xsl:attribute>
          [Formel <xsl:value-of select="count(preceding::tei:formula)+1"/>]
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:figure'>
    <xsl:choose>
      <xsl:when
          test="(local-name(preceding-sibling::node()[1]) = 'lb' and local-name(following-sibling::node()[1]) = 'lb') or @rendition='#c'">
        <xsl:element name="div">
          <xsl:attribute name="class">phbl dta-figure</xsl:attribute>
          <xsl:attribute name="type">
            <xsl:value-of select="count(preceding::tei:figure)+1"/>
          </xsl:attribute>
          <xsl:if test="@rendition='#c'">
            <xsl:attribute name="style">text-align:center</xsl:attribute>
          </xsl:if>
          <xsl:if test="@facs">
            <xsl:element name="img">
              <xsl:attribute name="src">
                <xsl:value-of select="@facs"/>
              </xsl:attribute>
            </xsl:element>
            <br/>
          </xsl:if>
          [
          <xsl:choose>
            <xsl:when test="@type='notatedMusic'">Musik</xsl:when>
            <xsl:otherwise>Abbildung</xsl:otherwise>
          </xsl:choose>
          <xsl:if test="tei:figDesc">
            <xsl:text> </xsl:text>
            <xsl:apply-templates select="tei:figDesc" mode="figdesc"/>
          </xsl:if>
          ]
          <xsl:apply-templates/>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="span">
          <xsl:attribute name="class">ph dta-figure</xsl:attribute>
          <xsl:attribute name="type">
            <xsl:value-of select="count(preceding::tei:figure)+1"/>
          </xsl:attribute>
          <xsl:if test="@facs">
            <xsl:element name="img">
              <xsl:attribute name="src">
                <xsl:value-of select="@facs"/>
              </xsl:attribute>
            </xsl:element>
            <br/>
          </xsl:if>
          [
          <xsl:choose>
            <xsl:when test="@type='notatedMusic'">Musik</xsl:when>
            <xsl:otherwise>Abbildung</xsl:otherwise>
          </xsl:choose>
          <xsl:if test="tei:figDesc">
            <xsl:text> </xsl:text>
            <xsl:apply-templates select="tei:figDesc" mode="figdesc"/>
          </xsl:if>
          ]
          <xsl:apply-templates/>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:figDesc'/>
  <xsl:template match='tei:figDesc' mode="figdesc">
    <xsl:apply-templates/>
  </xsl:template>
  <!-- end place holders -->

  <!-- editorial notes -->
  <xsl:template match='tei:note[@type="editorial"]'/>

  <!-- footnotes -->
  <xsl:template match='tei:note[@place="foot"]'>
    <xsl:if test="string-length(@prev)=0">
      <span class="fn-intext">
        <xsl:value-of select='@n'/>
      </span>
    </xsl:if>
  </xsl:template>

  <xsl:template match='tei:note[@place="foot"]' mode="footnotes">
    <div class="footnote" style="margin-bottom:1em">
      <xsl:choose>
        <xsl:when test="string-length(@prev)!=0 or string-length(@sameAs)!=0"></xsl:when>
        <xsl:otherwise>
          <span class="fn-sign">
            <xsl:value-of select='@n'/>
          </span>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text> </xsl:text>
      <xsl:apply-templates/>
      <xsl:apply-templates select='tei:fw[@place="bottom"][@type="catch"]' mode="fn-catch"/>
    </div>
  </xsl:template>

  <xsl:template match="tei:note/tei:fw"/>
  <xsl:template match="tei:note/tei:fw" mode="fn-catch">
    <div class="fw-bottom-catch">
      <xsl:apply-templates/>
    </div>
  </xsl:template>
  <!-- end footnotes -->

  <!-- end notes -->
  <xsl:template match='tei:note[@place="end"]'>
    <xsl:choose>
      <xsl:when test="string-length(.) &gt; 0">
        <xsl:choose>
          <xsl:when test="local-name(*[1])!='pb'">
            <div class="endnote endnote-indent">
              <span class="fn-sign">
                <xsl:value-of select='@n'/>
              </span>
              <xsl:text> </xsl:text>
              <xsl:apply-templates/>
            </div>
          </xsl:when>
          <xsl:otherwise>
            <div class="endnote">
              <xsl:apply-templates/>
            </div>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <span class="fn-sign">
          <xsl:value-of select='@n'/>
        </span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- end end notes -->

  <!-- marginals -->
  <xsl:template match='tei:note[@place="right" and not(@type)]'>
    <xsl:value-of select='@n'/>
    <span class="dta-marginal dta-marginal-right">
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:note[@place="left" and not(@type)]'>
    <xsl:value-of select='@n'/>
    <span class="dta-marginal dta-marginal-left">
      <xsl:apply-templates/>
    </span>
  </xsl:template>
  <!-- end marginals -->

  <xsl:template match='tei:gap'>
    <span class="gap">
      <xsl:text>[</xsl:text>
      <xsl:if test="@reason='lost'">verlorenes Material</xsl:if>
      <xsl:if test="@reason='insignificant'">irrelevantes Material</xsl:if>
      <xsl:if test="@reason='fm'">fremdsprachliches Material</xsl:if>
      <xsl:if test="@reason='illegible'">unleserliches Material</xsl:if>
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
      <xsl:text>]</xsl:text>
    </span>
  </xsl:template>

  <xsl:template match='tei:titlePage'>
    <div class="titlepage">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:titlePart'>
    <xsl:element name="div">
      <xsl:attribute name="class">titlepart titlepart-<xsl:value-of select="@type"/>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:docImprint'>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match='tei:docAuthor'>
    <span class="docauthor">
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:docDate'>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match='tei:byline'>
    <div class="byline">
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:publisher'>
    <xsl:element name="span">
      <xsl:attribute name="class">dta-publisher
        <xsl:choose>
          <xsl:when test="@rendition=''"/>
          <xsl:when test="contains(normalize-space(@rendition),' ')">
            <xsl:call-template name="splitRendition">
              <xsl:with-param name="value">
                <xsl:value-of select="normalize-space(@rendition)"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="findRendition">
              <xsl:with-param name="value">
                <xsl:value-of select="@rendition"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:head'>
    <xsl:choose>
      <xsl:when test='ancestor::tei:figure'>
        <span class='figdesc'>
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:when test="ancestor::tei:list or parent::tei:lg">
        <div class="dta-head">
          <xsl:apply-templates/>
        </div>
      </xsl:when>
      <xsl:when test="local-name(./*[position()=last()]) != 'lb' and local-name(following::*[1]) != 'lb'">
        <xsl:apply-templates/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test='parent::tei:div/@n or parent::tei:div'>
            <xsl:choose>
              <xsl:when test="parent::tei:div/@n > 6 or not(@n)">
                <div class="dta-head">
                  <xsl:apply-templates/>
                </div>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text disable-output-escaping="yes">&lt;h</xsl:text>
                <xsl:value-of select="parent::tei:div/@n"/>
                <xsl:text disable-output-escaping="yes"> class="dta-head"&gt;</xsl:text>
                <xsl:apply-templates/>
                <xsl:text disable-output-escaping="yes">&lt;/h</xsl:text>
                <xsl:value-of select="parent::tei:div/@n"/>
                <xsl:text disable-output-escaping="yes">&gt;</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>
          <xsl:when test='parent::tei:list'>
            <xsl:apply-templates/>
          </xsl:when>
          <xsl:otherwise>
            <h2>
              <xsl:apply-templates/>
            </h2>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- dramae -->
  <xsl:template match='tei:castList'>
    <div class="castlist">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:castGroup'>
    <xsl:choose>
      <!-- nested castGroups, e. g. http://www.deutschestextarchiv.de/dtaq/book/view/16258?p=10 -->
      <xsl:when test="tei:castGroup">
        <table class="dta-castgroup">
          <td>
            <xsl:apply-templates/>
          </td>
          <td class="roledesc">
            <xsl:apply-templates select="tei:roleDesc"/>
          </td>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <table class="dta-castgroup">
          <xsl:for-each select='tei:castItem'>
            <tr>
              <td class="castitem">
                <xsl:apply-templates/>
              </td>
              <xsl:if test="position()=1">
                <xsl:element name="td">
                  <xsl:attribute name="class">roledesc</xsl:attribute>
                  <xsl:attribute name="rowspan">
                    <xsl:value-of select="count(../tei:castItem)"/>
                  </xsl:attribute>
                  <xsl:apply-templates select="../tei:roleDesc"/>
                </xsl:element>
              </xsl:if>
              <xsl:if test="tei:actor">
                <td class="dta-actor">
                  <xsl:apply-templates select="tei:actor"/>
                </td>
              </xsl:if>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="tei:actor">
    <span class="dta-actor">
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:castItem[not(parent::tei:castGroup)]'>
    <div class="castitem">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:castList/tei:head'>
    <h2 class="head">
      <xsl:apply-templates/>
    </h2>
  </xsl:template>

  <xsl:template match='tei:role'>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match='tei:speaker'>
    <span class="speaker">
      <xsl:text> </xsl:text>
      <xsl:apply-templates/>
      <xsl:text> </xsl:text>
    </span>
  </xsl:template>

  <xsl:template match='tei:stage'>
    <xsl:choose>
      <xsl:when test="ancestor::tei:sp">
        <span class="stage">
          <xsl:text> </xsl:text>
          <xsl:apply-templates/>
          <xsl:text> </xsl:text>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <div class="stage">
          <xsl:apply-templates/>
        </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  <!-- end dramae -->

  <!-- poems -->
  <xsl:template match='tei:lg[@type="poem"]/tei:head'>
    <div class="head">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:lg[@type="poem"]'>
    <div class="poem">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:lg[not(@type="poem")]'>
    <div class="dta-lg">
      <xsl:apply-templates/>
    </div>
  </xsl:template>
  <!-- end poems -->

  <!-- letters -->
  <xsl:template match='tei:salute'>
    <xsl:element name="div">
      <xsl:attribute name="class">dta-salute
        <xsl:choose>
          <xsl:when test="@rendition=''"/>
          <xsl:when test="contains(normalize-space(@rendition),' ')">
            <xsl:call-template name="splitRendition">
              <xsl:with-param name="value">
                <xsl:value-of select="normalize-space(@rendition)"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="findRendition">
              <xsl:with-param name="value">
                <xsl:value-of select="@rendition"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:dateline'>
    <span class="dta-dateline">
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:closer'>
    <div class="dta-closer">
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </div>
  </xsl:template>
  <!-- end letters -->

  <xsl:template match='tei:div'>
    <xsl:element name="div">
      <xsl:choose>
        <xsl:when test="@type='advertisment' or @type='advertisement'">
          <div class="dta-anzeige">
            <xsl:apply-templates/>
          </div>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="class">
            <xsl:value-of select="@type"/>
          </xsl:attribute>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match="tei:sp">
    <div class="dta-sp">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="tei:spGrp">
    <xsl:choose>
      <xsl:when test="child::*[1][self::tei:stage][@rendition='#rightBraced']">
        <table>
          <tr>
            <td style="vertical-align:middle">
              <xsl:apply-templates select="child::*[1]"/>
            </td>
            <td class="braced-base braced-left">
              <xsl:for-each select="tei:sp">
                <div class="dta-sp">
                  <xsl:apply-templates/>
                </div>
              </xsl:for-each>
            </td>
          </tr>
        </table>
      </xsl:when>
      <xsl:when test="child::*[last()][self::tei:stage][@rendition='#leftBraced']">
        <table>
          <tr>
            <td class="braced-base braced-right">
              <xsl:for-each select="tei:sp">
                <div class="dta-sp">
                  <xsl:apply-templates/>
                </div>
              </xsl:for-each>
            </td>
            <td style="vertical-align:middle">
              <xsl:apply-templates select="child::*[last()]"/>
            </td>
          </tr>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:p'>
    <xsl:choose>
      <xsl:when test="ancestor::tei:sp and name(preceding-sibling::*[2]) != 'p'">
        <span class="dta-in-sp">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:when
          test="ancestor::tei:sp and local-name(preceding-sibling::node()[1]) != 'lb' and local-name(preceding-sibling::node()[1]) != 'pb'">
        <span class="dta-in-sp">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:when test="ancestor::tei:sp and local-name(preceding-sibling::node()[1]) = 'lb'">
        <p class="dta-p-in-sp-really">
          <xsl:apply-templates/>
        </p>
      </xsl:when>
      <xsl:when test="@rendition">
        <p>
          <xsl:call-template name="applyRendition"/>
          <xsl:apply-templates/>
        </p>
      </xsl:when>
      <xsl:when test="@prev">
        <p class="dta-no-indent">
          <xsl:apply-templates/>
        </p>
      </xsl:when>
      <xsl:otherwise>
        <p class="dta-p">
          <xsl:apply-templates/>
        </p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:argument'>
    <div class="dta-argument">
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match='tei:c'>
    <xsl:element name="span">
      <xsl:attribute name="id">
        <xsl:value-of select="@xml:id"/>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:l'>
    <xsl:choose>
      <xsl:when test="contains(@rendition,'#c') or contains(@rendition,'#et') or contains(@rendition,'#right')">
        <xsl:element name="div">
          <xsl:if test="@rendition">
            <xsl:call-template name="applyRendition"/>
          </xsl:if>
          <xsl:element name="span">
            <xsl:call-template name="applyXmlId"/>
            <xsl:call-template name="applyPrev"/>
            <xsl:call-template name="applyNext"/>
            <xsl:apply-templates/>
          </xsl:element>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="span">
          <xsl:if test="@rendition">
            <xsl:call-template name="applyRendition"/>
          </xsl:if>
          <xsl:call-template name="applyXmlId"/>
          <xsl:call-template name="applyPrev"/>
          <xsl:call-template name="applyNext"/>
          <xsl:apply-templates/>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:lb'>
    <xsl:if test="@n">
      <span class="dta-lb-n">
        <xsl:apply-templates select="@n"/>
      </span>
    </xsl:if>
    <xsl:if test="local-name(preceding-sibling::*[1]) != 'item'">
      <br/>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:template>


  <xsl:template match='tei:floatingText'>
    <xsl:element name="div">
      <xsl:attribute name="class">
        dta-floatingtext
        <xsl:choose>
          <xsl:when test="@rendition=''"/>
          <xsl:when test="contains(normalize-space(@rendition),' ')">
            <xsl:call-template name="splitRendition">
              <xsl:with-param name="value">
                <xsl:value-of select="normalize-space(@rendition)"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="findRendition">
              <xsl:with-param name="value">
                <xsl:value-of select="@rendition"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:attribute>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <!-- @prev/@next stuff -->
  <xsl:template name="applyPrev">
    <xsl:if test="@prev">
      <xsl:attribute name="data-prev">
        <xsl:value-of select="@prev"/>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template name="applyNext">
    <xsl:if test="@next">
      <xsl:attribute name="data-next">
        <xsl:value-of select="@next"/>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

  <xsl:template name="applyXmlId">
    <xsl:if test="@xml:id">
      <xsl:attribute name="data-xmlid">
        <xsl:value-of select="@xml:id"/>
      </xsl:attribute>
    </xsl:if>
  </xsl:template>

  <!-- renditions -->
  <xsl:template match='tei:hi'>
    <xsl:element name="span">
      <xsl:if test="@rendition">
        <xsl:call-template name="applyRendition"/>
      </xsl:if>
      <xsl:if test="@rend">
        <xsl:attribute name="class">dta-rend</xsl:attribute>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template name="applyRendition">
    <xsl:attribute name="class">
      <xsl:choose>
        <xsl:when test="@rendition=''"/>
        <xsl:when test="contains(normalize-space(@rendition),' ')">
          <xsl:call-template name="splitRendition">
            <xsl:with-param name="value">
              <xsl:value-of select="normalize-space(@rendition)"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="findRendition">
            <xsl:with-param name="value">
              <xsl:value-of select="@rendition"/>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="splitRendition">
    <xsl:param name="value"/>
    <xsl:choose>
      <xsl:when test="$value=''"/>
      <xsl:when test="contains($value,' ')">
        <xsl:call-template name="findRendition">
          <xsl:with-param name="value">
            <xsl:value-of select="substring-before($value,' ')"/>
          </xsl:with-param>
        </xsl:call-template>
        <xsl:call-template name="splitRendition">
          <xsl:with-param name="value">
            <xsl:value-of select="substring-after($value,' ')"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="findRendition">
          <xsl:with-param name="value">
            <xsl:value-of select="$value"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="findRendition">
    <xsl:param name="value"/>
    <xsl:choose>
      <xsl:when test="starts-with($value,'#')">
        <xsl:value-of select="substring-after($value,'#')"/>
        <xsl:text> </xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:for-each select="document($value)">
          <xsl:apply-templates select="@xml:id"/>
          <xsl:text> </xsl:text>
        </xsl:for-each>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- end renditions -->

  <xsl:template match='tei:cit'>
    <span class="dta-cit">
      <xsl:if test="@xml:id">
        <xsl:attribute name="data-id">
          <xsl:value-of select="@xml:id"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@prev">
        <xsl:attribute name="data-prev">
          <xsl:value-of select="@prev"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="@next">
        <xsl:attribute name="data-next">
          <xsl:value-of select="@next"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:epigraph'>
    <blockquote class="quote">
      <xsl:apply-templates/>
    </blockquote>
  </xsl:template>

  <xsl:template match='tei:quote'>
    <q class="quote">
      <xsl:apply-templates/>
    </q>
  </xsl:template>

  <xsl:template match='tei:q'>
    <q class="quote">
      <xsl:apply-templates/>
    </q>
  </xsl:template>

  <xsl:template match="tei:list">
    <xsl:choose>
      <xsl:when test='@rend="braced"'>
        <table class="list">
          <xsl:choose>
            <xsl:when test="tei:trailer">
              <xsl:for-each select='tei:item'>
                <tr>
                  <td class="item-left">
                    <xsl:apply-templates/>
                  </td>
                  <xsl:if test="position()=1">
                    <xsl:element name="td">
                      <xsl:attribute name="class">dta-list-trailer</xsl:attribute>
                      <xsl:attribute name="rowspan">
                        <xsl:value-of select="count(../tei:item)"/>
                      </xsl:attribute>
                      <xsl:apply-templates select="../tei:trailer"/>
                    </xsl:element>
                  </xsl:if>
                </tr>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise><!-- tei:head -->
              <xsl:for-each select='tei:item'>
                <tr>
                  <xsl:if test="position()=1">
                    <xsl:element name="td">
                      <xsl:attribute name="class">dta-list-head</xsl:attribute>
                      <xsl:attribute name="rowspan">
                        <xsl:value-of select="count(../tei:item)"/>
                      </xsl:attribute>
                      <xsl:apply-templates select="../tei:head"/>
                    </xsl:element>
                  </xsl:if>
                  <td class="item-right">
                    <xsl:apply-templates/>
                  </td>
                </tr>
              </xsl:for-each>
            </xsl:otherwise>
          </xsl:choose>
        </table>
      </xsl:when>
      <xsl:when test='@rendition="#leftBraced"'>
        <span class="braced-base braced-left">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:when test='@rendition="#rightBraced"'>
        <span class="braced-base braced-right">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:when test='@rendition="#leftBraced #rightBraced"'>
        <span class="braced-base braced-left-right">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <div class="dta-list">
          <xsl:apply-templates/>
        </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="tei:item">
    <xsl:choose>
      <xsl:when test="ancestor::tei:p">
        <span class="dta-list-item">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <div class="dta-list-item">
          <xsl:apply-templates/>
        </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:table'>
    <xsl:choose>
      <xsl:when test="not(string(.)) or not(normalize-space(.))">
        <div class="gap">[Tabelle]</div>
      </xsl:when>
      <xsl:otherwise>
        <table class="dta-table">
          <xsl:if test="tei:head">
            <caption>
              <xsl:apply-templates select="tei:head"/>
            </caption>
          </xsl:if>
          <xsl:for-each select="tei:row">
            <tr>
              <xsl:for-each select="tei:cell">
                <xsl:choose>
                  <xsl:when test="../@role='label'">
                    <xsl:element name="th">
                      <xsl:apply-templates/>
                    </xsl:element>
                  </xsl:when>
                  <xsl:otherwise>
                    <xsl:element name="td">
                      <xsl:if test="@cols">
                        <xsl:attribute name="colspan">
                          <xsl:value-of select="@cols"/>
                        </xsl:attribute>
                      </xsl:if>
                      <xsl:if test="@rows">
                        <xsl:attribute name="rowspan">
                          <xsl:value-of select="@rows"/>
                        </xsl:attribute>
                      </xsl:if>
                      <xsl:if test="@rendition='#c'">
                        <xsl:attribute name="style">text-align:center</xsl:attribute>
                      </xsl:if>
                      <xsl:if test="@rendition='#right'">
                        <xsl:attribute name="style">text-align:right</xsl:attribute>
                      </xsl:if>
                      <xsl:if test="@rendition='#et'">
                        <xsl:attribute name="style">padding-left:2em</xsl:attribute>
                      </xsl:if>
                      <xsl:apply-templates/>
                    </xsl:element>
                  </xsl:otherwise>
                </xsl:choose>
              </xsl:for-each>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:opener'>
    <span class="dta-opener">
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:trailer'>
    <span class="dta-trailer">
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:ref'>
    <xsl:element name="span">
      <xsl:attribute name="class">ref</xsl:attribute>
      <xsl:choose>
        <xsl:when test="starts-with(@target, 'http')">
          <xsl:element name="a">
            <xsl:attribute name="href">
              <xsl:value-of select="@target"/>
            </xsl:attribute>
            <xsl:apply-templates/>
          </xsl:element>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:element>
  </xsl:template>

  <xsl:template match='tei:bibl'>
    <span class="dta-bibl">
      <xsl:call-template name="applyRendition"/>
      <xsl:apply-templates/>
    </span>
  </xsl:template>

  <xsl:template match='tei:space[@dim="horizontal"]'>
    <xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;&amp;nbsp;</xsl:text>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match='tei:space[@dim="vertical"]'>
    <br class="space"/>
  </xsl:template>

  <xsl:template match='tei:supplied'>
    <span class="dta-supplied">
      <xsl:text>[</xsl:text>
      <xsl:apply-templates/>
      <xsl:text>]</xsl:text>
    </span>
  </xsl:template>

  <xsl:template match='tei:foreign'>
    <xsl:choose>
      <xsl:when test="not(child::node()) and @xml:lang">
        <span class="dta-foreign" title="fremdsprachliches Material">FM:
          <xsl:choose>
            <xsl:when test="@xml:lang='he' or @xml:lang='heb' or @xml:lang='hbo'">hebräisch</xsl:when>
            <xsl:when test="@xml:lang='el' or @xml:lang='grc' or @xml:lang='ell'">griechisch</xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@xml:lang"/>
            </xsl:otherwise>
          </xsl:choose>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match='tei:name[@type="artificialWork"]'>
    <span title="Kunstwerk oder Gebäude">
      <xsl:call-template name="link-ref">
        <xsl:with-param name="value">
          <xsl:value-of select="@ref"/>
        </xsl:with-param>
      </xsl:call-template>
    </span>
  </xsl:template>

  <xsl:template match='tei:persName'>
    <span title="Personenname">
      <xsl:call-template name="link-ref">
        <xsl:with-param name="value">
          <xsl:value-of select="@ref"/>
        </xsl:with-param>
      </xsl:call-template>
    </span>
  </xsl:template>

  <xsl:template match='tei:placeName'>
    <span title="Ortsname">
      <xsl:call-template name="link-ref">
        <xsl:with-param name="value">
          <xsl:value-of select="@ref"/>
        </xsl:with-param>
      </xsl:call-template>
    </span>
  </xsl:template>

  <xsl:template match='tei:metamark'>
    <xsl:choose>
      <xsl:when test="text() != ''">
        <span class="dta-metamark" title="metamark">
          <xsl:apply-templates/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <span class="dta-metamark" title="metamark">
          <xsl:text>&#x23A1;</xsl:text>
        </span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="link-ref">
    <xsl:param name="value"/>
    <xsl:choose>
      <xsl:when test="$value and starts-with($value,'http')">
        <xsl:element name="a">
          <xsl:attribute name="href">
            <xsl:choose>
              <xsl:when test="contains($value,' ')">
                <xsl:value-of select="substring-before($value,' ')"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="$value"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:apply-templates/>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>

</xsl:stylesheet>