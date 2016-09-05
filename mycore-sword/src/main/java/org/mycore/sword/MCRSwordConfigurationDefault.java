package org.mycore.sword;

import org.mycore.common.config.MCRConfiguration;
import org.swordapp.server.SwordConfiguration;

/**
 * @author Sebastian Hofmann (mcrshofm)
 */
public class MCRSwordConfigurationDefault implements SwordConfiguration {
    @Override
    public boolean returnDepositReceipt() {
        return true;
    }

    @Override
    public boolean returnStackTraceInError() {
        return true;
    }

    @Override
    public boolean returnErrorBody() {
        return true;
    }

    @Override
    public String generator() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Generator.Content");
    }

    @Override
    public String generatorVersion() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Generator.Version");
    }

    @Override
    public String administratorEmail() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Administrator.Mail");
    }

    @Override
    public String getAuthType() {
        return MCRConfiguration.instance().getString("MCR.SWORD.Auth.Method", "Basic");
    }

    @Override
    public boolean storeAndCheckBinary() {
        return false; // MCR code stores files and checks files..
    }

    @Override
    public String getTempDirectory() {
        return MCRConfiguration.instance().getString("MCR.SWORD.TempDirectory", System.getProperty("java.io.tmpdir"));
    }

    @Override
    public long getMaxUploadSize() {
        return MCRConfiguration.instance().getLong("MCR.SWORD.Max.Uploaded.File.Size");
    }

    @Override
    public String getAlternateUrl() {
        return null;
    }

    @Override
    public String getAlternateUrlContentType() {
        return null;
    }

    @Override
    public boolean allowUnauthenticatedMediaAccess() {
        return false;
    }
}
