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

package org.mycore.access.mcrimpl;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A class for representing an IP Address, or a range of IP addresses
 * 
 * @author Matthias Kramm
 */
public class MCRIPAddress {
    byte[] address;

    byte[] mask;

    public MCRIPAddress(String ip) throws UnknownHostException {
        int i = ip.indexOf('/');

        if (i >= 0) {
            String ipstr = ip.substring(0, i);
            String maskstr = ip.substring(i + 1);
            InetAddress address = InetAddress.getByName(ipstr);
            InetAddress mask = InetAddress.getByName(maskstr);
            init(address, mask);
        } else {
            InetAddress address = InetAddress.getByName(ip);
            init(address);
        }
    }

    public MCRIPAddress(InetAddress address, InetAddress mask) {
        init(address, mask);
    }

    public MCRIPAddress(InetAddress address) {
        init(address);
    }

    public void init(InetAddress address, InetAddress mask) {
        this.address = address.getAddress();
        this.mask = mask.getAddress();
    }

    public void init(InetAddress address) {
        int t;
        this.address = address.getAddress();
        this.mask = new byte[this.address.length];

        for (t = 0; t < this.address.length; t++)
            this.mask[t] = (byte) 255;
    }

    boolean contains(MCRIPAddress other) {
        int t;

        if (this.address.length != other.address.length) {
            throw new IllegalStateException("can't map IPv6 to IPv4 and vice versa");
        }

        for (t = 0; t < address.length; t++) {
            if ((this.address[t] & this.mask[t]) != (other.address[t] & this.mask[t])) {
                return false;
            }
        }

        return true;
    }
    
    public String toString(){
        StringBuffer sb = new StringBuffer("");
        for (int i=0; i<address.length; i++){
        	if(i > 0) 
        		sb.append(".");
        	sb.append((address[i]&255));
        }
        sb.append("/");
        for (int i=0; i<mask.length; i++){
        	if(i > 0) 
        		sb.append(".");
        	sb.append((mask[i]&255));
        }        
        return sb.toString();
        
    }
}
