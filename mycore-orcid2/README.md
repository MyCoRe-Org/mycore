# MyCoRe ORCID Integration

## Overview

The approaches are based on the official [orcid-model](https://github.com/ORCID/orcid-model) and use the section models.

## Configuration

Steps:

*   Register your application to get access to the [Public API](https://info.orcid.org/documentation/features/public-api/) or [Member API](https://info.orcid.org/documentation/features/member-api/) of ORCID.org

*   If possible, create a public read access token for use with the Public API. However, it is not mandatory.

*   Set the following properties, if necessary:
    *   MCR.ORCID2.OAuth.ClientID
    *   MCR.ORCID2.OAuth.ClientSecret
    *   MCR.ORCID2.OAuth.Scope
    *   MCR.ORCID2.Client.ReadPublicToken

## Examples

### Fetching works from Public API v3

    String orcid = "0000-0001-5065-6970";
    MCRORCIDReadClient client = MCRORCIDClientFactory.getInstance("v3").createPublicClient();

    // fetch work summary
    List<WorkSummary> summaries = client.fetch(orcid, MCRORCIDSectionImpl.WORKS, Works.class).getWorkGroup()
            .stream().flatMap(g -> g.getWorkSummary().stream()).toList();

    // fetch details
    long[] putCodes = summaries.stream().map(WorkSummary::getPutCode).mapToLong(Long::valueOf).toArray();
    List<Work> works = (List<Work>)(List<?>) client
            .fetch(orcid, MCRORCIDSectionImpl.WORKS, WorkBulk.class, putCodes).getBulk();

    // transform work to mods
    MCRContent mods = MCRORCIDWorkTransformerHelper.transformWork(works.get(0));

### Publishing work into current user's ORCID profile

    // transform object to work
    MCRObjectID objectID = null // TODO
    MCRContent object = MCRXMLMetadataManager.instance().retrieveContent(objectID);
    Work work = MCRORCIDWorkTransformerHelper.transformContent(object);

    // initialize member client for current user
    MCRORCIDCredentials credentials = MCRORCIDSessionUtils.getCurrentUser().getCredentials("ORCID");
    MCRORCIDClient client = MCRORCIDClientFactory.getInstance("v3").createMemberClient(credentials);

    // publish work to orcid
    long putCode = client.create(MCRORCIDSectionImpl.WORKS, work);
