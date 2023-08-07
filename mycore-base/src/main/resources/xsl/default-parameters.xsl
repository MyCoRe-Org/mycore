<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <!--
    Contains all parameters that are set by the MCRParameterCollector.
    * MCRParameterCollector.java
        * setFromRequestHeader
            - RequestURL
            - Referer
            - UserAgent
        * setUnmodifyableParameters
            - CurrentUser
            - CurrentLang
            - WebApplicationBaseURL
            - ServletsBaseURL
            - DefaultLang
            - User-Agent
        * setSessionID
            - HttpSession
            - JSessionID
    -->

    <xsl:param name="RequestURL" />
    <xsl:param name="Referer" />
    <xsl:param name="UserAgent" />
    <xsl:param name="CurrentUser" />
    <xsl:param name="CurrentLang" />
    <xsl:param name="WebApplicationBaseURL" />
    <xsl:param name="ServletsBaseURL" />
    <xsl:param name="DefaultLang" />
    <xsl:param name="User-Agent" />
    <xsl:param name="HttpSession" />
    <xsl:param name="JSessionID" />


</xsl:stylesheet>