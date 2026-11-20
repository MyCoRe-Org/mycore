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
package org.mycore.iview2.services;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.mycore.imagetiler.MCRImage;

/**
 * Creates a tiled test image (.iview2 file) for tests.
 */
public final class MCRTiledTestImage {

    private MCRTiledTestImage() {
    }

    /**
     * Creates a single colored PNG image and tiles it to <code>tileDir</code>.
     *
     * @return the .iview2 file
     */
    public static Path create(Path sourceDir, Path tileDir, String imagePath, int width, int height)
        throws IOException {
        Path sourceImage = sourceDir.resolve(imagePath);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        ImageIO.write(image, "png", sourceImage.toFile());
        MCRImage tiler = MCRImage.getInstance(sourceImage, (String) null, imagePath);
        tiler.setTileDir(tileDir);
        tiler.tile();
        return MCRImage.getTiledFile(tileDir, null, imagePath);
    }

    /**
     * Replaces every tile of the .iview2 file with data that is not a JPEG image.
     */
    public static void corruptAllTiles(Path tiledFile) throws IOException {
        try (FileSystem zipFileSystem = FileSystems.newFileSystem(tiledFile);
            Stream<Path> files = Files.walk(zipFileSystem.getPath("/"))) {
            for (Path tile : files.filter(path -> path.toString().endsWith(".jpg")).toList()) {
                Files.write(tile, "this is not a JPEG file".getBytes(StandardCharsets.UTF_8));
            }
        }
    }

}
