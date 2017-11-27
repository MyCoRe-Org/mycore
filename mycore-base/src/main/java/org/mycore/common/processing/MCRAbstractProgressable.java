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

package org.mycore.common.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base implementation for an {@link MCRProgressable}. If you use this class
 * make sure to call {@link #setProgress(Integer)} and {@link #setProgressText(String)}
 * to invoke the {@link MCRProgressableListener}.
 * 
 * @author Matthias Eichner
 */
public class MCRAbstractProgressable implements MCRListenableProgressable {

    protected Integer progress;

    protected String progressText;

    protected final List<MCRProgressableListener> progressListener;

    public MCRAbstractProgressable() {
        this.progress = null;
        this.progressText = null;
        this.progressListener = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Sets the progress for this process.
     * 
     * @param progress the new progress between 0 and 100
     */
    public void setProgress(Integer progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException(
                "Cannot set progress to " + progress + ". It has to be between 0 and 100.");
        }
        Integer oldProgress = this.progress;
        this.progress = progress;
        fireProgressChanged(oldProgress);
    }

    /**
     * Sets the progress text for this process.
     * 
     * @param progressText the new progress text
     */
    public void setProgressText(String progressText) {
        String oldProgressText = this.progressText;
        this.progressText = progressText;
        fireProgressTextChanged(oldProgressText);
    }

    @Override
    public Integer getProgress() {
        return this.progress;
    }

    @Override
    public String getProgressText() {
        return this.progressText;
    }

    @Override
    public void addProgressListener(MCRProgressableListener listener) {
        this.progressListener.add(listener);
    }

    @Override
    public void removeProgressListener(MCRProgressableListener listener) {
        this.progressListener.remove(listener);
    }

    protected void fireProgressChanged(Integer oldProgress) {
        synchronized (this.progressListener) {
            this.progressListener.forEach(listener -> listener.onProgressChange(this, oldProgress, getProgress()));
        }
    }

    protected void fireProgressTextChanged(String oldProgressText) {
        synchronized (this.progressListener) {
            this.progressListener
                .forEach(listener -> listener.onProgressTextChange(this, oldProgressText, getProgressText()));
        }
    }
}
