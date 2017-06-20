package org.mycore.iview.tests.image.api;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Selection {

    private static final Logger LOGGER = LogManager.getLogger(Selection.class);

    /**
     * @return a List of all Pixel wich contains this selection
     */
    public abstract List<Pixel> getPixel();

    /**
     * @return returns a hashmap wich contains the position as key and the pixel as value
     */
    public Map<Position, Pixel> getPositionPixelMap() {
        HashMap<Position, Pixel> positionPixelHashMap = new HashMap<>();

        List<Pixel> pixels = this.getPixel();
        for (Pixel p : pixels) {
            positionPixelHashMap.put(p.getPosition(), p);
        }

        return positionPixelHashMap;
    }

    /**
     * Creates a selection of all Pixel of a Buffered image
     *
     * @param bufferedImage the buffered image from wich the selection should be created
     * @return the selection
     */
    public static Selection fromBufferedImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        final ArrayList<Pixel> p = new ArrayList<Pixel>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                p.add(new Pixel(new Color(bufferedImage.getRGB(x, y)), new Position(x, y)));
            }
        }

        return new Selection() {
            @Override
            public List<Pixel> getPixel() {
                return p;
            }
        };
    }

    public Position getUpperLeft() {
        Map<Position, Pixel> positionPixelMap = this.getPositionPixelMap();
        Set<Position> positionSet = positionPixelMap.keySet();

        LOGGER.debug("getUpperLeft: ");
        int upper = 100000;
        int left = 100000;

        LOGGER.debug(String.format("(Positions : %d )", positionSet.size()));
        for (Position position : positionSet) {
            if (position.getX() < left) {
                left = position.getX();
            }

            if (position.getY() < upper) {
                upper = position.getY();
            }
        }

        LOGGER.debug("upper: " + upper);
        LOGGER.debug("left: " + left);

        return new Position(left, upper);
    }

    public Position getLowerRight() {
        Map<Position, Pixel> positionPixelMap = this.getPositionPixelMap();
        Set<Position> positionSet = positionPixelMap.keySet();

        LOGGER.debug("getLowerRight: ");
        int lower = 0;
        int right = 0;

        LOGGER.debug(String.format("(Positions : %d )", positionSet.size()));
        for (Position position : positionSet) {
            if (position.getX() > right) {
                right = position.getX();
            }

            if (position.getY() > lower) {
                lower = position.getY();
            }
        }

        LOGGER.debug("lower: " + lower);
        LOGGER.debug("right: " + right);

        return new Position(right, lower);
    }

    public BufferedImage toBufferedImage() {
        Position upperLeft = this.getUpperLeft();
        Position lowerRight = this.getLowerRight();

        BufferedImage result = new BufferedImage(lowerRight.getX() - upperLeft.getX() + 1,
            lowerRight.getY() - upperLeft.getY() + 1, BufferedImage.TYPE_INT_RGB);

        Map<Position, Pixel> positionPixelMap = this.getPositionPixelMap();
        Set<Position> positionSet = positionPixelMap.keySet();

        LOGGER.debug("start-Set-RGB()");
        for (Position position : positionSet) {
            result.setRGB(position.getX() - upperLeft.getX(), position.getY() - upperLeft.getY(),
                positionPixelMap.get(position).getColor().getRGB());
        }
        LOGGER.debug("end-Set-RGB()");

        return result;
    }

}
