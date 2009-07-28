<?xml version="1.0" encoding="UTF-8"?>

<!-- =====================================================================================
========================================================================================={

title: wcms_admin.xsl

Wertet /cms/session aus und erzeugt entsprechende Seite.

	- Menue (Seitenpflege, globale Einstellungen, Statistik, Abmelden)
	- globale Einstellung (Template) aendern
	- Nutzerstatistik anzeigen

template:
	- wcmsAdministration (name)
	- wcmsAdministration.welcome (name)
	- wcmsAdministration.logStatistic (name)
	- wcmsAdministration.managGlobal (name)
	- wcmsAdministration.managGlobal.defaultTempl(name)
	- wcmsAdministration.managGlobal.saveButton (name)

}=========================================================================================
====================================================================================== -->

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan"
	xmlns:encodeURL="xalan://java.net.URLEncoder"
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" >

	<xsl:param name="MCR.WCMS.backupPath" />
<!-- ====================================================================================={

section: Template: name="wcmsAdministration"

	- Startet je nach Inhalt der /cms/session ein Template

		- welcome - wcmsAdministration.welcome
		- logs - wcmsAdministration.logStatistic
		- managGlobal - wcmsAdministration.managGlobal


}===================================================================================== -->

	<xsl:template name="wcmsAdministration" >

	<xsl:choose>	   
		<xsl:when test=" /cms/session = 'welcome' " >
			<xsl:call-template name="wcmsAdministration.welcome" />	  
		</xsl:when>	
		
		<xsl:when test="/cms/session = 'logs' " >  
			<xsl:call-template name="wcmsAdministration.logStatistic" />
		</xsl:when> 		
		
		<xsl:when test=" /cms/session = 'managGlobal' " >
			<xsl:call-template name="wcmsAdministration.managGlobal" />
		</xsl:when>                                          	  
	  </xsl:choose>	

	</xsl:template>

<!-- ====================================================================================={

section: Template: name="wcmsAdministration.welcome"

	- erzeugt das Menue der Administrationsoberflaeche

		- ruft das Template wcms.headline
		- verweist auf das WCMSAdminServlet
		- je nach Aufgabe auf wird entsprechend der Parameter action gesetzt 

}===================================================================================== -->

	<xsl:template name="wcmsAdministration.welcome" >

		<table width="90%" border="0" cellspacing="0" cellpadding="0" align="center">

			<!-- Ueberschrift und Ausrichtung -->
			<xsl:call-template name="wcms.headline" >
				<xsl:with-param 
					name="infoText" 
					select="concat(/cms/userRealName,', ',i18n:translate('wcms.admin.greeting'))">
				</xsl:with-param>
				<xsl:with-param 
					name="align" 
					select="'left'">
				</xsl:with-param>
			</xsl:call-template>

			<tr>
				<td colspan="2">
					<br/>
					<xsl:value-of select="concat(i18n:translate('wcms.admin.options.header'),':')"/>
					<br/><br/>
					<img 
						src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
						width="16" height="8" border="0" alt="" title=""/> 
					<a href="{$ServletsBaseURL}MCRWCMSAdminServlet?action=choose">
						<xsl:value-of select="i18n:translate('wcms.admin.options.manage')"/>
					</a>

					<!-- Nur fuer den Administrator -->
					<xsl:if test=" /cms/userClass = 'admin' ">
						<br/> 
						<img 
							src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
							width="16" height="8" border="0" alt="" title=""/> 
						<a href="{$ServletsBaseURL}MCRWCMSAdminServlet?action=managGlobal">
							<xsl:value-of select="i18n:translate('wcms.admin.options.globalSetup')"/>
						</a>
					</xsl:if>

					<br/>
					<img 
						src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
						width="16" height="8" border="0" alt="" title="" />
					<a href="{$ServletsBaseURL}MCRWCMSAdminServlet?action=logs&amp;sort=date&amp;sortOrder=descending">
						<xsl:value-of select="i18n:translate('wcms.admin.options.showStats')"/>
					</a> 
					<br/><br/>
					<img 
						src="{$WebApplicationBaseURL}templates/master/template_wcms/IMAGES/naviMenu/greenArrow.gif" 
						width="16" height="8" border="0" alt="" title="" /> 
					<a href="javascript: window.close()">
						<xsl:value-of select="i18n:translate('wcms.labels.logout')"/>
					</a>
				</td>
			</tr>
		</table>

	</xsl:template>

<!-- ===================================================================================== -->

	<xsl:template name="wcmsAdministration.logStatistic" >

		<xsl:variable name="sortBy" select="/cms/sort" />
		<xsl:variable name="currentSortOrder" select="/cms/sort/@order" />
	
		<xsl:variable name="flipedSortOrder" >
			<xsl:call-template name="logs.flipSortOrder">
				<xsl:with-param name="currentSortOrder" select="$currentSortOrder"/>
			</xsl:call-template>
		</xsl:variable>	
	
		<!-- Menueleiste einblenden, Parameter = ausgewaehlter Menuepunkt -->
		<xsl:call-template name="menuleiste">
			<xsl:with-param name="menupunkt" select="'Statistik'" />
		</xsl:call-template>

		<xsl:variable name="wieSortiert" >
			<xsl:choose>
				<xsl:when test="count(/cms/loggings/log) > 1">
					<xsl:value-of select="i18n:translate('wcms.admin.sort.element')"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select=" count(/cms/loggings/log)" />
					<xsl:value-of select="i18n:translate('wcms.admin.sort.sortBy')"/>
				</xsl:otherwise>
			</xsl:choose>
			<!-- which column -->
			<xsl:choose>
				<xsl:when test=" $sortBy = 'date' " >
					<xsl:value-of select="i18n:translate('wcms.date')"/>
				</xsl:when>
				<xsl:when test="/cms/userClass != 'autor' and $sortBy = 'userRealName' " >
					<xsl:value-of select="i18n:translate('wcms.user')"/>
				</xsl:when>
				<xsl:when test=" $sortBy = 'labelPath' " >
					<xsl:value-of select="i18n:translate('wcms.site')"/>
				</xsl:when>
				<xsl:when test=" $sortBy = 'doneAction' " >
					<xsl:value-of select="i18n:translate('wcms.action')"/>
				</xsl:when>
				<xsl:when test="/cms/userClass = 'admin' and $sortBy = 'backupContentFile' " >
					<xsl:value-of select="i18n:translate('wcms.backupContent')"/>
				</xsl:when>
				<xsl:when test="/cms/userClass = 'admin' and $sortBy = 'backupNavigationFile' " >
					<xsl:value-of select="i18n:translate('wcms.backupNav')"/>
				</xsl:when>
			</xsl:choose>
			<!-- which order -->
			<xsl:choose>
				<xsl:when test="$currentSortOrder = 'ascending' " >
					<xsl:choose>
						<xsl:when test="$sortBy = 'date' " >
							<xsl:value-of select="i18n:translate('wcms.admin.sort.oldFirst')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="i18n:translate('wcms.admin.sort.ascending')"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="$sortBy = 'date' " >
							<xsl:value-of select="i18n:translate('wcms.admin.sort.newFirst')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="i18n:translate('wcms.admin.sort.descending')"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

	<!-- Seitenname -->
	<xsl:call-template name="zeigeSeitenname">
		<xsl:with-param name="seitenname">
			<xsl:value-of select="i18n:translate('wcms.admin.WCMSUserProt')"/>
		</xsl:with-param>
	</xsl:call-template>

	<!-- Inhaltsbereich -->
	<div id="statistic-width">
		<div id="statistic">
			<!-- div class="titel">WCMS Nutzungsprotokoll</div -->
			<div class="inhalt">
					<table>
						<!-- table headline -->
						<xsl:call-template name="logs.headLine">
							<xsl:with-param name="sortBy" select="$sortBy"/>
							<xsl:with-param name="currentSortOrder" select="$currentSortOrder"/>
						</xsl:call-template>
						<!-- END OF: table headline -->		

						<!-- content cells -->
						<xsl:choose>
							<!-- primary to sort by date -->
							<xsl:when test="$sortBy = 'date' " >
								<xsl:call-template name="logs.generateList">
										<xsl:with-param name="rootNode" select="/"/>
										<xsl:with-param name="currentSortOrder" select="$currentSortOrder"/>
										<xsl:with-param name="sortBy" select="$sortBy"/>	
								</xsl:call-template>
							</xsl:when>

							<!-- primary NOT to sort by date -->
							<xsl:otherwise>
								<xsl:for-each select="/cms/loggings/log">
								<xsl:sort select="@*[name() = $sortBy] | @*[concat('@',name()) = $sortBy]" order="{$currentSortOrder}" />
								<xsl:sort select="@date" order="{$flipedSortOrder}" />
								<xsl:sort select="@time" order="{$flipedSortOrder}" />
									<xsl:variable name="Rest" select="(position() div 2) *2 - round(position() div 2)*2" />
									<tr>

										<xsl:if test="$Rest != 0">
											<xsl:attribute name="class">
												<xsl:value-of select="'zebra'" />
											</xsl:attribute>
										</xsl:if>

										<!-- date, time -->
										<td>
											<xsl:value-of select="@time"/>
											<xsl:value-of select="i18n:translate('wcms.time')"/>
											<xsl:value-of select="substring(@date,9,2)" />.<xsl:value-of select="substring(@date,6,2)" />.<xsl:value-of select="substring(@date,1,4)" />
										</td>

										<!-- user -->
										<xsl:if test="/cms/userClass != 'autor' " >			  
											<td valign="top"><xsl:value-of select="@userRealName" /></td>
										</xsl:if>

										<!-- label path -->
										<td>
											<xsl:value-of select="@labelPath" />
											<xsl:variable name="viewAddress">
												<xsl:call-template name="get.viewAddress">
													<xsl:with-param name="backupContentFile" select="@backupContentFile" />
													<xsl:with-param name="backupNavigationFile" select="@backupNavigationFile" />																												
													<xsl:with-param name="doneAction" select="@doneAction" />
												</xsl:call-template>																
											</xsl:variable>
											<br/><br/>
											<a target="_blank" href="{$viewAddress}">
												anschauen &gt;&gt;
											</a>
										</td>

										<!-- done action -->
										<td>
											<xsl:choose>
												<xsl:when test="@doneAction = 'add' " >
													<xsl:value-of select="i18n:translate('wcms.action.created')"/>
												</xsl:when>
												<xsl:when test="@doneAction = 'edit' " >
													<xsl:value-of select="i18n:translate('wcms.action.changed')"/>
												</xsl:when>
												<xsl:when test="@doneAction = 'delete' " >
													<xsl:value-of select="i18n:translate('wcms.action.deleted')"/>
												</xsl:when>
												<xsl:when test="@doneAction = 'translate' " >
													<xsl:value-of select="i18n:translate('wcms.action.translated')"/>
												</xsl:when>
											</xsl:choose>
										</td>

										<!-- backup location -->
										<xsl:if test="/cms/userClass = 'systemAdmin' " >
											<td>
												<xsl:value-of select="@backupContentFile" />													-->
												<br/>
												<xsl:value-of select="@backupNavigationFile" />
											</td>
										</xsl:if>
										
										<td class="kommentar">
											<!-- show given notes -->
											<xsl:if test=" note != '' " >
												<xsl:value-of select=" note " />
											</xsl:if>
										</td>
									</tr>
							</xsl:for-each>
						</xsl:otherwise>
					</xsl:choose>
					<!-- END OF : content cells -->
				</table>

			</div>
		</div>
	<!-- Ende: Inhaltsbereich -->
	</div>
 </xsl:template>

<!-- ===================================================================================== -->	
	
<xsl:template name="get.viewAddress">
	<xsl:param name="backupContentFile" />
	<xsl:param name="backupNavigationFile" />
	<xsl:param name="doneAction" />		
	
	<!--content-->
	<xsl:variable name="file">
		<xsl:variable name="nextFile">
			<xsl:call-template name="get.nextContentFile">
				<xsl:with-param name="doneAction" select="$doneAction"/>
				<xsl:with-param name="backupContentFile" select="$backupContentFile"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="document($nextFile)/MyCoReWebPage!=''">
				<xsl:value-of select="$nextFile"/>
			</xsl:when>
			<!-- archived version not found -> newest version -> watch http:/.. -->
			<xsl:otherwise>
				<xsl:variable name="cleanHref">
					<xsl:call-template name="get.cleanHref">
						<xsl:with-param name="nextFile" select="$nextFile"/>
						<xsl:with-param name="frontPath" select="$MCR.WCMS.backupPath"/>						
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="encodeURL:encode(concat($WebApplicationBaseURL,$cleanHref),'UTF-8')"/>
			</xsl:otherwise>
		</xsl:choose>		
	</xsl:variable>
	<!--navigation base-->
	<xsl:variable name="navi">
		<xsl:variable name="nextFile">
			<xsl:choose>
				<xsl:when test="$doneAction!='delete'">
					<xsl:call-template name="get.nextVersion">
						<xsl:with-param name="current" select="$backupNavigationFile" />
					</xsl:call-template>									
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$backupNavigationFile"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="document($nextFile)/navigation!=''">
				<xsl:value-of select="$nextFile"/>
			</xsl:when>
			<!-- archived version not found -> newest version -> watch http:/.. -->
			<xsl:otherwise>
				<xsl:value-of select="encodeURL:encode(concat($WebApplicationBaseURL,'config/navigation.xml'),'UTF-8')"/>
			</xsl:otherwise>
		</xsl:choose>				
	</xsl:variable>
	<!--href in navigation.xml to be poped up right-->
	<xsl:variable name="href">
		<xsl:variable name="nextFile">
			<xsl:call-template name="get.nextContentFile">
				<xsl:with-param name="doneAction" select="$doneAction"/>
				<xsl:with-param name="backupContentFile" select="$backupContentFile"/>
			</xsl:call-template>			
		</xsl:variable>
		<xsl:variable name="cleanHref">
			<xsl:call-template name="get.cleanHref">
				<xsl:with-param name="nextFile" select="$nextFile"/>
				<xsl:with-param name="frontPath" select="$MCR.WCMS.backupPath"/>				
			</xsl:call-template>			
		</xsl:variable>
		<xsl:value-of select="encodeURL:encode(concat('/',$cleanHref), 'UTF-8')"/>
	</xsl:variable>
	
	<xsl:value-of select="concat($ServletsBaseURL,'MCRWCMSAdminServlet?action=view&amp;file=',$file,'&amp;XSL.navi=',$navi,'&amp;XSL.href=',$href)" />
</xsl:template>	

<!-- ===================================================================================== -->		
<xsl:template name="get.cleanHref">
	<xsl:param name="nextFile"/>
	<xsl:param name="frontPath"/>	
	<xsl:variable name="nextFile_backupPathDel">
		<xsl:value-of select="substring-after($nextFile,$frontPath)"/>
	</xsl:variable>
	<xsl:value-of select="concat(substring-before($nextFile_backupPathDel,'.xml'),'.xml')"/>
</xsl:template>	
<!-- ===================================================================================== -->			
<xsl:template name="get.nextContentFile">
	<xsl:param name="doneAction" />
	<xsl:param name="backupContentFile" />
	<xsl:choose>
		<xsl:when test="$doneAction='add' or $doneAction='delete'">
			<xsl:value-of select="$backupContentFile"/>
		</xsl:when>
		<xsl:otherwise>
			<xsl:call-template name="get.nextVersion">
				<xsl:with-param name="current" select="$backupContentFile" />
			</xsl:call-template>				
		</xsl:otherwise>
	</xsl:choose>	
</xsl:template>	
<!-- ===================================================================================== -->				
<xsl:template name="get.nextVersion">
	<xsl:param name="current"/>
	
		<xsl:choose>
			<xsl:when test="substring-after($current,'.xml')=''">
				<xsl:value-of select="concat($current,'.1')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="lastVersion">
					<xsl:value-of select="substring-after($current,'.xml.')"/>
				</xsl:variable>							
				<xsl:value-of select="concat(substring-before($current,'.xml'),'.xml.',number($lastVersion)+1)"/>					
			</xsl:otherwise>
		</xsl:choose>	
</xsl:template>
	
<!-- ====================================================================================={
	
section: Template: name="wcmsAdministration.managGlobal"

	- Formular zur Veränderung des  Seitentemplates

}===================================================================================== -->
	
		
<xsl:template name="wcmsAdministration.managGlobal" >

	<xsl:variable name="currentTempl" select="document($navigationBase)/navigation/@template"/>

	<!-- Menueleiste einblenden, Parameter = ausgewaehlter Menuepunkt -->
	<xsl:call-template name="menuleiste">
		<xsl:with-param name="menupunkt" select="i18n:translate('wcms.setup')" />
	</xsl:call-template>

	<!-- Seitenname -->
	<xsl:call-template name="zeigeSeitenname">
		<xsl:with-param name="seitenname" select="i18n:translate('wcms.projSetup')" />
	</xsl:call-template>

	<!-- Inhaltsbereich -->
	<div id="settings-width">

		<div id="settings">
			<form name="settings" action="{$ServletsBaseURL}MCRWCMSAdminServlet{$JSessionID}?action=saveGlobal" method="post">
				<div class="titel"><xsl:value-of select="i18n:translate('wcms.design')"/></div>
				<div class="inhalt">
					<fieldset>
						<p><xsl:value-of select="i18n:translate('wcms.design.hint')"/></p>

						<label for="defTempl"><xsl:value-of select="i18n:translate('wcms.design.mainDesign')"/></label>
						<select name="defTempl" size="1">
							<xsl:for-each select="/cms/templates/master/template">
								<xsl:choose>
									<xsl:when test="current() = $currentTempl">
										<option value="{current()}">
											<xsl:value-of select="current()" /><xsl:value-of select="i18n:translate('wcms.design.currentTempl')"/>
										</option>
									</xsl:when>
									<xsl:otherwise>
										<xsl:if test="current() != 'template_wcms'">
											<option value="{current()}">
												<xsl:value-of select="current()" />
											</option>
										</xsl:if>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</select>
					</fieldset>
				</div>
				<div class="knoepfe">
					<a class="button" href="javascript:document.settings.submit()"><xsl:value-of select="i18n:translate('wcms.buttons.saveChanges')"/></a>
				</div>
			</form>
		</div>
	<!-- Ende: Inhaltsbereich -->
	</div>

</xsl:template>

<!-- ====================================================================================={

section: Template: name="wcmsAdministration.managGlobal.saveButton"

	- generiert den submit button 

}===================================================================================== -->

<xsl:template name="wcmsAdministration.managGlobal.saveButton">
	<tr>
		<td colspan="2" align="right">
			<table cellspacing="0" border="0" cellpadding="0" align="right" class="wcms">
				<!-- submit -->
				<tr>
					<td align="right" class="button">
						<input class="button" value="{i18n:translate('wcms.buttons.save')}" type="submit"/>
					</td>
				</tr>
				<!-- END OF: submit -->
			</table>
		</td>
	</tr>
</xsl:template>

<!-- ====================================================================================={

section: Template: name="wcmsAdministration.managGlobal.defaultTempl"

	- erzeugt das Auswahlmenue fuer die vorhandenen Templates

}===================================================================================== -->

<xsl:template name="wcmsAdministration.managGlobal.defaultTempl" >

	<xsl:variable name="currentTempl" select="document($navigationBase)/navigation/@template"/>

	<br/>
	<xsl:value-of select="i18n:translate('wcms.design.defaultTempl')"/>
	<select name="defTempl" size="1">
		<xsl:for-each select="/cms/templates/master/template">
			<xsl:choose>
				<xsl:when test="current() = $currentTempl">
					<option value="{current()}">
						<xsl:value-of select="current()" /><xsl:value-of select="i18n:translate('wcms.design.currentTempl')"/>
					</option>
				</xsl:when>
				<xsl:otherwise>
					<option value="{current()}">
						<xsl:value-of select="current()" />
					</option>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</select>
</xsl:template>

<!-- =================================================================================== -->
	
<xsl:template name="logs.generateList">
	<xsl:param name="rootNode"/>
	<xsl:param name="currentSortOrder"/>
	<xsl:param name="sortBy"/>
	<xsl:param name="onlyCurrentPage" />	
	
		<xsl:for-each select="xalan:nodeset($rootNode)/cms/loggings/log">
			<xsl:sort select="@*[name() = $sortBy] | @*[concat('@',name()) = $sortBy]" order="{$currentSortOrder}" />  	
			<xsl:sort select="@time" order="{$currentSortOrder}" />

			<xsl:if test="($onlyCurrentPage='') or ($onlyCurrentPage!='' and contains(@backupContentFile, $onlyCurrentPage))" >
					
				<xsl:variable name="Rest" select="(position() div 2) *2 - round(position() div 2)*2" />
				<tr>
	
					<xsl:if test="$Rest != 0">
						<xsl:attribute name="class">
							<xsl:value-of select="'zebra'" />
						</xsl:attribute>
					</xsl:if>
					
					<!-- date, time -->	
					<td>
						<xsl:value-of select="@time"/>
						<xsl:value-of select="i18n:translate('wcms.time')"/>
						<xsl:value-of select="substring(@date,9,2)" />.<xsl:value-of select="substring(@date,6,2)" />.<xsl:value-of select="substring(@date,1,4)" />
					</td>
	
					<!-- user -->
					<xsl:if test="xalan:nodeset($rootNode)/cms/userClass != 'autor' " >			  
						<td valign="top"><xsl:value-of select="@userRealName" /></td>
					</xsl:if>
	
					<!-- label path -->
					<td>
						<xsl:value-of select="@labelPath" />
						<xsl:variable name="viewAddress">
							<xsl:call-template name="get.viewAddress">
								<xsl:with-param name="backupContentFile" select="@backupContentFile" />
								<xsl:with-param name="backupNavigationFile" select="@backupNavigationFile" />																												
								<xsl:with-param name="doneAction" select="@doneAction" />
							</xsl:call-template>																
						</xsl:variable>
						<br/><br/>
						<a target="_blank" href="{$viewAddress}">
							anschauen &gt;&gt;
						</a>
					</td>
	
					<!-- done action -->
					<td>
						<xsl:choose>
							<xsl:when test="@doneAction = 'add' " >
								<xsl:value-of select="i18n:translate('wcms.action.created')"/>
							</xsl:when>
							<xsl:when test="@doneAction = 'edit' " >
								<xsl:value-of select="i18n:translate('wcms.action.changed')"/>
							</xsl:when>
							<xsl:when test="@doneAction = 'delete' " >
								<xsl:value-of select="i18n:translate('wcms.action.deleted')"/>
							</xsl:when>
							<xsl:when test="@doneAction = 'translate' " >
								<xsl:value-of select="i18n:translate('wcms.action.translated')"/>
							</xsl:when>
						</xsl:choose>
					</td>
	
					<!-- backup location -->
					<xsl:if test="xalan:nodeset($rootNode)/cms/userClass = 'systemAdmin' " >
						<td>
							<xsl:value-of select="@backupContentFile" />													
							<br/>
							<xsl:value-of select="@backupNavigationFile" />
						</td>
					</xsl:if>
					
					<td class="kommentar">
						<!-- show given notes -->
						<xsl:if test=" note != '' " >
							<xsl:value-of select=" note " />
						</xsl:if>
					</td>
				</tr>				
			</xsl:if>
			
		</xsl:for-each>	
</xsl:template>
	
<!-- =================================================================================== -->	
		
<xsl:template name="logs.headLine">
	<xsl:param name="sortBy" />
	<xsl:param name="currentSortOrder" />	

	<xsl:variable name="flipedSortOrder" >
		<xsl:call-template name="logs.flipSortOrder">
			<xsl:with-param name="currentSortOrder" select="$currentSortOrder"/>
		</xsl:call-template>
	</xsl:variable>	
	
	<tr>
		<!-- date, time column -->
		<xsl:choose>
			<xsl:when test=" $sortBy = 'date' " >
				<th id="aktive-row">
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=date&amp;sortOrder={$flipedSortOrder}">
						<xsl:choose>
							<xsl:when test="$currentSortOrder='descending'">
								<xsl:text>&#8659; </xsl:text>
							</xsl:when>
							<xsl:when test="$currentSortOrder='ascending'">
								<xsl:text>&#8657; </xsl:text>
							</xsl:when>
						</xsl:choose>
						<xsl:value-of select="i18n:translate('wcms.date')"/>
					</a>
				</th>
			</xsl:when>
			<xsl:otherwise>
				<th>
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=date&amp;sortOrder={$currentSortOrder}">
						<xsl:value-of select="i18n:translate('wcms.date')"/>
					</a>
				</th>
			</xsl:otherwise>
		</xsl:choose>

		<!-- user name column -->
		<xsl:choose>
			<xsl:when test="/cms/userClass != 'autor' and $sortBy = 'userRealName' " >
				<th id="aktive-row">
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=userRealName&amp;sortOrder={$flipedSortOrder}">
						<xsl:choose>
							<xsl:when test="$currentSortOrder='descending'">
								<xsl:text>&#8659; </xsl:text>
							</xsl:when>
							<xsl:when test="$currentSortOrder='ascending'">
								<xsl:text>&#8657; </xsl:text>
							</xsl:when>
						</xsl:choose>
						<xsl:value-of select="i18n:translate('wcms.user')"/>
					</a>
				</th>
			</xsl:when>
			<xsl:when test="/cms/userClass != 'autor' and $sortBy != 'userRealName' " >
				<th>										
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=userRealName&amp;sortOrder={$currentSortOrder}">
						<xsl:value-of select="i18n:translate('wcms.user')"/>
					</a>
				</th>
			</xsl:when>
		</xsl:choose>

		<!-- page column -->
		<xsl:choose>
			<xsl:when test=" $sortBy = 'labelPath' " >
				<th id="aktive-row">
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=labelPath&amp;sortOrder={$flipedSortOrder}">
						<xsl:choose>
						<xsl:when test="$currentSortOrder='descending'">
								<xsl:text>&#8659; </xsl:text>
							</xsl:when>
							<xsl:when test="$currentSortOrder='ascending'">
								<xsl:text>&#8657; </xsl:text>
							</xsl:when>
						</xsl:choose>
						<xsl:value-of select="i18n:translate('wcms.site')"/>
					</a>
				</th>
			</xsl:when>
			<xsl:otherwise>
				<th>
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=labelPath&amp;sortOrder={$currentSortOrder}">
						<xsl:value-of select="i18n:translate('wcms.site')"/>
					</a>
				</th>
			</xsl:otherwise>
		</xsl:choose>

		<!-- done action column -->
		<xsl:choose>
			<xsl:when test=" $sortBy = 'doneAction' " >
				<th id="aktive-row">
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=doneAction&amp;sortOrder={$flipedSortOrder}">
						<xsl:choose>
							<xsl:when test="$currentSortOrder='descending'">
								<xsl:text>&#8659; </xsl:text>
							</xsl:when>
							<xsl:when test="$currentSortOrder='ascending'">
								<xsl:text>&#8657; </xsl:text>
							</xsl:when>
						</xsl:choose>											
						<xsl:value-of select="i18n:translate('wcms.action')"/>
					</a>
				</th>
			</xsl:when>
			<xsl:otherwise>
				<th>
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=doneAction&amp;sortOrder={$currentSortOrder}">
						<xsl:value-of select="i18n:translate('wcms.action')"/>
					</a>
				</th>
			</xsl:otherwise>
		</xsl:choose>

		<th>
			<xsl:value-of select="i18n:translate('wcms.comment')"/>
		</th>

		<!-- backup column -->
		<!-- xsl:choose>
			<xsl:when test="/cms/userClass = 'systemAdmin' and ($sortBy = 'backupContentFile' or $sortBy = 'backupNavigationFile') " >
				<th id="aktive-row">
					<b>Backup
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=backupContentFile&amp;sortOrder={$flipedSortOrder}">(Inhalt</a>
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=backupNavigationFile&amp;sortOrder={$flipedSortOrder}">, Navigation)</a>
					</b>
				</th>
			</xsl:when>
			<xsl:when test="/cms/userClass = 'systemAdmin' and ($sortBy != 'backupContentFile' and $sortBy != 'backupNavigationFile') " >
				<th>
					Backup
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=backupContentFile&amp;sortOrder={$currentSortOrder}"> (Inhalt</a>
					<a href="MCRWCMSAdminServlet?action=logs&amp;sort=backupNavigationFile&amp;sortOrder={$currentSortOrder}">, Navigation)</a>
				</th>
			</xsl:when>
		</xsl:choose-->
	</tr>	
</xsl:template>	
	
<!-- =================================================================================== -->	
	
<xsl:template name="logs.flipSortOrder">
	<xsl:param name="currentSortOrder"/>
	
	<xsl:choose>
		<xsl:when test="$currentSortOrder = 'ascending' " >
			<xsl:value-of select="'descending'" />
		</xsl:when>
		<xsl:otherwise>
			<xsl:value-of select="'ascending'" />	
		</xsl:otherwise>
	</xsl:choose>		
</xsl:template>
	
<!-- =================================================================================== -->		
	
</xsl:stylesheet>
