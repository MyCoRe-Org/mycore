/*
 * $Revision$ 
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.common.xml;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DocumentNavigator;
import org.jaxen.expr.EqualityExpr;
import org.jaxen.expr.Expr;
import org.jaxen.expr.LiteralExpr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NameStep;
import org.jaxen.expr.Predicate;
import org.jaxen.expr.Step;
import org.jaxen.saxpath.Axis;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Parent;
import org.mycore.common.MCRConstants;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRNodeBuilder {

    private final static Logger LOGGER = LogManager.getLogger(MCRNodeBuilder.class);

    private Map<String, Object> variables;

    private Object firstNodeBuilt = null;

    public MCRNodeBuilder() {
    }

    public MCRNodeBuilder(Map<String, Object> variables) {
        this.variables = variables;
    }

    public Object getFirstNodeBuilt() {
        return firstNodeBuilt;
    }

    public Element buildElement(String xPath, String value, Parent parent) throws JaxenException {
        return (Element) buildNode(xPath, value, parent);
    }

    public Attribute buildAttribute(String xPath, String value, Parent parent) throws JaxenException {
        return (Attribute) buildNode(xPath, value, parent);
    }

    public Object buildNode(String xPath, String value, Parent parent) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("start building " + simplify(xPath) + " relative to " + MCRXPathBuilder.buildXPath(parent));
        return buildExpression(baseXPath.getRootExpr(), value, parent);
    }

    private Object buildExpression(Expr expression, String value, Parent parent) throws JaxenException {
        if (expression instanceof EqualityExpr)
            return buildEqualityExpression((EqualityExpr) expression, parent);
        else if (expression instanceof LocationPath)
            return buildLocationPath((LocationPath) expression, value, parent);
        else
            return canNotBuild(expression);
    }

    @SuppressWarnings("unchecked")
    private Object buildLocationPath(LocationPath locationPath, String value, Parent parent) throws JaxenException {
        Object existingNode = null;
        List<Step> steps = locationPath.getSteps();
        int i, indexOfLastStep = steps.size() - 1;

        for (i = indexOfLastStep; i >= 0; i--) {
            String xPath = buildXPath(steps.subList(0, i + 1));
            existingNode = evaluateFirst(xPath, parent);

            if (existingNode instanceof Element) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("element already existing");
                parent = (Element) existingNode;
                break;
            } else if (existingNode instanceof Attribute) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("attribute already existing");
                break;
            } else if (LOGGER.isDebugEnabled())
                LOGGER.debug(xPath + " does not exist or is not a node, will try to build it");
        }

        if (i == indexOfLastStep)
            return existingNode;
        else
            return buildLocationSteps(steps.subList(i + 1, steps.size()), value, parent);
    }

    private Object evaluateFirst(String xPath, Parent parent) {
        return new MCRXPathEvaluator(variables, parent).evaluateFirst(xPath);
    }

    private String buildXPath(List<Step> steps) {
        StringBuffer path = new StringBuffer();
        for (Step step : steps)
            path.append("/").append(step.getText());
        return simplify(path.substring(1));
    }

    private Object buildLocationSteps(List<Step> steps, String value, Parent parent) throws JaxenException {
        Object built = null;

        for (Iterator<Step> iterator = steps.iterator(); iterator.hasNext();) {
            Step step = iterator.next();

            built = buildStep(step, iterator.hasNext() ? null : value, parent);
            if (built == null)
                return parent;
            else if (firstNodeBuilt == null)
                firstNodeBuilt = built;

            if (built instanceof Parent)
                parent = (Parent) built;
        }

        return built;
    }

    private Object buildStep(Step step, String value, Parent parent) throws JaxenException {
        if (step instanceof NameStep)
            return buildNameStep((NameStep) step, value, parent);
        else {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug(
                    "ignoring step, can not be built: " + step.getClass().getName() + " " + simplify(step.getText()));
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object buildNameStep(NameStep nameStep, String value, Parent parent) throws JaxenException {
        String name = nameStep.getLocalName();
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRConstants.getStandardNamespace(prefix);

        if (nameStep.getAxis() == Axis.CHILD) {
            if (parent instanceof Document)
                return buildPredicates(nameStep.getPredicates(), ((Document) parent).getRootElement());
            else
                return buildPredicates(nameStep.getPredicates(), buildElement(ns, name, value, (Element) parent));
        } else if (nameStep.getAxis() == Axis.ATTRIBUTE) {
            return buildAttribute(ns, name, value, (Element) parent);
        } else {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("ignoring axis, can not be built: " + nameStep.getAxis() + " "
                    + (prefix.isEmpty() ? "" : prefix + ":") + name);
            return null;
        }
    }

    private Element buildPredicates(List<Predicate> predicates, Element parent) throws JaxenException {
        for (Predicate predicate : predicates)
            new MCRNodeBuilder(variables).buildExpression(predicate.getExpr(), null, parent);
        return parent;
    }

    private Object buildEqualityExpression(EqualityExpr ee, Parent parent) throws JaxenException {
        if (ee.getOperator().equals("=")) {
            if ((ee.getLHS() instanceof LocationPath) && (ee.getRHS() instanceof LiteralExpr))
                return assignLiteral(ee.getLHS(), (LiteralExpr) (ee.getRHS()), parent);
            else if ((ee.getRHS() instanceof LocationPath) && (ee.getLHS() instanceof LiteralExpr))
                return assignLiteral(ee.getRHS(), (LiteralExpr) (ee.getLHS()), parent);
            else if (ee.getLHS() instanceof LocationPath) {
                String value = getValueOf(ee.getRHS().getText(), parent);
                if (value != null)
                    return assignLiteral(ee.getLHS(), value, parent);
            }
        }
        return canNotBuild(ee);
    }

    /**
     * Resolves the first match for the given XPath and returns its value as a String 
     * 
     * @param xPath the XPath expression
     * @param parent the context element or document 
     * @return the value of the element or attribute as a String
     */
    public String getValueOf(String xPath, Parent parent) {
        Object result = evaluateFirst(xPath, parent);

        if (result instanceof String)
            return (String) result;
        else if (result instanceof Element)
            return ((Element) result).getText();
        else if (result instanceof Attribute)
            return ((Attribute) result).getValue();
        else
            return null;
    }

    private Object assignLiteral(Expr expression, LiteralExpr literal, Parent parent) throws JaxenException {
        String xPath = simplify(expression.getText()) + "[.=" + literal.getText() + "]";
        return assignLiteral(expression, literal.getLiteral(), parent, xPath);
    }

    private Object assignLiteral(Expr expression, String literal, Parent parent) throws JaxenException {
        String delimiter = literal.contains("'") ? "\"" : "'";
        String xPath = simplify(expression.getText()) + "[.=" + delimiter + literal + delimiter + "]";
        return assignLiteral(expression, literal, parent, xPath);
    }

    private Object assignLiteral(Expr expression, String literal, Parent parent, String xPath) throws JaxenException {
        Object result = evaluateFirst(xPath, parent);

        if ((result instanceof Element) || (result instanceof Attribute))
            return result;
        else {
            xPath = simplify(expression.getText()) + "[9999]";
            return buildNode(xPath, literal, parent);
        }
    }

    private Element buildElement(Namespace ns, String name, String value, Element parent) {
        Element element = new Element(name, ns);
        if ((value != null) && !value.isEmpty())
            element.setText(value);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("building new element " + element.getName());
        if (parent != null)
            parent.addContent(element);
        return element;
    }

    private Attribute buildAttribute(Namespace ns, String name, String value, Element parent) {
        Attribute attribute = new Attribute(name, value == null ? "" : value, ns);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("building new attribute " + attribute.getName());
        if (parent != null)
            parent.setAttribute(attribute);
        return attribute;
    }

    /**
     * Removes obsolete child:: and attribute:: axis prefixes from given XPath
     */
    public static String simplify(String xPath) {
        return xPath.replaceAll("child::", "").replaceAll("attribute::", "@");
    }

    private Object canNotBuild(Expr expression) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("ignoring expression, can not be built: " + expression.getClass().getName() + " "
                + simplify(expression.getText()));
        return null;
    }
}
