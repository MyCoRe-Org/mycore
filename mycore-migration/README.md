# 2017.06 -> 2018.06


## mycore-pi

- Every property starting with `MCR.PI.Registration.` does now start with `MCR.PI.Service.`
- Every property which ends with `MetadataManager` or `Inscriber` does now end with `MetadataService`
  
- metadata services
  - MCRPersistentIdentifierMetadataManager -> MCRPIMetadataService
  - MCRURNObjectXPathMetadataManager -> MCRPIXPathMetadataService
  - MCRMODSDOIPersistentIdentifierMetadataManager -> MCRMODSDOIMetadataService
  - MCRMODSPURLPersistentIdentifierMetadataManager -> MCRMODSPURLMetadataService
  - MCRMODSURNPersistentIdentifierMetadataManager -> MCRMODSURNMetadataService

- pi registration services
  - MCRPIRegistrationService -> MCRPIService
  - MCRPIJobRegistrationService -> MCRPIJobService
  - MCRPIRegistrationServiceManager -> MCRPIServiceManager
  - MCRDOIRegistrationService -> MCRDOIService
  - MCRPURLJobRegistrationService -> MCRPURLJobService
  - MCRPURLRegistrationService -> MCRPURLService
  - MCRURNGranularOAIRegistrationService -> MCRURNGranularOAIService
  - MCRURNOAIRegistrationService -> MCRURNOAIService
  - MCRURNGranularRESTRegistrationService -> MCRURNGranularRESTService
  
- other
  - MCRPersistentIdentifierGenerator -> MCRPIGenerator
  - MCRPersistentIdentifierResolver -> MCRPIResolver
  - MCRLocalPersistentIdentifierResolver -> MCRLocalPIResolver
  - MCRPersistentUniformResourceLocator -> MCRPURL
  - MCRPersistentIdentifierParser -> MCRPIParser
  - MCRPersistentIdentifierManager -> MCRPIManager

- All Generators now work with MCRBase instead of MCRObjectID 
- MCRDOIService now maybe needs a job user property (check javadoc)

## mycore-mets

The fileGroups for TEI changed from 

``` 
<!-- 2017.06 -->
<mets:fileGrp USE="TRANSCRIPTION" />
<mets:fileGrp USE="TRANSLATION"/>
``` 
to 
```
<!-- 2018.06 -->
<mets:fileGrp USE="TEI.TRANSCRIPTION" />
<mets:fileGrp USE="TEI.TRANSLATION.DE" />
<mets:fileGrp USE="TEI.TRANSLATION.EN" />
``` 


There is also a new property:
```
# used to detect which translation languages are allowed (mets-generation) (default values de, en)
MCR.METS.Allowed.Translation.Subfolder=de,en
```

To migrate old mets-files we have a command:
```migrate tei entries in mets file of derivate {0}```

To find all affected derivates you can use this command:
```select objects with solr query +{!join from=derivateID to=id}fileName:mets.xml +{!join from=derivateID to=id}filePath:\/tei\/*```

A message like this should appear: `INFO: 2 objects selected`

Then you can use the following command to apply the migration command to all found objects:
```
execute for selected migrate tei entries in mets file of derivate {x}
```

If you don't do that you will receive this message in the viewer console:
```Unknown File Group : TRANSCRIPTION```

