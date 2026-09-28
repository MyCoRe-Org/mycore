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
package org.mycore.validation.pdfa;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

import org.mycore.common.MCRSessionMgr;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRMetaLinkID;
import org.mycore.datamodel.metadata.MCRObject;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

/**
 * Keeps the persisted PDF/A validation reports in sync with the files and the state of the objects they belong to.
 * <p>
 * Uploading or replacing a PDF file schedules its validation, deleting it removes its report. Entering an excluded
 * state removes the reports of all derivates of an object, returning to an eligible state schedules the validation
 * of every PDF file that has no report, without requiring another upload. Deleting an object or a derivate always
 * removes the associated reports.
 */
public class MCRPDFAValidationEventHandler extends MCREventHandlerBase {

    @Override
    protected void handlePathCreated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        handlePathUpdated(evt, path, attrs);
    }

    @Override
    protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        forPDFFile(path, (derivateId, file) -> {
            if (MCRPDFAReportManager.isEligible(derivateId)) {
                MCRPDFAReportManager.scheduleValidation(derivateId, file);
            } else {
                MCRPDFAReportManager.deleteReport(derivateId, file);
            }
        });
    }

    @Override
    protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        forPDFFile(path, MCRPDFAReportManager::deleteReport);
    }

    @Override
    protected void handleObjectUpdated(MCREvent evt, MCRObject obj) {
        boolean excluded = MCRPDFAReportManager.isExcluded(obj);
        if (evt.get(MCREvent.OBJECT_OLD_KEY) instanceof MCRObject old
            && MCRPDFAReportManager.isExcluded(old) == excluded) {
            return;
        }
        List<MCRObjectID> derivateIds = getDerivateIds(obj);
        // the state change is only visible to the jobs once it is committed
        MCRSessionMgr.getCurrentSession().onCommit(() -> {
            if (!excluded) {
                derivateIds.forEach(MCRPDFAReportManager::scheduleMissingValidations);
            } else if (MCRPDFAReportManager.isDeleteOnExcludedState()) {
                derivateIds.forEach(MCRPDFAReportManager::deleteReports);
            }
        });
    }

    @Override
    protected void handleObjectDeleted(MCREvent evt, MCRObject obj) {
        getDerivateIds(obj).forEach(MCRPDFAReportManager::deleteReports);
    }

    @Override
    protected void handleDerivateDeleted(MCREvent evt, MCRDerivate der) {
        MCRPDFAReportManager.deleteReports(der.getId());
    }

    private static List<MCRObjectID> getDerivateIds(MCRObject obj) {
        return obj.getStructure().getDerivates().stream().map(MCRMetaLinkID::getXLinkHrefID).toList();
    }

    private static void forPDFFile(Path path, BiConsumer<MCRObjectID, String> action) {
        MCRPath mcrPath = MCRPath.ofPath(path);
        String file = mcrPath.getOwnerRelativePath();
        if (!file.toLowerCase(Locale.ROOT).endsWith(MCRPDFAReportManager.PDF_SUFFIX)) {
            return;
        }
        action.accept(MCRObjectID.getInstance(mcrPath.getOwner()), file.substring(1));
    }
}
