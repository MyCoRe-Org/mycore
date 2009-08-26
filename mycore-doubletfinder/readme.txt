The doublet finder is a tool to identifiy and remove/relink redundancy objects.
There are only a few steps to configure and use it in your mycore application.

##################################################
# Step 1 - configure module properties
#################################################
The following properties are neccesary to creating a redundancy map for an object type. This redundancy map could later be edited to define which 
records are really doublets and which are not.

Possible properties for each object type:
MCR.doubletFinder.{type}.fieldsToSort=xxx,xxx...        Sorting the results. This improves the speed of comparision, so its good to set sort fields.
														It is neccesary that the attribute "sortable=true" is set for all fields in the searchfields.xml
														which are defined here. 
MCR.doubletFinder.{type}.fieldsToCompare=xxx,xxx...     All addable fields could be set here. This comparision is much faster then xpath compare.
														The attribute "addable=true" has to be set in the searchfields.xml for each field which is defined
														here.
MCR.doubletFinder.{type}.xpathToCompare=xxx,xxx...      If a metadata is not accessible as a field, you can read the value with an xpath expression (very slow!)
MCR.doubletFinder.{type}.tableHead=xxx,xxx...           not needed for comparision, will be later used as headline for the web editor

A small example:
MCR.doubletFinder.person.fieldsToSort=headingLastName,headingFirstName
MCR.doubletFinder.person.fieldsToCompare=headingLastName,headingFirstName
MCR.doubletFinder.person.xpathToCompare=mycoreobject/metadata/def.gender/gender/text()
MCR.doubletFinder.person.tableHead=Nachname,Vorname,Geschlecht

The compare algorithm is pretty simple. Each compare part gets the field or xpath as key. When two object will be compared, each of the key/value pair
has to be equal. So if you have a person with the name Peter Musterman the key/value pair is:
headingLastName=Musterman
headingFirstName=Peter

##################################################
# Step 2 - creating the redundancy map
#################################################
To create a redundancy map for an object type you can use the web cli. The command is "MCRRedundancyCommands -> generate redundancy map for type {0} with map generator {1}".
The type has to be one of your predefined types in mycore.properties ('author' in docportal for example). The map generator is the implementation of how the redundancy map
will be created. By default there are two possibilities:
defaultGenerator: compares all objects of a type with each other
fastGenerator: compares object n with n - 1 (fieldsToSort has to be set!)

In general the fastGenerator is sufficient if you want a standard compare. And its much faster, >50k persons in 2 seconds, defaultGenerator needs about 60 seconds.

The created redundancy map is saved in build/webapps/doubletFinder/ as redundancy-{type}.xml.


##################################################
# Step 3 - working with the map
#################################################
To edit a generated redundancy map the doublet finder provides an web editor. You can show them by simply call "/doubletFinder/redundancy-{type}.xml". The xml file will
be transformed by the stylesheet redundancyMap.xsl.

The web editor is split into two modes. First, the overview perspective and second the detail view. The overview perspective again is split into open, closed and error
records. At all these perspectives you see a list of record groups where you can choose one to switch to the detail view. At detail view you have the chance to evaluate each
record in the group. The following states are possible:
not edited              -   the record is not edited until now, every record has this state after creation
doublet                 -   the record is a doublet. records with this state will be deleted and relinked to the original object
no doublet (original)   -   the record is not a doublet but the original record! all doublets will be linked to this one
error                   -   the record is not a doublet and not the original. the record will be ignored at further processes


##################################################
# Step 4 - remove redundancy
#################################################
To process the redundancy map there is another cli command "clean up redundancy in database for type {0}". After executing this all doublets will be removed and their 
associated objects are relinked to the orignal. The redundancy-{xml}.xml removes all groups which are passed. So it is possible to execute this command with an unfinished
map.




##################################################
# Customize the comparator & string formatter
#################################################
The doubletfinder implements the possibility to use your own comparator and string formatter. This is usefull if you dont want an exact compare. The code below shows how
this could look like.

    MCRAdvancedFormatter formatter = new MCRAdvancedFormatter();
    MCRAdvancedComparator comparator = new MCRAdvancedComparator();
    generator.setStringFormatter(formatter);
    generator.setComparator(comparator);

    /**
     *  Replaces all special characters and whitespaces with asterisks.
     */
    private static class MCRAdvancedFormatter implements MCRRedundancyFormattable<String> {
        @Override
        public String format(String stringToFormat) {
            if (stringToFormat == null || stringToFormat.length() < MIN_CHARS) {
                return stringToFormat;
            }
            // replace all non words and all whitespaces with *
            String returnString = stringToFormat.replaceAll("\\W|\\s|$", "*");
            // remove all duplicate *
            for(int i = 0; i < returnString.length() - 1; i++) {
                char cC = returnString.charAt(i);
                char nC = returnString.charAt(i + 1);
                if((i == 0 && cC == '*') || (cC == '*' && nC == '*')) {
                    returnString = returnString.substring(0, i) + returnString.substring(i + 1);
                    i--;
                }
            }
            // no sense to return a to small string
            if(returnString.length() < MIN_CHARS)
                return stringToFormat;
            return returnString;
        }
    }

    /**
     *  Tries to do a pattern compare, if failed do an equal compare.
     */
    private static class MCRAdvancedComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if(o1.length() < MIN_CHARS)
                return simpleCompare(o1, o2);

            try {
                return patternCompare(o1, o2);
            } catch(PatternSyntaxException pse) {
                // cause the pattern string was not 100% correct,
                // do a simple equals compare
                return simpleCompare(o1, o2);
            }
        }

        protected int simpleCompare(String o1, String o2) {
            if(o1.equals(o2))
                return 0;
            return -1;
        }
        protected int patternCompare(String o1, String o2) {
            Pattern pattern = Pattern.compile(o1);
            Matcher m = pattern.matcher(o2);
            if(m.find())
                return 0;
            return -1;
        }
    }
