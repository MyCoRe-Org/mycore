/*
 * 
 * $Revision: 1.7 $ $Date: 2009/04/03 08:41:02 $
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
package org.mycore.frontend.metsmods;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

 
/**
 * This class is part of the MetsMods module. It is a representation of a item containing
 * filename, order, button up, button down and a check box which shows you whether the file
 * is included in mets (the last point isn't implemented yet). There you can also find a
 * text box that is made for holding the "orderlabel" text of a picture in a mets file. 
 * 
 * @author Stefan Freitag (sasf)
 * 
 * @version $Revision: 1.7 $ $Date: 2009/04/03 08:41:02 $
 */

public class MCRMetsModsPictureItem extends JPanel{

	private static final long serialVersionUID = -3003017208325790629L;
	MCRMetsModsPicture mmp;
	JLabel filename = new JLabel();
	JLabel order = new JLabel();
	JButton button_up = new JButton();
	JButton button_down = new JButton();
	JCheckBox checkmets = new JCheckBox();
	JTextField orderlabel = new JTextField();
	Color colorroom, colorhighlight;
	MCRMetsModsPictureContainer mmpc;
		
	public MCRMetsModsPictureItem(MCRMetsModsPicture mmp)
	{
		this.mmp = mmp;
		init(200);
	}
	
	public MCRMetsModsPictureItem(MCRMetsModsPicture mmp, int l)
	{
		this.mmp = mmp;
		init(l);
	}

	public void changeHighlight(Color c)
	{
		this.colorhighlight = c;
	}
	
	public int getOrderValue()
	{
		return mmp.getOrder();
	}
	
	public String getPictureName()
	{
		return mmp.getPicture();
	}
	
	public String getOrderLabelValue()
	{
		if(orderlabel.getText()!=null)
			return orderlabel.getText();
		
		return mmp.getOrderlabel();
	}
	
	public MCRMetsModsPicture getMetsModsPicture()
	{
		return mmp;
	}
	
	public void setContainer(MCRMetsModsPictureContainer mmpc)
	{
		this.mmpc = mmpc;
	}
	
	/**
	 * Once up in the list
	 */
	public void setUp()
	{
		mmpc.changeItem(this, true);		
	}
	
	/**
	 * Once down in the list
	 */
	public void setDown()
	{
		mmpc.changeItem(this, false);		
	}
	
	public void changeOL()
	{
		mmpc.changeOrderLabel(this, orderlabel.getText());
	}
	
	public void renew()
	{
		order.setText(String.valueOf(mmp.getOrder()));
	}
	
	private void init(int l)
	{
		this.setLayout(null);
		checkmets.setEnabled(false);
		int max_width = l+310;
		
		this.setBounds(0, 0, max_width, 20);
				
		colorhighlight = Color.LIGHT_GRAY;
		
		URL url1 = this.getClass().getResource("/pmud-up.png");
		URL url2 = this.getClass().getResource("/pmud-down.png");
		
		button_up = new JButton(new ImageIcon(url1));
		button_down = new JButton(new ImageIcon(url2));
				
		filename.setText(mmp.getPicture());
		order.setText(String.valueOf(mmp.getOrder()));
		
		filename.setBounds(0,0,l,20);
		order.setBounds(l,0,40,20);
		button_up.setBounds(l+40,0,20,20);
		button_down.setBounds(l+65,0,20,20);
		checkmets.setBounds(l+90,0,20,20);
		orderlabel.setBounds(l+120,0,180,20);
		orderlabel.setText(mmp.getOrderlabel());
		
		checkmets.addMouseListener(new MouseAdapter()
		{
			public void mouseEntered(MouseEvent me)
			{
				colorroom = getBackground();
				setBackground(colorhighlight);
			}
			
			public void mouseExited(MouseEvent me)
			{
				setBackground(colorroom);	
			}
		});
		
		button_up.addMouseListener(new MouseAdapter()
		{
			public void mouseEntered(MouseEvent me)
			{
				colorroom = getBackground();
				setBackground(colorhighlight);
			}
			
			public void mouseExited(MouseEvent me)
			{
				setBackground(colorroom);	
			}
		});
		
		button_down.addMouseListener(new MouseAdapter()
		{
			public void mouseEntered(MouseEvent me)
			{
				colorroom = getBackground();
				setBackground(colorhighlight);
			}
			
			public void mouseExited(MouseEvent me)
			{
				setBackground(colorroom);	
			}
		});
		
		button_up.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				setDown();
			}
		});
		
		button_down.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				setUp();
			}
		});
		
		/*orderlabel.getDocument().addDocumentListener(new DocumentListener(){


			public void changedUpdate(DocumentEvent arg0) {
				
				new javax.swing.JOptionPane().showMessageDialog(new javax.swing.JFrame(), "change");
			}


			public void insertUpdate(DocumentEvent arg0) {

				new javax.swing.JOptionPane().showMessageDialog(new javax.swing.JFrame(), "insert");
			}


			public void removeUpdate(DocumentEvent arg0) {

				new javax.swing.JOptionPane().showMessageDialog(new javax.swing.JFrame(), "remove");
			}
			
		});*/
		
		orderlabel.addFocusListener(new FocusListener(){

			public void focusGained(FocusEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			public void focusLost(FocusEvent arg0) {
				// TODO Auto-generated method stub
				changeOL();
			}
			
			
		});
		
		
		orderlabel.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				//new javax.swing.JOptionPane().showMessageDialog(new javax.swing.JFrame(), "now");
				changeOL();
			}
			
		});
		
		this.add(filename);
		this.add(order);
		this.add(button_up);
		this.add(button_down);
		this.add(checkmets);
		this.add(orderlabel);
	}
}
