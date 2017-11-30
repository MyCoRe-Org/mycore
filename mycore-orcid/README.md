# MyCoRe ORCID Integration

## Configuration
* [Register your application](https://support.orcid.org/knowledgebase/articles/343182) for access to the public API of ORCID.org  
* [Get a read-public access token](https://members.orcid.org/api/tutorial/read-orcid-records#readpub) as strongly recommended also for the public API. If you do not do this on your own, mycore-orcid will fetch the access token itself and log it as a warning.
* Set

    MCR.ORCID.OAuth.ClientID
    MCR.ORCID.OAuth.ClientSecret
    MCR.ORCID.OAuth.ReadPublicToken

## Fetching publications from an ORCID profile 

    MCRORCIDProfile orcid = new MCRORCIDProfile("0000-0001-5065-6970");
    
    MCRWorksSection worksSection = orcid.getWorksSection();
    Element modsCollectionWithSummaries = worksSection.buildMODSCollection();

    worksSection.fetchDetails();
    Element modsCollectionWithDetails = worksSection.buildMODSCollection();

    # Iterate through all grouped works:
    for (MCRGroupOfWorks group : worksSection) {
        for (MCRWork work : group.getWorks()) {
            String putCode = work.getPutCode();
            Element mods = work.getMODS();
        }
    }

## Publishing works into an ORCID profile

    MCRORCIDProfile orcid = new MCRORCIDProfile("0000-0001-5065-6970");
    orcid.setAccessToken("xxxxxx-xxxx-xxxxxxx-xxxx-xxxx"); // Token for scope /activities/update, get that from user
    
    MCRWorksSection worksSection = orcid.getWorksSection();
    
    # create publication:
    MCRWork work = worksSection.addWorkFrom(MCRObjectID.getInstance("mir_mods_00000001"));
    
    # update publication:
    work.update();
    
    # delete publication:
    work.delete();
