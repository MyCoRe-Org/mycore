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

package org.mycore.access.facts.condition.fact;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Test;
import org.mycore.access.facts.MCRFactsHolder;
import org.mycore.access.facts.fact.MCRStringFact;
import org.mycore.common.MCRTestCase;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class MCRRegExConditionTest extends MCRTestCase {

    @Test
    public void testConditionMatch() {
        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        holder.add(new MCRStringFact("fact1", "a_nice_sample"));
        MCRRegExCondition regExCondition = new MCRRegExCondition();
        regExCondition.parse(new Element("regex").setAttribute("basefact", "fact1").setText("a_.*_sample"));
        Assert.assertTrue("regex should match", regExCondition.matches(holder));
    }

    @Test
    public void testConditionNotMatch() {
        MCRFactsHolder holder = new MCRFactsHolder(new ArrayList<>());
        holder.add(new MCRStringFact("fact1", "no_nice_sample"));
        MCRRegExCondition regExCondition = new MCRRegExCondition();
        regExCondition.parse(new Element("regex").setAttribute("basefact", "fact1").setText("a_.*_sample"));
        Assert.assertFalse("regex should not match", regExCondition.matches(holder));
    }
}