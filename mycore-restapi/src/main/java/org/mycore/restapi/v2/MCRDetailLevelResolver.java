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

package org.mycore.restapi.v2;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.mycore.restapi.converter.MCRDetailLevel;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Resolves the {@link MCRDetailLevel} requested via the {@code detail} media type parameter
 * of the {@code Accept} header, e.g. {@code application/json; detail=SUMMARY}.
 * <p>
 * Any REST resource offering SUMMARY/NORMAL/DETAILED content negotiation on a GET
 * endpoint should use this instead of re-implementing the same header parsing.
 * <p>
 * The requested value is matched case-insensitively, so {@code summary}, {@code Summary}
 * and {@code SUMMARY} are all accepted - there is no reason to reject a request over
 * something as trivial as casing.
 */
public final class MCRDetailLevelResolver {

    /**
     * An example value for the {@code Accept} header's {@code detail} media type parameter,
     * for use in {@code @Parameter}/{@code @Schema} OpenAPI annotations.
     */
    public static final String DETAIL_LEVEL_EXAMPLE = "application/json; detail=SUMMARY";

    /**
     * Human-readable description of the {@code detail} media type parameter, for use in
     * {@code @Parameter} OpenAPI annotations documenting the {@code Accept} header of a GET
     * endpoint that supports SUMMARY/NORMAL/DETAILED content negotiation via {@link #resolve}.
     */
    public static final String DETAIL_LEVEL_DESCRIPTION =
        "Controls the level of detail in the response via the detail parameter. "
            + "Supported values: SUMMARY, NORMAL, DETAILED. "
            + "Example: " + DETAIL_LEVEL_EXAMPLE;

    private MCRDetailLevelResolver() {
    }

    /**
     * Resolves the detail level requested by the given request's {@code Accept} header.
     *
     * @param request the current request
     * @return the requested detail level, or {@link MCRDetailLevel#NORMAL} if none was given
     * @throws BadRequestException if the given detail level is not one of SUMMARY, NORMAL or
     *         DETAILED (regardless of case)
     */
    public static MCRDetailLevel resolve(ContainerRequestContext request) {
        Optional<String> detailLevelOptional = request.getAcceptableMediaTypes().stream()
            .flatMap(m -> m.getParameters().entrySet().stream()
                .filter(e -> MCRDetailLevel.MEDIA_TYPE_PARAMETER.equals(e.getKey()))).map(Map.Entry::getValue)
            .findFirst();
        if (detailLevelOptional.isEmpty()) {
            return MCRDetailLevel.NORMAL;
        }
        String detailLevel = detailLevelOptional.get();
        try {
            return MCRDetailLevel.valueOf(detailLevel.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown detail level: " + detailLevel, e);
        }
    }

}
