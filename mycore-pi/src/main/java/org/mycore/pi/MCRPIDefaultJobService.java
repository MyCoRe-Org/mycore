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
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.mycore.backend.jpa.MCREntityManagerProvider;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.backend.MCRPI;
import org.mycore.pi.exceptions.MCRPersistentIdentifierException;

public abstract class MCRPIDefaultJobService<T extends MCRPersistentIdentifier> extends MCRPIJobService<T> {

    public MCRPIDefaultJobService(String identType) {
        super(identType);
    }

    @Override
    public MCRPI insertIdentifierToDatabase(MCRBase obj, String additional, T identifier) {
        Date registrationStarted = null;
        if (getRegistrationPredicate().test(obj)) {
            registrationStarted = new Date();
            this.addRegisterJob(createJobContextParams(PiJobAction.REGISTER, obj, identifier));
        }

        MCRPI databaseEntry = new MCRPI(identifier.asString(), getType(), obj.getId().toString(), additional,
            this.getServiceID(), provideRegisterDate(obj, additional), registrationStarted);
        MCREntityManagerProvider.getCurrentEntityManager().persist(databaseEntry);
        return databaseEntry;
    }


    @Override
    protected void registerIdentifier(MCRBase obj, String additional, T pi)
        throws MCRPersistentIdentifierException {
        if (!"".equals(additional)) {
            String className = this.getClass().getName();
            throw new MCRPersistentIdentifierException(
                className + " doesn't support additional information! (" + additional + ")");
        }
        // this is done in insertIdentifier 
        //  -> Do we need a "Switch-Property" to configure this beaviour?
        // this.addRegisterJob(createJobContextParams(PiJobAction.REGISTER, obj, pi));
    }

    @Override
    protected void delete(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        this.addDeleteJob(createJobContextParams(PiJobAction.DELETE, obj, identifier));
    }

    @Override
    protected void update(T identifier, MCRBase obj, String additional)
        throws MCRPersistentIdentifierException {
        if (!this.hasRegistrationStarted(obj.getId(), additional)) {
            Predicate<MCRBase> registrationCondition = this.getRegistrationPredicate();
            if (registrationCondition.test(obj)) {
                this.updateStartRegistrationDate(obj.getId(), "", new Date());
                this.addRegisterJob(createJobContextParams(PiJobAction.REGISTER, obj, identifier));
            }
        } else if (this.isRegistered(obj.getId(), "")) {
            this.addUpdateJob(createJobContextParams(PiJobAction.UPDATE, obj, identifier));
        }
    }

    @Override
    public abstract void registerJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;
    
    @Override
    public abstract void updateJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;
        
    @Override
    protected abstract void deleteJob(Map<String, String> parameters) throws MCRPersistentIdentifierException;
    
    @Override
    protected Optional<String> getJobInformation(Map<String, String> contextParameters) {
        return Optional.empty();
    }

    protected abstract HashMap<String, String> createJobContextParams(PiJobAction action, MCRBase obj, T pi);
        

}
