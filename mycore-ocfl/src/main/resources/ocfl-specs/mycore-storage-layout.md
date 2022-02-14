# MyCoRe Storage Layout
-   Extension Name: mycore-storage-layout
-   Author: Robert Stephan, Tobias Lenhardt
-   Minimum OCFL Version: 1.0
-   OCFL Community Extensions Version: n/a
-   Obsoletes: MCRLayout
-   Obsoleted by: n/a

## Overview

This storage root extension maps OCFL Objects by segmenting their ID via the namespace, and on Objects and Derivates, the predefined SlotLayout or calculated one from the NumberPattern. This Layout works mostly like the native XML Store.

## Parameters
> Configuration is for the MyCoRe implementation only

-   **Name:** NumberPattern
    -   **Description:** The Pattern of the ObjectIDs
    -   **Type:** string
    -   **Configuration:** `MCR.Metadata.ObjectID.NumberPattern`
    -   **Constraints:** zeros (`0`) only
    -   **Default:** `0000000000`

-   **Name:** SlotLayoutClass
    -   **Description:** The Structure of the Object ID for generating the Directory, separated by dashes (`-`)
    -   **Type:** string
    -   **Configuration:** `MCR.IFS2.Store.class.SlotLayout`
    -   **Constraints:** Layers must be separated by `-`
    -   **Default:** `"(Length of NumberPattern - 4)-2-2"`

-   **Name:** SlotLayoutDerivate
    -   **Description:** The Structure of the Object ID for generating the Directory, separated by dashes (`-`)
    -   **Type:** string
    -   **Configuration:** `MCR.IFS2.Store.derivate.SlotLayout`
    -   **Constraints:** Layers must be separated by `-`
    -   **Default:** `"(Length of NumberPattern - 4)-2-2"`

## Example

### Configuration
|     Parameter      | Setting  |
| :----------------: | :------: |
|   NumberPattern    | 00000000 |
|  SlotLayoutClass   |  4-2-2   |
| SlotLayoutDerivate |  4-2-2   |

### Mappings
|                Object ID                |            Object Root Path            |
| :-------------------------------------- | :------------------------------------- |
| mcrobject:DocPortal_document_00000001   | mcrobject/DocPortal/document/0000/00   |
| mcrobject:DocPortal_document_12345678   | mcrobject/DocPortal/document/1234/56   |
| mcrderivate:DocPortal_derivate_00000002 | mcrderivate/DocPortal/derivate/0000/00 |
| mcrclass:Project_Classification         | mcrclass/Project_Classification        |


### Storage Hierarchy
```yaml
[storage root]
├── 0=ocfl_1.0
├── extensions
│   └── mycore-storage-layout
│       └── config.json
├── mycore-storage-layout.md
├── ocfl_1.0.txt
├── ocfl_extensions_1.0.md
├── ocfl_layout.json
├── mcrderivate
│   └── Project
│       └── derivate
│           ├── 0000
│           │   └── 01
│           │       ├── Project_derivate_00000101
│           │       │   └── ... [object root]
│           │       ├── Project_derivate_00000109
│           │       │   └── ... [object root]
│           │       └── Project_derivate_00000110
│           │           └── ... [object root]
│           └── 1234
│               └── 56
│                   └── Project_derivate_12345678
│                       └── ... [object root]
├── mcrobject
│   └── Project
│       └── doctype
│           ├── 0000
│           │   └── 01
│           │       ├── Project_doctype_00000101
│           │       │   └── ... [object root]
│           │       ├── Project_doctype_00000109
│           │       │   └── ... [object root]
│           │       └── Project_doctype_00000110
│           │           └── ... [object root]
│           └── 1234
│               └── 56
│                   └── Project_doctype_12345678
│                       └── ... [object root]
├── mcrclass
│   └── rfc5646
│       └── ... [object root]
├── mcruser
│   └── editor1A@local
│       └── ... [object root]
├── mcracl
│   └── rules
│       └── ... [object root]
└── mcrweb
    └── pages
        └── ... [object root]
```