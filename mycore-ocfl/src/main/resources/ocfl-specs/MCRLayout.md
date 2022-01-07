# File Layout using the MyCoRe SlotLayers
-   Extension Name: MCRLayout
-   Author: Tobias Lenhardt
-   Minimum OCFL Version: 1.0
-   OCFL Community Extensions Version: n/a
-   Obsoletes: n/a
-   Obsoleted by: n/a

## Overview

This storage root extension maps OCFL objects by segmenting their ID via the SlotLayout to bring back the original MyCoRe File Structure

## Configuration
`MCR.Metadata.ObjectID.NumberPattern`\
default: `0000000000`

`MCR.IFS2.Store.<Type>.SlotLayout`\
default: `4-2-2`

## Structure Sample

```yaml
[storage root]
├── 0=ocfl_1.0
├── extensions
│   └── MCRLayout
│       └── config.json
├── MCRLayout.md
├── ocfl_1.0.txt
├── ocfl_extensions_1.0.md
├── ocfl_layout.json
└── DocPortal
    ├── document
    │   ├── 0000
    │   │   └── 00
    │   │       ├── DocPortal_document_00000001
    │   │       │   └── ... [object root]
    │   │       └── DocPortal_document_00000002
    │   │           └── ... [object root]
    │   └── 1234
    │       ├── 56
    │       │   ├── DocPortal_document_12345678
    │       │   │   └── ... [object root]
    │       │   └── DocPortal_document_12345699
    │       │       └── ... [object root]
    │       └── 98
    │           ├── DocPortal_document_12349812
    │           │   └── ... [object root]
    │           └── DocPortal_document_12349855
    │               └── ... [object root]
    └── derivate
        └── 0000
            └── 00
                ├── DocPortal_derivate_00000001
                │   └── ... [object root]
                └── DocPortal_derivate_00000002
                    └── ... [object root]
```