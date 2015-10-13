package org.mycore.common;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

/**
 * Simple utility class to compare jdom content for equality. Deep equal
 * checks are supported. This class does not check or care for element
 * or attribute order. Comment, DocType, EntityRef and ProcessingInstruction's
 * are not supported. Supported are Elements, Attributes and Text nodes.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRJDOMEqualityComparator {

    /**
     * The basic filter accepts just Elements and well formed text nodes.
     * 
     * @param content the content to filter
     * @return true if the content is accepted, false otherwise
     */
    public static boolean basicContentFilter(Content content) {
        if (content instanceof Text) {
            Text textNode = (Text) content;
            String text = textNode.getTextNormalize();
            return !text.isEmpty();
        }
        if (content instanceof Element) {
            return true;
        }
        return false;
    }

    /**
     * Same as {@link #equals(List, List, Function)} but with basic content filter
     * {@link #basicContentFilter(Content)}.
     * 
     * @param l1 first list
     * @param l2 second list
     * @return true of both are equal
     */
    public static boolean equals(List<Content> l1, List<Content> l2) {
        Function<Content, Boolean> basicContentFilter = content -> basicContentFilter(content);
        return equals(l1, l2, basicContentFilter);
    }

    /**
     * Checks if both list's contains the same content. The order is not
     * important. Lists are equal if they have the same size() and if
     * each content in one list is contained within the other
     * ({@link #contains(Content, List, Function)}).
     * 
     * @param l1 first list
     * @param l2 second list
     * @param contentFilter Filter to remove unnecessary content
     *          (for example @see {@link #basicContentFilter(Content)}).
     *          If you don't want to remove any content use null here.
     * @return true of both are equal
     */
    public static boolean equals(List<Content> l1, List<Content> l2, Function<Content, Boolean> contentFilter) {
        if (l1 == null || l2 == null) {
            return false;
        }
        if (contentFilter != null) {
            l1 = l1.stream().filter(c -> contentFilter.apply(c)).collect(Collectors.toList());
            l2 = l2.stream().filter(c -> contentFilter.apply(c)).collect(Collectors.toList());
        }
        if (l1.size() != l2.size()) {
            return false;
        }
        for (Content c1 : l1) {
            if (!contains(c1, l2, contentFilter)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if both attribute lists are equal. Attribute lists are equal
     * when they have the same size and if each attribute in one list is 
     * contained within the other ({@link #contains(Attribute, List)})
     * 
     * @param l1 first attribute list
     * @param l2 second attribute list
     * @return true of both lists are equal
     */
    public static boolean equalAttributes(List<Attribute> l1, List<Attribute> l2) {
        if (l1 == null || l2 == null) {
            return false;
        }
        if (l1.size() != l2.size()) {
            return false;
        }
        for (Attribute a1 : l1) {
            if (!contains(a1, l2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if both content nodes are equal.
     * 
     * @param t1 first content node
     * @param t2 second content node
     * @param contentFilter Filter to remove unnecessary content
     *          (for example @see {@link #basicContentFilter(Content)}).
     *          If you don't want to remove any content use null here
     * @return true when equals
     */
    public static boolean equals(Content c1, Content c2, Function<Content, Boolean> contentFilter) {
        if (c1 == null || c2 == null) {
            return false;
        }
        if (!c1.getClass().equals(c2.getClass())) {
            return false;
        }
        if (c1 instanceof Element && c2 instanceof Element) {
            return equals((Element) c1, (Element) c2, contentFilter);
        }
        if (c2 instanceof Text && c2 instanceof Text) {
            return equals((Text) c1, (Text) c2);
        }
        return c1.equals(c2);
    }

    /**
     * Check if both elements are equal. Elements are equal when they have
     * the same name, namespace, equal content (@see {@link #equals(Content, Content)}
     * and equal attributes (@see {@link #equalAttributes(List, List)}.
     * 
     * @param e1 first element
     * @param e2 second element
     * @param contentFilter Filter to remove unnecessary content
     *          (for example @see {@link #basicContentFilter(Content)}).
     *          If you don't want to remove any content use null here.
     * @return true if both elements are equal
     */
    public static boolean equals(Element e1, Element e2, Function<Content, Boolean> contentFilter) {
        if (e1 == null || e2 == null) {
            return false;
        }
        if (!e1.getName().equals(e2.getName())) {
            return false;
        }
        if (!e1.getNamespace().equals(e2.getNamespace())) {
            return false;
        }
        if (!equals(e1.getContent(), e2.getContent(), contentFilter)) {
            return false;
        }
        if (!equalAttributes(e1.getAttributes(), e2.getAttributes())) {
            return false;
        }
        return true;
    }

    /**
     * Check if both text nodes are equal.
     * 
     * @param t1 first text node
     * @param t2 second text node
     * @return true when equals
     */
    public static boolean equals(Text t1, Text t2) {
        if (t1 == null || t2 == null) {
            return false;
        }
        return t1.getText().equals(t2.getText());
    }

    /**
     * Checks if both attributes are equal. Attributes are equal if they
     * both have the same name, namespace and value.
     * 
     * @param a1 first attribute
     * @param a2 second attribute
     * @return true when equals
     */
    public static boolean equals(Attribute a1, Attribute a2) {
        if (a1 == null || a2 == null) {
            return false;
        }
        if (!a1.getName().equals(a2.getName())) {
            return false;
        }
        if (!a1.getNamespace().equals(a2.getNamespace())) {
            return false;
        }
        if (!a1.getValue().equals(a2.getValue())) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the given content is in the given list.
     * 
     * @param content content to look up
     * @param list where to look
     * @param contentFilter Filter to remove unnecessary content (for example
     *          @see {@link #basicContentFilter(Content)}). If you don't want
     *          to remove any content use null here
     * @return true if the content is in the list
     */
    public static boolean contains(Content content, List<Content> list, Function<Content, Boolean> contentFilter) {
        if (contentFilter != null) {
            list = list.stream().filter(c -> contentFilter.apply(c)).collect(Collectors.toList());
        }
        for (Content compareContent : list) {
            if (equals(content, compareContent, contentFilter)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given attribute is in the given list.
     * 
     * @param attr attribute to look up
     * @param list where to look
     * @return true if the attribute is in the list
     */
    public static boolean contains(Attribute attr, List<Attribute> list) {
        for (Attribute compareAttr : list) {
            if (equals(attr, compareAttr)) {
                return true;
            }
        }
        return false;
    }

}
