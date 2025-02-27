/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.access.mcrimpl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.mycore.common.MCRException;

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
        this.address = address.getAddress();
        mask = new byte[this.address.length];
        Arrays.fill(mask, (byte) 255);
    }

    public boolean contains(MCRIPAddress other) {
        boolean result = false;
        if (address.length == other.address.length) {
            result = IntStream.range(0, address.length)
                .noneMatch(t -> (address[t] & mask[t]) != (other.address[t] & mask[t]));
        }

        return result;
    }

    public byte[] getAddress() {
        return address.clone();
    }

    @Override
    public String toString() {
        try {
            InetAddress inetAddress = InetAddress.getByAddress(address);
            InetAddress maskAddress = InetAddress.getByAddress(mask);
            StringBuilder sb = new StringBuilder();
            sb.append(inetAddress.getHostAddress());
            sb.append('/');
            sb.append(maskAddress.getHostAddress());
            return sb.toString();
        } catch (UnknownHostException e) {
            throw new MCRException(e); //should never happen
        }

    }
}
