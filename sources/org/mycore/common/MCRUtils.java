/**
 * $RCSfile$
 * $Revision$ $Date$
 *
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
 *
 **/

package org.mycore.common;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.*;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

/**
 * This class represent a general set of external methods to support
 * the programming API.
 *
 * @author Jens Kupferschmidt
 * @author Frank L�tzenkirchen
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date$
 **/
public class MCRUtils {

	public final static char COMMAND_OR='O';
	public final static char COMMAND_AND='A';
	public final static char COMMAND_XOR='X';

	// public constant data
	private static final Logger logger = Logger.getLogger(MCRUtils.class);

	/**
	 * This method check the language string base on RFC 1766 to the supported
	 * languages in mycore.
	 *
	 * @param lang          the language string
	 * @return true if the language was supported, otherwise false
	 **/
	public static final boolean isSupportedLang(String lang) {
		if ((lang == null) || ((lang = lang.trim()).length() == 0)) {
			return false;
		}
		for (int i = 0; i < MCRDefaults.SUPPORTED_LANG.length; i++) {
			if (lang.equals(MCRDefaults.SUPPORTED_LANG[i]))
				return true;
		}
		return false;
	}

	/**
	 * The method return the index of a language string in the
	 * statich field MCRDefault.SUPPORTED_LANG. If the lang is not
	 * supported -1 was returned.
	 *
	 * @param lang          the language string
	 * @return the index if the language was supported, otherwise -1
	 **/
	public static final int getPositionLang(String lang) {
		if ((lang == null) || ((lang = lang.trim()).length() == 0)) {
			return -1;
		}
		for (int i = 0; i < MCRDefaults.SUPPORTED_LANG.length; i++) {
			if (lang.equals(MCRDefaults.SUPPORTED_LANG[i])) return i;
		}
		return -1;
         }

	/**
	 * The method return the instance of DateFormat for the given language.
	 *
	 * @param lang          the language string
	 * @return the instance of DateFormat or null
	 **/
	public static final DateFormat getDateFormat(String lang) {
                int i = getPositionLang(lang);
		if (i == -1) {
			return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
		}
		return MCRDefaults.DATE_FORMAT[i];
	}

	/**
	 * The method check a date string for the pattern <em>tt.mm.jjjj</em>.
	 *
	 * @param date          the date string
	 * @return true if the pattern is correct, otherwise false
	 **/
	public static final boolean isDateInDe(String date) {
		if ((date == null) || ((date = date.trim()).length() == 0)) {
			return false;
		}
		date = date.trim().toUpperCase();
		if (date.length() != 10) {
			return false;
		}
		try {
			DateFormat df = getDateFormat("de");
			GregorianCalendar newdate = new GregorianCalendar();
			newdate.setTime(df.parse(date));
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

	/**
	 * The method check a date string for the pattern <em>yyyy-dd-mm</em>.
	 *
	 * @param date          the date string
	 * @return true if the pattern is correct, otherwise false
	 **/
	public static final boolean isDateInEn_UK(String date) {
		if ((date == null) || ((date = date.trim()).length() == 0)) {
			return false;
		}
		date = date.trim().toUpperCase();
		if (date.length() != 10) {
			return false;
		}
		try {
			DateFormat df = getDateFormat("en-UK");
			GregorianCalendar newdate = new GregorianCalendar();
			newdate.setTime(df.parse(date));
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

	/**
	 * The method check a date string for the pattern <em>yyyy/dd/mm</em>.
	 *
	 * @param date          the date string
	 * @return true if the pattern is correct, otherwise false
	 **/
	public static final boolean isDateInEn_US(String date) {
		if ((date == null) || ((date = date.trim()).length() == 0)) {
			return false;
		}
		date = date.trim().toUpperCase();
		if (date.length() != 10) {
			return false;
		}
		try {
			DateFormat df = getDateFormat("en-US");
			GregorianCalendar newdate = new GregorianCalendar();
			newdate.setTime(df.parse(date));
		} catch (ParseException e) {
			return false;
		}
		return true;
	}

	/**
	 * The methode convert the input date string to the ISO output string.
	 * If the input can't convert, the output is null.
	 *
	 * @param indate the date input
	 * @return the ISO output or null
	 **/
	public static final String covertDateToISO(String indate) {
		if ((indate == null) || ((indate = indate.trim()).length() == 0)) {
			return null;
		}
		GregorianCalendar calendar = new GregorianCalendar();
		boolean test = false;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		formatter.setLenient(false);
		try {
			calendar.setTime(formatter.parse(indate));
			test = true;
		} catch (ParseException e) {
		}
		if (!test) {
			for (int i = 0; i < MCRDefaults.SUPPORTED_LANG.length; i++) {
				DateFormat df = getDateFormat(MCRDefaults.SUPPORTED_LANG[i]);
				df.setLenient(false);
				try {
					calendar.setTime(df.parse(indate));
					test = true;
				} catch (ParseException e) {
				}
				if (test) {
					break;
				}
			}
		}
		if (!test) {
			return null;
		}
		formatter.setCalendar(calendar);
		return formatter.format(calendar.getTime());
	}

	/**
	 * The methode convert the input date string to the GregorianCalendar.
	 * If the input can't convert, the output is null.
	 *
	 * @param indate the date input
	 * @return the GregorianCalendar or null
	 **/
	public static final GregorianCalendar covertDateToGregorianCalendar(String indate) {
		if ((indate == null) || ((indate = indate.trim()).length() == 0)) {
			return null;
		}
		boolean era = true;
		int start = 0;
		if (indate.substring(0, 2).equals("AD")) {
			era = true;
			start = 2;
		}
		if (indate.substring(0, 2).equals("BC")) {
			era = false;
			start = 2;
		}
		GregorianCalendar calendar = new GregorianCalendar();
		boolean test = false;
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try {
			calendar.setTime(
				formatter.parse(indate.substring(start, indate.length())));
			if (!era) {
				calendar.set(Calendar.ERA, GregorianCalendar.BC);
			}
			test = true;
		} catch (ParseException e) {
		}
		if (!test) {
			for (int i = 0; i < MCRDefaults.SUPPORTED_LANG.length; i++) {
				DateFormat df = getDateFormat(MCRDefaults.SUPPORTED_LANG[i]);
				try {
					calendar.setTime(
						df.parse(indate.substring(start, indate.length())));
					if (!era) {
						calendar.set(Calendar.ERA, GregorianCalendar.BC);
					}
					test = true;
				} catch (ParseException e) {
				}
				if (test) {
					break;
				}
			}
		}
		if (!test) {
			return null;
		}
		return calendar;
	}

	/**
	 * This methode replace any characters to XML entity references.<p>
	 * <ul>
	 * <li> &lt; to &amp;lt;
	 * <li> &gt; to &amp;gt;
	 * <li> &amp; to &amp;amp;
	 * <li> &quot; to &amp;quot;
	 * <li> &apos; to &amp;apos;
	 * </ul>
	 *
	 * @param in  a string
	 * @return the converted string.
	 **/
	public static final String stringToXML(String in) {
		if (in == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer(2048);
		for (int i = 0; i < in.length(); i++) {
			if (in.charAt(i) == '<') {
				sb.append("&lt;");
				continue;
			}
			if (in.charAt(i) == '>') {
				sb.append("&gt;");
				continue;
			}
			if (in.charAt(i) == '&') {
				sb.append("&amp;");
				continue;
			}
			if (in.charAt(i) == '\"') {
				sb.append("&quot;");
				continue;
			}
			if (in.charAt(i) == '\'') {
				sb.append("&apos;");
				continue;
			}
			sb.append(in.charAt(i));
		}
		return sb.toString();
	}

	/**
	 * transforms a JDOM-Document to a String
	 * 
	 * @param jdom document to transform
	 * @return String representation of XML-Document
	 */
	public static final String xmlToString(Document jdom) {
		XMLOutputter out = new XMLOutputter("  ", true);
		return out.outputString(jdom);
	}

	/**
	 * This method convert a JDOM tree to a byte array.
	 *
	 * @param jdom_in the JDOM tree
	 * @return a byte array of the JDOM tree
	 **/
	public static final byte[] getByteArray(org.jdom.Document jdom)
		throws MCRPersistenceException {
		MCRConfiguration conf = MCRConfiguration.instance();
		String mcr_encoding =
			conf.getString(
				"MCR.metadata_default_encoding",
				MCRDefaults.ENCODING);
		ByteArrayOutputStream outb = new ByteArrayOutputStream();
		try {
			org.jdom.output.XMLOutputter outp =
				new org.jdom.output.XMLOutputter();
			outp.setEncoding(mcr_encoding);
			outp.setNewlines(true);
			outp.output(jdom, outb);
		} catch (Exception e) {
			throw new MCRPersistenceException("Can't produce byte array.");
		}
		return outb.toByteArray();
	}
	
	/**
	 * converts an Array of Objects to an Array of Strings
	 * using the toString() method.
	 * @param objects Array of Objects to be converted
	 * @return Array of Strings representing Objects
	 */
	public static final String[] getStringArray(Object[] objects){
		String[] returns=new String[objects.length];
		for (int i=0;i<objects.length;i++)
			returns[i]=objects[i].toString();
		return returns;
	}

	/**
	 * Copies all content read from the given input stream to the
	 * given output stream. Note that this method will NOT close the streams
	 * when finished copying.
	 * 
	 * @param source the InputStream to read the bytes from
	 * @param target out the OutputStream to write the bytes to, may be null
	 * @return true if Inputstream copied successfully to OutputStream
	 */
	public static boolean copyStream(InputStream source, OutputStream target) {
		MCRArgumentChecker.ensureNotNull(source, "InputStream source");
		try {
			// R E A D / W R I T E by chunks
			int chunkSize = 63 * 1024;
			// code will work even when chunkSize = 0 or chunks = 0;
			// Even for small files, we allocate a big buffer, since we
			// don't know the size ahead of time.
			byte[] ba = new byte[chunkSize];
			// keep reading till hit eof
			while (true) {
				int bytesRead = readBlocking(source, ba, 0, chunkSize);
				if (logger.isDebugEnabled()) {
					logger.debug(
						MCRUtils.class.getName()
							+ ".copyStream(): "
							+ bytesRead
							+ "bytes read");
				}
				if (bytesRead > 0) {
					if (target != null)
						target.write(ba, 0 /* offset in ba */
						, bytesRead /* bytes to write */
						);
				} else {
					source.close(); //not needed anymore
					break; // hit eof
				}
			} // end while
			// C L O S E, done by caller if wanted.
		} catch (IOException e) {
			return false;
		}
		// all was ok
		return true;
	} // end copy


	/**
	 * merges to HashSets of MyCoreIDs after specific rules
	 * @see #COMMAND_OR
	 * @see #COMMAND_AND
	 * @see #COMMAND_XOR
	 * @param set1 1st HashSet to be merged
	 * @param set2 2nd HashSet to be merged
	 * @param operation available COMMAND_XYZ
	 * @return merged HashSet
	 */
	public static final HashSet mergeHashSets(
		HashSet set1,
		HashSet set2,
		char operation) {
		HashSet merged = new HashSet();
		Object id;
		switch (operation) {
			case COMMAND_OR :
				merged.addAll((Collection)set1);
				merged.addAll((Collection)set2);
				break;
			case COMMAND_AND :
				for (Iterator it = set1.iterator();
					it.hasNext();
					) {
					id = it.next();
					if (set2.contains(id))
						merged.add(id);
				}
				break;
			case COMMAND_XOR :
				for (Iterator it = set1.iterator();
					it.hasNext();
					) {
					id = it.next();
					if (!set2.contains(id))
						merged.add(id);
				}
			for (Iterator it = set2.iterator();
				it.hasNext();
					) {
					id = it.next();
					if (!set1.contains(id) && !merged.contains(id))
						merged.add(id);
				}
				break;
			default :
				throw new IllegalArgumentException(
					"operation not permited: " + operation);
		}
		return merged;
	}
	/**
	 * Reads exactly <code>len</code> bytes from the input stream
	 * into the byte array. This method reads repeatedly from the
	 * underlying stream until all the bytes are read.
	 * InputStream.read is often documented to block like this, but in actuality it
	 * does not always do so, and returns early with just a few bytes.
	 * readBlockiyng blocks until all the bytes are read,
	 * the end of the stream is detected,
	 * or an exception is thrown. You will always get as many bytes as you
	 * asked for unless you get an eof or other exception.
	 * Unlike readFully, you find out how many bytes you did get.
	 *
	 * @param      b     the buffer into which the data is read.
	 * @param      off   the start offset of the data.
	 * @param      len   the number of bytes to read.
	 * @return     number of bytes actually read.
	 * @exception  IOException   if an I/O error occurs.
	 *
	 */
	private static final int readBlocking(
		InputStream in,
		byte b[],
		int off,
		int len)
		throws IOException {
		int totalBytesRead = 0;

		while (totalBytesRead < len) {
			int bytesRead =
				in.read(b, off + totalBytesRead, len - totalBytesRead);
			if (bytesRead < 0) {
				break;
			}
			totalBytesRead += bytesRead;
		}
		return totalBytesRead;
	} // end readBlocking

	/**
	 * <p>
	 * Returns String in with newStr substituted for find String.
	 * @param in String to edit
	 * @param find string to match
	 * @param newStr string to substitude for find
	*/

	public static String replaceString(String in, String find, String newStr) {

		char[] working = in.toCharArray();
		StringBuffer sb = new StringBuffer();

		int startindex = in.indexOf(find);
		if (startindex < 0)
			return in;

		int currindex = 0;

		while (startindex > -1) {
			for (int i = currindex; i < startindex; i++) {
				sb.append(working[i]);
			} //for
			currindex = startindex;
			sb.append(newStr);
			currindex += find.length();
			startindex = in.indexOf(find, currindex);
		} //while

		for (int i = currindex; i < working.length; i++) {
			sb.append(working[i]);
		} //for

		return sb.toString();

	}

	/**
	 * replacement for sun.misc.Service.provider(Class,ClassLoader) which is only available on sun jdk
	 * 
	 * @param service	Interface of instance needs to implement
	 * @param loader	URLClassLoader of Plugin
	 * @return Iterator over instances of service
	 */
	public static final Iterator getProviders(
		Class service,
		ClassLoader loader) {
		//we use a hashtable for this to keep controll of duplicates
		Hashtable classMap = new Hashtable();
		String name = "META-INF/services/" + service.getName();
		Enumeration services;
		try {
			services =
				(loader == null)
					? ClassLoader.getSystemResources(name)
					: loader.getResources(name);
		} catch (IOException ioe) {
			logger.error("Service: cannot load " + name);
			return classMap.values().iterator();
		}
		//Put all class names matching Service in nameSet
		while (services.hasMoreElements()) {
			URL url = (URL) services.nextElement();
			System.out.println(url);
			InputStream input = null;
			BufferedReader reader = null;
			try {
				input = url.openStream();
				reader =
					new BufferedReader(new InputStreamReader(input, "utf-8"));
				Object classInstance = null;
				for (StringBuffer className =
					new StringBuffer().append(reader.readLine());
					(className.length() != 4
						&& className.toString().indexOf("null") == -1);
					className.delete(0, className.length()).append(
						reader.readLine())) {
					//System.out.println("processing String: "+className.toString());					               	
					//remove any comments
					int comPos = className.toString().indexOf("#");
					if (comPos != -1)
						className.delete(comPos, className.length());
					//trim String
					int st = 0;
					int sblen = className.length();
					int len = sblen - 1;
					while ((st < sblen) && className.charAt(st) <= ' ')
						st++;
					while ((st < len) && className.charAt(len) <= ' ')
						len--;
					className.delete(len + 1, sblen).delete(0, st);
					//end trim String	 
					//if space letter is included asume first word as class name
					int spacePos = className.toString().indexOf(" ");
					if (spacePos != -1)
						className =
							className.delete(spacePos, className.length());
					//trim String
					st = 0;
					sblen = className.length();
					len = sblen - 1;
					while ((st < sblen) && className.charAt(st) <= ' ')
						st++;
					while ((st < len) && className.charAt(len) <= ' ')
						len--;
					className.delete(len + 1, sblen).delete(0, st);
					//end trim String	 
					if (className.length() > 0) {
						//we should have a proper class name now
						try {
							classInstance =
								Class
									.forName(className.toString(), true, loader)
									.newInstance();
							if (service.isInstance(classInstance))
								classMap.put(
									className.toString(),
									classInstance);
							else {
								classInstance = null;
								logger.error(
									className.toString()
										+ " does not implement "
										+ service.getName()
										+ "! Class instance will not be used.");
							}
						} catch (ClassNotFoundException e) {
							logger.error(
								"Service: cannot find class: " + className);
						} catch (InstantiationException e) {
							logger.error(
								"Service: cannot instantiate: " + className);
						} catch (IllegalAccessException e) {
							logger.error(
								"Service: illegal access to: " + className);
						} catch (NoClassDefFoundError e) {
							logger.error(
								"Service: " + e + " for " + className);
						} catch (Exception e) {
							logger.error(
								"Service: exception for: "
									+ className
									+ " "
									+ e);
						}
					}
				}
			} catch (IOException ioe) {
				logger.error("Service: problem with: " + url);
			} finally {
				try {
					if (input != null)
						input.close();
					if (reader != null)
						reader.close();
				} catch (IOException ioe2) {
					logger.error("Service: problem with: " + url);
				}
			}
		}
		return classMap.values().iterator();
	}
}
