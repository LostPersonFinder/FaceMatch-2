/*
Informational Notice:
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
an agency of the Department of Health and Human Services, United States Government.

- - The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

- - The license does not supersede any applicable United States law.

- - The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
(FAR) 48 C.F.R. Part52.227-14, Rights in Data—General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
•	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

•	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.

•	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/**
* Utils.java
 *
 * Version: $Revision: 1.0 $
 */
package workbench.util;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import fmservice.httputils.common.ResponseMessage;

/**
 * <P>
 *  Provides  various common functions to higher level classes 
 * </P>
 *
 */
public class Utils
{

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
    
    public static String getFileSystemRoot()
    {
        return (isWindows() ? "C:" : "/home");
    }
       /*-----------------------------------------------------------------------------------------------------------------*/ 
     /**
      * Load in log4j config properties from  log4j.properties file using PropertyConfigurator.
      * Set the log file name as a system property to be used for logging  messages
      * from the application.
      *------------------------------------------------------------------------------------------------------------------*/
    public static String  initLogging(Properties properties)
    {     
        String log4jConfProp = "";
        //Load in log4j config properties from  log4j.properties file
        log4jConfProp =  properties.getProperty("log4j.properties");

        // set the log file name as a 
        String log4jFilename = properties.getProperty("log4j.filename");
        if ( log4jConfProp == null || log4jFilename == null)
        {
            System.err.println("Missing log4jPropertyConfigurator and/or log file name in the input Properties");
            return null;
        }
        System.setProperty("log4j.filename", log4jFilename);
        PropertyConfigurator.configure(log4jConfProp);
        return log4jFilename;
    }
    
  /*----------------------------------------------------------------------------------------------*/
   public static  JSONArray readFileAsJSONArray( String fileName)
  {
      if (fileName == null || fileName.trim().length() == 0) 
      {
           System.out.println("No Property  file name specified");
           return null;
      }
      try
      {
          FileReader fileReader = new FileReader(fileName);
          JSONParser parser = new JSONParser();
          Object obj = parser.parse(fileReader);
          JSONArray testArray  = (JSONArray) obj;
          return testArray;
      }
      catch (IOException ioe) 
      {    
           System.out.println("Error in reading file " + fileName +", " +ioe.getMessage());
           ioe.printStackTrace();
           return null;
      }
      catch (ParseException pe) 
      {    
           System.out.println("Invalid JSON format. Error in parsing " + fileName + " contents to JSONArray");
           pe.printStackTrace();
           return null;
      }
  }
   
   //-------------------------------------------------------------------------------------------------------------
   // Return the server's result in the HTTPResponse as a formatted set of strings for printing
   // The result  has the following elements
   //   public int statusCode
   //   public String statusMsg;
   //   public String  returnContent;          
   //-------------------------------------------------------------------------------------------------------------
   public ArrayList<String> formatServerResponse(ResponseMessage serverMessage)
   {
       ArrayList<String> formattedResponse = new ArrayList();
       formattedResponse.add("Status code: " + serverMessage.status + ", message:  " +  serverMessage.returnMsg);
       
       JSONParser parser = new JSONParser();
       try
       {
            JSONObject retObj = (JSONObject)parser.parse(serverMessage.returnMsg);
            Iterator <String> it = retObj.keySet().iterator();
            while (it.hasNext())
            {
                String key = it.next();
                String value =  retObj.get(key).toString();
               formattedResponse.add ( key +" " + value);
            }
       }
       catch (ParseException pe)
       {
            formattedResponse.add("InvalidJSON format for server rerurned data: \n" +  serverMessage.returnMsg);
       }
       return  formattedResponse;
   }
   
   // Format the server response as a  single "formatted" String for pinting
      public String getFormattedtServerResponse(ResponseMessage serverMessage)
      {
            ArrayList<String> formattedLines =  formatServerResponse(serverMessage);
            String printString= formattedLines.get(0);
            for (int i = 0; i < formattedLines.size(); i++)
            {
                printString += "\n    " +  formattedLines.get(i);
            }
            return printString;
      }  
      
      // Get the coordinates from is String representation
      public static Rectangle  getFaceCoordinates(String regionStr)
      {
          String faceCoords = regionStr.replaceAll("f|p","").replaceAll("\\{|\\}", "");       // strip the f|p and outer bracket
          boolean hasLandmarks = regionStr.contains("i") || regionStr.contains("n") || regionStr.contains("m");
          if (hasLandmarks)
          {
              // discard the landmark coordinates and get only the face coordinates
                String[]  landmarks = faceCoords.split("\t");           // separeted by tabs
                if (landmarks.length == 0)                                       // not a properly formatted string
                    return null;
                faceCoords = landmarks[0];
          }  
          faceCoords = faceCoords.replaceAll("\\[|\\]", "");
          String[] rect = faceCoords.split(",|;");
            if (rect.length != 4)           // ignore if parsing problem
                return  null;
            int x = Integer.valueOf(rect[0]).intValue();
            int y = Integer.valueOf(rect[1]).intValue();
            int w = Integer.valueOf(rect[2]).intValue();
            int h = Integer.valueOf(rect[3]).intValue(); 
            return new Rectangle(x, y, w, h);
      }
}
 