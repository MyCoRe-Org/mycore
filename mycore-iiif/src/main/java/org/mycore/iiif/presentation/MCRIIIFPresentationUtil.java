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
