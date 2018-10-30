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
package fmservice.server.ops;

import fmservice.server.ops.imageops.GpuOpsManager;
import fmservice.server.storage.index.IndexStoreManager;
import fmservice.server.cache.ImageCacheManager;


import fmservice.server.global.FMContext;
import fmservice.server.global.DBContext;
import fmservice.server.cache.dbcontent.FMImage;
import fmservice.server.cache.dbcontent.ImageZone;
import fmservice.server.result.ImageIngestResult;
import fmservice.server.result.Status;
import fmservice.httputils.common.ServiceConstants;

import fmservice.server.global.Scope;
import fmservice.server.util.Timer;


import java.util.HashMap;
import java.util.ArrayList;

import java.util.Properties;
import org.apache.log4j.Logger;

/** FaceMatch Data Manager
 * @version $Revision: 1.0
*
* This is the (second tier) class that manages the persistent FaceMatch data, involving the 
* database and the indexed data - as they are accessed and updated during operations. 
* It does not directly deal with actual FaceMatch image operations 
* <P>
* It utilizes three lower level Manager objects to perform the required functions as follows:
*      1)  IndexStoreManager : to manage (add/delete) index descriptors for each ingested image
 *     2)  ImageCacheManager: To maintain the in-memory Cache, from the database and as new
 *              Image related objects are added/deleted during operation
 *     3) ImageIngestRecorder : To record necessary information, including updating the database,
 *                  when a new image is ingested or an existing one is removed from the system
* <P>
* It is instantiated as a Singleton object at startup, and performs the following functions at part of initialization:
*     - Receives FaceMatch configuration properties and  system-specific parameters
*     - Establishes the two GLOBAL  Context objects for the system (using the config properties)
*           a) DBContext - The database context object for dealing with the database
*           b) FMContext - The FaceMatch context object containing the DBContext and various FM configuration data
*                                   and establishing the "FM2 domain Scope" for all operations
*     - Instantiates the three singleton lower level Manager objects mentioned above (using Context objects)
*     - Determines if GPU is available or not for FaceMatch operations
*<P>
*
*  @version $Revision: 1.1 $
 * @date Feb 19, 2015
*
 * Change log:
 *     
 */
public class FMDataManager
{
    private static Logger log = Logger.getLogger(FMDataManager.class);
    private int initStatus = 0;
    private String[] errorData = {"0", ""};                    // error code and explanation string 
    private  Properties fmConfig = null;;                  // FM properties loaded from configuration file
   
   private DBContext dbContext = null;                 // Database connections, etc.
   private  FMContext fmContext = null;
     
   // private Scope scope = null;                                 // static DB data, loaded from DB
    
    protected static FMDataManager fmDataManager = null;
    protected IndexStoreManager indexStoreManager = null;
    protected ImageCacheManager indexCacheManager = null;
    protected  ImageIngestRecorder ingestRecorder = null;
 
    /*----------------------------------------------------------------------------------------------------*/
    /** Create the singleton FMDataManager object if not created already.
     * 
     * @param fmProperties FaceMatch2 configuration properties 
     * @return the Singleton FMDataManager object
     */
    public static FMDataManager createDataManager(Properties fmProperties)
    {
        if (fmDataManager == null)
            fmDataManager = new FMDataManager (fmProperties);
        else
            log.warn("FMDataManager already instantiated. Ignoring this call.");
        return fmDataManager;
    }
    
    /*----------------------------------------------------------------------------------------------------*/
    /** Get  the singleton FMDataManager object, assumed to be created already.
     * 
     * @return the FMDataManager object
     */
    public static FMDataManager getDataManager()
    {
        return fmDataManager;
    }
/*----------------------------------------------------------------------------------------------------*/
    protected  FMDataManager (Properties fmProperties)
    {
         fmDataManager = this;
        initStatus = initDataManager(fmProperties);
    }
 
    /*----------------------------------------------------------------------------------------------------*/
    /**
     * Get the FaceMatch Context object (containing FM2 configuration and other data).
    */
    public FMContext getFMContext()
    {
        return fmContext;
    }
 
    /*----------------------------------------------------------------------------------------------------*
    * To be invoked by the Top level manager
    */
    protected DBContext getDBContext()
    {
        return dbContext;
    }
    /*----------------------------------------------------------------------------------------------------*/
    // GPU is 'available'  when it is physically available and not turned off by the admin
    public boolean isGpuAvailable()
    {
         return fmContext.isGpuAvailable();
    }
    /*----------------------------------------------------------------------------------------------------*/            

    public Properties getFMConfig()
    {
        return this.fmConfig;
    }
    
    public ImageIngestRecorder getIngestRecorder()
    {
        return ingestRecorder;
    }
    
    public ImageCacheManager getIndexCacheManager()
    {
        return indexCacheManager;
    }
    
     public IndexStoreManager getIndexStoreManager()
    {
        return indexStoreManager;
    }
    /*----------------------------------------------------------------------------------------------------*/
    public int getStatus()
    {
        return initStatus;
    }
    /*----------------------------------------------------------------------------------------------------*/
    public String[] getError()
    {
        return errorData;
    }

    /********************************************************************************************/
    // Initialize the FaceMatch System using parameters specified in the configuration file
    // If there is any problem in initialization, it aborts (through exceptions etc.)
    // Steps:  
    //               1. Establish database connection
    //               2. Determine if GPU is available or not
    //               3. Define "domain scope" from static database tables
    //               4. Create/Initialize the IndexStoreManager
    //               5. Create/ntialize the ImageCacheManager
    //               6. Initialize the IngestRecorder
    /********************************************************************************************/
    protected int  initDataManager (Properties config)
    {
        // Establish database context for accessing the Facematch database
        fmConfig = config;
        dbContext  = null;
        try
        {
               dbContext = new DBContext(fmConfig);  
        }
        catch (Exception e)
        {
            log.fatal("Could not establish database connection", e);
            return ServiceConstants.DATABASE_ACCESS_EXCEPTION;
        }
         // Local database object cache managed by the CacheManager (not sent to MemCached)
         boolean hasGpu = false;
        boolean gpuEnabled = false;
        
        GpuOpsManager gpuManager = GpuOpsManager.getGpuManager();
        if (gpuManager != null)
        {
            hasGpu =  gpuManager.hasGPU();
            gpuEnabled =gpuManager.isGpuEnabled();
        }
        fmContext = new FMContext(dbContext, fmConfig, hasGpu, gpuEnabled);
        
        indexStoreManager = new IndexStoreManager(fmContext);
        indexCacheManager = new ImageCacheManager(fmContext, indexStoreManager);
        ingestRecorder  = new ImageIngestRecorder(fmContext);    
        return 1;
    }


    /*------------------------------------------------------------------------------------------------------------*/
    /**
    * Load  the image information from the database tables to memory cache.
    * Check the name of clients and their extents which should be loaded at system startup
    * 
    * @return Cache loading status 1 for success, 0 for failure
    *------------------------------------------------------------------------------------------------------------------*/
    public int loadExtentImagesToCache()
     {
         try
         {
                HashMap<String, ArrayList<String>> initialExtents = getExtentsToLoad();
                int numImages = indexCacheManager.loadExtentImages(initialExtents);
                if (numImages > 0)
                        log.info(">> Total of " + numImages + " images loaded to the Cache and added to MetadataIndexTrees.");
                else 
                        log.warn(" No initial image  loaded to cache");
                dbContext.complete();           // close connection to the database now
                return 1;
         }
         catch (Exception e)
        {
            log.fatal("Could not read from  database", e);
            return 0;
        }
    } 
    
    /*-------------------------------------------------------------------------------------------------------------------------------*/
   /**
    * Find the clients and their extents, that have to be loaded, from the config file
    * If none specified load all their  Image extents (which are active).
    * 
    * @return 
    *       A HashMap of each clientKey and its extent names to load at startup
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    
    public  HashMap<String, ArrayList<String>> getExtentsToLoad()
    {
        HashMap<String, ArrayList<String>> clientExtentList =     new HashMap<String, ArrayList<String>>();
       
         Scope scope = Scope.getInstance();
         ArrayList <String> clientNameList = Scope.getInstance().getAllClientNames();
         
        // check if any one is to be excluded from running
        String[] deferClients = null;
        String deferClientStr = fmConfig.getProperty("deferLoad.clients");
        if (deferClientStr != null)
        {
           deferClients = deferClientStr.split("\\W+");
        }
        
        if ( deferClients != null )
        {
            for (int i = 0;   i < deferClients.length; i++)
            {
                scope.getDeferredClientNames().add(deferClients[i]);
                if (clientNameList.contains(deferClients[i]))
                    clientNameList.remove(deferClients[i]);
            }
        }
        /**
        // for each client not in the "defer list", load data for all  active  extents
        **/
        for (int i = 0; i < clientNameList.size(); i++)            // for each client
        {
              ArrayList<String>  extentNames = scope.getExtentNamesForClient(clientNameList.get(i));
              clientExtentList.put(clientNameList.get(i), extentNames);
        }
        return clientExtentList;          
    }
    /*-----------------------------------------------------------------------------------------------------*/
  /** 
   * Update the database and the MetadataIndexTree following the successful ingest 
   * of an image or an imageRegion
   * @param extentId
   * @param ingestResult
   * @param metadataMap
   * @return 
   */  
    public  int  saveImageIngestInfo(ImageIngestResult ingestResult,
        HashMap<String, String> metadataMap)
    {
        // Update the database with the new images/zones etc recorded in the database
        int status = 0;
        Timer timer = new Timer();
        FMImage image =  ingestRecorder.storeImageIngestInfo( ingestResult, metadataMap);
        if (image != null)
        {
            ingestResult.postProcessTime = timer.getElapsedTime();
            status = 1;
        } 
        Timer.release(timer);
        return status;
    }
        
    /*------------------------------------------------------------------------------------------------------------*/
   /** Remove the specified image from the system.
    * Returns the number of records removed
    * --------------------------------------------------------------------------------------------------------*/
    public Status removeImageIngestInfo(FMImage image, ImageZone  zone)
    {
        return  ingestRecorder.removeImageIngestInfo(image, zone);
    }
   
}
