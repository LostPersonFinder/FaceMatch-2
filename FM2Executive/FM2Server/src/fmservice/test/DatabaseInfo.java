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
package fmservice.test;


import fmservice.httputils.common.ServiceConstants;

import fmservice.server.cache.CacheKeyGen;
import fmservice.server.cache.dbcontent.FMClient;
import fmservice.server.cache.dbcontent.FMImage;
import fmservice.server.cache.dbcontent.ImageExtent;
import fmservice.server.cache.dbcontent.ImageZone;
import fmservice.server.cache.dbcontent.MetadataField;

import fmservice.server.cache.ImageCacheManager;
import fmservice.server.cache.InvertedMetadataTree;
import fmservice.server.global.Scope;
import fmservice.server.ops.FMDataManager;

import fmservice.server.result.DBQueryResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.awt.Rectangle;

import org.apache.log4j.Logger;

/**
 *
 *
 */
public class DatabaseInfo implements ServiceConstants
{
    
    private static Logger log = Logger.getLogger(DatabaseInfo.class);
    
    // get all information about a specific client with a given name or key

     //-------------------------------------------------------------------------------------------------------
     //
     // Get Client information based upon its key
     //
    public  DBQueryResult getClientInfoForKey(String clientKey)
    {
           FMClient fmClient = Scope.getInstance().getClientWithKey(clientKey);
           if (fmClient == null)
           {
                log.error ("No client found with key: " + clientKey);
                return null;
           }
          
           DBQueryResult queryResult =  new DBQueryResult(DATABASE_QUERY_SVC, CLIENT_QUERY_OP);
           addClientData(fmClient, queryResult);
           return queryResult;
    }
    
     //-------------------------------------------------------------------------------------------------------
     // Get Client information based upon its name
     //
     public  DBQueryResult getClientInfoForName(String clientName)
    {
        DBQueryResult queryResult =  new DBQueryResult(DATABASE_QUERY_SVC, CLIENT_QUERY_OP);
        String clientKey = Scope.getInstance().clientName2Key(clientName);
        if (clientKey == null)
        {
            queryResult.setStatus(-1, "Invalid Client name: " + clientName +"; no key found.");
            return queryResult;
        }
       
        return getClientInfoForKey(clientKey); 
    }

//---------------------------------------------------------------------------------------------
    protected void addClientData(FMClient fmClient, DBQueryResult queryResult)
    {
         DBQueryResult.ClientInfo clientInfo = queryResult.clientInfo;
         clientInfo.clientKey = fmClient.getKey();
         clientInfo.clientName = fmClient.getName();

         clientInfo.creationDate = fmClient.getCreationDate().toString();
         clientInfo.storeThumbnails = fmClient.isStoreThumbnails();
         
         // store metadata information
         String clientName = fmClient.getName();
         HashMap <String, MetadataField> mdFieldMap = fmClient.getMetadataMap();
         Iterator <String> it = mdFieldMap.keySet().iterator();
         while (it.hasNext())
         {
             DBQueryResult.ClientInfo.MetadataInfo mdInfo = clientInfo.newMetadataInfoInstance();
             String mdFieldName = it.next();
             MetadataField mdField = mdFieldMap.get(mdFieldName);
             mdInfo.clientName = clientName;
             mdInfo.metadataName = mdFieldName;
             mdInfo.validValues = mdField.getValidValues() ;
             mdInfo.defaultValue = mdField.getDefaultValue();
             mdInfo.isSearchable = mdField.isSearchable();
             clientInfo.addMetadataInfo(mdInfo);
         }
         // Add ExtentInfo etc. - converting from internal to external form
          try
         {
             ArrayList <ImageExtent> imageExtents = fmClient.getImageExtents();
             ArrayList<DBQueryResult.ExtentInfo> extentInfoList = new ArrayList();
             for (int i = 0; i < imageExtents.size(); i++)
             {
                 addExtentInfoForClient(queryResult, imageExtents.get(i));
             }
         }
          catch (Exception e)
          {
              log.error("Error retrieving image extents for client " + clientName + ", "+ e.getMessage());
              queryResult.setStatus(-1, "Internal error in retriving Extent information for client " + clientInfo.clientName);
          }
           queryResult.setStatus(1, "");
     }
    //-------------------------------------------------------------------------------------------------------------------------//
    protected void addExtentInfoForClient(DBQueryResult queryResult, ImageExtent imageExtent)
    {
        DBQueryResult.ClientInfo clientInfo = queryResult.clientInfo;
        DBQueryResult.ExtentInfo extentInfo = clientInfo.newExtentInfoInstance();

        fillExtentInfo(imageExtent, extentInfo);
        clientInfo.addExtentInfo(extentInfo);
    }
    
    protected void fillExtentInfo(ImageExtent imageExtent, DBQueryResult.ExtentInfo extentInfo)
    {
        int extentId = imageExtent.getID();
        FMClient client = Scope.getInstance().getClientWithExtent(extentId);

        extentInfo.clientName = client.getName();
        extentInfo.extentName = imageExtent.getName();
        extentInfo.description = imageExtent.getDescription();
        extentInfo.creationDate = imageExtent.getCreationDate().toString();
        extentInfo.isActive = imageExtent.isActive();
        fillExtentImageInfo(extentInfo, imageExtent);
    }
    //-----------------------------------------------------------------------------------------------------------------------
    protected void fillExtentImageInfo(DBQueryResult.ExtentInfo extentInfo, ImageExtent imageExtent)
    {
        try
        {
            extentInfo.numImages = imageExtent.getImages().size();
            if (extentInfo.numImages > 0)
                fillMetadataInfo(extentInfo,  imageExtent);
        }
        catch(Exception e)
        {
            log.error ("Error getting database information for ImageExtent: " + imageExtent.getID(), e);
            extentInfo.numImages = -1;          // some error indicator
        }
    }
    
    //-----------------------------------------------------------------------------------------------------------------------
    // if the extent is loaded to memory, i.e. active:  get the info from cache - otherwise ignore it
    // (i.e. do not query the database to gather information for inactive/old extents)
    //-----------------------------------------------------------------------------------------------------------------------
    protected void fillMetadataInfo(DBQueryResult.ExtentInfo extentInfo, ImageExtent imageExtent)
    {
        if (imageExtent.isActive())
        {
            ImageCacheManager cacheManager = FMDataManager.getDataManager().getIndexCacheManager();
           InvertedMetadataTree mdTree = cacheManager.getMetadataTree(imageExtent.getID());  
            String[] branchNames = mdTree.getBranchNames();
            
           ArrayList<HashMap<String, String>> metadataGroups = new ArrayList();
            for (int i = 0; i < branchNames.length; i++)
            {
                int imagesInBranch = mdTree.getImagesInBranch(branchNames[i]).size();
                
                //decode the branch name for metadata field name and field value
                // Note: The final string representation would be as follows:  "gender: female, age:adult, count=5"
                LinkedHashMap<String, String> mdKeyValueMap = CacheKeyGen.decodeMetadataBranchName(branchNames[i]);
                
                // just add the number of images to the field vaue(e.g. fenale)
                mdKeyValueMap.put("count", String.valueOf(imagesInBranch));
                metadataGroups.add(mdKeyValueMap);
            }
            extentInfo.metadataGrouping =   metadataGroups;          
        }
    }
    
     //----------------------------------------------------------------------------------------------
    // Get information about a specific image Extent with a given name
    //----------------------------------------------------------------------------------------------
     public DBQueryResult getExtentInfoForClientKey(String clientKey, String extentName)
     {
          String clientName = Scope.getInstance().getClientName(clientKey);
          if (clientName == null)
          {
              DBQueryResult queryResult =  new DBQueryResult(DATABASE_QUERY_SVC, EXTENT_QUERY_OP);
              queryResult.setStatus(-1, "No client key found for client with key " + clientKey);
              return queryResult;
          }
          return getExtentInfoForClient( clientName,  extentName);   
     }   
    //----------------------------------------------------------------------------------------------
    // Get information about a specific image Extent with a given name
    //----------------------------------------------------------------------------------------------
     public DBQueryResult getExtentInfoForClient(String clientName, String extentName)
     {
         DBQueryResult queryResult =  new DBQueryResult(DATABASE_QUERY_SVC, EXTENT_QUERY_OP);
          ImageExtent imageExtent = Scope.getInstance().getImageExtent(clientName, extentName);
          if (imageExtent == null)
         {
              queryResult.setStatus(-1, "No ImageExtent key found for client with name " + extentName);
              return queryResult;
         }
         fillExtentInfo(imageExtent, queryResult.extentInfo);
         return queryResult;
     } 

    //----------------------------------------------------------------------------------------------
    // Get information about a specific image with a given tag
   //----------------------------------------------------------------------------------------------
      
     public DBQueryResult getImageInfoForClientKey(String clientKey, String extentName, String imageTag)
     {
         DBQueryResult queryResult =  new DBQueryResult(DATABASE_QUERY_SVC, IMAGE_QUERY_OP);
         String clientName = Scope.getInstance().getClientName(clientKey);
         if (clientName == null)
         {
             queryResult.setStatus(-1, "No client found with key  " + clientKey);
             return queryResult;
         }
         return getImageInfoForClient(clientName,  extentName, imageTag);
     }
     

    //--------------------------------------------------------------------------------------------- 
    // Return query result for a given ingested image
    //---------------------------------------------------------------------------------------------
     public DBQueryResult getImageInfoForClient(String clientName, String extentName, String imageTag)
     {
         DBQueryResult queryResult =  new DBQueryResult(DATABASE_QUERY_SVC, IMAGE_QUERY_OP);
         ImageExtent imageExtent = Scope.getInstance().getImageExtent(clientName, extentName);
         if (imageExtent == null)
          {
              queryResult.setStatus(-1, "ImageExtent " + extentName + " not found for client with name  " + clientName);
              return queryResult;
          }
         
          FMImage fmImage;
          try
          {
                 fmImage = FMImage.findImageByUniqueTag(Scope.getInstance().getDBContext(),
                                                imageExtent.getID(), imageTag);  
          }
          catch (Exception e)
          {
              String msg = "Internal error in getting image with Tag " + imageTag + " found for client " 
                      + clientName + " in ImageExtent "+ extentName;
               log.error(msg, e);
              queryResult.setStatus(-1, msg);
              return queryResult;

          }
          if (fmImage == null)
         {
              queryResult.setStatus(-1, "No image with Tag " + imageTag + " found for client " + clientName + 
                      " in ImageExtent "+ extentName);
          }
          else
          {
                      // format to external form
                    fillImageInfo(fmImage, clientName, extentName, queryResult.imageInfo);
          }
          return queryResult; 
     }
     
    //--------------------------------------------------------------------------------------------- 
    // Return query result for a given ingested image
    //---------------------------------------------------------------------------------------------
    protected  void fillImageInfo(FMImage image, String clientName, String extentName, 
            DBQueryResult.ImageInfo imageInfo)
    {
        imageInfo.clientName = clientName;
        imageInfo.extentName = extentName;
        imageInfo.imageTag = image.getUniqueTag();
        imageInfo.imageURL = image.getImageSource();
        imageInfo.ingestDate = image.getCreationDate().toString();
        
        // add zone information
        imageInfo.numRegions = image.getNumZones();
        ArrayList<ImageZone> zones = image.getZones();
        imageInfo.imageRegions = new ArrayList();
        for (int i = 0; i < zones.size(); i++)
        {
            Rectangle zoneRect = zones.get(i).getDimensions();
            imageInfo.imageRegions.add(zoneRect.toString());
        }
        // Add metadata information
        imageInfo.mdNameValuePair = image.getMetadataValues();
        return; 
    }
}

