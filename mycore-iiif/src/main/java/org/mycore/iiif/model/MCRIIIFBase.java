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

package org.mycore.iiif.model;

import com.google.gson.annotations.SerializedName;

/**
 * Base Class for most IIIF model classes
 */
public class MCRIIIFBase implements Cloneable {

    public static final String API_PRESENTATION_2 = "http://iiif.io/api/presentation/2/context.json";

    public static final String API_IMAGE_2 = "http://iiif.io/api/image/2/context.json";

    @SerializedName("@context")
    private String context;

    @SerializedName("@type")
    private String type;

    @SerializedName("@id")
    private String id;

    public MCRIIIFBase(String id, String type, String context) {
        if (id != null) {
            this.id = id;
        }
        if (context != null) {
            this.context = context;
        }
        if (type != null) {
            this.type = type;
        }
    }

    public MCRIIIFBase(String type, String context) {
        this(null, type, context);
    }

    public MCRIIIFBase(String context) {
        this(null, context);
    }

    public MCRIIIFBase() {
        this(null);
    }

    public String getContext() {
        return context;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setId(String id) {
        this.id = id;
    }

}
