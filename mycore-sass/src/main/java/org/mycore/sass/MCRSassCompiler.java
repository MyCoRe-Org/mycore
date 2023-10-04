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

package org.mycore.sass;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.protobuf.ByteString;

import de.larsgrefer.sass.embedded.SassCompilationFailedException;
import de.larsgrefer.sass.embedded.SassProtocolErrorException;
import de.larsgrefer.sass.embedded.connection.CompilerConnection;
import de.larsgrefer.sass.embedded.importer.CustomImporter;
import de.larsgrefer.sass.embedded.importer.Importer;
import de.larsgrefer.sass.embedded.logging.Log4jLoggingHandler;
import de.larsgrefer.sass.embedded.logging.LoggingHandler;
import de.larsgrefer.sass.embedded.util.ProtocolUtil;
import jakarta.servlet.ServletContext;
import sass.embedded_protocol.EmbeddedSass.InboundMessage;
import sass.embedded_protocol.EmbeddedSass.InboundMessage.CanonicalizeResponse;
import sass.embedded_protocol.EmbeddedSass.InboundMessage.CompileRequest;
import sass.embedded_protocol.EmbeddedSass.InboundMessage.FunctionCallResponse;
import sass.embedded_protocol.EmbeddedSass.InboundMessage.ImportResponse;
import sass.embedded_protocol.EmbeddedSass.InboundMessage.ImportResponse.ImportSuccess;
import sass.embedded_protocol.EmbeddedSass.OutboundMessage;
import sass.embedded_protocol.EmbeddedSass.OutboundMessage.CanonicalizeRequest;
import sass.embedded_protocol.EmbeddedSass.OutboundMessage.CompileResponse;
import sass.embedded_protocol.EmbeddedSass.OutboundMessage.CompileResponse.CompileSuccess;
import sass.embedded_protocol.EmbeddedSass.OutboundMessage.FunctionCallRequest;
import sass.embedded_protocol.EmbeddedSass.OutboundMessage.ImportRequest;
import sass.embedded_protocol.EmbeddedSass.OutputStyle;
import sass.embedded_protocol.EmbeddedSass.Syntax;

/**
 * A forked version of {@link de.larsgrefer.sass.embedded.SassCompiler} to support inputs from different artifacts.
 */

class MCRSassCompiler implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ServletContext servletContext;
    private OutputStyle outputStyle;
    private boolean generateSourceMaps;
    private boolean alertColor;
    private boolean alertAscii;
    private boolean verbose;
    private boolean quietDeps;
    private boolean sourceMapIncludeSources;
    private boolean emitCharset;
    private final CompilerConnection connection;
    private static final AtomicInteger COMPILE_REQUEST_IDS = new AtomicInteger();
    private final Map<Integer, CustomImporter> customImporters;
    private LoggingHandler loggingHandler;

    MCRSassCompiler(CompilerConnection connection, ServletContext servletContext) {
        this.outputStyle = OutputStyle.EXPANDED;
        this.generateSourceMaps = false;
        this.alertColor = false;
        this.alertAscii = false;
        this.verbose = false;
        this.quietDeps = false;
        this.sourceMapIncludeSources = false;
        this.emitCharset = false;
        this.customImporters = new HashMap<>();
        this.loggingHandler = new Log4jLoggingHandler(LOGGER);
        this.connection = connection;
        this.servletContext = servletContext;
    }

    protected CompileRequest.Builder compileRequestBuilder() {
        CompileRequest.Builder builder = CompileRequest.newBuilder();

        builder.setId(Math.abs(COMPILE_REQUEST_IDS.incrementAndGet()));
        builder.setStyle(outputStyle);
        builder.setSourceMap(generateSourceMaps);

        for (Importer value : customImporters.values()) {
            CompileRequest.Importer importer
                = CompileRequest.Importer.newBuilder()
                    .setImporterId(value.getId())
                    .build();
            builder.addImporters(importer);
        }

        builder.setAlertColor(alertColor);
        builder.setAlertAscii(alertAscii);
        builder.setVerbose(verbose);
        builder.setQuietDeps(quietDeps);
        builder.setSourceMapIncludeSources(sourceMapIncludeSources);
        builder.setCharset(emitCharset);
        return builder;
    }

    public CompileSuccess compile(String realFileName)
        throws SassCompilationFailedException, IOException {
        return this.compile(Objects.requireNonNull(realFileName), this.outputStyle);
    }

    public CompileSuccess compile(String realFileName, OutputStyle outputStyle)
        throws SassCompilationFailedException, IOException {
        String ourURL = MCRResourceImporter.SASS_URL_PREFIX + realFileName;
        MCRResourceImporter importer = new MCRResourceImporter(servletContext);
        ImportSuccess importSuccess = importer.handleImport(ourURL);
        Syntax syntax = importSuccess.getSyntax();
        ByteString content = importSuccess.getContentsBytes();
        customImporters.put(importer.getId(), importer.autoCanonicalize());

        CompileRequest.StringInput build = CompileRequest.StringInput.newBuilder()
            .setUrl(ourURL)
            .setSourceBytes(content)
            .setImporter(CompileRequest.Importer.newBuilder()
                .setImporterId(importer.getId())
                .build())
            .setSyntax(syntax)
            .build();

        try {
            return compileString(build, outputStyle);
        } finally {
            customImporters.remove(importer.getId());
        }
    }

    public CompileSuccess compileString(CompileRequest.StringInput string, OutputStyle outputStyle)
        throws IOException, SassCompilationFailedException {
        CompileRequest compileRequest = this.compileRequestBuilder()
            .setString(string)
            .setStyle(Objects.requireNonNull(outputStyle))
            .build();
        return this.execCompileRequest(compileRequest);
    }

    private CompileSuccess execCompileRequest(CompileRequest compileRequest)
        throws IOException, SassCompilationFailedException {
        OutboundMessage outboundMessage = this.exec(ProtocolUtil.inboundMessage(compileRequest));
        if (!outboundMessage.hasCompileResponse()) {
            throw new IllegalStateException("No compile response");
        } else {
            CompileResponse compileResponse = outboundMessage.getCompileResponse();
            if (compileResponse.getId() != compileRequest.getId()) {
                throw new IllegalStateException(
                    String.format(Locale.ENGLISH, "Compilation ID mismatch: expected %d, but got %d",
                    compileRequest.getId(), compileResponse.getId()));
            } else if (compileResponse.hasSuccess()) {
                return compileResponse.getSuccess();
            } else if (compileResponse.hasFailure()) {
                throw new SassCompilationFailedException(compileResponse.getFailure());
            } else {
                throw new IllegalStateException("Neither success nor failure");
            }
        }
    }

    private OutboundMessage exec(InboundMessage inboundMessage) throws IOException {
        synchronized (this.connection) {
            this.connection.sendMessage(inboundMessage);

            while (true) {
                OutboundMessage outboundMessage = this.connection.readResponse();
                switch (outboundMessage.getMessageCase()) {
                    case ERROR -> throw new SassProtocolErrorException(outboundMessage.getError());
                    case COMPILE_RESPONSE, VERSION_RESPONSE -> {
                        return outboundMessage;
                    }
                    case LOG_EVENT -> this.loggingHandler.handle(outboundMessage.getLogEvent());
                    case CANONICALIZE_REQUEST
                        -> this.handleCanonicalizeRequest(outboundMessage.getCanonicalizeRequest());
                    case IMPORT_REQUEST -> this.handleImportRequest(outboundMessage.getImportRequest());
                    case FILE_IMPORT_REQUEST -> throw new IllegalStateException(
                        "No file import request supported: " + outboundMessage.getFileImportRequest().getUrl());
                    case FUNCTION_CALL_REQUEST
                        -> this.handleFunctionCallRequest(outboundMessage.getFunctionCallRequest());
                    case MESSAGE_NOT_SET -> throw new IllegalStateException("No message set");
                    default -> throw new IllegalStateException(
                        "Unknown OutboundMessage: " + outboundMessage.getMessageCase());
                }
            }
        }
    }

    private void handleImportRequest(ImportRequest importRequest) throws IOException {
        ImportResponse.Builder importResponse = ImportResponse.newBuilder()
                .setId(importRequest.getId());

        CustomImporter customImporter = customImporters.get(importRequest.getImporterId());

        try {
            ImportSuccess success = customImporter.handleImport(importRequest.getUrl());
            if (success != null) {
                importResponse.setSuccess(success);
            }
        } catch (Exception t) {
            LOGGER.debug("Failed to handle ImportRequest {}", importRequest, t);
            importResponse.setError(getErrorMessage(t));
        }

        connection.sendMessage(ProtocolUtil.inboundMessage(importResponse.build()));
    }

    private void handleCanonicalizeRequest(CanonicalizeRequest canonicalizeRequest)
        throws IOException {
        CanonicalizeResponse.Builder canonicalizeResponse = CanonicalizeResponse.newBuilder()
                .setId(canonicalizeRequest.getId());

        CustomImporter customImporter = customImporters.get(canonicalizeRequest.getImporterId());

        try {
            String canonicalize
                = customImporter.canonicalize(canonicalizeRequest.getUrl(), canonicalizeRequest.getFromImport());
            if (canonicalize != null) {
                canonicalizeResponse.setUrl(canonicalize);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to handle CanonicalizeRequest {}", canonicalizeRequest, e);
            canonicalizeResponse.setError(getErrorMessage(e));
        }

        connection.sendMessage(ProtocolUtil.inboundMessage(canonicalizeResponse.build()));
    }

    private void handleFunctionCallRequest(FunctionCallRequest functionCallRequest) throws IOException {
        FunctionCallResponse.Builder response = FunctionCallResponse.newBuilder()
                .setId(functionCallRequest.getId());

        try {
            switch (functionCallRequest.getIdentifierCase()) {
                case NAME -> throw new UnsupportedOperationException(
                    "Calling function " + functionCallRequest.getName() + " is not supported");
                case FUNCTION_ID -> throw new UnsupportedOperationException("Calling functions by ID is not supported");
                case IDENTIFIER_NOT_SET -> throw new IllegalArgumentException("FunctionCallRequest has no identifier");
                default -> throw new UnsupportedOperationException(
                    "Unsupported external function identifier case: " + functionCallRequest.getIdentifierCase());
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to handle FunctionCallRequest", e);
            response.setError(getErrorMessage(e));
        }

        connection.sendMessage(ProtocolUtil.inboundMessage(response.build()));
    }

    private String getErrorMessage(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public void close() throws IOException {
        this.connection.close();
    }

}
