<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:mcri18n="http://www.mycore.de/xslt/i18n"
                version="3.0"
>

    <xsl:include href="functions/i18n.xsl" />

    <xsl:template match="properties-analyze">
        <site title="Properties">
            <div class="row">
                <div class="col-12">
                    <xsl:for-each select="component">
                        <xsl:call-template name="printComponent"/>
                    </xsl:for-each>
                </div>
            </div>
        </site>
    </xsl:template>

    <xsl:template name="printComponent">
        <div class="card">
            <xsl:variable name="safe_name" select="replace(@name, '\.', '_')"/>
            <div class="card-header" id="{$safe_name}">
                <h5 class="mb-0">
                    <xsl:value-of select="mcri18n:translate('component.properties-analyze.component')" /> -
                    <xsl:value-of select="@name"/> -
                    <button class="btn btn-link"
                            data-toggle="collapse"
                            data-target="#{$safe_name}-collapse-overwritten"
                            aria-expanded="true" aria-controls="{$safe_name}-collapse">
                        <xsl:value-of select="mcri18n:translate('component.properties-analyze.property.overwritten')" />
                    </button>
                    -
                    <button class="btn btn-link"
                            data-toggle="collapse"
                            data-target="#{$safe_name}-collapse-all"
                            aria-expanded="true" aria-controls="{$safe_name}-collapse">
                         <xsl:value-of select="mcri18n:translate('component.properties-analyze.property.others')" />
                    </button>
                </h5>
            </div>
            <div id="{$safe_name}-collapse-overwritten" class="collapse" aria-labelledby="{$safe_name}">
                <div class="card-body">
                    <div class="list-group">
                        <xsl:for-each select="property[@oldValue]">

                            <xsl:variable name="name" select="@name"/>
                            <xsl:variable name="savePropName" select="replace(@name, '\.', '_')"/>

                            <div>
                                <xsl:attribute name="class">
                                    list-group-item flex-column align-items-start
                                    <xsl:if test="@oldValue = @newValue">
                                        list-group-item-warning
                                    </xsl:if>
                                </xsl:attribute>
                                <div class="d-flex w-100 justify-content-between">
                                    <b>
                                        <button class="btn btn-link"
                                                data-toggle="modal"
                                                data-target="#{$safe_name}-{$savePropName}-modal"
                                                aria-expanded="true"
                                                aria-controls="{$safe_name}-{$savePropName}-modal">
                                            <xsl:value-of select="@name"/>
                                        </button>
                                    </b>
                                </div>

                                <div class="row">
                                    <div class="col-6">
                                        <xsl:value-of select="@oldValue"/>
                                    </div>
                                    <div class="col-6">
                                        <xsl:value-of select="@newValue"/>
                                    </div>
                                </div>

                                <div id="{$safe_name}-{$savePropName}-modal" class="modal fade" tabindex="-1"
                                     role="dialog" aria-labelledby="" aria-hidden="true">
                                    <div class="modal-dialog modal-lg modal-dialog-centered" role="document">
                                        <div class="modal-content">
                                            <div class="modal-header">
                                                <h5 class="modal-title" id="{$safe_name}-{$savePropName}-modal-title">
                                                    <xsl:value-of select="mcri18n:translate('component.properties-analyze.property.history')" />
                                                    <xsl:value-of select="@name"/>
                                                </h5>
                                                <button type="button" class="close" data-dismiss="modal"
                                                        aria-label="Close">
                                                    <span aria-hidden="true">&#xD7;</span>
                                                </button>
                                            </div>
                                            <div class="modal-body">
                                                <div class="list-group">
                                                    <xsl:if test="count(../preceding-sibling::component[count(property[@name = $name])&gt;0])&gt;0">
                                                        <xsl:for-each select="../preceding-sibling::component">
                                                            <xsl:if test="count(property[@name = $name])&gt;0">
                                                                <div class="list-group-item flex-column align-items-start">
                                                                    <div class="d-flex w-100 justify-content-between">
                                                                        <b>
                                                                            <xsl:value-of select="mcri18n:translate('component.properties-analyze.component')" /> -
                                                                            <xsl:value-of select="@name"/>
                                                                        </b>
                                                                    </div>
                                                                    <p class="mb-1">
                                                                        <xsl:for-each select="property[@name = $name]">
                                                                            <xsl:value-of select="@newValue"/>
                                                                        </xsl:for-each>
                                                                    </p>
                                                                </div>
                                                            </xsl:if>
                                                        </xsl:for-each>
                                                    </xsl:if>
                                                </div>
                                            </div>

                                            <div class="modal-footer">
                                                <button type="button" class="btn btn-secondary" data-dismiss="modal">
                                                    Schließen
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                            </div>
                        </xsl:for-each>
                    </div>
                </div>
            </div>
            <div id="{$safe_name}-collapse-all" class="collapse" aria-labelledby="{$safe_name}">
                <div class="card-body">
                    <div class="list-group">
                        <xsl:for-each select="property[not(@oldValue)]">
                            <div>
                                <xsl:attribute name="class">
                                    list-group-item flex-column align-items-start list-group-item-info
                                </xsl:attribute>
                                <div class="d-flex w-100 justify-content-between">
                                    <b>
                                        <xsl:value-of select="@name"/>
                                    </b>
                                </div>

                                <div class="row">
                                    <div class="col-6">
                                        <xsl:value-of select="@oldValue"/>
                                    </div>
                                    <div class="col-6">
                                        <xsl:value-of select="@newValue"/>
                                    </div>
                                </div>
                            </div>
                        </xsl:for-each>
                    </div>
                </div>
            </div>
        </div>
    </xsl:template>


</xsl:stylesheet>