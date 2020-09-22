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
import org.mycore.common.config.MCRConfigurationException;
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

    public static final String MAX_BYTES = "MaxImageBytes";

    private static final String TILE_FILE_PROVIDER_PROPERTY = "TileFileProvider";

    private static final Logger LOGGER = LogManager.getLogger(MCRIVIEWIIIFImageImpl.class);

    private final java.util.List<String> transparentFormats;

    private final MCRTileFileProvider tileFileProvider;

    public MCRIVIEWIIIFImageImpl(String implName) {
        super(implName);
        Map<String, String> properties = getProperties();
        String tileFileProviderClassName = properties.get(TILE_FILE_PROVIDER_PROPERTY);
        if (tileFileProviderClassName == null) {
            tileFileProvider = new MCRDefaultTileFileProvider();
        } else {
            Optional<MCRTileFileProvider> optTFP = MCRConfiguration2
                .getInstanceOf(getConfigPrefix() + TILE_FILE_PROVIDER_PROPERTY);
            if (optTFP.isPresent()) {
                tileFileProvider = optTFP.get();
            } else {
                throw new MCRConfigurationException(
                    "Configurated class (" + TILE_FILE_PROVIDER_PROPERTY + ") not found: "
                        + tileFileProviderClassName);
            }
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

        long resultingSize = (long) targetSize.getHeight() * targetSize.getWidth()
            * (imageQuality.equals(MCRIIIFImageQuality.color) ? 3 : 1);

        long maxImageSize = Optional.ofNullable(getProperties().get(MAX_BYTES)).map(Long::parseLong)
            .orElseThrow(() -> MCRConfiguration2.createConfigurationException(getConfigPrefix() + MAX_BYTES));
        if (resultingSize > maxImageSize) {
            throw new MCRIIIFImageProvidingException("Maximal image size is " + (maxImageSize / 1024 / 1024) + "MB. ["
                + resultingSize + "/" + maxImageSize + "]");
        }

        if (!SUPPORTED_FORMATS.contains(format.toLowerCase(Locale.ENGLISH))) {
            throw new MCRIIIFUnsupportedFormatException(format);
        }

        MCRTileInfo tileInfo = createTileInfo(identifier);
        Optional<Path> oTileFile = tileFileProvider.getTileFile(tileInfo);
        if (oTileFile.isEmpty()) {
            throw new MCRIIIFImageNotFoundException(identifier);
        }
        checkTileFile(identifier, tileInfo, oTileFile.get());
        MCRTiledPictureProps tiledPictureProps = getTiledPictureProps(oTileFile.get());

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

        try (FileSystem zipFileSystem = MCRIView2Tools.getFileSystem(oTileFile.get())) {
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

            MCRIIIFImageTileInformation tileInformation = new MCRIIIFImageTileInformation(256, 256);
            for (int i = 0; i < tiledPictureProps.getZoomlevel(); i++) {
                tileInformation.scaleFactors.add((int) Math.pow(2, i));
            }

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
        String[] splittedIdentifier = id.split("/", 2);
        switch (splittedIdentifier.length) {
            case 1:
                tileInfo = new MCRTileInfo(null, identifier, null);
                break;
            case 2:
                tileInfo = new MCRTileInfo(splittedIdentifier[0], splittedIdentifier[1], null);
                break;
            default:
                throw new MCRIIIFImageNotFoundException(identifier);
        }
        return tileInfo;
    }

    private void checkTileFile(String identifier, MCRTileInfo tileInfo, Path tileFilePath)
        throws MCRAccessException, MCRIIIFImageNotFoundException {
        if (!Files.exists(tileFilePath)) {
            throw new MCRIIIFImageNotFoundException(identifier);
        }
        if (tileInfo.getDerivate() != null
            && !checkPermission(identifier, tileInfo)) {
            throw MCRAccessException.missingPermission(
                "View the file " + tileInfo.getImagePath() + " in " + tileInfo.getDerivate(), tileInfo.getDerivate(),
                MCRAccessManager.PERMISSION_VIEW);
        }
    }

    protected boolean checkPermission(String identifier, MCRTileInfo tileInfo) {
        return MCRAccessManager.checkPermission(tileInfo.getDerivate(), MCRAccessManager.PERMISSION_VIEW) ||
            MCRAccessManager.checkPermission(tileInfo.getDerivate(), MCRAccessManager.PERMISSION_READ);
    }
}
