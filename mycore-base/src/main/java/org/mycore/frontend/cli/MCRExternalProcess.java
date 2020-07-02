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

package org.mycore.frontend.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.content.MCRByteContent;
import org.mycore.common.content.MCRContent;

public class MCRExternalProcess {

    private static final Logger LOGGER = LogManager.getLogger();

    private final InputStream stdin;

    private String[] command;

    private int exitValue;

    private CompletableFuture<MCRByteContent> output;

    private CompletableFuture<MCRByteContent> errors;

    public MCRExternalProcess(String... command) {
        this(null, command);
    }

    public MCRExternalProcess(String command) {
        this(command.split("\\s+"));
    }

    public MCRExternalProcess(InputStream stdin, String... command) {
        this.stdin = stdin;
        this.command = command;
    }

    public int run() throws IOException, InterruptedException {

        LOGGER.debug(() -> Stream.of(command)
            .collect(Collectors.joining(" ")));

        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = pb.start();
        CompletableFuture<Void> input = MCRStreamSucker.writeAllBytes(stdin, p);
        output = MCRStreamSucker.readAllBytesAsync(p.getInputStream()).thenApply(MCRStreamSucker::toContent);
        errors = MCRStreamSucker.readAllBytesAsync(p.getErrorStream()).thenApply(MCRStreamSucker::toContent);
        try {
            exitValue = p.waitFor();
        } catch (InterruptedException e) {
            p.destroy();
            throw e;
        }
        CompletableFuture.allOf(input, output, errors)
            .whenComplete((Void, ex) -> MCRStreamSucker.destroyProcess(p, ex));
        return exitValue;
    }

    public int getExitValue() {
        return exitValue;
    }

    public String getErrors() {
        return new String(errors.join().asByteArray(), Charset.defaultCharset());
    }

    public MCRContent getOutput() throws IOException {
        return output.join();
    }

    private static class MCRStreamSucker {

        private static final Logger LOGGER = LogManager.getLogger();

        private static CompletableFuture<byte[]> readAllBytesAsync(InputStream is) {
            return CompletableFuture.supplyAsync(() -> readAllBytes(is));
        }

        private static byte[] readAllBytes(InputStream is) throws UncheckedIOException {
            try {
                return is.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static MCRByteContent toContent(byte[] data) {
            return new MCRByteContent(data, System.currentTimeMillis());
        }

        private static void destroyProcess(Process p, Throwable ex) {
            if (ex != null) {
                LOGGER.warn("Error while sucking stdout or stderr streams.", ex);
            }
            LOGGER.debug("Destroy process {}", p.pid());
            p.destroy();
        }

        public static CompletableFuture<Void> writeAllBytes(InputStream stdin, Process p) throws UncheckedIOException {
            return Optional.ofNullable(stdin)
                .map(in -> CompletableFuture.runAsync(() -> {
                    try {
                        try (OutputStream stdinPipe = p.getOutputStream()) {
                            in.transferTo(stdinPipe);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }))
                .orElseGet(CompletableFuture::allOf);
        }
    }

}
