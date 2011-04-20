/*
 * 
 * $Revision: 1.4 $ $Date: 2009/03/04 11:49:12 $
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

import java.awt.Rectangle;
import java.util.ArrayList;

import javax.swing.JPanel;


/**
 * This class is part of the MetsMods module. It's a representation of a container for MCRMetsModsPictureItems.
 * 
 * @author Stefan Freitag (sasf)
 * 
 * @version $Revision: 1.4 $ $Date: 2009/03/04 11:49:12 $
 */

public class MCRMetsModsPictureContainer extends JPanel{

	private static final long serialVersionUID = 3355852105017958072L;

	ArrayList<MCRMetsModsPictureItem> itemlist = new ArrayList<MCRMetsModsPictureItem>();
		
	int itemheight = 20;
	int itemspace = 5;
	
	JPanel header = new JPanel();
	
	public MCRMetsModsPictureContainer()
	{
		init();
	}
	
	private void init()
	{
		this.setLayout(null);
	}
	
	public void addItem(MCRMetsModsPictureItem mcrmmpi)
	{
		itemlist.add(mcrmmpi);
		refresh(null);
	}

	public MCRMetsModsPictureItem searchItemByOrder(int order)
	{
		for(int i=0;i<itemlist.size();i++)
		{
			MCRMetsModsPictureItem item = itemlist.get(i);
			if(item.getOrderValue()==order) return item;
		}
		
		return null;
	}
	
	public void changeItem(MCRMetsModsPictureItem mcrmmpi, boolean direction)
	{
		if(direction)	//means once up (+1)
		{
			int index = mcrmmpi.getOrderValue();
			
			if(index==itemlist.size()) return;
			
			MCRMetsModsPictureItem old_item = searchItemByOrder(index);
			MCRMetsModsPictureItem new_item = searchItemByOrder(index+1);
			old_item.getMetsModsPicture().setOrder(index+1);
			new_item.getMetsModsPicture().setOrder(index);
									
			old_item.renew();
			new_item.renew();
			
			int lindex1 = itemlist.indexOf(old_item);
			int lindex2 = itemlist.indexOf(new_item);
			
			itemlist.set(lindex1, new_item);
			itemlist.set(lindex2, old_item);
			
		}
		else			//means once down (-1)
		{
			int index = mcrmmpi.getOrderValue();
			
			if(index==1) return;
			
			MCRMetsModsPictureItem old_item = searchItemByOrder(index);
			MCRMetsModsPictureItem new_item = searchItemByOrder(index-1);
			old_item.getMetsModsPicture().setOrder(index-1);
			new_item.getMetsModsPicture().setOrder(index);
									
			old_item.renew();
			new_item.renew();
			
			int lindex1 = itemlist.indexOf(old_item);
			int lindex2 = itemlist.indexOf(new_item);
			
			itemlist.set(lindex1, new_item);
			itemlist.set(lindex2, old_item);
		}
		Rectangle rect = this.getVisibleRect();
		refresh(rect);

	}
	
	public void changeOrderLabel(MCRMetsModsPictureItem mcrmmpi, String newlabel)
	{
		int index = mcrmmpi.getOrderValue();
		
		MCRMetsModsPictureItem item = searchItemByOrder(index);
		item.getMetsModsPicture().setOrderlabel(newlabel);
		
	}
	
	public void refresh(Rectangle pos)
	{			
		int y = 30;
		int x = 10;
		
		int w = 0;
		for(int i=0;i<itemlist.size();i++)
		{
			MCRMetsModsPictureItem item = itemlist.get(i);
			item.setContainer(this);
			
			if(item.getWidth()>w) w=item.getWidth();
			
			item.setLocation(x,y);
			y += (itemheight+itemspace);
			
			this.add(item);
		}
		
		this.setBounds(0,0,w,y);
		
		if(pos!=null)
		this.scrollRectToVisible(pos);
	}

}
