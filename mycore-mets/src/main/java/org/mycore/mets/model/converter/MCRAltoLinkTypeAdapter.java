package org.mycore.mets.model.converter;

import java.io.IOException;

import org.mycore.common.MCRException;
import org.mycore.mets.model.simple.MCRMetsAltoLink;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

/**
 * This is a helper class to help GSON to convert simple model to JSON.
 *
 * @author Sebastian Hofmann(mcrshofm)
 */
public class MCRAltoLinkTypeAdapter extends TypeAdapter<MCRMetsAltoLink> {

    @Override
    public void write(JsonWriter jsonWriter, MCRMetsAltoLink altoLink) throws IOException {
        jsonWriter.beginObject();
        jsonWriter.name("altoFile").value(altoLink.getFile().getId());
        jsonWriter.name("begin").value(altoLink.getBegin());
        jsonWriter.name("end").value(altoLink.getEnd());
        jsonWriter.endObject();
    }

    @Override
    public MCRMetsAltoLink read(JsonReader jsonReader) throws IOException {
        String fileID = null;
        String begin = null;
        String end = null;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "altoFile":
                    fileID = jsonReader.nextString();
                    break;
                case "begin":
                    begin = jsonReader.nextString();
                    break;
                case "end":
                    end = jsonReader.nextString();
                    break;
            }
        }
        jsonReader.endObject();

        if (fileID == null || begin == null || end == null) {
            throw new MCRException("Cannot read MCRMetsAltoLink! FileID && begin && end expected!");
        }
        return new MCRAltoLinkPlaceHolder(fileID, begin, end);
    }

    /**
     * Created by sebastian on 25.11.15.
     */
    public static class MCRAltoLinkPlaceHolder extends MCRMetsAltoLink {
        public static final String PLACEHOLDER_EXCEPTION_MESSAGE = "this is a placeholder class";

        private String fileID;

        public MCRAltoLinkPlaceHolder(String fileID, String begin, String end) {
            super(null, begin, end);
            this.fileID = fileID;
        }

        public String getFileID() {
            return fileID;
        }

        public void setFileID(String fileID) {
            this.fileID = fileID;
        }
    }
}
