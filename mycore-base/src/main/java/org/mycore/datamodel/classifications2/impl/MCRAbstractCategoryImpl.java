/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

package org.mycore.datamodel.classifications2.impl;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.util.concurrent.MCRReadWriteGuard;

/**
 * @author Thomas Scheffler (yagee)
 *
 * @since 2.0
 */
public abstract class MCRAbstractCategoryImpl implements MCRCategory {

    protected final MCRReadWriteGuard childGuard = new MCRReadWriteGuard();

    protected MCRCategory root;

    protected MCRCategory parent;

    protected SortedSet<MCRLabel> labels;

    protected List<MCRCategory> children;

    private MCRCategoryID id;

    private URI uri;

    private String defaultLang;

    private static final Set<String> LANGUAGES;

    static {
        LANGUAGES = new HashSet<>(MCRConfiguration2.getString("MCR.Metadata.Languages")
            .map(MCRConfiguration2::splitValue)
            .map(s -> s.collect(Collectors.toList()))
            .orElseGet(Collections::emptyList));
    }

    public MCRAbstractCategoryImpl() {
        super();
        if (defaultLang == null) {
            defaultLang = MCRConfiguration2.getString("MCR.Metadata.DefaultLang").orElse(MCRConstants.DEFAULT_LANG);
        }
        labels = new TreeSet<>();
    }

    @Override
    public List<MCRCategory> getChildren() {
        return childGuard.lazyLoad(this::childrenNotHere, this::initChildren, () -> children);
    }

    private boolean childrenNotHere() {
        return children == null;
    }

    private void initChildren() {
        setChildrenUnlocked(MCRCategoryDAOFactory.obtainInstance().getChildren(id));
    }

    protected void setChildrenUnlocked(List<MCRCategory> children) {
        //Does nothing. Please override
    }

    @Override
    public MCRCategoryID getId() {
        return id;
    }

    @Override
    public void setId(MCRCategoryID id) {
        this.id = id;
    }

    @Override
    public SortedSet<MCRLabel> getLabels() {
        return labels;
    }

    @Override
    public MCRCategory getRoot() {
        if (getId().isRootID()) {
            return this;
        }
        if (root == null && getParent() != null) {
            root = getParent().getRoot();
        }
        return root;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public void setURI(URI uri) {
        this.uri = uri;
    }

    @Override
    public boolean hasChildren() {
        return childGuard
            .read(() -> Optional.ofNullable(children).map(c -> !c.isEmpty()))
            .orElse(MCRCategoryDAOFactory.obtainInstance().hasChildren(id));
    }

    @Override
    public final boolean isCategory() {
        return !isClassification();
    }

    @Override
    public final boolean isClassification() {
        return getId().isRootID();
    }

    @Override
    public MCRCategory getParent() {
        return parent;
    }

    public void setParent(MCRCategory parent) {
        if (Objects.equals(this.parent, parent)) {
            return;
        }
        detachFromParent();
        this.parent = parent;
        if (parent != null) {
            parent.getChildren().add(this);
        }
    }

    void detachFromParent() {
        if (parent != null) {
            // remove this from current parent
            parent.getChildren().remove(this);
            parent = null;
        }
    }

    @Override
    public Optional<MCRLabel> getCurrentLabel() {
        if (labels.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
            getLabel(MCRSessionMgr.getCurrentSession().getCurrentLanguage())
                .orElseGet(() -> getLabel(defaultLang)
                    .orElseGet(() -> labels.stream().filter(l -> LANGUAGES.contains(l.getLang())).findFirst()
                        .orElseGet(() -> labels.stream().filter(l -> !l.getLang().startsWith("x-")).findFirst()
                            .orElseGet(() -> labels.getFirst())))));
    }

    @Override
    public Optional<MCRLabel> getLabel(String lang) {
        String languageTag = Locale.forLanguageTag(lang).toLanguageTag();
        for (MCRLabel label : labels) {
            if (label.getLang().equals(languageTag)) {
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return Optional.ofNullable(id).map(MCRCategoryID::toString).orElse(null);
    }
}
