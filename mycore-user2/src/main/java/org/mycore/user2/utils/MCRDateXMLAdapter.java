/*
 * $Id$
 * $Revision: 5697 $ $Date: 13.03.2012 $
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

package org.mycore.user2.utils;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.mycore.datamodel.common.MCRISO8601Format;
import org.mycore.datamodel.common.MCRISO8601FormatChooser;

/**
 * @author Thomas Scheffler (yagee)
 */
public class MCRDateXMLAdapter extends XmlAdapter<String, Date> {

    @Override
    public Date unmarshal(String v) throws Exception {
        TemporalAccessor dateTime = MCRISO8601FormatChooser.getFormatter(v, null).parse(v);
        Instant instant = Instant.from(dateTime);
        return Date.from(instant);
    }

    @Override
    public String marshal(Date v) throws Exception {
        TemporalAccessor dt = v.toInstant();
        return MCRISO8601FormatChooser.getFormatter(null, MCRISO8601Format.COMPLETE_HH_MM_SS).format(dt);
    }

}
