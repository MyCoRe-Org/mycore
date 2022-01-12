# MCRLayout: native MyCoRe Directory for OCFL
-   Extension Name: MCRLayout
-   Author: Tobias Lenhardt
-   Minimum OCFL Version: 1.0
-   OCFL Community Extensions Version: n/a
-   Obsoletes: n/a
-   Obsoleted by: n/a

## Overview

This storage root extension maps OCFL Objects by segmenting their ID via the predefined SlotLayout or calculated from the NumberPattern, using the same storage hierarchies as the native MyCoRe XML Store.

## Parameters
> Configuration is for the MyCoRe implementation only

-   **Name:** NumberPattern
    -   **Description:** The Pattern of the ObjectIDs
    -   **Type:** string
    -   **Configuration:** `MCR.Metadata.ObjectID.NumberPattern`
    -   **Constraints:** zeros (`0`) only
    -   **Default:** `0000000000`

-   **Name:** SlotLayout
    -   **Description:** The Structure of the Object ID for generating the Directory, separated by dashes (`-`)
    -   **Type:** string
    -   **Configuration:** `MCR.IFS2.Store.class.SlotLayout`
    -   **Constraints:** Layers must be separated by `-`
    -   **Default:** `"(Length of NumberPattern - 4)-2-2"`

## Example

### Configuration
|   Parameter   | Setting  |
| :-----------: | :------- |
| NumberPattern | 00000000 |
|  SlotLayout   | 4-2-2    |

### Mappings
|          Object ID          |      Object Root Path      |
| :-------------------------: | :------------------------: |
| DocPortal_document_00000001 | DocPortal/document/0000/00 |
| DocPortal_document_12345678 | DocPortal/document/1234/56 |
| DocPortal_derivate_00000002 | DocPortal/derivate/0000/00 |

### Storage Hierarchy
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