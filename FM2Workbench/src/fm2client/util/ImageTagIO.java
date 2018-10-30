/*
Informational Notice:
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
an agency of the Department of Health and Human Services, United States Government.

- The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

- The license does not supersede any applicable United States law.

- The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

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

package fm2client.util;


import java.util.Properties;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.apache.log4j.Logger;


public class ImageTagIO
{
    
    private static Logger log = Logger.getLogger(ImageTagIO.class);
    
    
        
 /*------------------------------------------------------------------------------------------------------*/ 
// Read the mapping of image tags to URL from the input file in the form  of
// a JSON Array ("tag", "url") under a top object "Tag2imageMap"
/*---------------------------------------------------------------------------------------------------------*/     
   public static LinkedHashMap<String, String> readImageTagMap(String imageMapFile)
    {
        return readImageTagMap( imageMapFile, null, null);
    }
    
/*------------------------------------------------------------------------------------------------------*/ 
// Read the mapping of image tags to URL from the input file in the form  of
// a JSON Array ("tag", "url") under a top object "Tag2imageMap" for a given client
    // and extent
/*---------------------------------------------------------------------------------------------------------*/     
   public static LinkedHashMap<String, String> readImageTagMap(String imageMapFile, String client, String extent)
   {
         LinkedHashMap <String, String> tag2urlMap = new LinkedHashMap();
         File file = new File (imageMapFile);
        if (!file.exists())
            return tag2urlMap;                // no input provided, create a new Map
      
        try
        {
            FileReader fileReader = new FileReader(imageMapFile);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(fileReader);
            JSONObject jobj = (JSONObject)obj;
            
            String mapClient = null;
            String mapExtent = null;
           if (client != null && extent != null)
           {
                mapClient = (String)jobj.get("client");
                mapExtent = (String)jobj.get("extent");
                
                 if ( (mapClient != null && !mapClient.equalsIgnoreCase(client)) ||
                     (mapExtent != null && !mapExtent.equalsIgnoreCase(extent)))
                 return tag2urlMap;             // empty map
           }

            JSONArray mappingArray  = (JSONArray)jobj.get("Tag2ImageMap");

            for (int i = 0; i < mappingArray.size(); i++)
            {
                JSONObject mapObject = (JSONObject)mappingArray.get(i);
                String tag = (String)mapObject.get("tag");
                String url = (String)mapObject.get("url");
                tag2urlMap.put(tag, url);
            }
            fileReader.close();
            return tag2urlMap;
        }
        catch (IOException ioe)
        {
            log.error("Could not access Image Tag to URL mapping file " + imageMapFile);
            return null;
        }
      catch (ParseException pe) 
      {    
           log.error("Invalid JSON format. Error in parsing " + imageMapFile + " contents to JSONArray");
           pe.printStackTrace();
           return null;
      }
    }  
    
 /*-----------------------------------------------------------------------------------------------------------------*/ 
// write the mapping of image tags to URL from the input file in the form  of
// JSONObjects "key" : "value"
     
    public static  int writeImageMap(String client, String extent, LinkedHashMap<String, String> imageMap,
            String imageMapFile)
    {
        if (imageMap == null || imageMap.size() == 0)
            return 0;               // nothing to write
        
        JSONArray mappingArray = new JSONArray();
        Iterator <String> iter = imageMap.keySet().iterator(); 
        while (iter.hasNext())
        {
            String tag = (String)iter.next();
            String url = (String)imageMap.get(tag);
            JSONObject jobj = new JSONObject();
            jobj.put("tag", tag);
            jobj.put("url", url);
            mappingArray.add(jobj);
        }
        JSONObject mapObject = new JSONObject();
        mapObject.put("client", client);
        mapObject.put("extent", extent);
        mapObject.put("Tag2ImageMap", mappingArray);
        try 
        {
            File mapfile = new File(imageMapFile);
            File dir = mapfile.getParentFile();
            if (!dir.exists())
                dir.mkdirs();
            FileWriter file = new FileWriter(imageMapFile);
            file.write(mapObject.toJSONString());
            file.flush();
            file.close();
            return 1;
        } 
        catch (IOException e) 
        {
            log.error("Could not write to Image Tag to URL mapping file " + imageMapFile);
            return 0;
       }
    }
    
     /*--------------------------------------------------------------------------------------------------------*/ 
     public static String getImageTagMapFileName(Properties clientProperties,
                String clientName, String extentName)
     {
         String prefix = extentName+".";
         String imageTagFile = clientProperties.getProperty(prefix+"imageFile2tag.filename");
         if (imageTagFile == null)
         {
             // check  for the default file at the default location
               prefix = (String)clientProperties.getProperty("fm2test.datadir")+"/"+ clientName+"/"+extentName+"/info/";
               imageTagFile =     prefix+extentName+"ImageTag2URLMap.json";
           }
          imageTagFile = imageTagFile.trim();
          log.info("Using imageTag2URLMap file: " + imageTagFile);
         return imageTagFile;
     }
}
