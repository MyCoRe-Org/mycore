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
import org.mycore.datamodel.common.MCRMetadataVersionType;

/**
 * A pruner that combines and compares revisions, based on an XPath expression, to decide which to keep and which to
 * discard.
 * The XPath expression marks content that should be ignored when comparing two revisions and when the content is equal,
 * the revisions are merged. There is a property NeedSameAuthor that can be set to true to require that the revisions
 * have the same author. The properties FirstAuthorWins and FirstDateWins can be set to true to decide which revision
 * metadata should be used in the merged revision.
 */
public class MCROCFLCombineIgnoreXPathPruner extends MCROCFLCombineComparePruner
    implements MCROCFLCombineComparePruner.RevisionMergeDecider {

    private XPathExpression<Content> xpath;

    private boolean firstAuthorWins;

    private boolean firstDateWins;

    private boolean needSameAuthor;

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

    public boolean isFirstAuthorWins() {
        return firstAuthorWins;
    }

    @MCRProperty(name = "FirstAuthorWins", required = true)
    public void setFirstAuthorWins(String firstAuthorWins) {
        this.firstAuthorWins = Boolean.parseBoolean(firstAuthorWins);
    }

    public void setFirstAuthorWins(boolean firstAuthorWins) {
        this.firstAuthorWins = firstAuthorWins;
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

    @MCRProperty(name = "NeedSameAuthor", required = true)
    public void setNeedSameAuthor(String needSameAuthor) {
        this.needSameAuthor = Boolean.parseBoolean(needSameAuthor);
    }

    public void setNeedSameAuthor(boolean needSameAuthor) {
        this.needSameAuthor = needSameAuthor;
    }

    public boolean needSameAuthor() {
        return needSameAuthor;
    }

    @Override
    public MCROCFLRevision buildMergedRevision(MCROCFLRevision current, MCROCFLRevision next, Document currentDocument,
        Document nextDocument) {
        MCRMetadataVersionType type = current.type();
        String user = chooseUser(current, next);
        Date date = chooseDate(current, next);

        if (type == MCRMetadataVersionType.DELETED) {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
        return new MCROCFLRevision(type, next.contentSupplier(), user, date, next.objectID());

    }

    public String chooseUser(MCROCFLRevision current, MCROCFLRevision next) {
        if (isFirstAuthorWins()) {
            return current.user();
        } else {
            return next.user();
        }
    }

    public Date chooseDate(MCROCFLRevision current, MCROCFLRevision next) {
        if (isFirstDateWins()) {
            return current.date();
        } else {
            return next.date();
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
    public boolean shouldMerge(MCROCFLRevision revision1, Document document1, MCROCFLRevision revision2,
        Document document2) {
        if (needSameAuthor() && !Objects.equals(revision1.user(), revision2.user())) {
            return false;
        }

        Document document1Clone = document1.clone();
        Document document2Clone = document2.clone();

        pruneXPathMatches(document1Clone);
        pruneXPathMatches(document2Clone);

        return MCRXMLHelper.deepEqual(document1Clone, document2Clone);
    }

    @Override
    public String toString() {
        return "MCROCFLCombineIgnoreXPathPruner{" +
                "xpath=" + xpath.getExpression() +
            ", firstAuthorWins=" + firstAuthorWins +
                ", firstDateWins=" + firstDateWins +
            ", needSameAuthor=" + needSameAuthor +
                '}';
    }
}
