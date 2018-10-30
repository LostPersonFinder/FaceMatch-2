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
package fmservice.server.ops.imageops;

import fmservice.httputils.common.FormatUtils;
import fmservice.httputils.common.ServiceConstants;

import fmservice.server.cache.dbcontent.FMImage;
import fmservice.server.cache.dbcontent.ImageExtent;
import fmservice.server.cache.dbcontent.FMClient;
import fmservice.server.cache.ImageCacheManager;
import fmservice.server.cache.dbcontent.DBUtils;
import fmservice.server.cache.dbcontent.ImageZone;

import fmservice.server.result.ImageIngestResult;
import fmservice.server.result.ImageQueryResult;
import fmservice.server.result.RemoveImageResult;
import fmservice.server.result.MultiExtentQueryResult;
import fmservice.server.result.FMServiceResult;
import fmservice.server.result.Status;

import fmservice.server.fminterface.adapter.RegionIngestAdapter;
import fmservice.server.fminterface.adapter.RegionQueryAdapter;

import fmservice.server.ops.FMDataManager;
import fmservice.server.global.FMContext;
import fmservice.server.global.DBContext;
import fmservice.server.global.Scope;

import fmservice.server.result.FaceRegion;
import fmservice.server.util.AgeGroupAllocator;
import fmservice.server.util.Timer;

//import java.nio.MappedByteBuffer;
import java.io.File;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collection;

import java.awt.Rectangle;

import org.apache.log4j.Logger;



/**
 * This class in called  on the server side to perform face finding operation by invoking
 * the FaceFinder object in the C++ FaceMatch library. T
 * The linking is performed using JNI modules connecting java classes to C++ DLL.
 * 
 * Note: Each of FM operations is performed on a separate thread to be non-blocking for
 * other incoming requests.
 * 
 *
 */
public class RegionOpsReqHandler implements ServiceConstants
{
    private Logger log = Logger.getLogger(RegionOpsReqHandler.class.getName());
    
    protected FMDataManager  fmDataManager;
    protected Properties fmConfig;
    protected FMContext fmContext;
    
    protected Scope scope;
    
    protected  HashMap<String, ImageSearchContext>  imageMatchCtxMap;      // for each extent/metadata node
    protected RegionIngestAdapter regionIngestAdapter;
    protected RegionQueryAdapter  regionQueryAdapter;
    protected String  indexMatchType  = "";          // for linear/ FLANN matching etc
    
    boolean okToQuery = false;
    
    int  requestID;                                 // facematching request ID (counter) since start up

    public RegionOpsReqHandler(FMDataManager dataManager)
    {
       fmDataManager = dataManager;
       scope = Scope.getInstance();
       fmConfig = scope.getFMConfig();
       fmContext = scope.getFMContext();
        
       //FaceMatchOptionsMap = FaceMatchOptionsReader.getFFOptions();
       regionIngestAdapter = new RegionIngestAdapter();

       // initiaze the Active extents known to the system for query
       int nextents   = initializeExtentsForQuery();
       okToQuery = (nextents > 0);
        indexMatchType = Scope.getInstance().getIndexMatchType();
    }
    
    /*-----------------------------------------------------------------------------------------------*/
    // Build the  InvertedMetadataTree with the current images etc. 
    // for each ImageExtent for performing queries, using the 
    // index type (DIST/SIFT etc.) in the FM configuration file
    //--------------------------------------------------------------------------------------------------*/
    protected int initializeExtentsForQuery()
    {
       ImageExtent[] imageExtents;
       ArrayList<String> deferredClientNames;
       ArrayList<ImageExtent> initExtents = new ArrayList();
       try
       {
           // Get the list of active ImageExtents that we may query
            deferredClientNames = Scope.getInstance().getDeferredClientNames();
            imageExtents = ImageExtent.findAll(fmContext.getDBContext());
            if (imageExtents == null)
                return 0;           // nothing to initialize

            for (int i = 0; i < imageExtents.length; i++)
            {
                ImageExtent extent = imageExtents[i];
                if (!extent.isActive())
                    continue;
                 
                // check if it belongs to a client for which all activities are deferred.
                 FMClient client = FMClient.find(fmContext.getDBContext(), extent.getClientID());
                 if (client == null)
                      continue;           // something wrong with DB??
                 if (deferredClientNames.contains(client.getName()))
                     continue;
                 initExtents.add(extent);
            }
       }
       catch (SQLException sqle)
       {
           log.error("Database error in accessing ImageExtent objects", sqle);
           return 0;
       }

       int extentCount = initExtents.size();
       if (extentCount == 0)
       {
           log.warn("No Active ImageExtents  currently exist and/or initialized for query operation." );
           return 0;
       }
        for (int i = 0; i <extentCount; i++)
        {
            fmDataManager.getIndexCacheManager().initSearchContextsForQuery(initExtents.get(i));
        }
       return extentCount;
    }

    /*----------------------------------------------------------------------------------------------------------
    * Perform image region (zones or faces) related operations, such as
    * ingesting faces in an image, querying for matching  images, etc.
    *
    *--------------------------------------------------------------------------------------------------------------*/
    public FMServiceResult  handleRequest(int reqId, int operation, 
        HashMap <String, Object>inputParams) throws Exception
    {
         requestID = reqId;
         FMServiceResult  serviceResult = null;
         
         if (operation == REGION_INGEST_OP)
         {
             serviceResult  =  ingestImageRegions(reqId, inputParams);
             return serviceResult;
         }
        else  if (operation ==REGION_QUERY_OP)
           serviceResult = queryRegionMatches(reqId, inputParams);
        
        else  if (operation == MULTIEXTENT_REGION_QUERY_OP)
           serviceResult = multiQueryRegionMatches(reqId, inputParams);
        
       else  if (operation ==REGION_REMOVE_OP)
           serviceResult =  removeIngestedRegions(reqId, inputParams);
         else
         {
             String errorMsg =  "Invalid Region related request: " + operation + " received.";
             log.error (errorMsg);
             serviceResult = new FMServiceResult(INVALID_OPERATION,  INVALID_SERVICE);
             serviceResult.setStatus(INVALID_OPERATION, errorMsg);
         }
         serviceResult.requestId = reqId;
         return serviceResult;
     }
    
    
    /** 
     * Ingest an input image.
     * The input parameters from the client provided in the HashMap  are:
    *   <param name="Key"></param>
    *   <param name="extentID"></param>
    *   <param name="url"></param>
    * --Image metadata appropriate for each client. For example:
    *       <param name="gender"></param>
    *       <param name="age"></param>
    *       <param name="region"></param>
    * 
    * @param requestID
    * @param inputParams ;
    * @return 
    */
    protected ImageIngestResult ingestImageRegions(int requestID, HashMap<String, Object> parameters)
    {
       
        // retrieve the input parameters
        String extentName = (String) parameters.get(EXTENT_NAME);
        String clientKey = (String) parameters.get(CLIENT_KEY);
        String clientName = Scope.getInstance().getClientName(clientKey);
        ImageExtent extent = Scope.getInstance().getImageExtent(clientName, extentName);
         if (extent == null || extent.getID() <= 0)
        {
            ImageIngestResult  errorResult  = new ImageIngestResult(-1);
            errorResult.setStatus(ServiceConstants.INVALID_EXTENT_NAME,
                "Extent " + extentName + " does not exist.");
            return errorResult;
        }

        // initialize the return object
        int extentId = extent.getID();
        ImageIngestResult ingestResult = new ImageIngestResult(extentId);
        if (!extent.isActive())
        {
            ingestResult.setStatus(INACTIVE_IMAGE_EXTENT, "Image Extent " + extentName + 
                " is not active for ingesting new images" );
            return ingestResult;
        }

        /**
         * check if an image was already ingested with the same tag and same
         * extentID.
         */
        String imageFileName = (String) parameters.get(URL);
        ingestResult.imageURL = imageFileName;
         String imageTag = (String) parameters.get(IMAGE_TAG);
        ingestResult.imageTag = imageTag;
        ingestResult.extentId = extentId;
        
        Status status = assertUniqueImageID(extentId, imageTag);
        if (!status.isSuccess())
        {
             ingestResult.setStatus(status);
             return ingestResult;
        }        
        
        // check if regions are already specified as input parameters
        String[] faceRegions = null;
        String regionStr = (String) parameters.get(REGION);
        if (regionStr != null)
        {
            faceRegions = FaceRegion.getRegionsInRequest(regionStr);
        }


        /**
         * If the image refers to an external URL, copy and save the data
         * locally. requestID is used to generate a temporary unique filename
         * for downloading
         */
        
        String url = imageFileName;
        if (!RegionIngestAdapter.isValidImageType(url))
        {
            String errorMsg =  "Invalid URL, image type not supported by FaceMatch";
            ingestResult.setStatus(BAD_IMAGE_URL, errorMsg);
             return ingestResult;
        }
        
        boolean isURL = FormatUtils.isValidURL(url);

        String localImageFileName = ImageLoader.loadClientImage(fmConfig,
            imageTag, (String) parameters.get(CLIENT_KEY), extentName, imageFileName, isURL);
        if (localImageFileName == null)
        {
            ingestResult.setStatus(BAD_IMAGE_URL, "Could not retrieve/access  image file " + imageFileName);
            return ingestResult;
        }

        // evaluate w.r.t. whether or not the GPU is available - user cannot enable/disable it per request
        boolean useGPU = fmDataManager.isGpuAvailable();
        String faceDetectOption = extent.getFacefindPerformanceOption();
        if (faceDetectOption == null)
            faceDetectOption = OPTIMAL;

        // determine the root to the index file store for this extent
        String indexStoreRoot  = scope.getIndexStoreRootPathForExtent(extentId);

        /*----------------------------------------------------------------------------------------------------*/
        // Ask the  RegionIngestAdapter to generate index descriptors for image regions 
        // and return index  file names
        //-----------------------------------------------------------------------------------------------------*/
        ingestResult = regionIngestAdapter.ingestRegions(extentId,
            indexStoreRoot, url, localImageFileName, imageTag, faceRegions, useGPU, faceDetectOption);
        
        boolean recordIngest = 
            ingestResult.getIngestedRegionCount() > 0;

       // if a thumbnail to be created, create after a successful ingest, before deleting local file
       if (recordIngest)
           createThumbnail(ingestResult, localImageFileName, extentName);

        // Delete the local file       
          ingestResult.imageURL =  imageFileName;
        if (isURL)
        {
            File localFile = new File(localImageFileName);
           localFile.delete();
        }
        if (!recordIngest)
        {
            log.warn("No regions ingested for image " + ingestResult.imageURL);
            String errorMsg =  "No faces found in the image, image not ingested";
            ingestResult.setStatus(NO_FACES_IN_IMAGE, errorMsg);
            return ingestResult;
        }
        // Store ingest information in the database and also in the index cache
        storeIngestInfo(ingestResult,  parameters);
        return ingestResult;
    }
     
       /*--------------------------------------------------------------------------------------------------------------------------*/
    /**Remove an input image or a specified face region (zone) within image
     * 
     * The input parameters from the client provided in the HashMap  are:
    * <param name="Key"></param>
    * <param name="extentID"></param>
    * <param name="imageTag"></param>
    * 
    * @param requestID  - currently not used
    * @param inputParams    // ingestInputParams = {image tag, region etc};
    * @return 
    */
     protected FMServiceResult  removeIngestedRegions(int requestID, HashMap <String, Object>parameters)
     {
         // initialize the return object
         String imageTag = (String) parameters.get(IMAGE_TAG);
         String extentName = (String) parameters.get(EXTENT_NAME);
         RemoveImageResult  result = new RemoveImageResult(extentName, imageTag);
         
        // retrieve the input parameters and validate
         String clientKey = (String)parameters.get(CLIENT_KEY);
        
        int extentId = scope.getExtentId(clientKey, extentName);
        if (extentId <= 0)         // extent does not exist or was deleted
        {
            result.setStatus(INVALID_PARAM, "ImageExtent " + extentName + "does not exist or was deleted");
            return result;
         }
        ImageExtent extent = DBUtils.getExtentWithID(fmContext.getDBContext(),  extentId);
         if (extent == null ||  !extent.isActive())
        {
            result.setStatus(INVALID_PARAM, "ImageExtent " + extentName + " is not currently active");
            return result;
        }
        /*------------------------------------------------------------------------------------------------*/
         /**
          * retrieve the image information from the database
          **/
        String region = (String)parameters.get(REGION);       // we allow only one region at a time

         DBContext dbContext = fmContext.getDBContext();
         FMImage image = null;
          ImageZone zone = null; 

         try
         {
            image = FMImage.findImageByUniqueTag(dbContext, extentId, imageTag);
            if  (image == null)         // Tag not found, unique
            {
                   result.setStatus(INVALID_PARAM, "Image With tag " + imageTag+ " not found for imageExtent " + extentName);
                   return result;
            }
            result.imageId = image.getID();
            // if a region is given, check if it is an ingsted region
            if (region != null)
            {
                FaceRegion faceRegion = FaceRegion.parseFMString(region);
                Rectangle r = faceRegion.regionCoord;
                zone = image.getZoneByRegion(r);
                if (zone == null)
                {
                   result.setStatus(INVALID_PARAM, "Region " + region + " not found for image with tag " + imageTag);
                   return result;
                 }
            }
         }  // region check
         catch (Exception e)
         {
              result.setStatus(DATABASE_ACCESS_EXCEPTION, "Error retrieving image with tag " + imageTag +
                  " or region from database" +  e.getMessage());
              log.error("Error retrieving image with tag " + imageTag +  " from database " ,  e);
               return result;
        }

         
         result = performRemoveRegionOps(result, image, region, zone);
       return result;
     }
     
 /*-----------------------------------------------------------------------------------------------------------------------------------*/
  /** performImageRemoval
   * Remove an image or zone as requested by the caller
   *
   * We need to perform four functions in the following order now, which should be synchronized
   *        1) If the index files are currently loaded to the ImageMatcher for query, remove from the set
   *        2) Update the inverted Metadata Tree by removing the image or region
   *        3) Delete the indexed data files from the IndexStore for the  index (query) version
   *        4) Delete the image/zone record from database
   *      
   *-----------------------------------------------------------------------------------------------------------------------------------*/
     RemoveImageResult performRemoveRegionOps( RemoveImageResult  opsResult, FMImage image,  
             String region, ImageZone zone)
     {
          Status status  =  removeFromIndexCache(image, region);
           if (!status.isSuccess())
           {
                opsResult.setStatus(status);
                return opsResult;
           }
        // remove the info from file system cache
         int numZones = (zone == null) ? image.getNumZones() : 1;
        status = fmDataManager.removeImageIngestInfo(image, zone);
        if (status.isSuccess())
        {
            // set the count of zones removed
            opsResult.removeCount = numZones;
        }
        opsResult.setStatus(status);
        return opsResult;
     } 

     /*---------------------------------------------------------------------------------------------------------------*/
     /** Create a thumbnail for the ingested image, if the client asked for it
      * 
      * @param result
      * @param imageFileName
      * @param extentName 
      */
     protected int createThumbnail(ImageIngestResult result,  
         String imageFileName,  String extentName)
     {
          int extentId = result.extentId;
           FMClient client = scope.getClientWithExtent(extentId);
           if (!client.isStoreThumbnails())
               return 0;
         
        // get the thumbnail root
            String rootPath = client.getThumbnailRoot();
            String thumbHeight = fmConfig.getProperty("thumbnail.maxHeight", "80");
            String thumbWidth= fmConfig.getProperty("thumbnail.maxWidth", "80");
            int height = Integer.parseInt(thumbHeight);
            int width = Integer.parseInt(thumbWidth);
            String thumbType =  fmConfig.getProperty("thumbnail.imagetype", "jpeg");
            String thumbnailPath = extentName+"/"+result.imageTag+"_thumb." + thumbType;
            String thumbnailFileName = rootPath+"/"+thumbnailPath;
            
            // create the  thumbnail and store in the designated directory
            int status = ThumbnailGenerator.createThumbnail(imageFileName, thumbnailFileName, 
                width, height, thumbType);
            if (status == 1)
               result.thumbnailPath = thumbnailPath;
            else
                 result.thumbnailPath = null;
            return status;    
     }
     
    /*-----------------------------------------------------------------------------------------------------------*/
    //  perform post processing, including storing data in the FaceMatch server database
   /*-----------------------------------------------------------------------------------------------------------*/
    protected int storeIngestInfo(ImageIngestResult ingestResult, HashMap <String, Object>parameters)
    {
        Timer dbTimer = new Timer();            // post processing start

        // Determine the input metadata parameters provided for this image (or the defaults)
        HashMap<String, String> metadataMap = 
                getInputMetadata(ingestResult.extentId, parameters, false);

        // update the database with ingest data
        int updateStatus  = fmDataManager.saveImageIngestInfo(ingestResult, metadataMap);
     
        Status ppStatus;
        if (updateStatus != SUCCESS)
            ppStatus = new Status(INTERNAL_SERVER_ERROR, "Error in updating FM database with  ingested image information." );
        else
            ppStatus = new Status(SUCCESS, "Image ingest information successfully recorded.");
        ingestResult.postProcessStatus = ppStatus;
        
        // also add it to the in-memory index cache (Metadata Index Tree etc.) for queries, etc
        updateIndexCache(ingestResult);
        ingestResult.postProcessTime = dbTimer.getElapsedTime();
        Timer.release(dbTimer);
        return ppStatus.statusCode;
    }
     /*---------------------------------------------------------------------------------------------------------------*/
     // Update th search context for follow up queries corresponding to the 
    // ingest of the given image
    /*---------------------------------------------------------------------------------------------------------------*/
     protected int updateIndexCache(ImageIngestResult ingestResult)
     {
        String indexType = ingestResult.indexType;
        String indexVersion = ingestResult.indexVersion;
        try
        {
            FMImage image = FMImage.find(fmContext.getDBContext(), ingestResult.imageId);
            ImageCacheManager indexCacheManager = fmDataManager.getIndexCacheManager();
            indexCacheManager.addIngestedImageToSearchSet(image, indexType, indexVersion);
        }
        catch (Exception e)
        {
            log.error("Could not add image with id: "+ ingestResult.imageId + " to index cache for query", e);
            return 0;
        }
        return 1;
     }
     
     /*---------------------------------------------------------------------------------------------------------------*/
     // Update th search context for follow up queries by removing the given region from cache
     /*---------------------------------------------------------------------------------------------------------------*/
     protected  Status removeFromIndexCache(FMImage image, String region)
     {
        try
        {
            ImageCacheManager indexCacheManager = fmDataManager.getIndexCacheManager();
            indexCacheManager.removeImageFromSearch(image, region);
        }
        catch (Exception e)
        {
            log.error("Could not remove image/zone from  index cache for query", e);
           return new Status( -1,  "Could not remove image/zone from  index cache for query");
        }
        return new Status( SUCCESS, "");
     }
   

     /*----------------------------------------------------------------------------------------------------------------
     ** Check if a given image has a new tag or a duplicate one
     @param
     @ return true if a duplicate one
     *--------------------------------------------------------------------------------------------------------------*/
     protected  Status assertUniqueImageID(int extentId , String imageTag)
     {
         try
         {
            DBContext dbContext = fmContext.getDBContext();
            FMImage image = FMImage.findImageByUniqueTag(dbContext, extentId, imageTag);
            if  (image == null)         // Tag not found, unique
               return (new Status(SUCCESS, ""));
            else
            {
                  String errorMsg =  "Duplicate ImageID. Image with image tag " + imageTag +
                      " aready ingested to FaceMatch for this event.";
                return (new Status(DUPLICATE_IMAGE_TAG, errorMsg));
             }  
        }
        catch (SQLException sqle)
        {
                String errorMsg =  "SQL Exception in accessing database: " + sqle.getMessage();     
                Status status = new Status(DATABASE_ACCESS_EXCEPTION, errorMsg);
                return status;
        }
     }
    
    /*--------------------------------------------------------------------------------------------------------------------*/
    /*  Search the indexed datasets to determine similarity  matches for faces  in the given 
    * image
    *--------------------------------------------------------------------------------------------------------------------*/
    protected ImageQueryResult queryRegionMatches(int requestID, HashMap <String, Object>parameters)
    {
       // retrieve the input parameters
        String extentName = (String) parameters.get(EXTENT_NAME);
        ImageQueryResult queryResult = queryRegionMatchesInExtent(requestID, extentName, parameters);
        return queryResult;
    }
    
   /*--------------------------------------------------------------------------------------------------------------------*/
    /*  Search the indexed datasets to determine similarity  matches for faces  in the given 
    * image within a given extent
    * if deleteLocal is true, delete the locally copied file of an URL after the query
    * Note: An Extent does not have to be active for queries, only for ingest
    *--------------------------------------------------------------------------------------------------------------------*/
    
    protected ImageQueryResult queryRegionMatchesInExtent(int requestID, String extentName, 
            HashMap <String, Object>parameters)
    {
        Timer debugQueryTimer = new Timer();
        // retrieve the input parameters
        String clientKey = (String) parameters.get(CLIENT_KEY);
        String clientName = Scope.getInstance().getClientName(clientKey);
        ImageExtent extent = Scope.getInstance().getImageExtent(clientName, extentName);
        if (extent == null || extent.getID() <= 0)
        {
            ImageQueryResult  errorResult  = new ImageQueryResult(-1);
            errorResult.setStatus(INVALID_EXTENT_NAME,
                "Extent " + extentName + " does not exist.");
            return errorResult;
        }
        
         int extentId = extent.getID();
         ImageQueryResult  queryResult  = new ImageQueryResult(extentId);
        String  imageFileName = (String)parameters.get(URL);     
        queryResult.imageURL = imageFileName;
        if (!RegionQueryAdapter.isValidImageType(imageFileName))
        {
            String errorMsg =  "Invalid URL, image type not supported by FaceMatch";
            queryResult.setStatus(BAD_IMAGE_URL, errorMsg);
             return queryResult;
        }
                 
         // check if face regions are already specified by the caller
        String[] faceRegions = null;
        String faceRegionSpec  = (String)parameters.get(REGION);
         if (faceRegionSpec != null)
            faceRegions = FaceRegion.getRegionsInRequest(faceRegionSpec);
         
         boolean isURL = FormatUtils.isValidURL(imageFileName);
         

        /*  If an external URL, copy and save the data locally. */
        String localImageFileName;
        localImageFileName = ImageLoader.loadClientImage(fmConfig, String.valueOf(requestID),
             (String)parameters.get(CLIENT_KEY), extentName, imageFileName, isURL);
        if (localImageFileName == null)
        {
            queryResult.setStatus(BAD_IMAGE_URL,  "Could not retrieve/access  image file " + imageFileName);
            return queryResult;
        }
        
        
         /**
         // Get the image metadata and determine the set of indexed data sets (domains) which need
         * to be searched for the given image.
         **/
         ImageSearchContext[] searchContexts =  getSearchContextsForImage(extentId, parameters);
         if (searchContexts == null || searchContexts.length== 0)
         {
              queryResult.setStatus(NO_IMAGE_AVAILABE, 
                  "No ingested images or images with same metadata available for matching in Extent " + extentName);      
              return queryResult;
         }

         // tell FaceMatch whether or not to use the GPU
         boolean requestGPU =  fmDataManager.isGpuAvailable();
         System.out.println("performing queries with  requestGPU " + (requestGPU? "true" : "false") +
                 " using " + searchContexts.length + " search contexts");
     
         // Get the optional parameters tolerance and max matches (pass as null if not given)
         Float tolerance =null;
         String toleranceStr =  (String)parameters.get(TOLERANCE);
         if (toleranceStr != null)
            tolerance = Float.valueOf(toleranceStr);
         
         Integer maxMatches = null;
         String maxMatchStr =  (String)parameters.get(MAX_MATCHES);
         if (maxMatchStr != null)
             maxMatches =  Integer.valueOf(maxMatchStr);
         
       // Ask the  RegionMatch Adapter to  get the ID of the matching images with ranking
       // Also check we are performing FLANN index matching
       RegionQueryAdapter  queryAdapter = new RegionQueryAdapter();
       queryAdapter.setVerboseMode(false);
       
       // Check if FLANN index matching should be performed
        if (indexMatchType.equals("FLANN"))
            queryAdapter.setMatchType(indexMatchType);

       // Note: original image file name is used for messages only
       String faceDetectOption = extent.getFacefindPerformanceOption();
       if (faceDetectOption == null)
            faceDetectOption = OPTIMAL;

       float queryInitTime = debugQueryTimer.getElapsedTime();
       log.trace("------- Initial set up time including URL download, prior to query : " + queryInitTime);
       
       queryResult = queryAdapter.queryMatches(extentId, searchContexts,
           localImageFileName, faceRegions, tolerance, maxMatches, requestGPU, faceDetectOption);
       
        float faceQueryTime = debugQueryTimer.getElapsedTime() - queryInitTime;
        log.trace("------- Time to perform  face match by QueryAdapter: " + faceQueryTime);
        queryResult.totalQueryTime = faceQueryTime;      // facefinding+indexloading+matching for all regions
       
       // Since the matched images refer to local copies of actual URLs, we need to convert from local file names
       // to actual URLs by checking into the database.
        // Note: first truncate to maxMatches to avoid unnecessary overhead of dealing with all matches
        queryResult.truncateMatches();
        setIngestImageFilenames(queryResult);

        if (isURL)
        {
            Timer t = new Timer();
            File localFile = new File(localImageFileName);
            localFile.delete();
            log.trace("Time to delete local file " + localImageFileName + ":  " + t.getElapsedTime());
            Timer.release(t);
        }
        
        queryResult.imageURL = imageFileName;  
        if (queryResult.isSuccess())
        {
             queryResult.setStatus( SUCCESS,  queryResult.getNumQueryRegions() + " face regions in input image matched in " + 
                queryResult.totalQueryTime + " msec.");
        }
         Timer.release(debugQueryTimer);
         return queryResult;
     } 
    /*-------------------------------------------------------------------------------------------------------------
    // Set the actual image URLs, rather than the locally copied file names given to FMLib
    // in the query result to be returned to the caller
    // The imageURL ir retrieved from the database by matching against the unique tag
    /*-------------------------------------------------------------------------------------------------------------*/
    protected void setIngestImageFilenames(ImageQueryResult queryResult)
    {
        Timer t = new Timer();
         int extentId = queryResult.extentId;
        DBContext dbContext = fmContext.getDBContext();
        int n = 0;
        ArrayList <ImageQueryResult.RegionMatchResult> regionMatchResults = queryResult.getRegionMatchResults();
        
        // For each region of the query image
        for (int i = 0; i < regionMatchResults.size(); i++)
        {
            // get all matches for this region being returned to the caller
            ImageQueryResult.RegionMatchResult matchResults= regionMatchResults.get(i);
            TreeMap<Float, ArrayList<String[]>> matches = matchResults.getMatches();
           
            // All  filenames and tags  for all returned matches :  elements 0 and 1 respectively of each match
            Collection< ArrayList<String[]>>  matchDetailColl = matches.values();
            Iterator<ArrayList<String[]>>  it = matchDetailColl.iterator();
            while (it.hasNext())
            {
                // matchData has all matches at a given distance; usually only 1
                ArrayList<String[]> matchData = it.next();
                for (int j = 0; j < matchData.size(); j++)
                {
                    String[] matchDetails = matchData.get(j);
                    String imageTag = matchDetails[1];
                    try
                    {
                        // if it has a region number, strip that
                        int index = imageTag.indexOf(":"+REGION_TAG);
                        if (index > 0)
                            imageTag = imageTag.substring(0, index);
                        FMImage ingestImage = FMImage.findImageByUniqueTag(dbContext, extentId, imageTag);
                        if (ingestImage == null)
                        {
                            log.error("Server  internal error; could not match image with tag " + imageTag + ", Extend ID: " + extentId);
                            continue;
                        }
                         matchDetails[0] = ingestImage.getImageSource();    // first element is the URL - substitutes local image filename
                         n++;
                    }
                    catch (Exception e)
                    {
                            log.error("Server  internal error; Database exception in getting image source for tag " + imageTag, e);
                            continue;
                    }
                } // end for
            } // end while
        }   
       log.trace(" --- Time to set "+ n + " Ingest file names in Query result: " + t.getElapsedTime());
       Timer.release(t);
    }
  /*------------------------------------------------------------------------------------------------------------*/
  /* 
    * Note: We copy the query image for each extent, to avois unnecessary complexity 
    * in using the locally copy for each extent and then cleaning up only once after all the queries
    * but we detect the faces only once
    /*------------------------------------------------------------------------------------------------------------*/

    protected MultiExtentQueryResult multiQueryRegionMatches(int requestID, 
            HashMap <String, Object>parameters)
    {
         // retrieve the input parameters
        String clientKey = (String) parameters.get(CLIENT_KEY);
        String clientName = Scope.getInstance().getClientName(clientKey);
    
        String extentNameParam = (String) parameters.get(EXTENT_NAMES);
        String[] extentNames = extentNameParam.split("\\W*,\\W*");
        ArrayList <ImageExtent> extents = new ArrayList();
        
        for (int i = 0; i < extentNames.length; i++)
       {
            // retrieve the input parameters
            String extentName = extentNames[i];
            ImageExtent extent = Scope.getInstance().getImageExtent(clientName, extentName);
            if (extent == null || extent.getID() <= 0)
            {
                log.error ("Invalid ImageExtent " + extentName + " with Client key: " + clientKey + " in query. Ignored.");
                continue;
            }
            // check if images available for the extent;
            try
            {
                if (extent.getImages().isEmpty())
                {
                   log.warn(  "No ingested images or images with same metadata available for matching in Extent " + extentName);        
                   continue;
                }
            }
            catch(Exception e)
            {
                log.error("SQL Exception accessing image list for Extent " + extent.getName() +", skipping query");
                continue;  
            }
             extents.add(extent);
       }
        
        MultiExtentQueryResult meQueryResult = new MultiExtentQueryResult();
        if (extents.isEmpty())
        {
            meQueryResult.setStatus(ServiceConstants.INVALID_EXTENT_NAME, 
                    "No valid Image Extents with available images found in the request");
            return meQueryResult;
        }
            
        // Perform search for the first good extent
        for (int i = 0; i < extents.size(); i++)
        {        
            ImageExtent extent = extents.get(i);
            ImageQueryResult queryResult = queryRegionMatchesInExtent(requestID, extent.getName(), parameters);
          //  if (queryResult.regionEntries == null || queryResult.regionEntries.isEmpty())     // no matches found
              if (queryResult.regionMatchResults == null || queryResult.regionMatchResults.isEmpty())     // no matches found
                continue;
            
            meQueryResult.addMatchResult(queryResult);          // sets face regions and facefind time
            // add the face regions as a parameter to avoid re-localizing the faces in the query URL
            if (parameters.get(REGION) == null)
            {
                String[] faceRegions = meQueryResult.queryRegions;
                String faceRegionStr = faceRegions[0];
                for (int j = 1; j < faceRegions.length; j ++)
                    faceRegionStr += "\t"+faceRegions[j];
                parameters.put(REGION, faceRegionStr);
            }
        }
        meQueryResult.setStatus(SUCCESS, "Query terminated successfully.");
       return meQueryResult;
     }      
        
        
    
  /*------------------------------------------------------------------------------------------------------------*/
    // Determine the search space for images that should be searched for the query image,
    // based upon the images  specified metadata
    // inputParams refers to parameters provided in the request
    /*------------------------------------------------------------------------------------------------------------*/
   protected ImageSearchContext[]  getSearchContextsForImage(int extentId,  HashMap inputParams)
   {
       // Determine the metadata fields/values specified for the query iimage, or use defaults
       HashMap<String, String> metadataMap = getInputMetadata(extentId, inputParams, true);
       
       ImageExtent extent = DBUtils.getExtentWithID(fmContext.getDBContext(), extentId);
       if (extent == null)
       {
           log.error("No Image extent found for the given extent id: " + extentId);
           return null;
       }
       ImageCacheManager indexManager = fmDataManager.getIndexCacheManager();
       ImageSearchContext[]   searchContexts = 
           indexManager.getSearchContextsForQueryImage(extent, metadataMap);
       return searchContexts;
   }
      
    /*------------------------------------------------------------------------------------------------------------*/
   /**
    * Determine the input metadata parameters provided for this image.
    * @return a Map of metadata field name and the specified value in the ingest data.
    *   if the field was not specified, return the default value.
    *  Note The "Age" field is treated separately and converted to ageGroup for search.
    */
   protected HashMap<String, String>  getInputMetadata( int extentId, 
           HashMap<String, Object> inputParams, boolean isSearch)
   {
       HashMap <String, String> imageMetadata = new HashMap();
       
       HashMap <String, String[]> searchableMetadata = scope.getSearchableMetadataValueSet(extentId);
       Iterator <String> it = searchableMetadata.keySet().iterator();
       while (it.hasNext())
       {
           String smFieldName = it.next();   // searchable metadata field name
           String defaultValue = scope.getDefaultMetadataValue(extentId, smFieldName);
           
           // If age is given, convert it to agegroup to help in  search
           String value = "";
           if (smFieldName.equals(AGE_GROUP))
           {
               value = getAgeGroupToUse(inputParams, isSearch);
                if (value == null || value.isEmpty())
                {
                    imageMetadata.put(smFieldName, defaultValue);
                    log.info ("Saving/Querying Age metadata as " + imageMetadata.get(smFieldName));
                    continue;
                }
           }
           else
           {
                String[] validValues = searchableMetadata.get(smFieldName);
                if (inputParams.containsKey(smFieldName))
                {
                     String valueStr =  (String) inputParams.get(smFieldName);
                     if (valueStr == null || valueStr.isEmpty())
                     {
                         imageMetadata.put(smFieldName, defaultValue);
                         continue;
                     }

                     // check if the given value is  isvalid
                     for (int i = 0; i < validValues.length; i++)
                     {
                         if (valueStr.equalsIgnoreCase(validValues[i]))
                         {
                             value = valueStr;
                             break;
                         }
                     }    // end for
               }   // end if
                if (value.isEmpty())         // not provided or invalid
                     value = defaultValue;
           }    // end else
           imageMetadata.put(smFieldName, value);
        }   
       return imageMetadata;
   }
   
 /*--------------------------------------------------------------------------------------------------------------*/  
     // get age or agegroup. If both specified, ignore agegroup.
    //-------------------------------------------------------------------------------------------------------------*/
       protected String getAgeGroupToUse(HashMap<String, Object> inputParams, boolean isSearch)
       {
            String  agegroup = (String) inputParams.get(AGE_GROUP); 
            if (agegroup != null)
            {
                agegroup = agegroup.toLowerCase();
                 if (!agegroup.matches(VALID_AGE_GROUPS))
                     agegroup = "";
            }
            // No agegroup specified, check age      
            else
            {
                String age = (String) inputParams.get(AGE);            // user specified age
                if (age != null)
                {
                    int ageVal = Integer.valueOf(age);
                    if (isSearch)
                        agegroup = AgeGroupAllocator.convertAgeToQueryGroup(ageVal);
                    else
                         agegroup = AgeGroupAllocator.convertAgeToGroup( ageVal);
                    System.out.println("Using agegroup = " + agegroup + " for age " + ageVal); 
                }
            }
            if (agegroup == null)  agegroup = "";
            return agegroup.toLowerCase();
       }
}
 
