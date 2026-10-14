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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRProperty;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.shared.CodecType;

/**
 * Extracts an embedded cover or a representative video frame at full resolution.
 * Prefers a non-blank opening frame; otherwise uses FFmpeg's thumbnail filter after seeking.
 */
public class MCRFFmpegVideoFrameGenerator implements MCRVideoFrameGenerator {

    private static final Logger LOGGER = LogManager.getLogger();

    private String executable = "ffmpeg";

    private String probeExecutable = "ffprobe";

    @MCRProperty(name = "Executable", required = false)
    public void setExecutable(String executable) {
        this.executable = executable;
    }

    @MCRProperty(name = "ProbeExecutable", required = false)
    public void setProbeExecutable(String probeExecutable) {
        this.probeExecutable = probeExecutable;
    }

    @Override
    public boolean checkRequirements(Path video) {
        try {
            return video != null && Files.isRegularFile(video) && Files.isReadable(video)
                && new FFmpeg(executable).isFFmpeg() && new FFprobe(probeExecutable).isFFprobe();
        } catch (IOException e) {
            LOGGER.warn("Video frame generation is unavailable for {} because FFmpeg or FFprobe cannot be used",
                video, e);
            return false;
        }
    }

    @Override
    public BufferedImage generateFrame(Path video) throws IOException {
        // FFmpeg needs a local file, while derivates may use another NIO file system.
        Path input = Files.createTempFile("MyCoRe-Video-", ".video");
        try {
            Files.copy(video, input, StandardCopyOption.REPLACE_EXISTING);
            Path output = Files.createTempFile("MyCoRe-Video-Frame-", ".png");
            try {
                FFmpeg ffmpeg = new FFmpeg(executable);
                FFmpegProbeResult probe = new FFprobe(probeExecutable).probe(input.toString());
                FFmpegStream stream = selectStream(probe);
                if (stream == null) {
                    throw new IOException("No video stream or embedded cover found in " + video);
                }
                if (!isCover(stream)) {
                    BufferedImage firstFrame = extractFrame(ffmpeg, input, output, stream, 0, false);
                    if (firstFrame != null && !isBlank(firstFrame)) {
                        return firstFrame;
                    }
                }
                long offset = isCover(stream) ? 0 : startOffset(probe, stream);
                BufferedImage frame = extractFrame(ffmpeg, input, output, stream, offset, !isCover(stream));
                // Very short videos may have no frame after seeking. Retry from the beginning.
                if (frame == null && offset > 0) {
                    frame = extractFrame(ffmpeg, input, output, stream, 0, !isCover(stream));
                }
                if (frame == null) {
                    throw new IOException("FFmpeg produced no readable video frame for " + video);
                }
                return frame;
            } finally {
                Files.deleteIfExists(output);
            }
        } finally {
            Files.deleteIfExists(input);
        }
    }

    /** Rejects almost entirely black or white opening frames, without resizing the image. */
    private boolean isBlank(BufferedImage image) {
        int black = 0;
        int white = 0;
        int samples = 0;
        int stepX = Math.max(1, image.getWidth() / 100);
        int stepY = Math.max(1, image.getHeight() / 100);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                if (red <= 16 && green <= 16 && blue <= 16) {
                    black++;
                }
                if (red >= 239 && green >= 239 && blue >= 239) {
                    white++;
                }
                samples++;
            }
        }
        return black >= samples * 0.98 || white >= samples * 0.98;
    }

    private FFmpegStream selectStream(FFmpegProbeResult probe) {
        return probe.getStreams().stream()
            .filter(stream -> stream.codec_type == CodecType.VIDEO && isCover(stream))
            .findFirst()
            .orElseGet(() -> probe.getStreams().stream()
                .filter(stream -> stream.codec_type == CodecType.VIDEO)
                .findFirst().orElse(null));
    }

    private boolean isCover(FFmpegStream stream) {
        return stream.disposition != null && stream.disposition.attached_pic;
    }

    private long startOffset(FFmpegProbeResult probe, FFmpegStream stream) {
        double duration = stream.duration;
        if (!Double.isFinite(duration) || duration <= 0) {
            duration = probe.getFormat() == null ? 0 : probe.getFormat().duration;
        }
        // Use 10% for short videos, at most 10 seconds; unknown duration starts at zero.
        return Double.isFinite(duration) && duration > 0 ? (long) (Math.min(10, duration * 0.1) * 1000) : 0;
    }

    private BufferedImage extractFrame(FFmpeg ffmpeg, Path input, Path output, FFmpegStream stream,
        long offset, boolean representative) throws IOException {
        Files.deleteIfExists(output);
        FFmpegBuilder builder = new FFmpegBuilder();
        builder.setInput(input.toString()).setStartOffset(offset, TimeUnit.MILLISECONDS);
        var result = builder.overrideOutputFiles(true)
            .addOutput(output.toString())
            .setFormat("image2")
            .setVideoCodec("png")
            .setFrames(1)
            .disableAudio()
            .disableSubtitle()
            .addExtraArgs("-map", "0:" + stream.index);
        if (representative) {
            result.addExtraArgs("-vf", "thumbnail=100");
        }
        ffmpeg.run(builder);
        return Files.isRegularFile(output) && Files.size(output) > 0 ? ImageIO.read(output.toFile()) : null;
    }

}
