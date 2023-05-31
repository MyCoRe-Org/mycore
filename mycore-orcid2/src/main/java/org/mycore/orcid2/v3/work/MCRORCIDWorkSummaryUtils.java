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

package org.mycore.orcid2.v3.work;

import java.util.function.Supplier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.mycore.orcid2.MCRORCIDUtils;
import org.mycore.orcid2.exception.MCRORCIDException;
import org.mycore.orcid2.exception.MCRORCIDTransformationException;
import org.mycore.orcid2.metadata.MCRORCIDMetadataUtils;
import org.mycore.orcid2.metadata.MCRORCIDPutCodeInfo;
import org.mycore.orcid2.util.MCRIdentifier;
import org.mycore.orcid2.v3.transformer.MCRORCIDWorkTransformerHelper;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.xml.sax.SAXException;

/**
 * Provides utility methods for ORCID WorkSummary.
 */
public class MCRORCIDWorkSummaryUtils {

    /**
     * Transforms List of work summaries to List of elements.
     * 
     * @param works List of work summaries
     * @return List of elements
     * @throws MCRORCIDException if build fails
     */
    public static List<Element> buildUnmergedMODSFromWorkSummaries(List<WorkSummary> works) {
        final List<Element> modsElements = new ArrayList<>();
        works.forEach(w -> {
            try {
                modsElements.add(MCRORCIDWorkTransformerHelper.transformWorkSummary(w).asXML().detachRootElement());
            } catch (IOException | JDOMException | SAXException | MCRORCIDTransformationException e) {
                throw new MCRORCIDException("Build failed", e);
            }
        });
        return modsElements;
    }

    /**
     * Returns a Stream of WorkSummaries matching the identifiers of the Work.
     *
     * @param work the Work
     * @param workSummaries List of WorkSummaries
     * @return Stream of matching WorkSummaries
     * @see MCRORCIDUtils#checkTrustedIdentifier
     */
    public static Stream<WorkSummary> findMatchingSummariesByIdentifiers(Work work, List<WorkSummary> workSummaries) {
        return findMatchingSummariesByIdentifiers(work, workSummaries.stream());
    }

    /**
     * Returns a Stream of WorkSummaries matching the identifiers of the Work.
     *
     * @param work the Work
     * @param workSummaries Stream of WorkSummaries
     * @return Stream of matching WorkSummaries
     * @see MCRORCIDUtils#checkTrustedIdentifier
     */
    public static Stream<WorkSummary> findMatchingSummariesByIdentifiers(Work work, Stream<WorkSummary> workSummaries) {
        final Set<MCRIdentifier> trustedIdentifiers = work.getExternalIdentifiers().getExternalIdentifier().stream()
            .filter(i -> MCRORCIDUtils.checkTrustedIdentifier(i.getType()))
            .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet());
        return findMatchingSummariesByIdentifiers(trustedIdentifiers, workSummaries);
    }

    // TODO ingnore case for identifiers/values
    // TODO identifiers may be case sensitive?
    /**
     * Returns a Stream of WorkSummaries matching a set of MCRIdentifiers.
     *
     * @param identifiers the set of MCRIdentifiers
     * @param workSummaries List of WorkSummaries
     * @return Stream of matching WorkSummaries
     */
    public static Stream<WorkSummary> findMatchingSummariesByIdentifiers(Set<MCRIdentifier> identifiers,
        Stream<WorkSummary> workSummaries) {
        if (identifiers.isEmpty()) {
            return Stream.empty();
        }
        return workSummaries
            .filter(
                w -> hasMatch(
                    w.getExternalIdentifiers().getExternalIdentifier().stream()
                        .map(i -> new MCRIdentifier(i.getType(), i.getValue())).collect(Collectors.toSet()),
                    identifiers));
    }

    /**
     * Updates work info based on Work with List of matching WorkSummary as reference.
     * 
     * @param work the Work
     * @param summaries List of WorkSummary
     * @param workInfo the initial work info
     */
    protected static void updateWorkInfoFromSummaries(Work work, List<WorkSummary> summaries,
        MCRORCIDPutCodeInfo workInfo) {
        final Supplier<Stream<WorkSummary>> matchingWorksSupplier
            = () -> findMatchingSummariesByIdentifiers(work, summaries);
        long ownPutCode = workInfo.getOwnPutCode();
        // validate current own put code
        if (ownPutCode == 0 || !checkPutCodeExistsInSummaries(summaries, ownPutCode)) {
            // try to find own work via identifiers as fallback
            workInfo.setOwnPutCode(getPutCodeCreatedByThisAppFromSummaries(matchingWorksSupplier.get()));
        }
        if (MCRORCIDMetadataUtils.SAVE_OTHER_PUT_CODES) { // optimization
            workInfo.setOtherPutCodes(getPutCodesNotCreatedByThisAppFromSummaries(matchingWorksSupplier.get()));
        } else {
            workInfo.setOtherPutCodes(null);
        }
    }

    /**
     * Checks Stream of WorkSummary for WorkSummary created by this application.
     * 
     * @param summaries Stream of WorkSummary
     * @return Optional of WorkSummary
     * @see MCRORCIDUtils#isCreatedByThisApplication
     */
    private static Optional<WorkSummary> findSummaryCreateByThisApplication(Stream<WorkSummary> summaries) {
        return summaries.filter(s -> MCRORCIDUtils.isCreatedByThisApplication(s.retrieveSourcePath())).findFirst();
    }

    private static boolean checkPutCodeExistsInSummaries(List<WorkSummary> works, long putCode) {
        return works.stream().filter(w -> w.getPutCode().equals(putCode)).findAny().isPresent();
    }

    private static long[] getPutCodesNotCreatedByThisAppFromSummaries(Stream<WorkSummary> works) {
        return works.filter(w -> !MCRORCIDUtils.isCreatedByThisApplication(w.retrieveSourcePath()))
            .map(WorkSummary::getPutCode).mapToLong(l -> (long) l).toArray();
    }

    private static long getPutCodeCreatedByThisAppFromSummaries(Stream<WorkSummary> works) {
        return findSummaryCreateByThisApplication(works).map(WorkSummary::getPutCode).orElse(0l);
    }

    /**
     * Checks if Set A of as matching element in Set B.
     * 
     * @param a Set A
     * @param b Set B
     * @return true if there is an element of A in B
     */
    private static boolean hasMatch(Set<MCRIdentifier> a, Set<MCRIdentifier> b) {
        a.retainAll(b);
        return !a.isEmpty();
    }
}
