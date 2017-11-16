# MyCoRe ORCID Integration

## Configuration
* [Register your application](https://support.orcid.org/knowledgebase/articles/343182) for access to the public API of ORCID.org  
* [Get a read-public access token](https://members.orcid.org/api/tutorial/read-orcid-records#readpub) as strongly recommended also for the public API. If you do not do this on your own, mycore-orcid will fetch the access token itself and log it as a warning.
* Set

    MCR.ORCID.OAuth.ClientID
    MCR.ORCID.OAuth.ClientSecret
    MCR.ORCID.OAuth.ReadPublicToken

## Usage

    MCRORCIDProfile orcid = new MCRORCIDProfile("0000-0001-5065-6970");
    MCRWorks works = orcid.getWorks();

    works.fetchSummaries();
    Element modsCollectionWithSummaries = works.buildMODSCollection();

    works.fetchDetails();
    Element modsCollectionWithDetails = works.buildMODSCollection();
