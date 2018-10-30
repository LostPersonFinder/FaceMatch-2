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

import fmservice.server.ops.FMDataManager;
import fmservice.server.fminterface.adapter.FaceFinderAdapter;

import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.FormatUtils;
import  fmservice.httputils.common.ServiceConstants;

import fmservice.server.util.Timer;

import fmservice.server.result.FaceFindResult;
import fmservice.server.result.FaceRegion;

import java.util.Properties;
import java.util.HashMap;
import java.io.File;


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
public class FaceFindReqHandler implements ServiceConstants
{
    private static Logger log = Logger.getLogger(FaceFindReqHandler.class.getName());
    
    protected FMDataManager fmDataManager;
    protected int statusCode;
    protected String errorMsg;
    int  requestID;                                 // facematching request ID (counter) since start up
    
  /* 
    // for reference only
   
   
    static String[] outputParamNames = { FACE_REGIONS, DISPLAY_REGIONS,
        URL_FETCH_MILLI , FM_PROCESS_MILLI, GPU_USED, STATUS_CODE, STATUS_MESSAGE};
   */ 
    
    public FaceFindReqHandler(FMDataManager dataManager)
    {
       fmDataManager = dataManager;
    }   
 
    public FaceFindResult  handleRequest(int reqId, int operation, 
        HashMap <String, Object>inputParams) throws Exception
    {
         if (operation != GET_FACES_OP)
             return null;
        requestID = reqId;
        
      
        // perform FaceFinding task
       FaceFindResult opsResult =  getFaces(requestID, inputParams);
       return opsResult;
     }
     /*----------------------------------------------------------------------------------------------------*/
    protected FaceFindResult  getFaces(int requestId, HashMap <String, Object>parameters)
    {
        FaceFindResult ffResult = new FaceFindResult();

        // initialize
        statusCode = BAD_IMAGE_URL;         
        errorMsg = "Invalid URL specified in Request";

        String imageFile = (String) parameters.get(URL);
        if (!FaceFinderAdapter.isValidImageType(imageFile))
        {
            errorMsg =  "Invalid URL, image type not supported by FaceMatch";
            ffResult.setStatus(BAD_IMAGE_URL, errorMsg);
             return ffResult;
        }
        boolean isURL= FormatUtils.isValidURL(imageFile);

       Properties fmConfig = fmDataManager.getFMConfig();
       // keep track of performance
       


       /*----------------------------------------------------------------------------
       // read the imageFile and keep track of performance
       //use a subdiretory as defined in the config file
       /*----------------------------------------------------------------------------*/
       Timer downloadTimer = new Timer();

       String localSubdir = fmConfig.getProperty("facefind.image.subdir");
       String localImageFileName = ImageLoader.loadClientImage(fmConfig,
           String.valueOf(requestID), (String)parameters.get(CLIENT_KEY), localSubdir, 
           imageFile, isURL);
       

        if (localImageFileName == null)
        {
            String msg =  "Could not retrieve/access image file " + imageFile;
             ffResult.setStatus(BAD_IMAGE_URL, errorMsg);
             return ffResult;
        }

         // record the time
        ffResult.urlFetchTime = downloadTimer.getElapsedTime();
        Timer.release(downloadTimer);
         
        File localFile = new File(localImageFileName);
        String path = localFile.getParent()+"/";                // FM expects path terminating with "/";
        String fname = localFile.getName();

         // tell FaceMatch whether or not to use the GPU - false if USE_GPU is false;
         boolean requestGPU = fmDataManager.isGpuAvailable();
         
         /**-----------------------------------------------------------------------------------------------
          * Retrieve FaceMatch related parameters from the input  map for the 
          * FaceFinder proxy module to talk to the FaceMatch Lib.
          /**-----------------------------------------------------------------------------------------------*/
         
        // Get the percentage by which the face has to be inflated (if too small)
        float inflateBy = (float)0.0;       // default: no inflation
        if ( parameters.containsKey(INFLATE_BY))
        {
            String inflateByStr  = (String)parameters.get(INFLATE_BY);
            inflateBy = Float.valueOf(inflateByStr);
        }
        
        boolean showLandmarks = false;
        if ( parameters.containsKey(LANDMARKS))
               showLandmarks = ((String)parameters.get(LANDMARKS)).equalsIgnoreCase("true");
      
        /**------------------------------------------------------------------
        // Send request to FaceFinder in FMLib and receive the results.
        // Use  the face detection option flags, in oder of precedence, as:
        // user -> server config -> server default
        //------------------------------------------------------------------*/
        
        String validPerformanceOptions = SPEED+"||"+ACCURACY+"||"+OPTIMAL+"||"+PROGRESSIVE;
        String faceFinderPerf = null;
        if ( parameters.containsKey(PERFORMANCE_PREF))
            faceFinderPerf = (String)parameters.get(PERFORMANCE_PREF);
        else 
           faceFinderPerf = fmConfig.getProperty("facefind.pref.default");
        
        if (faceFinderPerf == null || 
                    !(faceFinderPerf.toLowerCase().matches(validPerformanceOptions)))
             faceFinderPerf = OPTIMAL;
        
         // check if  a culled region within the image is   already specified by the caller
        String[] inputRegions = null;
        String inputRegionSpec  = (String)parameters.get(REGION);
        if (inputRegionSpec != null)
            inputRegions =FaceRegion.getRegionsInRequest(inputRegionSpec);
        
        
        FaceFinderAdapter ffAdapter = new FaceFinderAdapter();
        HashMap<String, Object> resultParams = ffAdapter.getFaces(path, fname, 
           inputRegions,  showLandmarks, inflateBy, requestGPU, faceFinderPerf);
      
        if (isURL )         // an URL copied locally for temporary use, must be deleted
        {
             localFile.delete();
        }
        
        
        // fill the output result info
        ffResult.imageURL = imageFile;
        ffResult.setStatus ((Integer)resultParams.get(STATUS_CODE), (String)resultParams.get(STATUS_MESSAGE));
        ffResult.faceFindTime = ((Float)resultParams.get(FACEFIND_TIME)).floatValue();  
        ffResult.gpuUsed = ((Boolean)resultParams.get(GPU_USED)).booleanValue();
        ffResult.performanceOption=faceFinderPerf;
        ffResult.perfOptionUsed = (String)resultParams.get(PERF_OPTION_USED);
        if (ffResult.isSuccess())
        {
            ffResult.faceRegions = (String[])resultParams.get(FACE_REGIONS);
            ffResult.displayRegions = (String[])resultParams.get(DISPLAY_REGIONS);
            ffResult.numFaces = ffResult.faceRegions.length;
            ffResult.skinColorMapperKind= (String)ffAdapter.skinColorMapKind();
        }
        return ffResult;
     }    

}

