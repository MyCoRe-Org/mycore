<?xml version="1.0"?>
<!--
  ~ This file is part of ***  M y C o R e  ***
  ~ See https://www.mycore.de/ for details.
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
  ~
  ~
  -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:str="http://exslt.org/strings"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:exslt="http://exslt.org/common"
                xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
                xmlns:csl="http://purl.org/net/xbiblio/csl"
                extension-element-prefixes="str"
                exclude-result-prefixes="xsl xalan exslt i18n csl"
>


    <xsl:param name="MCR.Export.Transformers"/>
    <xsl:param name="MCR.Export.CSL.Styles"/>
    <xsl:param name="MCR.Export.CSL.Rows"/>
    <xsl:param name="MCR.Export.Standalone.Rows"/>
    <xsl:param name="WebApplicationBaseURL"/>
    <xsl:param name="MCR.ContentTransformer.solr2csv.Stylesheet"/>

    <xsl:template name="exportGUI">
        <!-- supply
         response -> for search response
         basket -> for basket
         object -> for single mycoreobject
         -->
        <xsl:param name="type"/>
        <xsl:variable name="transformerTokens">
            <xsl:call-template name="Tokenizer">
                <xsl:with-param name="string" select="$MCR.Export.Transformers"/>
                <xsl:with-param name="delimiter" select="','"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="transformers" select="exslt:node-set($transformerTokens)/token" />
        <xsl:variable name="standaloneFormats">
            <xsl:value-of select="substring-before($transformers[1],':')"/>
            <xsl:for-each select="$transformers[position()>1]">
                <xsl:text>,</xsl:text>
                <xsl:value-of select="substring-before(.,':')"/>
            </xsl:for-each>
        </xsl:variable>
        <script type="text/javascript">
            window["MCR.Export.StandaloneFormats"] = &quot;<xsl:value-of select="$standaloneFormats"/>&quot;;
            window["MCR.Export.CSL.Rows"] = &quot;<xsl:value-of select="$MCR.Export.CSL.Rows"/>&quot;;
            window["MCR.Export.Standalone.Rows"] = &quot;<xsl:value-of select="$MCR.Export.Standalone.Rows"/>&quot;;
        </script>
        <script src="{$WebApplicationBaseURL}js/csl-export.js"/>
        <div class="input-group mb-3 flex-wrap" data-export="{$type}">
            <select name="format" class="form-control">
                <option value="">
                    <xsl:value-of select="i18n:translate('component.mods.csl.export.format')"/>
                </option>
                <option value="pdf">PDF</option>
                <option value="html">HTML</option>
                <xsl:if test="$type ='response'">
                    <xsl:if test="string-length($MCR.ContentTransformer.solr2csv.Stylesheet)&gt;0">
                        <option value="solr2csv">CSV</option>
                    </xsl:if>
                </xsl:if>
                <xsl:if test="$type = 'basket'">
                    <xsl:for-each select="$transformers">
                        <option value="{substring-before(.,':')}">
                            <xsl:value-of select="substring-after(.,':')"/>
                        </option>
                    </xsl:for-each>
                </xsl:if>
            </select>
            <select name="style">
                <xsl:attribute name="class">
                    <xsl:text>form-control</xsl:text>
                    <xsl:if test="$type = 'basket'">
                        <xsl:text> d-none</xsl:text>
                    </xsl:if>
                </xsl:attribute>
                <option value="">
                    <xsl:value-of select="i18n:translate('component.mods.csl.export.style')"/>
                </option>
                <xsl:call-template name="styles2Options"/>
            </select>
            <div class="input-group-append">
                <a href="#" class="action btn btn-md btn-csl-export" data-trigger-export="true">
                    <xsl:value-of select="i18n:translate('component.mods.csl.export')"/>

                </a>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="styles2Options">
        <xsl:for-each select="str:tokenize($MCR.Export.CSL.Styles, ',')">
            <option value="{text()}">
                <xsl:variable name="cslDocument" select="document(concat('resource:', text(), '.csl'))"/>
                <xsl:variable name="title" select="$cslDocument/csl:style/csl:info/csl:title"/>
                <xsl:variable name="short-title" select="$cslDocument/csl:style/csl:info/csl:title-short"/>

                <xsl:choose>
                    <xsl:when test="string-length($short-title) &gt; 0">
                        <xsl:value-of select="concat($short-title, ' (', $title, ')')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$title" />
                    </xsl:otherwise>
                </xsl:choose>
            </option>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
