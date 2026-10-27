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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.niofs.MCRPath;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.RunProcessFunction;
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

    private long timeout;

    @MCRProperty(name = "Executable", required = false)
    public void setExecutable(String executable) {
        this.executable = executable;
    }

    @MCRProperty(name = "ProbeExecutable", required = false)
    public void setProbeExecutable(String probeExecutable) {
        this.probeExecutable = probeExecutable;
    }

    /** Sets the maximum runtime in seconds of a single FFmpeg or FFprobe process. */
    @MCRProperty(name = "Timeout")
    public void setTimeout(String timeout) {
        this.timeout = Long.parseLong(timeout);
    }

    @Override
    public boolean checkRequirements(Path video) {
        try {
            return video != null && Files.isRegularFile(video) && Files.isReadable(video)
                && new FFmpeg(executable, processFunction()).isFFmpeg()
                && new FFprobe(probeExecutable, processFunction()).isFFprobe();
        } catch (IOException e) {
            LOGGER.warn("Video frame generation is unavailable for {} because FFmpeg or FFprobe cannot be used",
                video, e);
            return false;
        }
    }

    @Override
    public BufferedImage generateFrame(Path video) throws IOException {
        Path localVideo = localPath(video);
        if (localVideo != null) {
            return generateFrame(video, localVideo);
        }
        // FFmpeg needs a local file, so videos on other file systems are copied first.
        Path input = Files.createTempFile("MyCoRe-Video-", ".video");
        try {
            Files.copy(video, input, StandardCopyOption.REPLACE_EXISTING);
            return generateFrame(video, input);
        } finally {
            Files.deleteIfExists(input);
        }
    }

    /** Returns a file FFmpeg can read directly, or {@code null} if the video has to be copied. */
    private Path localPath(Path video) throws IOException {
        if (video instanceof MCRPath mcrPath) {
            try {
                Path physicalPath = mcrPath.toPhysicalPath();
                if (physicalPath != null && Files.isRegularFile(physicalPath)) {
                    return physicalPath;
                }
            } catch (UnsupportedOperationException e) {
                // OCFL stores provide no physical path.
                LOGGER.debug("No physical path for {}", video, e);
            }
            return null;
        }
        return FileSystems.getDefault().equals(video.getFileSystem()) ? video : null;
    }

    private BufferedImage generateFrame(Path video, Path input) throws IOException {
        Path output = Files.createTempFile("MyCoRe-Video-Frame-", ".png");
        try {
            FFmpeg ffmpeg = new FFmpeg(executable, processFunction());
            FFmpegProbeResult probe = new FFprobe(probeExecutable, processFunction()).probe(input.toString());
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

    private TimeoutProcessFunction processFunction() {
        return new TimeoutProcessFunction(timeout);
    }

    /** Starts processes like FFmpeg's default and kills them when they run longer than the timeout. */
    static final class TimeoutProcessFunction extends RunProcessFunction {

        private final long timeout;

        TimeoutProcessFunction(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public Process run(List<String> args) throws IOException {
            Process process = super.run(args);
            // The future completes when the process exits and fails only if the timeout elapses first.
            process.onExit()
                .orTimeout(timeout, TimeUnit.SECONDS)
                .whenComplete((finished, timeoutError) -> {
                    if (timeoutError != null) {
                        LOGGER.warn("Killing {} after {} seconds", args, timeout);
                        process.destroyForcibly();
                    }
                });
            return process;
        }
    }

}
