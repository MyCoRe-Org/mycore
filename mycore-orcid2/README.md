# MyCoRe ORCID Integration

## Overview

Module is based on the official [orcid-model](https://github.com/ORCID/orcid-model) and uses the section models.
The approach is based on OAuth.

## Configuration

Steps:

*   Register your application to get access to the [Public API](https://info.orcid.org/documentation/features/public-api/) or [Member API](https://info.orcid.org/documentation/features/member-api/) of ORCID.org

*   If possible, create a public read access token for use with the Public API. However, it is not mandatory.

*   Set the following properties, if necessary:
    *   MCR.ORCID2.OAuth.ClientID
    *   MCR.ORCID2.OAuth.ClientSecret
    *   MCR.ORCID2.OAuth.Scope (whitespace separated list of scopes, eg. '/read-limited /activities/update')
    *   MCR.ORCID2.Client.ReadPublicToken
    *   MCR.ORCID2.Client.V3.APIMode  (['public'|'member'] default is 'public')

### Scope

See https://info.orcid.org/faq/what-is-an-oauth-scope-and-which-scopes-does-orcid-support/ for available scopes. Note also, that you get an invalid scope error when you combine '/authenticate' with other 3-legged scopes because it is already included.

MCR.ORCID2.OAuth.Scope is not a mandatory property. You can also provide the scope as parameter using rsc/orcid/oauth/init.


## Examples

### Application configuration
Here an example of an complete ORCID sandbox configuration (ClientID and ClientSecret need to be changed), tested in MyCoRe UBO application.

    MCR.ContentTransformer.import.ORCID.Steps=import.ORCID2Works,import.MODS2MCRObj
    MCR.ContentTransformer.import.ORCID2Works.Class=org.mycore.ubo.importer.orcid.Orcid2WorksTransformer
    MCR.ORCID2.API.Resource.Packages=org.mycore.orcid2.rest.resources,org.mycore.orcid2.v3.rest.resources
    MCR.ORCID2.BaseURL=https://sandbox.orcid.org/
    MCR.ORCID2.Client.V3.APIMode=member
    MCR.ORCID2.Client.V3.ErrorHandler.Class=org.mycore.orcid2.v3.client.MCRORCIDClientErrorHandlerImpl
    MCR.ORCID2.Client.V3.MemberAPI=https://api.sandbox.orcid.org/v3.0
    MCR.ORCID2.Client.V3.PublicAPI=https://pub.sandbox.orcid.org/v3.0
    MCR.ORCID2.LinkURL=https://sandbox.orcid.org/
    MCR.ORCID2.Metadata.WorkInfo.SaveOtherPutCodes=false
    MCR.ORCID2.OAuth.ClientID=APP-GETYOUROWNCLIENTID
    MCR.ORCID2.OAuth.ClientSecret=12345678-1234-1234-1234-123456789012
    MCR.ORCID2.OAuth.Scope=/read-limited /activities/update
    MCR.ORCID2.Object.TrustedIdentifierTypes=
    MCR.ORCID2.PreFillRegistrationForm=true
    MCR.ORCID2.SupportedLanguageCodes=ar,cs,en,es,fr,it,ja,ko,pt,ru,zh_CN,zh_TW
    MCR.ORCID2.User.TrustedNameIdentifierTypes=orcid
    MCR.ORCID2.Work.PublishStates=published,
    MCR.ORCID2.Work.SourceURL=
    UBO.ORCID2.InfoURL=https://www.uni-due.de/ub/publikationsdienste/orcid.php


### Fetching works from Public API v3

    String orcid = "0000-0001-5065-6970";
    MCRORCIDReadClient client = MCRORCIDClientFactory.getInstance("V3").createPublicClient();

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
    MCRORCIDCredential credential = MCRORCIDSessionUtils.getCurrentUser().getCredentialByORCID("ORCID");
    MCRORCIDClient client = MCRORCIDClientFactory.getInstance("V3").createMemberClient(credential);

    // publish work to orcid
    long putCode = client.create(MCRORCIDSectionImpl.WORKS, work);
