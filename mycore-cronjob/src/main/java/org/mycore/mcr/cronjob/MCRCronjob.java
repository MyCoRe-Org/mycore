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

package org.mycore.mcr.cronjob;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

import org.mycore.common.MCRException;
import org.mycore.common.config.annotation.MCRPostConstruction;
import org.mycore.common.config.annotation.MCRProperty;
import org.mycore.common.processing.MCRAbstractProcessable;
import org.mycore.common.processing.MCRProcessableStatus;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

/**
 * {@link MCRCronjobManager#JOBS_CONFIG_PREFIX} and automatic executed. If you want to create your own
 * {@link MCRCronjob} you should maybe look if {@link MCRCommandCronJob} is good enough.
 *
 * The Default properties for this Configurable are:
 * {@link #cronType} and {@link #setCron(String)}
 */
public abstract class MCRCronjob implements Runnable {

    private MCRAbstractProcessable processable;

    /**
     * The format type of the {@link #cron} property. Default is Unix, other possible values are:
     * CRON4J, QUARTZ, UNIX, SPRING
     */
    @MCRProperty(name = "CronType", required = false)
    public String cronType;

    private Cron cron;

    private String id;

    public String getID() {
        return id;
    }

    public void setID(String id) {
        this.id = id;
    }

    @MCRPostConstruction
    public void checkConfiguration(String property) {
        setID(property.substring(MCRCronjobManager.JOBS_CONFIG_PREFIX.length()));
        this.processable = new MCRAbstractProcessable();
        this.processable.setStatus(MCRProcessableStatus.created);
        this.processable.setName(getClass().getSimpleName() + " - " + getDescription());
        this.processable.setProgressText("Wait for " + getCronDescription() + "..");    }

    public Cron getCron() {
        return cron;
    }

    /**
     * @param cron The description when the Cronjob should be executed.
     */
    @MCRProperty(name = "Cron")
    public void setCron(String cron) {
        CronType cronType = Optional.ofNullable(this.cronType)
            .map(CronType::valueOf)
            .orElse(CronType.UNIX);

        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(cronType);
        CronParser parser = new CronParser(cronDefinition);
        this.cron = parser.parse(cron);
    }

    public Optional<Long> getNextExecution() {
        ZonedDateTime now = ZonedDateTime.now();
        Cron cron = getCron();
        Optional<Duration> duration = ExecutionTime.forCron(cron).timeToNextExecution(now);
        return duration.map(Duration::toMillis);
    }

    public String getCronDescription() {
        CronDescriptor descriptor = CronDescriptor.instance(Locale.ENGLISH);
        return descriptor.describe(this.getCron());
    }

    public MCRAbstractProcessable getProcessable() {
        return processable;
    }

    @Override
    public final void run() {
        getProcessable().setStatus(MCRProcessableStatus.processing);
        getProcessable().setProgressText("running..");
        try {
            runJob();
            getProcessable().setStatus(MCRProcessableStatus.successful);
        } catch (Exception e) {
            getProcessable().setStatus(MCRProcessableStatus.failed);
            throw new MCRException("Error while running Cronjob " + getID() + " - " + getDescription(), e);
        }
    }

    /**
     * Will be executed according to the Cron schedule. Remember to call {@link #getProcessable()} and
     * update its values to let the user know what you are doing.
     */
    public abstract void runJob();

    /**
     * @return A Description what this Cronjob does.
     */
    public abstract String getDescription();

}
