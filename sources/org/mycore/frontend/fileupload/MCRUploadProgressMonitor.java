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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * This class implements a Dialog that shows messages and a progress bar while
 * creating, updating or deleting the derivates of a document. This class is
 * a singleton, there is only one instance at a time that you get with 
 * getDialog(). The MCRUploadProgressMonitor provides methods to set the next message that 
 * should be displayed and to start, update and finish the progress bar.
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @version $Revision$ $Date$
 */
public class MCRUploadProgressMonitor extends JDialog
{
  protected static MCRUploadProgressMonitor dialog;

  public static synchronized MCRUploadProgressMonitor getDialog()
  {
    if( dialog == null ) dialog = new MCRUploadProgressMonitor();
    return dialog;
  }

  protected JProgressBar progress;
  protected JTextArea    text;
  protected JButton      ok;

  protected MCRUploadProgressMonitor()
  {
    super( (Frame)null, "Nachrichten", true );
    try 
    {        
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");    
    } catch (Exception e) { }

    JPanel content = new JPanel();
    setContentPane( content );
    content.setLayout( new BorderLayout( 5, 5 ) );

    text = new JTextArea( "" );
    text.setEditable( false );
    text.setBackground( new JPanel().getBackground() );

    progress = new JProgressBar();
    progress.setStringPainted( true );

    ok = new JButton( "OK" );      
    ok.setEnabled( false );
    ok.addActionListener( new ActionListener()
    { public void actionPerformed( ActionEvent e )
      { 
        MCRUploadProgressMonitor.this.dispose(); 
        MCRUploadProgressMonitor.dialog = null;
      }
    } );

    JPanel buttons = new JPanel();
    buttons.setLayout( new FlowLayout( FlowLayout.RIGHT, 0, 0 ) );
    buttons.add( ok ); 

    JScrollPane scroller = new JScrollPane( text, 
      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
      JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );

    content.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    content.add( BorderLayout.NORTH,  progress );
    content.add( BorderLayout.CENTER, scroller );
    content.add( BorderLayout.SOUTH,  buttons  );

    setSize( 500, 250 );

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation( ( screen.width - 500 ) / 2, ( screen.height - 250 ) / 2 );
  }
 
  public synchronized void setMessage( final String message )
  {
    Runnable updater = new Runnable()
    {
      public void run()
      {
        MCRUploadProgressMonitor.this.show();
        MCRUploadProgressMonitor.this.requestFocus();
        text.append( message + "\n" );
        text.setCaretPosition( text.getText().length() );
        text.revalidate();
        repaint();
      }
    };
    SwingUtilities.invokeLater( updater );
  }
  
  public synchronized void startProgressBar( final int maximum )
  {
    Runnable updater = new Runnable()
    {
      public void run()
      {
        progress.setMaximum( maximum );
        progress.setValue( 0 );
      }
    };
    SwingUtilities.invokeLater( updater );
  }

  public synchronized void updateProgressBar( final int valueToAdd )
  {
    Runnable updater = new Runnable()
    {
      public void run()
      {
        progress.setValue( progress.getValue() + valueToAdd );
      }
    };
    SwingUtilities.invokeLater( updater );
  }

  public synchronized void finishProgressBar()
  {
    Runnable updater = new Runnable()
    {
      public void run()
      {
        progress.setValue( progress.getMaximum() );
      }
    };
    SwingUtilities.invokeLater( updater );
  }

  public synchronized void finish()
  { 
    finishProgressBar();
    Runnable updater = new Runnable()
    {
      public void run()
      { ok.setEnabled( true ); }
    };
    SwingUtilities.invokeLater( updater );
  }
}
