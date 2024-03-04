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

package org.mycore.ocfl.metadata.migration;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Parent;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.mycore.common.MCRConstants;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.xml.MCRXMLHelper;

/**
 * A pruner that combines and compares revisions, based on an XPath expression, to decide which to keep and which to
 * discard.
 * The XPath expression marks content that should be ignored when comparing two revisions and when the content is equal,
 * the revisions are merged. There is a property NeedSameAutor that can be set to true to require that the revisions
 * have the same author. The properties FirstAutorWins and FirstDateWins can be set to true to decide which revision
 * metadata should be used in the merged revision.
 */
public class MCROCFLCombineIgnoreXPathPruner extends MCROCFLCombineComparePruner
    implements MCROCFLCombineComparePruner.RevisionMergeDecider {

    private XPathExpression<Content> xpath;

    private boolean firstAutorWins;

    private boolean firstDateWins;

    private boolean needSameAutor;

    public XPathExpression<Content> getXpath() {
        return xpath;
    }

    @MCRProperty(name = "XPath", required = true)
    public void setXpath(String xpath) {
        this.xpath = MCRConstants.XPATH_FACTORY.compile(xpath, Filters.content(), null,
                MCRConstants.getStandardNamespaces());
    }

    public void setXpath(XPathExpression<Content> xpath) {
        this.xpath = xpath;
    }

    public boolean isFirstAutorWins() {
        return firstAutorWins;
    }

    @MCRProperty(name = "FirstAutorWins", required = true)
    public void setFirstAutorWins(String firstAutorWins) {
        this.firstAutorWins = Boolean.parseBoolean(firstAutorWins);
    }

    public void setFirstAutorWins(boolean firstAutorWins) {
        this.firstAutorWins = firstAutorWins;
    }

    public boolean isFirstDateWins() {
        return firstDateWins;
    }

    @MCRProperty(name = "FirstDateWins", required = true)
    public void setFirstDateWins(String firstDateWins) {
        this.firstDateWins = Boolean.parseBoolean(firstDateWins);
    }

    public void setFirstDateWins(boolean firstDateWins) {
        this.firstDateWins = firstDateWins;
    }

    @Override
    public RevisionMergeDecider getMergeDecider() {
        return this;
    }

    @MCRProperty(name = "NeedSameAutor", required = true)
    public void setNeedSameAutor(String needSameAutor) {
        this.needSameAutor = Boolean.parseBoolean(needSameAutor);
    }

    public void setNeedSameAutor(boolean needSameAutor) {
        this.needSameAutor = needSameAutor;
    }

    public boolean needSameAutor() {
        return needSameAutor;
    }

    @Override
    public MCROCFLRevision buildMergedRevision(MCROCFLRevision current, MCROCFLRevision next, Document currentDocument,
        Document nextDocument) {
        MCROCFLVersionType type = current.getType();
        String user = chooseUser(current, next);
        Date date = chooseDate(current, next);

        return switch (type) {
        case CREATE -> new MCROCFLCreateRevision(next.getContentSupplier(), user, date, next.getObjectID());
        case UPDATE -> new MCROCFLUpdateRevision(next.getContentSupplier(), user, date, next.getObjectID());
        default -> throw new IllegalArgumentException("Unsupported type: " + type);
        };
    }

    public String chooseUser(MCROCFLRevision current, MCROCFLRevision next) {
        if (isFirstAutorWins()) {
            return current.getUser();
        } else {
            return next.getUser();
        }
    }

    public Date chooseDate(MCROCFLRevision current, MCROCFLRevision next) {
        if (isFirstDateWins()) {
            return current.getDate();
        } else {
            return next.getDate();
        }
    }

    private void pruneXPathMatches(Document o1Clone) {
        List<Content> evaluate = getXpath().evaluate(o1Clone);
        for (Content content : new ArrayList<>(evaluate)) {
            Parent parent = content.getParent();
            if (parent != null) {
                parent.removeContent(content);
            }
        }
    }

    @Override
    public boolean shouldMerge(MCROCFLRevision r1, Document o1, MCROCFLRevision r2, Document o2) {
        if (needSameAutor() && !Objects.equals(r1.getUser(), r2.getUser())) {
            return false;
        }

        Document o1Clone = o1.clone();
        Document o2Clone = o2.clone();

        pruneXPathMatches(o1Clone);
        pruneXPathMatches(o2Clone);

        return MCRXMLHelper.deepEqual(o1Clone, o2Clone);
    }

    @Override
    public String toString() {
        return "MCROCFLCombineIgnoreXPathPruner{" +
                "xpath=" + xpath.getExpression() +
                ", firstAutorWins=" + firstAutorWins +
                ", firstDateWins=" + firstDateWins +
                ", needSameAutor=" + needSameAutor +
                '}';
    }
}
