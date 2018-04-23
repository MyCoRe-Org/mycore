package org.mycore.datamodel.metadata;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jdom2.Element;
import org.mycore.common.MCRException;

/**
 * Stores spatial information for geographic references. The latitude longitude values are stored in an array list,
 * where two BigDecimal build a point.
 *
 * @author Matthias Eichner
 */
public class MCRMetaSpatial extends MCRMetaDefault {

    private List<BigDecimal> data;

    public MCRMetaSpatial() {
        super();
        this.data = new ArrayList<>();
    }

    public MCRMetaSpatial(String subtag, String defaultLanguage, String type, Integer inherited) throws MCRException {
        super(subtag, defaultLanguage, type, inherited);
        this.data = new ArrayList<>();
    }

    /**
     * Returns the spatial data. Two entries build a point. The first is always the latitude and the second one
     * is always the longitude value.
     *
     * @return list of the spatial data
     */
    public List<BigDecimal> getData() {
        return this.data;
    }

    public void setData(List<BigDecimal> data) {
        this.data = data;
    }

    /**
     * Adds a new point to the data.
     *
     * @param lat the latitude value
     * @param lng the longitude value
     */
    public void add(BigDecimal lat, BigDecimal lng) {
        this.data.add(lat);
        this.data.add(lng);
    }

    @Override
    public void setFromDOM(Element element) throws MCRException {
        super.setFromDOM(element);
        String textData = element.getText();
        String[] splitData = textData.split(",");
        if (splitData.length % 2 != 0) {
            throw new MCRException(String.format(Locale.ROOT,
                    "Unable to parse MCRMetaSpatial cause text data '%s' contains invalid content", textData));
        }
        try {
            Arrays.stream(splitData).map(BigDecimal::new).forEach(this.data::add);
        } catch (NumberFormatException nfe) {
            throw new MCRException(String.format(Locale.ROOT,
                    "Unable to parse MCRMetaSpatial cause text data '%s' contains invalid content", textData), nfe);
        }
    }

    @Override
    public Element createXML() throws MCRException {
        Element element = super.createXML();
        return element.setText(this.data.stream().map(BigDecimal::toPlainString).collect(Collectors.joining(",")));
    }

    /**
     * Creates the JSON representation. Extends the {@link MCRMetaDefault#createJSON()} method
     * with the following data.
     *
     * <pre>
     *   {
     *     data: [50.92878, 11.5899]
     *   }
     * </pre>
     *
     */
    @Override
    public JsonObject createJSON() {
        JsonObject json = super.createJSON();
        JsonArray dataArray = new JsonArray();
        this.data.forEach(dataArray::add);
        json.add("data", dataArray);
        return json;
    }

    @Override
    public void validate() throws MCRException {
        super.validate();
        if (this.data.isEmpty()) {
            throw new MCRException("spatial list should contain content");
        }
        if (this.data.size() % 2 != 0) {
            throw new MCRException(
                    String.format(Locale.ROOT, "spatial list content '%s' is uneven", this.data.toString()));
        }
    }

    @Override
    public MCRMetaSpatial clone() {
        MCRMetaSpatial newSpatial = new MCRMetaSpatial(getSubTag(), getLang(), getType(), getInherited());
        newSpatial.setData(new ArrayList<>(this.data));
        return newSpatial;
    }

}
