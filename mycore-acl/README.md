# mycore-acl

## Access Key
Access keys offer an alternative authorization mechanism. Currently, objects (mods) and derivatives are supported as reference types.

### REST API

This module provides a REST API for managing Access Keys. The REST API is currently in a draft state but can be activated as follows:

```bash
MCR.RestApi.Draft.MCRAccessKey2=true
```

#### Permissions

Managing access keys requires explicit permissions. These permissions consist of a tuple comprising the reference type and permission, e.g., `(mods, manage-access-key-read)`. Also, `(POOLPRIVILEGE, manage-access-key)` brings full access.

### Access Key Manager

This module provides an Access Key Manager, which can be integrated into the Vue Servlet as follows:

```xml
<servlet>
  <servlet-name>AccessKeyManager</servlet-name>
  <servlet-class>org.mycore.webtools.vue.MCRVueRootServlet</servlet-class>
  <init-param>
    <param-name>heading</param-name>
    <param-value>component.acl.accesskey.frontend.title.main</param-value>
  </init-param>
  <init-param>
    <param-name>properties</param-name>
    <param-value>MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes</param-value>
  </init-param>
</servlet>
<servlet-mapping>
  <servlet-name>AccessKeyManager</servlet-name>
  <url-pattern>/access-key-manager/*</url-pattern>
</servlet-mapping>
```

#### Usage

The Access Key Manager can be used by administrators to manage all access keys, or specifically for a particular reference. To manage a specific reference, the available permissions must be passed.

- `/access-key-manager/admin/`
- `/access-key-manager/admin/<reference>`
- `/access-key-manager/<reference>?availablePermissions=<permissions>`
