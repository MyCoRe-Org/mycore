package org.mycore.services.packaging;


import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * Base class for every Packer. You should implement {@link #pack()} and {@link #rollback()}.
 */
public abstract class MCRPacker {

    public static final String PACKER_CONFIGURATION_PREFIX = "MCR.Packaging.Packer.";
    private Map<String, String> configuration;
    private Map<String, String> parameter;

    /**
     * should check if all required parameters are set!
     * @return false if the parameters are invalid
     */
    public abstract boolean checkSetup();

    /**
     * This method will be called and the MCRPacker should start packing according to the {@link #getConfiguration()} and {@link #getParameters()}!
     *
     * @throws ExecutionException
     */
    public abstract void pack() throws ExecutionException;

    /**
     * This method can be called in case of error and the MCRPacker should clean up trash from {@link #pack()}
     */
    public abstract void rollback();

    /**
     * @return a unmodifiable map with all properties (MCR.Packaging.Packer.MyPackerID. prefix will be removed from key) of this packer-id.
     */
    protected final Map<String, String> getConfiguration() {
        return Collections.unmodifiableMap(this.configuration);
    }

    final void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    /**
     * @return a unmodifiable map with parameters of a specific {@link MCRPackerJobAction}.
     */
    protected final Map<String, String> getParameters() {
        return Collections.unmodifiableMap(this.parameter);
    }

    final void setParameter(Map<String, String> parameter) {
        this.parameter = parameter;
    }
}