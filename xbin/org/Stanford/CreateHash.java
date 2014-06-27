package org.Stanford;

import java.security.MessageDigest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.lang.String;

public class CreateHash 
{
    public static String strToHash(String message, String algorithm)
    {
        String digest = null;    
        message = message.replaceAll("\\p{Punct}+|\\p{Space}+", "");

        try
        {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hash = md.digest(message.getBytes("UTF-8"));

            
            StringBuilder sb = new StringBuilder(2*hash.length);

            for(byte b : hash)
            {
                sb.append(String.format("%02x", b&0xff));
            }

            digest = sb.toString();

        }
        catch (UnsupportedEncodingException ex)
        {
            System.out.println(ex.getMessage());
        }
        catch (NoSuchAlgorithmException ex)
        {
            System.out.println(ex.getMessage());
        }

        return digest;
    }
}
