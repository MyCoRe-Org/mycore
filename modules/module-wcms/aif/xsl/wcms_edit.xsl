<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan">
	<!-- ============================================================================================================ -->
	<xsl:template name="wcmsEditContent">
		<xsl:param name="href"/>
		<!-- get label path -->
		<xsl:variable name="labelPath">
			<xsl:choose>
				<!-- given href is a built dir path of a menu -->
				<xsl:when test=" /cms/href[@mainMenu = 'true'] ">
					<xsl:for-each select="document($navigationBase)/navigation/*">
						<xsl:variable name="dirPath" select="concat( parent::node()/@dir , @dir )"/>
						<xsl:if test=" $dirPath = $href ">
							<xsl:value-of select="@label"/>
						</xsl:if>
					</xsl:for-each>
				</xsl:when>
				<!-- given href is a normal href included in the navigation.xml -->
				<xsl:otherwise>
					<xsl:variable name="preOrSuccessor">
						<xsl:if test="/cms/addAtPosition = 'predecessor' or /cms/addAtPosition = 'successor'  ">
							<xsl:value-of select="'true'"/>
						</xsl:if>
					</xsl:variable>
					<xsl:for-each select="document($navigationBase)/navigation//item[@href]">
						<xsl:if test="@href = $href">
							<xsl:for-each select="ancestor-or-self::*">
								<xsl:choose>
									<xsl:when test="position() = 2">
										<xsl:value-of select="@label"/>
									</xsl:when>
									<xsl:when test="position() > 2 and position() != last()">
										<xsl:value-of select="concat(' > ',./label)"/>
									</xsl:when>
									<xsl:when test="position() = last() and $preOrSuccessor != 'true'">
										<xsl:value-of select="concat(' > ',./label)"/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</xsl:if>
					</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- END OF: get label path -->
		<xsl:call-template name="buildInterface">
			<xsl:with-param name="href" select="$href"/>
			<xsl:with-param name="labelPath" select="$labelPath"/>
		</xsl:call-template>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface">
		<xsl:param name="href"/>
		<xsl:param name="labelPath"/>
		<!--
            <form name="editContent" action="{$ServletsBaseURL}WCMSActionServlet" method="post" onSubmit="switchKupuToHTML()" >
            -->
		<form name="editContent" action="{$ServletsBaseURL}WCMSActionServlet" method="post" onSubmit="switchKupuToHTML()">
			<input type="hidden" name="labelPath" value="{$labelPath}"/>
			<table width="90%" border="0" cellspacing="0" cellpadding="0" align="center">
				<xsl:call-template name="buildInterface.actionHeadline"/>
				<!-- error handling -->
				<xsl:call-template name="buildInterface.errorHandlingEdit"/>
				<tr>
					<!-- label path -->
					<td width="70%">
						<b> Seite: </b>
						<xsl:value-of select="concat('  ',$labelPath)"/>
						<xsl:if test="/cms/action = 'add'"> &gt; ...</xsl:if>
					</td>
					<!-- safe button -->
					<td align="right">
						<xsl:if test="/cms/action != 'delete'">
							<xsl:call-template name="buildInterface.general.safeButton"/>
						</xsl:if>
					</td>
				</tr>
				<tr>
					<td colspan="2">
						<br/>
					</td>
				</tr>
				<!-- display rigth interfaces -->
				<xsl:choose>
					<xsl:when test="/cms/action = 'delete'">
						<xsl:call-template name="buildInterface.delete">
							<xsl:with-param name="href" select="$href"/>
							<xsl:with-param name="labelPath" select="$labelPath"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="buildInterface.general">
							<xsl:with-param name="href" select="$href"/>
							<xsl:with-param name="labelPath" select="$labelPath"/>
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>
			</table>
		</form>
	</xsl:template>
	<!-- END OF: build interface  ================================================================================= -->
	<xsl:template name="buildInterface.actionHeadline">
		<xsl:choose>
			<xsl:when test="/cms/action = 'add'">
				<xsl:call-template name="wcms.headline">
					<xsl:with-param name="infoText" select="'Inhalt anlegen (Schritt 2/3)'"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="/cms/action = 'edit'">
				<xsl:call-template name="wcms.headline">
					<xsl:with-param name="infoText" select="'Inhalt bearbeiten (Schritt 2/3)'"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="/cms/action = 'delete'">
				<xsl:call-template name="wcms.headline">
					<xsl:with-param name="infoText" select="'Inhalt löschen (Schritt 2/3)'"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="/cms/action = 'translate'">
				<xsl:call-template name="wcms.headline">
					<xsl:with-param name="infoText" select="'Inhalt übersetzen (Schritt 2/3)'"/>
				</xsl:call-template>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.errorHandlingEdit">
		<xsl:choose>
			<xsl:when test=" /cms/error = 'emptyFormField' ">
				<tr>
					<th colspan="2" class="red" align="left"> Fehler aufgetreten: Formular nicht 
                                                vollständig ausgefüllt. <br/>
						<br/> Mindestens ein Formularfeld wurde nicht 
                                                ausgefüllt. </th>
				</tr>
				<tr>
					<td colspan="2">
						<br/>
						<br/>
					</td>
				</tr>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.delete">
		<xsl:param name="href"/>
		<xsl:param name="labelPath"/>
		<tr>
			<td colspan="2" height="10">
				<table cellspacing="0" cellpadding="0" align="left">
					<tr>
						<th class="red" width="100">
							<a href="{$ServletsBaseURL}WCMSActionServlet?delete=true&amp;labelPath={$labelPath}">
                                                             löschen</a>
						</th>
						<td width="50"/>
						<th class="green" width="100">
							<a href="{$ServletsBaseURL}WCMSActionServlet?delete=false"> nicht 
                                                            löschen</a>
						</th>
					</tr>
				</table>
			</td>
		</tr>
	</xsl:template>
	<!-- ================================================================================================= -->
	<xsl:template name="buildInterface.general">
		<xsl:param name="href"/>
		<xsl:param name="labelPath"/>
		<!-- meta data box -->
		<xsl:call-template name="buildInterface.general.metaData">
			<xsl:with-param name="href" select="$href"/>
			<xsl:with-param name="labelPath" select="$labelPath"/>
		</xsl:call-template>
		<!-- content -->
		<xsl:call-template name="buildInterface.general.content">
			<xsl:with-param name="href" select="$href"/>
		</xsl:call-template>
		<tr>
			<td colspan="2">
				<br/>
			</td>
		</tr>
		<!-- safe button -->
		<tr>
			<td align="right" colspan="2">
				<xsl:call-template name="buildInterface.general.safeButton"/>
			</td>
		</tr>
	</xsl:template>
	<!-- ================================================================================================= -->
	<xsl:template name="buildInterface.general.safeButton">
		<table cellspacing="0" cellpadding="0" align="right" class="env">
			<!-- submit -->
			<tr>
				<td align="right">
					<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/templates/master/template_wcms/IMAGES/box_left.gif" width="11" height="22" border="0" alt="" title=""/>
				</td>
				<td align="right" class="button_green">
					<xsl:choose>
						<xsl:when test="/cms/action/@mode = 'intern' ">
							<a href="javascript:switchKupuToHTML()">
								<b> Änderungen speichern</b>
							</a>
						</xsl:when>
						<xsl:otherwise>
							<a href="javascript:document.editContent.submit()">
								<b> Änderungen speichern</b>
							</a>
						</xsl:otherwise>
					</xsl:choose>
				</td>
				<td align="right">
					<img src="{$WebApplicationBaseURL}modules/module-wcms/uif/templates/master/template_wcms/IMAGES/box_right.gif" width="11" height="22" border="0" alt="" title=""/>
				</td>
			</tr>
			<!-- END OF: submit -->
		</table>
	</xsl:template>
	<!-- ================================================================================================= -->
	<xsl:template name="buildInterface.general.metaData">
		<xsl:param name="href"/>
		<xsl:param name="labelPath"/>
		<tr>
			<td colspan="2">
				<table width="100%" class="table_noGrid" border="0" cellspacing="0" cellpadding="0">
					<colgroup>
						<col width="50%"/>
						<col width="50%"/>
					</colgroup>
					<!-- head line -->
					<tr>
						<td align="center" class="green" colspan="2">
							<b> Metadaten</b>
						</td>
					</tr>
					<tr>
						<!-- label -->
						<th align="left" valign="top">
							<xsl:call-template name="buildInterface.general.metaData.label"/>
							<br/>
							<xsl:call-template name="buildInterface.general.metaData.getFileName"/>
							<!-- layout stuff -->                                          
	                                    <xsl:if test=" /cms/action != 'translate' " >
								<br/>                                    
								<span style="text-decoration:underline;"> Layout:</span>                                          
								<br/>
								<xsl:call-template name="buildInterface.general.metaData.selectTemplate">
									<xsl:with-param name="href" select="$href"/>
								</xsl:call-template>
								<xsl:call-template name="buildInterface.general.metaData.menuPointLayout"/>                                                                                    
	                                    </xsl:if>
						</th>
						<!-- navigation stuff -->
						<th align="left" valign="top">
	                                    <xsl:if test=" /cms/action != 'translate' ">
								<span style="text-decoration:underline;"> Navigationsverhalten:</span>
								<br/>
								<xsl:call-template name="buildInterface.general.metaData.target"/>
								<xsl:call-template name="buildInterface.general.metaData.replaceMenu"/>
								<xsl:call-template name="buildInterface.general.metaData.forwardToChildren"/>
								<xsl:call-template name="buildInterface.general.metaData.acl"/>	                                          
	                                    </xsl:if>                                          
						</th>
					</tr>
				</table>
			</td>
		</tr>
	</xsl:template>
	<!-- ================================================================================================= -->
	<xsl:template name="buildInterface.general.metaData.abstract">
		<span style="text-decoration:line-through;">
			<xsl:choose>
				<xsl:when test="/cms/action = 'translate'"> Kurzbeschreibung (<xsl:value-of select="$DefaultLang"/>):<br/> "<xsl:value-of select="/cms/abstract"/>" <br/>
					<br/> 
                                          übersetzte Kurzbeschreibung (<xsl:value-of select="$CurrentLang"/>):<br/>
					<textarea name="abstract_currentLang" rows="5" cols="60" wrap="off">
						<xsl:text> </xsl:text>
					</textarea>
				</xsl:when>
				<xsl:otherwise> Kurzbeschreibung:<br/>
					<textarea name="abstract" rows="4" cols="60" wrap="off">
						<xsl:text> </xsl:text>
					</textarea>
				</xsl:otherwise>
			</xsl:choose>
		</span>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.metaData.acl">
		<xsl:param name="href"/>
		<xsl:choose>
			<xsl:when test="/cms/action = 'edit' or /cms/action = 'add' ">
				<br/>
				<input type="checkbox" name="" value=""/>
				<span style="text-decoration:line-through;"> Zugriff auf Gruppe beschränken: </span>
				<input type="text" size="10" maxlength="40" name=""/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.metaData.getFileName">
		<xsl:if test="/cms/action = 'add' and /cms/action[@mode = 'intern'] ">
			<br/>Dateiname: <input class="inputfield" type="text" size="40" maxlength="35" name="href">
				<xsl:attribute name="value"/>
			</input>
                  <br/>
		</xsl:if>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.metaData.forwardToChildren">
		<xsl:param name="href"/>
		<xsl:choose>
			<xsl:when test="/cms/action = 'edit' ">
				<br/>
				<input type="checkbox" name="" value=""/>
				<span style="text-decoration:line-through;"> automatisch auf Unterseite weiterleiten: </span>
				<input type="text" size="10" maxlength="40" name=""/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.metaData.label">
		<xsl:choose>
			<xsl:when test="/cms/action='translate'"> Titel (<xsl:value-of select="$DefaultLang"/>):<br/> 
                                    "<xsl:value-of select="/cms/label"/>" <br/>
				<br/> übersetzter Titel (<xsl:value-of select="$CurrentLang"/>):<br/>
				<input type="text" size="60" maxlength="60" name="label_currentLang" style="border: 2px solid #9A5B11;">
					<xsl:attribute name="value"><xsl:value-of select="/cms/label_currentLang"/></xsl:attribute>
				</input>
			</xsl:when>
			<xsl:otherwise> Titel:<br/>
				<input type="text" size="60" maxlength="60" name="label" style="border: 2px solid #9A5B11;">
					<xsl:if test=" /cms/action = 'edit' ">
						<xsl:attribute name="value"><xsl:value-of select="/cms/label"/></xsl:attribute>
					</xsl:if>
				</input>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.metaData.menuPointLayout">
		<xsl:choose>
			<xsl:when test="/cms/action = 'add' or /cms/action = 'edit'">
				<br/> Anzeige im Menü: <select name="style" size="1">
					<xsl:choose>
						<xsl:when test="/cms/style = 'normal' ">
							<option selected="selected" value="normal"> normal </option>
							<option value="bold"> fett </option>
						</xsl:when>
						<xsl:otherwise>
							<option value="normal"> normal </option>
							<option selected="selected" value="bold"> fett </option>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:text> 
                                    </xsl:text>
				</select>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.metaData.replaceMenu">
		<xsl:choose>
			<xsl:when test="/cms/action = 'edit' or /cms/action = 'add' ">
				<xsl:if test="/cms/action[@mode='intern']">
					<br/>
					<xsl:choose>
						<xsl:when test="/cms/replaceMenu = 'true' ">
							<input type="checkbox" name="replaceMenu" value="true" checked="checked"/>
						</xsl:when>
						<xsl:otherwise>
							<input type="checkbox" name="replaceMenu" value="true"/>
						</xsl:otherwise>
					</xsl:choose> Ausgangspunkt für 
                                          neues Menü: </xsl:if>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.metaData.selectTemplate">
		<xsl:param name="href"/>
		<xsl:choose>
			<xsl:when test="/cms/action = 'add' or /cms/action = 'edit'"> Template: <!-- set template -->
				<!-- look for appropriate template entry and assign -> $template -->
				<xsl:variable name="template_temp">
					<!-- point to rigth item -->
					<xsl:for-each select="document($navigationBase) /navigation//item[@href = $href]">
						<!-- collect @template !='' entries along the choosen axis -->
						<xsl:for-each select="ancestor-or-self::*[  @template != '' ]">
							<xsl:if test="position()=last()">
								<xsl:value-of select="@template"/>
							</xsl:if>
						</xsl:for-each>
						<!-- END OF: collect @template !='' entries along the choosen axis -->
					</xsl:for-each>
					<!-- END OF: point to rigth item -->
				</xsl:variable>
				<xsl:variable name="template">
					<xsl:choose>
						<xsl:when test=" $template_temp != '' ">
							<xsl:value-of select="$template_temp"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="document($navigationBase) /navigation/@template"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<!-- END OF: look for appropriate template entry and assign -> $template -->
				<!-- check if template was heritated or real set -->
				<xsl:variable name="templateSet">
					<xsl:for-each select="document($navigationBase) /navigation//item[@href = $href]">
						<xsl:choose>
							<xsl:when test=" @template != '' ">
								<xsl:value-of select="'yes'"/>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="'no'"/>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:for-each>
				</xsl:variable>
				<!-- END OF: check if template was heritated or real set -->
				<!-- look for parent of appropriate template entry and assign -> $templateParent -->
				<xsl:variable name="templateParent">
					<!-- point to rigth item -->
					<xsl:for-each select="document($navigationBase) /navigation//item[@href = $href]">
						<!-- collect @template !='' entries along the choosen axis -->
						<xsl:for-each select="ancestor-or-self::*[  @template != '' ]">
							<xsl:if test="position()=last()-1">
								<xsl:value-of select="@template"/>
							</xsl:if>
						</xsl:for-each>
						<!-- END OF: collect @template !='' entries along the choosen axis -->
					</xsl:for-each>
					<!-- END OF: point to rigth item -->
				</xsl:variable>
				<!-- END OF: look for parent of appropriate template entry and assign -> $templateParent -->
				<select size="1" name="masterTemplate">
					<xsl:for-each select="/cms/templates/master/template">
						<xsl:choose>
							<!-- template is set-->
							<xsl:when test=" $templateSet = 'yes' ">
								<xsl:choose>
									<xsl:when test=" /cms/action = 'add' and /cms/addAtPosition = 'child' ">
										<xsl:choose>
											<!-- current template is the valid template -->
											<xsl:when test=" node() = $template ">
												<option selected="selected">
													<xsl:attribute name="value"><xsl:value-of select="'noAction'"/></xsl:attribute>
													<xsl:value-of select="node()"/> (Vorlage) </option>
											</xsl:when>
											<!-- END OF: current template is the valid template -->
											<xsl:otherwise>
												<option>
													<xsl:attribute name="value"><xsl:value-of select="node()"/></xsl:attribute>
													<xsl:value-of select="node()"/>
												</option>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test=" /cms/action = 'add' and ( /cms/addAtPosition = 'predecessor' or /cms/addAtPosition = 'successor'  ) ">
										<xsl:choose>
											<!-- current template is the valid template -->
											<xsl:when test=" node() = $templateParent ">
												<option selected="selected">
													<xsl:attribute name="value"><xsl:value-of select="'noAction'"/></xsl:attribute>
													<xsl:value-of select="node()"/> (Vorlage) </option>
											</xsl:when>
											<!-- EN DOF:current template is the valid template -->
											<xsl:otherwise>
												<option>
													<xsl:attribute name="value"><xsl:value-of select="node()"/></xsl:attribute>
													<xsl:value-of select="node()"/>
												</option>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:when test=" /cms/action = 'edit' ">
										<xsl:choose>
											<!-- current template is the valid template -->
											<xsl:when test=" node() = $template ">
												<option selected="selected">
													<xsl:attribute name="value"><xsl:value-of select="'noAction'"/></xsl:attribute>
													<xsl:value-of select="node()"/> (hier gesetzt) </option>
											</xsl:when>
											<!-- END OF: current template is the valid template -->
											<xsl:when test=" node() = $templateParent ">
												<option>
													<xsl:attribute name="value"><xsl:value-of select="'delete'"/></xsl:attribute>
													<xsl:value-of select="node()"/> (Vorlage) </option>
											</xsl:when>
											<xsl:otherwise>
												<option>
													<xsl:attribute name="value"><xsl:value-of select="node()"/></xsl:attribute>
													<xsl:value-of select="node()"/>
												</option>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
								</xsl:choose>
							</xsl:when>
							<!-- END OF: template is set -->
							<!-- template not set-->
							<xsl:otherwise>
								<xsl:choose>
									<!-- current template is the valid template -->
									<xsl:when test=" node() = $template ">
										<option selected="selected">
											<xsl:attribute name="value"><xsl:value-of select="'noAction'"/></xsl:attribute>
											<xsl:value-of select="node()"/> (Vorlage) </option>
									</xsl:when>
									<!-- END OF: current template is the valid template -->
									<xsl:otherwise>
										<option>
											<xsl:attribute name="value"><xsl:value-of select="node()"/></xsl:attribute>
											<xsl:value-of select="node()"/>
										</option>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:otherwise>
							<!-- END OF: template not set -->
						</xsl:choose>
					</xsl:for-each>
					<xsl:text> 
                                    </xsl:text>
				</select>
				<!-- END OF: set template -->
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- END OF: WCMS - select template  ================================================================================= -->
	<xsl:template name="buildInterface.general.metaData.target">
		<xsl:choose>
			<xsl:when test="/cms/action = 'edit' or /cms/action = 'add' "> Seite öffnet sich im: <select name="target" size="1">
					<xsl:choose>
						<xsl:when test="/cms/target = '_self' ">
							<option class="select" selected="selected" value="_self"> selben Fenster </option>
							<option value="_blank"> neuen Fenster </option>
						</xsl:when>
						<xsl:otherwise>
							<option class="select" value="_self"> selben Fenster </option>
							<option selected="selected" value="_blank"> neuen 
                                    Fenster </option>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:text> </xsl:text>
				</select>
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content">
		<xsl:param name="href"/>
		<tr>
			<td colspan="2">
				<br/>
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<table width="100%" class="table_noGrid" border="0" cellspacing="0" cellpadding="0">
					<tr>
						<td colspan="2" align="center" class="green" valign="bottom">
							<b> Editor zum Inhalt der Seite:</b>
						</td>
					</tr>
					<tr>
						<th align="left" valign="top">
							<xsl:call-template name="buildInterface.general.content.dynamicContentBinding">
								<xsl:with-param name="href" select="$href"/>
							</xsl:call-template>
							<xsl:call-template name="buildInterface.general.content.changeInfo">
								<xsl:with-param name="href" select="$href"/>
							</xsl:call-template>
						</th>
						<th align="left" valign="top">
							<xsl:call-template name="buildInterface.general.content.multimedia"/>
						</th>
					</tr>
					<tr>
						<th align="left" colspan="2">
							<!-- get content / link -->
							<xsl:choose>
								<xsl:when test="/cms/action = 'translate'">
									<!-- internal content -->
									<xsl:call-template name="buildInterface.general.content.getContent"/>
								</xsl:when>
								<xsl:otherwise>
									<!-- internal content -->
									<xsl:call-template name="buildInterface.general.content.getContent"/>
									<!-- external new content -->
									<xsl:call-template name="buildInterface.general.content.getExternalHREF"/>
									<!-- external existing content -->
									<xsl:call-template name="buildInterface.general.content.showExistingHREF">
										<xsl:with-param name="href" select="$href"/>
									</xsl:call-template>
								</xsl:otherwise>
							</xsl:choose>
							<!-- END OF: get content / link -->
						</th>
					</tr>
				</table>
			</td>
		</tr>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content.changeInfo">
		<xsl:if test="/cms/action = 'edit'">
			<br/>
			<br/>
			<br/>Was wurde verändert:<br/>
			<textarea name="changeInfo" rows="2" cols="40" wrap="off">
			</textarea>
		</xsl:if>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content.dynamicContentBinding">
		<xsl:param name="href"/>
		<xsl:choose>
			<xsl:when test="/cms/action = 'edit' or /cms/action = 'add' ">
				<!-- check if dynamicContentBinding exist -> $dcb-->
				<xsl:variable name="dcb">
					<xsl:if test=" count(document($navigationBase) /navigation//item[@href = $href]/dynamicContentBinding/rootTag) &gt; 0 ">
						<xsl:value-of select="'yes'"/>
					</xsl:if>
				</xsl:variable>
				<!-- END OF: check if dynamicContentBinding exist -> $dcb -->
                                     Dynamischer Inhalt:<br/>
				<br/>
				<!-- input field -->
				<input type="checkbox" name="dcbActionAdd" value="dcbActionAdd"/>XML binden:<input type="text" size="30" maxlength="40" name="dcbValueAdd"/>
				<!-- existing dcb root nodes -->
				<xsl:if test=" /cms/action = 'edit' and $dcb = 'yes' ">
					<br/>
					<input type="checkbox" name="dcbActionDelete" value="dcbActionDelete"/>
                              XML lösen: 
                              <!-- list of all dynamicContentBinding rootNodes -->
					<select name="dcbValueDelete" size="1">
						<xsl:for-each select="document($navigationBase) /navigation//item[@href = $href]/dynamicContentBinding/rootTag">
							<option>
								<xsl:value-of select="."/>
							</option>
						</xsl:for-each>
						<xsl:text> 
                                    </xsl:text>
					</select>
				</xsl:if>
				<!-- END OF: existing dcb root nodes -->
			</xsl:when>
		</xsl:choose>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content.validateXHTML">
		<!-- test if content is valid XHTML -->
		<xsl:if test=" /cms/error = 'invalidXHTML' ">
			<table>
				<tr>
					<td align="left" class="red">
						<b> Achtung: <br/>
						</b> Der Inhalt der Seite ist nicht 
                                                XHTML konform! Die Seite wird dadurch zwar gespeichert, kann aber nicht angezeigt 
                                                werden. <br/>
						<b>Tip:</b> Nutzen sie <a target="_blank" href="http://validator.w3.org/">XHTML-Syntaxprüfer</a>, um die Seite XHTL 
                                                konform zu machen. </td>
				</tr>
			</table>
		</xsl:if>
		<!-- END OF: test if content is valid XHTML -->
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content.getContent">
		<xsl:if test="/cms/action[@mode = 'intern'] ">
			<xsl:choose>
				<xsl:when test="/cms/action = 'translate'"> 
					<input type="hidden" name="dummy" value="true"/>                              
                              übersetzter Inhalt (<xsl:value-of select="$CurrentLang"/>):<br/>
	                        <xsl:call-template name="buildInterface.general.content.validateXHTML"/>
					<xsl:call-template name="kupu"/>
				</xsl:when>
				<xsl:otherwise>
                              <div id="test">
						<input type="hidden" name="dummy" value="true"/>.
                                    <br/>
						<span style="text-decoration:underline;">Statischer Inhalt:</span>
	                              <xsl:call-template name="buildInterface.general.content.validateXHTML"/>
                              </div>
					<xsl:call-template name="kupu"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content.getExternalHREF">
		<xsl:if test="/cms/action[@mode = 'extern'] and /cms/action = 'add'"> Linkadresse:<br/>
			<input class="inputfield" type="text" size="120" maxlength="256" name="href">
				<xsl:attribute name="value"/>
			</input>
		</xsl:if>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content.multimedia">
		<xsl:if test="/cms/action[@mode='intern'] = 'add' or /cms/action[@mode='intern'] = 'edit'">
			<table align="right" class="table_noGrid" border="1" cellspacing="0" cellpadding="0">
				<tr>
					<td align="center" class="green" colspan="2">
						<b> Multimedia:</b>
					</td>
				</tr>
				<tr>
					<th valign="top" align="left">
						<!-- list of available imaages --> Bilder:<br/>
						<input type="hidden" name="JavaScriptImagePath_hidden" value="{concat($WebApplicationBaseURL,/cms/imagePath)}"/>
						<select size="1" name="selectPicturePreview" onchange="previewPicture(document.editContent.JavaScriptImagePath_hidden.value)">
							<xsl:for-each select="/cms/images/image">
								<option>
									<xsl:attribute name="value"><xsl:value-of select="node()"/></xsl:attribute>
									<xsl:value-of select="node()"/>
								</option>
							</xsl:for-each>
							<xsl:text> </xsl:text>
						</select>
						<!-- END OF: list of available imaages -->
						<!-- list of available documents -->
						<br/>
						<br/>sonst. Dokumente:<br/>
						<select size="1">
							<xsl:for-each select="/cms/documents/document">
								<option>
									<xsl:attribute name="value"><xsl:value-of select="node()"/></xsl:attribute>
									<xsl:value-of select="node()"/>
								</option>
							</xsl:for-each>
							<xsl:text> </xsl:text>
						</select>
						<!-- END OF: list of available documents -->
						<!-- upload -->
						<br/>
						<br/>
						<a target="blank" href="{$ServletsBaseURL}WCMSFileUploadServlet?action=select">Bild oder sonst. 
                                                Dokument hochladen</a>
					</th>
					<!-- picture preview -->
					<th align="right" valign="top">
						<img name="image" width="120" height="120" border="0" alt="" title="">
							<xsl:attribute name="src"><xsl:value-of select="concat($WebApplicationBaseURL,substring-after(/cms/imagePath,'/'),/cms/images/image)"/></xsl:attribute>
						</img>
					</th>
				</tr>
				<!-- END OF: picture preview -->
				<tr>
					<th align="left" colspan="2" style="font-size:10px;font-weight:normal;">
						<!-- image path -->
						<b> Pfad für Bilder:</b>
						<br/>
						<xsl:value-of select="concat($WebApplicationBaseURL,substring-after(/cms/imagePath,'/'),'...')"/>
						<!-- document path -->
						<br/>
						<b> Pfad für Dokumente:</b>
						<br/>
						<xsl:value-of select="concat($WebApplicationBaseURL,substring-after(/cms/documentPath,'/'),'...')"/>
					</th>
				</tr>
			</table>
		</xsl:if>
	</xsl:template>
	<!-- ============================================================================================================ -->
	<xsl:template name="buildInterface.general.content.showExistingHREF">
		<xsl:param name="href"/>
		<xsl:if test="/cms/action[@mode = 'extern'] and /cms/action = 'edit'"> Linkadresse:<br/>
			<input class="inputfield" type="text" size="120" maxlength="256" name="href">
				<xsl:attribute name="value"><xsl:value-of select="$href"/></xsl:attribute>
			</input>
		</xsl:if>
	</xsl:template>
	<!-- ============================================================================================================ -->
</xsl:stylesheet>
