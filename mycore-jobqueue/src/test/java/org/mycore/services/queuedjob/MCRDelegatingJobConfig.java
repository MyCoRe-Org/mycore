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

package org.mycore.services.queuedjob;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * A delegating {@link MCRJobConfig} for testing purposes. All methods delegate to the given base config and can be
 * overridden selectively.
 */
public class MCRDelegatingJobConfig implements MCRJobConfig {

    private final MCRJobConfig delegate;

    public MCRDelegatingJobConfig(MCRJobConfig delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<Duration> timeTillReset(Class<? extends MCRJobAction> action) {
        return delegate.timeTillReset(action);
    }

    @Override
    public Optional<Integer> maxTryCount(Class<? extends MCRJobAction> action) {
        return delegate.maxTryCount(action);
    }

    @Override
    public Optional<Integer> maxJobThreadCount(Class<? extends MCRJobAction> action) {
        return delegate.maxJobThreadCount(action);
    }

    @Override
    public Optional<Boolean> activated(Class<? extends MCRJobAction> action) {
        return delegate.activated(action);
    }

    @Override
    public Optional<Integer> retryDelayMultiplier(Class<? extends MCRJobAction> action) {
        return delegate.retryDelayMultiplier(action);
    }

    @Override
    public Integer maxJobThreadCount() {
        return delegate.maxJobThreadCount();
    }

    @Override
    public Duration timeTillReset() {
        return delegate.timeTillReset();
    }

    @Override
    public Integer maxTryCount() {
        return delegate.maxTryCount();
    }

    @Override
    public Boolean activated() {
        return delegate.activated();
    }

    @Override
    public List<MCRJobStatusListener> jobStatusListeners(Class<? extends MCRJobAction> action) {
        return delegate.jobStatusListeners(action);
    }
}
