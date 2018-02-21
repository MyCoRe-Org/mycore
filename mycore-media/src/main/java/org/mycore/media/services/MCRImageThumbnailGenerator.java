package org.mycore.media.services;

import org.mycore.datamodel.niofs.MCRPath;

import org.mycore.imagetiler.MCRImage;
import org.mycore.imagetiler.MCRTiledPictureProps;
import org.mycore.iview2.services.MCRIView2Tools;

import javax.imageio.ImageReader;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

public class MCRImageThumbnailGenerator implements MCRThumbnailGenerator {

    private static final Pattern MATCHING_MIMETYPE = Pattern.compile("^image\\/.*");

    @Override
    public boolean matchesFileType(String mimeType, MCRPath path) {
        return MATCHING_MIMETYPE.matcher(mimeType).matches();
    }

    @Override
    public Optional<BufferedImage> getThumbnail(MCRPath path, int size) throws IOException {
        Path iviewFile = MCRImage.getTiledFile(MCRIView2Tools.getTileDir(), path.getOwner(), path.getFileName()
            .toString());
        MCRTiledPictureProps iviewFileProps = getIviewFileProps(iviewFile);
        final double width = iviewFileProps.getWidth();
        final double height = iviewFileProps.getHeight();
        final int newWidth = width > height ? (int) Math.ceil(size * width / height) : size;
        final int newHeight = width > height ? size : (int) Math.ceil(size * height / width);

        // this value determines the zoom level!
        final double scale = newWidth / width;

        // We always want to use the the best needed zoom level!
        int sourceZoomLevel = (int) Math.min(
            Math.max(0, Math.ceil(iviewFileProps.getZoomlevel() - Math.log(scale) / Math.log(1.0 / 2.0))),
            iviewFileProps.getZoomlevel());

        // scale is the real scale which is needed! zoomLevelScale is the scale of the nearest zoom level!
        double zoomLevelScale = Math.min(1.0, Math.pow(0.5, iviewFileProps.getZoomlevel() - sourceZoomLevel));

        // this is the scale which is needed from the nearest zoom level to the required size of image
        double drawScale = (newWidth / (width * zoomLevelScale));

        try (FileSystem zipFileSystem = MCRIView2Tools.getFileSystem(iviewFile)) {
            Path rootPath = zipFileSystem.getPath("/");
            ImageReader imageReader = MCRIView2Tools.getTileImageReader();
            BufferedImage testTile = MCRIView2Tools.readTile(rootPath, imageReader, sourceZoomLevel, 0, 0);
            BufferedImage targetImage = getTargetImage(newWidth, newHeight, testTile);
            Graphics2D graphics = targetImage.createGraphics();
            graphics.scale(drawScale, drawScale);
            for (int x = 0; x < Math.ceil(width * zoomLevelScale / 256); x++) {
                for (int y = 0; y < Math.ceil(height * zoomLevelScale / 256); y++) {
                    BufferedImage tile = MCRIView2Tools.readTile(rootPath, imageReader, sourceZoomLevel, x, y);
                    graphics.drawImage(tile, x * 256, y * 256, null);
                }
            }
            return Optional.of(targetImage);
        }
    }

    private MCRTiledPictureProps getIviewFileProps(Path tiledFile) throws IOException {
        MCRTiledPictureProps tiledPictureProps = null;
        try (FileSystem fileSystem = MCRIView2Tools.getFileSystem(tiledFile)) {
            tiledPictureProps = MCRTiledPictureProps.getInstanceFromDirectory(fileSystem.getPath("/"));
        } catch (IOException e) {
            throw new IOException("Could not provide image information!", e);
        }
        return tiledPictureProps;
    }

    private BufferedImage getTargetImage(int width, int height, BufferedImage firstTile) {
        return new BufferedImage(width, height, firstTile.getType());
    }
}
