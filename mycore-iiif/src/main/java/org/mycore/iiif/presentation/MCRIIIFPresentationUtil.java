package org.mycore.iiif.presentation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.stream.Collectors;

import org.mycore.common.MCRException;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.model.MCRIIIFBase;
import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotation;
import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotationBase;
import org.mycore.iiif.presentation.model.attributes.MCRDCMIType;
import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;
import org.mycore.iiif.presentation.model.basic.MCRIIIFRange;
import org.mycore.iiif.presentation.model.basic.MCRIIIFSequence;

public class MCRIIIFPresentationUtil {

    /**
     * The ids ({@link MCRIIIFBase#id}) are all local e.g. a canvas id is maybe abcdf.tiff then this methods replaces the id with a URI.
     * -&gt; http://my.repository.com/iiif/presentation/$impl/$identifier/canvas/abcdf.tiff
     * Works recursive
     * @param base where to start with replacing
     * @param impl
     * @param identifier
     */
    public static void correctIDs(MCRIIIFBase base, String impl, String identifier) {
        switch (base.getType()) {
            case MCRIIIFCanvas.TYPE:
                base.setId(getImplBaseURL(impl, identifier) + "canvas/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFCanvas) {
                    ((MCRIIIFCanvas) base).images.forEach(annotation -> correctIDs(annotation, impl, identifier));
                }
                break;
            case MCRIIIFAnnotation.TYPE:
                base.setId(getImplBaseURL(impl, identifier) + "annotation/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFAnnotationBase && base instanceof MCRIIIFAnnotation) {
                    ((MCRIIIFAnnotation) base).refresh();
                    correctIDs(((MCRIIIFAnnotation) base).getResource(), impl, identifier);
                }

                break;
            case MCRIIIFSequence.TYPE:
                base.setId(getImplBaseURL(impl, identifier) + "sequence/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFSequence) {
                    ((MCRIIIFSequence) base).canvases.forEach(canvas -> correctIDs(canvas, impl, identifier));
                }
                break;
            case MCRIIIFRange.TYPE:
                base.setId(getImplBaseURL(impl, identifier) + "range/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFRange) {
                    ((MCRIIIFRange) base).canvases = ((MCRIIIFRange) base).canvases.stream()
                        .map(c -> getImplBaseURL(impl, identifier) + "canvas/" + encodeUTF8(c))
                        .collect(Collectors.toList());
                    ((MCRIIIFRange) base).ranges = ((MCRIIIFRange) base).ranges.stream()
                        .map(r -> getImplBaseURL(impl, identifier) + "range/" + encodeUTF8(r))
                        .collect(Collectors.toList());
                }
                break;
            case MCRIIIFManifest.TYPE:
                base.setId(getImplBaseURL(impl, identifier) + "manifest");
                if (base instanceof MCRIIIFManifest) {
                    ((MCRIIIFManifest) base).sequences.forEach(seq -> correctIDs(seq, impl, identifier));
                    ((MCRIIIFManifest) base).structures.forEach(seq -> correctIDs(seq, impl, identifier));
                }
                break;
            default:
                if (base.getType().equals(MCRDCMIType.Image.toString())) {
                    return;
                }

                base.setId(getImplBaseURL(impl, identifier) + "res/" + base.getId());
        }
    }

    private static String getImplBaseURL(String impl, String identifier) {
        return MCRFrontendUtil.getBaseURL() + "rsc/iiif/presentation/" + impl + "/" + identifier + "/";
    }

    private static String encodeUTF8(String c) {
        try {
            return URLEncoder.encode(c, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new MCRException(e);
        }
    }

}
