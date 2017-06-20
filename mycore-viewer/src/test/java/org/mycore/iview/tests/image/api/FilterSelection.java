package org.mycore.iview.tests.image.api;

import java.util.ArrayList;
import java.util.List;

public class FilterSelection extends Selection {

    public FilterSelection(Selection fs, PixelFilter filter) {
        this.source = fs;
        this.filter = filter;
    }

    private Selection source;

    private PixelFilter filter;

    @Override
    public List<Pixel> getPixel() {
        List<Pixel> pixels = this.source.getPixel();
        List<Pixel> filteredPixels = new ArrayList<>();
        for (Pixel pixel : pixels) {
            if (filter.filter(pixel)) {
                filteredPixels.add(pixel);
            }
        }

        return filteredPixels;
    }
}
