/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.iiif.presentation.model.attributes;

import java.util.Optional;

public class MCRIIIFMetadata {

    private String label;

    private Object value;

    public MCRIIIFMetadata(String label, String value) {
        this.label = label;
        this.value = value;
    }

    public MCRIIIFMetadata(String label, MCRIIIFMetadataValue value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Optional<MCRIIIFMetadataValue> getValue() {
        if (!(this.value instanceof MCRIIIFMetadataValue)) {
            return Optional.empty();
        }
        return Optional.of((MCRIIIFMetadataValue) this.value);
    }

    public Optional<String> getStringValue() {
        if (!(this.value instanceof String)) {
            return Optional.empty();
        }
        return Optional.of((String) this.value);
    }

}
