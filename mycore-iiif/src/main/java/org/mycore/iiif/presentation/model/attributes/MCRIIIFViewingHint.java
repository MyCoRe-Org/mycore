package org.mycore.iiif.presentation.model.attributes;

import com.google.gson.annotations.SerializedName;

/**
 * A hint to the client as to the most appropriate method of displaying the resource.
 *
 * @see <a href="http://iiif.io/api/presentation/2.0/#technical-properties">IIIF-Documentation</a>
 */
public enum MCRIIIFViewingHint {

    /**
     * &quot;The canvases referenced from the resource are all individual sheets, and should not be presented in a
     * page-turning interface. Examples include a set of views of a 3 dimensional object, or a set of the front sides of
     * photographs in a collection.&quot;
     */
    @SerializedName("individuals")
    individuals,

    /**
     * &quot;The canvases represent pages in a bound volume, and should be presented in a page-turning interface if one
     * is available. The first canvas is a single view (the first recto) and thus the second canvas represents the back
     * of the object in the first canvas.&quot;
     */
    @SerializedName("paged")
    paged,

    /**
     * &quot;
     * Valid only for collections. Collections with this hint consist of multiple manifests that each form part of a
     * logical whole. Clients might render the collection as a table of contents, rather than with thumbnails. Examples
     * include multi-volume books or a set of journal issues or other serials. &quot;
     */
    @SerializedName("multi-part")
    multi_part,
    /**
     * &quot;Each canvas is the complete view of one side of a long scroll or roll and an appropriate rendering might
     * only
     * display part of the canvas at any given time rather than the entire object. &quot;
     */
    @SerializedName("continuous")
    continuous,

    /**
     * &quot;Canvases with this hint must not be presented in a page turning interface, and must be skipped over when
     * determining the page sequence. This viewing hint must be ignored if the current sequence or manifest does not
     * have the ‘paged’ viewing hint.&quot;
     */
    @SerializedName("non-paged")
    non_paged,

    /**
     * &quot;Only valid on a range. A range which has this viewingHint is the top-most node in a hierarchy of ranges
     * that
     * represents a structure to be rendered by the client to assist in navigation. For example, a table of contents
     * within a paged object, major sections of a 3d object, the textual areas within a single scroll, and so forth.
     * Other ranges that are descendants of the “top” range are the entries to be rendered in the navigation structure.
     * There may be multiple ranges marked with this hint. If so, the client should display a choice of multiple
     * structures to navigate through.&quot;
     */
    @SerializedName("top")
    top,
    /**
     * &quot;Canvases with this hint, in a sequence or manifest with the “paged” viewing hint, must be displayed by
     * themselves, as they depict both parts of the opening. If all of the canvases are like this, then page turning is
     * not possible, so simply use “individuals” instead.&quot;
     */
    @SerializedName("facing-pages")
    facing_pages
}
