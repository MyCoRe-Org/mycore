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
