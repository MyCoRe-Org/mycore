package org.mycore.sass;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
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
import de.larsgrefer.sass.embedded.importer.FileImporter;
import de.larsgrefer.sass.embedded.importer.Importer;
import de.larsgrefer.sass.embedded.logging.Log4jLoggingHandler;
import de.larsgrefer.sass.embedded.logging.LoggingHandler;
import de.larsgrefer.sass.embedded.util.ProtocolUtil;
import de.larsgrefer.sass.embedded.util.SyntaxUtil;
import sass.embedded_protocol.EmbeddedSass;

/**
 * A forked version of {@link de.larsgrefer.sass.embedded.SassCompiler} to support inputs from different artifacts.
 *
 * @see <a href=”https://github.com/sass/embedded-protocol/issues/40#issuecomment-750500809">sass/embedded-protocol#40</a>
 */
/*
Copy of original message for reference:

The way the protocol handles relative imports is somewhat different than the way LibSass has historically done so (but
more in line with how Ruby Sass did originally and Dart Sass does now). Rather than passing the previous import URL to
the host and relying on the host to handle relative resolution in some custom way, the compiler relies on URLs' native
notion of "relative resolution" handle relative imports consistently across all importers.

To begin with, every Sass stylesheet loaded by @import, @use, etc has a "canonical URL", an absolute URL that uniquely
identifies that stylesheet. Stylesheets loaded from the filesystem are effectively loaded using a built-in importer
that canonicalizes to file: URLs and loads those URLs from the physical filesystem.

To resolve an @import/@use/etc that appears within a given stylesheet, the compiler first resolves it relative to that
stylesheet's canonical URL and checks whether the importer that loaded that stylesheet can load the new URL. If not,
the compiler passes it to the full chain of importers and load paths to try to canonicalize it. If none of them can,
the compiler throws an error.

For example, let's say you tell the compiler to compile the file input.scss with contents @use "colors". Here's what
happens:

    The compiler canonicalizes the file using the built-in filesystem importer (that's what happens if you specify a
path input). This importer converts input.scss to the canonical URL file:///path/to/working/directory/input.scss, then
loads that URL from disk and returns the file's contents.

    The compiler then sees @use "colors". It first checks if colors can be resolved relative to input.scss. The
resolved URL is file:///path/to/working/directory/colors (by standard URL resolution), so it checks whether that can be
canonicalized by the filesystem importer (because that's the importer that loaded input.scss). If so, the canonical URL
is sent back as an ImportRequest and that's how the URL is loaded.

    If the filesystem importer can't canonicalize file:///path/to/working/directory/colors (probably because no
stylesheet exists with that name), the original unresolved URL colors is passed on to any other importers that are
registered. These importers don't have access to the original stylesheet's URL because the way a URL is loaded
shouldn't be sensitive to the context in which it appears, except for the specific case of relative URL resolution
which is well-understood and consistent.

For your use-case you can:

    Use a CompileRequest with input.string.importer.importer_id set to your importer's ID and input.string.url set to
the absolute file: URL of the entrypoint stylesheet. Doing this rather than using input.path will tell the compiler to
use your custom importer rather than the default filesystem importer, so your importer will be called to handle
relative URLs.

    When you receive a CanonicalizeRequest with an absolute URL, slice off the union FS root of the url and check the
relative result against all the roots, then return whichever one has a file that matches.

    When you receive an ImportRequest, just load that file from the filesystem.

You could also use a file_importer_id to avoid the extra round-trip of the ImportRequest, once embedded Dart Sass has
implemented that.

    I reckon there are a couple of host/compiler typos in that paragraph: it's describing for context how the compiler
implements @import, I think it should read:

        When loading a URL, the host compiler must first try resolving that URL relative to the canonical URL of the
current file, and canonicalizing the result using the importer that loaded the current file. If this returns null, the
host compiler must then try canonicalizing the original URL with each importer in order until one returns something
other than null. That is the result of the import.

    All we need in the host is the Url part -- same as the Sass_Importer_Fn url param if you're porting from libsass,
except that one call is now split into two RPCs canonicalize + import.
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
        } catch (Exception t) {
            LOGGER.debug("Failed to execute FileImportRequest {}", fileImportRequest, t);
            fileImportResponse.setError(getErrorMessage(t));
        }

        connection.sendMessage(ProtocolUtil.inboundMessage(fileImportResponse.build()));
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
        } catch (Exception t) {
            LOGGER.debug("Failed to handle ImportRequest {}", importRequest, t);
            importResponse.setError(getErrorMessage(t));
        }

        connection.sendMessage(ProtocolUtil.inboundMessage(importResponse.build()));
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
        } catch (Exception e) {
            LOGGER.debug("Failed to handle CanonicalizeRequest {}", canonicalizeRequest, e);
            canonicalizeResponse.setError(getErrorMessage(e));
        }

        connection.sendMessage(ProtocolUtil.inboundMessage(canonicalizeResponse.build()));
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

    public EmbeddedSass.OutputStyle getOutputStyle() {
        return this.outputStyle;
    }

    private static class NullImporter extends CustomImporter {

        @Override
        public String canonicalize(String url, boolean fromImport) {
            return null;
        }

        @Override
        public EmbeddedSass.InboundMessage.ImportResponse.ImportSuccess handleImport(String url) {
            return null;
        }
    }

}
