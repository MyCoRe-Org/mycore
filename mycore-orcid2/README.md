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
    *   MCR.ORCID2.v3.ReadPublicToken

*   The Client talks default to the Production APIs. The developer API can be activated with`MCR.ORCID2.v3.IsSandbox=true`. Therefore, developer Credentials are required

## Examples

### Fetching Works from Public API

    String orcid = "0000-0001-5065-6970";
    MCRORCIDReadClient client = MCRORCIDAPIClientFactoryImpl.getInstance().createPublicClient();

    // fetch work summary
    List<WorkSummary> summaries = client.fetch(orcid, MCRORCIDSectionImpl.WORKS, Works.class).getWorkGroup()
            .stream().flatMap(g -> g.getWorkSummary().stream()).toList();

    // fetch details
    long[] putCodes = summaries.stream().map(WorkSummary::getPutCode).mapToLong(Long::valueOf).toArray();
    List<Work> works = (List<Work>)(List<?>) client
            .fetch(orcid, MCRORCIDSectionImpl.WORKS, WorkBulk.class, putCodes).getBulk();

    // transform work to mods
    Element mods = MCRORCIDWorkTransformer.getInstance().convertToMODS(work.get(0));

### Publishing work into current user's ORCID profile

    // transform object to work
    MCRObjectID objectID = null // TODO
    MCRContent object = MCRXMLMetadataManager.instance().retrieveContent(objectID);
    Work work = MCRORCIDWorkTransformer.getInstance().convertObject(object);

    // initialize member client for current user
    MCRORCIDCredentials credentials = MCRORCIDSessionUtils.getCurrentUser().getCredentials();
    MCRORCIDClient client = MCRORCIDAPIClientFactoryImpl.getInstance().createMemberClient(credentials);

    // publish work to orcid
    long putCode = client.create(MCRORCIDSectionImpl.WORKS, work);
