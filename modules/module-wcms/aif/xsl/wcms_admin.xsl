<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan" >

<!-- wcmsAdmin  ============================================================================== -->
<xsl:template name="wcmsAdministration" >

  <xsl:choose>	   
    <xsl:when test=" /cms/session = 'welcome' " >
	  <xsl:call-template name="wcmsAdministration.welcome" />	  
	</xsl:when>	
	
    <xsl:when test="/cms/session = 'logs' " >  
	  <xsl:call-template name="wcmsAdministration.logStatistic" />
    </xsl:when> 		
		  		  
  </xsl:choose>	

</xsl:template>
<!-- END OF: wcmsAdmin  ================================================================================= -->


<!-- welcome  ============================================================================== -->
<xsl:template name="wcmsAdministration.welcome" >

<table width="90%" border="0" cellspacing="0" cellpadding="0" align="center">

  <xsl:call-template name="wcms.headline" >
      <xsl:with-param name="infoText" select="concat(/cms/userRealName,', herzlich Willkommen im Web Content Management System !')"></xsl:with-param>
      <xsl:with-param name="align" select="'left'"></xsl:with-param>
  </xsl:call-template>
	
  <tr>
	<td colspan="2"> <br/> Die folgenden Optionen stehen für sie zur Verfügung: <br/><br/> <img 
		src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/greenArrow.gif" width="16" 
		height="8" border="0" alt="" title=""/> <a href="{$ServletsBaseURL}WCMSAdminServlet?action=choose">Verwaltung der 
		Webpräsenz</a> <br/> <img 
		src="{$WebApplicationBaseURL}modules/module-wcms/uif/web/common/images/naviMenu/greenArrow.gif" width="16" 
		height="8" border="0" alt="" title=""/> <!-- 	 <a href="/common/wcms/logs.xml">Nutzungsstatistik einsehen</a> -->
		 <a href="{$ServletsBaseURL}WCMSAdminServlet?action=logs&amp;sort=date&amp;sortOrder=descending">Nutzungsstatistik einsehen</a> 
		<br/><br/> <img src="{$WebApplicationBaseURL}modules/module-wcms/uif/templates/master/template_wcms/IMAGES/greenArrow.gif" 
		width="16" height="8" border="0" alt="" title="" /> <a href="javascript: window.close()">Abmelden</a> </td>
  </tr>

</table>

</xsl:template>
<!-- END OF: welcome  ================================================================================= -->

<!-- logStatistic  ==================================================================================== -->
<xsl:template name="wcmsAdministration.logStatistic" >

<xsl:variable name="sortBy" select="/cms/sort" />
<xsl:variable name="currentSortOrder" select="/cms/sort/@order" />

<xsl:variable name="flipedSortOrder" >
	<xsl:choose>
	  <xsl:when test="$currentSortOrder = 'ascending' " >
	    <xsl:value-of select="'descending'" />
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:value-of select="'ascending'" />	
	  </xsl:otherwise>
	</xsl:choose>	
</xsl:variable>

<table width="90%" border="0" cellspacing="0" cellpadding="0" align="center">

  <tr>
    <th width="50%" align="left">MyCoRe-WCMS :: das Web Content Management Modul</th>
    <th width="50%" align="right">
WCMS-Nutzer:
	  '<xsl:value-of select="/cms/userID" />'
	  (<xsl:value-of select="/cms/userClass" />)	
	</th>
  </tr>
    
  <!-- sort order information -->
  <tr>
    <th align="left" nowrap="yes" class="gray_noBorder">
      <br/>	
	   <xsl:value-of select=" count(/cms/loggings/log)" /> Einträge	sortiert nach 
			<!-- which column -->
			<xsl:choose>
			  <xsl:when test=" $sortBy = 'date' " >
		        Datum, Zeit
			  </xsl:when>
			  <xsl:when test="/cms/userClass != 'autor' and $sortBy = 'userRealName' " >
				Nutzer
			  </xsl:when>
			  <xsl:when test=" $sortBy = 'labelPath' " >
				Seite
			  </xsl:when>
			  <xsl:when test=" $sortBy = 'doneAction' " >
				Aktion
			  </xsl:when>
			  <xsl:when test="/cms/userClass = 'admin' and $sortBy = 'backupContentFile' " >
				Backup Inhalt  		  
			  </xsl:when>
			  <xsl:when test="/cms/userClass = 'admin' and $sortBy = 'backupNavigationFile' " >
				Backup Navigation
			  </xsl:when>
			</xsl:choose>			  
			<!-- which order -->
			<xsl:choose>
			  <xsl:when test="$currentSortOrder = 'ascending' " >
					<xsl:choose>
					  <xsl:when test="$sortBy = 'date' " >
						(von alt nach neu)
					  </xsl:when>
					  <xsl:otherwise>
						(aufsteigend)				
					  </xsl:otherwise>
					</xsl:choose>
			  </xsl:when>
			  <xsl:otherwise>
					<xsl:choose>
					  <xsl:when test="$sortBy = 'date' " >
						(von neu nach alt)
					  </xsl:when>
					  <xsl:otherwise>
						(absteigend)
					  </xsl:otherwise>
					</xsl:choose>
			  </xsl:otherwise>
			</xsl:choose>
	</th>
    <th align="right" class="gray_noBorder">
      <br/>
	    Nutzungsprotokoll WCMS-Modul
	</th>
  </tr>
  <!-- END OF: sort order information -->
    
  <tr> 
    <td colspan="2"><br/></td>
  </tr>
  
  <!-- back button -->  
  <tr>
    <td colspan="2" align="right" width="100%">
	    <table cellspacing="0" cellpadding="0" align="right">
		  <tr>
				<td align="right">
					<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/templates/master/template_wcms/IMAGES/box_left.gif" 
						width="11" height="22" border="0" alt="" title="" />
				</td>
				 <td align="right" class="button">
			       <a href="{$ServletsBaseURL}WCMSLoginServlet">zurück zur WCMS-Startseite</a>
				 </td>
				<td align="right">
					<img 
						src="{$WebApplicationBaseURL}modules/module-wcms/uif/templates/master/template_wcms/IMAGES/box_right.gif" 
						width="11" height="22" border="0" alt="" title="" />
				</td>
		  </tr>
		</table>
    </td>
  </tr>
  <!-- END OF: back button -->    
  
  <tr> 
    <td colspan="2"><br/></td>
  </tr>  
  
  <!-- log table -->
  <tr>
    <td width="100%" colspan="2" align="center" valign="top">
	  <table class="table" width="100%" cellspacing="0" cellpadding="0">
	    <!-- table headline -->
	    <tr>
		    <!-- date, time column -->
			<xsl:choose>
			  <xsl:when test=" $sortBy = 'date' " >
		         <td align="center" class="green">
		           <a href="WCMSAdminServlet?action=logs&amp;sort=date&amp;sortOrder={$flipedSortOrder}">Datum, Zeit</a>
		         </td>			
			  </xsl:when>
			  <xsl:otherwise>
		        <th><a href="WCMSAdminServlet?action=logs&amp;sort=date&amp;sortOrder={$currentSortOrder}">Datum, Zeit</a></th>						
			  </xsl:otherwise>
			</xsl:choose>
			
			<!-- user name column -->
			<xsl:choose>
			  <xsl:when test="/cms/userClass != 'autor' and $sortBy = 'userRealName' " >
			    <td align="center" class="green"><a href="WCMSAdminServlet?action=logs&amp;sort=userRealName&amp;sortOrder={$flipedSortOrder}">Nutzer</a></td>
			  </xsl:when>
			  <xsl:when test="/cms/userClass != 'autor' and $sortBy != 'userRealName' " >
			    <th><a href="WCMSAdminServlet?action=logs&amp;sort=userRealName&amp;sortOrder={$currentSortOrder}">Nutzer</a></th>
			  </xsl:when>
			</xsl:choose>			
			
			<!-- page column -->
			<xsl:choose>
			  <xsl:when test=" $sortBy = 'labelPath' " >
			      <td align="center" class="green">
			         <a href="WCMSAdminServlet?action=logs&amp;sort=labelPath&amp;sortOrder={$flipedSortOrder}">Seite</a>
			      </td>
			  </xsl:when>
			  <xsl:otherwise>
			      <th><a href="WCMSAdminServlet?action=logs&amp;sort=labelPath&amp;sortOrder={$currentSortOrder}">Seite</a></th>

			  </xsl:otherwise>
			</xsl:choose>			
			
			<!-- done action column -->
			<xsl:choose>
			  <xsl:when test=" $sortBy = 'doneAction' " >
				  <td align="center" class="green"><a href="WCMSAdminServlet?action=logs&amp;sort=doneAction&amp;sortOrder={$flipedSortOrder}">Aktion</a></td>
			  </xsl:when>
			  <xsl:otherwise>
				  <th><a href="WCMSAdminServlet?action=logs&amp;sort=doneAction&amp;sortOrder={$currentSortOrder}">Aktion</a></th>
			  </xsl:otherwise>
			</xsl:choose>					  
			
			<!-- backup column -->
			<xsl:choose>
			  <xsl:when test="/cms/userClass = 'systemAdmin' and ($sortBy = 'backupContentFile' or $sortBy = 'backupNavigationFile') " >
		        <td align="center" class="green">
				 <b>
			      Backup<a href="WCMSAdminServlet?action=logs&amp;sort=backupContentFile&amp;sortOrder={$flipedSortOrder}"> (Inhalt</a>
				  <a href="WCMSAdminServlet?action=logs&amp;sort=backupNavigationFile&amp;sortOrder={$flipedSortOrder}">, Navigation)</a>
				 </b>
			    </td>		  		  		  		  		  
			  </xsl:when>
			  <xsl:when test="/cms/userClass = 'systemAdmin' and ($sortBy != 'backupContentFile' and $sortBy != 'backupNavigationFile') " >
		        <th>
			      Backup<a href="WCMSAdminServlet?action=logs&amp;sort=backupContentFile&amp;sortOrder={$currentSortOrder}"> (Inhalt</a>
				  <a href="WCMSAdminServlet?action=logs&amp;sort=backupNavigationFile&amp;sortOrder={$currentSortOrder}">, Navigation)</a>
			    </th>		  		  		  		  		  
			  </xsl:when>
			</xsl:choose>			  
	    </tr>
	    <!-- END OF: table headline -->		
		
		<!-- content cells -->
  	    <xsl:choose>
		  <!-- primary to sort by date -->
		  <xsl:when test="$sortBy = 'date' " >
			<xsl:for-each select="/cms/loggings/log">					  			
	  		  <xsl:sort select="@*[name() = $sortBy] | @*[concat('@',name()) = $sortBy]" order="{$currentSortOrder}" />  	
			  <xsl:sort select="@time" order="{$currentSortOrder}" />  	
			    <tr>
					  <!-- date, time -->
				      <td valign="top">
					    <xsl:value-of select="substring(@date,9,2)" />.<xsl:value-of select="substring(@date,6,2)" />.<xsl:value-of select="substring(@date,1,4)" />,
					     <br/>
						  <xsl:value-of select="@time" />	Uhr	  
						 <br/>
					  </td>
					  <!-- user -->			  
					  <xsl:if test="/cms/userClass != 'autor' " >			  
				        <td valign="top"><xsl:value-of select="@userRealName" /></td>
					  </xsl:if>				
					  <!-- label path -->  
				      <td valign="top"><xsl:value-of select="@labelPath" />
					
						 <!-- show given notes -->
						 <xsl:if test=" note != '' " >
							 <br/><br/>
								 Autor: 
								 <i>
									 '<xsl:value-of select=" note " />'
								 </i>
						 </xsl:if>
			      
				      </td>
					  
					  <!-- done action -->
						<xsl:choose>
						  <xsl:when test="@doneAction = 'add' " >
						      <td valign="top" style="color:#9C6300;">erstellt</td>							
						  </xsl:when>
						  <xsl:when test="@doneAction = 'edit' " >
						      <td valign="top" >geändert</td>							
						  </xsl:when>
						  <xsl:when test="@doneAction = 'delete' " >
						      <td valign="top" style="color:#AF0101;">gelöscht</td>							
						  </xsl:when>							  							
						  <xsl:when test="@doneAction = 'translate' " >
						      <td valign="top" style="color:#206F20;">übersetzt</td>							
						  </xsl:when>    
						</xsl:choose>
							
					  <!-- backup location -->
					  <xsl:if test="/cms/userClass = 'systemAdmin' " >		  
					    <td valign="top"><xsl:value-of select="@backupContentFile" />		  		  
							<br/>
						    <xsl:value-of select="@backupNavigationFile" />		  		  			
					    </td>		  		  		  		  		  
				 	  </xsl:if>				  
			    </tr>				
			  </xsl:for-each>								  
			</xsl:when>
			
		    <!-- primary NOT to sort by date -->			
			<xsl:otherwise>
 			  <xsl:for-each select="/cms/loggings/log">					  						
	  		    <xsl:sort select="@*[name() = $sortBy] | @*[concat('@',name()) = $sortBy]" order="{$currentSortOrder}" />  		
			    <xsl:sort select="@date" order="{$flipedSortOrder}" />  	
			    <xsl:sort select="@time" order="{$flipedSortOrder}" />  	
				    <tr>
						  <!-- date, time -->
					      <td valign="top">
						    <xsl:value-of select="substring(@date,9,2)" />.<xsl:value-of select="substring(@date,6,2)" />.<xsl:value-of select="substring(@date,1,4)" />,
						     <br/>
							  <xsl:value-of select="@time" />	Uhr	  
							 <br/>
						  </td>
						  <!-- user -->			  
						  <xsl:if test="/cms/userClass != 'autor' " >			  
					        <td valign="top"><xsl:value-of select="@userRealName" /></td>
						  </xsl:if>				
						  <!-- label path -->  
					      <td valign="top"><xsl:value-of select="@labelPath" /></td>

						  <!-- done action -->
							<xsl:choose>
							  <xsl:when test="@doneAction = 'add' " >
							      <td valign="top" style="color:#9C6300;">erstellt</td>							
							  </xsl:when>
							  <xsl:when test="@doneAction = 'edit' " >
							      <td valign="top" >geändert</td>							
							  </xsl:when>
							  <xsl:when test="@doneAction = 'delete' " >
							      <td valign="top" style="color:#AF0101;">gelöscht</td>							
							  </xsl:when>
						  <xsl:when test="@doneAction = 'translate' " >
						      <td valign="top" style="color:#206F20;">übersetzt</td>							
						  </xsl:when>							    
							</xsl:choose>

						  <!-- backup location -->
						  <xsl:if test="/cms/userClass = 'systemAdmin' " >		  
						    <td valign="top"><xsl:value-of select="@backupContentFile" />		  		  
								<br/>
							    <xsl:value-of select="@backupNavigationFile" />		  		  			
						    </td>		  		  		  		  		  
						  </xsl:if>				  
				    </tr>								  
			  </xsl:for-each>								  					
		    </xsl:otherwise>
	    </xsl:choose>		
		<!-- END OF : content cells -->		
		
	  </table>
	</td>
  </tr>
  <!-- END OF: log table -->
  
  <tr> 
    <td colspan="2"><br/></td>
  </tr>

  <!-- back button -->  
  <tr>
    <td colspan="2" align="right" width="100%">
	    <table cellspacing="0" cellpadding="0" align="right">
		  <tr>
				<td align="right">
					<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/templates/master/template_wcms/IMAGES/box_left.gif" 
						width="11" height="22" border="0" alt="" title="" />
				</td>
				 <td align="right" class="button">
			       <a href="{$ServletsBaseURL}WCMSLoginServlet">zurück zur WCMS-Startseite</a>
				 </td>
				<td align="right">
					<img 
						src="{$WebApplicationBaseURL}modules/module-wcms/uif/templates/master/template_wcms/IMAGES/box_right.gif" 
						width="11" height="22" border="0" alt="" title="" />
				</td>
		  </tr>
		</table>
    </td>
  </tr>
  <!-- END OF: back button -->        
   
  <tr> 
    <td colspan="2"><br/></td>
  </tr>   
   
</table>
 
</xsl:template>
<!-- END OF: logStatistic  ================================================================================= -->

</xsl:stylesheet>
