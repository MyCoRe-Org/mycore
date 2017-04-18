/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

/*
 *  This file is part of ***  M y C o R e  ***
 *  See http://www.mycore.de/ for details.
 *
 *  This program is free software; you can use it, redistribute it
 *  and / or modify it under the terms of the GNU General Public License
 *  (GPL) as published by the Free Software Foundation; either version 2
 *  of the License or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program, in a file called gpl.txt or license.txt.
 *  If not, write to the Free Software Foundation Inc.,
 *  59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 *
 */

package org.mycore.sass;


import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.IOUtils;

/**
 * Imports scss files using {@link ServletContext}.
 */
public class MCRServletContextResourceImporter implements Importer {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ServletContext context;

    public MCRServletContextResourceImporter(ServletContext context) {
        this.context = context;
    }

    @Override
    public Collection<Import> apply(String url, Import previous) {
        try {
            String absolute = url;
            if (previous != null) {
                absolute = previous.getAbsoluteUri().resolve(absolute).toString();
            }

            URL resource = null;
            List<String> possibleNameForms = getPossibleNameForms(absolute);
            int i = 0;
            while (i < possibleNameForms.size() && (resource = context.getResource(normalize(possibleNameForms.get(i)))) == null) {
                i++;
            }
            if (resource == null) {
                return null;
            }

            String contents = getStringContent(possibleNameForms.get(i));
            URI absoluteUri = resource.toURI();

            LOGGER.debug("Resolved " + url + " to " + absoluteUri.toString());
            return Stream.of(new Import(absolute, absolute, contents)).collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            LOGGER.error("Error while resolving " + url, e);
            return null;
        }
    }

    private List<String> getPossibleNameForms(String relative) {
        ArrayList<String> nameFormArray = new ArrayList<>();

        int lastSlashPos = relative.lastIndexOf('/');
        if (lastSlashPos != -1) {
            String _Form = relative.substring(0, lastSlashPos) + "/_" + relative.substring(lastSlashPos + 1);
            nameFormArray.add(_Form);
            nameFormArray.add(_Form + ".scss");
        }

        nameFormArray.add(relative);
        nameFormArray.add(relative + ".scss");

        return nameFormArray;
    }

    private String getStringContent(String resource) throws IOException {
        try (InputStream resourceAsStream = context.getResourceAsStream(normalize(resource))) {
            InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream, "UTF-8");
            return IOUtils.toString(inputStreamReader);
        }
    }

    private String normalize(String resource) {
        return !resource.startsWith("/") ? "/" + resource : resource;
    }
}
