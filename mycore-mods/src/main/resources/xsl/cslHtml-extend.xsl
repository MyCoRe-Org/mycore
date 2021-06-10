<?xml version="1.0"?>
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
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:html="http://www.w3.org/1999/xhtml">


    <xsl:mode on-no-match="shallow-copy"/>

    <xsl:template match="html:div">
        <xsl:copy>
            <xsl:call-template name="applyStyle"/>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates />
        </xsl:copy>
    </xsl:template>

    <xsl:template name="applyStyle">
        <xsl:variable name="style">
            <xsl:if test="contains(@class, 'csl-entry')">
                <xsl:text>font-size: 12pt;padding-top: 10px;width: 90%;</xsl:text>
            </xsl:if>
            <xsl:if test="contains(@class, 'csl-left-margin')">
                <xsl:text>float: left;display: inline;padding-right: 0.5em;</xsl:text>
            </xsl:if>
            <xsl:if test="contains(@class, 'csl-right-inline')">
                <xsl:text>display: inline;</xsl:text>
            </xsl:if>
        </xsl:variable>
        <xsl:if test="string-length($style)&gt;0">
            <xsl:attribute name="style">
                <xsl:value-of select="$style"/>
            </xsl:attribute>
        </xsl:if>
    </xsl:template>


</xsl:stylesheet>