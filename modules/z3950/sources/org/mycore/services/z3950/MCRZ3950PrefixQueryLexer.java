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
  Parse a prefix string
      based on "Little Language pattern (Patterns in Java, Mark Grand, 1998)
      and code from jzkit2 (http://developer.k-int.com/jzkit2/)
 * @author Harald Richter
 * @version $Revision$ $Date$
 */
 

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

public class MCRZ3950PrefixQueryLexer
{
  StreamTokenizer input = null;

  static final int ERROR=-1;
  static final int AND=1;
  static final int OR=2;
  static final int NOT=3;
  static final int ATTR=4;
  static final int ATTRSET=5;
  static final int TERM=6;
  static final int SPACE=7;
  static final int EQUALS=8;
  static final int EOL=9;
  static final int EOF=10;
  static final int NUMBER=11;
  static final int ELEMENTNAME=12;
  static final int PARAM=13;

  public MCRZ3950PrefixQueryLexer(Reader r)
  {
    input = new StreamTokenizer(r);
    input.resetSyntax();
    input.eolIsSignificant(true);
    input.parseNumbers();
    input.wordChars('\u0000', '\u00ff');
    input.ordinaryChar('=');
    input.quoteChar('"');
    input.whitespaceChars(' ',' ');
  }

  String getString()
  {
    return input.sval;
  }

  int getInt()
  {
    return (int)input.nval;
  }

  double getNumber()
  {
    return input.nval;
  }

  int nextToken()
  {
    int token=0;
    try
    {
      switch (input.nextToken())
      {
        case StreamTokenizer.TT_EOF:
          token=EOF;
          break;
        case StreamTokenizer.TT_EOL:
          token=EOL;
          break;
        case StreamTokenizer.TT_WORD:
          if ( input.sval.equalsIgnoreCase("@AND") )
            token=AND;
          else if ( input.sval.equalsIgnoreCase("@OR") )
            token = OR;
          else if ( input.sval.equalsIgnoreCase("@NOT") )
            token = NOT;
          else if ( input.sval.equalsIgnoreCase("@ATTR") )
            token = ATTR;
          else if ( input.sval.equalsIgnoreCase("@ATTRSET") )
            token = ATTRSET;
          else if ( input.sval.equalsIgnoreCase("@ELEMENTNAME") )
            token = ELEMENTNAME;
          else if ( input.sval.equalsIgnoreCase("@PARAM") )
            token = PARAM;
          else 
            token = TERM;
          break;
        case StreamTokenizer.TT_NUMBER:
          token=NUMBER;
          break;
        case '=':
          token = EQUALS;
          break;
        case '"':
            token = TERM;
          break;
      }
    }
    catch(IOException e)
    {
       token=EOF;
    }
    return token;
  }
}
