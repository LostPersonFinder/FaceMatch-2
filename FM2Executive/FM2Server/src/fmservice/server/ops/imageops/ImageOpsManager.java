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

import fmservice.server.ops.ImageExtentManager;
import fmservice.httputils.common.ServiceConstants;
import fmservice.server.fminterface.adapter.FaceMatchAdapterBase;

import fmservice.server.global.DBContext;
import fmservice.server.global.FMContext;
import fmservice.server.global.Scope;
import fmservice.server.ops.FMDataManager;

import fmservice.server.util.Timer;

//import fmservice.server.result.FMServiceResult;
import fmservice.server.result.*;
import fmservice.server.storage.rdbms.PerformanceRecorder;

import java.nio.MappedByteBuffer;
import java.sql.SQLException;

import java.util.Properties;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 *
 *
 */

public class ImageOpsManager implements ServiceConstants
{
    
    private static Logger log = Logger.getLogger(ImageOpsManager.class);
    
    // Save the dataManager object which handles both the imagestore and the Cache data
    protected FMDataManager fmDataManager;
    
    // singleton object for handling requests
    protected ImageExtentManager extentReqHandler;
    protected RegionOpsReqHandler regionReqHandler;
    protected FaceFindReqHandler  faceFindReqHandler;
    protected WholeImageMatchReqHandler wholeImageReqHandler;
    
    // FaceMatch options to be used for the image operation
    protected  HashMap  FaceMatchOptionsMap;
    
    protected int initStatus = -1;
    protected boolean gpuOn;
    
    // Map of each image vs. its index data in memory
    // This is not used as we are presently not reading index data to memoy
    // from the Java side, but from the C++ faceMatch lib  
    protected HashMap <String, MappedByteBuffer> image2indexMap;
    
    // Maintain a static counter to uniquely identify each request
    private int  OpsRequestNum;         // not currently used
    
    /*-----------------------------------------------------------------------------------------------*/
    public ImageOpsManager( FMDataManager  dataManager, Properties fmConfig)
    {
         fmDataManager = dataManager;
         init(fmConfig);
         
         OpsRequestNum = 0;
    }
    
    protected void   init(Properties fmConfig)
    {
        // initialize the interface to FaceMatch system.
         String faceMatchOptionsFile = fmConfig.getProperty("facematch.options.filename");
         if (faceMatchOptionsFile == null || faceMatchOptionsFile.trim().isEmpty())
         {
             log.error("Please provide FaceMatch options file name as property  \"facematch.options.filename\" in the FM config file.");
             initStatus = FM_INIT_ERROR;
             return;
         }
         
         // initialize the JNI framework and connection to the C++ FM library
         String fmLibName = (String) fmConfig.getProperty("facematch.nativeLibName");
         int fmInitStatus = FaceMatchAdapterBase.init(faceMatchOptionsFile, fmLibName);
         if (fmInitStatus != SUCCESS)
         {
             initStatus = FM_INIT_ERROR;
             return;
         }
         
         // Make sure that we have linked to the correct version of the FM Dynamic Link library
         String configFmLibVersion = (String) fmConfig.getProperty("fmlib.version");            // required version in config file
         String linkedFMLibVerson = FaceMatchAdapterBase.getFMLibVersion();
         if (!configFmLibVersion.equals(linkedFMLibVerson))
         {
             log.error("FaceMatchLibrary version mismatch, Expected " + configFmLibVersion +", Linked to " + linkedFMLibVerson);
             initStatus =FMLIB_VERSION_MISMATCH;
             return;
         } 
         log.info (">>> Using FaceMatch Library Version: " + linkedFMLibVerson);
        initStatus = SUCCESS;   
    }
    
    public int getInitializationStatus()
    {
        return initStatus;
    }
    
    public String getFMLibVersion()
    {
        return FaceMatchAdapterBase.getFMLibVersion();
    }
        
   /*------------------------------------------------------------------------------------------------------------*/
    /** perform the specified image operation by interfacing the FacaMatch image ops library.
     * 
     * @param service
     * @param operation
     * @param inputParams
     * @return results of ingest operation
     *-----------------------------------------------------------------------------------------------------------------------------*/
       public  FMServiceResult  processRequest(int service, int operation, 
           HashMap <String, Object>  inputParams)
       {
            gpuOn = Scope.getInstance().getFMContext().isGpuAvailable();
            FaceMatchAdapterBase.setGpuStatus(gpuOn);

           OpsRequestNum ++;
           Timer serviceTimer = new Timer();
           try
           {
               if (service == IMAGE_EXTENT_SVC)
               {
                     if  (extentReqHandler  == null)
                             extentReqHandler =  new ImageExtentManager(fmDataManager);
                    FMServiceResult  extentOpsResult  =  extentReqHandler.handleRequest(
                     OpsRequestNum, operation, inputParams );
                    extentOpsResult.serviceTime = serviceTimer.getElapsedTime();
                    Timer.release(serviceTimer);
                    return  extentOpsResult;
               }
                if (service == FACE_FIND_SVC)
                {
                    if  (faceFindReqHandler  == null)
                       faceFindReqHandler =  new FaceFindReqHandler(fmDataManager);
                    FMServiceResult faceFindResult  =  faceFindReqHandler.handleRequest(
                         OpsRequestNum, operation, inputParams );
                    recordServerPerformance(faceFindResult, true);
                    faceFindResult.serviceTime = serviceTimer.getElapsedTime();
                    Timer.release(serviceTimer);
                    return  faceFindResult;
                 }
                else if (service == FACE_MATCH_REGION_SVC)
                {
                    Timer t1 = new Timer();               
                    if ( regionReqHandler  == null)
                        regionReqHandler = new RegionOpsReqHandler(fmDataManager);
                    
                     FMServiceResult regionOpsResult = 
                        regionReqHandler.handleRequest(OpsRequestNum, operation, inputParams);   
                     Float faceMatchTime = t1.getElapsedTime();
                     
                     recordServerPerformance(regionOpsResult, true);
                     Float serviceTime = serviceTimer.getElapsedTime();
                     regionOpsResult.serviceTime  = serviceTime;
                     Timer.release(serviceTimer);
                     
                     log.trace("<------- RegionOpsReqHandler total Face match time: "  +
                             faceMatchTime + ",  Total Service Time: " +  serviceTime);
                     return  regionOpsResult;
               }
                 else if (service == WHOLE_IMAGE_MATCH_SVC)
                {
                    if ( wholeImageReqHandler  == null)
                        wholeImageReqHandler = new WholeImageMatchReqHandler(fmDataManager);
                     FMServiceResult  wholeImageOpsResult = null;
                       //wholeImageReqHandler.handleRequest(OpsRequestNum, operation, inputParams);    
                    return  wholeImageOpsResult;
               }  
                else     // unknown request
                 {
                     FMServiceResult invalidReqResult = new FMServiceResult(service, operation);
                     invalidReqResult.setStatus(INVALID_SERVICE,  ( "Service or  specified operation Type in " + service + "not implemented."));
                     Timer.release(serviceTimer);
                     return invalidReqResult;
                 }
           }
           catch (Exception e)                  // other types of errors caused by coding/runtime errors
           {
               log.error("Exception in handling service: " + service + ", operation: " + operation, e);
               
               FMServiceResult  errorResult = new FMServiceResult(service, operation);
               String errorMsg = e.getMessage();
               if (errorMsg == null || errorMsg.isEmpty())
                   errorMsg = "Unknown Server Error";
               errorResult.setStatus(INTERNAL_SERVER_ERROR,  errorMsg);
               Timer.release(serviceTimer);
               return errorResult;
           }
       } 
       
       /*--------------------------------------------------------------------------------------------------------*/
       // Record the time used to complete the operation by the FaceMatch2 server, including
       // the time taken by the FaceMatch library
       /*--------------------------------------------------------------------------------------------------------*/
       protected void recordServerPerformance(FMServiceResult serviceResult, boolean commit)
       {
          if (serviceResult.getStatusCode() != 1)
                return;                                 // operation not successful, so don't record in DB
            
          boolean record = false;
          FMContext fmContext = Scope.getInstance().getFMContext();
          
          int ops = serviceResult.getOperationType();
          if (ops == ServiceConstants.GET_FACES_OP)
              record = fmContext.isRecordingPeformance(ServiceConstants.GET_FACES);
          else if (ops == ServiceConstants.REGION_INGEST_OP)
              record = fmContext.isRecordingPeformance(ServiceConstants.INGEST);
          else if (ops == ServiceConstants.REGION_QUERY_OP)
              record = fmContext.isRecordingPeformance(ServiceConstants.QUERY);

          if (!record)
              return;
            
            DBContext dbContext = fmContext.getDBContext();
            PerformanceRecorder.recordServerPerformance(dbContext, serviceResult);
            try
            {
                  dbContext.commit();
                  dbContext.complete();
                  return;
            }
             catch (SQLException e)
            {
                log.error("Database Exception in recording Facematch performance in FM2 database", e);
            }

       }
}
