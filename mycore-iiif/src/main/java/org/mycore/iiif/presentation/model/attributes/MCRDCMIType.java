package org.mycore.iiif.presentation.model.attributes;

/**
 * @see <a href="http://dublincore.org/documents/dcmi-type-vocabulary/#dcmitype-Dataset">dcmitype-Dataset</a>
 */
public enum MCRDCMIType {
    /**
     * An aggregation of resources.
     * A collection is described as a group; its parts may also be separately described.
     */
    Collection("dctypes:Collection"),
    /**
     * Data encoded in a defined structure.
     * Examples include lists, tables, and databases. A dataset may be useful for direct machine processing.
     */
    Dataset("dctypes:Dataset"),
    /**
     * A non-persistent, time-based occurrence.
     * Metadata for an event provides descriptive information that is the basis for discovery of the purpose, location,
     * duration, and responsible agents associated with an event. Examples include an exhibition, webcast, conference,
     * workshop, open day, performance, battle, trial, wedding, tea party, conflagration.
     */
    Event("dctypes:Event"),
    /**
     * A visual representation other than text.
     * Examples include images and photographs of physical objects, paintings, prints, drawings, other images and
     * graphics, animations and moving pictures, film, diagrams, maps, musical notation. Note that Image may include
     * both electronic and physical representations.
     */
    Image("dctypes:Image"),
    /**
     * A resource requiring interaction from the user to be understood, executed, or experienced.
     * Examples include forms on Web pages, applets, multimedia learning objects, chat services, or virtual reality
     * environments.
     */
    InteractiveResource("dctypes:InteractiveResource"),
    /**
     * A series of visual representations imparting an impression of motion when shown in succession.
     * Examples include animations, movies, television programs, videos, zoetropes, or visual output from a simulation.
     * Instances of the type Moving Image must also be describable as instances of the broader type Image.
     */
    MovingImage("dctypes:MovingImage"),
    /**
     * An inanimate, three-dimensional object or substance.
     * Note that digital representations of, or surrogates for, these objects should use Image, Text or one of the other
     * types.
     */
    PhysicalObject("dctypes:PhysicalObject"),
    /**
     * A system that provides one or more functions.
     * Examples include a photocopying service, a banking service, an authentication service, interlibrary loans, a
     * Z39.50 or Web server.
     */
    Service("dctypes:Service"),
    /**
     * A computer program in source or compiled form.
     * Examples include a C source file, MS-Windows .exe executable, or Perl script.
     */
    Software("dctypes:Software"),
    /**
     * A resource primarily intended to be heard.
     * Examples include a music playback file format, an audio compact disc, and recorded speech or sounds.
     */
    Sound("dctypes:Sound"),
    /**
     * A static visual representation.
     * Examples include paintings, drawings, graphic designs, plans and maps. Recommended best practice is to assign the
     * type Text to images of textual materials. Instances of the type Still Image must also be describable as instances
     * of the broader type Image.
     */
    StillImage("dctypes:StillImage"),
    /**
     * A resource consisting primarily of words for reading.
     * Examples include books, letters, dissertations, poems, newspapers, articles, archives of mailing lists. Note that
     * facsimiles or images of texts are still of the genre Text.
     */
    Text("dctypes:Text");

    private final String stringExpr;

    MCRDCMIType(String stringExpr) {
        this.stringExpr = stringExpr;
    }

    @Override
    public String toString() {
        return this.stringExpr;
    }
}
