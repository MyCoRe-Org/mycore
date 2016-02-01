package org.mycore.iiif.parser;


import org.mycore.iiif.model.MCRIIIFImageTargetSize;

public class MCRIIIFScaleParser {

    private String targetScale;
    private int sourceRegionWidth;
    private int sourceRegionHeight;

    public MCRIIIFScaleParser(String targetScale, int w, int h) {
        this.targetScale = targetScale;
        this.sourceRegionWidth = w;
        this.sourceRegionHeight = h;
    }

    private boolean isFull() {
        return targetScale.equals("full");
    }

    private boolean isPercent() {
        return targetScale.startsWith("pct:");
    }

    private boolean isBestFit() {
        return targetScale.startsWith("!");
    }

    public MCRIIIFImageTargetSize parseTargetScale() {
        if (isFull())
            return new MCRIIIFImageTargetSize(sourceRegionWidth, sourceRegionHeight);

        if (isPercent())
            return parsePercentValue();

        StringBuilder wBuilder = new StringBuilder(), hBuilder = new StringBuilder();
        getWidthAndHeightStrings(wBuilder, hBuilder);
        String w = wBuilder.toString(), h = hBuilder.toString();

        try {
            if (w.length() == 0) {
                return scaleToHeight(Integer.parseInt(h));
            } else if (h.length() == 0) {
                return scaleToWidth(Integer.parseInt(w));
            } else if (isBestFit()) {
                return bestFit(Integer.parseInt(w), Integer.parseInt(h));
            } else {
                return scale(Integer.parseInt(w), Integer.parseInt(h));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(h + " or " + w + " ist not a valid scale value!", e);
        }


    }

    private MCRIIIFImageTargetSize scale(int w, int h) {
        if (w <= 0) {
            throw new IllegalArgumentException("width to scale must be positive! [" + w + "]");
        }
        if (h <= 0) {
            throw new IllegalArgumentException("height to scale must be positive! [" + h + "]");
        }
        return new MCRIIIFImageTargetSize(w, h);
    }

    private MCRIIIFImageTargetSize bestFit(int w, int h) {
        if (w <= 0) {
            throw new IllegalArgumentException("width to scale must be positive! [" + w + "]");
        }
        if (h <= 0) {
            throw new IllegalArgumentException("height to scale must be positive! [" + h + "]");
        }
        double ratio = Math.min(((double) w) / this.sourceRegionWidth, ((double) h) / this.sourceRegionHeight);
        return new MCRIIIFImageTargetSize((int) Math.round(this.sourceRegionWidth * ratio), (int) Math.round(this.sourceRegionHeight * ratio));
    }

    private MCRIIIFImageTargetSize scaleToWidth(Integer w) {
        if (w <= 0) {
            throw new IllegalArgumentException("width to scale must be positive! [" + w + "]");
        }
        double ratio = ((double) this.sourceRegionHeight) / this.sourceRegionWidth;
        return new MCRIIIFImageTargetSize(w, (int) Math.ceil(ratio * w));
    }

    private MCRIIIFImageTargetSize scaleToHeight(Integer h) {
        if (h <= 0) {
            throw new IllegalArgumentException("height to scale must be positive! [" + h + "]");
        }
        double ratio = ((double) this.sourceRegionWidth) / this.sourceRegionHeight;
        return new MCRIIIFImageTargetSize((int) Math.ceil(ratio * h), h);
    }

    private MCRIIIFImageTargetSize parsePercentValue() {
        String rightValue = this.targetScale.substring("pct:".length());
        double percentValue;

        try {
            percentValue = Double.parseDouble(rightValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(rightValue + " is not a valid scale!", e);
        }

        if (percentValue <= 0) {
            throw new IllegalArgumentException("Scale has to be positive! [" + percentValue + "]");
        }

        return new MCRIIIFImageTargetSize((int) Math.ceil(percentValue / 100 * sourceRegionWidth), (int) Math.ceil(percentValue / 100 * sourceRegionHeight));
    }

    private void getWidthAndHeightStrings(StringBuilder wBuilder, StringBuilder hBuilder) {
        char[] chars = this.targetScale.toCharArray();
        boolean writeW = true;
        boolean first = true;
        for (char currentChar : chars) {
            switch (currentChar) {
                case ',':
                    first = false;
                    if (!writeW) {
                        throw new IllegalArgumentException("second , found in scale!");
                    }
                    writeW = false;
                    break;
                case '!':
                    if (!first) {
                        throw new IllegalArgumentException("! should be located only in the first position of scale!");
                    }
                    first = false;
                    break;
                default:
                    first = false;
                    if (writeW) {
                        wBuilder.append(currentChar);
                    } else {
                        hBuilder.append(currentChar);
                    }
                    break;
            }
        }

    }


}
