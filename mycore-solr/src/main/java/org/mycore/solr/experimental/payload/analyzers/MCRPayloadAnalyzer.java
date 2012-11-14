package org.mycore.solr.experimental.payload.analyzers;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.WhitespaceTokenizer;
import org.apache.lucene.analysis.payloads.DelimitedPayloadTokenFilter;
import org.apache.lucene.analysis.payloads.PayloadEncoder;
import org.apache.lucene.util.Version;
import org.mycore.solr.experimental.payload.encoder.MCRCoordinatePayloadEncoder;



public class MCRPayloadAnalyzer extends Analyzer {
    private PayloadEncoder encoder;

    private Version version;

    public MCRPayloadAnalyzer(Version version) {
        this.encoder = new MCRCoordinatePayloadEncoder();
        this.version = version;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        TokenStream tokenStream = new WhitespaceTokenizer(version, reader);
        tokenStream = new LowerCaseFilter(version, tokenStream);
        tokenStream = new DelimitedPayloadTokenFilter(tokenStream, '|', encoder);
        return tokenStream;
    }
}
