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
package fmservice.server.fminterface.adapter;

import fmservice.httputils.common.ServiceConstants;
import fmservice.server.storage.index.IndexStoreManager;
import fmservice.server.ops.imageops.GpuOpsManager;

import fmservice.server.fminterface.proxy.FRDProxy;
import fmservice.server.fminterface.proxy.FaceFinderProxy;
import fmservice.server.fminterface.proxy.FaceRegionMatcherProxy;
import fmservice.server.global.ConfigurationManager;
import fmservice.server.result.FaceRegion;

import fmservice.server.result.ImageIngestResult;

import fmservice.server.result.Status;
import fmservice.server.util.Timer;

import java.nio.ByteBuffer;
import java.io.File;

import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * WebServer interface to the FaceMatch FaceRegion related method to the Java Native interface.
 * This module invokes the "native"  Java modules which are then implemented in C++ to 
 * invoke the FaceMatch library calls.
 *
 */
public class RegionIngestAdapter extends FaceMatchAdapterBase
{
    
    private static Logger log = Logger.getLogger(RegionIngestAdapter.class.getName());
  
    protected ByteBuffer[] indexDataBuffers;
    protected  boolean bufferMode = true;           // read index data to memory buffers 
    
   protected boolean isTesting = false;
    
    protected  float  faceFindTime =  (float) -1.0;
    

    public RegionIngestAdapter()
    {
       super();
    }
    
     
    /*-------------------------------------------------------------------------------------------------------------------------*/
    /* Steps:
    * 1. Request for an FRD with OMP locks (*)
    * 2. If no face regions specified, Find faces with these  FRD (*)
    * 3. If no faces found: 
            return
    * 4. Find the  indexing type as provided in the options file
    * 5. For each Face: 
        Ingest the face to a directory  under given storage root  and imageTag/index combination
    * 6. Save face regions and index file type, name in the output HashTable 
    * 7. Return the HashTable
    *-------------------------------------------------------------------------------------------------------------------------*/
    
    public ImageIngestResult  ingestRegions( int extentId, String indexRootPath, 
        String url, String localImageFileName, String imageTag, String[] faceRegions,  
        boolean useGPU, String facefinderOption )    
    {
         Timer ingestTimer = new Timer();
         // initialize
          ImageIngestResult ingestResult  = new ImageIngestResult(extentId);
          ingestResult.imageURL = url;
          ingestResult.imageTag = imageTag;
          
          String indexType =getIndexType();
          if  (indexType == null || indexType.isEmpty())
          {
              ingestResult.setStatus(FM_INIT_ERROR,  "No index type specified in the FaceMatch option file");
              return ingestResult;
          }     
           String indexVersion = getIndexVersion();
          if (indexVersion == null)
          {
             ingestResult.setStatus(FM_INIT_ERROR,  "No index version specified in the FaceMatch option file");
              return ingestResult;
          }     
          ingestResult.indexType =  indexType;
          ingestResult.indexVersion =  indexVersion; 
          ingestResult.faceFindPerfOption = facefinderOption;
         if (!isInitialized())
         {
             ingestResult.setStatus(FM_INIT_ERROR,  "FaceMatch Library interface not initialized");
             return  ingestResult;
         }
          
         // Make sure that the path exists to store index data. If not create it. Should be (Client's storage root)/(imageextentName)
          String indexDir  = "";
         if (isTesting)         // for testing only, no database access
             indexDir = indexRootPath+"/"+indexType+"/"+indexVersion;
         // for regular operation
         else    
             indexDir = IndexStoreManager.getIndexFileDirName(extentId, indexType, indexVersion);
         
          File file = new File (indexDir);
          if (!file.exists())
          {
               boolean ok =  file.mkdirs();
               if (!ok)
               {
                    ingestResult.setStatus(INVALID_INDEXSTORE_PATH, 
                        "Cannot create directory to store index data " + indexDir);
                    Timer.release(ingestTimer);
                    return ingestResult;
               }
            }

         String statusMsg;
                 
         FRDProxy frd = getFRDWithLock(useGPU);
          if (frd == null)
         {
            statusMsg =  "Could not create FaceRegionDetector with Lock.  Cannot proceed.";
            ingestResult.setStatus(FM_INIT_ERROR,  statusMsg);
            Timer.release(ingestTimer);
            return  ingestResult;
         }
          ingestResult.gpuUsed = frd.usingGPU();
          
         long facefindFlags =FaceMatchAdapterBase.getFacefinderFlags(facefinderOption);
        
          String[] regions;
         if (faceRegions == null)
         {
              FaceDetectionInfo faceInfo = extractFaceRegionsInImage(localImageFileName, null, frd, facefinderOption);
              regions = faceInfo.detectedFaces;
              ingestResult.faceFindTime = ingestTimer.getElapsedTime();
              ingestResult.faceFindOptionUsed = faceInfo.optionUsed;
         }
         else 
         {
             ingestResult.faceFindOptionUsed = NOT_USED;
             ingestResult.faceFindTime = 0;
             // validate syntax of each faceRegion
             for (int i = 0; i < faceRegions.length; i++)
             {
                 if (!isValidRegion(faceRegions[i]))
                 {
                      statusMsg =  "Invalid input face region format or size: " + faceRegions[i];
                      ingestResult.setStatus( INVALID_FACE_REGION,  statusMsg);
                      Timer.release(ingestTimer);
                      return ingestResult;
                 }
             }
             regions = faceRegions;
         }
         
         if (regions == null)       // No faces in the image
         {
             statusMsg =  "No faces found for ingest in image with tag:" + imageTag;
             ingestResult.setStatus( NO_FACES_IN_IMAGE,  statusMsg);
             Timer.release(ingestTimer);
             return ingestResult;
         }
        /*---------------------------------------------------------------------------------------------------*/
         // now ingest each region to and store the index data for each region in a separate index file
         // and add the informatin to the overall ImageIndexResult
         //----------------------------------------------------------------------------------------------------//
         ingestResult =  ingestFaceRegions(ingestResult, indexDir, 
                 localImageFileName, imageTag, regions,   (int) facefindFlags, frd);
 
        ingestResult.totalIngestTime = ingestTimer.getElapsedTime();
       if (ingestResult.getIngestedRegionCount() > 0)
       {
             ingestResult.setStatus( SUCCESS, "Image "+imageTag + " ingested in " + 
            (ingestResult.getTotalIngestTime()) + " msec.");
       }
        Timer.release(ingestTimer);
        return ingestResult;
     }
 
  /*-------------------------------------------------------------------------------------------------------
  *   Ingest the extracted (or pre-specified) face regions in the image
  *   record individual region  ingest time and add to the overall OpsResult
  *  Note: parameter imageResult is used both as input and output
    ------------------------------------------------------------------------------------------------------*/  
     protected  ImageIngestResult  ingestFaceRegions( ImageIngestResult ingestResult, 
         String indexDir,  String imageFileName,  String imageTag,  
         String[] faceRegions,  int ffFlags, FRDProxy frd)
     {
       
            HashMap fmOptions = FaceMatchOptions;
            int imagedim = ( (Long) fmOptions.get( "DefaultWholeImgDim")).intValue();
            
            int fmFlags = (int) getDefaultFaceMatcherFlags();
            
            /*---------
            * Create  the ImageMatcher for performinig the ingest of the image
            *------*/
            String indexType = getIndexType();
            String indexFileExtension = IndexFileExtension;
            FaceRegionMatcherProxy  faceMatcher = new FaceRegionMatcherProxy(frd,
               indexType,ffFlags, imagedim, fmFlags, false );
            if (!faceMatcher.isNativeMatcherCreated())
            {
                String errorMessage = faceMatcher.getErrorMessage();
                ingestResult.setStatus(0, errorMessage);
                return ingestResult;
            }
            /*---------------------------------------------------------------------------------------
            // ingest each region, and collect performance data
            // The regionTag is imageTag:[regionIndex], which is used
            // if we want to remove that region in a remove call later.
            // Note: ingest time is computed for each  region
            /*------------------------------------------------------------------------------------------*/
            //
            Timer regTimer = new Timer();
            for (int i = 0; i < faceRegions.length; i++)
            {
                if (i  > 0)
                    System.out.println ("ingesting face " + (i+1) + " for image "  + imageTag);
                
                int regionIndex = i;
                String regionTag = REGION_TAG + regionIndex;          // (e.g. rgn0, rgn1, ...)
                // ingest each region independently */
                String indexFileName = indexDir + "/" + (imageTag + "_" + regionTag) + "." + indexType + indexFileExtension;

                // Note: region tag is used to remove a region in an image
                Status ingestStatus = faceMatcher.ingestNsaveRegion(indexFileName,
                        imageFileName, imageTag, faceRegions[i], regionTag);
                float ingestTime = regTimer.getElapsedTime();

                String faceRegion = faceRegions[i].replaceAll("^\\s+", "").replaceAll("\\s$", "");
                if (ingestStatus.isSuccess())
                {
                    ingestResult.addResult(faceRegion, regionIndex, indexFileName, ingestTime);
                } 
                else
                {
                    String errorMessage = faceMatcher.getErrorMessage();
                    log.warn("Could not ingest face:  FaceMatcher returned error: " + errorMessage);
                    ingestResult.serviceStatus = ingestStatus;
                    ingestResult.addResult(faceRegion, regionIndex, null, ingestTime);
                }
            }
            Timer.release(regTimer);
            // Also note: Falsely identified faces by user might not have been  ingested by the ImageMatcher
            if (ingestResult.getIngestedRegionCount() == 0)
            {
                ingestResult.setStatus(NO_VALID_FACES_IN_REGION, "No valid face regions found, image not ingested");                
            }
            return ingestResult;
     }

    /*------------------------------------------------------------------------------------------------*/
    // Local testing
    /*-----------------------------------------------------------------------------------------------*/ 
    public static void main(String[] args)
    {
        org.apache.log4j.BasicConfigurator.configure();
        
        boolean ingest = true; //false;
        boolean query = true;
        int NANO2MILLI = (int) Math.pow(10, 6);

        String faceMatchOptionsFile;
        String indexRoot;
        String configFile;
        String libName;

        faceMatchOptionsFile = "<TopDir>/FM2Server/installDir/fmoptions/FFParameters.json";
        indexRoot = "<TopDir>/FM2Server/localTestDir/index";
        configFile = "<TopDir>/FM2Server/installDir/config/FM2ServerLocal.cfg";

           // Get system configuration from Proprties file
        ConfigurationManager.loadConfig(configFile);
      
        // get the Properties object representing the configuration
        Properties fmConfig = ConfigurationManager.getConfig();
        GpuOpsManager gpuManager =   GpuOpsManager.createGpuManager(fmConfig);
        boolean useGPU = GpuOpsManager.getGpuManager().isGpuEnabled();
        System.out.println("--------------- Performing tests " + (useGPU ? " with " : " without " )+ "GPU --------------------------" +
           "\n---------------------------------------------------------------------------------------------\n");;
               
        if (ingest)
        {
            long start = System.nanoTime();
            RegionIngestAdapter  rgmAdapter = new RegionIngestAdapter();
            rgmAdapter.isTesting = true;

           
            rgmAdapter.init(faceMatchOptionsFile,  (String) fmConfig.get("facematch.nativeLibName"));
            String indexType = rgmAdapter.getIndexType();                   // may be "DIST"
            String indexVersion = rgmAdapter.getIndexVersion();          // may be "V1"
                        
            FaceFinderProxy.setVerboseLevel(0);
            FaceRegionMatcherProxy.setVerboseLevel(0);

            String imageDir = "<TopDir>/localTestDir/imagefiles/ncmec";
            
            String[] imageNames = {"keith_multi.jpg",  "threegirls.png", "lena.png\tf[211,202;189,189]"}; 
            String[] imageTags = {"Keith", "ThreeG", "lena-1"};
            

           for (int i = 0; i < imageNames.length; i++)
            {
               System.out.println("------------------------------------------------------------------------------------");
               String imageFile = imageNames[i];
               String imageTag = imageTags[i];

               String FFperformance = PROGRESSIVE;
               ImageIngestResult result =  rgmAdapter.ingestRegions(
                  1, indexRoot, (imageDir+"/"+imageFile), (imageDir+"/"+imageFile), imageTag, null, useGPU, FFperformance);     
              if (result.isSuccess())
                System.out.println(" TEST" + (i+1) + "PerformanceFlags " + getFacefinderFlags(FFperformance)  
                        + ", RegionIngestAdapter:  Successfully completed Ingest  for image " + imageNames[i] );
              else
                   System.out.println(" TEST" + (i+1) + " RegionMatchAdapter:  Error ingesting image " + imageNames[i] + "\n" 
                   + result.serviceStatus.statusMessage);
                  
            }
            long stop = System.nanoTime();
            long timeDiff = (stop - start);
            float millisec = (float)timeDiff/NANO2MILLI;
            System.out.println("\"**** Total time to ingest faces: " + millisec +" millisec ******");
        }
    }
}
