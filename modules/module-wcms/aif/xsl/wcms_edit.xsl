<?xml version="1.0" encoding="UTF-8"?>

<!-- =====================================================================================
========================================================================================={
title: wcms_edit.xsl

Erstellt die Bearbeitungmaske zuvor ausgewaehlter Aktion.

	- Menüpunkt anlegen
	- Inhalt bearbeiten (wcmsEditContent)
	- Menuepunkt loeschen
	- Inhalt uebersetzen

template:
	- wcmsEditContent													wcmsEdit.buildInterface
		- buildInterfaceDelete											wcmsEdit.buildInterface.delete
		- buildInterfaceGeneral											wcmsEdit.buildInterface.general
			- errorOnBuildInterfaceGeneral								wcmsEdit.buildInterface.general.error
			- menuePunktName											wcmsEdit.buildInterface.general.menuPoint
			- buildInterface.general.safeButton							wcmsEdit.buildInterface.general.sendButton
			- buildInterface.general.content.changeInfo					wcmsEdit.buildInterface.general.changeComment
			- buildInterface.general.metaData.selectTemplate			wcmsEdit.buildInterface.general.options.template
			- buildInterface.general.metaData.menuPointLayout			wcmsEdit.buildInterface.general.options.menuPointStyle
			- buildInterface.general.metaData.target					wcmsEdit.buildInterface.general.options.target
			- buildInterface.general.metaData.replaceMenu				wcmsEdit.buildInterface.general.options.replaceMenu
			- buildInterface.general.metaData.forwardToChildren			wcmsEdit.buildInterface.general.options.forwardToChildren
			- buildInterface.general.metaData.acl						wcmsEdit.buildInterface.general.options.access
			- buildInterface.general.content.dynamicContentBinding		wcmsEdit.buildInterface.general.options.dynamicContent
			- buildInterface.general.content.multimedia					wcmsEdit.buildInterface.general.options.multimedia

}=========================================================================================
====================================================================================== -->

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan">


<!-- ====================================================================================={
section: Template: name="wcmsEditContent"

	- erzeugt Variable: labelPath
	- initialisiert den Seitenaufbau 

Aufruf:	
	- buildInterfaceDelete
	- buildInterfaceGeneral
}===================================================================================== -->
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
										<xsl:value-of select="concat(' » ',./label)"/>
									</xsl:when>
									<xsl:when test="position() = last() and $preOrSuccessor != 'true'">
										<xsl:value-of select="concat(' » ',./label)"/>
									</xsl:when>
								</xsl:choose>
							</xsl:for-each>
						</xsl:if>
					</xsl:for-each>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<!-- END OF: get label path -->

		<!-- Je nach gewaehlter Aktion-->
		<xsl:choose>
			<!-- Aktion: Menuepunkt loeschen -->
			<xsl:when test="/cms/action = 'delete'">
				<xsl:call-template name="buildInterfaceDelete">
					<xsl:with-param name="href" select="$href"/>
					<xsl:with-param name="labelPath" select="$labelPath"/>
				</xsl:call-template>
			</xsl:when>
			<!-- Aktion: anlegen, bearbeiten oder uebersetzen -->
			<xsl:otherwise>
				<xsl:call-template name="buildInterfaceGeneral">
					<xsl:with-param name="href" select="$href"/>
					<xsl:with-param name="labelPath" select="$labelPath"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterfaceDelete"

	- Sicherheitsabfrage vor dem Loeschen eines Menuepunktes

Aufruf:
	- menuleiste
	- zeigeSeitenname
}===================================================================================== -->
	<xsl:template name="buildInterfaceDelete">
		<xsl:param name="href"/>
		<xsl:param name="labelPath"/>

		<!-- Menueleiste einblenden, Parameter = ausgewaehlter Menuepunkt -->
		<xsl:call-template name="menuleiste">
			<xsl:with-param name="menupunkt" select="'Bearbeiten'" />
		</xsl:call-template>

		<!-- Seitenname -->
		<xsl:call-template name="zeigeSeitenname">
			<xsl:with-param name="seitenname" select="'Löschen des Menüpunktes'" />
		</xsl:call-template>

			<!-- Inhaltsbereich -->
			<div id="delete-width">
				<div id="delete">
					<div class="titel">Löschen</div>
					<div class="inhalt">
						<p>
							<xsl:text>Folgender Menüpunkt wird entfernt:</xsl:text>
						</p>
						<p>
							<span><xsl:value-of select="$labelPath"/></span>
						</p>
					</div>
					<div class="knoepfe">
						<a href="{$ServletsBaseURL}WCMSActionServlet?delete=false&amp;wcmsID=0024" class="button">
							<xsl:text>Nein, nicht löschen.</xsl:text>
						</a>
						<a href="{$ServletsBaseURL}WCMSActionServlet?delete=true&amp;labelPath={$labelPath}&amp;wcmsID=0024" class="button">
							<xsl:text>Ja, löschen.</xsl:text>
						</a>
					</div>
				</div>
			<!-- Ende: Inhaltsbereich -->
			</div>
		</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterfaceGeneral"

	- erzeugt das Formular zum Anlegen, Bearbeiten, 
	Uebersetzen von Menuepunkten

Aufruf:
	- menuleiste
	- zeigeSeitenname
	- errorOnBuildInterfaceGeneral
	- menuePunktName
	- buildInterface.general.safeButton
	- buildInterface.general.content.changeInfo
	- buildInterface.general.metaData
	- buildInterface.general.content.dynamicContentBinding
	- buildInterface.general.content.multimedia
}==================================================================================== -->

	<xsl:template name="buildInterfaceGeneral">
		<xsl:param name="href"/>
		<xsl:param name="labelPath"/>

		<!-- Menueleiste, Parameter = ausgewaehlter Menuepunkt -->
		<xsl:call-template name="menuleiste">
			<xsl:with-param name="menupunkt" select="'Bearbeiten'" />
		</xsl:call-template>

		<!-- Seitenname -->
		<xsl:call-template name="zeigeSeitenname">
			<xsl:with-param name="seitenname" select="'Inhalt bearbeiten'" />
		</xsl:call-template>

		<!-- lokale Navigation -->
		<div id="info">
			<a href="{$ServletsBaseURL}WCMSAdminServlet?action=choose&amp;wcmsID=0024" class="button">Zurück zur Auswahl</a>
			<a href="#editor" class="button">Editor zentrieren</a>
		</div>

		<!-- Editor -->
		<div id="editor-width">
			<form name="editContent" action="{$ServletsBaseURL}WCMSActionServlet?wcmsID=0024" method="post">
				<input type="hidden" name="labelPath" value="{$labelPath}"/>



				<!-- Fehlermeldung -->
				<xsl:if test="/cms/error !=''">
					<div id="editorFehler">
						<xsl:call-template name="errorOnBuildInterfaceGeneral"/>
					</div>
				</xsl:if>

				<!-- Inhalt Menuename-->
				<div id="menupunkt_info">
					<fieldset>
						<label for="position">Position</label>
						<span name="position"><xsl:value-of select="$labelPath"/></span>
						<br/>
						<label for="label">Name</label>
						<xsl:call-template name="menuePunktName"/>
						<br/>
						<xsl:if test="/cms/action[@mode = 'intern'] and /cms/action = 'add'">
							<label for="href">Dateiname</label>
							<input type="text" size="40" maxlength="35" name="href" value="" />
							<br/>
						</xsl:if>
						<xsl:if test="/cms/action[@mode = 'extern']">
							<label for="href">Verweis auf URL:</label>
							<xsl:choose>
								<!-- external new content -->
								<xsl:when test="/cms/action = 'add'">
									<input type="text" size="120" maxlength="256" name="href" class="input">
										<xsl:attribute name="value">http://</xsl:attribute>
									</input>
								</xsl:when>
								<!-- external existing content -->
								<xsl:when test="/cms/action = 'edit'">
									<input type="text" size="120" maxlength="256" name="href" class="input">
										<xsl:attribute name="value"><xsl:value-of select="$href"/></xsl:attribute>
									</input>
								</xsl:when>
							</xsl:choose>
							<br/>
						</xsl:if>
					</fieldset>
				</div>

				<!-- Inhalt Seiteninhalt -->
				<div id="editor">
					<xsl:if test="/cms/action[@mode = 'intern'] ">
						<xsl:call-template name="fck"/>
						<input type="hidden" name="dummy" value="true"/>
					</xsl:if>
				</div>

				<!-- Speichern Inhalt -->
				<div id="speichern">
					<!-- div class="comment"-->
						<fieldset>
							<xsl:if test="/cms/action = 'edit'">
								<div id="kommentar">
									<!--label for="changeInfo">Kommentar</label-->
									<textarea name="changeInfo" cols="30" rows="3" onclick="this.select();">
										<xsl:text>Kein Kommentar abgegeben</xsl:text>
									</textarea>
								</div>
							</xsl:if>
							<div id="saveSwitches">
								<xsl:call-template name="buildInterface.general.safeButton"/>
								<!-- <a class="button" href="javascript:zeigeElement();">Optionen</a> -->
								<!-- <a class="button" href="#top">top</a> -->
								<a class="button" href="{$ServletsBaseURL}WCMSAdminServlet?action=choose&amp;wcmsID=0024">Abbrechen</a>
							</div>
						</fieldset>
					<!-- streckt div main so daß er seine Kinder umschließt -->
					<div class="clear">
						&#160;
					</div>
				</div>

				<!-- Inhalt Einstellungen -->
				<xsl:if test="/cms/action = 'add' or /cms/action = 'edit'">
					<div id="optionen">
						<div id="option-left">
							<fieldset>
								<label for="masterTemplate">Gestaltung</label>
								<xsl:call-template name="buildInterface.general.metaData.selectTemplate">
									<xsl:with-param name="href" select="$href"/>
								</xsl:call-template>
								<br />
								<label for="target">Darstellung</label>
								<xsl:call-template name="buildInterface.general.metaData.target"/>
								<br />
								<label for="replaceMenu">Menüwurzel</label>
								<xsl:call-template name="buildInterface.general.metaData.replaceMenu"/>
								<br />
							</fieldset>
						</div>
						<div id="option-left-2">
							<fieldset>
								<xsl:call-template name="buildInterface.general.content.dynamicContentBinding">
									<xsl:with-param name="href" select="$href"/>
								</xsl:call-template>
							</fieldset>
						</div>
						<div class="clear">
							&#160;
						</div>
					</div>
				</xsl:if>

				<xsl:call-template name="buildInterface.general.metaData.menuPointLayout"/>
				<!-- xsl:call-template name="buildInterface.general.metaData.forwardToChildren"/-->
				<!-- xsl:call-template name="buildInterface.general.metaData.acl"/-->
				<!-- xsl:call-template name="buildInterface.general.content.multimedia"/ -->

			<!-- streckt div main so, dass es seine Kinder umschließt -->
					<div class="clear">
						&#160;
					</div>

			</form>
		</div>
		<!-- Ende: Inhaltsbereich -->
	</xsl:template>

	<xsl:template name="fck">

		<xsl:choose>
			<xsl:when test="/cms/action = 'translate'">
				<textarea id="fck_editor" name="content_currentLang" width="80" height="20" editorWidth="640" editorHeight="320">
					<xsl:copy-of select="document(concat($WebApplicationBaseURL,/cms/href,'?XSL.Style=xml'))/MyCoReWebPage/section[@xml:lang=$CurrentLang]/node()" />
				</textarea>
			</xsl:when>
			<xsl:otherwise>
				<textarea id="fck_editor" name="content" width="80" height="20" editorWidth="640" editorHeight="320">
					<xsl:copy-of select="document(concat($WebApplicationBaseURL,/cms/href,'?XSL.Style=xml'))/MyCoReWebPage/section[@xml:lang=$CurrentLang]/node()" />				
				</textarea>							
			</xsl:otherwise>
		</xsl:choose>

      <script type="text/javascript"><xsl:text>
        window.onload = function()
        {
      	  var oFCKeditor = new FCKeditor( '</xsl:text>
      	  <xsl:value-of select="'fck_editor'" />
      	  <xsl:text>' ) ;
      	  oFCKeditor.BasePath	= '</xsl:text>
      	  <xsl:value-of select="concat($WebApplicationBaseURL,'fck/')" />
      	  <xsl:text>' ;
      	  oFCKeditor.Height = </xsl:text>
      	  <xsl:value-of select="320" />
      	  <xsl:text> ;
          oFCKeditor.ToolbarSet = 'mcr' ;
      	  oFCKeditor.ReplaceTextarea() ;
        }
        </xsl:text></script>
	</xsl:template>


<!-- ====================================================================================={
section: Template: name="errorOnBuildInterfaceGeneral"
	- Auswertung erhaltener Fehlermeldungen 
}===================================================================================== -->
	<xsl:template name="errorOnBuildInterfaceGeneral">
		<table>
			<tr>
				<td>
					<script LANGUAGE="JAVASCRIPT">
						schreibeDatum();
					</script>
				</td>
				<td>
					<xsl:choose>
						<xsl:when test=" /cms/error = 'emptyFormField' ">
							Beim Speichern trat ein Fehler auf.<br />
							Es sind weitere Angaben notwendig.<br />
							Prüfe ob ein Name und ein Dateiname angegeben wurde.
						</xsl:when>
						<xsl:when test=" /cms/error = 'non_valid_xhtml_content' ">
							Fehler aufgetreten: Eingegebener Content war nicht XHTML/XML konform.
							<br/>
							Deaktivieren sie den Haken bei "HTML-Codekorrektur ausschalten" oder 
							überprüfen sie ihren Content.
						</xsl:when>
						<xsl:when test=" /cms/error = 'invalidXHTML' ">
							Der Inhalt der Seite ist nicht XHTML konform! 
							Die Seite wird dadurch zwar gespeichert, kann aber nicht angezeigt werden. 
							<br/>
							Tip: Nutzen sie <a target="_blank" href="http://validator.w3.org/">
							XHTML-Syntaxprüfer</a>, um die Seite XHTL konform zu machen.
						</xsl:when>
						<xsl:otherwise>
							Unbekannter Fehler aufgetreten.
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</tr>
		</table>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="menuePunktName"
	- Eingabefeld fuer den Namen des Menuepunktes
	- Anzeige des Namen in der Defaultsprache
}===================================================================================== -->
	<xsl:template name="menuePunktName">
		<xsl:choose>
			<xsl:when test="/cms/action='translate'">
				<xsl:text>[</xsl:text>
				<xsl:value-of select="$DefaultLang"/>
				<xsl:text>] </xsl:text>
				<xsl:value-of select="/cms/label"/>
				<br />
				<xsl:text>[</xsl:text>
				<xsl:value-of select="$CurrentLang"/>
				<xsl:text>] </xsl:text>
				<input type="text" size="60" maxlength="60" name="label" class="text">
					<xsl:attribute name="value">
						<xsl:value-of select="/cms/label_currentLang"/>
					</xsl:attribute>
				</input>
			</xsl:when>
			<xsl:otherwise>
				<input type="text" size="60" maxlength="60" name="label" class="text">
					<xsl:if test=" /cms/action = 'edit' ">
						<xsl:attribute name="value">
							<xsl:value-of select="/cms/label"/>
						</xsl:attribute>
					</xsl:if>
				</input>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.safeButton"
	- Knopf zum Speichern der Aenderungen
}===================================================================================== -->
	<xsl:template name="buildInterface.general.safeButton">
		<a  class="button" href="javascript:document.editContent.submit()">Änderungen speichern</a>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.metaData.selectTemplate"
	- Auswahlbox fuer ein eigenes Template
}===================================================================================== -->
	<xsl:template name="buildInterface.general.metaData.selectTemplate">
		<xsl:param name="href"/>

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

		<select size="1" name="masterTemplate" class="auswahl">
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
											<xsl:value-of select="node()"/> [ist aktiv] </option>
									</xsl:when>
									<!-- END OF: current template is the valid template -->
									<xsl:when test=" node() = $templateParent ">
										<option>
											<xsl:attribute name="value"><xsl:value-of select="'delete'"/></xsl:attribute>
											<xsl:value-of select="node()"/> [ist aktiv] </option>
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
			<xsl:text> </xsl:text>
		</select>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.metaData.menuPointLayout"
	- Auswahlbox zur Darstellung des Menuepunktes
}===================================================================================== -->
	<xsl:template name="buildInterface.general.metaData.menuPointLayout">
		<xsl:choose>
			<xsl:when test="/cms/action = 'add' or /cms/action = 'edit'">
				<!--
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
					<xsl:text> </xsl:text>
				</select>
				-->
				<input name="style" type="hidden" value="normal" />
			</xsl:when>
		</xsl:choose>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.metaData.target"
	- Ziel des Verweises festlegen
}===================================================================================== -->
	<xsl:template name="buildInterface.general.metaData.target">
		<select name="target" size="1" class="auswahl">
			<xsl:choose>
				<xsl:when test="/cms/target = '_self' ">
					<option class="select" selected="selected" value="_self">im selben Fenster</option>
					<option value="_blank">im neuen Fenster</option>
				</xsl:when>
				<xsl:otherwise>
					<option class="select" value="_self">im selben Fenster</option>
					<option selected="selected" value="_blank">im neuen Fenster</option>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:text> </xsl:text>
		</select>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.metaData.replaceMenu"
	- Auswahl zum Setzen eines neuen Wurzelmenuepunktes
}===================================================================================== -->
	<xsl:template name="buildInterface.general.metaData.replaceMenu">
		<xsl:if test="/cms/action[@mode='intern']">
			<xsl:choose>
				<xsl:when test="/cms/replaceMenu = 'true' ">
					<input type="checkbox" name="replaceMenu" value="true" checked="checked" class="box"/>
					<span><xsl:text> übergeordnete Menüpunkte ausblenden</xsl:text></span>
				</xsl:when>
				<xsl:otherwise>
					<input type="checkbox" name="replaceMenu" value="true" class="box"/>
					<span><xsl:text> übergeordnete Menüpunkte ausblenden</xsl:text></span>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.metaData.forwardToChildren"
	- unbekanntes Feature [deaktiviert]
}===================================================================================== -->
	<xsl:template name="buildInterface.general.metaData.forwardToChildren">
		<xsl:param name="href"/>
		<xsl:choose>
			<xsl:when test="/cms/action = 'edit' ">
				<br/>
				<input type="checkbox" name="" value="" class="box"/>
				<span style="text-decoration:line-through;"> automatisch auf Unterseite weiterleiten: </span>
				<input type="text" size="10" maxlength="40" name=""/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.metaData.acl"
	- Zugriffsberechtigung festlegen [deaktiviert]
}===================================================================================== -->
	<xsl:template name="buildInterface.general.metaData.acl">
		<xsl:param name="href"/>
		<xsl:choose>
			<xsl:when test="/cms/action = 'edit' or /cms/action = 'add' ">
				<br/>
				<input type="checkbox" name="" value="" class="ckeckbox"/>
				<span style="text-decoration:line-through;"><xsl:text> Zugriff auf Gruppe beschränken</xsl:text></span>
				<input type="text" size="10" maxlength="40" name="" class="text"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.content.dynamicContentBinding"
	- Angabe eines Typs zur Zuweisung von dynamischen Seiten
}===================================================================================== -->
	<xsl:template name="buildInterface.general.content.dynamicContentBinding">
		<xsl:param name="href"/>

		<!-- check if dynamicContentBinding exist -> $dcb-->
		<xsl:variable name="dcb">
			<xsl:if test=" count(document($navigationBase) /navigation//item[@href = $href]/dynamicContentBinding/rootTag) &gt; 0 ">
				<xsl:value-of select="'yes'"/>
			</xsl:if>
		</xsl:variable>
		<!-- END OF: check if dynamicContentBinding exist -> $dcb -->

		<label for="dcbValueAdd">Dynamischer Inhalt</label>
		<input type="text" size="30" maxlength="40" name="dcbValueAdd" class="text"/>
		<br/>
		<label for="dcbActionAdd">&#160;</label>
		<input type="checkbox" name="dcbActionAdd" value="dcbActionAdd"  class="box"/>
		<span><xsl:text> XML-Knoten zuordnen</xsl:text></span>
		<br />
		<xsl:if test=" /cms/action = 'edit' and $dcb = 'yes' ">
			<label for="dcbValueDeleteSelect">&#160;</label>
			<select name="dcbValueDeleteSelect" size="1" class="auswahl">
				<xsl:for-each select="document($navigationBase) /navigation//item[@href = $href]/dynamicContentBinding/rootTag">
					<option>
						<xsl:value-of select="."/>
					</option>
				</xsl:for-each>
				<xsl:text></xsl:text>
			</select>
			<br />
			<label for="dcbActionDelete">&#160;</label>
			<input type="checkbox" name="dcbActionDelete" value="dcbActionDelete" class="box"/>
			<span><xsl:text> Ausgewählte Zuordnung aufheben</xsl:text></span>
			<br />
		</xsl:if>
	</xsl:template>

<!-- ====================================================================================={
section: Template: name="buildInterface.general.content.multimedia"
	- 
}===================================================================================== -->
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
						<a target="blank" href="{$ServletsBaseURL}WCMSFileUploadServlet?action=select&amp;wcmsID=0024">Bild oder sonst. 
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

<!-- =================================================================================== -->
</xsl:stylesheet>
