<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
      <!-- =================================================================================================== -->
<!--
Template: wcms.getBrowserAddress
synopsis: The template will be used to identify the currently selected menu entry and the belonging element item/@href in the navigationBase
These strategies are embarked on:
1. RequestURL - lang ?= @href - lang
2. RequestURL - $WebApplicationBaseURL - lang ?= @href - lang

3. Root element ?= item//dynamicContentBinding/rootTag
-->
      <xsl:template name="wcms.getBrowserAddress">
		
            <xsl:variable name="RequestURL.langDel" >
			<xsl:call-template name="UrlDelParam"> 
			      <xsl:with-param name="url" select="$RequestURL"  />
	                  <xsl:with-param name="par" select="'lang'" /> 
                  </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="RequestURL.WebURLDel" >
			<xsl:value-of select="concat('/',substring-after($RequestURL,$WebApplicationBaseURL))" />
            </xsl:variable>
            <xsl:variable name="RequestURL.WebURLDel.langDel" >
			<xsl:call-template name="UrlDelParam"> 
			      <xsl:with-param name="url" select="$RequestURL.WebURLDel"  />
	                  <xsl:with-param name="par" select="'lang'" /> 
                  </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="completeRootNode" select="document($navigationBase)/navigation" />

            <!-- test if navigation.xml contains the current browser address -->
            <xsl:variable name="browserAddress_href" >
                  <!-- verify each item  -->
                  <xsl:for-each select="$completeRootNode//item[@href]" >
                        <!-- remove par lang from @href -->
                        <xsl:variable name="href.langDel">
					<xsl:call-template name="UrlDelParam"> 
					      <xsl:with-param name="url" select="current()/@href"  />
			                  <xsl:with-param name="par" select="'lang'" /> 
		                  </xsl:call-template>                              
                        </xsl:variable>

	                  <xsl:if test="( $RequestURL.langDel = $href.langDel )
                              or
                              ($RequestURL.WebURLDel.langDel = $href.langDel) ">
                              <xsl:value-of select="@href" />
                        </xsl:if>
                        
	            </xsl:for-each>
                  <!-- END OF: verify each item -->
            </xsl:variable>
            
            <!-- look for appropriate dynamicContentBinding/rootTag -> $browserAddress_dynamicContentBinding -->
            <xsl:variable name="browserAddress_dynamicContentBinding" >
                  <xsl:if test=" $browserAddress_href = '' " >
                        <!-- assign name of rootTag -> $rootTag -->
                        <xsl:variable name="rootTag" select="name(*)" />
                        <xsl:for-each select="$completeRootNode//dynamicContentBinding/rootTag" >
                              <xsl:if test=" current() = $rootTag " >
                                    <xsl:for-each select="ancestor-or-self::*[@href]">
                                          <xsl:if test="position()=last()" >
                                                <xsl:value-of select="@href" />
                                          </xsl:if>
                                    </xsl:for-each>
                              </xsl:if>
                        </xsl:for-each>
                  </xsl:if>
            </xsl:variable>
            <!-- END OF: look for appropriate dynamicContentBinding/rootTag -> $browserAddress_dynamicContentBinding -->
            <!-- END OF: test if navigation.xml contains the current browser address -->
            <!-- assign right browser address -->
            <xsl:choose>
                  <xsl:when test=" $browserAddress_href != '' " >
                        <xsl:value-of select="$browserAddress_href" />
                  </xsl:when>
                  <xsl:when test=" $browserAddress_dynamicContentBinding != '' " >
                        <xsl:value-of select="$browserAddress_dynamicContentBinding" />
                  </xsl:when>
                  <xsl:otherwise>
                        <xsl:value-of select="'nichts'" />                        
                  </xsl:otherwise>
            </xsl:choose>
            <!-- END OF: assign right browser address -->

      </xsl:template>
      <!-- =================================================================================================== -->
      <xsl:template name="wcms.getTemplate">
           
            <xsl:param name="browserAddress" />            
           
            <!-- point to rigth item -->
            <xsl:for-each select="document($navigationBase) /navigation//item[@href = $browserAddress]" >
                  <!-- collect @template !='' entries along the choosen axis -->
                  <xsl:for-each select="ancestor-or-self::*[  @template != '' ]">
                        <xsl:if test="position()=last()" >
                              <xsl:value-of select="@template" />
                        </xsl:if>
                  </xsl:for-each>
                  <!-- END OF: collect @template !='' entries along the choosen axis -->
            </xsl:for-each>
            <!-- END OF: point to rigth item -->
      </xsl:template>
      <!-- =================================================================================================== -->
</xsl:stylesheet>