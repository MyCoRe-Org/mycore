<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- =====================================================================================
========================================================================================={

title: login.xsl

Seite zum Anmelden an die Verwalterweboberflaeche des MyCoRe-WCMS

	- erzeugt das Formular zur Anmeldung

template:
	- wcms.login (name)

}=========================================================================================
====================================================================================== -->

<xsl:stylesheet 
	version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan" 
    xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation" >

	<xsl:include href="wcms_coreFunctions.xsl" />
	
<!-- ====================================================================================={
section: Template: wcmsLogin

	- erzeugt das Formular zur Anmeldung
	- wertet eventuelle Fehlermeldungen aus
}===================================================================================== -->

	<xsl:template name="wcmsLogin" >

		<!-- Menueleiste einblenden, Parameter = ausgewaehlter Menuepunkt -->
		<xsl:call-template name="menuleiste">
			<xsl:with-param name="menupunkt" select="'Anmelden'" />
		</xsl:call-template>

		<!-- Seitenname -->
		<xsl:call-template name="zeigeSeitenname">
			<xsl:with-param name="seitenname" select="'Anmelden'" />
		</xsl:call-template>
		<!-- Inhaltsbereich -->
		<div id="login">
			<div id="login-width">

				<!-- Hinweisfenster -->
				<!--
				<div id="hinweis_kopf">
					Hinweis
				</div>
				-->
				<xsl:if test="/cms/error !=''">
					<div id="login-hinweis">
						<table>
							<tr>
								<td>
									<script LANGUAGE="JAVASCRIPT">
										schreibeDatum();
									</script>
								</td>
								<td>
									<xsl:choose>
										<xsl:when test="/cms/error = 'denied'">
											<xsl:value-of select="i18n:translate('wcms.login.denied')" />
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="i18n:translate('wcms.login.denied.unknown')" />
										</xsl:otherwise>
									</xsl:choose>
								</td>
							</tr>
						</table>
					</div>
				</xsl:if>

				<!-- Loginfenster -->
				<!--
				<div id="aktion_kopf">
					Aktion
				</div>
				-->
				<div id="login-maske">
					<form name="login" action="{$ServletsBaseURL}WCMSLoginServlet" method="post">
						<fieldset>
							<!-- legend style="display:inline;">Anmelden zu statischen Seiten</legend>
							<p>Um statische Seiten anzulegen und zu pflegen, melden Sie sich bitte hier an.</p-->
							<label for="wcms-usr">Benutzername</label>
							<input id="wcms-usr" name="userID" type="text" />
							<br />
							<label for="wcms-pwd">Passwort</label>
							<input id="wcms-pwd" name="userPassword" type="password" />
							<a class="button" href="javascript:document.login.submit()">Anmelden</a>
							<br />
						</fieldset>
					</form>
				</div>

			</div>


			<!-- streckt div main so daß er seine Kinder umschließt -->
			<div class="clear">
				&#160;
			</div>

		</div>
<!-- /DIV main -->
	</xsl:template>
	<!-- END OF: WCMSLogin  ================================================================================= -->
</xsl:stylesheet>