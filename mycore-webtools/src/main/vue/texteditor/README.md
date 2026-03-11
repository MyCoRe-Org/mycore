# MyCoRe Text Editor

A Vue 3 web component embedded in the MyCoRe Java application for viewing and
editing MyCoRe XML resources (objects, derivates, and classifications) via the
MyCoRe REST API v2.

## Features

- Syntax-highlighted XML editor with line numbers
- Breadcrumb navigation for objects, derivates, and their file contents
- Ctrl+Click on MCR IDs or file names to navigate directly to that resource
- Write-access detection — Update button only shown when the user has permission

## URL structure

| URL | Description |
|---|---|
| `/objects/{objectId}` | View/edit a MyCoRe object |
| `/objects/{objectId}/derivates/{derivateId}` | View/edit a derivate |
| `/objects/{objectId}/derivates/{derivateId}/contents` | Browse derivate files |
| `/objects/{objectId}/derivates/{derivateId}/contents/{file}` | View/edit a file |
| `/classifications/{classificationId}` | View/edit a classification |

## Development

In development mode, the app runs as a standalone Vue dev server proxying to a
local MyCoRe instance at `http://localhost:8080/jportal/`. If your application
runs on a different URL, update the base URL in `src/router/index.ts`. In
production it is built and served as part of the MyCoRe Java application.

Install dependencies:
```sh
yarn
```

Start dev server:
```sh
yarn dev
```

Type-check:
```sh
yarn type-check
```

### Example URLs (dev)

```
http://localhost:5173/objects/jportal_jpjournal_00000599
http://localhost:5173/objects/jportal_jpvolume_00118654/derivates/jportal_derivate_00201808
http://localhost:5173/classifications/jportal_class_00000004
```
