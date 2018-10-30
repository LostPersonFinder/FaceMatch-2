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
package fmservice.httputils.common;

import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import java.awt.Rectangle;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;


import org.apache.log4j.Logger;

/**
 *
 *
 * 
 * Date: August 11, 2009
 */
public class FormatUtils 
{
    
        private static Logger log = Logger.getLogger(FormatUtils.class);
        public static String FS = "/";
      
     /*************************************************************************************
     * De-serialize the byte stream (received from the server) to its object form 
     **************************************************************************************/
     public static Object deserializeObject(byte[] data)
     {
        if (data == null || data.length == 0)
             return null;
       
         Object deserializedObj = null;
        try
        {
          ByteArrayInputStream bais = new ByteArrayInputStream(data);
          ObjectInputStream ois = new ObjectInputStream(bais);
           deserializedObj = ois.readObject();
        }
        catch (Exception e)
         {
             log.error("Invalid object deserialization request", e);
         }
        return deserializedObj;
    }
     
    /*************************************************************************************
     * Serialize the object (to be sent to the server as request content) to a byte stream
     **************************************************************************************/
     public static byte[] serializeObject(Object obj)
     {
        byte[] data = null;
        try
        {
           ByteArrayOutputStream bout = new ByteArrayOutputStream();   
           ObjectOutputStream oout = new ObjectOutputStream(bout);    
           oout.writeObject(obj);   
           oout.close();   
           data = bout.toByteArray();
        }
        catch (Exception e)
         {
            log.error("Invalid object serialization request", e);
         }
        return data;
    }

  /************************************************************************
    * return the value of a positive integer parameter in the 
    * HTTP Request/ResponeHashMap
     * @param map
     * @param param
     * @return
     *******************************************************************/
     public static int getIntVal(Map map,  String param)
     {
         List values = (List)map.get(param);
         if (values == null || values.size() == 0)
            return -1;           
        String value = (String)values.get(0);
        return (Integer.parseInt(value));
     }

// convenience method
   public static  int getIntVal(Map map, int param)
   {
       return getIntVal(map, String.valueOf(param));
   }

   /*****************************************************************************/
  public static int[] getIntValues(Map map,  String param)
  {
      List values = (List)map.get(param);
      if (values == null || values.size() == 0)
            return null;
      
      int n = values.size();
      int[]  intValues = new int[n];
      for (int i = 0; i < n; i++)
      {
          try
          {
              String valueStr = (String)values.get(i);
              if (valueStr.equals(""))       // no value given 
                intValues[i] = 0; 
              else
                  intValues[i] = Integer.parseInt(valueStr);
          }
          catch (NumberFormatException ne)
          {
              intValues[i]  = -1;
          }    
      }        
      return intValues;
  }  
   
    /**********************************************************************************     
    // Get a value stored as a List in the parameter map
    * *****************************************************************************/
     public  static boolean getBooleanVal (Map map, String param)
     {
         List values = (List)map.get(param);
         if (values == null || values.size() == 0)
            return false;           
        String value = (String)values.get(0);
        return (value.equalsIgnoreCase("true")? true : false);
     }
   
       /********************************************************************************/
    
   public static HashMap buildMap(String[] names, Object[] values)
    {
        HashMap map = new HashMap();
        for (int i = 0; i < names.length; i++)
        {
            map.put(names[i], values[i]);
        }
        return map;
    }

    /**********************************************************************************
    * Create a HashMap with a  boolean value - usually for sending in a response header
    ********************************************************************************/
      public static HashMap  buildBooleanMap(String key, boolean value)
     {
         String valueStr = (value == true ? "true" : "false");
         HashMap map = new HashMap();
         map.put(key, valueStr);
         return map;
     }
      
   /******************************************************************************************
    * Create a HashMap with an integer  value - usually for sending in a response header
    **********************************************************************************************/
      public static HashMap  buildIntegerMap(String key, int ival)
     {
         String valueStr = String.valueOf(ival);
         HashMap map = new HashMap();
         map.put(key, valueStr);
         return map;
     }
      
 /*---------------------------------------------------------------------------------------------------------------------*/     
   /** Encode the full URL for an HTTP request from the client.
    * Discard trailing blanks in parameters if any
    */
    public static String encodeURLRequest(String serverURL, String service, 
                List<NameValuePair>params)
    {
        String requestStr = serverURL.trim();
       if (service != null && !service.isEmpty())
           requestStr += FS+service.trim();
       if (params != null && !params.isEmpty())
       {
           requestStr += "?";
           String paramString = URLEncodedUtils.format(params, "UTF-8");
           requestStr += paramString.trim();
       }
       return requestStr;
    }
/*---------------------------------------------------------------------------------------------------------------------
 /**
  * Validate the syntax of an URL (without opening the connection), by converting it to URI.
  */      
    public static boolean isValidURL(String url)
    {  
        URL u = null;
        try 
        {  
               u = new URL(url);  
               u.toURI();
               return true;
        } 
        catch (MalformedURLException e) { return false; }
        catch (URISyntaxException e) { return false; }
    } 
/*------------------------------------------------------------------------------*/
  /**
  * Validate the syntax of an URI, which might be an URL 
  */         
   public static boolean  isValidURISyntax(String uri)
   {
       try
       {
           URI u = new URI(uri);
           return true;
       }
       catch (URISyntaxException e)
       { 
           return false;
       }
   }
   /**
    * Verify that the image/face regions are specified in the proper format in the input.
    * The String is a JSON Array to allow multiples values for the parameter, as follows
    * f[x, y, w, h]\ f[x, y, w, h]...
    * where f => face with x, y, w, h => top left corner and width, height of whole face
    */
   public  static boolean isValidRegionFormat(String regStr)
   {
       Rectangle[] rects =  getRegionRects(regStr);
       return (rects != null);
   }
       
  /*---------------------------------------------------------------------------------------------------------------*/   
    public static  Rectangle[]  getRegionRects(String regStr)    
   { 
        if (regStr == null || regStr.length() < 10)
             return null;
    
        try
        {
            String[] regions = regStr.split("\t");       
            Rectangle[] rects = new Rectangle[regions.length];
            for (int i = 0; i < regions.length; i++)
            {
                String str = regStr;
                char fc = regStr.charAt(0);
                 if (fc  == 'f' ||  fc == 'p')
                    str = str.substring(1);
                
                String coordStr =str.replaceAll("^\\[", "").replaceAll("\\]$", "");
                String[] coord = coordStr.split(",|;");
                Rectangle r = new Rectangle(Integer.parseInt(coord[0]), Integer.parseInt(coord[1]),
                    Integer.parseInt(coord[2]), Integer.parseInt(coord[3]));
                rects[i] = r;
            }
            return rects;
        }
        catch(Exception e)
        {
            return null;
        }
   }
    
    protected static Rectangle getCoordinates(String coordStr)
  {
        coordStr = coordStr.replaceAll("\\[|\\]", "");       // remove the bracket
        String[] coords = coordStr.split(",|;");
        Rectangle r = new Rectangle ();
        r.x =  Integer.parseInt(coords[0]);
        r.y =  Integer.parseInt(coords[1]);
        r.width =  Integer.parseInt(coords[2]);
        r.height =  Integer.parseInt(coords[3]);
        return r;
  }
    
/*-------------------------------------------------------------------------------------------------*/            
     public static void main(String[] args)
    {
         String[] urlNames = {
            "https://pl.nlm.nih.gov/tmp/pfif_cache/2013-uttrakhand-floods.personfinder.google.orgSLASHperson.36617142__1116576081.png",
            "https://pl.nlm.nih.gov/tmp/pfif_cache/2012-08-philippines-flood.personfinder.google.orgSLASHperson.9537108__1047566831.png",
             "https://pl.nlm.nih.gov/tmp/pfif_cache/2012-08-philippines-flood.personfinder.tmp.gif"
        };
        
        for (int i = 0; i < urlNames.length;  i++)
        {
            boolean valid = isValidURL(urlNames[i]);
            System.out.println(urlNames[i] +" is : " + ( valid? "valid URL " : " NOT valid URL") );
        }
    }
}
