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
package fmservice.server.entry;


import fmservice.httputils.common.ServiceConstants;
import fmservice.server.global.ConfigurationManager;

import fmservice.server.ops.imageops.GpuOpsManager;
import fmservice.server.ops.imageops.ImageOpsManager;
import fmservice.server.ops.FMDataManager;
import fmservice.server.ops.FMClientManager;

import fmservice.server.global.Scope;
import fmservice.server.global.FMContext;

import fmservice.server.result.FMServiceResult;
import fmservice.server.result.ServerStatusResult;

import fmservice.server.util.Timer;
import fmservice.server.util.Utils;
        
import java.util.Properties;
import java.util.HashMap;


import org.apache.log4j.Logger;

/**
 * FMServiceManager - The front end class which manages operations of he FaceMatch2 system.
*
* It is responsible for initializing the FaceMatch system at start up time to handle 
* client requests received as HTTP REST-based messages.
*
* It is a singleton object, instantiated at FM2 Web Server  startup, and performs
* the following functions at part of initialization:
*     - Reads the configuration file for system-specific parameters
*     - Instantiates the various Singleton manager objects, namely:
*           ConfigurationManager, DataManager, ImageOpsManager, and GPU manager
*     -  Creates the singleton FaceMatchContext object with 
*               FM2 configuration, database connection info, etc 
*
*  If initializes fails or is incomplete, appropriate error messages are logged.
*----------------------------------------------------------------------------------------------------------------------
*
*  @version $Revision: 1.1 $
*
 * Change log:
 *     
 */
public class FMServiceManager implements ServiceConstants
{
    private static Logger log = Logger.getLogger(FMServiceManager.class);
    
    private int initStatus = 0;
    private String[] errorData = {"0", ""};                // error code and explanation string 
    private  Properties fmConfig = null;;                  // FM properties loaded from configuration file
   
    private FMContext fmContext = null;                // Full context, database and memory 
    private String fmVersion = "";                           // Facematch System Version
    private String fmConfigVersion = "";                  // Required FMLib version - from Configuration file
     private String HomeDir = ".";                          // TBD: Get from local text config file
   //-------------------------------------------------------------------------------------------------------
   // singleton objects. accessed from all HTTP Requests ( servletContext threads)
    protected static FMServiceManager fmServiceManager = null;
    protected static FMDataManager  fmDataManager = null;
    protected static GpuOpsManager  gpuManager = null;
    protected static ImageOpsManager  imageOpsManager = null;

    
  // -----------------------------------------------------------------------------------------------------*/
  /** Instantiate the singleton FMServiceManager, if not already instantiated
   * 
   * @param configFile  Configuration file to configure and  initialize the FM2 system
   * @return static FMServiceManager object
   * -----------------------------------------------------------------------------------------------------*/
  
    public static FMServiceManager createServiceManager(String configFile)
    {
        if (fmServiceManager == null)
            fmServiceManager = new FMServiceManager (configFile);
        else
            log.warn("FMServiceManager already instantiated. Ignoring this call.");
        return fmServiceManager;
    }

    
    /*-----------------------------------------------------------------------------------------------------*/
    /** Constructor.
    * 
    * @param configFile  Configuration file to configure and  initialize the FM2 system
    *
   /*-----------------------------------------------------------------------------------------------------*/
    
    protected  FMServiceManager (String configFile)
    {
        // initialize the system for facematch operations
        initStatus = initFaceMatchOperation(configFile);     
        if (initStatus != 1)
            errorExit();
            
        
        // find the available memory etc.
        log.info (">>> Heap Memory usage Info:   " + Utils.heapMemUsage() );
    }
    
    /*----------------------------------------------------------------------------------------------------*/	
    /** 
     * Return the singleton FMServiceManager object.
     */
    public  static FMServiceManager getFMServiceManager()
    {
        return fmServiceManager;
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
    /**Initialize the FaceMatch System using parameters specified in the configuration file.
    * <P> If there is any problem in initialization, it aborts (through exceptions etc.)
    * Steps:  1. Load configuration info
    *               2. Establish database connection
    *               3. Create FaceMatchContext object
    *              4. Define domain scope from static database tables (FM2 clients, ImageExtents. Metadata registries, etc)
    * </P>
    * @param configFile
    *               Facematch configuration file
    * @return initialization success or  failure
    /********************************************************************************************/
    protected int  initFaceMatchOperation (String configFile)
    {
        
     // Get system configuration from Proprties file
        ConfigurationManager.loadConfig(configFile);
      
       // get the Properties object representing the configuration
       fmConfig = ConfigurationManager.getConfig();
       
       // set the version FM system number - may be queried by clientss
       fmVersion = fmConfig.getProperty("fm2server.version");
       if (fmVersion == null)
       {
           fmVersion = "FM2:V_UNKNOWN";
           log.warn("FaceMatch Server Version number not provided in Configuration file. Using default: " + fmVersion);
       }
      
       /*----------------------------------------------------------------------------------------------*/ 
       // Initialize the GPUManager and allow usre tu use the GPU if it is functional
       gpuManager = GpuOpsManager.createGpuManager(fmConfig);
       gpuManager.setGpuUse(true);            // default: Use it
       //gpuManager.setGpuUse(false);            // Disable it for testing
       
       /*----------------------------------------------------------------------------------------------*/
       int status =  initializeDataManager();
       if (status == 1)
       {
            fmContext = fmDataManager.getFMContext();
       } 
       if (status != 1)
             return (status); 
       status = initializeImageOpsManager();
       if (status != 1)
           return 0;
       
  
     /*----------------------------------------------------------------------------------------------*/  
       // load database information on images and create metadataIndexTrees for
       // exiting sets
        Timer timer = new Timer(); 
        status = loadDBContentsToCache();
        float loadTime =  timer.getElapsedTime();
        Timer.release(timer);
        if (status == 1)
            log.info(">>> FaceMatch system Cache initialized successfully in " + (loadTime/1000) + " seconds <<<");
        else
        {
            log.warn("FaceMatch System could not load database contents to cache");
        }
        
        //------------------------------------------------------------------------------------------------
        // set performance recording to on (in future: off)
        fmContext.recordFMPerformance(true);
        
       return status;
     } 
    /*-----------------------------------------------------------------------------------------------------*/
    protected int  initializeDataManager()
    {
       fmDataManager = FMDataManager.createDataManager(fmConfig);
       int status = fmDataManager.getStatus();
       if ( status != SUCCESS)
       {
             log.fatal("Could not  instantiate FMDataManager");
       }
       return status;
    }
  /*-----------------------------------------------------------------------------------------------------*/
    /** Initialize the framework for processing FaceMatch imaging operations
     * 
     */
    protected int  initializeImageOpsManager()
    {
        if  (imageOpsManager == null)
        {    
            // Note: The following operation performs FaceMatch LOAD_LIBRARY function for shared objects (SO)
             imageOpsManager  = new ImageOpsManager(fmDataManager,  fmConfig); 
        }
      if (imageOpsManager.getInitializationStatus() != SUCCESS)
      {
             log.fatal("Could not initialize ImageOpsManager");
            return 0;
       }
      return 1;
    }
    
    
      /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Close the operations related to face matching due to Shared library load and other such errors 
     * 
     */
    protected void errorExit()
    {
        // TBD: Anything special to be done??
        
        log.fatal("\n#########################################\n"
                + "Facematch operations terminated due to initialization error. \n"
                + "#########################################");
        fmContext = null;
        fmServiceManager = null;
        fmContext = null;
        fmServiceManager = null;
       System.exit(-1);
    }
    
    /*------------------------------------------------------------------------------------------------------------------*/
    /** Load the image information for valid clients/extents to memory cache
     * @return cache loading status
    *------------------------------------------------------------------------------------------------------------------*/
    public int  loadDBContentsToCache()
    {
        int status = fmDataManager.loadExtentImagesToCache();
        return status;
    }
   
    /*-------------------------------------------------------------------------------------------------------------------------------*/
  /**
   * Check if the given credentials are valid for a Facematch admin from the database info
   * 
   * @param adminName
   * @param adminPW
   * @return true/false
   */
    public boolean verifyAdmin(String adminName, String adminPW)
    {
       return fmContext.getDomainScope().isAdmin(adminName, adminPW);
    }
  
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Close the operations related to fmservice.
     * 
     */
    public void close()
    {
        // TBD: Anything special to be done??
        log.info("Facematch operations closed by System administrator:");
        fmContext = null;
        fmServiceManager = null;
        return;
    }
    
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Turn the GPU usage to on or off  (by Admin  - for testing only)
     * GPU is available for follow-up FM operations  if it exists and its use is not turned off by admin  
     */
    public void setGpuUsage(boolean use)
    {
        String setting = use ? "with GPU" : "**without**  GPU" ;
        log.debug("Facematch operations will be peformed " + setting);
        gpuManager.setGpuUse(use);
        fmContext.setGpuStatus(gpuManager.isGpuEnabled());        // for FM operations
        return;
    }
    
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Check if GPU is available  for FM operations
     */
    public boolean isGpuAvailable()
    {
        return gpuManager.isGpuEnabled();
    }
   
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Return the FaceMatch system/software version number
     */
    public String getFMVersion()
    {
        return  fmVersion;
    }
    
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Return the  current Heap space usage by the FaceMatch system
     */
    public String getHeapSpaceUsageInfo()
    {
        return  Utils.heapMemUsage();
    }
    
    
   /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Turn on/off Performance recording for all  FaceMatch operation, as specified 
     * 
     * @param  record if true: record performance; stop recording otherwise
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    public void recordPerformance(boolean record)
    {

        if (record)
            log.info("Facematch operation performance recording is turned on");
        else
        {
            log.warn("Facematch operation performance recording is turned off");
        }
         
        fmContext.recordFMPerformance(record);      // turn on or off
    }
   
    /*-------------------------------------------------------------------------------------------------------------------------------*/
    /**
    * Turn on/off Performance recording of a specific FaceMatch operation 
     * 
     * @param  record if true: record performance; stop recording otherwise
     * @param  ops Type of operation (FaceFinding, Ingest or Query
     *                          null means all operations
   /*-------------------------------------------------------------------------------------------------------------------------------*/
    public void recordPerformance(boolean record, String ops)
    {
         if (record)
            log.info("Facematch operation performance recording is turned on");
        else
            log.warn("Facematch operation performance recording is turned off");
        
        if (ops == null || ops.isEmpty())
        {
             fmContext.recordFMPerformance(record);         // all types
         }
        else
        {
             fmContext.recordFMPerformance(record, ops);  
        }
    }
   /*---------------------------------------------------------------------------------------------*/
    
    public ServerStatusResult getServerStatusInfo()
    {
        ServerStatusResult statusResult = 
                new ServerStatusResult(INFO_SVC, GET_SYSTEM_STATUS, 1, "Successful");
        statusResult.serverVersion = fmVersion;
        statusResult.fmLibVersion = imageOpsManager.getFMLibVersion();
        statusResult.gpuStatus = gpuManager.isGpuEnabled();
        statusResult.heapSpaceUsage = getHeapSpaceUsageInfo();
        statusResult.dbConnectionOpen = fmContext.getDBContext().isValidConnection();
        statusResult.defaultFaceFindPref = 
                fmContext.getFMConfiguration().getProperty("facefind.pref.default");
        
        statusResult.facefindRecording = fmContext.isRecordingPeformance(GET_FACES);
        statusResult.ingestRecording=  fmContext.isRecordingPeformance(INGEST);
        statusResult.queryRecording = fmContext.isRecordingPeformance(QUERY);
        return statusResult;   
    }
    
 
    /**-------------------------------------------------------------------------------------------------------------------------------*
     * Perform the image processing operation requested by the Web client.
     * 
     * Note: For better performance, each requested operation is performed on the request thread,
     *  but access to database and cache updates are synchronized to avoid race conditions.
     * /*-------------------------------------------------------------------------------------------------------------------------------*/
    public  FMServiceResult  performService(int service, int operation, 
                HashMap <String, Object> inputParams)
    {
        if (service == ADMIN_SVC  && operation == ADD_FMCLIENT)
        {
           FMClientManager clientManager =  new FMClientManager(fmContext);
            FMServiceResult result  =  clientManager.addNewClient(ADMIN_SVC, ADD_FMCLIENT, inputParams);
            return result;
        }
        else if (service == ADMIN_SVC  && operation ==GPU_ON)
        {
            setGpuUsage(true);
            int status = gpuManager.isGpuEnabled() ? SUCCESS : FAILURE;
            ServerStatusResult result = new ServerStatusResult(service,  operation, status, "Request completed");
            return result;
        }
        
        else if (service == ServiceConstants.ADMIN_SVC  && operation == ServiceConstants.GPU_OFF)
        {
            setGpuUsage(false);
            int status =  (gpuManager.isGpuEnabled() == false) ? SUCCESS : FAILURE;
            ServerStatusResult result = new ServerStatusResult(service,  operation, status, "Request completed");
            return result;
        }
        else if (service == ServiceConstants.ADMIN_SVC  && operation == ServiceConstants.FM_SHUTDOWN)
        {
            ;
        }
        
        /*---------------------------------------------------------------------------------------------------*/
        // The following is not implemented
        
        
        
        String clientKey = null;
        boolean invalidKey = false;
        // A key is not necessary for getting all DB information
        //------------------------------------------------------------------------------------------------------------------      
        // for all other service  (FM) requests must have a client key (or a clientName for retrieving information
        // retrieve the key  and validate it
        String clientName =(String)inputParams.get(ServiceConstants.CLIENT_NAME_PARAM);
        if (clientName != null && !clientName.isEmpty())
        {
              clientKey = Scope.getInstance().clientName2Key(clientName);
              if (clientKey == null)
                    invalidKey = true;
        }
        else
        {
            if (operation != ServiceConstants.ALL_CLIENT_QUERY_OP)
            {
                clientKey =(String)inputParams.get(ServiceConstants.CLIENT_KEY);
                if (Scope.getInstance().getClientWithKey(clientKey) == null)
                    invalidKey = true;
            }
        }
        if (invalidKey)
        {
            FMServiceResult invalidKeyResult = 
                new FMServiceResult(ServiceConstants.INVALID_PARAM,  -1);
            invalidKeyResult.setStatus( ServiceConstants.INVALID_CLIENT_KEY,
                (clientKey + " is not a known key assigned to any FaceMatch client."));
            return invalidKeyResult;
        }
        return handleFMRequest(service, operation, inputParams);
    }
  /*-------------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * perform the specified image operation by interfacing with the FaceMatch image ops library.
     * <p>
     * All services and operations are defined as constants (integers) in the ServiceConstants module
     * according to the FM2  ICD and web.xml
     * 
     * @param service       FM2 service type 
     * @param operation   operation type for the service 
     * 
     * @param inputParams   Input parameters for the operation 
     * @return  FMServiceResult object, corresponding to the operation performed
     /*-------------------------------------------------------------------------------------------------------------------------------*/
       public   FMServiceResult  handleFMRequest(int service, int operation, 
           HashMap <String, Object>  inputParams)
       {
           FMServiceResult result = 
             imageOpsManager.processRequest(service, operation, inputParams); 
          return result;
       }

}
