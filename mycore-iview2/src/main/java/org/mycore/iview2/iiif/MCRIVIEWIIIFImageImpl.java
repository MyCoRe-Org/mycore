/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.iview2.iiif;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessException;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.iiif.image.MCRIIIFImageUtil;
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
import org.mycore.iview2.backend.MCRDefaultTileFileProvider;
import org.mycore.iview2.backend.MCRTileFileProvider;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.iview2.services.MCRIView2Tools;

public class MCRIVIEWIIIFImageImpl extends MCRIIIFImageImpl {

    public static final String DEFAULT_PROTOCOL = "http://iiif.io/api/image";

    public static final double LOG_HALF = Math.log(1.0 / 2.0);

    public static final java.util.List<String> SUPPORTED_FORMATS = Arrays.asList(ImageIO.getReaderFileSuffixes());

    public static final String MAX_BYTES_PROPERTY = "MaxImageBytes";

    private static final String TILE_FILE_PROVIDER_PROPERTY = "TileFileProvider";

    private static final String IDENTIFIER_SEPARATOR_PROPERTY = "IdentifierSeparator";

    private static final Logger LOGGER = LogManager.getLogger(MCRIVIEWIIIFImageImpl.class);

    private final java.util.List<String> transparentFormats;

    protected final MCRTileFileProvider tileFileProvider;

    public MCRIVIEWIIIFImageImpl(String implName) {
        super(implName);
        Map<String, String> properties = getProperties();
        String tileFileProviderClassName = properties.get(TILE_FILE_PROVIDER_PROPERTY);
        if (tileFileProviderClassName == null) {
            tileFileProvider = new MCRDefaultTileFileProvider();
        } else {
            tileFileProvider = MCRConfiguration2.getInstanceOfOrThrow(
                MCRTileFileProvider.class, getConfigPrefix() + TILE_FILE_PROVIDER_PROPERTY);
        }

        transparentFormats = Arrays.asList(properties.get("TransparentFormats").split(","));
    }

    private String buildURL(String identifier) {
        return MCRIIIFImageUtil.getIIIFURL(this) + MCRIIIFImageUtil.encodeImageIdentifier(identifier);
    }

    @Override
    public BufferedImage provide(String identifier,
        MCRIIIFImageSourceRegion region,
        MCRIIIFImageTargetSize targetSize,
        MCRIIIFImageTargetRotation rotation,
        MCRIIIFImageQuality imageQuality,
        String format) throws MCRIIIFImageNotFoundException, MCRIIIFImageProvidingException,
        MCRIIIFUnsupportedFormatException, MCRAccessException {

        if (!SUPPORTED_FORMATS.contains(format.toLowerCase(Locale.ENGLISH))) {
            throw new MCRIIIFUnsupportedFormatException(format);
        }

        checkMaximumFileSize(targetSize, imageQuality);

        MCRTileInfo tileInfo = createTileInfo(identifier);
        Optional<Path> oTileFile = tileFileProvider.getTileFile(tileInfo);
        if (oTileFile.isEmpty()) {
            throw new MCRIIIFImageNotFoundException(identifier);
        }
        checkTileFile(identifier, tileInfo, oTileFile.get());
        MCRTiledPictureProps tiledPictureProps = getTiledPictureProps(oTileFile.get());

        int sourceZoomLevel = getSourceZoomLevel(region, targetSize, tiledPictureProps);

        // largestScaling is the real scale which is needed! zoomLevelScale is the scale of the nearest zoom level!
        double zoomLevelScale = Math.min(1.0, Math.pow(0.5, tiledPictureProps.getZoomlevel() - sourceZoomLevel));

        // now we detect the tiles to draw!
        double tileSizeFactor = zoomLevelScale / 256;
        MCRIIIFImageSourceRegion sourceTiles = new MCRIIIFImageSourceRegion(
            (int) Math.floor(region.x1() * tileSizeFactor),
            (int) Math.floor(region.y1() * tileSizeFactor),
            (int) Math.ceil(region.x2() * tileSizeFactor),
            (int) Math.ceil(region.y2() * tileSizeFactor));

        MCRIIIFImageTargetSize rotatedSize = getRotatedSize(targetSize, rotation);
        BufferedImage targetImage = new BufferedImage(rotatedSize.width(), rotatedSize.height(),
            getImageType(imageQuality, format));
        try (FileSystem zipFileSystem = MCRIView2Tools.getFileSystem(oTileFile.get())) {
            Path rootPath = zipFileSystem.getPath("/");

            Graphics2D graphics = targetImage.createGraphics();
            try {
                applyRotation(graphics, rotation, rotatedSize, targetSize);
                applyScale(graphics, region, targetSize, sourceZoomLevel, zoomLevelScale);
                drawTiles(rootPath, sourceZoomLevel, sourceTiles, graphics);
            } finally {
                graphics.dispose();
            }
        } catch (IOException e) {
            throw new MCRIIIFImageProvidingException("Error while reading tiles!", e);
        }

        return targetImage;
    }

    private static int getSourceZoomLevel(MCRIIIFImageSourceRegion region, MCRIIIFImageTargetSize targetSize,
        MCRTiledPictureProps tiledPictureProps) {
        MCRIIIFImageTargetSize sourceSize = new MCRIIIFImageTargetSize(
            region.x2() - region.x1(),
            region.y2() - region.y1());

        // this value determines the zoom level!
        double largestScaling = Math.max(
            (double) targetSize.width() / sourceSize.width(),
            (double) targetSize.height() / sourceSize.height());

        // We always want to use the the best needed zoom level!
        return (int) Math.min(
            Math.max(0, Math.ceil(tiledPictureProps.getZoomlevel() - Math.log(largestScaling) / LOG_HALF)),
            tiledPictureProps.getZoomlevel());
    }

    private int getImageType(MCRIIIFImageQuality imageQuality, String format) {
        final int imageType = switch (imageQuality) {
            case bitonal -> BufferedImage.TYPE_BYTE_BINARY;
            case gray -> BufferedImage.TYPE_BYTE_GRAY;
            //color is also default case
            default -> transparentFormats.contains(format) ? BufferedImage.TYPE_4BYTE_ABGR
                : BufferedImage.TYPE_3BYTE_BGR;
        };
        return imageType;
    }

    private static MCRIIIFImageTargetSize getRotatedSize(MCRIIIFImageTargetSize targetSize,
        MCRIIIFImageTargetRotation rotation) {
        if (rotation.degrees() % 180 == 0) {
            return targetSize;
        }
        if (rotation.degrees() % 90 == 0) {
            return new MCRIIIFImageTargetSize(targetSize.height(), targetSize.width());
        }
        double rotatationRadians = Math.toRadians(rotation.degrees());
        double sinRotation = Math.sin(rotatationRadians);
        double cosRotation = Math.cos(rotatationRadians);

        MCRIIIFImageTargetSize rotatedSize = new MCRIIIFImageTargetSize(
            (int) (Math.abs(targetSize.width() * cosRotation) + Math.abs(targetSize.height() * sinRotation)),
            (int) (Math.abs(targetSize.width() * sinRotation) + Math.abs(targetSize.height() * cosRotation)));
        return rotatedSize;
    }

    private void checkMaximumFileSize(MCRIIIFImageTargetSize targetSize, MCRIIIFImageQuality imageQuality)
        throws MCRIIIFImageProvidingException {
        long resultingSize = (long) targetSize.height() * targetSize.width()
            * (imageQuality.equals(MCRIIIFImageQuality.color) ? 3 : 1);

        long maxImageSize = Optional.ofNullable(getProperties().get(MAX_BYTES_PROPERTY)).map(Long::parseLong)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(getConfigPrefix() + MAX_BYTES_PROPERTY));
        if (resultingSize > maxImageSize) {
            throw new MCRIIIFImageProvidingException("Maximal image size is " + (maxImageSize / 1024 / 1024) + "MB. ["
                + resultingSize + "/" + maxImageSize + "]");
        }
    }

    private static void drawTiles(Path tileRoot, int sourceZoomLevel, MCRIIIFImageSourceRegion sourceTiles,
        Graphics2D graphics) throws IOException {
        for (int x = sourceTiles.x1(); x < sourceTiles.x2(); x++) {
            for (int y = sourceTiles.y1(); y < sourceTiles.y2(); y++) {
                ImageReader imageReader = MCRIView2Tools.getTileImageReader();
                BufferedImage tile = MCRIView2Tools.readTile(tileRoot, imageReader, sourceZoomLevel, x, y);
                graphics.drawImage(tile, x * 256, y * 256, null);
            }
        }
    }

    private static void applyScale(Graphics2D graphics, MCRIIIFImageSourceRegion region,
        MCRIIIFImageTargetSize targetSize, int sourceZoomLevel, double zoomLevelScale) {
        // absolute region in zoom level this nearest zoom level
        MCRIIIFImageSourceRegion scaledRegion = new MCRIIIFImageSourceRegion(
            (int) Math.round(region.x1() * zoomLevelScale),
            (int) Math.round(region.y1() * zoomLevelScale),
            (int) Math.round(region.x2() * zoomLevelScale),
            (int) Math.round(region.y2() * zoomLevelScale));
        // this is the scale which is needed from the nearest zoom level to the required size of image
        int regionWidth = region.x2() - region.x1();
        int regionHeight = region.y2() - region.y1();
        double drawScaleX = targetSize.width() / (regionWidth * zoomLevelScale);
        double drawScaleY = targetSize.height() / (regionHeight * zoomLevelScale);
        LOGGER.info(() -> String.format(Locale.ROOT, "Using zoom-level: %d and scales %s/%s!", sourceZoomLevel,
            drawScaleX, drawScaleY));
        graphics.scale(drawScaleX, drawScaleY);
        graphics.translate(-scaledRegion.x1(), -scaledRegion.y1());

        graphics.scale(zoomLevelScale, zoomLevelScale);
        graphics.setClip(region.x1(), region.y1(), regionWidth, regionHeight);
        graphics.scale(1 / zoomLevelScale, 1 / zoomLevelScale);
    }

    private static void applyRotation(Graphics2D graphics, MCRIIIFImageTargetRotation rotation,
        MCRIIIFImageTargetSize rotatedSize, MCRIIIFImageTargetSize targetSize) {
        if (rotation.mirrored()) {
            graphics.scale(-1, 1);
            graphics.translate(-rotatedSize.width(), 0);
        }

        graphics.translate((rotatedSize.width() - targetSize.width()) / 2.0,
            (rotatedSize.height() - targetSize.height()) / 2.0);
        double xt = targetSize.width() / 2.0;
        double yt = targetSize.height() / 2.0;
        graphics.rotate(Math.toRadians(rotation.degrees()), xt, yt);
    }

    public MCRIIIFImageInformation getInformation(String identifier)
        throws MCRIIIFImageNotFoundException, MCRIIIFImageProvidingException, MCRAccessException {
        try {
            MCRTileInfo tileInfo = createTileInfo(identifier);
            Optional<Path> oTiledFile = tileFileProvider.getTileFile(tileInfo);
            if (oTiledFile.isEmpty()) {
                throw new MCRIIIFImageNotFoundException(identifier);
            }
            final Path tileFilePath = oTiledFile.get();
            checkTileFile(identifier, tileInfo, tileFilePath);
            MCRTiledPictureProps tiledPictureProps = getTiledPictureProps(tileFilePath);

            MCRIIIFImageInformation imageInformation = new MCRIIIFImageInformation(MCRIIIFBase.API_IMAGE_2,
                buildURL(identifier), DEFAULT_PROTOCOL, tiledPictureProps.getWidth(), tiledPictureProps.getHeight(),
                Files.getLastModifiedTime(tileFilePath).toMillis());

            MCRIIIFImageTileInformation tileInformation = new MCRIIIFImageTileInformation(256, 256,
                MCRIIIFImageTileInformation.scaleFactorsAsPowersOfTwo(tiledPictureProps.getZoomlevel()));

            imageInformation.tiles.add(tileInformation);

            return imageInformation;
        } catch (FileSystemNotFoundException | IOException e) {
            LOGGER.error("Could not find Iview ZIP for {}", identifier, e);
            throw new MCRIIIFImageNotFoundException(identifier);
        }
    }

    @Override
    public MCRIIIFImageProfile getProfile() {
        MCRIIIFImageProfile mcriiifImageProfile = new MCRIIIFImageProfile();

        mcriiifImageProfile.formats = SUPPORTED_FORMATS.stream().filter(s -> !s.isEmpty())
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
        } catch (IOException e) {
            throw new MCRIIIFImageProvidingException("Could not provide image information!", e);
        }
        return tiledPictureProps;
    }

    protected MCRTileInfo createTileInfo(String identifier) throws MCRIIIFImageNotFoundException {
        MCRTileInfo tileInfo = null;
        String id = identifier.contains(":/") ? identifier.replaceFirst(":/", "/") : identifier;
        String separator = getProperties().getOrDefault(IDENTIFIER_SEPARATOR_PROPERTY, "/");
        String[] splittedIdentifier = id.split(separator, 2);
        tileInfo = switch (splittedIdentifier.length) {
            case 1 -> new MCRTileInfo(null, identifier, null);
            case 2 -> new MCRTileInfo(splittedIdentifier[0], splittedIdentifier[1], null);
            default -> throw new MCRIIIFImageNotFoundException(identifier);
        };
        return tileInfo;
    }

    private void checkTileFile(String identifier, MCRTileInfo tileInfo, Path tileFilePath)
        throws MCRAccessException, MCRIIIFImageNotFoundException {
        if (!Files.exists(tileFilePath)) {
            throw new MCRIIIFImageNotFoundException(identifier);
        }
        if (tileInfo.derivate() != null
            && !checkPermission(identifier, tileInfo)) {
            throw MCRAccessException.missingPermission(
                "View the file " + tileInfo.imagePath() + " in " + tileInfo.derivate(), tileInfo.derivate(),
                MCRAccessManager.PERMISSION_VIEW);
        }
    }

    protected boolean checkPermission(String identifier, MCRTileInfo tileInfo) {
        return MCRAccessManager.checkPermission(tileInfo.derivate(), MCRAccessManager.PERMISSION_VIEW) ||
            MCRAccessManager.checkPermission(tileInfo.derivate(), MCRAccessManager.PERMISSION_READ);
    }
}
