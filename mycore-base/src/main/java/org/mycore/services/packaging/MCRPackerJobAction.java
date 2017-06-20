package org.mycore.services.packaging;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;

/**
 * Used to run a {@link MCRPacker} inside a {@link org.mycore.services.queuedjob.MCRJobQueue}
 *
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRPackerJobAction extends MCRJobAction {

    private static final Logger LOGGER = LogManager.getLogger();

    private MCRPacker packerInstance;

    public MCRPackerJobAction() {
    }

    public MCRPackerJobAction(MCRJob job) {
        super(job);
    }

    @Override
    public final boolean isActivated() {
        return true;
    }

    @Override
    public String name() {
        return "MCRPackerJobAction-" + getPackerId();
    }

    private String getPackerId() {
        return getParameters().get("packer");
    }

    public MCRPacker getPackerInstance() {
        return this.packerInstance;
    }

    @Override
    public final void execute() throws ExecutionException {
        String packerId = getPackerId();
        Map<String, String> packerConfiguration = getConfiguration(packerId);
        packerInstance = MCRConfiguration.instance()
            .getInstanceOf(MCRPacker.PACKER_CONFIGURATION_PREFIX + packerId + ".Class");

        Map<String, String> parameters = getParameters();

        packerInstance.setParameter(parameters);
        packerInstance.setConfiguration(packerConfiguration);
        LOGGER.info(() -> {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(getPackerId()).append(" starts packing with parameters: ");
            parameters.entrySet().forEach(
                (entry) -> messageBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(";"));
            return messageBuilder.toString();
        });
        packerInstance.pack();
    }

    protected final Map<String, String> getParameters() {
        return this.job.getParameters();
    }

    public static final Map<String, String> getConfiguration(String packerId) {
        String packerConfigPrefix = MCRPacker.PACKER_CONFIGURATION_PREFIX + packerId + ".";
        return MCRConfiguration
            .instance()
            .getPropertiesMap(packerConfigPrefix)
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(e -> e.getKey().substring(packerConfigPrefix.length()), Map.Entry::getValue));
    }

    @Override
    public void rollback() {
        if (packerInstance != null) {
            packerInstance.rollback();
        }
    }

}
