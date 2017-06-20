/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 3
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
package org.mycore.services.queuedjob;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.mycore.frontend.jersey.filter.access.MCRRestrictedAccess;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * @author Ren\u00E9 Adler (eagle)
 *
 */
@Path("jobqueue")
@Singleton
public class MCRJobQueueResource {

    @GET()
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    public Response listJSON() {
        try {
            Queues queuesEntity = new Queues();
            queuesEntity.addAll(
                MCRJobQueue.INSTANCES.keySet().stream().map(n -> new Queue(n)).collect(Collectors.toList()));

            return Response.ok().status(Response.Status.OK).entity(queuesEntity)
                .build();
        } catch (Exception e) {
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.name()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }

    @GET()
    @Produces(MediaType.APPLICATION_XML)
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    public Response listXML() {
        try {
            Queues queuesEntity = new Queues();
            queuesEntity.addAll(
                MCRJobQueue.INSTANCES.keySet().stream().map(n -> new Queue(n)).collect(Collectors.toList()));

            return Response.ok().status(Response.Status.OK).entity(toJSON(queuesEntity))
                .build();
        } catch (Exception e) {
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.name()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }

    @GET()
    @Path("{name:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    public Response queueJSON(@PathParam("name") String name) {
        try {
            Queue queue = MCRJobQueue.INSTANCES.entrySet().stream().filter(e -> e.getKey().equals(name)).findFirst()
                .map(e -> {
                    Queue q = new Queue(e.getKey());

                    MCRJobQueue jq = e.getValue();
                    Iterable<MCRJob> iterable = () -> jq.iterator(null);
                    q.jobs = StreamSupport.stream(iterable.spliterator(), false).map(j -> new Job(j))
                        .collect(Collectors.toList());

                    return q;
                }).orElse(null);

            return Response.ok().status(Response.Status.OK).entity(toJSON(queue))
                .build();
        } catch (Exception e) {
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.name()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }

    @GET()
    @Path("{name:.+}")
    @Produces(MediaType.APPLICATION_XML)
    @MCRRestrictedAccess(MCRJobQueuePermission.class)
    public Response queueXML(@PathParam("name") String name) {
        try {
            Queue queue = MCRJobQueue.INSTANCES.entrySet().stream().filter(e -> e.getKey().equals(name)).findFirst()
                .map(e -> {
                    Queue q = new Queue(e.getKey());

                    MCRJobQueue jq = e.getValue();
                    Iterable<MCRJob> iterable = () -> jq.iterator(null);
                    q.jobs = StreamSupport.stream(iterable.spliterator(), false).map(j -> new Job(j))
                        .collect(Collectors.toList());

                    return q;
                }).orElse(null);

            return Response.ok().status(Response.Status.OK).entity(queue)
                .build();
        } catch (Exception e) {
            final StreamingOutput so = (OutputStream os) -> e
                .printStackTrace(new PrintStream(os, false, StandardCharsets.UTF_8.name()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(so).build();
        }
    }

    private <T> String toJSON(T entity) throws JsonGenerationException, JsonMappingException, IOException {
        StringWriter sw = new StringWriter();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JaxbAnnotationModule());
        mapper.writeValue(sw, entity);

        return sw.toString();
    }

    // JAXB Wrapper Classes

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

        public Job() {
        }

        public Job(MCRJob job) {
            this.id = job.getId();
            this.status = job.getStatus().toString().toLowerCase(Locale.ROOT);

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
