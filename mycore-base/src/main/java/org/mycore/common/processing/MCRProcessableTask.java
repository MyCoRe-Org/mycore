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

/**
 * Helper class to encapsulate a task within a processable. If the task
 * implements the {@link MCRListenableProgressable} interface the
 * progress will be delegated.
 * 
 * @author Matthias Eichner
 */
public abstract class MCRProcessableTask<T> extends MCRAbstractProcessable {

    protected T task;

    public MCRProcessableTask(T task) {
        super();
        this.task = task;
        delegateProgressable();
    }

    public T getTask() {
        return task;
    }

    protected void delegateProgressable() {
        if (this.task instanceof MCRListenableProgressable) {
            ((MCRListenableProgressable) this.task).addProgressListener(new MCRProgressableListener() {
                @Override
                public void onProgressTextChange(MCRProgressable source, String oldProgressText,
                    String newProgressText) {
                    setProgressText(newProgressText);
                }

                @Override
                public void onProgressChange(MCRProgressable source, Integer oldProgress, Integer newProgress) {
                    setProgress(newProgress);
                }
            });
        }
    }

}
