package org.mycore.pi;

import static java.net.URLDecoder.decode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Element;
import org.jdom2.transform.JDOMSource;
import org.mycore.access.MCRAccessManager;
import org.mycore.common.MCRException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public class MCRPIURIResolver implements URIResolver {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ID_ARG = "id";
    private static final String ADDITIONAL_ARG = "additional";
    private static final String OBJECT_ID_ARG = "objectID";
    private static final String PI_ARG = "pi";
    private static final String SERVICE_ARG = "service";
    private static final String HAS_IDENTIFIER_CREATED_METHOD = "hasIdentifierCreated";
    private static final String HAS_IDENTIFIER_REGISTRATION_STARTED_METHOD = "hasIdentifierRegistrationStarted";
    private static final String HAS_IDENTIFIER_REGISTERED_METHOD = "hasIdentifierRegistered";
    private static final String HAS_MANAGED_PI_METHOD = "hasManagedPI";
    private static final String IS_MANAGED_PI_METHOD = "isManagedPI";
    private static final String GET_PI_SERVICE_INFORMATION_METHOD = "getPIServiceInformation";

    public static boolean hasIdentifierCreated(String service, String id, String additional) {
        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isCreated(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasIdentifierRegistrationStarted(String service, String id, String additional) {
        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.hasRegistrationStarted(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasIdentifierRegistered(String service, String id, String additional) {
        MCRPIService<MCRPersistentIdentifier> registrationService = MCRPIServiceManager
            .getInstance().getRegistrationService(service);
        return registrationService.isRegistered(MCRObjectID.getInstance(id), additional);
    }

    public static boolean hasManagedPI(String objectID) {
        return !MCRPIManager.getInstance()
            .getRegistered(MCRMetadataManager.retrieveMCRObject(MCRObjectID.getInstance(objectID))).isEmpty();
    }

    public static boolean isManagedPI(String pi, String id) {
        return MCRPIManager.getInstance().getInfo(pi).stream().anyMatch(info -> info.getMycoreID()
            .equals(id));
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        String[] parts = href.split(":", 2);

        if (parts.length != 2) {
            throw new TransformerException("Invalid href format: " + href);
        }

        String subURL = parts[1];

        String[] methodParts = subURL.split("(/(\\?)?)", 2);
        if (methodParts.length != 1 && methodParts.length != 2) {
            throw new TransformerException("Invalid href format: " + href);
        }

        String method = methodParts[0];
        String arguments = methodParts[1];

        Map<String, String> argumentMap = getArgumentMap(href, arguments);

        return switch (method) {
            case HAS_IDENTIFIER_CREATED_METHOD -> {
                requireArguments(argumentMap, HAS_IDENTIFIER_CREATED_METHOD, SERVICE_ARG, ID_ARG, ADDITIONAL_ARG);
                yield wrapBoolean(
                    hasIdentifierCreated(argumentMap.get(SERVICE_ARG), argumentMap.get(ID_ARG), argumentMap.get(
                        ADDITIONAL_ARG)));
            }
            case HAS_IDENTIFIER_REGISTRATION_STARTED_METHOD -> {
                requireArguments(argumentMap, HAS_IDENTIFIER_REGISTRATION_STARTED_METHOD, SERVICE_ARG, ID_ARG,
                    ADDITIONAL_ARG);
                yield wrapBoolean(
                    hasIdentifierRegistrationStarted(argumentMap.get(SERVICE_ARG), argumentMap.get(ID_ARG),
                        argumentMap.get(ADDITIONAL_ARG)));
            }
            case HAS_IDENTIFIER_REGISTERED_METHOD -> {
                requireArguments(argumentMap, HAS_IDENTIFIER_REGISTERED_METHOD, SERVICE_ARG, ID_ARG, ADDITIONAL_ARG);
                yield wrapBoolean(hasIdentifierRegistered(argumentMap.get(SERVICE_ARG), argumentMap.get(ID_ARG),
                    argumentMap.get(ADDITIONAL_ARG)));
            }
            case HAS_MANAGED_PI_METHOD -> {
                requireArguments(argumentMap, HAS_MANAGED_PI_METHOD, OBJECT_ID_ARG);
                yield wrapBoolean(hasManagedPI(argumentMap.get(OBJECT_ID_ARG)));
            }
            case IS_MANAGED_PI_METHOD -> {
                requireArguments(argumentMap, IS_MANAGED_PI_METHOD, PI_ARG, ID_ARG);
                yield wrapBoolean(isManagedPI(argumentMap.get(PI_ARG), argumentMap.get(ID_ARG)));
            }
            case GET_PI_SERVICE_INFORMATION_METHOD -> {
                requireArguments(argumentMap, GET_PI_SERVICE_INFORMATION_METHOD, OBJECT_ID_ARG);
                yield wrapElement(getPIServiceInformation(argumentMap.get(OBJECT_ID_ARG)));
            }
            default -> throw new TransformerException("Unknown method: " + method);
        };

    }

    private static void requireArguments(Map<String, String> argumentMap, String fnName, String... requiredArgs)
        throws TransformerException {
        for (String arg : requiredArgs) {
            if (!argumentMap.containsKey(arg)) {
                throw new TransformerException("Missing required argument '" + arg + "' in function " + fnName + "!");
            }
        }
    }

    private static Map<String, String> getArgumentMap(String href, String arguments) {
        return Arrays.stream(arguments.split("&"))
            .map(arg -> arg.split("=", 2))
            .map(pair -> {
                if (pair.length == 1) {
                    return new String[] { pair[0], "" };
                }
                return pair;
            })
            .filter(pair -> pair.length == 2)
            .map(pair -> {
                try {
                    return new String[] { decode(pair[0], StandardCharsets.UTF_8),
                        decode(pair[1], StandardCharsets.UTF_8) };
                } catch (Exception e) {
                    throw new MCRException("Error decoding arguments in href: " + href, e);
                }
            })
            .collect(Collectors.toMap(pair -> pair[0], pair -> pair[1]));
    }

    private Source wrapBoolean(boolean value) {
        Element booleanElement = new Element("boolean");
        booleanElement.setText(Boolean.toString(value));
        return new JDOMSource(booleanElement);
    }

    private Source wrapElement(Element element) {
        return new JDOMSource(element);
    }

    public static Element getPIServiceInformation(String objectID) {
        Element e = new Element("list");
        MCRBase obj = MCRMetadataManager.retrieve(MCRObjectID.getInstance(objectID));
        MCRPIServiceManager.getInstance().getServiceList()
            .stream()
            .map(rs -> buildServiceElement(objectID, rs, obj))
            .forEach(e::addContent);
        return e;
    }

    private static Element buildServiceElement(String objectID,
        MCRPIService<MCRPersistentIdentifier> rs, MCRBase obj) {
        Element service = new Element("service");

        service.setAttribute("id", rs.getServiceID());
        service.setAttribute("type", rs.getType());

        try {
            service.setAttribute("inscribed",
                rs.getMetadataService().getIdentifier(obj, "")
                    .map(i -> Boolean.TRUE.toString())
                    .orElse(Boolean.FALSE.toString()));

        } catch (MCRPersistentIdentifierException e1) {
            LOGGER.warn("Error happened while try to read PI from object: {}", objectID, e1);
            service.setAttribute("inscribed", "false");
        }

        String permission = "register-" + rs.getServiceID();
        boolean canRegister = MCRAccessManager.checkPermission(objectID, MCRAccessManager.PERMISSION_WRITE) &&
            MCRAccessManager.checkPermission(obj.getId(), permission);
        service.setAttribute("permission", Boolean.toString(canRegister));

        return service;
    }

}
