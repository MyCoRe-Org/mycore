package org.mycore.iiif.model;


import com.google.gson.annotations.SerializedName;

/**
 * Base Class for most IIIF model classes
 */
public class MCRIIIFBase implements Cloneable{

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
