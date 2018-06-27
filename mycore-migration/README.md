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

### MCRGenericPIGenerator replaces MCRURNDateCounterGenerator

- MCRGenericPIGenerator replaces MCRURNDateCounterGenerator   
- MCRURNDateCounterGenerator did not work properly if you restarted your applcation
- MCRGenericPIGenerator generates identifiers for every type

Properties:

**GeneralPattern:**
```
MCR.PI.Generator.myGenerator.GeneralPattern=urn:nbn:de:gbv:$CurrentDate-$ObjectType-$ObjectNumber-$Count-
```

Supported parts:

- **$CurrentDate**    - replaced with the current date honoring the **DateFormat** property
- **$ObjectDate**     - replaced with the creation date of the object honoring the **DateFormat** property
- **$ObjectType**     - replaced with the type of the object. Can be mapped with **ObjectTypeMapping** property
- **$ObjectProject**   - replaced with the project id of the object. Can be mapped with **ObjectProjectMapping** property
- **$ObjectNumber**   - replaced with the number of the object id.
- **$Count**          - replaced with the count of the existing pi with the same parts listed above, honoring  the **CountPrecision** property

**DateFormat:**
```
MCR.PI.Generator.myGenerator.DateFormat=yyyy
```
The SimpleDateFormat which will be used to express $CurrentDate or $ObjectDate

**ObjectTypeMapping**
```
MCR.PI.Generator.myGenerator.TypeMapping=document:doc,disshab:diss,Thesis:Thesis,bundle:doc,mods:test
```
A comma separated list of key value pairs to map a object type to a specific string. The key ist the object type and the value the resulting string.
If none match the type is used as the string.

**ObjectProjectMapping**
```
MCR.PI.Generator.myGenerator.ObjectProjectMapping=openagrar:oa,zimport:oa
```
A comma separated list of key value pairs to map a project id to a specific string. The key ist the project id of the object and the value the resulting string.
If none match the project id is used as the string.

**CountPrecision**
```
MCR.PI.Generator.myGenerator.CountPrecision=3
```
A number indicating how many space the $Count needs. The value 3 will produce 001, 002, ... , 999. The value -1 leads to a normal count (1...999). 

**Type**
```
MCR.PI.Generator.myGenerator.Type=dnbURN
```
The type of the PI, used to parse the resulting string to a pi java object.

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

## mycore-mods


To migrate old mods-files we have a Stylesheet.
You need to select all mods-files first:
```
select objects with solr query objectType:mods
```

A message like this should appear: `INFO: 2 objects selected`

Then you can use the following command to apply the migration command to all found objects:
```
execute for selected xslt {x} with file resource:xsl/mycoreobject-migrate-language.xsl
```
