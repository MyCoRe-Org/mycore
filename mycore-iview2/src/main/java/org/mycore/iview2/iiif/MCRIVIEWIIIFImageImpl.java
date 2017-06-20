package org.mycore.iview2.iiif;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.access.MCRAccessException;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.frontend.MCRFrontendUtil;
import org.mycore.iiif.image.impl.MCRIIIFImageImpl;
import org.mycore.iiif.image.impl.MCRIIIFImageNotFoundException;
import org.mycore.iiif.image.impl.MCRIIIFImageProvidingException;
import org.mycore.iiif.image.impl.MCRIIIFUnsupportedFormatException;
import org.mycore.iiif.image.model.MCRIIIFFeatures;
import org.mycore.iiif.image.model.MCRIIIFImageInformation;
import org.mycore.iiif.image.model.MCRIIIFImageProfile;
import org.mycore.iiif.image.model.MCRIIIFImageQuality;
import org.mycore.iiif.image.model.MCRIIIFImageSourceRegion;
import org.mycore.iiif.image.model.MCRIIIFImageTargetRotation;
import org.mycore.iiif.image.model.MCRIIIFImageTargetSize;
import org.mycore.iiif.image.model.MCRIIIFImageTileInformation;
import org.mycore.iiif.model.MCRIIIFBase;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRIVIEWIIIFImageImpl extends MCRIIIFImageImpl {

    public static final String DEFAULT_PROTOCOL = "http://iiif.io/api/image";

    public static final double LOG_HALF = Math.log(1.0 / 2.0);

    public static final java.util.List<String> SUPPORTED_FORMATS = Arrays.asList(ImageIO.getReaderFileSuffixes());

    public static final String MAX_BYTES = "MCR.IIIFImage.Iview.MaxImageBytes";

    private static final String TILE_FILE_PROVIDER_PROPERTY = "TileFileProvider";

    private static Logger LOGGER = LogManager.getLogger(MCRIVIEWIIIFImageImpl.class);

    private java.util.List<String> transparentFormats;

    private MCRTileFileProvider tileFileProvider;

    public MCRIVIEWIIIFImageImpl(String implName) {
        super(implName);
        Map<String, String> properties = getProperties();

        String tileFileProviderClassName = properties.get(TILE_FILE_PROVIDER_PROPERTY);

        if (tileFileProviderClassName == null) {
            tileFileProviderClassName = MCRDefaultTileFileProvider.class.getName();
        }

        try {
            Class<MCRTileFileProvider> classObject = (Class<MCRTileFileProvider>) Class
                .forName(tileFileProviderClassName);
            Constructor<MCRTileFileProvider> constructor = classObject.getConstructor();
            tileFileProvider = constructor.newInstance();
        } catch (ClassNotFoundException e) {
            throw new MCRConfigurationException(
                "Configurated class (" + TILE_FILE_PROVIDER_PROPERTY + ") not found: " + tileFileProviderClassName, e);
        } catch (NoSuchMethodException e) {
            throw new MCRConfigurationException("Configurated class (" + TILE_FILE_PROVIDER_PROPERTY
                + ") needs a default constructor: " + tileFileProviderClassName);
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new MCRException(e);
        }
        transparentFormats = Arrays.asList(properties.get("TransparentFormats").split(","));
    }

    private String buildURL(String identifier) {
        try {
            return MCRFrontendUtil.getBaseURL() + "rsc/iiif/image/" + getImplName() + "/"
                + URLEncoder.encode(identifier, "UTF-8");
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
        String format) throws MCRIIIFImageNotFoundException, MCRIIIFImageProvidingException,
        MCRIIIFUnsupportedFormatException, MCRAccessException {

        long resultingSize = (long) targetSize.getHeight() * targetSize.getWidth()
            * (imageQuality.equals(MCRIIIFImageQuality.color) ? 3 : 1);

        long maxImageSize = MCRConfiguration.instance().getLong(MAX_BYTES);
        if (resultingSize > maxImageSize) {
            throw new MCRIIIFImageProvidingException("Maximal image size is " + (maxImageSize / 1024 / 1024) + "MB. ["
                + resultingSize + "/" + maxImageSize + "]");
        }

        if (!SUPPORTED_FORMATS.contains(format.toLowerCase(Locale.ENGLISH))) {
            throw new MCRIIIFUnsupportedFormatException(format);
        }

        Path tiledFile = tileFileProvider.getTiledFile(identifier);
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
                if (transparentFormats.contains(format)) {
                    targetImage = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                } else {
                    targetImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                }
        }

        // this value determines the zoom level!
        double largestScaling = Math.max(targetWidth / sourceWidth, targetHeight / sourceHeight);

        // We always want to use the the best needed zoom level!
        int sourceZoomLevel = (int) Math.min(
            Math.max(0, Math.ceil(tiledPictureProps.getZoomlevel() - Math.log(largestScaling) / LOG_HALF)),
            tiledPictureProps.getZoomlevel());

        // largestScaling is the real scale which is needed! zoomLevelScale is the scale of the nearest zoom level!
        double zoomLevelScale = Math.min(1.0, Math.pow(0.5, tiledPictureProps.getZoomlevel() - sourceZoomLevel));

        // this is the scale which is needed from the nearest zoom level to the required size of image
        double drawScaleX = (targetWidth / (sourceWidth * zoomLevelScale)),
            drawScaleY = (targetHeight / (sourceHeight * zoomLevelScale));

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

        try (FileSystem zipFileSystem = MCRIView2Tools.getFileSystem(tileFileProvider.getTiledFile(identifier))) {
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

            LOGGER.info(String.format(Locale.ROOT, "Using zoom-level: %d and scales %s/%s!", sourceZoomLevel,
                drawScaleX, drawScaleY));

            for (int x = x1Tile; x < x2Tile; x++) {
                for (int y = y1Tile; y < y2Tile; y++) {
                    ImageReader imageReader = MCRIView2Tools.getTileImageReader();
                    BufferedImage tile = MCRIView2Tools.readTile(rootPath, imageReader, sourceZoomLevel, x, y);
                    graphics.drawImage(tile, x * 256, y * 256, null);
                }
            }

        } catch (IOException e) {
            throw new MCRIIIFImageProvidingException("Error while reading tiles!", e);
        }

        return targetImage;
    }

    public MCRIIIFImageInformation getInformation(String identifier)
        throws MCRIIIFImageNotFoundException, MCRIIIFImageProvidingException, MCRAccessException {
        try {
            Path tiledFile = tileFileProvider.getTiledFile(identifier);
            MCRTiledPictureProps tiledPictureProps = getTiledPictureProps(tiledFile);

            MCRIIIFImageInformation imageInformation = new MCRIIIFImageInformation(MCRIIIFBase.API_IMAGE_2,
                buildURL(identifier), DEFAULT_PROTOCOL, tiledPictureProps.getWidth(), tiledPictureProps.getHeight());

            MCRIIIFImageTileInformation tileInformation = new MCRIIIFImageTileInformation(256, 256);
            for (int i = 0; i < tiledPictureProps.getZoomlevel(); i++) {
                tileInformation.scaleFactors.add((int) Math.pow(2, i));
            }

            imageInformation.tiles.add(tileInformation);

            return imageInformation;
        } catch (FileSystemNotFoundException e) {
            LOGGER.error("Could not find Iview ZIP for " + identifier, e);
            throw new MCRIIIFImageNotFoundException(identifier);
        }
    }

    @Override
    public MCRIIIFImageProfile getProfile() {
        MCRIIIFImageProfile mcriiifImageProfile = new MCRIIIFImageProfile();

        mcriiifImageProfile.formats = this.SUPPORTED_FORMATS.stream().filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

        mcriiifImageProfile.qualities.add("color");
        mcriiifImageProfile.qualities.add("bitonal");
        mcriiifImageProfile.qualities.add("gray");

        mcriiifImageProfile.supports.add(MCRIIIFFeatures.mirroring);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.regionByPct);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.regionByPx);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.rotationArbitrary);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.rotationBy90s);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.sizeAboveFull);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.sizeByWhListed);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.sizeByForcedWh);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.sizeByH);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.sizeByPct);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.sizeByW);
        mcriiifImageProfile.supports.add(MCRIIIFFeatures.sizeByWh);

        return mcriiifImageProfile;
    }

    private MCRTiledPictureProps getTiledPictureProps(Path tiledFile) throws MCRIIIFImageProvidingException {
        MCRTiledPictureProps tiledPictureProps = null;
        try (FileSystem fileSystem = MCRIView2Tools.getFileSystem(tiledFile)) {
            tiledPictureProps = MCRTiledPictureProps.getInstanceFromDirectory(fileSystem.getPath("/"));
        } catch (IOException | JDOMException e) {
            throw new MCRIIIFImageProvidingException("Could not provide image information!", e);
        }
        return tiledPictureProps;
    }

}
