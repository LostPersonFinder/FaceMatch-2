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
package fmservice.server.result;

import fmservice.httputils.common.ServiceConstants;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 *  Get query results about a Client or Extent or Image
 * 
 *
 * 
 */
public class DBQueryResult  extends FMServiceResult implements ServiceConstants
{
    public  ClientInfo clientInfo;
    public  ExtentInfo extentInfo = null;
    public ImageInfo imageInfo = null;
    
    
    public DBQueryResult(int svcType, int operType)
    {
        super (svcType, operType, 1, "");
        if (operType == CLIENT_QUERY_OP)
            clientInfo = new ClientInfo();
        else if (operType == EXTENT_QUERY_OP)
            extentInfo = new ExtentInfo();
         else if (operType == IMAGE_QUERY_OP)
            imageInfo = new ImageInfo();
    }
 
    /*-----------------------------------------------------------------------------------------------*/
    /** Convert self to a JSON string form (to be sent to the client side)
     */
     public  String convertToJSONString() 
     {
         JSONObject jsonObj = convertToJSONObject();
         String  jsonString = jsonObj.toJSONString();
         return jsonString;
     }
    
    
    // Convert query result to JSON string
    protected   JSONObject  convertToJSONObject()
    {
        JSONObject  resObj = new JSONObject();
        fillStandardInfo(resObj);

       if (isSuccess())
       {   
            if (operation == CLIENT_QUERY_OP)
                resObj.put("ClientInfo", clientInfo.convertToJSONObject());
            else if (operation == EXTENT_QUERY_OP)
                resObj.put("ExtentInfo", extentInfo.convertToJSONObject());
            else if (operation == IMAGE_QUERY_OP)
                resObj.put("ImageInfo", imageInfo.convertToJSONObject());
            else
                ;
       }
       return resObj;
    }
    
    /*---------------------------------------------------------------------------------------------*/
    // Query result pertainig to a Client
    public class ClientInfo
    {
        public String clientName;
        public String creationDate;
        public String clientKey;
        public boolean storeThumbnails;
        public  int numActiveExtents;
        public  int numTotalExtents;
        public int totalImages;
        
        public ArrayList <MetadataInfo> metadataInfoList;
        public ArrayList <ExtentInfo> extentInfoList;
        
        // static info about client's metadata
        public class MetadataInfo
        {
             public String clientName;
             public String metadataName;
             public boolean isSearchable;
             public String[] validValues;
             public String defaultValue;
         }
        
        /*------------------------------------------------------*/
         public ClientInfo()
         {
             metadataInfoList = new ArrayList();
             extentInfoList = new ArrayList();
             numActiveExtents = 0;
             numTotalExtents = 0;
             totalImages = 0;
        
         }
         
         public ExtentInfo newExtentInfoInstance()
        {
            return new ExtentInfo();
        }
         
         public void addMetadataInfo(MetadataInfo mdInfo)
        {
             metadataInfoList.add(mdInfo);
        }
         
         public MetadataInfo newMetadataInfoInstance()
         {
             return new MetadataInfo();
         }
         
         public void addExtentInfo(ExtentInfo extInfo)
        {
            extentInfoList.add(extInfo);
            numTotalExtents++;
            if (extInfo.isActive)
                numActiveExtents++;
            totalImages += extInfo.numImages;
        }
         
         
         //-------------------------------------------------------------------------------------------
         // For returning to  service requestor
         //-------------------------------------------------------------------------------------------
         public JSONObject  convertToJSONObject()
         {
               // First Convert to a JSONObject 
            JSONObject  resObj = new JSONObject();
            resObj.put("clientName", clientName);
            resObj.put("clientKey", clientKey);
            resObj.put("creationDate", creationDate);
            resObj.put("numImages", totalImages);
            resObj.put("thumbNailsStored", storeThumbnails);
            resObj.put("numExtents", numTotalExtents);
            resObj.put("activeExtents", numActiveExtents);
            

            // add the static metadata information
             JSONArray metadataArray = new JSONArray();
             int nm = metadataInfoList.size();
             for (int i = 0; i < nm; i++)
            {
                MetadataInfo mdInfo = metadataInfoList.get(i);
                JSONObject  mdObj = new JSONObject();
                mdObj.put("name", mdInfo.metadataName);
                
                String validValues = mdInfo.validValues[0];
                for (int j = 1; j < mdInfo.validValues.length; j++)
                    validValues += (", "+ mdInfo.validValues[j]);
                mdObj.put("validValues", validValues);
                
                mdObj.put("searchable", mdInfo.isSearchable);
                if (mdInfo.isSearchable)
                    mdObj.put("defaultValue", mdInfo.defaultValue);
                metadataArray.add(mdObj);
            } 
            resObj.put("metadataFields", metadataArray);
             
            // add properties of all Extents for this client  
             if (extentInfoList != null && extentInfoList.size() > 0)
             {
                // add properties of all Extents for this client  
                JSONArray jsonArray = new JSONArray();
                for (int i = 0; i < extentInfoList.size();  i++)   
                {
                    JSONObject extentObj = extentInfoList.get(i).convertToJSONObject();
                    jsonArray.add(extentObj);
                }
                resObj.put("extentInfo", jsonArray);
            }   
            return resObj;
        }
    }
    //----------------------------------------------------------------------------------------------------------//
    // Query result pertaining to an ImageExtent
    //----------------------------------------------------------------------------------------------------------//
     public class ExtentInfo
    {
        public String extentName;
        public String description;
        public String clientName;
        public String creationDate;
        public boolean isActive;
        public int numImages;
        public ArrayList<HashMap<String, String>> metadataGrouping;
       //public HashMap <String, String> metadataMap;  // [metadata field name, value] + count
        
        public ExtentInfo()
        {          
        }
         //-------------------------------------------------------------------------------------------
         // For returning to  service requestor
         //-------------------------------------------------------------------------------------------
        public JSONObject   convertToJSONObject()
        {
             // First Convert to a JSONObject 
            JSONObject  resObj = new JSONObject();
            resObj.put("clientName", clientName);
            resObj.put("extentName", extentName);
            resObj.put("description", description);
            resObj.put("creationDate", creationDate);
            resObj.put("numImages", numImages);

            // add number of images in each metadatagroup
            if (numImages > 0)
            {
                JSONArray metadataArray = new JSONArray();
                int ns = metadataGrouping.size();
                for (int i = 0; i < ns; i++)
               {
                   HashMap<String, String>metadataMap = metadataGrouping.get(i);
                   JSONObject  mdObj = new JSONObject();
                   Iterator <String> it = metadataMap.keySet().iterator();
                   while (it.hasNext())
                  {
                      String key = it.next();
                      mdObj.put(key, metadataMap.get(key));
                   }  
                   metadataArray.add(mdObj);
               }
                resObj.put("imageGrouping", metadataArray);
            }
            return  resObj;
        }
        
          public String convertToJSONString()
         {
             JSONObject jsonObject = convertToJSONObject();
             return jsonObject.toJSONString();
         }
     }
   //-----------------------------------------------------------------------------/
     public class ImageInfo 
    {
         public String clientName;
         public String extentName;
         public String imageTag;
         public String imageURL;
         public String ingestDate;
         public int numRegions;
         public ArrayList<String> imageRegions;
         public HashMap<String, String> mdNameValuePair;
     
        public  ImageInfo()
        {
        }
        
        //----------------------------------------------------------------------------------------------------------//
        // For returning to  service requestor
        //----------------------------------------------------------------------------------------------------------//
        public JSONObject convertToJSONObject()
        {
            // First Convert to a JSONObject 
            JSONObject  resObj = new JSONObject();
            resObj.put("clientName", clientName);
            resObj.put("extentName", extentName);
            resObj.put("imageTag", imageTag);
            resObj.put("imageURL", imageURL);
            resObj.put("ingestDate", ingestDate);
            resObj.put("numRegions", numRegions);

            // add Region coordinates
            String faceRegionStr = "";
            for (int i = 0; i < imageRegions.size(); i++)
            {
                faceRegionStr += imageRegions.get(i);
                if (i > 0)
                    faceRegionStr += ",  ";
            }
            resObj.put("regions", faceRegionStr);
            
            // add image metadata
             JSONArray metadataArray = new JSONArray();
         
            Iterator <String> it = mdNameValuePair.keySet().iterator();
            while (it.hasNext())
            {
                String fieldName = it.next();
                String filedValue = mdNameValuePair.get(fieldName);
                String mdInfo = fieldName +" : " + filedValue;
                metadataArray.add(mdInfo);
            }
            resObj.put("imageMetadata", metadataArray);
            return  resObj;
        }
     }
}
