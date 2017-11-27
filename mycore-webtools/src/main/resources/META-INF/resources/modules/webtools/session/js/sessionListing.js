/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

var mycore = mycore || {};
mycore.session = mycore.session || {};

mycore.session.listing = {

  sessions: [],
  filteredSessions: [],
  baseURL: null,
  resourceURL: null,

  sortCriteria: {
    criteria: null,
    asc: true
  },

  init: function(baseURL) {
    mycore.session.listing.baseURL = baseURL;
    mycore.session.listing.resourceURL = baseURL + "rsc/session/";
  },

  load: function(resolveHostname) {
    resolveHostname = resolveHostname == null ? false : resolveHostname;
    var container = $("#sessionListingContainer");
    $.get(mycore.session.listing.resourceURL + "list?resolveHostname=" + resolveHostname).done(function(sessions) {
      mycore.session.listing.sessions = sessions;
      mycore.session.listing.filteredSessions = sessions.slice();
      var sc = mycore.session.listing.sortCriteria;
      if (sc.criteria !== null) {
        sc.asc = !sc.asc;
        $("#sessionListingTable th[data-criteria='" + sc.criteria + "']").click();
      } else {
        mycore.session.listing.render();
      }
    }).fail(function(err) {
      console.log(err);
      if (err.status == 401) {
        container.html("Insufficient permissions: You don't have the rights to access the session data.");
      } else {
        container.html("Loading error: " + err.statusText);
      }
    });
  },

  kill: function(sessionId) {
    var question = $("#i18n-kill-question").text();
    var doKilling = confirm(question);
    if(!doKilling) {
      return;
    }
    $.post(mycore.session.listing.resourceURL + "kill/" + sessionId).done(function() {
      mycore.session.listing.sessions = mycore.session.listing.sessions.filter(function(session) {
        return session.id !== sessionId;
      });
      mycore.session.listing.filteredSessions = mycore.session.listing.filteredSessions.filter(function(session) {
        return session.id !== sessionId;
      });
      $("#" + sessionId).remove();
    }).fail(function(err) {
      console.log(err);
      alert("Unable to remove session " + sessionId);
    });
  },

  sortSessions: function(criteria, sortFunction) {
    var sc = mycore.session.listing.sortCriteria;
    if (criteria === sc.criteria) {
      sc.asc = !sc.asc;
    } else {
      sc.criteria = criteria;
      sc.asc = true;
    }
    mycore.session.listing.filteredSessions.sort(function(a, b) {
      return sortFunction(sc.asc ? a : b, sc.asc ? b : a);
    });
    mycore.session.listing.render();
  },

  render: function() {
    var spinner = $("#sessionListingLoadingSpinner");
    spinner.addClass("hidden");
    var contentDiv = $("#sessionListingContent");
    var table = $("#sessionListingTable");
    contentDiv.removeClass("hidden");

    var dateOptions = {
      year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit"
    };
    var locale = "de-DE";
    var sc = mycore.session.listing.sortCriteria;
    $("#sessionListingTable th i")
      .removeClass("fa-sort fa-sort-asc fa-sort-desc")
      .addClass(function() {
        if ($(this).parents("th").data("criteria") === sc.criteria) {
          return sc.asc ? "fa-sort-asc" : "fa-sort-desc";
        }
        return "fa-sort";
      });

    // empty table
    $(".table-session-entry").remove();

    // print table
    for (var session of mycore.session.listing.filteredSessions) {
      var tr = $("<tr id='" + session.id + "' class='table-session-entry'>" +
        "<td>" + session.login + (session.realName != null ? (" (" + session.realName + ")") : "") + "</td>" +
        "<td>" + session.ip + (session.hostname != null ? ("(" + session.hostname + ")") : "") + "</td>" +
        "<td>" + new Date(session.createTime).toLocaleDateString(locale, dateOptions) + "</td>" +
        "<td>" + new Date(session.lastAccessTime).toLocaleDateString(locale, dateOptions) + "</td>" +
        "<td align='center'>" +
        "<div style='height: 15px; width: 15px; background-color: " + session.constructingStacktrace.color + "; border: 1px solid #999; cursor: pointer;'" +
        " onclick='mycore.session.listing.showStacktrace(\"" + session.id + "\");'" +
        " data-toggle='modal' data-target='#stacktraceModal'></div>" +
        "</td>" +
        "<td><i href='javascript:void(0)' onclick='mycore.session.listing.kill(`" + session.id + "`)' class='fa fa-times' style='cursor: pointer;'></i></td>" +
        "</tr>"
      );
      table.append(tr);
    }
  },

  showStacktrace: function(id) {
    var dialog = $("#stacktraceModalBody");
    dialog.empty();
    var session = mycore.session.listing.getSession(id);
    var stacktrace = session.constructingStacktrace.stacktrace;
    var header = dialog.parents(".modal-dialog").find(".modal-header");
    if (session.firstURI != null) {
      header.find(".modal-title").html(session.firstURI);
      header.show();
    } else {
      header.hide();
    }
    var content = "";
    for (var line of stacktrace) {
      content += line.class + "." + line.method + " (" + line.file + ":" + line.line + ")\n";
    }
    dialog.html(content);
  },

  getSession: function(id) {
    for (var session of mycore.session.listing.sessions) {
      if (session.id == id) {
        return session;
      }
    }
    return null;
  },

  sortByLogin: function() {
    mycore.session.listing.sortSessions("login", function(a1, a2) {
      return a1.login.localeCompare(a2.login);
    });
  },

  sortByIP: function() {
    mycore.session.listing.sortSessions("ip", function(a1, a2) {
      return a1.ip.localeCompare(a2.ip);
    });
  },

  sortByFirstAccess: function() {
    mycore.session.listing.sortSessions("createTime", function(a1, a2) {
      return a1.createTime - a2.createTime;
    });
  },

  sortByLastAccess: function() {
    mycore.session.listing.sortSessions("lastAccess", function(a1, a2) {
      return a1.lastAccessTime - a2.lastAccessTime;
    });
  },

  sortByStacktrace: function() {
    mycore.session.listing.sortSessions("stackTrace", function(a1, a2) {
      return a1.constructingStacktrace.color.localeCompare(a2.constructingStacktrace.color);
    });
  },

  onFilterChange: function() {
    var filter = $("#sessionListingFilter").val().toLowerCase();
    mycore.session.listing.filteredSessions = mycore.session.listing.sessions.slice();
    if (filter == null || filter != "") {
      for (var session of mycore.session.listing.sessions) {
        if (session.id.toLowerCase().indexOf(filter) > -1 ||
          session.login.toLowerCase().indexOf(filter) > -1 ||
          (session.realName != null && session.realName.toLowerCase().indexOf(filter) > -1) ||
          session.ip.toLowerCase().indexOf(filter) > -1 ||
          (session.hostname != null && session.hostname.toLowerCase().indexOf(filter) > -1)) {
          continue;
        }
        var foundInStracktrace = false;
        for (var line of session.constructingStacktrace.stacktrace) {
          if (line.class.toLowerCase().indexOf(filter) > -1 ||
            line.file.toLowerCase().indexOf(filter) > -1 ||
            line.method.toLowerCase().indexOf(filter) > -1 ||
            line.line.toString().indexOf(filter) > -1) {
            foundInStracktrace = true;
            break;
          }
        }
        if (!foundInStracktrace) {
          var index = mycore.session.listing.filteredSessions.indexOf(session);
          mycore.session.listing.filteredSessions.splice(index, 1);
        }
      }
    }
    mycore.session.listing.render();
  },

  onHostnameResolvingChange: function() {
    var resolveHostname = $("#sessionListingResolveHostname").is(':checked');
    mycore.session.listing.load(resolveHostname);
  }

};
