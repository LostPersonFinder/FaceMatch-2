/*
 * /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fmservice.server.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

// for checksum computation 
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;


public class ChecksumCalculator
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ChecksumCalculator.class);
    
    // return the name of algorithm used for computing checksum of a file
    public static String getChecksumMethod()
    {
        return "MD5";
    }
    
   /****/      
    public static String getMD5Checksum(String filename)
    {
        return  getFileChecksum(filename);
    }
    
    /****
     * Return the checksum of data contents of a file.
     * Checksum is computed using MD5 algorithm and returned 
     * as a text string. 
     * 
     * @param filename file whose checksum is to be calculated
     * @return checksum value in hex
     ****/
   
    public static String getFileChecksum(String filename)
    {
       // Read through a digest input stream that will work out the MD5   
        String csHex = null;        // checksum in Hex
        try
        { 
            FileInputStream fis = new FileInputStream(filename);
            byte[] data = new byte[fis.available()];
            fis.read(data);
            
            //csHex = getFileChecksum(data);
             MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            byte[] csBytes  = md.digest(data);
            csHex = toHex(csBytes);     // convert checksum to hex form
            fis.close();   
        }
        
        catch (FileNotFoundException  nfe)
        {
            log.error("File " + filename + " not found"); 
        }
        
      // any other exception
        catch (Exception e) 
        {
           log.error("Error getting checksum for file " + filename , e);      // anything else;
        }
        return csHex;                   // checksum in hex
    }
    
  /*-------------------------------------------------------------------------------------------------*/
    /**
     * Return the checksum of byte data contents of a buffer
     * @param bytedata   a bytestream  whose checksum is to be calculated
     * @return checksum value in hex
    */  
      public static String getFileChecksum(byte[] byteData)
    {
       // Read through a digest input stream that that is used to create a  file    
        String csHex = null;        // checksum in Hex
        try
        {   
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            byte[] csBytes  = md.digest(byteData);
            csHex = toHex(csBytes);     // convert checksum to hex form
        }
        
        // Should never happen
        catch (NoSuchAlgorithmException nsae)
        {
            log.error("Caught NoSuchAlgorithmException for MD5 \n"
                    + nsae.getMessage());
        }
        catch (Exception e) 
        {
            log.error("Unknown exception in calculating checksum", e);      // anything else;
        }
        return csHex;                   // checksum in hex
    }
    
    /***
     * convert a byte array to Hex String
     ***/
    public static String toHex(byte[] data)
    {
        StringBuffer result = new StringBuffer();

        // This is far from the most efficient way to do things...
        for (int i = 0; i < data.length; i++)
        {
            int low =  (data[i] & 0x0F);
            int high = (data[i] & 0xF0);

            result.append(Integer.toHexString(high).substring(0, 1));
            result.append(Integer.toHexString(low));
        }
        return result.toString();
    }
    
     /***
     *  Return the MD5 checksum of a String
     ***/

    public static String getStringChecksum(String myString)
    {
        String csHex = null;        // checksum in Hex
        try
        {   
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            byte[] csBytes  = md.digest(myString.getBytes());
            csHex = toHex(csBytes);     // convert checksum to hex form
            return csHex;
        }
         // any other exception
        catch (Exception e) 
        {
           log.error("Error getting checksum for String " + myString , e);      // anything else;
           return null;
        }
        
    }
    
    
        ////////////////////////////////////////////////////////////
    public static void main(String[] args)
    {
         /* String[] filenames = { "filename1"  , "FILENAME2"};          // tbd
  
          for (int i =0; i < filenames.length; i++)
          {
               //String filePath = imageDir+"/"+filenames[i];
               String filePath = filenames[i];
               System.out.println( "file: " + filePath);     
               String checksum = ChecksumCalculator.getFileChecksum(filePath);
               System.out.println(filenames[i] +",  checksum: "+ checksum);
          }
          */
          String myString = "fm2-Ops$$";
          String checksum = ChecksumCalculator.getStringChecksum(myString);
          System.out.println(myString+",  checksum: "+ checksum);
         System.exit(0);
    }
}
