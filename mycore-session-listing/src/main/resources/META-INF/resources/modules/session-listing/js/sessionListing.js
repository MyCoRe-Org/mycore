var mycore = mycore || {};
mycore.session = mycore.session || {};

mycore.session.listing = {

  load: function(baseURL) {
    var container = $("#sessionListingContainer");
    $.get(baseURL + "rsc/session/list").done(function(sessions) {
      mycore.session.listing.sessions = sessions;
      mycore.session.listing.render();
    }).fail(function(err) {
      console.log(err);
      if(err.status == 401) {
        container.html("Insufficient permissions: You don't have the rights to access the session data.");
      } else {
        container.html("Loading error: " + err.statusText);
      }
    });
  },

  render: function() {
    var spinner = $("#sessionListingLoadingSpinner");
    spinner.addClass("hidden");
    var table = $("#sessionListingTable");
    table.removeClass("hidden");

    var dateOptions = {
        weekday: "long", year: "numeric", month: "short",
        day: "numeric", hour: "2-digit", minute: "2-digit"
    };
    var locale = "de-DE";

    // empty table
    $(".table-session-entry").remove();

    // print table
    for(var session of mycore.session.listing.sessions) {
      var tr = $("<tr id='" + session.id + "' class='table-session-entry'>" +
          "<td>" + session.login + "</td>" +
          "<td>" + (session.realName != null ? session.realName : "") + "</td>" +
          "<td>" + session.ip + "</td>" +
          "<td>" + new Date(session.createTime).toLocaleDateString(locale, dateOptions) + "</td>" +
          "<td>" + new Date(session.lastAccessTime).toLocaleDateString(locale, dateOptions)  + "</td>" +
          "<td>" + new Date(session.loginTime).toLocaleDateString(locale, dateOptions)  + "</td>" +
          "<td align='center'>" +
            "<div style='height: 15px; width: 15px; background-color: " + session.constructingStacktrace.color + "; border: 1px solid #999; cursor: pointer;'" +
            		  " onclick='mycore.session.listing.showStacktrace(\"" + session.id + "\");'" +
            		  " data-toggle='modal' data-target='#stacktraceModal'></div>" +
          "</td>" +
      		"</tr>");
      table.append(tr);
    }

  },

  showStacktrace: function(id) {
    var dialog = $("#stacktraceModalBody");
    dialog.empty();
    var session = mycore.session.listing.getSession(id);
    var stacktrace = session.constructingStacktrace.stacktrace;
    var content = "";
    for(var line of stacktrace) {
      var div = "<div>";
      div += line.class + "." + line.method + " (" + line.file + ":" + line.line + ")";
      div += "</div>";
      content += div;
    }
    dialog.html(content);
  },

  getSession: function(id) {
    for(var session of mycore.session.listing.sessions) {
      if(session.id == id) {
        return session;
      }
    }
    return null;
  },
  
  sortByLogin: function() {
    mycore.session.listing.sessions.sort(function(s1, s2) {
      return s1.login.localeCompare(s2.login);
    });
    mycore.session.listing.render();
  },

  sortByName: function() {
    mycore.session.listing.sessions.sort(function(s1, s2) {
      if(s1.realName == null || s2.realName == null) {
        return 0;
      }
      return s1.realName.localeCompare(s2.realName);
    });
    mycore.session.listing.render();
  },

  sortByIP: function() {
    mycore.session.listing.sessions.sort(function(s1, s2) {
      return s1.ip.localeCompare(s2.ip);
    });
    mycore.session.listing.render();
  },

  sortByFirstAccess: function() {
    mycore.session.listing.sessions.sort(function(s1, s2) {
      return s1.createTime - s2.createTime;
    });
    mycore.session.listing.render();
  },
  
  sortByLastAccess: function() {
    mycore.session.listing.sessions.sort(function(s1, s2) {
      return s1.lastAccessTime - s2.lastAccessTime;
    });
    mycore.session.listing.render();
  },
  
  sortByLoginTime: function() {
    mycore.session.listing.sessions.sort(function(s1, s2) {
      return s1.loginTime - s2.loginTime;
    });
    mycore.session.listing.render();
  }

}