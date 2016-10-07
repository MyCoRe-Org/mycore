# MyCoRe - IIIF

This module contains classes to help you implementing the 
IIIF-Image API 2.0(http://iiif.io/api/image/2.0/) and the 
IIIF Presentation API 2.0.(http://iiif.io/api/presentation/2.0/)



MyCoRe already contains two implementations MyCoRe-Iview contains a IIIF-Image implementation and MyCoRe-Mets contains a
IIIF-Presentation implementation(just a draft). 

+ MCR.IIIFImage.**\<impl_name\>**=**\<impl_class\>** where \<impl_name\> is your implemetation name e.g. **Iview** and \<impl_class\>
 is your class extending _org.mycore.iiif.image.impl.MCRIIIFImageImpl_ e.g. **_org.mycore.iview2.iiif.MCRIVIEWIIIFImageImpl_**.
Your class constructor should have one _String_ argument which you should pass to super(), otherwise the _getProperties()_ 
(see next point) will not work right or the _IIIFImageResource_ just crashes.

+ Now you can feed your impl with different properties MCR.IIIFImage.**\<impl_name\>**.**\<property_name\>**=**\<property_value\>**.
With the __getProperties()__ method of the _org.mycore.iiif.image.impl.MCRIIIFImageImpl_ class you cant now receive the 
value with the property name.

+ You can also use different implementation names with the same class and different properties and you get multiple 
implementations with different behavior.

+ All the above also works with the Presentation API just the property prefix is MCR.IIIFPresentation. and the class to 
extend is _MCRIIIFPresentationImpl_.

+ The web service is now available under http://my.mycore.repository.org/mycore_context/rsc/iiif/image/impl_name/ 
(Image API) and http://my.mycore.repository.org/mycore_context/rsc/iiif/presentation/impl_name/ (Presentation API)

