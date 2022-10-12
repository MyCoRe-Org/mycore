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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:mods="http://www.loc.gov/mods/v3"
                version="3.0">

    <xsl:mode on-no-match="shallow-copy" />

    <xsl:param name="MCR.MODS.Migration.CovertLabelList" />

    <xsl:template match="mods:extension[@displayLabel]">
        <xsl:variable name="displayLabels" select="tokenize($MCR.MODS.Migration.CovertLabelList, ',')" />
        <xsl:choose>
            <xsl:when test="@displayLabel=$displayLabels">
                <xsl:copy>
                    <xsl:attribute name="type"><xsl:value-of select="@displayLabel" /></xsl:attribute>
                    <xsl:apply-templates />
                </xsl:copy>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="." />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>