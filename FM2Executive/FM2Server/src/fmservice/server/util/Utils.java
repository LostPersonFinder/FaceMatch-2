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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import java.util.Properties;
import java.lang.management.ManagementFactory;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


import java.util.ArrayList;

import org.apache.log4j.Logger;
/**
 * <P>
 *  Provides  various common functions to higher level classes 
 * </P>
 * 
 *
 * @version $Revision: 1.0 $
 * @date  2015/02/20
 *
 */
public class Utils
{
    private static  Logger log = Logger.getLogger(Utils.class);
    private static String OS_NAME = System.getProperty("os.name").toLowerCase();
    static String fs = File.separator;
        
     /*---------------------------------------------------------------------------------------------------------------------------    
    // Dereference a property in the property file that may refer to another environmental type symbol
    // that should also be contained in the same Properies object
    // For example:   configFile = $APP_HOME/config.txt. APPHOME must be also known here
    // The separator is assumed to ne "/"
    *----------------------------------------------------------------------------------------------------------------------*/
    public  static String getDereferencedProperty( Properties properties, String namedProperty)
    {
        String prop = properties.getProperty(namedProperty);
        if (prop == null)
            return null;
        if (!prop.startsWith("$"))
            return prop;
       
        // resolve reference to the environmental type symbol
        int pindex = prop.indexOf("/");
        if (pindex == -1)
           pindex = prop.length();
        String env = prop.substring(1, pindex);
        String envStr = properties.getProperty(env);
        if (env == null)
            return prop;            // could not be resolved; may not be a referenced string
        else            // dereference
        {
            prop = prop.replace("$"+env, envStr);
            //System.out.println("Util: Dereferenced " + namedProperty + " as: " + prop );
            return prop;
        }
    }
    
    // check if we are running in Linux environment
    public static boolean isWindows()
    {
        return (OS_NAME.indexOf("win") >= 0);
        
    }
    
    // check if we are running in Unix/Linux environment
    public static boolean isLinux()
    {
        return (OS_NAME.indexOf("nix") >= 0 || OS_NAME.indexOf("nux") >= 0 ||
            OS_NAME.indexOf("aix") > 0 );
    }
    
    public static boolean isSolaris() 
    {
	return (OS_NAME.indexOf("sunos") >= 0);
    }
    
    
    public static String heapMemUsage() 
    {
        long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long max = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
       double percentage = (double)(used*100.0)/max;
        
        // truncate to two decimal places
        percentage = Math.floor(percentage * 100) / 100;
        
        return (""+used+" of "+max+" ("+ percentage+ "%)");
    }

    
    /*----------------------------------------------------------------------------------------------*/
    public  static JSONObject readFileAsJSONObject( String fileName)
    {
        if (fileName == null || fileName.trim().length() == 0) 
        {
            log.error ("No  file name specified");
             return null;
        }
        File file = new File(fileName);
        if (!file.exists())
       {    
          log.error("File " + fileName +" does not exist.");
          return null;
       }    

        JSONParser  parser = new JSONParser();
        try
        {
            FileReader fileReader = new FileReader(fileName);

            Object jsonObj = parser.parse(fileReader);
            return (JSONObject)jsonObj;
        }
        catch (IOException ioe) 
        {    
             log.error("Error in reading file " + fileName +", " +ioe.getMessage() );
             return null;
         }
        catch (ParseException pe) 
        {    
              String errorMsg = ( "Invalid JSON format. Error in parsing " + fileName + " contents to JSONObject" + ", Position: " +  parser.getPosition());
              log.error(errorMsg);
              return null;
        }
    }
    
  /*----------------------------------------------------------------------------------------------*/
   public  static JSONArray readFileAsJSONArray( String fileName)
  {
      if (fileName == null || fileName.trim().length() == 0) 
      {
           log.error("No JSON file name specified");
           return null;
      }
      
      JSONParser parser = new JSONParser();
      try
      {
          FileReader fileReader = new FileReader(fileName);
          Object obj = parser.parse(fileReader);
          JSONArray testArray  = (JSONArray) obj;
          return testArray;
      }
      catch (IOException ioe) 
      {    
           log.error("Error in reading file " + fileName, ioe);
           return null;
      }
      catch (ParseException pe) 
      { 
           String errorMsg = ( "Invalid JSON format. Error in parsing " + fileName + " contents to JSONObject" + ", Position: " +  parser.getPosition());
           log.error(errorMsg);
           return null;
      }
  }
  /*---------------------------------------------------------------------------------------------------------*/
   /**
    * Split a string without losing the split string, which may be a regular expression
    * @param string
    * @param splitRegex
    * @return Array of string segments
    *-----------------------------------------------------------------------------------------------------------*/
   public static ArrayList<String> splitNKeepPattern(String string, String splitRegex) 
   {
        ArrayList<String> result = new ArrayList<String>();
        Pattern p = Pattern.compile(splitRegex);
        Matcher m = p.matcher(string);
        int index = 0;
        while (index < string.length()) 
        {
            if (m.find()) 
            {
                int splitIndex = m.end();
                String splitString = m.group();
                String segment = string.substring(index,splitIndex-splitString.length());
                result.add(segment);
                index += segment.length();
            } 
            else
            {
                String segment = string.substring(index);
                result.add(segment);
                index += segment.length();
            }
        }
        return result;
    }
   
    /*---------------------------------------------------------------------------------------------------------*/
   /**
    * Get the basic name part of a file sans its directory and extension
    * @param fileName Full name of file with or without the directory part
    * @return base name of file
    *-----------------------------------------------------------------------------------------------------------*/
   public static String getFileBaseName(String fileName)
   {
       if (fileName == null || fileName.isEmpty())
           return "";
        File file = new File(fileName);
        String fname = file.getName();
        int pos = fname.lastIndexOf(".");
        if (pos > 0) 
            fname = fname.substring(0, pos);
        return fname;
   }
   
   /*---------------------------------------------------------------------------------------------------------*/
   /** Create a file containing the list of Strings as individual lines
    * If the file exists, delete it.
    * @param fileName Full name of file with or without the directory part
    * @return status: 1 -> success,  0 -> file could not be created
    *-----------------------------------------------------------------------------------------------------------*/ 
   public static String createListFile(String[]  list, File outfilePath, String outfileName)
   {
       try
      {
            File listFile = new File (outfilePath.getAbsoluteFile()+"/"+outfileName); 
            if (listFile.exists())
                listFile.delete();
            
            BufferedWriter fileListWriter = new BufferedWriter(new FileWriter(listFile));
            for (int i = 0; i < list.length; i++)
            {
                fileListWriter.write(list[i]);
                fileListWriter.newLine();
            }
            fileListWriter.flush();
            fileListWriter.close();
            return listFile.getAbsolutePath() ;
      }
       catch (Exception e)
       {
           log.error("Error creating List File " + outfileName + " in directory: " + outfilePath.getAbsolutePath() );
           return null;
       }
   }

   
   public static void main(String[] args)
   {
       //String jsonFile = "<TopDir>/FM2FullTest/FMQueryResultTemplate.json";
       String jsonFile = "<TopDir>/FM2FullTest/FMFaceFindResultTemplate.json";
       
       JSONObject queryResult = Utils.readFileAsJSONObject(jsonFile);
       System.out.println(queryResult == null ? " Invalid JSON format" : "Valid JSON format");
   }
}
