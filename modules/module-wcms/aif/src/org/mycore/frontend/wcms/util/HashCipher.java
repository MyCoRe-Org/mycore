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

package org.mycore.frontend.wcms.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCipher {
    /** Creates a new instance of HashCipher */
    public HashCipher() {
    }

    public static final void main(String[] args) throws ArrayIndexOutOfBoundsException, NoSuchAlgorithmException {
        try {
             System.out.println(crypt(args[0]));
        } catch (ArrayIndexOutOfBoundsException aioobe) {
             System.out.println("Usage: java wcms.util.HashCipher<\"PasswordString\">");
             System.out.println("Example: java wcms.util.HashCipher \"tESt&58y\"");
        }
    }

    public static String crypt(String string) throws NoSuchAlgorithmException {
        byte[] buffer = new byte[12];

        // DigestInputStream dis = new DigestInputStream(new InputStream.getInstance());
        
        buffer = string.getBytes();

        String outstr = new String(hash(buffer));

        return outstr;
    }

    private static String hash(byte[] buffer) throws NoSuchAlgorithmException {
        String s = "";
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(buffer);
        buffer = md.digest(buffer);

        for (int i = 0; i < buffer.length; i++) {
            s += Integer.toHexString(buffer[i] & 0xFF);

            // System.out.print( (s.length() == 1 ) ? "0"+s : s );
        }

        return s;
    }
}
