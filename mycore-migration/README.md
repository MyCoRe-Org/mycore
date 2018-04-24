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
