package org.mycore.iiif.presentation.model.attributes;

import org.mycore.iiif.image.model.MCRIIIFImageProfile;
import org.mycore.iiif.model.MCRIIIFBase;

/**
 * A link to a service that makes more functionality available for the resource, such as from an image to the base URI
 * of an associated IIIF Image API service. The service resource should have additional information associated with it
 * in order to allow the client to determine how to make appropriate use of it, such as a profile link to a service
 * description. It may also have relevant information copied from the service itself. This duplication is permitted in
 * order to increase the performance of rendering the object without necessitating additional HTTP requests.
 *
 * @see <a href="http://iiif.io/api/presentation/2.0/#linking-properties">IIIF-Documentation</a>
 */
public class MCRIIIFService extends MCRIIIFBase {

    /**
     * This can be a String or also @link {@link MCRIIIFImageProfile}
     **/
    public String profile;

    public MCRIIIFService(String id, String context) {
        super(id, null, context);
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

}
