#The MyCoRe-Viewer
The MyCoRe-Viewer is a client which can be used to view mycore-derivates or pdf files. 
The MyCoRe-Viewer can be used directly in MyCoRe and use the backend provided by MyCoRe-Iview, but it can also be used standalone which is described later.

##How can i build the MyCoRe-Viewer?
### Requirements
The MyCoRe-Viewer is mostly written in Typescript and uses Maven to build. The Typescript compiler runs in Node.js.

What you need :

*   Java
*   Maven
*   Node.js

You don't need to install Grunt or Typescript. Both will be downloaded in the build process.

### Build

To Build the Project you just need to run:
 
`mvn clean install`

Maven will copy the files, downloads dependencies, execute grunt, run tests and finally produce a jar file.

### Result
If you are a java developer you can simply use the created maven artifact by adding following lines to your pom.xml

      <dependency>
        <groupId>org.mycore</groupId>
        <artifactId>mycore-viewer</artifactId>
        <version>0.2-SNAPSHOT</version>
        <scope>runtime</scope>
      </dependency>
      
Deploy your webapp and the JS files are located in
`modules ▸ iview2`

If you are not a java developer you can ignore the jar file and just pick up the files located in:

`target ▸ classes ▸ META-INF ▸ resources ▸ modules ▸ iview2 `

##How can i run the examples?
To run the examples you just need to run:

`mvn jetty:run`

This starts a web server on localhost with port 9201. The examples are now located in:

`http://127.0.0.1:9201/classes/example/`

##How can i use the viewer?
### The MyCoRe Way
MyCoRe by default contains the new image viewer. The viewer is accessible through a jersey resource.
Just simple type     

`http://your.application.url/rsc/viewer/the.id.of.a.derivate/the.path.to.the.image    `

`e.g. http://archive.thulb.uni-jena.de/hisbest/rsc/viewer/HisBest_derivate_00012389/VD17-405363435_0001.tif`

But on the metadata page and in the search result list still the old viewer is linked. 
How you can change that is described below.

#### Docportal based Application
To use the new Viewer in your Docportal based application you just need to add the following property:

`MCR.Viewer.useNewViewer=true`

The new viewer is now accessible through the metadata page or the search results.

#### MIR based Application

TODO

#### I have a custom metadata page

TODO

## What can i configure?
If you use the Viewer in MyCoRe you can configure like described in MyCoRe Configuration.


### MyCoRe Configuration
You can change the following options in the mycore.properties. If you u.se the Viewer outside of MyCoRe you can only change Parameters listed in **Direct Configuration**.

####Logo Configuration

`MCR.Viewer.logo.URL=` a path of the logo relative to the base url (optional)

`MCR.Viewer.logo.css=` a path to a additional stylesheet for your logo (optional)
####Metadata Configuration

`MCR.Viewer.metadata.transformer = ` The viewer knows the object which holds the derivate. If you set this transformer the viewer will display the transformation result in the Structure overview. MyCoRe includes a default style for mods-viewer. e.g.
`MCR.ContentTransformer.mycoreobject-viewer.Class=` org.mycore.common.content.transformer.MCRXSLTransformer
`MCR.ContentTransformer.mycoreobject-viewer.Stylesheet=`xsl/mycoreobject-mods-pure.xsl,xsl/mods-pure-viewer.xsl

#### Direct Configuration in MyCoRe
By default MyCoRe includes/removes all the necessary scripts and css automatically. It also generates the HTML file and calls the constructor.
If you want to use the direct configuration in MyCoRe you need to implement your own **MCRViewerConfigurationStrategy**.
//TODO

### Direct Configuration
In direct configuration you need to include all the necessary modules(js files) and css files by your self.

You need to pass the following options as JSON object to the MyCoReViewer#Constructor

####Required Parameters

`doctype:string` tells the viewer which doctype to use. (mets or pdf)

`metsURL:string` tells the viewer where the mets.xml is located. (mets only)

`imageXmlPath:string` location of the imageinfo.xml. Derivate and Image will be added. (e.g. if the path is http://url.org/ it will load http://url.org/derivate/image/imageinfo.xml)

`tileProviderPath:string` location where the viewer can load tiles. Derivate and Image will be added to the path. You can add multiple comma separated paths for load balancing.

`filePath:string` location where the viewer can find the start file. If doctype == "pdf" it needs to point to the pdf. If doctype == "mets" it needs to point to the start image relative to the derivate.

`derivate:string` the id of the derivate which contains all the files.

`i18nURL:string` the url of the language provider. The viewer expects a JSON object which contains language as keys and the translated text as value. {lang} can be a placeholder for the language to load. (e.g. http://archive.thulb.uni-jena.de/hisbest/servlets/MCRLocaleServlet/{lang}/component.iview2.*)

`lang:string` a shorthand for the language e.g. de or en

`webApplicationBaseURL:string` the location where the viewer can receive the file located in `target ▸ classes ▸ META-INF ▸ resources`

####Optional Parameters

`chapter.enabled:boolean` enables the chapter in toolbar dropdown menu

`chapter.showOnStart:boolean` should the chapter be opened on start (only desktop viewer default: **true**)

`imageOverview.enabled:boolean` should the image overview be enabled in toolbar dropwdown menu (default: **true**)

`canvas.overview.enabled:boolean` if true the overview will be shown in the lower right corner (default: **true**)

`permalink.updateHistory:boolean` if true the url will update on image change (default: **true**)

`permalink.viewerLocationPattern:string`  the pattern used to build the location to the viewer. (default: **{baseURL}rsc/viewer/{derivate}/{file}**)
 


##How does the Viewer work?
The Viewer is a combination of different components, which are connected through a event system. Every component has a special purpose. 

### The MyCoReComponents
The MyCoReComponents are grouped in modules, every module in a separate javascript file. 
The modules should be only loaded if they are really needed.

Every MyCoReComponent has 4 Methods:

* **constructor**: needed to create a instance of the component
* **handlesEvents**: the viewer needs this to detect which component handles which events 
* **trigger**: triggers a event. 
* **handle**: will be called if a event of **handlesEvents** was triggered by a component

All MyCoReComponents that are available will be added to the global array **IVIEW_COMPONENTS**. If a instance of MyCoReViewer is created he iterates over the array and calls the **constructor** of every MyCoReComponent. The only argument which is passed is the settings object which was passed to the MyCoReViewer constructor. After calling the constructor, every **init** method will be called.

This short list describes the functionality of all standard MyCoReComponents

#### Basic MyCoReComponents
The Basic MyCoReComponents are loaded in every instance of the Viewer.

* **MyCoReChapterComponent** is used to display the structure tree in the sidebar. The structure can be read from the **StructureModel**

* **MyCoReI18NComponent** is used to load the language files and provide a LanguageModel. Its fires the language model loaded event, which can be used to translate things. 

* **MyCoReImageOverviewComponent** displays a list of images in the sidebar.
 
* **MyCoReImageScrollComponent** display the images in the middle of the viewer. It also manages the **PageLayouts** and **AbstractPage**s
 
* **MyCoRePermalinkComponent** provides a link to the user which contains the exact state of the viewer.
 
* **MyCoReToolbarComponent** builds and displays the **ToolbarModel**.
 
* **MyCoReContainerComponent** manages the border layout of the viewer.

#### Desktop MyCoReComponents
The Desktop MyCoReComponents are used when the viewer is displayed fullscreen in a desktop Browser.

* **MyCoReImageInformationComponent** displays the current zoom in %, the order number of the page and the rotation in ° in the bottom of the desktop viewer.
 
* **MyCoReDesktopToolbarProviderComponent** provides the **ToolbarModel** of the desktop viewer.

* **MyCoRePageDesktopLayoutProviderComponent** provides the **PageLayouts** of the desktop viewer.

* **MyCoReSearchComponent** provides a simple index which can be used to search through the document.

#### Mobile MyCoReComponents
The Mobile MyCoReComponents are used when the viewer is displayed on a mobile device.

* **MyCoReImagebarComponent** displays images of the derivate in a bar on the bottom of the mobile viewer, which can be used to navigate.

* **MyCoReMobileToolbarProviderComponent** provides the **ToolbarModel** of the mobile viewer.

* **MyCoRePageMobileLayoutProviderComponent** provides the **PageLayouts** of the mobile viewer.


#### METS MyCoReComponents

* **MyCoReMetsComponent** loads the METS file and parses it to a **StructureModel**.

* **MyCoRePrintComponents** can convert pages from the METS file to PDF.

* **MyCoReTiledImagePageProviderComponent**: Pages in mycore a separated in tiles. With the help of the imageinfo.xml of a Page these tiles can be combined. Provides TiledPages which implements the **AbstractPage** interface.

#### PDF MyCoReComponents

* **MyCoRePDFViewerComponent** loads the PDF and parses it to a **StructureModel**. It also provides PDFPages which implements the **AbstractPage** interface.


## How can i write a plugin?
TODO

