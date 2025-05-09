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
package org.mycore.pi.condition;

import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.datamodel.metadata.MCRBase;
import org.mycore.pi.MCRPIManager;

/**
 * PI Predicate, that checks if another PersistentIdentifier was registered within the PI component
 * before the current PI will be created or registered.
 * <p>
 * Use the properties *.Service and *.Type 
 * to specify the PI service and type of the PI which should be checked. 
 * <p>
 * sample configuration:
 * MCR.PI.Service.RosDokURN.CreationPredicate=org.mycore.pi.condition.MCRPIAndPredicate
 * MCR.PI.Service.RosDokURN.CreationPredicate.1=org.mycore.pi.condition.MCRPIOtherPIRegisteredPredicate
 * MCR.PI.Service.RosDokURN.CreationPredicate.1.Service=MCRLocalID
 * MCR.PI.Service.RosDokURN.CreationPredicate.1.Type=local_id
 * ...
 * 
 * @author Robert Stephan
 *
 */
public class MCRPIOtherPIRegisteredPredicate extends MCRPIPredicateBase
    implements MCRPICreationPredicate, MCRPIObjectRegistrationPredicate {

    @MCRProperty(name = "Type")
    public String type;

    @MCRProperty(name = "Service")
    public String service;

    @Override
    public boolean test(MCRBase mcrBase) {
        return MCRPIManager.getInstance().isRegistered(mcrBase.getId(), "", type, service);
    }

}
