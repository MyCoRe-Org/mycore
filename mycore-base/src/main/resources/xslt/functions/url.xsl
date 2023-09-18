<?xml version="1.0"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:mcrurl="http://www.mycore.de/xslt/url"
                exclude-result-prefixes="fn xs">

    <xsl:function name="mcrurl:get-param" as="xs:string">
        <xsl:param name="url" as="xs:string"/>
        <xsl:param name="par" as="xs:string"/>

        <xsl:variable name="afterParam">
            <xsl:choose>
                <xsl:when test="contains($url,concat('?',$par,'='))">
                    <!-- Parameter is right after a question mark //-->
                    <xsl:value-of select="substring-after($url,concat('?',$par,'='))" />
                </xsl:when>
                <xsl:when test="contains($url,concat('&amp;',$par,'='))">
                    <!-- Parameter is right after a & sign //-->
                    <xsl:value-of select="substring-after($url,concat('&amp;',$par,'='))" />
                </xsl:when>
                <xsl:otherwise>
                    <!-- The parameter is not specified //-->
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="contains($afterParam,'&amp;')">
                <!-- cut off other parameters -->
                <xsl:value-of select="substring-before($afterParam,'&amp;')" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$afterParam" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>


    <xsl:function name="mcrurl:add-session" as="xs:string">
        <xsl:param name="url" />
        <!-- There are two possibility for a parameter to appear in an URL:
            1.) after a ? sign
            2.) after a & sign
            In both cases the value is either limited by a & sign or the string end
            //-->
        <xsl:choose>
            <xsl:when test="starts-with($url,$WebApplicationBaseURL)">
                <!--The document is on our server-->
                <xsl:variable name="pathPart">
                    <xsl:choose>
                        <xsl:when test="contains($url,'?')">
                            <xsl:value-of select="substring-before($url,'?')" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$url" />
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="queryPart">
                    <xsl:value-of select="substring-after($url,$pathPart)" />
                </xsl:variable>
                <xsl:value-of select="concat($pathPart,$HttpSession,$queryPart)" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$url" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="mcrurl:delete-session" as="xs:string">
        <xsl:param name="url" />
        <xsl:value-of select="replace($url, ';jsessionid=[^?#]+', '')" />
    </xsl:function>


    <xsl:function name="mcrurl:set-param">
        <xsl:param name="url" />
        <xsl:param name="par" />
        <xsl:param name="value" />
        <!-- There are two possibility for a parameter to appear in an URL:
            1.) after a ? sign
            2.) after a & sign
            In both cases the value is either limited by a & sign or the string end
            //-->
        <xsl:variable name="asFirstParam">
            <xsl:value-of select="concat('?',$par,'=')" />
        </xsl:variable>
        <xsl:variable name="asOtherParam">
            <xsl:value-of select="concat('&amp;',$par,'=')" />
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="contains($url,$asFirstParam) or contains($url,$asOtherParam)">
                <!-- Parameter is present -->
                <xsl:variable name="asParam">
                    <xsl:choose>
                        <xsl:when test="contains($url,$asFirstParam)">
                            <!-- Parameter is right after a question mark //-->
                            <xsl:value-of select="$asFirstParam" />
                        </xsl:when>
                        <xsl:when test="contains($url,$asOtherParam)">
                            <!-- Parameter is right after a & sign //-->
                            <xsl:value-of select="$asOtherParam" />
                        </xsl:when>
                    </xsl:choose>
                </xsl:variable>
                <xsl:variable name="newurl">
                    <xsl:value-of select="substring-before($url,$asParam)" />
                    <xsl:value-of select="$asParam" />
                    <xsl:value-of select="$value" />
                    <xsl:if test="contains(substring-after($url,$asParam),'&amp;')">
                        <!--OK now we know that there are parameters left //-->
                        <xsl:value-of select="concat('&amp;',substring-after(substring-after($url,$asParam),'&amp;'))" />
                    </xsl:if>
                    <xsl:if test="contains($url, '#')">
                        <xsl:value-of select="concat('#',substring-after($url, '#'))" />
                    </xsl:if>
                </xsl:variable>
                <xsl:value-of select="$newurl" />
            </xsl:when>
            <xsl:otherwise>
                <!-- The parameter is not yet specified //-->
                <xsl:choose>
                    <xsl:when test="contains($url,'?')">
                        <!-- Other parameters are present //-->
                        <xsl:value-of select="concat($url,'&amp;',$par,'=',$value)" />
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- No other parameter are present //-->
                        <xsl:value-of select="concat($url,'?',$par,'=',$value)" />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>
