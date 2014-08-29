package org.Stanford;

import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.w3c.dom.Document;
import javax.xml.transform.dom.DOMResult;

import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.Record;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Subfield;
import org.marc4j.MarcException;
import org.marc4j.marc.MarcFactory;

public class MarcToXML {
    
    public static Document MarcToXML(String marc) {

        System.err.println("\nCONVERTING MARC TO XML\n");

        try 
        {
            String[] tagList;
            tagList = new String[15];

            tagList[0] = "600";
            tagList[1] = "610";
            tagList[2] = "611";
            tagList[3] = "630";
            tagList[4] = "648";
            tagList[5] = "650";
            tagList[6] = "651";
            tagList[7] = "653";
            tagList[8] = "654";
            tagList[9] = "655";
            tagList[10] = "656";
            tagList[11] = "657";
            tagList[12] = "662";
            tagList[13] = "690";
            tagList[14] = "691";

            Map<String, String> sfMap = new HashMap();
            
            sfMap.put ("a","Name"); 
            sfMap.put ("e","Occupation");
            sfMap.put ("l","Language");
            sfMap.put ("t","Title");
            sfMap.put ("v","GenreForm");
            sfMap.put ("x","Topic");
            sfMap.put ("y","Temporal");
            sfMap.put ("z","Geographic");

            InputStream input = new FileInputStream(marc);
            MarcReader reader = new MarcStreamReader(input);
            
            DOMResult result = new DOMResult();
            MarcWriter writer = new MarcXmlWriter(result);
            
            MarcFactory factory = MarcFactory.newInstance();

            while (reader.hasNext())
            {
                try
                {
                    Record record = reader.next();
                    
                    try
                    {
                        for (int s=0; s < tagList.length; s++)
                        {
                            String tag = tagList[s];

                            switch (tag)
                            {
                                case "600": 
                                case "610": 
                                case "611": 
                                    sfMap.put ("a","Name"); 
                                    break;
                                
                                case "630": 
                                    sfMap.put ("a","Title"); 
                                    break;
                                
                                case "650":
                                case "653":
                                case "654":
                                    sfMap.put ("a","Topic");
                                    break;

                                case "651":
                                case "662":
                                    sfMap.put ("a","Geographic");
                                    break;
                            
                                case "655":
                                    sfMap.put ("a","GenreForm");
                                    break;
                            
                                case "648":
                                    sfMap.put ("a","Temporal");
                                    break;

                                case "656": 
                                    sfMap.put ("a","Occupation");
                                    break;
                            }
                            
                            List subjectFields = record.getVariableFields(tagList[s]);
                            Iterator dataFieldIterator = subjectFields.iterator();

                            while (dataFieldIterator.hasNext())
                            {
                                DataField dataField = (DataField) dataFieldIterator.next();

                                List subFieldList = dataField.getSubfields();
                                Iterator subFieldIterator = subFieldList.iterator();
                                while (subFieldIterator.hasNext())
                                {
                                    Subfield sf = (Subfield) subFieldIterator.next();
                                    char code = sf.getCode();
                                    String codeStr = String.valueOf(code);
                                    String data = sf.getData();
                                    
                                    if (!codeStr.equals("=") && sfMap.containsKey(codeStr))    
                                    {
                                        sf.setData(sfMap.get(codeStr) + "#" + data);
                                    }
                                }

                                char ind2 = dataField.getIndicator2();
                                String indicatorString = String.valueOf(ind2);
                                String stringtoAdd = "~" + indicatorString;
                                dataField.addSubfield(factory.newSubfield('9',stringtoAdd));
                            }
                        }

                        List allFields = record.getDataFields();
                        Iterator dataFieldIterator = allFields.iterator();

                        while (dataFieldIterator.hasNext())
                        {
                            DataField dataField = (DataField) dataFieldIterator.next();
                            Subfield questionMarkSubfield = dataField.getSubfield("?".charAt(0));
                            dataField.removeSubfield(questionMarkSubfield);
                        }
                    }
                    catch (NullPointerException e)
                    {   
                        System.err.println(e.getMessage());

                        String recordNum = "";
                        List cfl = record.getControlFields();
                        Iterator cfit = cfl.iterator();
                        while (cfit.hasNext())
                        {
                            ControlField cf = (ControlField) cfit.next();
                            if (cf.getTag().equals("001"))
                            {
                                recordNum = cf.getData();
                            }
                        }
                        System.err.println(recordNum);
                    }

                    writer.write(record);
                }
                catch (MarcException e)
                {
                    System.err.println(e.getCause());
                }
            }
            writer.close();
            Document doc = (Document) result.getNode();
            System.err.println("DONE WITH MARCXML CONVERSION\n");
            return doc;
        }
        catch (NullPointerException e)
        {
            System.err.println(e.getMessage());
        }
        catch (FileNotFoundException e)
        {
            System.err.println(e.getMessage());
        }
        return null;
    } 
};
