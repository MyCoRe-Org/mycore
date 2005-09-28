/*
 * $RCSfile$
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.frontend.indexbrowser;

import java.util.List;
import java.util.Vector;

import javax.servlet.RequestDispatcher;

import org.jdom.Document;
import org.jdom.Element;
import org.mycore.backend.sql.MCRSQLConnection;
import org.mycore.backend.sql.MCRSQLConnectionPool;
import org.mycore.common.MCRCache;
import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

/**
 * @author Frank Lützenkirchen
 * 
 * TODO: Refactoring Java, XML, XSL TODO: case insensitiv machen TODO:
 * Sortierung fixen
 */
public class MCRIndexServlet extends MCRServlet {
    protected void doGetPost(MCRServletJob job) throws Exception {
        MCRBrowseRequest br = new MCRBrowseRequest(job.getRequest());
        MCRIndexConfiguration ic = getConfiguration(br.getIndex());
        String query = buildSqlQuery(br, ic);

        // Build output index page
        Element page = new Element("indexpage");
        page.setAttribute("path", br.getCanonicalRequestPath());

        Element eIndex = new Element("index");
        page.addContent(eIndex);
        eIndex.setAttribute("id", br.getIndex());

        // Execute SQL Query
        MCRSQLConnection mcrConn = MCRSQLConnectionPool.instance().getConnection();

        try {
            java.sql.Connection conn = mcrConn.getJDBCConnection();

            java.sql.Statement stmt = conn.createStatement(java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_READ_ONLY);
            java.sql.ResultSet rs = stmt.executeQuery(query);

            try {
                rs.last();

                int numRows = rs.getRow();
                rs.beforeFirst();

                Element results = new Element("results");
                page.addContent(results);
                results.setAttribute("numHits", String.valueOf(numRows));

                if (br.search != null) {
                    results.setAttribute("search", br.search);
                    results.setAttribute("mode", br.mode);
                }

                int from = Math.max(1, br.getFrom());
                int to = Math.min(numRows, br.getTo());

                int numSelectedRows = to - from + 1;

                if (numSelectedRows <= ic.maxPerPage) {
                    for (int i = from; i <= to; i++) {
                        rs.absolute(i);

                        Element v = new Element("value");
                        v.setAttribute("pos", String.valueOf(i));

                        Element ev = new Element("idx");
                        ev.addContent(rs.getString("idxvalue"));
                        v.addContent(ev);

                        for (int j = 0; j < ic.extraFields.length; j++) {
                            String value = rs.getString(ic.extraFields[j]);
                            Element col = new Element("col");
                            col.setAttribute("name", ic.extraFields[j]);
                            col.addContent(value);
                            v.addContent(col);
                        }

                        results.addContent(v);
                    }
                } else {
                    int stepSize = calculateStepSize(numSelectedRows, ic.maxPerPage);
                    List delims = buildDelimList(from, to, stepSize, rs);
                    buildPrefixDifference(delims);
                    buildXML(results, delims);
                }
            } finally {
                try {
                    rs.close();
                    stmt.close();
                } catch (Exception ignored) {
                }
            }
        } finally {
            mcrConn.release();
        }

        sendXML(job, page, ic.style);
    }

    // **************************************************************************
    private List buildDelimList(int from, int to, int steps, java.sql.ResultSet rs) throws Exception {
        List delims = new Vector();

        for (int i = from; i <= to; i++) {
            rs.absolute(i);
            delims.add(new MCRRangeDelim(i, rs.getString("idxvalue")));

            i = Math.min((i + steps) - 1, to);
            rs.absolute(i);

            String value = rs.getString("idxvalue");

            while ((i < to) && rs.next() && value.equals(rs.getString("idxvalue")))
                i++;

            if ((i < to) && ((to - i) < 3)) {
                i = to;
                rs.absolute(i);
                value = rs.getString("idxvalue");
            }

            delims.add(new MCRRangeDelim(i, value));
        }

        return delims;
    }

    // **************************************************************************
    private void buildPrefixDifference(List delims) {
        for (int i = 0; i < delims.size(); i++) {
            MCRRangeDelim curr = (MCRRangeDelim) (delims.get(i));
            MCRRangeDelim prev = (MCRRangeDelim) (delims.get(Math.max(0, i - 1)));
            MCRRangeDelim next = (MCRRangeDelim) (delims.get(Math.min(i + 1, delims.size() - 1)));

            String vCurr = curr.value;
            String vPrev = ((i > 0) ? prev.value : "");
            String vNext = ((i < (delims.size() - 1)) ? next.value : "");

            String a = buildPrefixDifference(vCurr, vPrev);
            String b = buildPrefixDifference(vCurr, vNext);
            curr.diff = ((a.length() > b.length()) ? a : b);
        }
    }

    // **************************************************************************
    private void buildXML(Element results, List delims) {
        for (int i = 0; i < delims.size(); i += 2) {
            MCRRangeDelim start = (MCRRangeDelim) (delims.get(i));
            MCRRangeDelim end = (MCRRangeDelim) (delims.get(i + 1));

            Element range = new Element("range");
            results.addContent(range);

            Element eFrom = new Element("from");
            eFrom.setAttribute("pos", String.valueOf(start.pos));
            eFrom.setAttribute("short", start.diff);
            eFrom.addContent(start.value);
            range.addContent(eFrom);

            Element eTo = new Element("to");
            eTo.setAttribute("pos", String.valueOf(end.pos));
            eTo.setAttribute("short", end.diff);
            eTo.addContent(end.value);
            range.addContent(eTo);
        }
    }

    // **************************************************************************
    private void sendXML(MCRServletJob job, Element root, String style) throws Exception {
        Document jdom = new Document(root);
        job.getRequest().setAttribute("MCRLayoutServlet.Input.JDOM", jdom);
        job.getRequest().setAttribute("XSL.Style", style);

        RequestDispatcher rd = getServletContext().getNamedDispatcher("MCRLayoutServlet");
        rd.forward(job.getRequest(), job.getResponse());
    }

    // **************************************************************************
    private MCRCache configurations = new MCRCache(20);

    private synchronized MCRIndexConfiguration getConfiguration(String ID) {
        MCRIndexConfiguration ic = (MCRIndexConfiguration) (configurations.get(ID));

        if (ic == null) {
            ic = new MCRIndexConfiguration(ID);
            configurations.put(ID, ic);
        }

        return ic;
    }

    // **************************************************************************
    private String buildPrefixDifference(String a, String b) {
        if (a.equals(b)) {
            return a;
        }

        StringBuffer pdiff = new StringBuffer();

        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            pdiff.append(a.charAt(i));

            if (a.charAt(i) != b.charAt(i)) {
                break;
            }
        }

        if ((a.length() > b.length()) && (b.equals(pdiff.toString()))) {
            pdiff.append(a.charAt(pdiff.length()));
        }

        return pdiff.toString();
    }

    // **************************************************************************
    private String buildSqlQuery(MCRBrowseRequest br, MCRIndexConfiguration ic) {
        StringBuffer sql = new StringBuffer("select ");

        if (ic.distinct) {
            sql.append("distinct ");
        }

        sql.append(ic.browseField).append(" as idxvalue");

        if ((ic.fields != null) && (ic.fields.trim().length() > 0)) {
            sql.append(", ").append(ic.fields.trim());
        }

        sql.append(" from ").append(ic.table);

        Vector conditions = new Vector();

        if (br.search != null) {
            if (br.mode.equals("contains")) {
                conditions.addElement(ic.browseField + " like '%" + br.search + "%'");
            } else if (br.mode.equals("prefix")) {
                conditions.addElement(ic.browseField + " like '" + br.search + "%'");
            }
        }

        if (ic.filter != null) {
            conditions.addElement(ic.filter);
        }

        for (int i = 0; i < conditions.size(); i++) {
            String cond = (String) (conditions.get(i));
            sql.append((i == 0) ? " where " : " and ");
            sql.append("(").append(cond).append(")");
        }

        sql.append(" order by idxvalue ").append(ic.order);

        return sql.toString();
    }

    // **************************************************************************
    private int calculateStepSize(int numSelectedRows, int maxPerPage) {
        for (int i = 1;; i++) {
            double dNum = (double) numSelectedRows;
            double dI = 1.0 / ((double) i);
            double root = Math.pow(dNum, dI);

            if (root <= maxPerPage) {
                return (int) (Math.floor(dNum / root));
            }
        }
    }
}
