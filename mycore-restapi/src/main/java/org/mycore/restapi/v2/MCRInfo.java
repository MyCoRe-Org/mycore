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

package org.mycore.restapi.v2;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.mycore.common.MCRCoreVersion;
import org.mycore.restapi.converter.MCRInstantXMLAdapter;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@Path("/mycore")
@OpenAPIDefinition(
    tags = @Tag(name = MCRRestUtils.TAG_MYCORE_ABOUT, description = "informations about this repository"))
public class MCRInfo {

    private static final Date INIT_TIME = new Date();

    @Context
    Request request;

    @GET
    @Path("version")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Operation(description = "get MyCoRe version information",
        responses = {
            @ApiResponse(content = @Content(schema = @Schema(implementation = GitInfo.class)))
        },
        tags = MCRRestUtils.TAG_MYCORE_ABOUT)
    public Response getVersion() {
        Optional<Response> cachedResponse = MCRRestUtils.getCachedResponse(request, INIT_TIME);
        return cachedResponse.orElseGet(
            () -> Response.ok(new GitInfo(MCRCoreVersion.getVersionProperties())).lastModified(INIT_TIME).build());
    }

    @XmlRootElement
    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    static class GitInfo {
        private String branch;

        private BuildInfo build;

        private GitClosestTag closestTag;

        private GitCommitInfo commit;

        private boolean dirty;

        private Map<String, String> remoteURL;

        private Set<String> tags;

        private GitInfo() {
            //JAXB;
        }

        GitInfo(Map<String, String> props) {
            branch = props.get("git.branch");
            build = new BuildInfo(props);
            closestTag = new GitClosestTag(props);
            commit = new GitCommitInfo(props);
            dirty = Boolean.parseBoolean(props.get("git.dirty"));
            remoteURL = new LinkedHashMap<>();
            props.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith("git.remote."))
                .filter(e -> e.getKey().endsWith(".url"))
                .forEach(e -> {
                    String name = e.getKey().split("\\.")[2];
                    remoteURL.put(name, e.getValue());
                });
            tags = Stream.of(props.get("git.tags").split(",")).collect(Collectors.toSet());
        }

        @XmlElement
        public BuildInfo getBuild() {
            return build;
        }

        @JsonProperty(required = true)
        @XmlElement
        public GitClosestTag getClosestTag() {
            return closestTag;
        }

        @XmlElement
        public GitCommitInfo getCommit() {
            return commit;
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public boolean isDirty() {
            return dirty;
        }

        @JsonProperty(required = true)
        @XmlElement
        public Map<String, String> getRemoteURL() {
            return remoteURL;
        }

        @XmlElementWrapper(name = "tags")
        @XmlElement(name = "tag")
        public Set<String> getTags() {
            return tags;
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public String getBranch() {

            return branch;
        }
    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    public static class BuildInfo {
        private Instant time;

        private User user;

        private String host;

        private String version;

        private BuildInfo() {
            //JAXB
        }

        BuildInfo(Map<String, String> props) {
            time = Instant.parse(props.get("git.build.time"));
            user = new User(props.get("git.build.user.name"), props.get("git.build.user.email"));
            host = props.get("git.build.host");
            version = props.get("git.build.version");
        }

        @XmlAttribute
        @XmlJavaTypeAdapter(MCRInstantXMLAdapter.class)
        public Instant getTime() {
            return time;
        }

        @XmlElement
        public User getUser() {
            return user;
        }

        @XmlAttribute
        public String getHost() {
            return host;
        }

        @XmlAttribute
        public String getVersion() {
            return version;
        }
    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    public static class GitCommitInfo {
        private GitId id;

        private GitCommitMessage message;

        private Instant time;

        private User user;

        private GitCommitInfo() {
            //JAXB
        }

        GitCommitInfo(Map<String, String> props) {
            id = new GitId(props);
            message = new GitCommitMessage(props.get("git.commit.message.full"), props.get("git.commit.message.short"));
            time = Instant.parse(props.get("git.commit.time"));
            user = new User(props.get("git.commit.user.name"), props.get("git.commit.user.email"));
        }

        @JsonProperty(required = true)
        @XmlElement
        public GitId getId() {
            return id;
        }

        @JsonProperty(required = true)
        @XmlElement
        public GitCommitMessage getMessage() {
            return message;
        }

        @JsonProperty(required = true)
        @XmlAttribute
        @XmlJavaTypeAdapter(MCRInstantXMLAdapter.class)
        public Instant getTime() {
            return time;
        }

        @XmlElement
        public User getUser() {
            return user;
        }
    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    public static class GitClosestTag {
        private int commitCount;

        private String name;

        private GitClosestTag() {
            //JAXB
        }

        GitClosestTag(Map<String, String> props) {
            name = props.get("git.closest.tag.name");
            commitCount = Integer.parseInt(props.get("git.closest.tag.commit.count"));
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public int getCommitCount() {
            return commitCount;
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public String getName() {
            return name;
        }
    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    public static class GitId {
        String abbrev;

        String describeShort;

        String describe;

        String full;

        private GitId() {
            //JAXB
        }

        private GitId(Map<String, String> props) {
            abbrev = props.get("git.commit.id.abbrev");
            describeShort = props.get("git.commit.id.describe-short");
            describe = props.get("git.commit.id.describe");
            full = props.get("git.commit.id.full");
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public String getAbbrev() {
            return abbrev;
        }

        @JsonProperty(value = "describe-short", required = true)
        @XmlAttribute
        public String getDescribeShort() {
            return describeShort;
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public String getDescribe() {
            return describe;
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public String getFull() {
            return full;
        }
    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    public static class GitCommitMessage {
        private String full;

        private String shortMsg;

        private GitCommitMessage() {
            //JAXB
        }

        GitCommitMessage(String full, String shortMsg) {
            this.full = full;
            this.shortMsg = shortMsg;
        }

        @XmlValue
        public String getFull() {
            return full;
        }

        @XmlAttribute
        public String getShort() {
            return shortMsg;
        }
    }

    @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
    public static class User {
        private String name;

        private String email;

        private User() {
            //JAXB
        }

        User(String name, String email) {
            this.name = name;
            this.email = email;
        }

        @JsonProperty(required = true)
        @XmlAttribute
        public String getName() {
            return name;
        }

        @XmlAttribute
        public String getEmail() {
            return email;
        }
    }
}
