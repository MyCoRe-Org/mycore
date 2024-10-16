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

package org.mycore.pi;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.access.MCRAccessManager;
import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.common.MCRSession;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.MCRSystemUserInformation;
import org.mycore.common.MCRTransactionHelper;
import org.mycore.common.MCRUserInformation;
import org.mycore.common.MCRUserInformationResolver;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;
import org.mycore.services.queuedjob.MCRJobQueue;
import org.mycore.services.queuedjob.MCRJobQueueManager;

/**
 * Implementation of a {@link MCRPIService} which helps to outsource a registration task to a {@link MCRJob}
 * e.G. send a POST request to a REST api
 *
 * @param <T>
 */
public abstract class MCRPIJobService<T extends MCRPersistentIdentifier>
    extends MCRPIService<T> {

    public static final String JOB_API_USER_PROPERTY = "JobApiUser";

    protected static final String REGISTRATION_PREDICATE = "RegistrationPredicate";

    protected static final String CREATION_PREDICATE = "CreationPredicate";

    private static final Logger LOGGER = LogManager.getLogger();

    public MCRPIJobService(String identType) {
        super(identType);
    }

    private static MCRJobQueue getJobQueue() {
        return MCRJobQueueManager.getInstance().getJobQueue(MCRPIRegisterJobAction.class);
    }

    protected abstract void deleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;

    protected abstract void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;

    protected abstract void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;

    /**
     * Hook in to rollback mechanism of {@link MCRJobAction#rollback()} by overwriting this method.
     *
     * @param parameters the parameters which was passed to {@link #addJob(PiJobAction, Map<String, String>)} 
     *        with piJobAction = 'DELETE'
     * @throws MCRPersistentIdentifierException throw {@link MCRPersistentIdentifierException} if something goes
     *                                          wrong during rollback
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected void rollbackDeleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        // can be used to rollback
    }

    /**
     * Hook in to rollback mechanism of {@link MCRJobAction#rollback()} by overwriting this method.
     *
     * @param parameters the parameters which was passed to {@link #addJob(PiJobAction, Map<String, String>)} 
     *        with piJobAction = 'UPDATE'
     * @throws MCRPersistentIdentifierException throw {@link MCRPersistentIdentifierException} if something goes
     *                                          wrong during rollback
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected void rollbackUpdateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        // can be used to rollback
    }

    /**
     * Hook in to rollback mechanism of {@link MCRJobAction#rollback()} by overwriting this method.
     *
     * @param parameters the parameters which was passed to {@link #addJob(PiJobAction, Map<String, String>)} 
     *        with piJobAction = 'REGISTER'
     * @throws MCRPersistentIdentifierException throw {@link MCRPersistentIdentifierException} if something goes
     *                                          wrong during rollback
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    protected void rollbackRegisterJob(Map<String, String> parameters) throws MCRPersistentIdentifierException {
        // can be used to rollback
    }

    @Override
    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional, T identifier, MCRPIServiceDates dates) {
        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getServiceID(), dates);
        MCREntityManagerProvider.getCurrentEntityManager().persist(databaseEntry);
        return databaseEntry;
    }

    @Override
    public MCRPIServiceDates registerIdentifier(MCRBase obj, String additional, T identifier)
        throws MCRPersistentIdentifierException {
        if (!additional.equals("")) {
            throw new MCRPersistentIdentifierException(
                getClass().getName() + " doesn't support additional information! (" + additional + ")");
        }
        if (getRegistrationPredicate().test(obj)) {
            this.addJob(PiJobAction.REGISTER,
                createJobContextParams(PiJobAction.REGISTER, obj, identifier, additional));
            return new MCRPIServiceDates(new Date(), null);
        }
        return new MCRPIServiceDates(null, null);
    }

    @Override
    protected void delete(T identifier, MCRBase obj, String additional) throws MCRPersistentIdentifierException {
        this.addJob(PiJobAction.DELETE, createJobContextParams(PiJobAction.DELETE, obj, identifier, additional));
    }

    /**
     * Adds a job for a given PIAction, which will be called in the persistent {@link MCRJob} environment 
     * in an extra thread.
     *
     * @param piJobAction      the action that the job executes (REGISTER, UPDATE, DELETE)
     * 
     * @param contextParameters pass parameters which are needed to register the PI. The parameter action and
     *                          registrationServiceID will be added, because they are necessary to reassign the job to
     *                          the right {@link MCRPIJobService} and method.
     */
    protected void addJob(PiJobAction piJobAction, Map<String, String> contextParameters) {
        MCRJob job = createJob(contextParameters, piJobAction);
        getJobQueue().offer(job);
    }

    /**
     * If you use {@link #updateRegistrationDate(MCRObjectID, String, Date)} or
     * {@link #updateStartRegistrationDate(MCRObjectID, String, Date)} then you should validate if the user has the
     * rights for this. This methods validates this and throws a handsome exception.
     *
     * @param id of the object
     */
    protected void validateJobUserRights(MCRObjectID id) throws MCRPersistentIdentifierException {
        if (!MCRAccessManager.checkPermission(id, MCRAccessManager.PERMISSION_WRITE)) {
            throw new MCRPersistentIdentifierException(
                String.format(Locale.ROOT,
                    "The user %s does not have rights to %s the object %s. You should set the property %s to "
                        + "a user which has the rights.",
                    MCRSessionMgr.getCurrentSession().getUserInformation().getUserID(),
                    MCRAccessManager.PERMISSION_WRITE,
                    id,
                    JOB_API_USER_PROPERTY));
        }
    }

    /**
     * Can be used to update the registration date in the database. The most {@link MCRPIJobService}
     * only add the pi to the object and then to the database, with registration date of null. Later the job will
     * register the pi and then change the registration date to the right value.
     * <b>If you use this methods from a job you should have called {@link #validateJobUserRights} before!</b>
     *
     * @param mycoreID   the id of the {@link org.mycore.datamodel.metadata.MCRBase} which has the pi assigned
     * @param additional information like path to a file
     * @param date       the new registration date
     */
    protected void updateRegistrationDate(MCRObjectID mycoreID, String additional, Date date) {
        MCRPI pi = MCRPIManager.getInstance()
            .get(this.getServiceID(), mycoreID.toString(), additional);
        pi.setRegistered(date);
        updateFlag(mycoreID, additional, pi);
    }

    /**
     * Can be used to update the startRegistration date in the database. The most {@link MCRPIJobService}
     * only add the pi to the object and then to the database, with registration or startRegistration date of null.
     * After a job is created the Registration service should update the date.
     * <b>If you use this methods from a job you should have called {@link #validateJobUserRights} before!</b>
     *
     * @param mycoreID   the id of the {@link org.mycore.datamodel.metadata.MCRBase} which has the pi assigned
     * @param additional information like path to a file
     * @param date       the new registration date
     */
    protected void updateStartRegistrationDate(MCRObjectID mycoreID, String additional, Date date) {
        MCRPI pi = MCRPIManager.getInstance()
            .get(this.getServiceID(), mycoreID.toString(), additional);
        pi.setRegistrationStarted(date);
        updateFlag(mycoreID, additional, pi);
    }

    /**
     * Tries to parse a identifier with a specific type.
     *
     * @param identifier the identifier to parse
     * @return parsed identifier or {@link Optional#empty()} if there is no parser for the type
     * or the parser can't parse the identifier
     * @throws ClassCastException when type does not match the type of T
     */
    protected Optional<T> parseIdentifier(String identifier) {
        MCRPIParser<T> parserForType = MCRPIManager.getInstance()
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
        params.put("registrationServiceID", this.getServiceID());
        job.setParameters(params);

        return job;
    }

    /**
     * Result of this will be passed to {@link MCRJobAction#name()}
     *
     * @param contextParameters the parameters of the job
     * @return Some Information what this job will do or just {@link Optional#empty()},
     * then a default message is generated.
     */
    protected abstract Optional<String> getJobInformation(Map<String, String> contextParameters);

    public void runAsJobUser(PIRunnable task) throws MCRPersistentIdentifierException {
        final boolean jobUserPresent = isJobUserPresent();
        MCRSession session = MCRSessionMgr.getCurrentSession();
        MCRUserInformation savedUserInformation = null;

        if (jobUserPresent) {
            final String jobUser = getJobUser();
            savedUserInformation = session.getUserInformation();
            MCRUserInformation userInformation = MCRUserInformationResolver.instance().getOrThrow(jobUser);

            /* workaround https://mycore.atlassian.net/browse/MCR-1400*/
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
            session.setUserInformation(userInformation);
            LOGGER.info("Continue as User {}", jobUser);
        } else {
            savedUserInformation = session.getUserInformation();
            session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
            session.setUserInformation(MCRSystemUserInformation.getJanitorInstance());
        }

        boolean transactionActive = !MCRTransactionHelper.isTransactionActive();
        try {
            if (transactionActive) {
                MCRTransactionHelper.beginTransaction();
            }
            task.run();
        } finally {
            if (transactionActive && MCRTransactionHelper.isTransactionActive()) {
                MCRTransactionHelper.commitTransaction();
            }

            if (jobUserPresent) {
                LOGGER.info("Continue as previous User {}", savedUserInformation.getUserID());

                /* workaround https://mycore.atlassian.net/browse/MCR-1400*/
                session.setUserInformation(MCRSystemUserInformation.getGuestInstance());
                session.setUserInformation(savedUserInformation);
            }
        }
    }

    private String getJobUser() {
        String jobApiUser = this.getProperties().get(JOB_API_USER_PROPERTY);
        // try to remain compatible with values configured before MCR-3033
        if (jobApiUser != null && !jobApiUser.contains(":")) {
            String userProviderKey = MCRUserInformationResolver.PROVIDERS_KEY + ".user";
            String userProviderClass = "org.mycore.user2.MCRUserProvider";
            if (this.getProperties().get(userProviderKey).equals(userProviderClass)) {
                LOGGER.warn("JobApiUser references username '" + jobApiUser
                    + "' directly. Switching to 'user:" + jobApiUser
                    + "' using the compatible user information provider "
                    + userProviderClass + " configured in "
                    + userProviderKey);
                jobApiUser = "user:" + jobApiUser;
            } else {
                LOGGER.error("JobApiUser references username '" + jobApiUser
                    + "' directly. Unable to switch to compatible user information provider "
                    + userProviderClass + " since it is not configured in "
                    + userProviderKey);
                throw new MCRConfigurationException("Invalid JobApiUser: " + jobApiUser);
            }
        }
        return jobApiUser;
    }

    private boolean isJobUserPresent() {
        return this.getProperties().containsKey(JOB_API_USER_PROPERTY);
    }

    void delegateAction(final Map<String, String> contextParameters) throws MCRPersistentIdentifierException {
        runAsJobUser(() -> {
            switch (getAction(contextParameters)) {
                case REGISTER -> registerJob(contextParameters);
                case UPDATE -> updateJob(contextParameters);
                case DELETE -> deleteJob(contextParameters);
                default -> throw new MCRPersistentIdentifierException("Unhandled action type!");
            }
        });
    }

    void delegateRollback(final Map<String, String> contextParameters) throws MCRPersistentIdentifierException {
        runAsJobUser(() -> {
            switch (getAction(contextParameters)) {
                case REGISTER -> rollbackRegisterJob(contextParameters);
                case UPDATE -> rollbackUpdateJob(contextParameters);
                case DELETE -> rollbackDeleteJob(contextParameters);
                default -> throw new MCRPersistentIdentifierException("Unhandled action type!");
            }
        });
    }

    protected PiJobAction getAction(Map<String, String> contextParameters) {
        return PiJobAction.valueOf(contextParameters.get("action"));
    }

    @Override
    protected void checkConfiguration() throws MCRConfigurationException {
        super.checkConfiguration();
        if (getProperties().containsKey("RegistrationConditionProvider")) {
            throw new MCRConfigurationException("The MCRPIService " + getServiceID() +
                " uses old property key RegistrationConditionProvider, use " + REGISTRATION_PREDICATE + " instead.");
        }
    }

    @Override
    protected void update(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (this.isRegistered(obj.getId(), additional)) {
            this.addJob(PiJobAction.UPDATE, createJobContextParams(PiJobAction.UPDATE, obj, identifier, additional));
        } else if (!this.hasRegistrationStarted(obj.getId(), additional) &&
            this.getRegistrationPredicate().test(obj) &&
            validateRegistrationDocument(obj, identifier, additional)) {
            this.updateStartRegistrationDate(obj.getId(), additional, new Date());
            this.addJob(PiJobAction.REGISTER,
                createJobContextParams(PiJobAction.REGISTER, obj, identifier, additional));
        }
    }

    protected abstract boolean validateRegistrationDocument(MCRBase obj, T identifier, String additional);

    protected abstract HashMap<String, String> createJobContextParams(PiJobAction action, MCRBase obj, T identifier,
        String additional);

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
        void run() throws MCRPersistentIdentifierException;
    }

}
