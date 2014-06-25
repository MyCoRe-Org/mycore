/*
 * $Id$
 * $Revision: 5697 $ $Date: Dec 18, 2013 $
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

package org.mycore.common;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.mycore.common.config.MCRConfigurationException;

/**
 * Forwards every method to {@link org.mycore.common.config.MCRConfiguration}
 * @author Thomas Scheffler (yagee)
 * @deprecated use {@link org.mycore.common.config.MCRConfiguration} instead
 */
@Deprecated
public class MCRConfiguration {

    private static MCRConfiguration singleton = new MCRConfiguration();

    org.mycore.common.config.MCRConfiguration backend;

    private MCRConfiguration() {
        backend = org.mycore.common.config.MCRConfiguration.instance();
    }

    public Properties getProperties() {
        return toProperties(getPropertiesMap());
    }

    private Properties toProperties(Map<String, String> propertiesMap) {
        Properties props = new Properties();
        props.putAll(propertiesMap);
        return props;
    }

    public Properties getProperties(String startsWith) {
        return toProperties(getPropertiesMap(startsWith));
    }

    /**
     * Returns the single instance of this class that can be used to read and
     * manage the configuration properties.
     * 
     * @return the single instance of <CODE>MCRConfiguration</CODE> to be used
     */
    public static MCRConfiguration instance() {
        return singleton;
    }

    public final long getSystemLastModified() {
        return backend.getSystemLastModified();
    }

    public final void systemModified() {
        backend.systemModified();
    }

    public Map<String, String> getPropertiesMap() {
        return backend.getPropertiesMap();
    }

    public Map<String, String> getPropertiesMap(String startsWith) {
        return backend.getPropertiesMap(startsWith);
    }

    public void configureLogging() {
        backend.configureLogging();
    }

    public <T> T getInstanceOf(String name, String defaultname, Class<T> type) {
        return backend.<T>getInstanceOf(name, defaultname);
    }

    public <T> T getInstanceOf(String name, String defaultname) throws MCRConfigurationException {
        return backend.getInstanceOf(name, defaultname);
    }

    public <T> T getInstanceOf(String name, Class<T> type) throws MCRConfigurationException {
        return backend.<T>getInstanceOf(name);
    }

    public <T> T getInstanceOf(String name) throws MCRConfigurationException {
        return backend.getInstanceOf(name);
    }

    public <T> T getSingleInstanceOf(String name, String defaultname) throws MCRConfigurationException {
        return backend.getSingleInstanceOf(name, defaultname);
    }

    public <T> T getSingleInstanceOf(String name, String defaultname, Class<T> type) throws MCRConfigurationException {
        return backend.<T>getSingleInstanceOf(name, defaultname);
    }

    public <T> T getSingleInstanceOf(String name) {
        return backend.getSingleInstanceOf(name);
    }

    public <T> T getSingleInstanceOf(String name, Class<T> type) {
        return backend.<T>getSingleInstanceOf(name);
    }

    public String getString(String name) {
        return backend.getString(name);
    }

    public List<String> getStrings(String name) {
        return backend.getStrings(name);
    }

    public List<String> getStrings(String name, List<String> defaultValue) {
        return backend.getStrings(name, defaultValue);
    }

    public String getString(String name, String defaultValue) {
        return backend.getString(name, defaultValue);
    }

    public int getInt(String name) throws NumberFormatException {
        return backend.getInt(name);
    }

    public int getInt(String name, int defaultValue) throws NumberFormatException {
        return backend.getInt(name, defaultValue);
    }

    public long getLong(String name) throws NumberFormatException {
        return backend.getLong(name);
    }

    public long getLong(String name, long defaultValue) throws NumberFormatException {
        return backend.getLong(name, defaultValue);
    }

    public float getFloat(String name) throws NumberFormatException {
        return backend.getFloat(name);
    }

    public float getFloat(String name, float defaultValue) throws NumberFormatException {
        return backend.getFloat(name, defaultValue);
    }

    public double getDouble(String name) throws NumberFormatException {
        return backend.getDouble(name);
    }

    public double getDouble(String name, double defaultValue) throws NumberFormatException {
        return backend.getDouble(name, defaultValue);
    }

    public boolean getBoolean(String name) {
        return backend.getBoolean(name);
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        return backend.getBoolean(name, defaultValue);
    }

    public void set(String name, String value) {
        backend.set(name, value);
    }

    public void initialize(Map<String, String> props, boolean clear) {
        backend.initialize(props, clear);
    }

    public void set(String name, int value) {
        backend.set(name, value);
    }

    public void set(String name, long value) {
        backend.set(name, value);
    }

    public void set(String name, float value) {
        backend.set(name, value);
    }

    public void set(String name, double value) {
        backend.set(name, value);
    }

    public void set(String name, boolean value) {
        backend.set(name, value);
    }

    public void list(PrintStream out) {
        backend.list(out);
    }

    public void list(PrintWriter out) {
        backend.list(out);
    }

    public void store(OutputStream out, String header) throws IOException {
        backend.store(out, header);
    }

    public String toString() {
        return backend.toString();
    }
}
