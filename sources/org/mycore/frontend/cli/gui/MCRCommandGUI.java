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
package mycore.gui;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import mycore.common.*;
import mycore.commandline.*;
import mycore.datamodel.*;

/**
 * This class is a very simple GUI for handling MyCoRe objects. 
 *
 * You can query local and remote MyCoRe servers, view results as XML and
 * add, save, and remove objects from a local MyCoRe instance. Really
 * simple as you see.
 *
 * To be done: - showing results with stylesheets
 *
 * @author marc schluepmann
 * @version $Revision$ $Date$
 */
public class MCRCommandGUI extends JFrame {
    MCRConfiguration config;
    JPanel queryPanel = new JPanel( new FlowLayout() );
    JPanel resultPanel = new JPanel();
    JPanel detailPanel = new JPanel();
    JPanel funcPanel = new JPanel();
    JList resultList = new JList();
    JTextPane detailView = new JTextPane();
    JScrollPane resultScrollPane = new JScrollPane( resultList );
    JScrollPane detailScrollPane = new JScrollPane( detailView );
    JLabel resultListLabel = new JLabel( "available Objects:", JLabel.TRAILING );
    GridBagLayout resultPanelLayout = new GridBagLayout();
    JComboBox queryType = new JComboBox();
    JTextField queryField = new JTextField( 15 );
    JButton queryButton = new JButton( "Search" );
    JButton addButton = new JButton( "Add..." );
    JButton removeButton = new JButton( "Remove" );
    JButton saveButton = new JButton( "Save..." );
    JFileChooser fileChooser = new JFileChooser();
    JComboBox hostSelector = new JComboBox();
    JCheckBox xmlShow = new JCheckBox( "Show as XML" );

    String queryString;
    String queryItemString = "Document";
    String querySearchString = "";
    String[] queryHosts;
    String queryHostString = "local";
    //    Document resultDoc;
    String selDocId;
    MCRObjectIdentifier selObject;

    public MCRCommandGUI() {
	try {
	    config = MCRConfiguration.instance();
	    config.loadFromFile( "mycore.properties" );
	    String hosts = config.getString( "MCR.communication_hostaliases" );
	    String[] types = { "Document", "Legalentity" };
	    Vector queryHosts = new Vector(2);
	    queryHosts.add( "local" );
	    queryHosts.add( "remote" );

	    StringTokenizer stok = new StringTokenizer( hosts, "," );
	    while( stok.hasMoreTokens() )
		queryHosts.addElement( stok.nextToken() );

	    for( int j = 0; j < queryHosts.size(); j++ ) 
		hostSelector.addItem( (String)queryHosts.get( j ) );
	    
	    setQueryTypes( types );
	    buildGui();
	}
	catch(Exception e) {
	    e.printStackTrace();
	}
    }

    public void setQueryTypes( String[] types ) {
  	for( int i = 0; i<types.length; i++ )
  	    queryType.addItem( types[i] );
  	queryType.updateUI();
    }
    
    private void buildGui() throws Exception {
	this.setSize( 640, 480 );
	this.getContentPane().setLayout( new BorderLayout() );
	this.getContentPane().add( queryPanel, BorderLayout.NORTH );
	this.getContentPane().add( resultPanel,  BorderLayout.WEST );
	this.getContentPane().add( detailScrollPane, BorderLayout.CENTER );
	this.getContentPane().add( funcPanel, BorderLayout.SOUTH );

	resultPanel.setLayout( resultPanelLayout );
	GridBagConstraints c = new GridBagConstraints();

	c.fill = GridBagConstraints.BOTH;
	c.weightx = 1.0;
	c.weighty = 1.0;
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.gridheight = GridBagConstraints.RELATIVE;
	resultPanelLayout.setConstraints( resultScrollPane, c );
	resultPanel.add( resultScrollPane );

	c.fill = GridBagConstraints.CENTER;
	c.weightx = 0.0;
	c.weighty = 0.0;
	c.gridheight = GridBagConstraints.REMAINDER;
	resultPanelLayout.setConstraints( resultListLabel, c );
	resultPanel.add( resultListLabel );

	resultScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
	resultScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
	resultScrollPane.setMaximumSize( new Dimension( 250, 500 ) );
	resultScrollPane.setMinimumSize( new Dimension( 150, 87 ) );
	resultScrollPane.setPreferredSize( new Dimension( 200, 400 ) );

	detailScrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
	detailScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	
	queryPanel.add( hostSelector );
	queryPanel.add( queryType );
	queryPanel.add( queryField );
	queryPanel.add( queryButton );
	queryPanel.add( xmlShow );

  	queryType.addItemListener( new ItemListener() {
  		public void itemStateChanged( ItemEvent evt ) {
		    setResults( null );
		    detailView.setText( "" );
  		    queryItemString = ((String)evt.getItem()).toLowerCase();
  		}
  	    }
  				   );

  	hostSelector.addItemListener( new ItemListener() {
  		public void itemStateChanged( ItemEvent evt ) {
  		    queryHostString = (String)evt.getItem();
		    setResults( null );
		    detailView.setText( "" );
		    if( queryHostString.equalsIgnoreCase( "local" ) ) {
			addButton.setEnabled( true );
			saveButton.setEnabled( true );
			removeButton.setEnabled( true );
		    }
		    else {
			addButton.setEnabled( false );
			saveButton.setEnabled( false );
			removeButton.setEnabled( false );
		    }
  		}
  	    }
  				   );

	queryButton.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent evt ) {
		    try {
			MCRQueryResult result = new MCRQueryResult();
			MCRQueryResultArray resarray = result.setFromQuery( queryHostString, 
									    queryItemString, 
									    queryField.getText() 
									    );
			setResults( resarray );
			//System.out.println( domToString( resultDoc, null ) );
		    }
		    catch( Exception e ) {
			System.out.println( e.getMessage() );
		    }
		}
	    }
				       );

  	addButton.addActionListener( new ActionListener() {
  		public void actionPerformed( ActionEvent evt ) {
  		    fileChooser.rescanCurrentDirectory();
		    int returnVal = fileChooser.showOpenDialog( null );       
		    if( returnVal == JFileChooser.APPROVE_OPTION ) {
			File selFile = fileChooser.getSelectedFile();
			try {
			    if( selFile.isFile() )
				MCRObjectCommands.loadFromFile( selFile.getAbsolutePath() );
			    if( selFile.isDirectory() )
				MCRObjectCommands.loadFromDirectory( selFile.getAbsolutePath() );
  			}
  			catch( Exception e ) {
  			    System.out.println( e.getMessage() );
  			}
  		    }
  		}
  	    }
  				     );
   
  	saveButton.addActionListener( new ActionListener() {
  		public void actionPerformed( ActionEvent evt ) {
		    //		    fileChooser.setSelectedFile( new File( selDocId + ".xml" ) );
  		    int returnVal = fileChooser.showSaveDialog( null );
  		    if( returnVal == JFileChooser.APPROVE_OPTION ) {
  			try {
  			    FileOutputStream fos = new FileOutputStream( fileChooser.getSelectedFile() );
			    MCRObject obj = new MCRObject();
			    obj.receiveFromDatastore( selDocId );
  			    fos.write( obj.createXML() );
			    fos.close();
  			}
  			catch( Exception e ) {
  			    System.out.println( e.getMessage() );
  			}
  		    }
  		}
  	    }
				      );
	
  	removeButton.addActionListener( new ActionListener() {
  		public void actionPerformed( ActionEvent evt ) {
  		    try {
  			int answer = JOptionPane.showConfirmDialog( null, "Do you really want to delete the selected item from the content store?", "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
  			if( answer == JOptionPane.YES_OPTION ) { 
			    MCRObjectCommands.delete( selDocId );
  			}
  		    }
  		    catch( Exception e ) {
  			System.out.println( e.getMessage() );
  		    }
  		}
  	    }
  					);
   
  	resultList.addListSelectionListener( new ListSelectionListener() {
  		public void valueChanged( ListSelectionEvent evt ) {
  		    selObject = (MCRObjectIdentifier)resultList.getSelectedValue();
  		    if( selObject != null && queryHostString.equalsIgnoreCase( "local" ) ) {
  			showResult( selObject );
			addButton.setEnabled( true );
  			removeButton.setEnabled( true );
  			saveButton.setEnabled( true );
  		    }
		    else if( selObject != null && !queryHostString.equalsIgnoreCase( "local" ) ) {
  			showResult( selObject );
			addButton.setEnabled( false );
  			removeButton.setEnabled( false );
  			saveButton.setEnabled( false );
		    }
		    else {
  			detailView.setText( "" );
  			removeButton.setEnabled( false );
 		    }
		}
	    }
					     );
	resultList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
	xmlShow.setSelected( true );
	xmlShow.setVisible( false );
	hostSelector.setToolTipText( "Host to query at" );
	queryType.setToolTipText( "Object type to query for" );
	queryField.setToolTipText( "Query" );
	queryButton.setToolTipText( "Start query" );
	resultList.setToolTipText( "Query result" );
	detailView.setToolTipText( "Object content" );
	addButton.setToolTipText( "Adds a new object" );
	removeButton.setEnabled( false );
	removeButton.setToolTipText( "Removes selected object" );
	saveButton.setEnabled( false );
	saveButton.setToolTipText( "Saves selected object to local disk" );
	funcPanel.add( addButton );
	funcPanel.add( saveButton );
	funcPanel.add( removeButton );
	fileChooser.setFileFilter( new XMLFileFilter() );
	fileChooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
	detailView.setMaximumSize( new Dimension( 300, 400 ) );
	detailView.setMinimumSize( new Dimension( 300, 57 ) );
	detailView.setPreferredSize( new Dimension( 300, 400) );
	detailView.setEditable( false );
    }

    public void setResults( MCRQueryResultArray results ) {
	if( results != null ) {
	    resultList.setModel( new MCRQueryResultModel( results ) );
	    resultListLabel.setText( "available Objects: " 
				     + results.size() );
	    MCRObjectIdentifier[] objIDs = new MCRObjectIdentifier[results.size()]; 
	    for( int i = 0; i < results.size(); i++ ) {
		objIDs[i] = new MCRObjectIdentifier( queryItemString,
						  results.getId( i ), 
						  results.getHost( i ) 
						  );
	    }
	    resultList.setListData( objIDs );
	}
	else {
	    MCRObjectIdentifier[] objIDs = new MCRObjectIdentifier[0]; 
	    resultListLabel.setText( "available Objects:" );
	    resultList.setListData( objIDs );
	    
	}
	resultList.updateUI();
    }

    protected String byteToString( byte[] xml, String xsl ) 
	throws Exception {
	TransformerFactory tff = TransformerFactory.newInstance();
	Transformer trans;
	if( xsl == null ) {
	    trans = tff.newTransformer();
	}
	else {
	    FileInputStream in = new FileInputStream( xsl );
	    trans = tff.newTransformer( new StreamSource( in ) );
	    trans.setParameter( "WebApplicationBaseURL", "./" );
	}

	StreamSource src = new StreamSource( new ByteArrayInputStream( xml ) );
	ByteArrayOutputStream o = new ByteArrayOutputStream();
	StreamResult tar = new StreamResult( o );
	trans.transform( src, tar );
	return o.toString();
    }

    public String transformResult( byte[] xml ) {
	String result = null;
	try {
	    if( !xmlShow.isSelected() ) {
		detailView.setContentType( "text/html" );
		String xslfile = "stylesheets"
		    + "/mcr_results-ObjectMetadata-"
		    + queryItemString
		    + "-DE.xsl";
		result = byteToString( xml, xslfile );
	    }
	    else {
		detailView.setContentType( "text/xml" );
		result = byteToString( xml, null );
	    }
	}
	catch( Exception e ) {
	    System.out.println( e.getMessage() );
	}
	return result;
    }

    public void showResult( MCRObjectIdentifier objID ) {
	try {
	    MCRQueryResult result = new MCRQueryResult();
	    MCRQueryResultArray resarray = result.setFromQuery( objID.getHost(), 
								objID.getType(), 
								"/mycoreobject[@ID='" + objID.getId() + "']" 
								);
	    String id = resarray.getId( 0 );
	    detailView.setText( transformResult( resarray.exportElementToByteArray( 0 ) ) );
	    detailView.updateUI();
	}
	catch( Exception e ) {
	    System.out.println( e.getMessage() );
	}
    }	
    
    public Document parseXMLFile( String filename ) throws Exception {
	//	boolean validate = Boolean.getBoolean( config.getProperty( "validatexml", "false" ) );
	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	dbf.setValidating( false );
	DocumentBuilder db = dbf.newDocumentBuilder();
	return db.parse( new File( filename ) );
    }

    public static void main( String[] args ) {
	MCRCommandGUI gui = new MCRCommandGUI();
	gui.setVisible( true );
    }
}


