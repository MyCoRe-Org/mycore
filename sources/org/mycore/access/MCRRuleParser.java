/**
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 **/

package org.mycore.access;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import org.mycore.user.MCRUser;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Class for parsing Access Control Definitions.
 * 
 * @author   Matthias Kramm
 **/

class MCRRuleParser
{
    private static Pattern bracket = Pattern.compile("\\([^)(]*\\)");
    private static Pattern and = Pattern.compile("\\band\\b");
    private static Pattern or = Pattern.compile("\\bor\\b");
    private static Pattern not = Pattern.compile("\\s*\\bnot\\b\\s*");
    private static Pattern marker = Pattern.compile("@<([0-9]*)>@");
    private static DateFormat dateformat = new SimpleDateFormat("dd.MM.yyyy");

    private static String extendClauses(String s, List l)
    {
	while(true) {
	    Matcher m = marker.matcher(s);
	    if(m.find()) {
		String c = m.group();
		String clause = (String)l.get(Integer.parseInt(m.group(1)));
		s = s.replaceAll(c, clause);
	    } else {
		break;
	    }
	}
	return s;
    }

    private static MCRAccessCtrlDefinition parse(String s) throws MCRParseException
    {
	s = s.replaceAll("\t", " ")
	     .replaceAll("\n", " ")
	     .replaceAll("\r", " ");
	return parse(s, null);
    }

    private static MCRAccessCtrlDefinition parse(String s, List l) throws MCRParseException
    {
	if(l==null) {
	    l = new ArrayList();
	}
	s = s.trim();

	/* replace all bracket expressions with $n */
	while(true) {
	    /* remove outer brackets () */
	    while(s.charAt(0)=='(' && s.charAt(s.length()-1)==')' && 
	       s.substring(1, s.length()-1).indexOf('(')<0 &&
	       s.substring(1, s.length()-1).indexOf(')')<0) 
	    {
		s = s.substring(1, s.length()-1).trim();
	    }
	    Matcher m = bracket.matcher(s);
	    if(m.find()) {
		String clause = m.group();
		s = s.substring(0, m.start()) + "@<" + l.size() + ">@" + s.substring(m.end());
		l.add(extendClauses(clause, l));
	    } else {
		break;
	    }
	}

	/* handle OR */
	Matcher m = or.matcher(s);
	if(m.find()) {
	    if(m.start()<=0) throw new MCRParseException("left side of OR missing while parsing \""+s+"\"");
	    if(m.end()>=s.length()-1) throw new MCRParseException("right side of OR missing while parsing \""+s+"\"");
	    MCRAccessCtrlDefinition left = parse(extendClauses(s.substring(0, m.start()),l), l);
	    MCRAccessCtrlDefinition right = parse(extendClauses(s.substring(m.end()),l), l);
	    return new MCROrClause(left, right);
	}
	/* handle AND */
	m = and.matcher(s);
	if(m.find()) {
	    if(m.start()<=0) throw new MCRParseException("left side of AND missing while parsing \""+s+"\"");
	    if(m.end()>=s.length()-1) throw new MCRParseException("right side of AND missing while parsing \""+s+"\"");
	    MCRAccessCtrlDefinition left = parse(extendClauses(s.substring(0, m.start()),l), l);
	    MCRAccessCtrlDefinition right = parse(extendClauses(s.substring(m.end()),l), l);
	    return new MCRAndClause(left, right);
	}
	/* handle NOT  */
	s = s.trim();
	if(s.toLowerCase().startsWith("not ")) {
	    MCRAccessCtrlDefinition inverse = parse(extendClauses(s.substring(4), l), l);
	    return new MCRNotClause(inverse);
	}
	/* handle specific rules */
	if(s.equalsIgnoreCase("false"))
	    return new MCRFalseClause();
	if(s.equalsIgnoreCase("true"))
	    return new MCRTrueClause();
	if(s.startsWith("group "))
	    return new MCRGroupClause(s.substring(6).trim());
	if(s.startsWith("user "))
	    return new MCRUserClause(s.substring(5).trim());
	if(s.startsWith("ip ")) {
	    return new MCRIPClause(s.substring(3).trim());
	} if(s.startsWith("date ")) {
	    s = s.substring(5).trim();
	    if(s.startsWith(">=")) return new MCRDateAfterClause(parseDate(s.substring(2).trim(), false));
	    else if(s.startsWith("<=")) return new MCRDateBeforeClause(parseDate(s.substring(2).trim(), true));
	    else if(s.startsWith(">")) return new MCRDateAfterClause(parseDate(s.substring(1).trim(), true));
	    else if(s.startsWith("<")) return new MCRDateBeforeClause(parseDate(s.substring(1).trim(), false));
	}

	throw new MCRParseException("syntax error: "+extendClauses(s, l));
    }
    private static Date parseDate(String s, boolean dayafter) throws MCRParseException
    {
	try {
	    long time = dateformat.parse(s).getTime();
	    if(dayafter)
		time += 1000*60*60*24;
	    return new Date(time);
	} catch(java.text.ParseException e) {
	    throw new MCRParseException("unable to parse date "+s);
	}
    }

    public static void main(String args[]) throws Exception
    {
	//constants (true, false)
	MCRAccessCtrlDefinition access1 = parse("(true or false) and (false or true)");
	System.out.println(access1.toString());
	MCRAccessCtrlDefinition access2 = parse("((true or false) and (false or true))");
	System.out.println(access2.toString());
	//access only in the year 2005
	MCRAccessCtrlDefinition access3 = parse("((date >= 01.01.2005) and (date < 01.01.2006))");
	System.out.println(access3.toString());
	//access only on the 30.2.2005
	MCRAccessCtrlDefinition access35 = parse("((date >= 30.02.2005) and (date <= 30.02.2005))");
	System.out.println(access35.toString());
	// access for all users except two
	MCRAccessCtrlDefinition access4 = parse("not (user wichtel or (user waldmeister))");
	System.out.println(access4.toString());
	// access for three users
	MCRAccessCtrlDefinition access5 = parse("(user wichtel) or (user waldmeister) or user schlumpf");
	System.out.println(access5.toString());
	// two groups (user must be in both groups)
	MCRAccessCtrlDefinition access6 = parse("group admins and group interne");
	System.out.println(access6.toString());
	// "not admins" and "false" as group names
	MCRAccessCtrlDefinition access7 = parse("group not admins and group false");
	System.out.println(access7.toString());
    }
} 
