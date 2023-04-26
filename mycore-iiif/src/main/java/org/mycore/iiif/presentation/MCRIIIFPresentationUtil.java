/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

package org.mycore.iiif.presentation;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.model.MCRIIIFBase;
import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotation;
import org.mycore.iiif.presentation.model.attributes.MCRDCMIType;
import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;
import org.mycore.iiif.presentation.model.basic.MCRIIIFRange;
import org.mycore.iiif.presentation.model.basic.MCRIIIFSequence;

public class MCRIIIFPresentationUtil {

    /**
     * The ids ({@link MCRIIIFBase#getId()}) are all local e.g. a canvas id is maybe abcdf.tiff then this methods
     *  replaces the id with a URI.
     * -&gt; http://my.repository.com/iiif/presentation/$impl/$identifier/canvas/abcdf.tiff
     * Works recursive
     * @param base where to start with replacing
     */
    public static void correctIDs(MCRIIIFBase base, String impl, String identifier) {
        switch (base.getType()) {
            case MCRIIIFCanvas.TYPE -> {
                base.setId(getImplBaseURL(impl, identifier) + "canvas/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFCanvas canvas) {
                    canvas.images.forEach(annotation -> correctIDs(annotation, impl, identifier));
                }
            }
            case MCRIIIFAnnotation.TYPE -> {
                base.setId(getImplBaseURL(impl, identifier) + "annotation/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFAnnotation iiifAnnotation) {
                    iiifAnnotation.refresh();
                    correctIDs(iiifAnnotation.getResource(), impl, identifier);
                }
            }
            case MCRIIIFSequence.TYPE -> {
                base.setId(getImplBaseURL(impl, identifier) + "sequence/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFSequence sequence) {
                    sequence.canvases.forEach(canvas -> correctIDs(canvas, impl, identifier));
                }
            }
            case MCRIIIFRange.TYPE -> {
                base.setId(getImplBaseURL(impl, identifier) + "range/" + encodeUTF8(base.getId()));
                if (base instanceof MCRIIIFRange range) {
                    range.canvases = range.canvases.stream()
                        .map(c -> getImplBaseURL(impl, identifier) + "canvas/" + encodeUTF8(c))
                        .collect(Collectors.toList());
                    range.ranges = range.ranges.stream()
                        .map(r -> getImplBaseURL(impl, identifier) + "range/" + encodeUTF8(r))
                        .collect(Collectors.toList());
                }
            }
            case MCRIIIFManifest.TYPE -> {
                base.setId(getImplBaseURL(impl, identifier) + "manifest");
                if (base instanceof MCRIIIFManifest manifest) {
                    manifest.sequences.forEach(seq -> correctIDs(seq, impl, identifier));
                    manifest.structures.forEach(seq -> correctIDs(seq, impl, identifier));
                }
            }
            default -> {
                if (base.getType().equals(MCRDCMIType.Image.toString())) {
                    return;
                }
                base.setId(getImplBaseURL(impl, identifier) + "res/" + base.getId());
            }
        }
    }

    private static String getImplBaseURL(String impl, String identifier) {
        String impAndSlash = Objects.equals(impl, "") ? "" : impl + "/";
        return MCRFrontendUtil.getBaseURL() + "api/iiif/presentation/v2/" + impAndSlash + identifier + "/";
    }

    private static String encodeUTF8(String c) {
        return URLEncoder.encode(c, StandardCharsets.UTF_8);
    }

}
