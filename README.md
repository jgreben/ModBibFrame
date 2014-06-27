ModBibFrame
===========

A Modified version of the marc2bibframe conversion tool based on a wrapper script proveded by Kevin Ford

This utility will accept a file of MARC bibliographic records as input and output modified BIBFRAME records. It uses the marc2bibframe XQuery modules (https://github.com/lcnetdev/marc2bibframe) as well as a Java wrapper all provided by Kevin Ford. The original Java wrapper is provided in a Gist here: https://gist.github.com/kefo/10416746

The ModBibFrame utility takes a file of MARC records and creates a DOM object of them using the marc4j utility (https://github.com/marc4j/marc4j). A modified JAR file is provided that has the most recently updated MarcXMLWriter source code. This allows the MARC records to be converted into A DOM object that is passed directly to the BIBFRAME conversion script. A final step in this process involves the modification of the BIBFRAME URIs into hashable strings based on the contents of the elements' child nodes. This modification step will allow for consitent URIs across batches of records. The output DOM document is passed directly to the ModBibFrame class for processing using JDOM, and an output stream is the result.

To run the ModBibFrame utility from within the xbin directory do:

java -classpath .:lib/myMarc4j.jar:lib/jdom-2.0.5.jar:lib/saxon9he.jar:lib/ojdbc14.jar Main /path/to/MARC/records.mrc [ Create hash from URI string (true | false) ] /path/to/log/directory/and/log.filename 

The output BIBFRAME record is a StreamResult, so redirect standard out and standard error as necessary.

Dependencies:
JDOM 2.0.5 (http://www.jdom.org/downloads/index.html)
Saxon-HE (http://saxon.sourceforge.net/)
Oracle JDBC (http://www.oracle.com/technetwork/database/enterprise-edition/jdbc-10201-088211.html)

Place all JAR files in the xbin/lib directory.

Benchmark:

To allow this untility to run on large batches of records adjust the Java heap as needed. For a batch of 100,000 records, 32GB of heap seemed to work well (-Xms32g -Xmx32g) and takes approximately 20 minutes to run.
