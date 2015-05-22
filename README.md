ModBibFrame
===========

A Modified version of the marc2bibframe conversion tool based on a wrapper script proveded by Kevin Ford

This utility will accept a file of MARC bibliographic records as input and output modified Bibframe records. It uses the marc2bibframe XQuery modules (https://github.com/lcnetdev/marc2bibframe) as well as a Java wrapper all provided by Kevin Ford (https://github.com/kefo). The original Java wrapper is provided in a Gist: https://gist.github.com/kefo/10416746

The ModBibFrame utility is a three-step process that takes a file of MARC records and 1) creates a DOM object of them using the marc4j utility (https://github.com/marc4j/marc4j). This allows the MARCXML records to be passed directly to 2) the Bibframe conversion modules. The output of marc2bibframe in this context is also a DOM that is then 3) passed directly to the ModBibFrame class for post-processing using JDOM2 and final output.

The post-processing involves the replacement of the default generated Bibframe URIs into local URI's with hashable strings that are created using the element's text content. A mapping object is used to keep track of the default generated LC URIs and turn them into the new local URIs. This allows for consitently linking URIs across batches of records. The output Bibframe RDF is a StreamResult, so redirect standard out and standard error as necessary.

To run the ModBibFrame utility from within the xbin directory do:

java -classpath .:lib/myMarc4j.jar:lib/jdom-2.0.5.jar:lib/saxon9he.jar:lib/ojdbc14.jar Main /path/to/MARC/records.mrc

The files in the conf directory are text files that let the utility know which Bibframe element text you choose to be rolled up into a `rdf:about` or `rdf:resource` URI. The file name corresponds to the Bibframe element to be modified. Add new files with the appropriate name and text element names as needed for new elements. The files indicated by the _resources suffix tell the utility to convert the corresponding rdf:resource URI to match the rdf:about URI.

Customize the conf/conversion.conf file to set the appropriate outcome for the log path, base uri, whether to create bnodes, whether to create an MD5 hash, and whether to use (ans subsequently cleanup) Authority keys as part of the hasAuthority element (see below).

-----

Dependencies:

JDOM 2.0.5 (http://www.jdom.org/downloads/index.html)
Saxon-HE (http://saxon.sourceforge.net/)
Oracle JDBC (http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-10201-088211.html)

Place all JAR files in the xbin/lib directory.

-----

Optionally Looking up Authority IDs to roll up into URI strings:

(The following applies to all <bf:hasAuthority> elements except <bf:Topic>)

The server.conf file is a special file that allows you to do a database lookup for an LC (or other) authority ID based on a given system's authority key. The purpose of this is to create unique URIs for different People, Organizations, Places, Events, etc. that may have the same name but are distinguished by an authority ID.

This will only work for dumped MARC records that are output with an authority key in the '=' subfield of the output record (for example by using the SirsiDynix Symphony catalogdump API [using the -z option]).

Unless modified and recompiled in LookupAuthID.java, the SQL must be in the form of:

select TABLE_ID_COL from TABLE_NAME where TABLE_KEY_COL = [the contents of the '=' subfield]

If your ILS system does not support this scheme and you cannot (or do not) want to modify the Java classes, simply leave the server.conf file as provided without values. If you have a different ILS and would like to see support for this functionality using your ILS, please contact Joshua Greben (jgreben@stanford.edu).

-----

Benchmark:

To allow this untility to run on large batches of records adjust the Java heap as needed. For a batch of 100,000 records, 32GB of heap seemed to work well (-Xms32g -Xmx32g) and takes approximately 20 minutes to run.
