/*
 * $Id$
 * $Revision: 5697 $ $Date: May 13, 2013 $
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

package org.mycore.solr.legacy;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mycore.common.MCRException;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRSetCondition;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.solr.MCRSolrUtils;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRConditionTransformer {

    public static String toSolrQueryString(@SuppressWarnings("rawtypes") MCRCondition condition, Set<String> usedFields) {
        return toSolrQueryString(condition, usedFields, false).toString();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static StringBuilder toSolrQueryString(MCRCondition condition, Set<String> usedFields, boolean subCondition) {
        if (condition instanceof MCRQueryCondition) {
            MCRQueryCondition qCond = (MCRQueryCondition) condition;
            return handleQueryCondition(qCond, usedFields);
        }
        if (condition instanceof MCRSetCondition) {
            MCRSetCondition<MCRCondition> setCond = (MCRSetCondition<MCRCondition>) condition;
            return handleSetCondition(setCond, usedFields, subCondition);
        }
        if (condition instanceof MCRNotCondition) {
            MCRNotCondition notCond = (MCRNotCondition) condition;
            return handleNotCondition(notCond, usedFields);
        }
        throw new MCRException("Cannot handle MCRCondition class: " + condition.getClass().getCanonicalName());
    }

    private static StringBuilder handleQueryCondition(MCRQueryCondition qCond, Set<String> usedFields) {
        String field = qCond.getFieldName();
        String value = qCond.getValue();
        String operator = qCond.getOperator();
        usedFields.add(field);
        switch (operator) {
            case "like":
            case "contains":
                return getTermQuery(field, value.trim());
            case "=":
            case "phrase":
                return getPhraseQuery(field, value);
            case "<":
                return getLTQuery(field, value);
            case "<=":
                return getLTEQuery(field, value);
            case ">":
                return getGTQuery(field, value);
            case ">=":
                return getGTEQuery(field, value);
        }
        throw new UnsupportedOperationException("Do not know how to handle operator: " + operator);
    }

    @SuppressWarnings("rawtypes")
    private static StringBuilder handleSetCondition(MCRSetCondition<MCRCondition> setCond, Set<String> usedFields,
        boolean subCondition) {
        boolean stripPlus;
        if (setCond instanceof MCROrCondition) {
            stripPlus = true;
        } else if (setCond instanceof MCRAndCondition) {
            stripPlus = false;
        } else {
            throw new UnsupportedOperationException("Do not know how to handle "
                + setCond.getClass().getCanonicalName() + " set operation.");
        }
        List<MCRCondition<MCRCondition>> children = setCond.getChildren();
        if (children.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (subCondition) {
            sb.append("+(");
        }
        Iterator<MCRCondition<MCRCondition>> iterator = children.iterator();
        StringBuilder subSb = toSolrQueryString(iterator.next(), usedFields, true);
        sb.append(stripPlus ? stripPlus(subSb) : subSb);
        while (iterator.hasNext()) {
            sb.append(" ");
            subSb = toSolrQueryString(iterator.next(), usedFields, true);
            sb.append(stripPlus ? stripPlus(subSb) : subSb);
        }
        if (subCondition) {
            sb.append(")");
        }
        return sb;
    }

    @SuppressWarnings("rawtypes")
    private static StringBuilder handleNotCondition(MCRNotCondition notCond, Set<String> usedFields) {
        MCRCondition child = notCond.getChild();
        StringBuilder sb = new StringBuilder();
        sb.append("-");
        StringBuilder solrQueryString = toSolrQueryString(child, usedFields, true);
        stripPlus(solrQueryString);
        if (solrQueryString == null || solrQueryString.length() == 0) {
            return null;
        }
        sb.append(solrQueryString);
        return sb;
    }

    private static StringBuilder getRangeQuery(String field, String lowerTerm, boolean includeLower, String upperTerm,
        boolean includeUpper) {
        StringBuilder sb = new StringBuilder();
        sb.append('+');
        sb.append(field);
        sb.append(":");
        sb.append(includeLower ? '[' : '{');
        sb.append(lowerTerm != null ? ("*".equals(lowerTerm) ? "\\*" : MCRSolrUtils.escapeSearchValue(lowerTerm)) : "*");
        sb.append(" TO ");
        sb.append(upperTerm != null ? ("*".equals(upperTerm) ? "\\*" : MCRSolrUtils.escapeSearchValue(upperTerm)) : "*");
        sb.append(includeUpper ? ']' : '}');
        return sb;
    }

    private static StringBuilder getLTQuery(String field, String value) {
        return getRangeQuery(field, null, true, value, false);
    }

    private static StringBuilder getLTEQuery(String field, String value) {
        return getRangeQuery(field, null, true, value, true);
    }

    private static StringBuilder getGTQuery(String field, String value) {
        return getRangeQuery(field, value, false, null, true);
    }

    private static StringBuilder getGTEQuery(String field, String value) {
        return getRangeQuery(field, value, true, null, true);
    }

    private static StringBuilder getTermQuery(String field, String value) {
        if (value.length() == 0)
            return null;
        StringBuilder sb = new StringBuilder();
        sb.append('+');
        sb.append(field);
        sb.append(":");
        String replaced = value.replaceAll("\\s+", " AND ");
        if (value.length() == replaced.length()) {
            sb.append(MCRSolrUtils.escapeSearchValue(value));
        } else {
            sb.append("(");
            sb.append(MCRSolrUtils.escapeSearchValue(replaced));
            sb.append(")");
        }
        return sb;
    }

    private static StringBuilder getPhraseQuery(String field, String value) {
        StringBuilder sb = new StringBuilder();
        sb.append('+');
        sb.append(field);
        sb.append(":");
        sb.append('"');
        sb.append(MCRSolrUtils.escapeSearchValue(value));
        sb.append('"');
        return sb;
    }

    private static StringBuilder stripPlus(StringBuilder sb) {
        if (sb == null || sb.length() == 0) {
            return sb;
        }
        if (sb.charAt(0) == '+') {
            sb.deleteCharAt(0);
        }
        return sb;
    }

}
