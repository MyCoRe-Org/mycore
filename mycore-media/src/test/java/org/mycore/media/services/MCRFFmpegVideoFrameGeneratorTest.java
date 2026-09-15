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

package org.mycore.media.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mycore.common.MCRTestConfiguration;
import org.mycore.common.MCRTestProperty;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.test.MyCoReTest;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;

@MyCoReTest
class MCRFFmpegVideoFrameGeneratorTest {

    @TempDir
    Path directory;

    @Test
    void preservesResolutionOnNonDefaultFileSystem() throws Exception {
        requireFFmpeg();
        Path video = directory.resolve("video.mkv");
        new FFmpeg().run(List.of("-f", "lavfi", "-i", "color=c=red:s=640x360",
            "-frames:v", "1", "-c:v", "ffv1", video.toString()));
        try (var fileSystem = FileSystems.newFileSystem(directory.resolve("videos.zip"), Map.of("create", "true"))) {
            Path storedVideo = fileSystem.getPath("/video with spaces.mkv");
            Files.copy(video, storedVideo);
            BufferedImage frame = configuredGenerator().generateFrame(storedVideo);
            assertEquals(640, frame.getWidth());
            assertEquals(360, frame.getHeight());
            // Verify actual decoded image content, not merely the dimensions.
            int red = (frame.getRGB(320, 180) >> 16) & 0xff;
            assertTrue(red > 240);
        }
    }

    @Test
    void prefersAttachedCoverAtOriginalResolution() throws Exception {
        requireFFmpeg();
        Path cover = directory.resolve("cover.png");
        BufferedImage coverImage = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < coverImage.getHeight(); y++) {
            for (int x = 0; x < coverImage.getWidth(); x++) {
                coverImage.setRGB(x, y, 0xff0000);
            }
        }
        ImageIO.write(coverImage, "png", cover.toFile());
        Path video = directory.resolve("covered.mp4");
        new FFmpeg().run(List.of("-f", "lavfi", "-i", "color=c=blue:s=320x180:d=1",
            "-i", cover.toString(), "-map", "0:v", "-map", "1:v", "-c:v:0", "mpeg4",
            "-c:v:1", "png", "-disposition:v:1", "attached_pic", video.toString()));
        BufferedImage frame = configuredGenerator().generateFrame(video);
        assertEquals(640, frame.getWidth());
        assertEquals(360, frame.getHeight());
        assertTrue(((frame.getRGB(320, 180) >> 16) & 0xff) > 240);
    }

    @Test
    void selectsRepresentativeFrameAfterSeeking() throws Exception {
        requireFFmpeg();
        Path video = directory.resolve("representative.mkv");
        // At the 10% seek position the video is still red. Most of the following frames are blue.
        new FFmpeg().run(List.of("-f", "lavfi", "-i", "color=c=red:s=320x180:r=10:d=2",
            "-f", "lavfi", "-i", "color=c=blue:s=320x180:r=10:d=8",
            "-filter_complex", "[0:v][1:v]concat=n=2:v=1:a=0[v]", "-map", "[v]",
            "-c:v", "ffv1", video.toString()));
        BufferedImage frame = configuredGenerator().generateFrame(video);
        assertEquals(320, frame.getWidth());
        assertEquals(180, frame.getHeight());
        assertTrue(((frame.getRGB(160, 90) >> 16) & 0xff) > 240);
    }

    @Test
    void preservesOpeningFrame() throws Exception {
        requireFFmpeg();
        BufferedImage frame = configuredGenerator().generateFrame(videoWithOpening("red"));
        assertEquals(320, frame.getWidth());
        assertEquals(180, frame.getHeight());
        assertTrue(((frame.getRGB(160, 90) >> 16) & 0xff) > 240);
    }

    @Test
    void fallsBackForBlackOrWhiteOpening() throws Exception {
        requireFFmpeg();
        for (String color : List.of("black", "white")) {
            BufferedImage frame = configuredGenerator().generateFrame(videoWithOpening(color));
            assertTrue((frame.getRGB(160, 90) & 0xff) > 240);
            assertTrue(((frame.getRGB(160, 90) >> 16) & 0xff) < 16);
        }
    }

    private Path videoWithOpening(String color) throws IOException {
        Path video = directory.resolve(color + ".mkv");
        new FFmpeg().run(List.of("-f", "lavfi", "-i", "color=c=" + color + ":s=320x180:r=10:d=2",
            "-f", "lavfi", "-i", "color=c=blue:s=320x180:r=10:d=8",
            "-filter_complex", "[0:v][1:v]concat=n=2:v=1:a=0[v]", "-map", "[v]",
            "-c:v", "ffv1", video.toString()));
        return video;
    }

    @Test
    void rejectsAudioWithoutCover() throws Exception {
        requireFFmpeg();
        Path audio = directory.resolve("audio.wav");
        new FFmpeg().run(List.of("-f", "lavfi", "-i", "sine=duration=0.1", audio.toString()));
        IOException exception = assertThrows(IOException.class, () -> configuredGenerator().generateFrame(audio));
        assertTrue(exception.getMessage().contains("No video stream"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.Media.VideoFrameGenerator.ProbeExecutable", string = "/missing/ffprobe")
    })
    void injectsProbeExecutable() throws Exception {
        requireFFmpeg();
        Path video = directory.resolve("video.mp4");
        Files.writeString(video, "input");
        IOException exception = assertThrows(IOException.class, () -> configuredGenerator().generateFrame(video));
        assertTrue(exception.getMessage().contains("/missing/ffprobe"));
    }

    @Test
    void rejectsInvalidVideo() throws Exception {
        requireFFmpeg();
        Path video = directory.resolve("broken.mp4");
        Files.writeString(video, "not a video");
        assertThrows(IOException.class, () -> configuredGenerator().generateFrame(video));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.Media.VideoFrameGenerator.Executable", string = "/missing/ffmpeg")
    })
    void injectsExecutable() throws Exception {
        Path video = directory.resolve("video.mp4");
        Files.writeString(video, "input");
        assertFalse(configuredGenerator().checkRequirements(video));
        IOException exception = assertThrows(IOException.class, () -> configuredGenerator().generateFrame(video));
        assertTrue(exception.getMessage().contains("/missing/ffmpeg"));
    }

    @Test
    @MCRTestConfiguration(properties = {
        @MCRTestProperty(key = "MCR.Media.VideoFrameGenerator.Class", classNameOf = CustomGenerator.class)
    })
    void replacesGeneratorThroughConfiguration() {
        assertInstanceOf(CustomGenerator.class, configuredGenerator());
    }

    private MCRVideoFrameGenerator configuredGenerator() {
        return MCRConfiguration2.getInstanceOfOrThrow(MCRVideoFrameGenerator.class, "MCR.Media.VideoFrameGenerator");
    }

    private void requireFFmpeg() {
        try {
            assumeTrue(new FFmpeg().isFFmpeg(), "FFmpeg is required for frame extraction tests");
            assumeTrue(new FFprobe().isFFprobe(), "FFprobe is required for frame extraction tests");
        } catch (IOException e) {
            assumeTrue(false, "FFmpeg is not installed: " + e.getMessage());
        }
    }

    public static class CustomGenerator implements MCRVideoFrameGenerator {

        @Override
        public BufferedImage generateFrame(Path video) {
            return new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        }
    }
}
