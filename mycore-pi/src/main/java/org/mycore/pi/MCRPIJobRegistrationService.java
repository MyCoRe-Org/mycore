package org.mycore.pi;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRUserInformation;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;
import org.mycore.services.queuedjob.MCRJobQueue;
import org.mycore.user2.MCRUser;
import org.mycore.user2.MCRUserManager;

/**
 * Implementation of a {@link MCRPIRegistrationService} which helps to outsource a registration task to a {@link MCRJob}
 * e.G. send a POST request to a REST api
 * @param <T>
 */
public abstract class MCRPIJobRegistrationService<T extends MCRPersistentIdentifier>
    extends MCRPIRegistrationService<T> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final MCRJobQueue REGISTER_JOB_QUEUE = initializeJobQueue();
    public static final String JOB_API_USER_PROPERTY = "jobApiUser";

    public MCRPIJobRegistrationService(String registrationServiceID, String identType) {
        super(registrationServiceID, identType);
    }

    private static MCRJobQueue initializeJobQueue() {
        LOGGER.info("Initializing jobQueue for PIRegistration!");
        return MCRJobQueue.getInstance(MCRPIRegisterJobAction.class);
    }

    public abstract void deleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;

    public abstract void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;

    public abstract void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;

    /**
     * Hook in to rollback mechanism of {@link MCRJobAction#rollback()} by overwriting this method.
     * @param parameters the parameters which was passed to {@link #addDeleteJob(Map)}
     * @throws MCRPersistentIdentifierException throw {@link MCRPersistentIdentifierException} if something goes
     * wrong during rollback
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected void rollbackDeleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
    }

    /**
     * Hook in to rollback mechanism of {@link MCRJobAction#rollback()} by overwriting this method.
     * @param parameters the parameters which was passed to {@link #updateJob(Map)}
     * @throws MCRPersistentIdentifierException throw {@link MCRPersistentIdentifierException} if something goes
     * wrong during rollback
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected void rollbackUpdateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
    }

    /**
     * Hook in to rollback mechanism of {@link MCRJobAction#rollback()} by overwriting this method.
     * @param parameters the parameters which was passed to {@link #addRegisterJob(Map)}
     * @throws MCRPersistentIdentifierException throw {@link MCRPersistentIdentifierException} if something goes
     * wrong during rollback
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected void rollbackRegisterJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
    }

    /**
     * @see #addRegisterJob(Map)
     */
    protected void addDeleteJob(Map<String, String> contextParameters) {
        MCRJob job = createJob(contextParameters, PiJobAction.DELETE);
        REGISTER_JOB_QUEUE.offer(job);
    }

    /**
     * @see #addRegisterJob(Map)
     */
    protected void addUpdateJob(Map<String, String> contextParameters) {
        MCRJob job = createJob(contextParameters, PiJobAction.UPDATE);
        REGISTER_JOB_QUEUE.offer(job);
    }

    /**
     * Adds a register job which will be called in the persistent {@link MCRJob} environment in a extra thread.
     * @param contextParameters pass parameters which are needed to register the PI. The parameter action and
     *                          registrationServiceID will be added, because they are necessary to reassign the job to
     *                          the right {@link MCRPIJobRegistrationService} and method.
     */
    protected void addRegisterJob(Map<String, String> contextParameters) {
        MCRJob job = createJob(contextParameters, PiJobAction.REGISTER);
        REGISTER_JOB_QUEUE.offer(job);
    }

    /**
     * Can be used to update the registration date in the database. The most {@link MCRPIJobRegistrationService}
     * only add the pi to the object and then to the database, with registration date of null. Later the job will
     * register the pi and then change the registration date to the right value.
     * @param mycoreID the id of the {@link org.mycore.datamodel.metadata.MCRBase} which has the pi assigned
     * @param additional information like path to a file
     * @param date the new registration date
     */
    protected void updateRegistrationDate(MCRObjectID mycoreID, String additional, Date date) {
        MCRPI pi = MCRPersistentIdentifierManager.getInstance()
            .get(this.getRegistrationServiceID(), mycoreID.toString(), additional);
        pi.setRegistered(date);
        updateFlag(mycoreID, additional, pi);
    }

    /**
     * Can be used to update the startRegistration date in the database. The most {@link MCRPIJobRegistrationService}
     * only add the pi to the object and then to the database, with registration or startRegistration date of null.
     * After a job is created the Registration service should update the
     * @param mycoreID the id of the {@link org.mycore.datamodel.metadata.MCRBase} which has the pi assigned
     * @param additional information like path to a file
     * @param date the new registration date
     */
    protected void updateStartRegistrationDate(MCRObjectID mycoreID, String additional, Date date) {
        MCRPI pi = MCRPersistentIdentifierManager.getInstance()
            .get(this.getRegistrationServiceID(), mycoreID.toString(), additional);
        pi.setRegistrationStarted(date);
        updateFlag(mycoreID, additional, pi);
    }

    /**
     * Tries to parse a identifier with a specific type.
     * @param identifier the identifier to parse
     * @return parsed identifier or {@link Optional#EMPTY} if there is no parser for the type or the parser can`t parse
     * the identifier
     * @throws ClassCastException when type does not match the type of T
     */
    protected Optional<T> parseIdentifier(String identifier) {
        MCRPersistentIdentifierParser<T> parserForType = MCRPersistentIdentifierManager.getInstance()
            .getParserForType(getType());

        if (parserForType == null) {
            return Optional.empty();
        }

        return parserForType.parse(identifier);
    }

    private MCRJob createJob(Map<String, String> contextParameters, PiJobAction action) {
        MCRJob job = new MCRJob(MCRPIRegisterJobAction.class);

        HashMap<String, String> params = new HashMap<>(contextParameters);
        params.put("action", action.toString());
        params.put("registrationServiceID", this.getRegistrationServiceID());
        job.setParameters(params);

        return job;
    }

    /**
     * Result of this will be passed to {@link MCRJobAction#name()}
     * @param contextParameters the parameters of the job
     * @return Some Information what this job will do or just {@link Optional#EMPTY}, then a default message is generated.
     */
    protected abstract Optional<String> getContextInformation(Map<String, String> contextParameters);

    public void runAsJobUser(PIRunnable task) throws MCRPersistentIdentifierException {
        boolean jobUserPresent = this.getProperties().containsKey(JOB_API_USER_PROPERTY);
        String jobUser = this.getProperties().get(JOB_API_USER_PROPERTY);
        MCRSession session = null;
        MCRUserInformation savedUserInformation = null;


        if (jobUserPresent) {
            session = MCRSessionMgr.getCurrentSession();
            savedUserInformation = session.getUserInformation();
            MCRUser user = MCRUserManager.getUser(jobUser);

            /* workaround https://mycore.atlassian.net/browse/MCR-1400*/
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());

            session.setUserInformation(user);
            LOGGER.info("Continue as User {}", jobUser);
        }


        try {
            task.run();
        } finally {
            if (jobUserPresent) {
                LOGGER.info("Continue as User {}", savedUserInformation.getUserID());

                /* workaround https://mycore.atlassian.net/browse/MCR-1400*/
                session.setUserInformation(MCRSystemUserInformation.getGuestInstance());

                session.setUserInformation(savedUserInformation);
            }
        }
    }

    void delegateAction(final Map<String, String> contextParameters) throws MCRPersistentIdentifierException {
        runAsJobUser(() -> {
            switch (getAction(contextParameters)) {
                case REGISTER:
                    registerJob(contextParameters);
                    break;
                case UPDATE:
                    updateJob(contextParameters);
                    break;
                case DELETE:
                    deleteJob(contextParameters);
                    break;
                default:
                    throw new MCRPersistentIdentifierException("Unhandled action type!");
            }
        });
    }


    void delegateRollback(final Map<String, String> contextParameters) throws MCRPersistentIdentifierException {
        runAsJobUser(() -> {
            switch (getAction(contextParameters)) {
                case REGISTER:
                    rollbackRegisterJob(contextParameters);
                    break;
                case UPDATE:
                    rollbackUpdateJob(contextParameters);
                    break;
                case DELETE:
                    rollbackDeleteJob(contextParameters);
                    break;
                default:
                    throw new MCRPersistentIdentifierException("Unhandled action type!");
            }
        });
    }

    protected PiJobAction getAction(Map<String, String> contextParameters) {
        return PiJobAction.valueOf(contextParameters.get("action"));
    }

    public enum PiJobAction {
        DELETE("delete"), REGISTER("register"), UPDATE("update");

        private final String action;

        PiJobAction(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }

    }

    private interface PIRunnable {
        public void run() throws MCRPersistentIdentifierException;
    }

}
