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
package org.mycore.services.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashSet;

import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRUtils;
import org.mycore.datamodel.ifs.MCRFileContentTypeFactory;
import org.mycore.datamodel.ifs.MCRFileContentType;
import org.mycore.services.plugins.FilterPluginTransformException;
import org.mycore.services.plugins.TextFilterPlugin;

/**
 * Provide some info about your class!
 * 
 * @author Thomas Scheffler (yagee)
 */
public class PdfPlugin implements TextFilterPlugin {
	private static HashSet contentTypes = null;
	private static String name = "Yagee's amazing PDF Filter";
	private static final int MAJOR=0;
	private static final int MINOR=4;
	private static String info = null;
	private static String p2t_info = null;

	/**
	 * 
	 */
	public PdfPlugin() {
		super();
		if (contentTypes == null) {
			contentTypes = new HashSet();
			if (MCRFileContentTypeFactory.isTypeAvailable("pdf"))
				contentTypes.add(MCRFileContentTypeFactory.getType("pdf"));
		}
		if (p2t_info==null)
			pdftotext();
		if (info == null)
			info =
				new StringBuffer("This filter uses XPDF for transformation.")
					.append("\nSource code is available on http://www.foolabs.com/xpdf/")
					.append("\nCurrently using: ")
					.append(p2t_info)
					.toString();
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getInfo()
	 */
	public String getInfo() {
		return info;
	}
	
	private boolean pdftotext(){
		int rc;
		final String[] testcommand={"pdftotext","-v"};
		String s;
		StringBuffer infofetch=new StringBuffer();
		try {
			Process p=Runtime.getRuntime().exec(testcommand);
			BufferedReader stdError = new BufferedReader(new
			InputStreamReader(p.getErrorStream()));
			while ((s = stdError.readLine()) != null) {
				infofetch.append(s).append(", ");
			}
			rc = p.waitFor();
			p2t_info=infofetch.deleteCharAt(infofetch.length()-2).toString();		
		} catch (IOException e) {
			if (e.getMessage().indexOf("not found")>0)
				throw new MCRConfigurationException(testcommand[0]+" is not installed or in search path!",e);
			else
				throw new MCRConfigurationException("Error while excuting "+testcommand,e);
		} catch (InterruptedException e) {
			throw new MCRConfigurationException("Error while excuting "+testcommand,e);
		}
		return (rc==99);
	}

	private boolean pdftotext(File pdffile,File txtfile){
		int rc;
		final String[] testcommand={"pdftotext","-raw",pdffile.getAbsolutePath(),txtfile.getAbsolutePath()};
		String s;
		try {
			System.err.println(testcommand[0]+" "+testcommand[1]+" "+testcommand[2]+" "+testcommand[3]);
			Process p=Runtime.getRuntime().exec(testcommand);
			BufferedReader stdError = new BufferedReader(new
			InputStreamReader(p.getErrorStream()));
			while ((s = stdError.readLine()) != null) {
				System.err.println(s);
			}
			rc = p.waitFor();
		} catch (IOException e) {
			if (e.getMessage().indexOf("not found")>0)
				throw new MCRConfigurationException(testcommand[0]+" is not installed or in search path!",e);
			else
				throw new MCRConfigurationException("Error while excuting "+testcommand,e);
		} catch (InterruptedException e) {
			throw new MCRConfigurationException("Error while excuting "+testcommand,e);
		}
		return (rc==00);
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#getSupportedContentTypes()
	 */
	public HashSet getSupportedContentTypes() {
		return contentTypes;
	}

	/* (non-Javadoc)
	 * @see org.mycore.services.plugins.TextFilterPlugin#transform(org.mycore.datamodel.ifs.MCRFileContentType, org.mycore.datamodel.ifs.MCRContentInputStream, java.io.OutputStream)
	 */
//	public boolean transform(
//		MCRFileContentType ct,
//		InputStream input,
//		OutputStream output)
//		throws FilterPluginTransformException {
//		if (!getSupportedContentTypes().contains(ct))
//			throw new FilterPluginTransformException(
//				"ContentType "
//					+ ct
//					+ " is not supported by "
//					+ getName()
//					+ "!");
//		boolean success = true;
//		try {
//			stripText(input, output);
//		} catch (IOException e) {
//			success = false;
//			StringBuffer msg =
//				new StringBuffer("Error while transforming Inputstream: I/O Error");
//			throw new FilterPluginTransformException(msg.toString(), e);
//		} finally {
//			return success;
//		}
//	}
	
	public boolean transform(
		MCRFileContentType ct,
		InputStream input,
		OutputStream output)
		throws FilterPluginTransformException {
			if (!getSupportedContentTypes().contains(ct))
				throw new FilterPluginTransformException(
					"ContentType "
						+ ct
						+ " is not supported by "
						+ getName()
						+ "!");
			boolean success = false;
			try {
				System.err.println("===== PDF decoding starts ====");
				File pdffile=File.createTempFile("inp",".pdf");
				FileOutputStream fout=new FileOutputStream(pdffile);
				pdffile.deleteOnExit();
				MCRUtils.copyStream(input,fout);
				fout.flush(); fout.close();
				File txtfile=File.createTempFile("out",".txt");
				txtfile.deleteOnExit();
				success=pdftotext(pdffile,txtfile);
				pdffile.delete();
				FileInputStream fin=new FileInputStream(txtfile);
				System.err.println("[0]");
				if (MCRUtils.copyStream(fin,output)){
					System.err.println("[1]");
					txtfile.delete();
					System.err.println("[2]");
				}
			} catch (FileNotFoundException e) {
				throw new FilterPluginTransformException("File was not found!",e);
			} catch (IOException e) {
				throw new FilterPluginTransformException("General I/O Exception occured",e);
			}
		return success;
	}

	/**
	 * @see org.mycore.services.plugins.TextFilterPlugin#getMajorNumber()
	 */
	public int getMajorNumber() {
		return MAJOR;
	}

	/**
	 * @see org.mycore.services.plugins.TextFilterPlugin#getMinorNumber()
	 */
	public int getMinorNumber() {
		return MINOR;
	}

}
