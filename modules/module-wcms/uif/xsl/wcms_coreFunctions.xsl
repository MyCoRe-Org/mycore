<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
      <!-- =================================================================================================== -->
      <xsl:template name="wcms.getBrowserAddress">
            <!-- GET BROWSER ADDRESS -> $browserAddress-->
            <!-- test if navigation.xml contains the current browser address -->
            <xsl:variable name="completeRootNode" select="document($navigationBase)/navigation" />
            <xsl:variable name="browserAddress_extern" >
                  <!-- verify each item with @type="extern" -->
                  <xsl:for-each select="$completeRootNode//item[@type='extern']" >
                        <!-- cut if neccesary the lang parameter at the end of the URL -->
                        <xsl:choose>
                              <xsl:when test="current()[@href = $RequestURL ]">
                                    <xsl:value-of select="$RequestURL" />
                              </xsl:when>
                              <xsl:when test="current()[@href = (substring-before($RequestURL, concat('?lang=',$CurrentLang) ))]">
                                    <xsl:value-of select="(substring-before($RequestURL, concat('?lang=',$CurrentLang) ))" />
                              </xsl:when>
                        </xsl:choose>
                  </xsl:for-each>
                  <!-- END OF: verify each item with @type="extern" -->
            </xsl:variable>
            <!-- verify each item with @type="intern" -->
            <xsl:variable name="browserAddress_intern" >
                  <xsl:if test=" $browserAddress_extern = '' " >
                        <xsl:for-each select="$completeRootNode//item[@type='intern']" >
                              <!-- cut if neccesary the lang parameter at the end of the URL -->
                              <xsl:choose>
                                    <!-- test if normal webapp path reduced by one doubled '/' -->
                                    <xsl:when 
                                          test=" current()[@href = concat('/',substring-after($RequestURL,$WebApplicationBaseURL)) ] ">
                                          <xsl:value-of select="concat('/',substring-after($RequestURL,$WebApplicationBaseURL))" 
                                                />
                                    </xsl:when>
                                    <!-- lang attribute at end cuted -->
                                    <xsl:when 
                                          test="current()[@href = (substring-before(concat('/',substring-after($RequestURL,$WebApplicationBaseURL)), concat('?lang=',$CurrentLang) ))]">
                                          <xsl:value-of 
                                                select="(substring-before(concat('/',substring-after($RequestURL,$WebApplicationBaseURL)), concat('?lang=',$CurrentLang) ))" 
                                                />
                                    </xsl:when>
                              </xsl:choose>
                        </xsl:for-each>
                  </xsl:if>
            </xsl:variable>
            <!-- END OF: verify each item with @type="intern" -->
            <!-- look for appropriate dynamicContentBinding/rootTag -> $browserAddress_dynamicContentBinding -->
            <xsl:variable name="browserAddress_dynamicContentBinding" >
                  <xsl:if test=" $browserAddress_extern = '' and $browserAddress_intern = '' " >
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
                  <xsl:when test=" $browserAddress_extern != '' " >
                        <xsl:value-of select="$browserAddress_extern" />
                  </xsl:when>
                  <xsl:when test=" $browserAddress_intern != '' " >
                        <xsl:value-of select="$browserAddress_intern" />
                  </xsl:when>
                  <xsl:when test=" $browserAddress_dynamicContentBinding != '' " >
                        <xsl:value-of select="$browserAddress_dynamicContentBinding" />
                  </xsl:when>
            </xsl:choose>
            <!-- END OF: assign right browser address -->
            <!-- END OF: GET BROWSER ADDRESS -> $browserAddress-->
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