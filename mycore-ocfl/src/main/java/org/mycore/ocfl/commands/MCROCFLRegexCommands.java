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

package org.mycore.ocfl.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.mycore.frontend.cli.annotation.MCRCommandGroup;
import org.mycore.ocfl.classification.MCROCFLXMLClassificationManager;
import org.mycore.ocfl.metadata.MCROCFLXMLMetadataManager;
import org.mycore.ocfl.repository.MCROCFLRepositoryProvider;
import org.mycore.ocfl.user.MCROCFLXMLUserManager;
import org.mycore.ocfl.util.MCROCFLObjectIDPrefixHelper;

import io.ocfl.api.OcflRepository;

/**
 * All OCFL commands utilizing RegEx for bulk operations
 *
 * @author Tobias Lenhardt [Hammer1279]
 */
@MCRCommandGroup(name = "OCFL Regular Expression Commands")
public class MCROCFLRegexCommands {

    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean confirmPurge;

    private static final String METADATA_REPOSITORY_KEY = MCRConfiguration2
        .getString("MCR.Metadata.Manager.Repository")
        .orElse(null);

    private static final String CLASSIFICATION_REPOSITORY_KEY = MCRConfiguration2
        .getString("MCR.Classification.Manager.Repository")
        .orElse(null);

    private static final String USER_REPOSITORY_KEY = MCRConfiguration2
        .getString("MCR.Users.Manager.Repository")
        .orElse(null);

    /*
     * add other regex as well in the future:
     * delete,update,sync
     * list -> dry run
     */

    // Purge
    @SuppressWarnings({"MixedMutabilityReturnType", "PMD.UnusedAssignment"})
    @MCRCommand(syntax = "purge all ocfl entries matching {0}",
        help = "Purge all ocfl entries that match the given regex, use .* for all")
    public static List<String> purgeMatchAll(String regex) {
        if (!confirmPurge) {
            logConfirm("entries");
            confirmPurge = true;
            return Collections.emptyList();
        }
        List<String> commands = new ArrayList<>();
        String[] parts = getRegexParts(regex);
        if (MCROCFLObjectIDPrefixHelper.MCROBJECT.matches(parts[0])
            || MCROCFLObjectIDPrefixHelper.MCRDERIVATE.matches(parts[0])) {
            confirmPurge = true;
            commands.addAll(purgeMatchObj(parts[1]));
        }
        if (MCROCFLObjectIDPrefixHelper.CLASSIFICATION.matches(parts[0])) {
            confirmPurge = true;
            commands.addAll(purgeMatchClass(parts[1]));
        }
        if (MCROCFLObjectIDPrefixHelper.USER.matches(parts[0])) {
            confirmPurge = true;
            commands.addAll(purgeMatchUsr(parts[1]));
        }
        confirmPurge = false;
        return commands;
    }

    @MCRCommand(syntax = "purge ocfl objects matching {0}",
        help = "Purge ocfl objects that are matching the RegEx {0}")
    public static List<String> purgeMatchObj(String regex) {
        MCROCFLXMLMetadataManager manager = new MCROCFLXMLMetadataManager();
        manager.setRepositoryKey(METADATA_REPOSITORY_KEY);
        if (!confirmPurge) {
            logConfirm("objects");
            confirmPurge = true;
            return Collections.emptyList();
        }
        confirmPurge = false;
        return manager.getRepository().listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.MCROBJECT)
                || obj.startsWith(MCROCFLObjectIDPrefixHelper.MCRDERIVATE))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.MCROBJECT, ""))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.MCRDERIVATE, ""))
            .filter(obj -> obj.matches(regex))
            .map(id -> buildPurgeCommand("object", id))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "purge ocfl classifications matching {0}",
        help = "Purge ocfl classifications that are matching the RegEx {0}")
    public static List<String> purgeMatchClass(String regex) {
        if (!confirmPurge) {
            logConfirm("classes");
            confirmPurge = true;
            return Collections.emptyList();
        }
        confirmPurge = false;
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(CLASSIFICATION_REPOSITORY_KEY);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.CLASSIFICATION))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.CLASSIFICATION, ""))
            .filter(obj -> obj.matches(regex))
            .map(id -> buildPurgeCommand("classification", id))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "purge ocfl users matching {0}",
        help = "Purge ocfl users that are matching the RegEx {0}")
    public static List<String> purgeMatchUsr(String regex) {
        if (!confirmPurge) {
            logConfirm("users");
            confirmPurge = true;
            return Collections.emptyList();
        }
        confirmPurge = false;
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(USER_REPOSITORY_KEY);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.USER))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.USER, ""))
            .filter(obj -> obj.matches(regex))
            .map(id -> buildPurgeCommand("user", id))
            .collect(Collectors.toList());
    }

    // Purge Marked

    @MCRCommand(syntax = "purge all marked ocfl entries matching {0}",
        help = "Purge all marked ocfl entries that match the given regex, use .* for all")
    public static List<String> purgeMarkedMatchAll(String regex) {
        String[] parts = getRegexParts(regex);
        List<String> commands = new ArrayList<>();
        if (MCROCFLObjectIDPrefixHelper.MCROBJECT.matches(parts[0])
            || MCROCFLObjectIDPrefixHelper.MCRDERIVATE.matches(parts[0])) {
            commands.add("purge marked ocfl objects matching " + parts[1]);
        }
        if (MCROCFLObjectIDPrefixHelper.CLASSIFICATION.matches(parts[0])) {
            commands.add("purge marked ocfl classifications matching " + parts[1]);
        }
        if (MCROCFLObjectIDPrefixHelper.USER.matches(parts[0])) {
            commands.add("purge marked ocfl users matching " + parts[1]);
        }
        return commands;
    }

    private static String[] getRegexParts(String regex) {
        String[] parts = regex.split(":", 2);
        if (parts.length < 2) {
            parts = Arrays.copyOf(parts, parts.length + 1);
            parts[1] = parts[0];
            parts[0] = ".*";
        }
        parts[0] += ":";
        return parts;
    }

    @MCRCommand(syntax = "purge marked ocfl objects matching {0}",
        help = "Purge marked ocfl objects that are matching the RegEx {0}")
    public static List<String> purgeMarkedMatchObj(String regex) {
        MCROCFLXMLMetadataManager manager = new MCROCFLXMLMetadataManager();
        manager.setRepositoryKey(METADATA_REPOSITORY_KEY);
        return streamDeleted(regex, manager)
            .map(id -> buildPurgeCommand("object", id))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "purge marked ocfl classifications matching {0}",
        help = "Purge marked ocfl classifications that are matching the RegEx {0}")
    public static List<String> purgeMarkedMatchClass(String regex) {
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(CLASSIFICATION_REPOSITORY_KEY);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.CLASSIFICATION))
            .filter(obj -> Objects.equals(repository.describeObject(obj).getHeadVersion().getVersionInfo().getMessage(),
                MCROCFLXMLClassificationManager.MESSAGE_DELETED))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.CLASSIFICATION, ""))
            .filter(obj -> obj.matches(regex))
            .map(id -> buildPurgeCommand("classification", id))
            .collect(Collectors.toList());
    }

    @MCRCommand(syntax = "purge marked ocfl users matching {0}",
        help = "Purge marked ocfl users that are matching the RegEx {0}")
    public static List<String> purgeMarkedMatchUsr(String regex) {
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(USER_REPOSITORY_KEY);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.USER))
            .filter(obj -> Objects.equals(repository.describeObject(obj).getHeadVersion().getVersionInfo().getMessage(),
                MCROCFLXMLUserManager.MESSAGE_DELETED))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.USER, ""))
            .filter(obj -> obj.matches(regex))
            .map(id -> buildPurgeCommand("user", id))
            .collect(Collectors.toList());
    }

    // Restore

    @MCRCommand(syntax = "restore all ocfl entries matching {0}",
        help = "Restores all ocfl entries that match the given regex, use .* for all")
    public static List<String> restoreMatchAll(String regex) {
        String[] parts = getRegexParts(regex);
        List<String> commands = new ArrayList<>();
        if (MCROCFLObjectIDPrefixHelper.MCROBJECT.matches(parts[0])
            || MCROCFLObjectIDPrefixHelper.MCRDERIVATE.matches(parts[0])) {
            commands.add("restore ocfl objects matching " + parts[1]);
        }
        if (MCROCFLObjectIDPrefixHelper.CLASSIFICATION.matches(parts[0])) {
            commands.add("restore ocfl classifications matching " + parts[1]);
        }
        if (MCROCFLObjectIDPrefixHelper.USER.matches(parts[0])) {
            commands.add("restore ocfl users matching " + parts[1]);
        }
        return commands;
    }

    @MCRCommand(syntax = "restore ocfl objects matching {0}",
        help = "Restore ocfl objects that are matching the RegEx {0}")
    public static List<String> restoreMatchObj(String regex) {
        MCROCFLXMLMetadataManager manager = new MCROCFLXMLMetadataManager();
        manager.setRepositoryKey(METADATA_REPOSITORY_KEY);
        return streamDeleted(regex, manager)
            .map(id -> buildRestoreCommand("object", id,
                manager.listRevisions(MCRObjectID.getInstance(id)).size() - 1))
            .toList();
    }

    @MCRCommand(syntax = "restore ocfl classifications matching {0}",
        help = "Restore ocfl classifications that are matching the RegEx {0}")
    public static List<String> restoreMatchClass(String regex) {
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(CLASSIFICATION_REPOSITORY_KEY);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.CLASSIFICATION))
            .filter(obj -> Objects.equals(repository.describeObject(obj).getHeadVersion().getVersionInfo().getMessage(),
                MCROCFLXMLClassificationManager.MESSAGE_DELETED))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.CLASSIFICATION, ""))
            .filter(obj -> obj.matches(regex))
            .map(id -> buildRestoreCommand("classification", id,
                repository.describeObject(MCROCFLObjectIDPrefixHelper.CLASSIFICATION + id).getVersionMap().size() - 1))
            .toList();
    }

    @MCRCommand(syntax = "restore ocfl users matching {0}",
        help = "Restore ocfl users that are matching the RegEx {0}")
    public static List<String> restoreMatchUsr(String regex) {
        OcflRepository repository = MCROCFLRepositoryProvider.getRepository(USER_REPOSITORY_KEY);
        return repository.listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.USER))
            .filter(obj -> Objects.equals(repository.describeObject(obj).getHeadVersion().getVersionInfo().getMessage(),
                MCROCFLXMLUserManager.MESSAGE_DELETED))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.USER, ""))
            .filter(obj -> obj.matches(regex))
            .map(id -> buildRestoreCommand("user", id, 
                repository.describeObject(MCROCFLObjectIDPrefixHelper.USER + id).getVersionMap().size() - 1))
            .collect(Collectors.toList());
    }

    private static Stream<String> streamDeleted(String regex, MCROCFLXMLMetadataManager manager) {
        return manager.getRepository().listObjectIds()
            .filter(obj -> obj.startsWith(MCROCFLObjectIDPrefixHelper.MCROBJECT)
                || obj.startsWith(MCROCFLObjectIDPrefixHelper.MCRDERIVATE))
            .filter(obj -> Objects.equals(
                manager.getRepository().describeObject(obj).getHeadVersion().getVersionInfo().getMessage(), "Deleted"))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.MCROBJECT, ""))
            .map(obj -> obj.replace(MCROCFLObjectIDPrefixHelper.MCRDERIVATE, ""))
            .filter(obj -> obj.matches(regex));
    }

    private static String buildPurgeCommand(String type, String id) {
        return "purge " + type + " " + id + " from ocfl";
    }

    private static String buildRestoreCommand(String type, String id, int version) {
        return "restore " + type + " " + id + " from ocfl with version v" + version;
    }

    private static void logConfirm(String type) {
        LOGGER.info(() -> String.format(Locale.ROOT, """

        \u001B[93mEnter the command again to confirm \u001B[4mPERMANENTLY\u001B[24m deleting ALL\
         matching OCFL %s.\u001B[0m
        \u001B[41mTHIS ACTION CANNOT BE UNDONE!\u001B[0m""", type));
    }

}
