/**
 * $RCSfile$
 * $Revision$ $Date$
 *
 * Copyright (C) 2000 University of Essen, Germany
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
 * along with this program, normally in the file documentation/license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/

package org.mycore.frontend.fileupload;

import javax.swing.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * This class represents the basic applet to start the GUI for
 * transfering files and directories
 *
 * @author Harald Richter
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUploadApplet extends JApplet implements ActionListener {
  
	JPanel exteriorPanel,bottomPanel;
	JButton button;
		
	String uploadId;
  File selectedFiles[] = null;

  protected JLabel       locationLabel;
  protected JTextField   locationField;
  protected JButton      locationButton;

  protected static JFileChooser locationChooser;
  
  static String targetURL;
  
	protected static AppletContext appletContext;
  protected static String        base;
  protected static String        servletsBase;     

  /** Let the browser return to a given URL */
  protected static void returnToURL(  )
  {
    try
    { 
      appletContext.showDocument( new URL( targetURL ) ); 
    }
    catch( MalformedURLException exc )
    { 
      System.out.println( "MALFORMED URL: " + targetURL ); }
    }
        
	public void init()
	{
    try 
    {        
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");    
    } catch (Exception e) { }
    
    uploadId     = getParameter( "uploadId"   );
    
    targetURL    = getParameter( "url"    );
    System.out.println("targetURL " + targetURL);
    servletsBase = getParameter( "ServletsBase"    );
    System.out.println("ServletsBase " + servletsBase);

    MCRUploadCommunicator.setPeerURL( servletsBase + "MCRUploadServlet" );
		
		appletContext = getAppletContext();
		
    exteriorPanel = new JPanel();
    bottomPanel   = new JPanel();

    locationButton = new JButton( "ausw\u00e4hlen..." );
    locationButton.addActionListener( new ActionListener()
    { public void actionPerformed( ActionEvent e ) {
      handleLocationButton();
    } } );
    
    bottomPanel.setLayout(new BorderLayout(5,5));
		bottomPanel.add(locationButton,BorderLayout.EAST);
    bottomPanel.setBackground( Color.white );
		
    exteriorPanel.setLayout(new BorderLayout(5,5));
    exteriorPanel.setBackground( Color.white );
    exteriorPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    JPanel pSouth = new JPanel();
    pSouth.setLayout( new FlowLayout( FlowLayout.RIGHT ) );
    pSouth.add( bottomPanel );
		
    exteriorPanel.add( bottomPanel,  BorderLayout.SOUTH  );
		
    setContentPane(exteriorPanel);

    locationLabel  = new JLabel( "Datei oder Verzeichnis:" );
    locationField  = new JTextField( 30 );
    locationField.addKeyListener(new KeyAdapter()
    { public void keyTyped(KeyEvent e) {
      handleLocationField( );
    } } );
//    button        = new JButton( "Dateien \u00fcbertragen..." );
    button        = new JButton( "\u00fcbertragen..." );
    button.setEnabled( false );

    button.addActionListener(this);
    
    JPanel panel = new JPanel();
    panel.add( locationLabel );
    panel.add( locationField );
    panel.add( button );
    panel.setBackground( Color.white );
    
    exteriorPanel.add( panel, BorderLayout.CENTER );
	}
	
		
	public void handleLocationField()
	{
    if ( locationField.getText().length() > 0 )
      button.setEnabled( true );
    else
      button.setEnabled( false );
  }
  
	public void actionPerformed(ActionEvent event)
	{
    try
    { 
      selectedFiles = new File[1];
      selectedFiles[0] = new File( locationField.getText() );
      doUpload( ); 
      returnToURL();
    }
    catch (Exception ex){ System.out.println( ex ); }
	}
  
  protected void doUpload( )
  {
    button.setEnabled( false );
    Thread th = new Thread()
    { public void run(  ) {
      try
      {
        MCRUploadProgressMonitor.getDialog().setMessage( "Upload mit ID " + uploadId + " wird gestartet..." );
        MCRUploadCommunicator comm =  new MCRUploadCommunicator(); 
        comm.sendDerivate( uploadId, selectedFiles);
        MCRUploadProgressMonitor.getDialog().setMessage( "Upload mit ID " + uploadId + " wurde beendet." );
      }
      catch( Exception ex ) 
      { MCRUploadProgressMonitor.getDialog().setMessage( handleException( ex ) ); }
      finally
      { MCRUploadProgressMonitor.getDialog().finish(); }
    } };
    th.start();
    
    MCRUploadProgressMonitor.getDialog().show();
  }
  
  public static String handleException( Exception ex )
  {
    StringBuffer message = new StringBuffer();
    if( ex instanceof IOException )
    {
      message.append( "Fehler in der Netzwerkkommunikation:\n" );
      message.append( "Meldung:\n  " ).append( ex.getMessage() );
    } 
    else if( ex instanceof MCRUploadException )
    {
      message.append( "Fehlermeldung des Servers:\n" );
      MCRUploadException sse = (MCRUploadException)ex;
      message.append( "Meldung:\n  " )
             .append( sse.getServerSideStackTrace() )
             .append( "\n" );
    }
    else
    {
      message.append( "Ein Fehler ist aufgetreten:\n" )
             .append( "Meldung:\n  " ).append( ex.getClass().getName() )
             .append( "\n" ).append( ex.getMessage() );
      ex.printStackTrace();
    }
    return message.toString();    
  }

  protected void handleLocationButton()
  {
    if( locationChooser == null ) 
    { 
      locationChooser = new JFileChooser();
      locationChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
      locationChooser.setMultiSelectionEnabled(true);

      File c = new File( "C:\\" );
      if( c.exists() ) locationChooser.setCurrentDirectory( c );
    }

    int  result = locationChooser.showDialog( this, "Datei(en) oder Verzeichnis(se) w\u00e4hlen" );
    
    if (result == JFileChooser.APPROVE_OPTION) 
    {
      selectedFiles = locationChooser.getSelectedFiles();
      for (int i=0, n=selectedFiles.length; i<n; i++) 
      {
        System.out.println("Selected: " 
                          + selectedFiles[i].getParent() 
                          + " --- " 
                          + selectedFiles[i].getName()
                          + " " 
                          + selectedFiles[i].getPath());
      }
      doUpload( ); 
      returnToURL();
    }

  }
}

