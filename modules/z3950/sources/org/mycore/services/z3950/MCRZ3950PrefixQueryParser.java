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

package org.mycore.services.z3950;

/**
 * Build MyCoRe query condition from Z39.50-query string in PQF (Prefix Query Format)
 * based on Code from jzkit2 (http://developer.k-int.com/jzkit2/)
 
 * @author Harald Richter
 * @version $Revision$ $Date$
 */

import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;
import java.util.Properties;

import org.hibernate.Transaction;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.services.fieldquery.MCRFieldDef;
import org.mycore.services.fieldquery.MCRQueryCondition;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.common.MCRException;
import org.mycore.common.MCRConfiguration;

public class MCRZ3950PrefixQueryParser
{

  private int token = 0;

  private MCRZ3950PrefixQueryLexer p;

  private String defaultAttrset = "bib-1";

  private static Properties default_conversion_rules = null;

  public static Properties getDefaultConversionRules()
  {
    if (default_conversion_rules == null)
    {
      default_conversion_rules = MCRConfiguration.instance().getProperties("MCR.z3950");
    }

    return default_conversion_rules;
  }

  public MCRZ3950PrefixQueryParser(Reader r)
  {
    p = new MCRZ3950PrefixQueryLexer(r);
  }

  public MCRCondition parse() throws MCRException
  {
    MCRAndCondition cAnd = new MCRAndCondition();
    MCRCondition condition = null;
    token = p.nextToken();

    if (token == MCRZ3950PrefixQueryLexer.ATTRSET)
    {
      // Consume the Attrset token
      token = p.nextToken();
      defaultAttrset = p.getString();
      System.err.println("Setting attrset " + defaultAttrset);

      // Consume the namespace value token
      token = p.nextToken();
      condition = visitPrefixQuery(defaultAttrset);
    } else
    {
      condition = visitPrefixQuery(null);
    }

    if (token != MCRZ3950PrefixQueryLexer.EOF)
      throw new MCRException("Unparsed text at end of PQF expression: " + p.getString());

    cAnd.addChild(condition);
    return cAnd;
  }

  private MCRCondition visitPrefixQuery(String currentAttrset) throws MCRException
  {
    MCRCondition qn = null;

    switch (token)
    {
    case MCRZ3950PrefixQueryLexer.AND:
      token = p.nextToken();
      MCRAndCondition andc = new MCRAndCondition();
      MCRCondition anda = visitPrefixQuery(currentAttrset);
      if (null != anda)
        andc.addChild(anda);
      anda = visitPrefixQuery(currentAttrset);
      if (null != anda)
        andc.addChild(anda);
      return andc;

    case MCRZ3950PrefixQueryLexer.OR:
      token = p.nextToken();
      MCROrCondition orc = new MCROrCondition();
      MCRCondition ora = visitPrefixQuery(currentAttrset);
      if (null != ora)
        orc.addChild(ora);
      ora = visitPrefixQuery(currentAttrset);
      if (null != ora)
        orc.addChild(ora);
      return orc;

    case MCRZ3950PrefixQueryLexer.NOT:
      token = p.nextToken();
      MCRAndCondition notc = new MCRAndCondition();
      MCRCondition nota = visitPrefixQuery(currentAttrset);
      if (null != nota)
        notc.addChild(nota);
      nota = visitPrefixQuery(currentAttrset);
      if (null != nota)
        notc.addChild(new MCRNotCondition(nota));
      return notc;

    case MCRZ3950PrefixQueryLexer.TERM:
      qn = visitQueryNode(currentAttrset);
      break;

    case MCRZ3950PrefixQueryLexer.ATTR:
      qn = visitQueryNode(currentAttrset);
      break;
    }

    return qn;
  }

  private MCRQueryCondition visitQueryNode(String currentAttrset) throws MCRException
  {

    Vector terms = new Vector();

    String use = null, relation = null, structure = null, truncation = null;
    while (token == MCRZ3950PrefixQueryLexer.ATTR)
    {
      int attrType = 0;
      Object attrVal = null;
      String localAttrset = "MCR.z3950";

      // Consume the @attr and see what's next
      token = p.nextToken();

      // See if there is an attrset, as in "@attr gils 1=2016"
      if (token == MCRZ3950PrefixQueryLexer.TERM)
      {
        // It must be an attribute set identifier, since attr types are always numeric
        localAttrset = p.getString();
        token = p.nextToken();
      }

      // Process the attribute as in @attr 1=4
      if (token == MCRZ3950PrefixQueryLexer.NUMBER)
      {
        attrType = p.getInt();
        // Consume the attribute token
        token = p.nextToken();
      } else
        throw new MCRException("Unexpected error processing RPN query, expected attribute type");

      // Ensure that there is an equals
      if (token == MCRZ3950PrefixQueryLexer.EQUALS)
      {
        // Consume it
        token = p.nextToken();
      } else
        throw new MCRException("Unexpected error processing RPN query, expected =");

      // Ensure there is a value
      if (token == MCRZ3950PrefixQueryLexer.NUMBER)
      {
        attrVal = java.math.BigInteger.valueOf(p.getInt());
        // Consume It
        token = p.nextToken();
      } else if (token == MCRZ3950PrefixQueryLexer.TERM)
      {
        // With the new attribute set architecture we will start to get string values... Deal here
        attrVal = p.getString();
        // Consume It
        token = p.nextToken();
      } else
        throw new MCRException(
            "Unexpected error processing RPN query, expected str or num attribute");

      // Use the config to figure out which one of
      // ACCESS_POINT_ATTR = "AccessPoint"; "Relation"; "Position"; "Structure"; "Truncation"; "Completeness";
      // the selected attr relates to

      String attrType_str = "" + attrType;

      String lookupStr = null;
      if (localAttrset != null)
        lookupStr = localAttrset + "." + attrType;
      else
        lookupStr = currentAttrset + "." + attrType;

      String internalAttrType = getDefaultConversionRules().getProperty(lookupStr);

      if (internalAttrType == null)
        throw new MCRException("Query attribute not found in properties: " + lookupStr);

      //      System.err.println("++attrType_str="+attrType_str + " attrValue="+attrVal);
      lookupStr = localAttrset + "." + attrType_str + "." + attrVal;
      String mcr = getDefaultConversionRules().getProperty(lookupStr);
      if (null == mcr)
        throw new MCRException("Query attribute not found in properties: " + lookupStr);

      switch (attrType)
      {
      case 1:
        use = mcr;
        break;
      case 2:
        relation = mcr;
        break;
      case 4:
        structure = mcr;
        break;
      case 5:
        truncation = mcr;
        break;
      }
    }

    // See if we have an element name
    if (token == MCRZ3950PrefixQueryLexer.ELEMENTNAME)
    {
      // Consume the element name token and move on to the actual element name
      token = p.nextToken();

      // Consume the actual element name
      token = p.nextToken();
    }

    // Process any terms following the attrs

    // System.err.println("Expecting terms . Next token type = "+token);
    while ((token == MCRZ3950PrefixQueryLexer.TERM) || (token == MCRZ3950PrefixQueryLexer.NUMBER))
    {

      // Handle the term
      if (token == MCRZ3950PrefixQueryLexer.TERM)
        terms.addElement(p.getString());
      else
        terms.addElement("" + p.getNumber());

      //      System.err.println("Processing Term(s)"+p.getString());

      token = p.nextToken();
    }

    String value = "";
    if (terms.size() > 0)
    {
      for (int i = 0; i < terms.size(); i++)
      {
        if (i > 0)
          value = value + " ";
        value = value + terms.get(i);
      }
    } else
      throw new MCRException("No Terms");

    String operator;
    
    MCRFieldDef fd = MCRFieldDef.getDef(use);
    if ("identifier".equals(fd.getDataType()))
      operator = "=";
    else 
      operator = "contains";
    
    if (null != relation)               // <, <=, =, >, <=
      operator = relation;
    if (null != structure)              // phrase, word/worlist
      operator = structure;
    if (null != truncation)
    {
      operator = "like";
      if ("1".equals(truncation) )      // right
        value = value + "*";
      else if ("2".equals(truncation) ) // left
        value = "*" + value;
      else if ("3".equals(truncation) ) // left and right
        value = "*" + value + "*";
    }
    
    return new MCRQueryCondition(fd, operator, value);
  }

  public static void main(String args[])
  {
    //OK    String pqfQuery = "@attrset bib-1 @and @attr 4=1 @attr 1=1 \"bob dylan\" @or @attr 1=4 or1 @attr 1=4 or2 xxx";
    String pqfQuery = "@attrset bib-1 @not  @attr 4=1 @attr 1=1 \"bob dßylanä\" @attr 1=4 äöüWert3 ÄÖÜ?*";
    //OK    String pqfQuery = "@attrset bib-1 @and @attr 1=4 Wert1 @not @attr 1=4 Wert2    @attr 1=4 Wert3";
    //OK    String pqfQuery = "@attrset bib-1 @or @attr 1=4 Wert1 @not @attr 1=4 Wert2    @attr 1=4 Wert3";
    //OK    String pqfQuery = "@attrset bib-1 @or @attr 1=4 Wert1 @not @attr 1=4 Wert2    @attr 1=4 Wert3a Wert3b";
    //OK    String pqfQuery = "@attrset bib-1 @or @attr 1=1 Wert1 @not @attr 1=4 Wert2    @attr 1=4 @attr 4=1 Wert3a Wert3b";
    //OK   String pqfQuery = "@attrset bib-1 @and @attr 1=1 Wert1 @not @attr 1=4 Wert2    @attr 1=4 @attr 4=1 Wert3a Wert3b";
    //OK    String pqfQuery = "@attrset bib-1 @not @attr 1=1 Wert1 @and @attr 1=4 Wert2    @attr 1=4 @attr 4=1 Wert3a Wert3b";
    //    String pqfQuery = "@attrset bib-1 @not @attr 4=1 @attr 1=1 \"bob dylan\" @attr 1=4 not2 aaouAOU";
    if (1 == args.length)
      pqfQuery = args[0];

    Transaction tx = MCRHIBConnection.instance().getSession().beginTransaction();
    MCRZ3950PrefixQueryParser pqs = new MCRZ3950PrefixQueryParser(new StringReader(
        pqfQuery));
    try
    {
      MCRCondition result = pqs.parse();
      System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
      System.out.println("pqf query    : " + pqfQuery);
      System.out.println("mycore query : " + result.toString());
      tx.commit();
    } catch (Exception e)
    {
      tx.rollback();
      e.printStackTrace();
    }
  }
}
