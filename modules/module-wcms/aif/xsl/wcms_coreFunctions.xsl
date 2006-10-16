<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation">
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
            <!-- test if navigation.xml contains the current browser address -->
            <xsl:variable name="browserAddress_href" >
                  <!-- verify each item  -->
                  <xsl:for-each select="$loaded_navigation_xml//item[@href]" >
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
                        <xsl:for-each select="$loaded_navigation_xml//dynamicContentBinding/rootTag" >
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
            </xsl:choose>
            <!-- END OF: assign right browser address -->

      </xsl:template>
      <!-- =================================================================================================== -->
      <xsl:template name="wcms.getTemplate">
            <xsl:param name="browserAddress" />            
            <xsl:param name="navigationBase" />                        
           
           <xsl:variable name="template_tmp">
	            <!-- point to rigth item -->
	            <xsl:for-each select="$loaded_navigation_xml//item[@href = $browserAddress]" >
	                  <!-- collect @template !='' entries along the choosen axis -->
	                  <xsl:for-each select="ancestor-or-self::*[  @template != '' ]">
	                        <xsl:if test="position()=last()" >
	                              <xsl:value-of select="@template" />
	                        </xsl:if>
	                  </xsl:for-each>
	                  <!-- END OF: collect @template !='' entries along the choosen axis -->
	            </xsl:for-each>
	            <!-- END OF: point to rigth item -->
            </xsl:variable>
	            
		<xsl:choose>
                  <!-- assign appropriate template -->
                  <xsl:when test="$template_tmp != ''">
                  	<xsl:value-of select="$template_tmp" />                        
                  </xsl:when>
                  <!-- default template -->
                  <xsl:otherwise>
                        <xsl:value-of select="$loaded_navigation_xml/@template" />
                  </xsl:otherwise>
            </xsl:choose>
            
      </xsl:template>

<!-- ====================================================================================={
section: Template: name="menuleiste"

	- Erzeugt das Menue fuer das wcms

parameters:
	menupunkt - Name des hervorgehobenen Menuepunktes
}===================================================================================== -->

	<xsl:template name="menuleiste">
		<xsl:param name="menupunkt" />

		<div id="nav">

			<!-- rechte Seite -->
			<div class="rightmenu">
				<ul class="menu">
					<li>
						<a href="{$WebApplicationBaseURL}modules/module-wcms/aif/web/login.xml">
							<xsl:choose>
								<xsl:when test="$menupunkt='Anmelden'">
									<xsl:attribute name="class">
										<xsl:value-of select="'current'"/>
									</xsl:attribute>
									<xsl:value-of select="i18n:translate('wcms.labels.login')"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="i18n:translate('wcms.labels.logout')"/>
								</xsl:otherwise>
							</xsl:choose>
						</a>
					</li>
					<li>
						<a href="{$WebApplicationBaseURL}">
							<xsl:value-of select="i18n:translate('wcms.labels.close')"/>
						</a>
					</li>					
				</ul>
			</div>

			<!-- linke Seite -->
			<xsl:if test="$menupunkt!='Anmelden'">

				<div class="leftmenu">
					<ul class="menu">
						<li>
							<a href="{$ServletsBaseURL}WCMSAdminServlet{$JSessionID}?action=choose">
								<xsl:if test="$menupunkt='Bearbeiten'">
									<xsl:attribute name="class">
										<xsl:value-of select="'current'"/>
									</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="i18n:translate('wcms.labels.edit')"/>
							</a>
						</li>
						<li>
							<script type="text/javascript">								
								function OpenWindow (address) {
								  MyWindow = window.open(address, "Multimedia", "width=400,height=350,scrollbars=yes");
								  MyWindow.window.moveTo(screen.width*0.5,screen.height*0.2);
								  MyWindow.focus();
								}
							</script>	
							<a href="{$WebApplicationBaseURL}modules/module-wcms/aif/web/multimedia.xml{$JSessionID}" onclick="OpenWindow(this.href); return false">
								<xsl:value-of select="i18n:translate('wcms.multimedia')"/>
							</a>
						</li>
						<li>
							<a href="{$ServletsBaseURL}WCMSAdminServlet{$JSessionID}?action=managGlobal">
								<xsl:if test="$menupunkt='Einstellungen'">
									<xsl:attribute name="class">
										<xsl:value-of select="'current'"/>
									</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="i18n:translate('wcms.setup')"/>
							</a>
						</li>
						<li>
							<a href="{$ServletsBaseURL}WCMSAdminServlet{$JSessionID}?action=logs&amp;sort=date&amp;sortOrder=descending">
								<xsl:if test="$menupunkt='Statistik'">
									<xsl:attribute name="class">
										<xsl:value-of select="'current'"/>
									</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="i18n:translate('wcms.stats')"/>
							</a>
						</li>
<!--						<li>
							<a href="{$WebApplicationBaseURL}modules/module-wcms/aif/web/help.xml">
								<xsl:if test="$menupunkt='Hilfe'">
									<xsl:attribute name="class">
										<xsl:value-of select="'current'"/>
									</xsl:attribute>
								</xsl:if>
								<xsl:value-of select="i18n:translate('wcms.help')"/>
							</a>
						</li>-->
					</ul>
				</div>
			</xsl:if>

			<!-- Container schliessen, Float beenden -->
			<div id="clearmenu">
				&#160;
			</div>



		</div><!-- nav -->

	
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="zeigeSeitenname"

	- Stellt den Namen der Seite dar

parameters:
	seitenname - Name der Seite
}===================================================================================== -->

	<xsl:template name="zeigeSeitenname">
		<xsl:param name="seitenname" />
		<!-- Seitenname -->
		<div id="seitenname">
			<h3><xsl:value-of select="$seitenname"/></h3>
		</div>
	</xsl:template>

<!-- =================================================================================================== -->
</xsl:stylesheet>