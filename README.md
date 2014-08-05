ModBibFrame
===========

A Modified version of the marc2bibframe conversion tool based on a wrapper script proveded by Kevin Ford

This utility will accept a file of MARC bibliographic records as input and output modified BIBFRAME records. It uses the marc2bibframe XQuery modules (https://github.com/lcnetdev/marc2bibframe) as well as a Java wrapper all provided by Kevin Ford. The original Java wrapper is provided in a Gist here: https://gist.github.com/kefo/10416746

The ModBibFrame utility takes a file of MARC records and creates a DOM object of them using the marc4j utility (https://github.com/marc4j/marc4j). A modified JAR file is provided that has the most recently updated MarcXMLWriter source code. This allows the MARC records to be converted into A DOM object that is passed directly to the BIBFRAME conversion script. A final step in this process involves the modification of the BIBFRAME URIs into hashable strings based on the contents of the elements' child nodes. This modification step will allow for consitent URIs across batches of records. The output DOM document is passed directly to the ModBibFrame class for processing using JDOM, and an output stream is the result.

To run the ModBibFrame utility from within the xbin directory do:

java -classpath .:lib/myMarc4j.jar:lib/jdom-2.0.5.jar:lib/saxon9he.jar:lib/ojdbc14.jar Main /path/to/MARC/records.mrc
<br>
<!--java -classpath .:lib/myMarc4j.jar:lib/jdom-2.0.5.jar:lib/saxon9he.jar:lib/ojdbc14.jar Main /path/to/MARC/records.mrc [ Create hash from URI string (true | false) ] /path/to/log/directory/and/log.filename [ baseURI (e.g. http://linked-data.stanford.edu) ]
-->
The output BIBFRAME record is a StreamResult, so redirect standard out and standard error as necessary.

Customize the conf/convert.conf file to set the appropriate outcome for the log path, base uri, whether to create bnodes, whether to create an MD5 hash, and whether to cleanup Authority keys.

-----

Dependencies:

JDOM 2.0.5 (http://www.jdom.org/downloads/index.html)
Saxon-HE (http://saxon.sourceforge.net/)
Oracle JDBC (http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-10201-088211.html)

Place all JAR files in the xbin/lib directory.

The files in the conf directory contain text files that let the utility know which BIBFRAME elements' text you want rolled up into the rdf:about URI string. The files indicated by the _resources suffix tell the utility to convert the corresponding rdf:resource URI to match the rdf:about URI.

-----

Looking up Authority IDs to roll up into URI strings:

(The following applies to all <bf:hasAuthority> elements except <bf:Topic>)

The server.conf file is a special file that allows you to do a database lookup for an LC (or other) authority ID based on a given system's authority key. The purpose of this is to create unique URIs for different People, Organizations, Places, Events, etc. that may have the same name but are distinguished by an authority ID.

This will work for MARC records output using the SirsiDynix Symphony catalogdump API (using the -z option) or with MARC records that contain an authority key in the '=' subfield of the output record.

Unless modified and recompiled in LookupAuthID.java, the SQL must be in the form of:

select TABLE_ID_COL from TABLE_NAME where TABLE_KEY_COL = [the contents of the '=' subfield]

If your ILS system does not support this scheme and you cannot (or do not) want to modify the Java classes, simply leave the server.conf file as provided without values. If you have a different ILS and would like to see support for this functionality using your ILS, please contact Joshua Greben (jgreben@stanford.edu).

-----

Benchmark:

To allow this untility to run on large batches of records adjust the Java heap as needed. For a batch of 100,000 records, 32GB of heap seemed to work well (-Xms32g -Xmx32g) and takes approximately 20 minutes to run.
