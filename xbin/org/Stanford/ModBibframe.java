package org.Stanford;

import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;
import java.lang.Boolean;
import java.sql.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Attribute;
import org.jdom2.Namespace;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.DOMOutputter;

public class ModBibframe 
{
    public static boolean createHash;
    public static String baseURI;

    public static Map<String, String> uriMapper = new HashMap();
    public static DOMBuilder builder = new DOMBuilder();
    public static CreateHash uriHash = new CreateHash();

    public static LookupAuthID lookup = new LookupAuthID();
    public static Properties props = PropGet.getProps("conf/server.conf");
    public static Connection connection = lookup.OpenAuthDBConnection(props);

    public static org.w3c.dom.Document ModBibframe(org.w3c.dom.Document rdfFile, String baseURI, boolean createhash) throws Exception
    {
        try 
        {
            if (baseURI == null)
            {
                baseURI = "";
            }
            else
            {
                System.err.println("\nMODIFYING BIBFRAME RDF");
                System.err.println("BASE URI IS SET TO: " + baseURI);
            }

            boolean creatHash;
            createHash = Boolean.valueOf(createhash);

            Document doc = (Document) builder.build(rdfFile);
            Element rootNode = doc.getRootElement();

            Namespace rdf = rootNode.getNamespace();
            Namespace bf = rootNode.getNamespace("http://bibframe.org/vocab/");
            Namespace madsrdf = rootNode.getNamespace("http://www.loc.gov/mads/rdf/v1#");
            Namespace relators = rootNode.getNamespace("http://id.loc.gov/vocabulary/relators/");

            //Identifier
            List identifiers = rootNode.getChildren("Identifier", bf);
            ArrayList<String> identifiersList = new ArrayList<String>(getSubElementList("identifiers"));
            ModURIforElements(identifiers, identifiersList, bf, rdf, madsrdf, baseURI, createHash);
            
            //Annotation
            List annotations = rootNode.getChildren("Annotation", bf);
            ArrayList<String> annotationsList = new ArrayList<String>(getSubElementList("annotations"));
            ModURIforElements(annotations, annotationsList, bf, rdf, madsrdf, baseURI, createHash);

            Iterator<Element> annotationIterator = annotations.iterator();
            ArrayList<String> annotationsResourcesList = new ArrayList<String>(getSubElementList("annotations_resources"));
            String myAnnotationURI = "";
            while (annotationIterator.hasNext())
            {
                try
                {
                    Element myAnnotation = (Element) annotationIterator.next();
                    myAnnotationURI = myAnnotation.getAttribute("about", rdf).getValue();
                    Element annotationDerivedFrom = myAnnotation.getChild("derivedFrom", bf);
                    String annotationResourceURI = "";
                    int cutoff;
                    int basecut;
                    if (annotationDerivedFrom != null)
                    {
                        annotationResourceURI = annotationDerivedFrom.getAttribute("resource", rdf).getValue();
                        cutoff = annotationResourceURI.indexOf(".marcxml.xml");
                        basecut = baseURI.length();
                        uriMapper.put(annotationResourceURI, annotationResourceURI.substring(basecut, cutoff)); 
                    }
                    ModURIforResources(myAnnotation, annotationsResourcesList, bf, rdf, baseURI, createHash);
                }
                catch (NullPointerException e)
                {
                    System.err.println(e.getMessage());
                }
            }

            //Title
            List titles = rootNode.getChildren("Title", bf);
            ArrayList<String> titlesList = new ArrayList<String>(getSubElementList("titles"));
            ModURIforElements(titles, titlesList, bf, rdf, madsrdf, baseURI, createHash);
                        
            //Agents
            List agents = rootNode.getChildren("Agent", bf);
            ArrayList<String> agentList = new ArrayList<String>(getSubElementList("agents"));
            ModURIforElements(agents, agentList, bf, rdf, madsrdf, baseURI, createHash);
            
            //Person
            List persons = rootNode.getChildren("Person", bf);
            ArrayList<String> personsList = new ArrayList<String>(getSubElementList("persons"));
            CleanupAuth.modMads(persons, baseURI, createHash, bf, rdf, madsrdf);
            ModURIforElements(persons, personsList, bf, rdf, madsrdf, baseURI, createHash);
            
            //Organization
            List organizations = rootNode.getChildren("Organization", bf);
            ArrayList<String> organizationsList = new ArrayList<String>(getSubElementList("organizations"));
            CleanupAuth.modMads(organizations, baseURI, createHash, bf, rdf, madsrdf);
            ModURIforElements(organizations, organizationsList, bf, rdf, madsrdf, baseURI, createHash);
            
            //Meeting
            List meetings = rootNode.getChildren("Meeting", bf);
            ArrayList<String> meetingsList = new ArrayList<String>(getSubElementList("meetings"));
            CleanupAuth.modMads(meetings, baseURI, createHash, bf, rdf, madsrdf);
            ModURIforElements(meetings, meetingsList, bf, rdf, madsrdf, baseURI, createHash);

            //Event
            List events = rootNode.getChildren("Event", bf);
            ArrayList<String> eventsList = new ArrayList<String>(getSubElementList("events"));
            CleanupAuth.modMads(events, baseURI, createHash, bf, rdf, madsrdf);
            ModURIforElements(events, eventsList, bf, rdf, madsrdf, baseURI, createHash);

            //Topic
            List topics = rootNode.getChildren("Topic", bf);
            ArrayList<String> topicsList = new ArrayList<String>(getSubElementList("topics"));
            CleanupAuth.modMads(topics, baseURI, createHash, bf, rdf, madsrdf);
            ModURIforElements(topics, topicsList, bf, rdf, madsrdf, baseURI, createHash);

            Iterator<Element> topicIterator = topics.iterator();
            ArrayList<String> topicsResourcesList = new ArrayList<String>(getSubElementList("topics_resources"));
            while (topicIterator.hasNext())
            {
                Element myTopic = (Element) topicIterator.next();
                ModURIforResources(myTopic, topicsResourcesList, bf, rdf, baseURI, createHash);
            }
            
            //Place
            List places = rootNode.getChildren("Place", bf);
            ArrayList<String> placesList = new ArrayList<String>(getSubElementList("places"));
            CleanupAuth.modMads(places, baseURI, createHash, bf, rdf, madsrdf);
            ModURIforElements(places, placesList, bf, rdf, madsrdf, baseURI, createHash);
            
            Iterator<Element> placeIterator = places.iterator();
            ArrayList<String> placesResourcesList = new ArrayList<String>(getSubElementList("places_resources"));
            while (placeIterator.hasNext())
            {
                Element myPlace = (Element) placeIterator.next();
                ModURIforResources(myPlace, placesResourcesList, bf, rdf, baseURI, createHash);
            }
            
            //Language
            List languages = rootNode.getChildren("Language", bf);
            ArrayList<String> languagesList = new ArrayList<String>(getSubElementList("languages"));
            CleanupAuth.modMads(languages, baseURI, createHash, bf, rdf, madsrdf);
            ModURIforElements(languages, languagesList, bf, rdf, madsrdf, baseURI, createHash);
                        
            //Classification
            List classifications = rootNode.getChildren("Classification", bf);
            ArrayList<String> classificationsList = new ArrayList<String>(getSubElementList("classifications"));
            ModURIforElements(classifications, classificationsList, bf, rdf, madsrdf, baseURI, createHash);
            
            //Intended Audience
            List audiences = rootNode.getChildren("IntendedAudience", bf);
            ArrayList<String> intendedAudienceList = new ArrayList<String>(getSubElementList("audiences"));
            ModURIforElements(audiences, intendedAudienceList, bf, rdf, madsrdf, baseURI, createHash);
            
            //Instance
            List instances = rootNode.getChildren("Instance", bf);
            ArrayList<String> instancesList = new ArrayList<String>(getSubElementList("instances"));
            ModURIforElements(instances, instancesList, bf, rdf, madsrdf, baseURI, createHash);
            
            Iterator<Element> instanceIterator = instances.iterator();
            ArrayList<String> instancesResourceList = new ArrayList<String>(getSubElementList("instances_resources"));
            while (instanceIterator.hasNext())
            {
                Element myInstance = (Element) instanceIterator.next();
                ModURIforResources(myInstance, instancesResourceList, bf, rdf, baseURI, createHash);
            }
            
            //HeldItem
            List heldItems = rootNode.getChildren("HeldItem", bf);
            ArrayList<String> heldItemsList = new ArrayList<String>(getSubElementList("helditems"));
            ModURIforElements(heldItems, heldItemsList, bf, rdf, madsrdf, baseURI, createHash);
            
            Iterator<Element> heldItemIterator = heldItems.iterator();
            while (heldItemIterator.hasNext())
            {
                Element myHeldItem = (Element) heldItemIterator.next();
                List holdingForElement = myHeldItem.getChildren("holdingFor", bf);
                Iterator<Element> holdingForIterator = holdingForElement.iterator();
                while (holdingForIterator.hasNext())
                {
                    Element holdingFor = (Element) holdingForIterator.next();
                    String lcResourceURI = holdingFor.getAttribute("resource", rdf).getValue();
                    String myResourceURI = uriMapper.get(lcResourceURI);
                    if (myResourceURI != null)
                    {
                        holdingFor.getAttribute("resource", rdf).setValue(myResourceURI);
                    }
                }
            }

            //Work (must be last so that rdf:resources can be set)
            List works = rootNode.getChildren("Work", bf);
            //System.err.println("Number of Works:" + works.size());
            
            ArrayList<String> worksList = new ArrayList<String>(getSubElementList("works"));
            CleanupAuth.cleanupWorks(works, bf, madsrdf);
            ModURIforElements(works, worksList, bf, rdf, madsrdf, baseURI, createHash);
            
            ArrayList<String> worksResourcesList = new ArrayList<String>(getSubElementList("works_resources"));
            ArrayList<String> relatorTermsList = new ArrayList<String>(getSubElementList("relators"));

            Iterator<Element> workIterator = works.iterator();
            while (workIterator.hasNext())
            {
                Element myWork = (Element) workIterator.next();
                ModURIforResources(myWork, worksResourcesList, bf, rdf, baseURI, createHash);
                ModURIforRelatorResources(myWork, relatorTermsList, rdf, relators, baseURI, createHash);
            }
            
            uriMapper.clear();

            if (connection != null)
            {
                connection.close();
            }

            DOMOutputter domOutput = new DOMOutputter();
            
            // display nice
            domOutput.setFormat(Format.getPrettyFormat().setLineSeparator("\n"));
            org.w3c.dom.Document modDoc = domOutput.output(doc);
            
            return modDoc;
        } 
        catch (Throwable t) 
        {
            t.printStackTrace();
        }
        /*catch (org.jdom2.IllegalDataException e)
        {
            System.err.println(e.getCause());
        }*/
        return null;
    }

    public static void ModURIforResources(Element element, ArrayList<String> subElements, Namespace bf, Namespace rdf, String baseuri, boolean cretaeHash)
    {
        for (int s = 0; s < subElements.size(); s++)
        {
            List listOfSubElements = element.getChildren(subElements.get(s), bf);
            Iterator<Element> subElementIterator = listOfSubElements.iterator();
            while (subElementIterator.hasNext())
            {
                Element mySubElement = (Element) subElementIterator.next();
                Attribute myAttribute = mySubElement.getAttribute("resource", rdf);
                if (myAttribute != null)
                {
                    String lcResourceURI = myAttribute.getValue();
                    String myResourceURI = uriMapper.get(lcResourceURI);
                    
                    /*if (lcResourceURI != null)
                    {
                        System.err.println(lcResourceURI + " (" + myResourceURI + ")");
                    }*/

                    if (myResourceURI != null)
                    {
                        String URI_VALUE = myResourceURI.toLowerCase();
                        if (createHash)
                        {
                            URI_VALUE = uriHash.strToHash(URI_VALUE, "MD5");
                        }
                        mySubElement.getAttribute("resource", rdf).setValue(baseuri + URI_VALUE);
                    }
                }
            }
        }
    }

    public static void ModURIforRelatorResources(Element element, ArrayList<String> subElements, Namespace rdf, Namespace relators, 
                                                    String baseuri, boolean cretaeHash)
    {
        for (int s = 0; s < subElements.size(); s++)
        {
            String myChild = subElements.get(s).trim();
            List listOfSubElements = element.getChildren(myChild, relators);
            Iterator<Element> subElementIterator = listOfSubElements.iterator();
            while (subElementIterator.hasNext())
            {
                Element mySubElement = (Element) subElementIterator.next();
                Attribute myAttribute = mySubElement.getAttribute("resource", rdf);
                if (myAttribute != null)
                {
                    String lcResourceURI = myAttribute.getValue();
                    String myResourceURI = uriMapper.get(lcResourceURI);
                    if (myResourceURI != null)
                    {
                        String URI_VALUE = myResourceURI.toLowerCase();
                        if (createHash)
                        {
                            URI_VALUE = uriHash.strToHash(URI_VALUE, "MD5");
                        }
                        mySubElement.getAttribute("resource", rdf).setValue(baseuri + URI_VALUE);
                    }
                }
            }
        }
    }

    public static void ModURIforElements(List listOfElements, ArrayList<String> subElements, Namespace bf, Namespace rdf, Namespace madsrdf, 
                                            String baseuri, boolean cretaeHash)
    {
        String lcURI = "";
        Iterator<Element> elementIterator = listOfElements.iterator();
        while (elementIterator.hasNext())
        {
            try
            {
                List<String> listOfStrings = new ArrayList<String>();
                Element element = (Element) elementIterator.next();
                lcURI = element.getAttribute("about", rdf).getValue();

                Element subElement;
                String elementURI = "";
                String subElementString = "";
                String authorityKey = "";
                String authorityID = "";
                String authorityURI = "";
                String authorityString = "";
                String authorityStringURI = "";
                String textElement = "";
                String textReplacement = "";
                String VIAFTagNum = "921";
                String locURI = "http://id.loc.gov/authorities/";

                for (int s = 0; s < subElements.size(); s++)
                {
                    String myChild = subElements.get(s).trim();
                    if (myChild.equals("hasAuthority"))
                    {
                        Element hasAuthority = element.getChild("hasAuthority", bf);
                        if (hasAuthority != null)
                        {
                            Element authority = hasAuthority.getChild("Authority", madsrdf);
                            Element authLabel = authority.getChild("authoritativeLabel", madsrdf);
                            authorityString = authLabel.getText();

                            String elementName = element.getName();
                            if (elementName != null && !elementName.equals("Topic"))
                            {
                                authorityKey = lookup.getAuthorityKey(authorityString);

                                if (authorityKey != null && authorityKey != "")
                                {
                                    authorityID = lookup.LookupAuthIDfromDB(authorityKey, connection, props);
                                }

                                subElementString = stripPunctAndSpace(CleanupAuth.removeAuthKeyfromString(authorityString));   
                            }

                            if (authorityID != null && authorityID.length() > 0)
                            {
                                authorityURI = lookup.LookupAuthURIfromDB(authorityID, VIAFTagNum, connection);
                                
                                //System.err.println("authID" + authorityID);
                                //System.err.println("authURI" + authorityURI);
                                
                                if (authorityURI != null && authorityURI.length() > 0)
                                {
                                    if (authorityURI.indexOf("viaf") > 0)
                                    {
                                        hasAuthority.setAttribute("resource", authorityURI, rdf);
                                    }
                                    else if (authorityURI.toLowerCase().matches("(n[a-z])([0-9]+)"))
                                    {
                                        locURI += "names/" + authorityURI.toLowerCase(); 
                                        hasAuthority.setAttribute("resource", locURI, rdf);
                                    }
                                    else if (authorityURI.toLowerCase().matches("(sh)([0-9]+)"))
                                    {
                                        locURI += "subjects/" + authorityURI.toLowerCase(); 
                                        hasAuthority.setAttribute("resource", locURI, rdf);
                                    }
                                }
                                else if (authorityString != null && authorityString != "")
                                {
                                    authorityStringURI = CleanupAuth.removeAuthKeyfromString(authorityString);
                                    authorityStringURI = stripPunctAndSpace(authorityStringURI).toLowerCase();
                                    hasAuthority.setAttribute("about", baseuri + authorityStringURI, rdf);
                                }
                            }
                        }
                    }
                    else
                    {   
                        if (element.getContentSize() > 0)
                        {
                            subElement = element.getChild(myChild, bf);
                            if (subElement != null)
                            {
                                textElement = subElement.getText();
                            }

                            if (textElement.indexOf("^A") > 0)
                            {
                                textElement = CleanupAuth.removeAuthKeyfromString(textElement);
                            }
                        }

                        subElementString = stripPunctAndSpace(textElement);
                    }

                    if (!listOfStrings.contains(subElementString))
                    {    
                        elementURI += subElementString.toLowerCase();
                        listOfStrings.add(subElementString);
                    }
                }

                //Set the Element rdf:about URI
                String URI_VALUE = elementURI + authorityID.toLowerCase();
                if (createHash)
                {
                    URI_VALUE = uriHash.strToHash(URI_VALUE, "MD5");
                }

                element.getAttribute("about", rdf).setValue(baseuri + URI_VALUE);
                uriMapper.put(lcURI, elementURI + authorityID);
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                e.printStackTrace();
            }
            catch (StringIndexOutOfBoundsException e)
            {
                e.printStackTrace();
            }
            catch (NullPointerException e)
            {
                System.err.println(e.getMessage());
            }

        }

    }

    public static String stripPunctAndSpace(String str)
    {
        //for time being also strip out authority key from 6XX field until this can be removed from all Topics except madsrdf...
        //String strippedString = str.replaceAll("\\p{Punct}+|\\p{Space}+|[A]{1}[0-9]+", "");
        String strippedString = str.replaceAll("\\p{Punct}+|\\p{Space}+", "");
        return strippedString;
    }

    public static ArrayList<String> getSubElementList (String filename)
    {
        try
        {
            Scanner s = new Scanner(new File("conf/" + filename));
            ArrayList<String> result = new ArrayList<String>();
            String line = "";

            while (s.hasNext())
            {
                line = s.next();   
                if (line.indexOf("#") != 0)
                {
                    result.add(line);
                }
            }
            s.close();
            return result;
        }
        catch (NoSuchElementException e)
        {
            //e.printStackTrace();
        }
        catch (FileNotFoundException e)
        {
            System.err.println(e.getMessage());
        }
        return null;
    }
}
