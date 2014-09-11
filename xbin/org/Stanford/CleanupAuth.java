package org.Stanford;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Namespace;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.DOMOutputter;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Scanner;
import java.util.Properties;
import java.sql.*;

public class CleanupAuth
{
    public static Pattern p1 = Pattern.compile("\\^A[0-9]+");
    public static Pattern p2 = Pattern.compile("[-\\s]+~[0-9]{1}");

    public static DOMBuilder builder = new DOMBuilder();
    public static LookupAuthID lookup = new LookupAuthID();
    public static Properties props = PropGet.getProps("conf/server.conf");
    public static Properties convProps = PropGet.getProps("conf/conversion.conf");
    public static Connection connection = lookup.OpenAuthDBConnection(props);

    public static String parseType = convProps.getProperty("PARSE");
    
    public static  ArrayList <String> getMadsType()
    {
        ArrayList <String> madsType = new ArrayList <String>(8);
        
        madsType.add("Occupation#"); 
        madsType.add("Temporal#");
        madsType.add("GenreForm#");
        madsType.add("Topic#");
        madsType.add("Language#");
        madsType.add("Name#");
        madsType.add("Geographic#");
        madsType.add("Title#");    
        
        return madsType;
    }

    public static org.w3c.dom.Document Cleanup(org.w3c.dom.Document rdfFile) throws Exception
    {
        System.err.println("\nCLEANING UP AUTHORITY KEYS");
        System.err.println("Parse Type is " + parseType);

        Document doc = (Document) builder.build(rdfFile);
        Element rootNode = doc.getRootElement();

        Namespace bf = rootNode.getNamespace("http://bibframe.org/vocab/");
        Namespace madsrdf = rootNode.getNamespace("http://www.loc.gov/mads/rdf/v1#");

        ArrayList<String> cleanupList = new ArrayList<String>(ModBibframe.getSubElementList("cleanup"));

        //Person
        List persons = rootNode.getChildren("Person", bf);
        removeAuthKeyfromString(persons, cleanupList, bf, madsrdf);

        //Agent
        List agents = rootNode.getChildren("Agent", bf);
        removeAuthKeyfromString(agents, cleanupList, bf, madsrdf);

        //Place
        List places = rootNode.getChildren("Place", bf);
        removeAuthKeyfromString(places, cleanupList, bf, madsrdf);

        //Organization
        List organizations = rootNode.getChildren("Organization", bf);
        removeAuthKeyfromString(organizations, cleanupList, bf, madsrdf);

        //Meeting
        List meetings = rootNode.getChildren("Meeting", bf);
        removeAuthKeyfromString(meetings, cleanupList, bf, madsrdf);

        //Event
        List events = rootNode.getChildren("Event", bf);
        removeAuthKeyfromString(events, cleanupList, bf, madsrdf);
        
        //Temporal
        List temporals = rootNode.getChildren("Temporal", bf);
        removeAuthKeyfromString(temporals, cleanupList, bf, madsrdf);

        //Topic
        List topics = rootNode.getChildren("Topic", bf);
        removeAuthKeyfromString(topics, cleanupList, bf, madsrdf);

        //Works
        ArrayList<String> worksCleanupList = new ArrayList<String>(ModBibframe.getSubElementList("works_cleanup"));

        List works = rootNode.getChildren("Work", bf);
        removeAuthKeyfromString(works, worksCleanupList, bf, madsrdf);

        DOMOutputter domOutput = new DOMOutputter();

        // display nice
        domOutput.setFormat(Format.getPrettyFormat().setLineSeparator("\n"));
        org.w3c.dom.Document cleanDoc = domOutput.output(doc);

        return cleanDoc;
    }

    public static void modMads(List listOfElements, String baseuri, boolean createHash, Namespace bf, Namespace rdf, Namespace madsrdf)
    {
        rdf = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");;
        bf = Namespace.getNamespace("bf", "http://bibframe.org/vocab/");
        madsrdf = Namespace.getNamespace("madsrdf", "http://www.loc.gov/mads/rdf/v1#");

        ArrayList <Element> listOfComponents = new ArrayList <Element>();

        ArrayList <String> madsType = getMadsType();

        //create the MADS/RDF schema and then find/replace the tag in the string
        Iterator <Element> elementIterator = listOfElements.iterator();
        while (elementIterator.hasNext())
        {
            try
            {
                Element element = elementIterator.next();

                Element hasAuthority = element.getChild("hasAuthority", bf);
                Element Authority = hasAuthority.getChild("Authority", madsrdf);
                Element authoritativeLabel = Authority.getChild("authoritativeLabel", madsrdf);
                Element authorizedAccessPoint = element.getChild("authorizedAccessPoint", bf);
                Element label = element.getChild("label", bf);

                String taggedAuth = authorizedAccessPoint.getText();

                String[] headings = taggedAuth.split("--");
                String authorityKey = "";
                String authorityID = "";
                String heading = "";
                String cleanHeading = "";
                String textHeading = "";
                String madsClass = "";
                String textToClean = "";
                String cleanText = "";
                String madsString = "";
                String predicate = "";
                String[] pair;

                if (headings.length > 1)
                {
                    //create the componentList
                    Element componentList = new Element("componentList", madsrdf);
                    Attribute parseTypeAttrib = new Attribute("parseType", parseType, rdf);
                    componentList.setAttribute(parseTypeAttrib);

                    if (parseType.equals("Collection"))
                    {
                        predicate = "about";
                    }
                    else if (parseType.equals("Resource"))
                    {
                        predicate = "resource";
                    }


                    for (int h=0; h < headings.length; h++)
                    {
                        pair = headings[h].split("#");

                        try 
                        {
                            madsClass = pair[0];
                            heading = pair[1];

                            authorityKey = lookup.getAuthorityKey(heading);

                            if (authorityKey != null && authorityKey != "")
                            {
                                authorityID = lookup.LookupAuthIDfromDB(authorityKey, connection, props);
                            }

                            if (!madsClass.equals("Topic"))
                            {
                                cleanHeading = stripPunctAndSpace(removeAuthKeyfromString(heading) + authorityID).toLowerCase();
                            }
                            else
                            {
                                cleanHeading = stripPunctAndSpace(heading).toLowerCase();
                            }

                            if (createHash)
                            {
                                cleanHeading = CreateHash.strToHash(cleanHeading, "MD5");
                            }

                            madsClass = stripPunctAndSpace(madsClass);
                            Element madsElement = new Element(madsClass, madsrdf);
                            Attribute componentAttrib = new Attribute(predicate, baseuri + cleanHeading, rdf);
                            madsElement.setAttribute(componentAttrib);

                            textHeading = removeAuthKeyfromString(heading);
                            Element textHeadingLabel = new Element("label", bf);
                            textHeadingLabel.addContent(textHeading);
                            madsElement.addContent(textHeadingLabel);

                            listOfComponents.add(madsElement);
                        }
                        catch (ArrayIndexOutOfBoundsException e)
                        {
                            //System.err.println(e.getMessage);
                        }
                    }

                    if (listOfComponents.size() > 1)
                    {
                        Authority.addContent(componentList);
                        componentList.addContent(listOfComponents);
                    }
                }

                //now cleanup the madsTypes from the label, title, and authorizedAccessPoint
                Iterator <String> madsTypeIterator = madsType.iterator();
                while (madsTypeIterator.hasNext())
                {
                    madsString = madsTypeIterator.next();
                    textToClean = authoritativeLabel.getText();
                    cleanText = textToClean.replaceAll(madsString, "");
                    authoritativeLabel.setText(cleanText);
                    label.setText(cleanText);
                    authorizedAccessPoint.setText(cleanText);
                }

                listOfComponents.clear();

            }
            catch (NullPointerException e)
            {
                //System.err.println(e.getMessage());
            }
        }
    }

    public static void cleanupWorks(List listOfElements, Namespace bf, Namespace madsrdf)
    {
        bf = Namespace.getNamespace("bf", "http://bibframe.org/vocab/");
        madsrdf = Namespace.getNamespace("madsrdf", "http://www.loc.gov/mads/rdf/v1#");

        String textHeading = "";
        String textToClean = "";
        String cleanText = "";
        String madsString = "";
        String pair[];

        ArrayList <String> madsType = getMadsType();
        
        //create the MADS/RDF schema and then find/replace the tag in the string
        Iterator <Element> elementIterator = listOfElements.iterator();
        while (elementIterator.hasNext())
        {
            Element element = elementIterator.next();

            Iterator <String> madsTypeIterator = madsType.iterator();
            while (madsTypeIterator.hasNext())
            {
                madsString = madsTypeIterator.next();
                try
                {
                    Element hasAuthority = element.getChild("hasAuthority", bf);
                    Element Authority = hasAuthority.getChild("Authority", madsrdf);
                    Element authoritativeLabel = Authority.getChild("authoritativeLabel", madsrdf);
                    textToClean = authoritativeLabel.getText();
                    if (!textToClean.equals(""))
                    {
                        cleanText = textToClean.replaceAll(madsString, "");
                        authoritativeLabel.setText(cleanText); 
                    }
                }
                catch (NullPointerException e)
                {}
                try
                {
                    Element authorizedAccessPoint = element.getChild("authorizedAccessPoint", bf);
                    textToClean = authorizedAccessPoint.getText();
                    if (!textToClean.equals(""))
                    {
                        cleanText = textToClean.replaceAll(madsString, "");
                        authorizedAccessPoint.setText(cleanText);
                    }
                }
                catch (NullPointerException e)
                {}
                try
                {
                    Element label = element.getChild("label", bf);
                    textToClean = label.getText();
                    if (!textToClean.equals(""))
                    {
                        cleanText = textToClean.replaceAll(madsString, "");
                        label.setText(cleanText);
                    }
                }
                catch (NullPointerException e)
                {}
                try
                {
                    Element title = element.getChild("title", bf);
                    textToClean = title.getText();
                    if (!textToClean.equals(""))
                    {
                        cleanText = textToClean.replaceAll(madsString, "");
                        title.setText(cleanText);
                    }
                }
                catch (NullPointerException e)
                {}
            }
        }
    }

    public static void removeAuthKeyfromString(List listOfElements, ArrayList<String> subElements, Namespace bf, Namespace madsrdf)
    {
        Iterator<Element> elementIterator = listOfElements.iterator();
        while (elementIterator.hasNext())
        {
            Element element = (Element) elementIterator.next();
            List subElementList;
            Element subElement;

            for (int s = 0; s < subElements.size(); s++)
            {
                try
                {
                    String[] subElementNs = subElements.get(s).split(":");

                    if (subElementNs[0].equals("madsrdf"))
                    {
                        subElementList = element.getChildren(subElementNs[1], madsrdf);
                    }
                    else
                    {
                        subElementList = element.getChildren(subElementNs[1], bf);
                    }

                    Iterator<Element> subElementListIterator = subElementList.iterator();
                    while (subElementListIterator.hasNext())
                    {
                        subElement = (Element) subElementListIterator.next();
                        if (subElement.getName().equals("hasAuthority"))
                        {
                            Element madsAuthority = subElement.getChild("Authority", madsrdf);
                            Element authoritativeLabel = madsAuthority.getChild("authoritativeLabel", madsrdf);
                            removeAuthKeyfromString(authoritativeLabel);
                        }
                        else
                        {
                            removeAuthKeyfromString(subElement);
                        }
                    }
                }
                catch (NullPointerException e)
                {
                    //System.err.println(e.getMessage());
                }
                catch (ArrayIndexOutOfBoundsException e)
                {
                    //System.err.println(e.getMessage());
                }

            }
        }
    }

    public static void removeAuthKeyfromString(Element element)
    {
        String original = "";
        String textReplacement = "";

        original = element.getText();

        Matcher m1 = p1.matcher(original);
        while (m1.find())
        {
            textReplacement = m1.replaceAll("");
            element.setText(textReplacement);
        }

        original = element.getText();
        Matcher m2 = p2.matcher(original);
        while (m2.find())
        {
            textReplacement = m2.replaceAll("");
            element.setText(textReplacement);
        }

    }

    public static String removeAuthKeyfromString(String textElement)
    {
        String original = "";
        String textReplacement = "";

        original = textElement;
        textReplacement = textElement;

        Matcher m1 = p1.matcher(original);

        while (m1.find())
        {
            textReplacement = m1.replaceAll("");
        }

        original = textReplacement;
        Matcher m2 = p2.matcher(original);

        while (m2.find())
        {
            textReplacement = m2.replaceAll("");
        }

        return textReplacement;
    }

    public static String stripPunctAndSpace(String str)
    {
        //for time being also strip out authority key from 6XX field until this can be removed from all Topics except madsrdf...
        String strippedString = str.replaceAll("\\p{Punct}+|\\p{Space}+", "");
        return strippedString;
    }

};
