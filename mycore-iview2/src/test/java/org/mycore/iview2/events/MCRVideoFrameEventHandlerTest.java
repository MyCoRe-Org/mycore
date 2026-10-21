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

package org.mycore.iview2.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.common.config.MCRConfiguration2;
import org.mycore.iview2.backend.MCRVideoFrameJobAction;
import org.mycore.iview2.frontend.MCRIView2Commands;
import org.mycore.media.services.MCRVideoFrameGenerator;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobQueue;
import org.mycore.services.queuedjob.MCRJobQueueManager;
class MCRVideoFrameEventHandlerTest {

    @Test
    void queuesEveryVideoIncludingNestedFilesAndUpdates() throws Exception {
        MCRPath first = video("/first.mp4");
        MCRPath second = video("/folder/second.webm");
        BasicFileAttributes attributes = mock(BasicFileAttributes.class);
        when(attributes.isRegularFile()).thenReturn(true);
        MCRJobQueue queue = mock(MCRJobQueue.class);
        MCRJobQueueManager manager = mock(MCRJobQueueManager.class);
        when(manager.getJobQueue(MCRVideoFrameJobAction.class)).thenReturn(queue);
        try (var managers = mockStatic(MCRJobQueueManager.class);
            var contentTypes = mockStatic(MCRContentTypes.class);
            var configuration = mockStatic(MCRConfiguration2.class)) {
            managers.when(MCRJobQueueManager::getInstance).thenReturn(manager);
            contentTypes.when(() -> MCRContentTypes.probeContentType(first)).thenReturn("video/mp4");
            contentTypes.when(() -> MCRContentTypes.probeContentType(second)).thenReturn("video/webm");
            configuration.when(() -> MCRConfiguration2.getInstanceOfOrThrow(MCRVideoFrameGenerator.class,
                "MCR.Media.VideoFrameGenerator")).thenReturn(new AvailableGenerator());
            MCRVideoFrameEventHandler handler = new MCRVideoFrameEventHandler();
            handler.handlePathCreated(null, first, attributes);
            handler.handlePathCreated(null, second, attributes);
            handler.handlePathUpdated(null, second, attributes);
            handler.handlePathRepaired(null, second, attributes);
            ArgumentCaptor<MCRJob> jobs = ArgumentCaptor.forClass(MCRJob.class);
            verify(queue, times(4)).add(jobs.capture());
            assertEquals("/first.mp4", jobs.getAllValues().getFirst()
                .getParameter(MCRVideoFrameJobAction.PATH_PARAMETER));
            for (MCRJob job : jobs.getAllValues().subList(1, 4)) {
                assertEquals("/folder/second.webm", job.getParameter(MCRVideoFrameJobAction.PATH_PARAMETER));
                assertEquals("test_derivate_00000001", job.getParameter(MCRVideoFrameJobAction.DERIVATE_PARAMETER));
            }
        }
    }

    @Test
    void ignoresDirectoriesLocalPathsAndNonVideos() throws Exception {
        MCRPath path = video("/document.pdf");
        BasicFileAttributes attributes = mock(BasicFileAttributes.class);
        try (var managers = mockStatic(MCRJobQueueManager.class);
            var contentTypes = mockStatic(MCRContentTypes.class)) {
            MCRVideoFrameEventHandler handler = new MCRVideoFrameEventHandler();
            handler.handlePathCreated(null, path, attributes);
            handler.handlePathCreated(null, Path.of("local.mp4"), attributes);
            when(attributes.isRegularFile()).thenReturn(true);
            contentTypes.when(() -> MCRContentTypes.probeContentType(path)).thenReturn("application/pdf");
            handler.handlePathCreated(null, path, attributes);
            contentTypes.when(() -> MCRContentTypes.probeContentType(path)).thenReturn(null);
            handler.handlePathCreated(null, path, attributes);
            managers.verifyNoInteractions();
        }
    }

    @Test
    void doesNotQueueVideoWhenGeneratorRequirementsAreMissing() throws Exception {
        MCRPath path = video("/unavailable.mp4");
        BasicFileAttributes attributes = mock(BasicFileAttributes.class);
        when(attributes.isRegularFile()).thenReturn(true);
        MCRJobQueueManager manager = mock(MCRJobQueueManager.class);
        MCRJobQueue queue = mock(MCRJobQueue.class);
        when(manager.getJobQueue(MCRVideoFrameJobAction.class)).thenReturn(queue);
        try (var managers = mockStatic(MCRJobQueueManager.class);
            var contentTypes = mockStatic(MCRContentTypes.class);
            var configuration = mockStatic(MCRConfiguration2.class)) {
            managers.when(MCRJobQueueManager::getInstance).thenReturn(manager);
            contentTypes.when(() -> MCRContentTypes.probeContentType(path)).thenReturn("video/mp4");
            configuration.when(() -> MCRConfiguration2.getInstanceOfOrThrow(MCRVideoFrameGenerator.class,
                "MCR.Media.VideoFrameGenerator")).thenReturn(new UnavailableGenerator());
            new MCRVideoFrameEventHandler().handlePathCreated(null, path, attributes);
            verify(queue, times(0)).add(org.mockito.ArgumentMatchers.any(MCRJob.class));
        }
    }

    @Test
    void deletesTilesWithoutReadingDeletedVideo() {
        MCRPath path = video("/folder/deleted.mp4");
        BasicFileAttributes attributes = mock(BasicFileAttributes.class);
        when(attributes.isRegularFile()).thenReturn(true);
        try (var commands = mockStatic(MCRIView2Commands.class);
            var contentTypes = mockStatic(MCRContentTypes.class)) {
            new MCRVideoFrameEventHandler().handlePathDeleted(null, path, attributes);
            commands.verify(() -> MCRIView2Commands.deleteImageTiles("test_derivate_00000001", "/folder/deleted.mp4"));
            contentTypes.verifyNoInteractions();
        }
    }

    private MCRPath video(String relativePath) {
        MCRPath path = mock(MCRPath.class);
        when(path.getOwner()).thenReturn("test_derivate_00000001");
        when(path.getOwnerRelativePath()).thenReturn(relativePath);
        return path;
    }

    private static class UnavailableGenerator implements MCRVideoFrameGenerator {

        @Override
        public BufferedImage generateFrame(Path video) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean checkRequirements(Path video) {
            return false;
        }
    }

    private static final class AvailableGenerator extends UnavailableGenerator {

        @Override
        public boolean checkRequirements(Path video) {
            return true;
        }
    }

}
