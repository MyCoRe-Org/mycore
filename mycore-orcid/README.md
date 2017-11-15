# MyCoRe ORCID Integration

## Configuration
* [Register your application](https://support.orcid.org/knowledgebase/articles/343182) for access to the public API of ORCID.org  
* [Get a read-public access token](https://members.orcid.org/api/tutorial/read-orcid-records#readpub) as strongly recommended also for the public API:
* Set MCR.ORCID.ReadPublicToken property

## Usage

    MCRORCIDProfile orcid = new MCRORCIDProfile("0000-0001-5065-6970");
    MCRWorks works = orcid.getWorks();

    works.fetchSummaries();
    Element modsCollectionWithSummaries = works.buildMODSCollection();

    works.fetchDetails();
    Element modsCollectionWithDetails = works.buildMODSCollection();
