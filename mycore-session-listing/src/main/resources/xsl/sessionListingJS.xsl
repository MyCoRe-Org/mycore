<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="sessionListingJS">
    <div id="sessionListingContainer">
      <div id="sessionListingLoadingSpinner">
        <h3 class="text-center">
          <i class="fa fa-circle-o-notch fa-spin"></i>
          Loading session data. Please wait...
        </h3>
      </div>

      <table id="sessionListingTable" class="table hidden">
        <tr>
          <th>
            Login
            <a href="javascript:mycore.session.listing.sortByLogin()">
              <i class="fa fa-sort "></i>
            </a>
          </th>
          <th>
            Name
            <a href="javascript:mycore.session.listing.sortByName()">
              <i class="fa fa-sort "></i>
            </a>
          </th>
          <th>
            IP
            <a href="javascript:mycore.session.listing.sortByIP()">
              <i class="fa fa-sort "></i>
            </a>
          </th>
          <th>
            erster Zugriff
            <a href="javascript:mycore.session.listing.sortByFirstAccess()">
              <i class="fa fa-sort "></i>
            </a>
          </th>
          <th>
            letzter Zugriff
            <a href="javascript:mycore.session.listing.sortByLastAccess()">
              <i class="fa fa-sort "></i>
            </a>
          </th>
          <th>
            angemeldet seit
            <a href="javascript:mycore.session.listing.sortByLoginTime()">
              <i class="fa fa-sort "></i>
            </a>
          </th>
          <th>Stacktrace</th>
        </tr>
      </table>
    </div>

    <div class="modal fade" id="stacktraceModal" tabindex="-1" role="dialog">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <button type="button" class="close" data-dismiss="modal" aria-label="Close"></button>
            <h4 class="modal-title">Stacktrace</h4>
          </div>
          <div class="modal-body" id="stacktraceModalBody">
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-default" data-dismiss="modal">Schließen</button>
          </div>
        </div>
      </div>
    </div>

    <script src="{$WebApplicationBaseURL}modules/session-listing/js/sessionListing.js"></script>
    <script>
      $(document).ready(function() {
        var baseURL = "<xsl:value-of select='$WebApplicationBaseURL' />";
        mycore.session.listing.load(baseURL);
      });
    </script>
  </xsl:template>

</xsl:stylesheet>
