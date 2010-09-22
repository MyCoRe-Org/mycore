/*
 * $Revision$ 
 * $Date$
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

package org.mycore.oai;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.jdom.Namespace;

/**
 * Provides some constants like namespaces, schema location, error codes and so on as defined by the OAI-PMH 2.0 protocol.
 * 
 * @author Frank L\u00FCtzenkirchen
 */
interface MCROAIConstants {
    final static Namespace NS_XSI = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

    final static Namespace NS_OAI = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");

    final static Namespace NS_OAI_ID = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/oai-identifier");

    final static Namespace NS_FRIENDS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/friends/");

    final static Namespace NS_OAI_DC = Namespace.getNamespace("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");

    final static Namespace NS_DC = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");

    final static String SCHEMA_LOC_OAI = "http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd";

    final static String SCHEMA_LOC_OAI_ID = "http://www.openarchives.org/OAI/2.0/oai-identifier http://www.openarchives.org/OAI/2.0/oai-identifier.xsd";

    final static String SCHEMA_LOC_FRIENDS = "http://www.openarchives.org/OAI/2.0/friends/ http://www.openarchives.org/OAI/2.0/friends.xsd";

    final static String SCHEMA_LOC_OAI_DC = "http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd";

    final static String ARG_VERB = "verb";

    final static String ARG_IDENTIFIER = "identifier";

    final static String ARG_METADATA_PREFIX = "metadataPrefix";

    final static String ARG_FROM = "from";

    final static String ARG_UNTIL = "until";

    final static String ARG_RESUMPTION_TOKEN = "resumptionToken";

    final static String ARG_SET = "set";

    final static String V_REQUIRED = "V_REQUIRED";

    final static String V_OPTIONAL = "V_OPTIONAL";

    final static String V_EXCLUSIVE = "V_EXCLUSIVE";

    final static String V_ALWAYS = "V_ALWAYS";

    final static String ERROR_BAD_VERB = "badVerb";

    final static String ERROR_BAD_ARGUMENT = "badArgument";

    final static String ERROR_BAD_RESUMPTION_TOKEN = "badResumptionToken";

    final static String ERROR_NO_SET_HIERARCHY = "noSetHierarchy";

    final static String ERROR_CANNOT_DISSEMINATE_FORMAT = "cannotDisseminateFormat";

    final static String ERROR_ID_DOES_NOT_EXIST = "idDoesNotExist";

    final static String ERROR_NO_METADATA_FORMATS = "noMetadataFormats";

    final static String ERROR_NO_RECORDS_MATCH = "noRecordsMatch";

    /** Datestamp granularity is currently fixed to day level */
    final static String GRANULARITY = "YYYY-MM-DD";

    final static String DATESTAMP_PATTERN = "yyyy-MM-dd";
}
