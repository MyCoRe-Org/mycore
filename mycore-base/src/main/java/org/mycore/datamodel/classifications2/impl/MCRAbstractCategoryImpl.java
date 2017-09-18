/**
 *
 * $Revision$ $Date$
 *
 * This file is part of ** M y C o R e **
 * Visit our homepage at http://www.mycore.de/ for details.
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
 * along with this program, normally in the file license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 **/
package org.mycore.datamodel.classifications2.impl;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.mycore.common.MCRConstants;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.classifications2.MCRCategory;
import org.mycore.datamodel.classifications2.MCRCategoryDAOFactory;
import org.mycore.datamodel.classifications2.MCRCategoryID;
import org.mycore.datamodel.classifications2.MCRLabel;
import org.mycore.util.concurrent.MCRReadWriteGuard;

/**
 * @author Thomas Scheffler (yagee)
 *
 * @version $Revision$ $Date$
 * @since 2.0
 */
public abstract class MCRAbstractCategoryImpl implements MCRCategory {

    protected final MCRReadWriteGuard childGuard = new MCRReadWriteGuard();

    protected MCRCategory root;

    protected MCRCategory parent;

    protected Set<MCRLabel> labels;

    protected List<MCRCategory> children;

    private MCRCategoryID id;

    private URI URI;

    private String defaultLang;

    private static HashSet<String> LANGUAGES;

    {
        LANGUAGES = new HashSet<>(MCRConfiguration.instance().getStrings("MCR.Metadata.Languages",
            Collections.emptyList()));
    }

    public MCRAbstractCategoryImpl() {
        super();
        if (defaultLang == null) {
            defaultLang = MCRConfiguration.instance().getString("MCR.Metadata.DefaultLang", MCRConstants.DEFAULT_LANG);
        }
        labels = new HashSet<MCRLabel>();
    }

    public List<MCRCategory> getChildren() {
        return childGuard.lazyLoad(this::childrenNotHere, this::initChildren, () -> children);
    }

    private boolean childrenNotHere() {
        return children == null;
    }

    private void initChildren() {
        setChildrenUnlocked(MCRCategoryDAOFactory.getInstance().getChildren(id));
    }

    protected abstract void setChildrenUnlocked(List<MCRCategory> children);

    public MCRCategoryID getId() {
        return id;
    }

    public void setId(MCRCategoryID id) {
        this.id = id;
    }

    public Set<MCRLabel> getLabels() {
        return labels;
    }

    public MCRCategory getRoot() {
        if (getId().isRootID()) {
            return this;
        }
        if (root == null && getParent() != null) {
            root = getParent().getRoot();
        }
        return root;
    }

    public URI getURI() {
        return URI;
    }

    public void setURI(URI uri) {
        URI = uri;
    }

    public boolean hasChildren() {
        return childGuard
            .read(() -> Optional.ofNullable(children).map(c -> !c.isEmpty()))
            .orElse(MCRCategoryDAOFactory.getInstance().hasChildren(id));
    }

    public final boolean isCategory() {
        return !isClassification();
    }

    public final boolean isClassification() {
        return getId().isRootID();
    }

    public MCRCategory getParent() {
        return parent;
    }

    public void setParent(MCRCategory parent) {
        if (this.parent == parent) {
            return;
        }
        detachFromParent();
        this.parent = parent;
        if (parent != null) {
            parent.getChildren().add(this);
        }
    }

    /**
     *
     */
    void detachFromParent() {
        if (parent != null) {
            // remove this from current parent
            parent.getChildren().remove(this);
            parent = null;
        }
    }

    public Optional<MCRLabel> getCurrentLabel() {
        if (labels.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
            getLabel(MCRSessionMgr.getCurrentSession().getCurrentLanguage())
                .orElseGet(() -> getLabel(defaultLang)
                    .orElseGet(() -> labels.stream().filter(l -> LANGUAGES.contains(l.getLang())).findFirst()
                        .orElseGet(() -> labels.stream().filter(l -> !l.getLang().startsWith("x-")).findFirst()
                            .orElseGet(() -> labels.iterator().next())))));
    }

    public Optional<MCRLabel> getLabel(String lang) {
        String languageTag = Locale.forLanguageTag(lang).toLanguageTag();
        for (MCRLabel label : labels) {
            if (label.getLang().equals(languageTag)) {
                return Optional.of(label);
            }
        }
        return Optional.empty();
    }

    public String toString() {
        return Optional.ofNullable(id).map(MCRCategoryID::toString).orElse(null);
    }
}
