#!/bin/ksh
#
# Shell script to start the MyCoRe Command Line Interface

mcr1=$1
mcr2=$2
mcr3=$3
mcr4=$4

#mcr1=query

#mcr2=local
#mcr2=remote

#mcr3=document
#mcr3=legalentity

#mcr4="x"
#mcr4="(metadata[.=\"Zug\"])"
#mcr4="(metadata[.=\"Zug\"])"
#mcr4="(metadata[.=\"Zug\"])"
#mcr4="(metadata[.=\"*Zug*\"])"
#mcr4="(metadata/titles[title=\"99\" and @type=\"Maintitle\" and @type=\"Haupttitel\"])"
#mcr4="(metadata/rights[right=\"Kupferschmidt Jens\"])"
#mcr4="(metadata/rights[right=\"Kupfer* J*s\"])"
#mcr4="(metadata/rights[right=\"Jens\"]) or (metadata/rights[right=\"Max\"])" 
#mcr4="(metadata/rights[right=\"Kupfer*\"]) and (metadata/rights[right=\"Max*\"])" 
#mcr4="(metadata/titles[title=\"Schoenheide\"]) and (metadata/rights[right=\"Kupferschmidt\"])"
#mcr4="(metadata/personnames/personname[surename=\"Kupfi\" and @type=\"Nickname\"])"
#mcr4="(metadata/dates[date=\"24.02.1964\" and @type=\"Birthday\"])"
#mcr4="(metadata/dates[date!=\"24.02.1964\" and @type=\"Birthday\"])"
#mcr4="(metadata/dates[date<\"24.03.1964\" and @type=\"Birthday\"])"
#mcr4="(metadata/dates[date>\"24.01.1964\" and @type=\"Birthday\"])"
#mcr4="(metadata/dates[date>=\"23.02.1964\" and @type=\"Birthday\"]) or (metadata/personnames/personname[surename=\"Kupfi\" and @type=\"Nickname\"])"
#mcr4="(metadata/publishers[publisher=\"Jens\"])"
#mcr4="(metadata/publishers[publisher=\"MyCoReDemoDC_LegalEntity_1\"])"

java mycore.commandline.MCRCommandLineInterface $mcr1 $mcr2 $mcr3 $mcr4 $5 $6 $7 $8 $9

