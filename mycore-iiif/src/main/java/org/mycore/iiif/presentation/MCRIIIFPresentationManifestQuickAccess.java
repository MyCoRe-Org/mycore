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

