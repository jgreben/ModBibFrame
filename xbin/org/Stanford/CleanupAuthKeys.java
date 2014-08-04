package org.Stanford;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Attribute;
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

public class CleanupAuthKeys
{
    public static Pattern p1 = Pattern.compile("\\^A[0-9]+");
    public static Pattern p2 = Pattern.compile("[-\\s]+~[0-9]{1}");
    
    public static DOMBuilder builder = new DOMBuilder();
    
    public static org.w3c.dom.Document Cleanup(org.w3c.dom.Document rdfFile) throws Exception
    {
        System.err.println("\nCLEANING UP AUTHORITY KEYS");

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

    public static void removeAuthKeyfromString(List listOfElements, ArrayList<String> subElements, Namespace bf, Namespace madsrdf)
    {
        Iterator<Element> elementIterator = listOfElements.iterator();
        while (elementIterator.hasNext())
        {
            Element element = (Element) elementIterator.next();
            List subElementList;
            Element subElement;

            //System.err.println("Element:" + element.getName());

            for (int s = 0; s < subElements.size(); s++)
            {
                try
                {
                    String[] subElementNs = subElements.get(s).split(":");

                    if (subElementNs[0].equals("madsrdf"))
                    {
                        subElementList = element.getChildren(subElementNs[1], madsrdf);
                       // subElement = element.getChild(subElementNs[1], madsrdf);
                    }
                    else
                    {
                        subElementList = element.getChildren(subElementNs[1], bf);
                        //subElement = element.getChild(subElementNs[1], bf);
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
                    System.err.println(e.getMessage());
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
            //System.err.println(element.getName() + " > " + original);
            
            textReplacement = m1.replaceAll("");
            
            //System.err.println("< " + textReplacement);
            
            element.setText(textReplacement);
        }
        
        original = element.getText();
        Matcher m2 = p2.matcher(original);
        while (m2.find())
        {
            //System.err.println(element.getName() + " > " + original);
            
            textReplacement = m2.replaceAll("");
            
            //System.err.println("< " + textReplacement);
            
            element.setText(textReplacement);
        }

    }
    
    public static String removeAuthKeyfromString(String textElement)
    {
        String original = "";
        String textReplacement = "";
        
        original = textElement;
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
};
