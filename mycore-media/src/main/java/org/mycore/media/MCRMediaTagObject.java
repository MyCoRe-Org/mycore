/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.media;

import org.jdom2.Element;

/**
 * This Object holds various media tags from parsed media files.
 * 
 * @author René Adler (Eagle)
 *
 */
public class MCRMediaTagObject implements Cloneable {
    //titles
    protected String collection;

    protected String season;

    protected String title;

    protected String album;

    protected String trackName;

    //relation with other composants
    protected int trackPosition;

    //people
    protected String author;

    protected String creator;

    protected String performer;

    protected String performerURL;

    protected String producer;

    protected String accompaniment;

    protected String composer;

    protected String arranger;

    protected String publisher;

    protected String publisherURL;

    //classification
    protected String contentType;

    protected String subject;

    protected String summary;

    protected String description;

    protected String keywords;

    protected String period;

    protected String language;

    //Temporal Information
    protected String releaseDate;

    protected String recordDate;

    //personal information
    protected String genre;

    protected String comment;

    @Override
    public String toString() {
        String out = "";

        if (title != null)
            out += "Title            : " + title + "\n";
        if (album != null)
            out += "Album            : " + album + "\n";
        if (trackName != null)
            out += "Track            : " + trackName + "\n";
        if (trackPosition != 0)
            out += "Position         : " + trackPosition + "\n";
        if (performer != null)
            out += "Performer        : " + performer + "\n";
        if (recordDate != null)
            out += "Record Date      : " + recordDate + "\n";
        if (genre != null)
            out += "Genre            : " + genre + "\n";
        if (comment != null)
            out += "Comment          : " + comment + "\n";

        return out;
    }

    /**
     * Output metadata as XML.
     */
    public Element toXML() {
        Element xml = new Element("tags");

        MCRMediaObject.createElement(xml, "collection", collection);
        MCRMediaObject.createElement(xml, "season", season);
        MCRMediaObject.createElement(xml, "title", title);
        MCRMediaObject.createElement(xml, "album", album);
        MCRMediaObject.createElement(xml, "track", trackName);
        MCRMediaObject.createElement(xml, "track/@position", String.valueOf(trackPosition));
        MCRMediaObject.createElement(xml, "performer", performer);
        MCRMediaObject.createElement(xml, "performer/@url", performerURL);
        MCRMediaObject.createElement(xml, "accompaniment", accompaniment);
        MCRMediaObject.createElement(xml, "composer", composer);
        MCRMediaObject.createElement(xml, "arranger", arranger);
        MCRMediaObject.createElement(xml, "publisher", publisher);
        MCRMediaObject.createElement(xml, "publisher/@url", publisherURL);
        MCRMediaObject.createElement(xml, "contentType", contentType);
        MCRMediaObject.createElement(xml, "subject", subject);
        MCRMediaObject.createElement(xml, "description", description);
        MCRMediaObject.createElement(xml, "keywords", keywords);
        MCRMediaObject.createElement(xml, "period", period);
        MCRMediaObject.createElement(xml, "language", language);
        MCRMediaObject.createElement(xml, "genre", genre);
        MCRMediaObject.createElement(xml, "comment", comment);

        MCRMediaObject.createElement(xml, "recordDate", recordDate);
        MCRMediaObject.createElement(xml, "releaseDate", releaseDate);

        return xml;
    }

    public static MCRMediaTagObject buildFromXML(Element xml) {
        MCRMediaTagObject tags = new MCRMediaTagObject();

        tags.collection = MCRMediaObject.getXMLValue(xml, "collection");
        tags.season = MCRMediaObject.getXMLValue(xml, "season");
        tags.album = MCRMediaObject.getXMLValue(xml, "album");
        tags.trackName = MCRMediaObject.getXMLValue(xml, "track");
        tags.trackPosition = Integer.parseInt(MCRMediaObject.getXMLValue(xml, "track/@position", "0"));
        tags.performer = MCRMediaObject.getXMLValue(xml, "performer");
        tags.performerURL = MCRMediaObject.getXMLValue(xml, "performer/@url");
        tags.accompaniment = MCRMediaObject.getXMLValue(xml, "accompaniment");
        tags.composer = MCRMediaObject.getXMLValue(xml, "composer");
        tags.arranger = MCRMediaObject.getXMLValue(xml, "arranger");
        tags.publisher = MCRMediaObject.getXMLValue(xml, "publisher");
        tags.publisherURL = MCRMediaObject.getXMLValue(xml, "publisher/@url");
        tags.contentType = MCRMediaObject.getXMLValue(xml, "contentType");
        tags.subject = MCRMediaObject.getXMLValue(xml, "subject");
        tags.description = MCRMediaObject.getXMLValue(xml, "description");
        tags.keywords = MCRMediaObject.getXMLValue(xml, "keywords");
        tags.period = MCRMediaObject.getXMLValue(xml, "period");
        tags.language = MCRMediaObject.getXMLValue(xml, "language");
        tags.genre = MCRMediaObject.getXMLValue(xml, "genre");
        tags.comment = MCRMediaObject.getXMLValue(xml, "comment");

        tags.recordDate = MCRMediaObject.getXMLValue(xml, "recordDate");
        tags.releaseDate = MCRMediaObject.getXMLValue(xml, "releaseDate");

        return tags;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
