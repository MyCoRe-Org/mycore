<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- ============================================== -->
<!-- $Revision$ $Date$ -->
<!-- ============================================== --> 

<xsl:stylesheet 
  version="1.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  exclude-result-prefixes="xsl xalan"
>

<xsl:include href="webpage.xsl" />
<xsl:variable name="printable"  select="'no'" />
<xsl:include href="section.xsl"    />

<!-- ============ Seitentitel ============ -->
<xsl:variable name="page.title" select="Eingabefehler" /> 
<xsl:variable name="PageID" select="'admin'" />

<xsl:template match="user_action_messages"> 
    <xsl:choose>
      <xsl:when test="@section = 'pw_changed'">
        <div class="section">
          <xsl:value-of select="'Passwort geändert von: '" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" value="Zurück zum Formular" class="editorButton"/>
      </xsl:when>
      <xsl:when test="@section = 'user_created'">
        <div class="section">
          <xsl:value-of select="'Nutzer eingerichtet: '" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" value="Zurück zum Formular" class="editorButton"/>
      </xsl:when>
      <xsl:when test="@section = 'user_deleted'">
        <div class="section">
        <p>
          Nutzer gelöscht:
        </p>
          <xsl:for-each select="section">
            <xsl:value-of select="./@value" />
          </xsl:for-each>
        </div>
      </xsl:when>
      <xsl:when test="@section = 'action_not_found'">
        <div class="section">
          <xsl:value-of select="'Ungültiger Wert von action in user_action_message: '" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" value="Zurück zum Formular" class="editorButton"/>
      </xsl:when>
      <xsl:when test="@section = 'user_changed'">
        <div class="section">
          <xsl:value-of select="'Nutzerdaten geändert: '" />
          <xsl:value-of select="@value" />
        </div>
        <input type="button" onClick="javascript:history.back()" value="Zurück zum Formular" class="editorButton"/>
      </xsl:when>
      <xsl:otherwise>
        <div class="section">
          Bitte korrigieren Sie die folgenden Eingabefehler im Formular:
          <ul>
            <xsl:for-each select="*">
              <li>
                <xsl:apply-templates select="." /> 
              </li>
            </xsl:for-each>
          </ul>
          <input type="button" onClick="javascript:history.back()" value="Zurück zum Formular" class="editorButton"/>
        </div>
      </xsl:otherwise>
    </xsl:choose>    
</xsl:template>        

<!-- ======== Kopiere ======== -->

<xsl:template match="*">
  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:copy-of select="." />
    </xsl:for-each>
    <xsl:apply-templates select="node()" />
  </xsl:copy>
</xsl:template>

<xsl:template match="no_user_data">
  Bitte geben Sie die <b>Nutzerkennung</b> ein!
</xsl:template>

<xsl:template match="no_access">
  Ungültige Kombination von Nutzerkennung und Passwort!
</xsl:template>

<xsl:template match="no_pwold">
  Bitte geben Sie das aktuelle <b>Passwort</b> ein!
</xsl:template>

<xsl:template match="no_pwnew1">
  Bitte geben Sie das neue <b>Passwort</b> ein!
</xsl:template>
 
<xsl:template match="no_pwnew2">
  Bitte wiederholen Sie das neue <b>Passwort</b>!
</xsl:template>

<xsl:template match="no_pwnewident">
  Die neuen Passworte sind unterschiedlich!
</xsl:template>

<xsl:template match="not_allowed_changepw">
  Sie dürfen das Passwort nicht ändern!
</xsl:template>

<xsl:template match="not_allowed_createuser">
  Sie dürfen keine Nutzer anlegen!
</xsl:template>

<xsl:template match="user_exists">
  Nutzer bereits vorhanden!
</xsl:template>

<xsl:template match="no_le_exists">
  Legal Entity mit der angegebenen ID existiert nicht!
</xsl:template>

<xsl:template match="not_allowed_delete">
  Sie dürfen sich nicht selbst löschen!
</xsl:template>

<xsl:template match="not_allowed_changeuser">
  Sie dürfen die Nutzerdaten nicht ändern!
</xsl:template>

<xsl:template match="not_allowed_changegroup">
  Sie dürfen keine Gruppenzugehörigkeit ändern!
</xsl:template>

<xsl:template match="not_allowed_changeowngroup">
  Sie können Ihre eigene Gruppenzugehörigkeit nicht ändern!
</xsl:template>

<xsl:template match="no_user_selected">
  Kein Nutzer ausgewählt!
</xsl:template>

<!-- ============ Ende Stylesheet ============ -->

</xsl:stylesheet>
