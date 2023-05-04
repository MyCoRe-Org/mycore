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

package org.mycore.services.queuedjob.rest.resources;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.mycore.common.MCRClassTools;
import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;
import org.mycore.restapi.annotations.MCRAccessControlExposeHeaders;
import org.mycore.restapi.annotations.MCRRequireTransaction;
import org.mycore.services.queuedjob.MCRJob;
import org.mycore.services.queuedjob.MCRJobAction;
import org.mycore.services.queuedjob.MCRJobDAO;
import org.mycore.services.queuedjob.MCRJobQueue;
import org.mycore.services.queuedjob.MCRJobQueueManager;
import org.mycore.services.queuedjob.MCRJobQueuePermission;
import org.mycore.services.queuedjob.MCRJobStatus;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;

/**
 * REST resource for job queues. Requires {@link MCRJobQueuePermission} to access.
 * @author René Adler (eagle)
 */
@Path("/")
@Singleton
public class MCRJobQueueResource {

    private static final String X_TOTAL_COUNT_HEADER = "X-Total-Count";

    /**
     * REST resource to receive all job queues.
     * @return a list of all job queues as JSON or XML
     */
    @GET()
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    @MCRRequireTransaction()
    public Response listJSON() {
        Queues queuesEntity = new Queues();
        queuesEntity.addAll(
            MCRJobQueueManager.getInstance().getJobQueues().stream()
                .map(MCRJobQueue::getAction)
                .map(Class::getName)
                .map(Queue::new)
                .collect(Collectors.toList()));

        return Response
            .ok()
            .status(Response.Status.OK)
            .entity(queuesEntity)
            .build();
    }

    /**
     * REST resource for a specific job queue.
     * @param name the name of the job queue
     * @param status a filter for status of the jobs to return (optional)
     * @param parameters a filter for parameters of the jobs to return (optional)
     * @param offset the start index of the jobs to return (optional)
     * @param limit the maximum number of jobs to return (optional)
     * @return response with the jobs as JSON or XML
     */
    @GET()
    @Path("{name:.+}")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    @MCRAccessControlExposeHeaders(X_TOTAL_COUNT_HEADER)
    @MCRRequireTransaction()
    public Response queueJSON(@PathParam("name") String name,
        @QueryParam("status") List<MCRJobStatus> status,
        @QueryParam("parameter") List<String> parameters,
        @QueryParam("offset") Integer offset,
        @QueryParam("limit") Integer limit) {

        HashMap<String, String> parameterMap = new HashMap<>();

        parameters.forEach(p -> {
            String[] split = p.split(":");
            if (split.length == 2) {
                parameterMap.put(URLDecoder.decode(split[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(split[1], StandardCharsets.UTF_8));
            }
        });

        Queue queue = new Queue(name);
        MCRJobDAO dao = MCRJobQueueManager.getInstance()
            .getJobDAO();

        Class<? extends MCRJobAction> action = null;
        try {
            action = MCRClassTools.forName(name);
        } catch (ClassNotFoundException e) {
            throw new NotFoundException(e);
        }

        int count = dao.getJobCount(action, parameterMap, status);

        queue.jobs = dao
            .getJobs(action, parameterMap, status, limit, offset)
            .stream()
            .map(Job::new)
            .collect(Collectors.toList());
        return Response
            .ok()
            .status(Response.Status.OK)
            .header(X_TOTAL_COUNT_HEADER, count)
            .entity(queue)
            .build();
    }

    @XmlRootElement(name = "queues")
    static class Queues {
        @XmlElement(name = "queue")
        List<Queue> queues;

        void add(Queue queue) {
            if (queues == null) {
                queues = new ArrayList<>();
            }

            queues.add(queue);
        }

        void addAll(List<Queue> queues) {
            if (this.queues == null) {
                this.queues = new ArrayList<>();
            }

            this.queues.addAll(queues);
        }
    }

    @XmlRootElement(name = "queue")
    static class Queue {
        @XmlAttribute
        String name;

        @XmlElement(name = "job")
        List<Job> jobs;

        Queue() {
        }

        Queue(String name) {
            this.name = name;
        }
    }

    @XmlRootElement(name = "job")
    static class Job {
        @XmlAttribute
        long id;

        @XmlAttribute
        String status;

        @XmlElement(name = "date")
        List<TypedDate> dates;

        @XmlElement(name = "parameter")
        List<Parameter> parameters;

        @XmlElement(name = "exception")
        String exception;

        @XmlElement(name = "tries")
        Integer tries;

        Job() {
        }

        Job(MCRJob job) {
            this.id = job.getId();
            // convert like jersey does, so url parameter looks same
            this.status = job.getStatus().toString().toUpperCase(Locale.ROOT);

            List<TypedDate> dates = new ArrayList<>();
            if (job.getAdded() != null) {
                dates.add(new TypedDate("added", job.getAdded()));
            }
            if (job.getStart() != null) {
                dates.add(new TypedDate("start", job.getStart()));
            }
            if (job.getFinished() != null) {
                dates.add(new TypedDate("finished", job.getFinished()));
            }
            if (job.getException() != null) {
                this.exception = job.getException();
            }
            if (job.getTries() != null) {
                this.tries = job.getTries();
            }

            this.parameters = job.getParameters().entrySet().stream().map(e -> new Parameter(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

            if (!dates.isEmpty()) {
                this.dates = dates;
            }
        }
    }

    @XmlRootElement(name = "date")
    static class TypedDate {
        @XmlAttribute
        String type;

        @XmlValue
        Date date;

        TypedDate() {
        }

        TypedDate(String type, Date date) {
            this.type = type;
            this.date = date;
        }
    }

    @XmlRootElement(name = "parameter")
    static class Parameter {
        @XmlAttribute
        String name;

        @XmlValue
        String value;

        Parameter() {
        }

        Parameter(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}
