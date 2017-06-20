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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mycore.iiif.presentation.model.additional.MCRIIIFAnnotationBase;
import org.mycore.iiif.presentation.model.basic.MCRIIIFCanvas;
import org.mycore.iiif.presentation.model.basic.MCRIIIFManifest;
import org.mycore.iiif.presentation.model.basic.MCRIIIFRange;
import org.mycore.iiif.presentation.model.basic.MCRIIIFSequence;

public class MCRIIIFPresentationManifestQuickAccess {

    private final MCRIIIFManifest manifest;

    private final Map<String, MCRIIIFSequence> idSequenceMap = new ConcurrentHashMap<>();

    private final Map<String, MCRIIIFCanvas> idCanvasMap = new ConcurrentHashMap<>();

    private final Map<String, MCRIIIFAnnotationBase> idAnnotationMap = new ConcurrentHashMap<>();

    private final Map<String, MCRIIIFRange> idRangeMap = new ConcurrentHashMap<>();

    public MCRIIIFPresentationManifestQuickAccess(final MCRIIIFManifest manifest) {
        this.manifest = manifest;

        this.manifest.sequences.forEach(seq -> {
            seq.canvases.forEach(canvas -> {
                idCanvasMap.put(canvas.getId(), canvas);
                canvas.images.forEach(annotation -> idAnnotationMap.put(annotation.getId(), annotation));
            });

            idSequenceMap.put(seq.getId(), seq);
        });

        this.manifest.structures.forEach(range -> {
            idRangeMap.put(range.getId(), range);
        });

    }

    public MCRIIIFSequence getSequence(String id) {
        return idSequenceMap.get(id);
    }

    public MCRIIIFCanvas getCanvas(String id) {
        return idCanvasMap.get(id);
    }

    public MCRIIIFAnnotationBase getAnnotationBase(String id) {
        return idAnnotationMap.get(id);
    }

    public MCRIIIFRange getRange(String id) {
        return idRangeMap.get(id);
    }

    public MCRIIIFManifest getManifest() {
        return manifest;
    }

}
