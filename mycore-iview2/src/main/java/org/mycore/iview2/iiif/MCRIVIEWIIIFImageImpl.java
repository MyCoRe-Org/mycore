package org.mycore.iview2.iiif;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.MCRIIIFImageImpl;
import org.mycore.iiif.model.MCRIIIFFeatures;
import org.mycore.iiif.model.MCRIIIFImageInformation;
import org.mycore.iiif.model.MCRIIIFImageQuality;
import org.mycore.iiif.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.model.MCRIIIFImageTargetSize;
import org.mycore.iiif.model.MCRIIIFImageTileInformation;
import org.mycore.iiif.model.MCRIIIFProfile;
import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRIVIEWIIIFImageImpl implements MCRIIIFImageImpl {

    public static final String DEFAULT_CONTEXT = "http://iiif.io/api/image/2/context.json";
    public static final String DEFAULT_PROTOCOL = "http://iiif.io/api/image";
    public static final double LOG_HALF = Math.log(1.0 / 2.0);
    public static final java.util.List<String> SUPPORTED_FORMATS = Arrays.asList(ImageIO.getReaderFileSuffixes());
    public static final String MAX_BYTES = "MCR.IIIFImage.Iview.MaxImageBytes";

    private static Logger LOGGER = Logger.getLogger(MCRIVIEWIIIFImageImpl.class);

    private static String buildURL(String identifier) {
        try {
            return MCRFrontendUtil.getBaseURL() + "rsc/iiif/image/" + URLEncoder.encode(identifier, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new MCRException("UTF-8 is not supported!", e);
        }
    }

    @Override
    public BufferedImage provide(String identifier,
                                 MCRIIIFImageSourceRegion region,
                                 MCRIIIFImageTargetSize targetSize,
                                 MCRIIIFImageTargetRotation rotation,
                                 MCRIIIFImageQuality imageQuality,
                                 String format) throws ImageNotFoundException, ProvidingException, UnsupportedFormatException {


        long resultingSize = (long) targetSize.getHeight() * targetSize.getWidth() * (imageQuality.equals(MCRIIIFImageQuality.color) ? 3 : 1);

        long maxImageSize = MCRConfiguration.instance().getLong(MAX_BYTES);
        if (resultingSize > maxImageSize) {
            throw new ProvidingException("Maximal image size is " + (maxImageSize / 1024 / 1024) + "MB. [" + resultingSize + "/" + maxImageSize + "]");
        }

        if (!SUPPORTED_FORMATS.contains(format.toLowerCase(Locale.ENGLISH))) {
            throw new MCRIIIFImageImpl.UnsupportedFormatException(format);
        }

        Path tiledFile = getTiledFile(identifier);
        MCRTiledPictureProps tiledPictureProps = getTiledPictureProps(tiledFile);

        int sourceWidth = region.getX2() - region.getX1();
        int sourceHeight = region.getY2() - region.getY1();

        double targetWidth = targetSize.getWidth();
        double targetHeight = targetSize.getHeight();


        double rotatationRadians = Math.toRadians(rotation.getDegrees());
        double sinRotation = Math.sin(rotatationRadians);
        double cosRotation = Math.cos(rotatationRadians);

        final int height = (int) (Math.abs(targetWidth * sinRotation) + Math.abs(targetHeight * cosRotation));
        final int width = (int) (Math.abs(targetWidth * cosRotation) + Math.abs(targetHeight * sinRotation));

        BufferedImage targetImage;
        switch (imageQuality) {
            case bitonal:
                targetImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
                break;
            case gray:
                targetImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                break;
            case color:
            default:
                targetImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        }


        // this value determines the zoom level!
        double largestScaling = Math.max(targetWidth / sourceWidth, targetHeight / sourceHeight);

        // We always want to use the the best needed zoom level!
        int sourceZoomLevel = (int) Math.min(Math.max(0, Math.ceil(tiledPictureProps.getZoomlevel() - Math.log(largestScaling) / LOG_HALF)), tiledPictureProps.getZoomlevel());

        // largestScaling is the real scale which is needed! zoomLevelScale is the scale of the nearest zoom level!
        double zoomLevelScale = Math.min(1.0, Math.pow(0.5, tiledPictureProps.getZoomlevel() - sourceZoomLevel));

        // this is the scale which is needed from the nearest zoom level to the required size of image
        double drawScaleX = (targetWidth / (sourceWidth * zoomLevelScale)), drawScaleY = (targetHeight / (sourceHeight * zoomLevelScale));

        // absolute region in zoom level this nearest zoom level
        double x1 = region.getX1() * zoomLevelScale,
                x2 = region.getX2() * zoomLevelScale,
                y1 = region.getY1() * zoomLevelScale,
                y2 = region.getY2() * zoomLevelScale;

        // now we detect the tiles to draw!
        int x1Tile = (int) Math.floor(x1 / 256),
                y1Tile = (int) Math.floor(y1 / 256),
                x2Tile = (int) Math.ceil(x2 / 256),
                y2Tile = (int) Math.ceil(y2 / 256);

        try (FileSystem zipFileSystem = MCRIView2Tools.getFileSystem(getTiledFile(identifier))) {
            Path rootPath = zipFileSystem.getPath("/");

            Graphics2D graphics = targetImage.createGraphics();
            if (rotation.isMirrored()) {
                graphics.scale(-1, 1);
                graphics.translate(-width, 0);
            }

            int xt = (int) ((targetWidth - 1) / 2), yt = (int) ((targetHeight - 1) / 2);
            graphics.translate((width - targetWidth) / 2, (height - targetHeight) / 2);
            graphics.rotate(rotatationRadians, xt, yt);

            graphics.scale(drawScaleX, drawScaleY);
            graphics.translate(-x1, -y1);

            graphics.scale(zoomLevelScale, zoomLevelScale);
            graphics.setClip(region.getX1(), region.getY1(), sourceWidth, sourceHeight);
            graphics.scale(1 / zoomLevelScale, 1 / zoomLevelScale);

            LOGGER.info(String.format(Locale.ROOT, "Using zoom-level: %d and scales %s/%s!", sourceZoomLevel, drawScaleX, drawScaleY));

            for (int x = x1Tile; x < x2Tile; x++) {
                for (int y = y1Tile; y < y2Tile; y++) {
                    ImageReader imageReader = MCRIView2Tools.getTileImageReader();
                    BufferedImage tile = MCRIView2Tools.readTile(rootPath, imageReader, sourceZoomLevel, x, y);
                    graphics.drawImage(tile, x * 256, y * 256, null);
                }
            }

        } catch (IOException e) {
            throw new ProvidingException("Error while reading tiles!", e);
        }

        return targetImage;
    }

    public MCRIIIFImageInformation getInformation(String identifier) throws ImageNotFoundException, ProvidingException {
        Path tiledFile = getTiledFile(identifier);
        MCRTiledPictureProps tiledPictureProps = getTiledPictureProps(tiledFile);

        MCRIIIFImageInformation imageInformation = new MCRIIIFImageInformation(DEFAULT_CONTEXT, buildURL(identifier), DEFAULT_PROTOCOL, tiledPictureProps.getWidth(), tiledPictureProps.getHeight());


        MCRIIIFImageTileInformation tileInformation = new MCRIIIFImageTileInformation(256, 256);
        for (int i = 0; i < tiledPictureProps.getZoomlevel(); i++) {
            tileInformation.scaleFactors.add((int) Math.pow(2, i));
        }

        imageInformation.tiles.add(tileInformation);

        return imageInformation;
    }

    @Override
    public MCRIIIFProfile getProfile() {
        MCRIIIFProfile mcriiifProfile = new MCRIIIFProfile();

        mcriiifProfile.formats = this.SUPPORTED_FORMATS.stream().filter(s -> !s.isEmpty()).collect(Collectors.toSet());

        mcriiifProfile.qualities.add("color");
        mcriiifProfile.qualities.add("bitonal");
        mcriiifProfile.qualities.add("gray");

        mcriiifProfile.supports.add(MCRIIIFFeatures.mirroring);
        mcriiifProfile.supports.add(MCRIIIFFeatures.regionByPct);
        mcriiifProfile.supports.add(MCRIIIFFeatures.regionByPx);
        mcriiifProfile.supports.add(MCRIIIFFeatures.rotationArbitrary);
        mcriiifProfile.supports.add(MCRIIIFFeatures.rotationBy90s);
        mcriiifProfile.supports.add(MCRIIIFFeatures.sizeAboveFull);
        mcriiifProfile.supports.add(MCRIIIFFeatures.sizeByWhListed);
        mcriiifProfile.supports.add(MCRIIIFFeatures.sizeByForcedWh);
        mcriiifProfile.supports.add(MCRIIIFFeatures.sizeByH);
        mcriiifProfile.supports.add(MCRIIIFFeatures.sizeByPct);
        mcriiifProfile.supports.add(MCRIIIFFeatures.sizeByW);
        mcriiifProfile.supports.add(MCRIIIFFeatures.sizeByWh);

        return mcriiifProfile;
    }

    private MCRTiledPictureProps getTiledPictureProps(Path tiledFile) throws ProvidingException {
        MCRTiledPictureProps tiledPictureProps = null;
        try {
            tiledPictureProps = MCRTiledPictureProps.getInstanceFromFile(tiledFile);
        } catch (IOException | JDOMException e) {
            throw new ProvidingException("Could not provide image information!", e);
        }
        return tiledPictureProps;
    }

    private Path getTiledFile(String identifier) throws ImageNotFoundException {
        String[] splittedIdentifier = identifier.split("/", 2);

        if (splittedIdentifier.length < 2) {
            throw new ImageNotFoundException(identifier);
        }

        String derivate = splittedIdentifier[0];
        String imagePath = splittedIdentifier[1];

        if (!Files.exists(MCRPath.getPath(derivate, imagePath))) {
            throw new ImageNotFoundException(identifier);
        }

        return MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), derivate, imagePath);
    }


}
