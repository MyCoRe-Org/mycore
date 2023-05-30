package org.mycore.sass;

import static de.larsgrefer.sass.embedded.util.ProtocolUtil.inboundMessage;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
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
import de.larsgrefer.sass.embedded.importer.FileImporter;
import de.larsgrefer.sass.embedded.importer.Importer;
import de.larsgrefer.sass.embedded.logging.Log4jLoggingHandler;
import de.larsgrefer.sass.embedded.logging.LoggingHandler;
import de.larsgrefer.sass.embedded.util.ProtocolUtil;
import de.larsgrefer.sass.embedded.util.SyntaxUtil;
import sass.embedded_protocol.EmbeddedSass;

/**
 * @see <a href=”https://github.com/sass/embedded-protocol/issues/40#issuecomment-750500809">sass/embedded-protocol#40</a>
 */
class MCRSassCompiler implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger();
    private EmbeddedSass.OutputStyle outputStyle;
    private boolean generateSourceMaps;
    private boolean alertColor;
    private boolean alertAscii;
    private boolean verbose;
    private boolean quietDeps;
    private boolean sourceMapIncludeSources;
    private boolean emitCharset;
    private final CompilerConnection connection;
    private static final AtomicInteger COMPILE_REQUEST_IDS = new AtomicInteger();
    private final Map<Integer, FileImporter> fileImporters;
    private final Map<Integer, CustomImporter> customImporters;
    private LoggingHandler loggingHandler;

    MCRSassCompiler(CompilerConnection connection) {
        this.outputStyle = EmbeddedSass.OutputStyle.EXPANDED;
        this.generateSourceMaps = false;
        this.alertColor = false;
        this.alertAscii = false;
        this.verbose = false;
        this.quietDeps = false;
        this.sourceMapIncludeSources = false;
        this.emitCharset = false;
        this.fileImporters = new HashMap<>();
        this.customImporters = new HashMap<>();
        this.loggingHandler = new Log4jLoggingHandler(LOGGER);
        this.connection = connection;
    }

    public void registerImporter(FileImporter fileImporter) {
        this.fileImporters.put(fileImporter.getId(), Objects.requireNonNull(fileImporter));
    }

    public void registerImporter(CustomImporter customImporter) {
        this.customImporters.put(customImporter.getId(), Objects.requireNonNull(customImporter));
    }

    protected EmbeddedSass.InboundMessage.CompileRequest.Builder compileRequestBuilder() {
        EmbeddedSass.InboundMessage.CompileRequest.Builder builder
            = EmbeddedSass.InboundMessage.CompileRequest.newBuilder();

        builder.setId(Math.abs(COMPILE_REQUEST_IDS.incrementAndGet()));
        builder.setStyle(outputStyle);
        builder.setSourceMap(generateSourceMaps);

        for (Importer value : customImporters.values()) {
            EmbeddedSass.InboundMessage.CompileRequest.Importer importer
                = EmbeddedSass.InboundMessage.CompileRequest.Importer.newBuilder()
                    .setImporterId(value.getId())
                    .build();
            builder.addImporters(importer);
        }

        for (Importer value : fileImporters.values()) {
            EmbeddedSass.InboundMessage.CompileRequest.Importer importer
                = EmbeddedSass.InboundMessage.CompileRequest.Importer.newBuilder()
                    .setFileImporterId(value.getId())
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

    public EmbeddedSass.OutboundMessage.CompileResponse.CompileSuccess compile(URL source)
        throws SassCompilationFailedException, IOException {
        return this.compile(Objects.requireNonNull(source), this.getOutputStyle());
    }

    public EmbeddedSass.OutboundMessage.CompileResponse.CompileSuccess compile(URL source,
        EmbeddedSass.OutputStyle outputStyle) throws SassCompilationFailedException, IOException {
        EmbeddedSass.Syntax syntax;
        ByteString content;
        URLConnection urlConnection = source.openConnection();
        try (InputStream in = urlConnection.getInputStream()) {
            content = ByteString.readFrom(in);
            syntax = SyntaxUtil.guessSyntax(urlConnection);
        }

        CustomImporter importer = new NullImporter();
        customImporters.put(importer.getId(), importer);

        EmbeddedSass.InboundMessage.CompileRequest.StringInput build
            = EmbeddedSass.InboundMessage.CompileRequest.StringInput.newBuilder()
                .setUrl(source.toString())
                .setSourceBytes(content)
                .setImporter(EmbeddedSass.InboundMessage.CompileRequest.Importer.newBuilder()
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

    public EmbeddedSass.OutboundMessage.CompileResponse.CompileSuccess compileString(
        EmbeddedSass.InboundMessage.CompileRequest.StringInput string, EmbeddedSass.OutputStyle outputStyle)
        throws IOException, SassCompilationFailedException {
        EmbeddedSass.InboundMessage.CompileRequest compileRequest
            = this.compileRequestBuilder().setString(string).setStyle(Objects.requireNonNull(outputStyle)).build();
        return this.execCompileRequest(compileRequest);
    }

    private EmbeddedSass.OutboundMessage.CompileResponse.CompileSuccess execCompileRequest(
        EmbeddedSass.InboundMessage.CompileRequest compileRequest) throws IOException, SassCompilationFailedException {
        EmbeddedSass.OutboundMessage outboundMessage = this.exec(ProtocolUtil.inboundMessage(compileRequest));
        if (!outboundMessage.hasCompileResponse()) {
            throw new IllegalStateException("No compile response");
        } else {
            EmbeddedSass.OutboundMessage.CompileResponse compileResponse = outboundMessage.getCompileResponse();
            if (compileResponse.getId() != compileRequest.getId()) {
                throw new IllegalStateException(String.format("Compilation ID mismatch: expected %d, but got %d",
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

    private EmbeddedSass.OutboundMessage exec(EmbeddedSass.InboundMessage inboundMessage) throws IOException {
        synchronized (this.connection) {
            this.connection.sendMessage(inboundMessage);

            while (true) {
                EmbeddedSass.OutboundMessage outboundMessage = this.connection.readResponse();
                switch (outboundMessage.getMessageCase()) {
                    case ERROR -> throw new SassProtocolErrorException(outboundMessage.getError());
                    case COMPILE_RESPONSE, VERSION_RESPONSE -> {
                        return outboundMessage;
                    }
                    case LOG_EVENT -> this.loggingHandler.handle(outboundMessage.getLogEvent());
                    case CANONICALIZE_REQUEST
                        -> this.handleCanonicalizeRequest(outboundMessage.getCanonicalizeRequest());
                    case IMPORT_REQUEST -> this.handleImportRequest(outboundMessage.getImportRequest());
                    case FILE_IMPORT_REQUEST -> this.handleFileImportRequest(outboundMessage.getFileImportRequest());
                    case FUNCTION_CALL_REQUEST
                        -> this.handleFunctionCallRequest(outboundMessage.getFunctionCallRequest());
                    case MESSAGE_NOT_SET -> throw new IllegalStateException("No message set");
                    default -> throw new IllegalStateException(
                        "Unknown OutboundMessage: " + outboundMessage.getMessageCase());
                }
            }
        }
    }

    private void handleFileImportRequest(EmbeddedSass.OutboundMessage.FileImportRequest fileImportRequest)
        throws IOException {
        EmbeddedSass.InboundMessage.FileImportResponse.Builder fileImportResponse
            = EmbeddedSass.InboundMessage.FileImportResponse.newBuilder()
                .setId(fileImportRequest.getId());

        FileImporter fileImporter = fileImporters.get(fileImportRequest.getImporterId());

        try {
            File file = fileImporter.handleImport(fileImportRequest.getUrl(), fileImportRequest.getFromImport());
            if (file != null) {
                fileImportResponse.setFileUrl(file.toURI().toURL().toString());
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to execute FileImportRequest {}", fileImportRequest, t);
            fileImportResponse.setError(getErrorMessage(t));
        }

        connection.sendMessage(inboundMessage(fileImportResponse.build()));
    }

    private void handleImportRequest(EmbeddedSass.OutboundMessage.ImportRequest importRequest) throws IOException {
        EmbeddedSass.InboundMessage.ImportResponse.Builder importResponse
            = EmbeddedSass.InboundMessage.ImportResponse.newBuilder()
                .setId(importRequest.getId());

        CustomImporter customImporter = customImporters.get(importRequest.getImporterId());

        try {
            EmbeddedSass.InboundMessage.ImportResponse.ImportSuccess success
                = customImporter.handleImport(importRequest.getUrl());
            if (success != null) {
                importResponse.setSuccess(success);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to handle ImportRequest {}", importRequest, t);
            importResponse.setError(getErrorMessage(t));
        }

        connection.sendMessage(inboundMessage(importResponse.build()));
    }

    private void handleCanonicalizeRequest(EmbeddedSass.OutboundMessage.CanonicalizeRequest canonicalizeRequest)
        throws IOException {
        EmbeddedSass.InboundMessage.CanonicalizeResponse.Builder canonicalizeResponse
            = EmbeddedSass.InboundMessage.CanonicalizeResponse.newBuilder()
                .setId(canonicalizeRequest.getId());

        CustomImporter customImporter = customImporters.get(canonicalizeRequest.getImporterId());

        try {
            String canonicalize
                = customImporter.canonicalize(canonicalizeRequest.getUrl(), canonicalizeRequest.getFromImport());
            if (canonicalize != null) {
                canonicalizeResponse.setUrl(canonicalize);
            }
        } catch (Throwable e) {
            LOGGER.debug("Failed to handle CanonicalizeRequest {}", canonicalizeRequest, e);
            canonicalizeResponse.setError(getErrorMessage(e));
        }

        connection.sendMessage(inboundMessage(canonicalizeResponse.build()));
    }

    private void handleFunctionCallRequest(EmbeddedSass.OutboundMessage.FunctionCallRequest functionCallRequest)
        throws IOException {
        EmbeddedSass.InboundMessage.FunctionCallResponse.Builder response
            = EmbeddedSass.InboundMessage.FunctionCallResponse.newBuilder()
                .setId(functionCallRequest.getId());

        try {
            switch (functionCallRequest.getIdentifierCase()) {
                case NAME -> throw new UnsupportedOperationException(
                    "Calling function " + functionCallRequest.getName() + " is not supported");
                case FUNCTION_ID -> throw new UnsupportedOperationException("Calling functions by ID is not supported");
                case IDENTIFIER_NOT_SET -> throw new IllegalArgumentException("FunctionCallRequest has no identifier");
            }
        } catch (Throwable e) {
            LOGGER.debug("Failed to handle FunctionCallRequest", e);
            response.setError(getErrorMessage(e));
        }

        connection.sendMessage(inboundMessage(response.build()));
    }

    private String getErrorMessage(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public void close() throws IOException {
        this.connection.close();
    }

    public EmbeddedSass.OutputStyle getOutputStyle() {
        return this.outputStyle;
    }

    private static class NullImporter extends CustomImporter {

        @Override
        public String canonicalize(String url, boolean fromImport) throws Exception {
            return null;
        }

        @Override
        public EmbeddedSass.InboundMessage.ImportResponse.ImportSuccess handleImport(String url)
            throws Exception {
            return null;
        }
    }

}
