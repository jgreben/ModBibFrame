package org.Stanford;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.Scanner;
import java.sql.*;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.pool.OracleConnectionCacheManager;

public class LookupAuthID
{
    private static OracleDataSource ods = null;
    public static String result = "";

    Properties props = new Properties();

    /*public static Properties getProps()
    {
        Properties props = new Properties();
        try 
        {
            File inFile = new File(new File(Thread.currentThread().getContextClassLoader().getResource("").toURI()), "conf/server.conf");
            FileInputStream in = new FileInputStream(inFile);
            props.load(in);
            in.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println(e.getMessage());
        }
        catch (IOException e)
        {
            System.err.println(e.getMessage());
        }
        catch (URISyntaxException e)
        {
            System.err.println(e.getMessage());
        }
        
        return props;
    }*/
    
    public static Connection OpenAuthDBConnection(Properties props)
    { 
        String url = "";

        props = PropGet.getProps("conf/server.conf");
        String USER = props.getProperty("USER");
        String PASS = props.getProperty("PASS");
        String SERVER = props.getProperty("SERVER");
        String SERVICE_NAME = props.getProperty("SERVICE_NAME");
        
        Connection connection = null;
        
        try
        {
            url = "jdbc:oracle:thin:@" + SERVER + ":1521:" + SERVICE_NAME;
            
            ods = new OracleDataSource();
            ods.setURL(url);
            ods.setUser(USER);
            ods.setPassword(PASS);
            ods.setConnectionCachingEnabled(false);
            ods.setConnectionCacheName("CACHE");
            
            Properties cacheProps = new Properties();
            cacheProps.setProperty("MinLimit", "1");
            cacheProps.setProperty("InitialLimit", "1");
            cacheProps.setProperty("AbandonedConnectionTimeout", "300");
            cacheProps.setProperty("PropertyCheckInterval", "280");
            
            ods.setConnectionCacheProperties(cacheProps);
            connection = ods.getConnection();
        }
        catch(SQLException e)
        {
            System.err.println("SQLException:" + e.getMessage());
        }
        
        return connection;
    }
    
    public static String LookupAuthIDfromDB(String key, Connection connection, Properties props)
    {
        String TABLE_KEY_COL = props.getProperty("KEY_COLUMN");
        String TABLE_ID_COL = props.getProperty("ID_COLUMN");
        String TABLE_NAME = props.getProperty("TABLE_NAME");

        String sql = "";

        try 
        {
            sql = "select " + TABLE_ID_COL + " from " + TABLE_NAME + " where " + TABLE_KEY_COL + " = '" + key + "'";
            //sql = "select AUTHORITY_ID from AUTHORITY where AUTHORITY_KEY = '" + key + "'";
            Statement s = null;
            ResultSet rs = null;

            s = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            rs = s.executeQuery(sql);

            while (rs.next())
            {
                result = rs.getString(1).trim();
            }
            rs.close();
            s.close();
        }
        catch(SQLException e)
        {
            System.err.println("SQLException:" + e.getMessage());
        }
        
        return result;
    }
};
