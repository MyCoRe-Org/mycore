<?xml version="1.0" encoding="ISO-8859-1"?>
<!-- ============================================== -->
<!-- $Revision: 1.4 $ $Date: 2004-12-28 23:38:21 $ -->
<!-- ============================================== -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xlink="http://www.w3.org/1999/xlink" 
	exclude-result-prefixes="xlink" >

	<xsl:include href="MyCoReLayout.xsl" />
	<xsl:variable name="EmptyWorkflow">
		<xsl:text>Der Workflow ist leer.</xsl:text>
	</xsl:variable>
	<xsl:variable name="PageTitle">
		<xsl:choose>
			<xsl:when test="/MyCoReWebPage/section[ lang($CurrentLang)]/@title != '' ">
				<xsl:value-of select="/MyCoReWebPage/section[lang($CurrentLang)]/@title"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="/MyCoReWebPage/section[lang($DefaultLang)]/@title"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:variable name="Servlet" select="'undefined'"/>
	<!-- ================================================================================================================= -->
	<xsl:template match="/MyCoReWebPage">
            <!--
	  <xsl:param name="browserAddress" />
	  <xsl:param name="template"  />				
	  -->
		<xsl:choose>
			<xsl:when test="$template = 'template_docportal' ">
				<div id="help" class="help">
					<div class="resultcmd" style="padding-left:20px;">
						<table style="width:100%;" cellpadding="0" cellspacing="0">
							<tr style="height:20px;">
								<td class="resultcmd">
									<xsl:value-of select="$MainTitle"/>
								</td>
								<td class="resultcmd" style="text-align:right;padding-right:5px;vertical-align:bottom;">
									<xsl:value-of select="$PageTitle"/>
								</td>
							</tr>
						</table>
					</div>
					<div style="padding:20px;">
						<!--
						<xsl:apply-templates select="section[lang($CurrentLang)] | section[lang('all')]">
							<xsl:with-param name="PrivOfUser" select="$PrivOfUser" />
						</xsl:apply-templates>
						-->
						<xsl:choose>
							<xsl:when test=" section[lang($CurrentLang)] != '' ">
								<xsl:apply-templates select="section[lang($CurrentLang)] | section[lang('all')]">
									<!--
									<xsl:with-param name="browserAddress" select="$browserAddress" />
                                                      -->
								</xsl:apply-templates>				
							</xsl:when>
							<xsl:otherwise>
								<xsl:apply-templates select="section[lang($DefaultLang)]">
                                                      <!--
									<xsl:with-param name="browserAddress" select="$browserAddress" />
                                                      -->
								</xsl:apply-templates>								
							</xsl:otherwise>
						</xsl:choose>						
					</div>
					<div class="resultcmd" style="padding-left:20px;"> &#160; </div>
				</div>						
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test=" section[lang($CurrentLang)] != '' ">
						<xsl:apply-templates select="section[lang($CurrentLang)] | section[lang('all')]">
                                          <!--
							<xsl:with-param name="browserAddress" select="$browserAddress" />
                                          -->
						</xsl:apply-templates>				
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates select="section[lang($DefaultLang)]">
                                          <!--
							<xsl:with-param name="browserAddress" select="$browserAddress" />
                                          -->
						</xsl:apply-templates>								
					</xsl:otherwise>
				</xsl:choose>				
			</xsl:otherwise>
		</xsl:choose>

	</xsl:template>
	<!-- ================================================================================================================= -->
	<!-- - - - - - - - - Identity Transformation  - - - - - - - - - -->
	<xsl:template match='@*|node()'>
		<xsl:copy>
			<xsl:apply-templates select='@*|node()'/>
		</xsl:copy>
	</xsl:template>
	<!-- ================================================================================================================= -->
	<xsl:template match="section">
            <!--
		<xsl:param name="browserAddress" />
            -->
		<xsl:for-each select="node()">
			<xsl:choose>
				<xsl:when test="name() = 'editor'">
					<center>
						<xsl:apply-templates select="." >
							<xsl:with-param name="WebApplicationBaseURL" select="WebApplicationBaseURL" />
							<xsl:with-param name="CurrentLang" select="CurrentLang" />
						</xsl:apply-templates>
					</center>
				</xsl:when>
				<xsl:when test="name() = 'workflow'">
					<center>
						<xsl:apply-templates select="." >
							<xsl:with-param name="WebApplicationBaseURL" select="WebApplicationBaseURL" />
							<xsl:with-param name="CurrentLang" select="CurrentLang" />
							<xsl:with-param name="PrivOfUser" select="$PrivOfUser" />
						</xsl:apply-templates>
					</center>
				</xsl:when>
				<!-- added by wcms -->
				<xsl:when test="name() = 'toc' or 'TOC'">
					<xsl:apply-templates select="." >
                                    <!--
						<xsl:with-param name="browserAddress" select="$browserAddress" />
                                    -->
					</xsl:apply-templates>
				</xsl:when>
				<!-- end: added by wcms -->
				<xsl:otherwise>
					<xsl:copy-of select="."/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
	<!-- =============================================================================================== -->
	<xsl:template match="/MyCoReWebPage/head/buttons">
		<xsl:if test="lang('de')">
			<table border="0" cellspacing="4">
				<xsl:for-each select="button">
					<xsl:if test="position() = 1">
						<xsl:text disable-output-escaping="yes">&lt;tr&gt;</xsl:text>
					</xsl:if>
					<xsl:if test="position() = count(../button) div 2 + 1">
						<xsl:text disable-output-escaping="yes">&lt;tr&gt;</xsl:text>
					</xsl:if>
					<xsl:text disable-output-escaping="yes">&lt;td&gt;</xsl:text>
					<xsl:if test="string-length(@title)">
						<xsl:text disable-output-escaping="yes">&lt;a href='</xsl:text>
						<xsl:value-of select="@action"/>
						<xsl:text disable-output-escaping="yes">'&gt;</xsl:text>
						<xsl:text disable-output-escaping="yes">&lt;img src='</xsl:text>
						<xsl:value-of select="@image"/>
						<xsl:text>' border='0' alt='</xsl:text>
						<xsl:value-of select="@title"/>
						<xsl:text disable-output-escaping="yes">'&gt;</xsl:text>
						<xsl:text disable-output-escaping="yes">&lt;/a&gt;</xsl:text>
					</xsl:if>
					<xsl:text disable-output-escaping="yes">&lt;/td&gt;</xsl:text>
					<xsl:if test="position() = count(../button) div 2">
						<xsl:text disable-output-escaping="yes">&lt;/tr&gt;</xsl:text>
					</xsl:if>
					<xsl:if test="position() = count(../button)">
						<xsl:text disable-output-escaping="yes">&lt;/tr&gt;</xsl:text>
					</xsl:if>
				</xsl:for-each>
			</table>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>