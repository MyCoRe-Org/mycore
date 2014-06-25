/*
 * $Id$
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
package org.mycore.buildtools.anttypes;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.types.selectors.BaseExtendSelector;
import org.apache.tools.ant.types.selectors.FileSelector;

/**
 * A {@link ResourceSelector} and {@link FileSelector} that filters mycore application modules.
 * Modules are detected by manifest entry <code>MCR-Application-Module</code>.
 * @author Thomas Scheffler (yagee)
 */
public class MCRApplicationModuleSelector extends BaseExtendSelector implements ResourceSelector {

    /* (non-Javadoc)
     * @see org.apache.tools.ant.types.selectors.FileSelector#isSelected(java.io.File, java.lang.String, java.io.File)
     */
    @Override
    public boolean isSelected(File basedir, String filename, File file) throws BuildException {
        validate();
        try {
            return isApplicationJAR(file);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    @Override
    public boolean isSelected(Resource r) {
        validate();
        try {
            if (r instanceof FileResource) {
                FileResource fr = (FileResource) r;
                return isApplicationJAR(fr.getFile());
            }
            log(r.toString() + " is not a local file.", Project.MSG_DEBUG);
            return false;
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    private boolean isApplicationJAR(File candidate) throws IOException {
        log("Checking " + candidate, Project.MSG_DEBUG);
        if (candidate.getName().endsWith(".jar")) {
            JarFile mycoreJar = new JarFile(candidate);
            Manifest manifest = mycoreJar.getManifest();
            if (manifest == null || manifest.getMainAttributes() == null)
                return false;
            final String moduleName = manifest.getMainAttributes().getValue("MCR-Application-Module");
            if (moduleName != null) {
                log("Found application module: " + moduleName);
                return true;
            }
        }
        return false;
    }

}
