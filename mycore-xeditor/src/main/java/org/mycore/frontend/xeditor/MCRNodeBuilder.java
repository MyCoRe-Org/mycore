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

package org.mycore.frontend.xeditor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathFactory;

/**
 * @author Frank L\u00FCtzenkirchen
 */
public class MCRNodeBuilder {

    private final static Logger LOGGER = Logger.getLogger(MCRNodeBuilder.class);

    public static Object build(String xPath, String value, Map<String, Object> variables, Parent parent) throws JaxenException {
        BaseXPath baseXPath = new BaseXPath(xPath, new DocumentNavigator());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("start building " + simplify(baseXPath.getRootExpr().getText()) + " relative to "
                    + MCRXPathBuilder.buildXPath(parent));
        return buildExpression(baseXPath.getRootExpr(), value, variables, parent);
    }

    private static Object buildExpression(Expr expression, String value, Map<String, Object> variables, Parent parent) {
        if (expression instanceof EqualityExpr)
            return buildEqualityExpression((EqualityExpr) expression, variables, parent);
        else if (expression instanceof LocationPath)
            return buildLocationPath((LocationPath) expression, value, variables, parent);
        else
            return canNotBuild(expression);
    }

    @SuppressWarnings("unchecked")
    private static Object buildLocationPath(LocationPath locationPath, String value, Map<String, Object> variables, Parent parent) {
        Object existingNode = null;
        List<Step> steps = locationPath.getSteps();
        int i, indexOfLastStep = steps.size() - 1;

        for (i = indexOfLastStep; i >= 0; i--) {
            String xPath = buildXPath(steps.subList(0, i + 1));
            existingNode = evaluateFirst(xPath, variables, parent);

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
            return buildLocationSteps(steps.subList(i + 1, steps.size()), value, variables, parent);
    }

    private static Object evaluateFirst(String xPath, Map<String, Object> variables, Parent parent) {
        XPathFactory factory = XPathFactory.instance();
        Object result = factory.compile(xPath, Filters.fpassthrough(), variables, MCRUsedNamespaces.getNamespaces()).evaluateFirst(parent);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("evaluating " + xPath + " returns " + result);
        return result;
    }

    private static String buildXPath(List<Step> steps) {
        StringBuffer path = new StringBuffer();
        for (Step step : steps)
            path.append("/").append(step.getText());
        return simplify(path.substring(1));
    }

    private static Object buildLocationSteps(List<Step> steps, String value, Map<String, Object> variables, Parent parent) {
        Object built = null;

        for (Iterator<Step> iterator = steps.iterator(); iterator.hasNext();) {
            Step step = iterator.next();

            built = buildStep(step, iterator.hasNext() ? null : value, variables, parent);
            if (built == null)
                return parent;
            if (built instanceof Parent)
                parent = (Parent) built;
        }

        return built;
    }

    private static Object buildStep(Step step, String value, Map<String, Object> variables, Parent parent) {
        if (step instanceof NameStep)
            return buildNameStep((NameStep) step, value, variables, parent);
        else {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("ignoring step, can not be built: " + step.getClass().getName() + " " + simplify(step.getText()));
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object buildNameStep(NameStep nameStep, String value, Map<String, Object> variables, Parent parent) {
        String name = nameStep.getLocalName();
        String prefix = nameStep.getPrefix();
        Namespace ns = prefix.isEmpty() ? Namespace.NO_NAMESPACE : MCRUsedNamespaces.getNamespace(prefix);

        if (nameStep.getAxis() == Axis.CHILD) {
            if (parent instanceof Document)
                return buildPredicates(nameStep.getPredicates(), variables, ((Document) parent).getRootElement());
            else
                return buildPredicates(nameStep.getPredicates(), variables, buildElement(ns, name, value, (Element) parent));
        } else if (nameStep.getAxis() == Axis.ATTRIBUTE) {
            return buildAttribute(ns, name, value, (Element) parent);
        } else {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("ignoring axis, can not be built: " + nameStep.getAxis() + " " + (prefix.isEmpty() ? "" : prefix + ":") + name);
            return null;
        }
    }

    private static Element buildPredicates(List<Predicate> predicates, Map<String, Object> variables, Element parent) {
        for (Predicate predicate : predicates)
            buildExpression(predicate.getExpr(), null, variables, parent);
        return parent;
    }

    private static Object buildEqualityExpression(EqualityExpr ee, Map<String, Object> variables, Parent parent) {
        if ((ee.getLHS() instanceof LocationPath) && (ee.getRHS() instanceof LiteralExpr) && ee.getOperator().equals("="))
            return assignLiteral(ee.getLHS(), (LiteralExpr) (ee.getRHS()), variables, parent);
        else if ((ee.getRHS() instanceof LocationPath) && (ee.getLHS() instanceof LiteralExpr) && ee.getOperator().equals("="))
            return assignLiteral(ee.getRHS(), (LiteralExpr) (ee.getLHS()), variables, parent);
        else
            return canNotBuild(ee);
    }

    private static Object assignLiteral(Expr expression, LiteralExpr literal, Map<String, Object> variables, Parent parent) {
        String xPath = simplify(expression.getText()) + "[.=" + literal.getText() + "]";
        Object result = evaluateFirst(xPath, variables, parent);

        if ((result instanceof Element) || (result instanceof Attribute))
            return result;
        else {
            xPath = simplify(expression.getText()) + "[9999]";
            try {
                return build(xPath, literal.getLiteral(), variables, parent);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private static Element buildElement(Namespace ns, String name, String value, Element parent) {
        Element element = new Element(name, ns);
        element.setText(value == null ? "" : value);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("building new element " + element.getName());
        if (parent != null)
            parent.addContent(element);
        return element;
    }

    private static Attribute buildAttribute(Namespace ns, String name, String value, Element parent) {
        Attribute attribute = new Attribute(name, value == null ? "" : value, ns);
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("building new attribute " + attribute.getName());
        if (parent != null)
            parent.setAttribute(attribute);
        return attribute;
    }

    private static String simplify(String xPath) {
        return xPath.replaceAll("child::", "").replaceAll("attribute::", "@");
    }

    private static Object canNotBuild(Expr expression) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("ignoring expression, can not be built: " + expression.getClass().getName() + " " + simplify(expression.getText()));
        return null;
    }
}
