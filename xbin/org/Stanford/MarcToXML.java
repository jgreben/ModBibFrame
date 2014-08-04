package org.Stanford;

import java.io.*;
import java.util.List;
import java.util.Iterator;

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

        try {

            InputStream input = new FileInputStream(marc);
            MarcReader reader = new MarcStreamReader(input);
            DOMResult result = new DOMResult();
            MarcWriter writer = new MarcXmlWriter(result);

            MarcFactory factory = MarcFactory.newInstance();

            String [] subjectTagList = {"600","610","611","630","648","650","651","653","654","655","656","657","658","662"};

            while (reader.hasNext())
            {
                try
                {
                    Record record = reader.next();
                    
                    try
                    {
                        for (int s=0; s < subjectTagList.length; s++)
                        {
                            List subjectFields = record.getVariableFields(subjectTagList[s]);
                            Iterator dataFieldIterator = subjectFields.iterator();

                            while (dataFieldIterator.hasNext())
                            {
                                DataField dataField = (DataField) dataFieldIterator.next();
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
