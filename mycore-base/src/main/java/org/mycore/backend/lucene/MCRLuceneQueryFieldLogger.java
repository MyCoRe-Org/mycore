/*
 * $Id$
 * $Revision: 5697 $ $Date: 29.09.2010 $
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

package org.mycore.backend.lucene;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Thomas Scheffler (yagee)
 *
 */
public class MCRLuceneQueryFieldLogger {

    private final ReentrantLock newFieldLock;

    private final HashMap<String, AtomicLong> fieldUsage;

    public MCRLuceneQueryFieldLogger(Properties props) {
        newFieldLock = new ReentrantLock();
        fieldUsage = new HashMap<String, AtomicLong>();
        if (props != null) {
            for (Entry<Object, Object> entry : props.entrySet()) {
                String field = entry.getKey().toString().intern();
                long counter = Long.parseLong(entry.getValue().toString());
                AtomicLong value = new AtomicLong(counter);
                fieldUsage.put(field, value);
            }
        }
    }

    public void useField(String field) {
        AtomicLong value = fieldUsage.get(field);
        if (value != null) {
            value.incrementAndGet();
            return;
        }
        newFieldLock.lock();
        try {
            value = fieldUsage.get(field);
            if (value != null) {
                value.incrementAndGet();
                return;
            } else {
                value = new AtomicLong(1);
                fieldUsage.put(field, value);
            }
        } finally {
            newFieldLock.unlock();
        }
    }

    public Properties getFieldUsageAsProperties() {
        Properties props = new Properties();
        for (Entry<String, AtomicLong> entry : fieldUsage.entrySet()) {
            props.put(entry.getKey(), entry.getValue().toString());
        }
        return props;
    }

}
