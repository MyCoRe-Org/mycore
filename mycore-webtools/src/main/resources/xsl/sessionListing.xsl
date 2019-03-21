<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:i18n="xalan://org.mycore.services.i18n.MCRTranslation"
                exclude-result-prefixes="i18n">

  <xsl:template match="sessionListing">
    <div id="sessionListingContainer" style="margin-top: 16px;">

      <div id="sessionListingLoadingSpinner">
        <h3 class="text-center">
          <i class="fa fa-circle-o-notch fa-spin"></i>
          <xsl:value-of select="i18n:translate('component.session-listing.loadingPleaseWait')"/>
        </h3>
      </div>

      <div id="sessionListingContent" class="hidden">
        <div class="card">
          <div class="card-header">
            <h3>
              <xsl:value-of select="i18n:translate('component.session-listing.options')"/>
            </h3>
          </div>
          <div class="card-body">
            <div class="form-group row">
              <label for="sessionListingFilter" class="col-sm-2 col-form-label">
                <xsl:value-of select="i18n:translate('component.session-listing.filter')"/>
              </label>
              <div class="col-sm-10">
                <input type="text" class="form-control" id="sessionListingFilter" placeholder="Filter"
                       onchange="mycore.session.listing.onFilterChange()"/>
              </div>
            </div>
            <div class="form-check">
              <input id="sessionListingResolveHostname" class="form-check-input" type="checkbox"
                     onchange="mycore.session.listing.onHostnameResolvingChange()"/>
              <label for="sessionListingFilter" class="form-check-label">
                <xsl:value-of select="i18n:translate('component.session-listing.resolveHostNames')"/>
              </label>
            </div>
          </div>
        </div>

        <table id="sessionListingTable" class="table">
          <tr>
            <th data-criteria="login" role="button" tabindex="0" onclick="mycore.session.listing.sortByLogin()">
              <xsl:value-of select="i18n:translate('component.session-listing.login')"/>
              <i class="fa fa-sort "></i>
            </th>
            <th data-criteria="ip" role="button" tabindex="0" onclick="mycore.session.listing.sortByIP()">
              <xsl:value-of select="i18n:translate('component.session-listing.ip')"/>
              <i class="fa fa-sort "></i>
            </th>
            <th data-criteria="createTime" role="button" tabindex="0"
                onclick="mycore.session.listing.sortByFirstAccess()">
              <xsl:value-of select="i18n:translate('component.session-listing.firstAccess')"/>
              <i class="fa fa-sort "></i>
            </th>
            <th data-criteria="lastAccess" role="button" tabindex="0"
                onclick="mycore.session.listing.sortByLastAccess()">
              <xsl:value-of select="i18n:translate('component.session-listing.lastAccess')"/>
              <i class="fa fa-sort "></i>
            </th>
            <th data-criteria="stackTrace" role="button" tabindex="0"
                onclick="mycore.session.listing.sortByStacktrace()">
              Stacktrace
              <i class="fa fa-sort "></i>
            </th>
            <th>
              <span id="i18n-kill-question" style="display: none;">
                <xsl:value-of select="i18n:translate('component.session-listing.killQuestion')"/>
              </span>
            </th>
          </tr>
        </table>
      </div>
    </div>

    <div class="modal fade" id="stacktraceModal" tabindex="-1" role="dialog">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title"></h4>
          </div>
          <div class="modal-body">
            <pre id="stacktraceModalBody" class="pre-scrollable"></pre>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-dismiss="modal">Schließen</button>
          </div>
        </div>
      </div>
    </div>

    <script src="{$WebApplicationBaseURL}modules/webtools/session/js/sessionListing.js"></script>
    <xsl:variable name="quot">
      <xsl:text>"</xsl:text>
    </xsl:variable>
    <script>
      $(document).ready(function() {
      <xsl:value-of select="concat('var baseURL = ',$quot,$WebApplicationBaseURL,$quot, ';')"/>
      mycore.session.listing.init(baseURL);
      mycore.session.listing.load(false);
      });
    </script>
  </xsl:template>

</xsl:stylesheet>
