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

import fmservice.server.fminterface.proxy.JNILoader;
import fmservice.server.fminterface.proxy.InfoProxy;
import fmservice.server.fminterface.proxy.FRDProxy;
//import fmservice.server.util.RunTimeCmd;

import fmservice.httputils.common.ServiceConstants;
import fmservice.server.fminterface.proxy.FaceFinderProxy;
import fmservice.server.result.FaceRegion;
import java.awt.Rectangle;

import java.io.File;

import java.util.HashMap;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

/**
 * This class is the superclass of all adapters for interfacing with the FaceMatch library through the "proxy" classes.
 * It loads the FaceMatch native SO/DLL and reads the options files to get  constants and optimal parameters for
 * calling the FaceMatch Library methods (for example:  model file path, face finder flags, etc.)
 * <p>
 * All FMLib objects returned by this module are the corresponding "Proxy" java objects that interface with
 * the actual C++ objects using the JNI modules.
 * <p>
 * It also provides the basic functions such as building a static FRD.
 * <p>
 * Note: this module uses both "log" and "System.out" calls as the methods may be used locally for standalone test
 * without logging to a file and rely upon standard output to the terminal
 * 
 *
 * 
 * ** Change Log:
 * *    GPU is always turned off to make the system run in a Virtual Environment without GPU and CUDA.
 * *    If GPU is later available, set gpuAvailable = true
 * 
 */
public class FaceMatchAdapterBase implements ServiceConstants
{
   private static Logger log = Logger.getLogger(FaceMatchAdapterBase.class.getName());
   
   // file types not processed by FMLib and OpenCV
   public static String[] excludeImageTypes = {".gif"};
   public static boolean jniLoaded = false;

   // Map of needed FaceMatchLib option parameters and their values
    public static HashMap <String, Object> FaceMatchOptions = null;
    
    public static FRDProxy  GPU_FRD = null;           // detect faces using GPU
    public static FRDProxy  CPU_FRD = null;           // detect faces using CPU
    
    public static FRDProxy  GPU_LOCK_FRD = null;           // detect faces using GPU and Lock
    public static FRDProxy  CPU_LOCK_FRD = null;           // detect faces using CPU and Lock

    protected static int initStatus = 0;
    protected static boolean useGpu = false;           // current default
    protected static boolean gpuAvailable = false;
    
    protected boolean     fmDebug = false;             //true;
    
    protected static String fmLibVersion = "";          // default
    protected static String configFMLibVersion = "Unknown";          // default

    
    // various flag settings for face finding  performance preference
    // Actual values are set after (static) init function is called 
    public static long SpeedFFflags = 0;
    public static long OptimalFFflags = 0;                  // supports 90 degree rotation
    public static long AccuracyFFflags = 0;                // support full (30 degree)  rotation
    public static long LandmarkFlags = 0;
    public static long MinLandmarkDim= 0;             // Min dimension beyond which ladmarks needed for region  match
    public static String SkinColorMapKind = "";                  // 
    
    public static String optionUsed = "";                   // performance option used to detect faces
    
    public static String IndexFileExtension;
    public int nFRDs = 0;
    
    public FaceMatchAdapterBase()
    {
      ;            // TBD: currently do nothing
    }
    
    /*-------------------------------------------------------------------------------------*/
    /**
    * Check if FMLib process this image type. 
    * Presently "gif" images are not supported at OpenCV level
    * 
    * @param fileName  name of image file including its extension
    * @return true or false 
    * --------------------------------------------------------------------------------------*/
    
    public static boolean isValidImageType(String fileName)
    {
        for (int i = 0; i < excludeImageTypes.length; i++)
        {
            if (fileName.toLowerCase().endsWith(excludeImageTypes[i]))
                return false;
        }
        return true;
        
    }
    
/*--------------------------------------------------------------------------------------------------------------------------------------*/
 /** This method is invoked at system startup time to load the  interface with the
  * FaceMatch C++ interface (.SO/DLL) and to read the FaceMatch parameters options
  * for various functions, as recommended by the FM library developers.
  * 
  * @param faceMatchOptionsFile  Configuration file with FaceMatch options for indexing/querying etc.
  * @param jniLibName Name of the Java Native Interface (JNI) library to invoke the C++ Facematch library
  * 
  * @return status
  **--------------------------------------------------------------------------------------------------------------------------------------*/
    public static  int init(String  faceMatchOptionsFile, String fmLibName)
     {
        if (initStatus == 1)
        {
            log.warn ("FaceMatch interface already initialized");
            return SUCCESS;
        }
        //-------------------------------------------------------------
       // load the  JNI Shared library
       //-------------------------------------------------------------
       if (!jniLoaded)
        {
            jniLoaded = JNILoader.isLoaded(fmLibName);
            if (!jniLoaded)
            {
                log.fatal("FaceMatch interface library was not loaded. Please put it in the correct path and restart.");
                initStatus = FMLIB_LOAD_ERROR;
                return initStatus;
            }
        }
        // FMLibVersion used only for returning tto the caller
         fmLibVersion = getFMLibVersion();
       
        //-----------------------------------------------------------------------------------------------
        //  Read FaceMatch specific default parameters 
        //-----------------------------------------------------------------------------------------------
        if ( FaceMatchOptions  == null)
         {
            FaceMatchOptionsReader.readFFOptions( faceMatchOptionsFile);
            
            FaceMatchOptions  = FaceMatchOptionsReader.getFFOptions();
            if (FaceMatchOptions == null)
            {
                log.error("Error in reading FaceMatch option parameters from " + faceMatchOptionsFile );
                initStatus = FMLIB_OPTION_ERROR;
                return initStatus;
            }
         }

          String sflagStr= (String)FaceMatchOptions.get("FF_speedFlags");
          SpeedFFflags = getFaceFinderFlagSetting(sflagStr);
          
          String aflagStr =  (String)FaceMatchOptions.get("FF_accuracyFlags");
          AccuracyFFflags = getFaceFinderFlagSetting(aflagStr);
          
          String oflagStr =  (String)FaceMatchOptions.get("FF_optimalFlags");
          OptimalFFflags = getFaceFinderFlagSetting(oflagStr);
          
          String lflagStr = (String)FaceMatchOptions.get("FF_landmarkFlags");
          LandmarkFlags = getFaceFinderFlagSetting(lflagStr);
         
                  
          IndexFileExtension = (String)FaceMatchOptions.get("IndexFileExtension");
          if (IndexFileExtension == null)
             IndexFileExtension = ".ndx"; 
          
         MinLandmarkDim  = ((Long)FaceMatchOptions.get("MinLandmarkDim" )).intValue();

          log.info("FaceMatch interface successfully initialized.");
          initStatus  = 1;
          return initStatus;
    }
    
    //-------------------------------------------------------------------------------------------------------- 
    // Return the Version number of the FaceMatch Library we are connected to.
    // Should be called only after JNI is loaded.
    //---------------------------------------------------------------------------------------------------------
    public static String getFMLibVersion()
    {
        return InfoProxy.getFMLibVersion();
        
    }
    
     /*-------------------------------------------------------------------------------------------------------------------------*/
    // Check if a specified region is a valid face region  (f|p)[x,y;w,h] where width and height are 
     // acceptable to FaceMatch Lib.
   /*-------------------------------------------------------------------------------------------------------------------------*/
     
     public boolean isValidRegion(String regionString)
     {
         FaceRegion faceRegion = FaceRegion.parseFMString( regionString);
         if (faceRegion == null)
             return false;
         int width = faceRegion.regionCoord.width;
         int height = faceRegion.regionCoord.height;
         
         int  minRegionDim  = ((Long)FaceMatchOptions.get("CRegDimMin" )).intValue();
         if (width < minRegionDim || height < minRegionDim)
             return false;
         
         return true;
     }
     
   /*-------------------------------------------------------------------------------------------------------------------------*/
    // Check if landmarks should be extracted (through FMLib) for this query region
     // Required if we are querying with a large face, for which no landmarks are given
     
   /*-------------------------------------------------------------------------------------------------------------------------*/
     
     public boolean addLandmarksForQuery(String regionString, String  ffPref)
     {
         //if (ffPref .equalsIgnoreCase(SPEED))      // no landmarks needed for a SPEED setting
         //    return false;
         
         FaceRegion faceRegion = FaceRegion.parseFMString( regionString);
         if (faceRegion == null)        // not a valid region
             return false;
         
         int width = faceRegion.regionCoord.width;
         int height = faceRegion.regionCoord.height;
         if (width < MinLandmarkDim || height < MinLandmarkDim)
             return false;
        
        // Need landmark specification, Check  if landmarks are already  given
         if (faceRegion.numLandmarks > 0)
             return false;
         
         return true;
     }
 
   /*-------------------------------------------------------------------------------------------------*/
   /** 
    * Verify if connection for FaceMatch operations are already in place
    * 
    * @return  boolean true or false
    */
   public  boolean  isInitialized()
   {
       return (initStatus == 1);
   }
   
     /*-------------------------------------------------------------------------------------------------*/
    /** Set the GPU usage as on or off
     * 
     * @param gpuOn  true if GPU is to be used
     */
    public  static void  setGpuStatus(boolean gpuOn)
   {
       if (gpuAvailable)
       {
        useGpu = gpuOn;
        log.info("*** Perorming tests " + (useGpu ? " with " : " without " ) + " GPU ***");
       }
       else
       {
          useGpu = false;
          log.info("*** GPU not available. Performing tests with CPU ***"); 
       }
   }
   
/*--------------------------------------------------------------------------------------------------*/
 /** Get indexing type to be used for ingest and face matching operations.
  * <p>
  * Note:  Index type and version are used to create the paths to the index file storage directory
  * 
  * @return  index Type
  */
   public  String getIndexType()
   {
       if (!isInitialized())
           return null;
       
       return ((String) FaceMatchOptions.get("IndexType"));
   }
   
 /*--------------------------------------------------------------------------------------------------*/
 /** Get the index version number to be used for ingest/query operations.
  * 
  * @return  index version 
  */
   public  String getIndexVersion()
   {
       if (!isInitialized())
           return null;
       
       return ((String) FaceMatchOptions.get("IndexVersion"));
   }
   
   public  String skinColorMapKind()
   {
       if (!isInitialized())
           return null;
       
       return ( (String)FaceMatchOptions.get("SkinColorMapperKind" ));
   }
   

   /*---------------------------------------------------------------------------------------------------------*/
    /** Get the static FaceRegionDetector object with Lock, and with/without GPU.
    * @param useGPU - if true, use the GPU, else use CPU
    * @return specific FRD
    /*---------------------------------------------------------------------------------------------------------*/
    public  FRDProxy getFRDWithLock (boolean useGPU)
    {
        boolean withLock = true;
        if (gpuAvailable && useGPU)
        {
            if (GPU_LOCK_FRD == null)
            {
                System.out.println(" -- FaceMatchAdapterBase: Creating new FRD with Lock and GPU");
                GPU_LOCK_FRD = createFRD(true, withLock);
            }
            return GPU_LOCK_FRD;
        }
        else
        {
             if (CPU_LOCK_FRD == null)
             {
                System.out.println(" -- FaceMatchAdapterBase: Creating new FRD with Lock and CPU");
                CPU_LOCK_FRD = createFRD(false, withLock);
             }
            return CPU_LOCK_FRD;
        }  
    }
    
    /*---------------------------------------------------------------------------------------------------------*/
    /** Get (static) FaceRegionDetector object without any Lock, and with/without GPU.
    *  @param useGPU - if true, use the GPU, else use CPU
    * @return specific GPU
    * 
    * --- not used --
    /*---------------------------------------------------------------------------------------------------------*
    public  FRDProxy getDefaultFRD (boolean useGPU)
    {
        if (useGPU)
        {
            if (GPU_FRD == null)
            {
                System.out.println(" -- FaceMatchAdapterBase: Creating new FRD with GPU");
                GPU_FRD = createFRD(true, false);
            }
            return GPU_FRD;
        }
        else
        {
             if (CPU_FRD == null)
             {
                 System.out.println(" - -FaceMatchAdapterBase: Creating new FRD with CPU");
                CPU_FRD = createFRD(false, false);
             }
            return CPU_FRD;
        }  
    }
    */
    
    /*-------------------------------------------------------------------------------------------*/
    /**  Check  if FaceMatch library internally used the GPU.
    *  Note: it might not be used for some reason, even if used requested it expliciltly)
    * @return true if FMLib is using it
    *-----------------------------------------------------------------------------------------------*/
    public boolean isGPUUsedbyFM(FRDProxy frd)
    {
        if (!gpuAvailable)
            return false;
        
        if (useGpu != frd.usingGPU() )
        {
            log.warn(">> GPU Use mismatch - FM2Server: useGPU = " + (useGpu? "TRUE" : "FALSE") + ", FM2Lib Usage: " +  frd.usingGPU());
        }
        return frd.usingGPU();

    }
/*---------------------------------------------------------------------------------------------------------*/
 /** Create a FaceMatchRegionDetector object as specified.
  * 
  * @param useGPU
  * @param withLock
  * @return FRDProxy object for face detection
  */
    protected  FRDProxy createFRD (boolean useGPU, boolean withLock)
    {
        if (!gpuAvailable && useGPU)
        {
             log.warn("GPU not available for this system; Creating FRD with CPU.");
             useGPU=false;
        }
        FRDProxy.setVerboseLevel(2);
        nFRDs++;
        log.info("Creating new FRD: useGPU = " + useGPU + " withLock: " + withLock +", total FRDs: " + nFRDs);
        try
        {
            HashMap ffOptions = FaceMatchOptions;
            // get the required parametrs and flags
            String XMLModelPath = (String) ffOptions.get("XMLModelPath" );
            
            // make sure that the file path exists
            File path  = new File (XMLModelPath);
            if (!path.exists())
            {
                log.error("FaceMatch model path " + path + " does not exist. Cannot create FRD");
                return null;
            }
            String FaceModelFN  = (String) ffOptions.get("FaceModelFN" );
            String ProfileModelFN = (String) ffOptions.get("ProfileModelFN");

            // get other parameters
            String SkinColorMapperKind =  (String) ffOptions.get( "SkinColorMapperKind" );
            String SkinColorParmFN =  (String) ffOptions.get( "SkinColorParmFN" );
            
            int  FaceDiameterMin = ((Long)ffOptions.get("FaceDiameterMin" )).intValue();
            int  FaceDiameterMax = ((Long)ffOptions.get("FaceDiameterMax" )).intValue();

           double  SkinMassT = ( (Double)ffOptions.get( "SkinMassT" )).doubleValue();
           double  SkinLikelihoodT = ((Double)ffOptions.get("SkinLikelihoodT" )).doubleValue();
           double  FaceAspectLimit  = ((Double)ffOptions.get("FaceAspectLimit" )).doubleValue();

          
          FRDProxy frd = new FRDProxy(
                XMLModelPath, FaceModelFN, ProfileModelFN, 
                SkinColorMapperKind, SkinColorParmFN, FaceDiameterMin, FaceDiameterMax,
                SkinMassT, SkinLikelihoodT, FaceAspectLimit, useGPU, withLock); 
            return frd;
     }
        catch(Exception e)
        {
            log.error("Error creating FRD with default FaceMatch parameters., cannot proceed with FaceFinding operations.", e);
            return null;
        }
    }
    /*---------------------------------------------------------------------------------------------------------------*/
    // Get the Face detection  flags corresponding to various options
    /*----------------------------------------------------------------------------------------------------*/    
       public static int getFacefinderFlags(String performanceOption)
      {
          if (performanceOption.equalsIgnoreCase(ACCURACY))
              return (int)AccuracyFFflags;
          else if (performanceOption.equalsIgnoreCase(OPTIMAL))
              return (int)OptimalFFflags;
          else if (performanceOption.equalsIgnoreCase(SPEED))
              return (int)SpeedFFflags;
          else
              return 0;
      }
   /*---------------------------------------------------------------------------------------------------------------*/
    // Get the default FaceFinder flags
    /*----------------------------------------------------------------------------------------------------*/
    protected static long getDefaultFaceFinderFlags()
    {
         HashMap ffOptions = FaceMatchOptions;
         String  defFlags = (String)ffOptions.get("FF_defaultFlags");
         long ffFlags = getFaceFinderFlagSetting( defFlags);
         return ffFlags;
    }
    
    /*----------------------------------------------------------------------------------------------------*/
    // Get the  FaceFinder flags corresponding to their string representation
    /*----------------------------------------------------------------------------------------------------*/
    protected static long getFaceFinderFlagSetting(String  flags)
    {
        if (flags == null || flags.length() == 0)
            return 0;
        
         HashMap ffOptions = FaceMatchOptions;
         JSONObject  flagBits = (JSONObject)ffOptions.get("FF_Flag_bits" );     // definition of all facematch flag values
         
        String[] optionFlags = flags.split("\\|");
         long   ffFlags = 0;
         for (int i = 0; i < optionFlags.length; i++)
         {
             String bitName = optionFlags[i].replaceAll("^\\s+", "").replaceAll("\\s+$", "");
             int bitPosition = ((Long)flagBits.get(bitName)).intValue();
             //System.out.println("Bit name: "  + bitName +", value : " + bitPosition);
              ffFlags += (int)Math.pow(2, bitPosition);
         }
         //    System.out.println("ffFlags = " + ffFlags);
         return ffFlags;
    }
        
    /*----------------------------------------------------------------------------------------------------*/
    // Get the default FaceFinder flags to be used
    /*----------------------------------------------------------------------------------------------------*/
    protected static long getFaceFinderRotationFlags()
    {
         HashMap ffOptions = FaceMatchOptions;
         String  rotationFlagStr = (String)ffOptions.get("FF_rotationFlags");
         return getFaceFinderFlagSetting(rotationFlagStr);
    }
    
     /*---------------------------------------------------------------------------------------------------------------*/
    // Get the default FaceFinder flags to be used
    /*----------------------------------------------------------------------------------------------------*/
    protected static long getFaceFinderWithLandmarkFlags()
    {
         HashMap ffOptions = FaceMatchOptions;
        String  landmarkFlagStr = (String)ffOptions.get("FF_landmarkFlags");
        return  getFaceFinderFlagSetting(landmarkFlagStr);
    }
    
    
    /*----------------------------------------------------------------------------------------------------*/
      protected static long getDefaultFaceMatcherFlags()
      {
            HashMap ffOptions = FaceMatchOptions;
            return ( (Long)ffOptions.get("FM_defaultFlags")).longValue();        // currently 0
             
             // TBD: parse as for FaceFinder flags if non-zero
      }
      
      
      /*---------------------------------------------------------------------------*/
      // Return the value of FLAN image type as set in the options file
      //
       protected static long getFLANNFaceMatcherFlags()
      {
           HashMap ffOptions = FaceMatchOptions; 
           JSONObject  fmIndexOptions = (JSONObject)ffOptions.get("FM_ImgDscNdxOptions" );   
           
           //int bitPosition = ((Long)indexOptions.get(flannType)).intValue();     
           int bitPosition = ((Long)fmIndexOptions.get("dmFlann")).intValue();   
           long flannFlag = (long)Math.pow(2, bitPosition);

          System.out.println("flanFlag = " + flannFlag);
          return (flannFlag);
      }

      //------------------------------------------------------------------------------------------------------//
      // Service method
      protected void setStatus(HashMap <String, Object> paramMap,  int status, String msg)
      {
            paramMap.put(STATUS_CODE,  new Integer(status));
            paramMap.put(STATUS_MESSAGE,  msg);
      }
   
   /*----------------------------------------------------------------------------------------------------------------------*/
      protected FaceDetectionInfo extractFaceRegionsInImage(String imageFileName, 
              String[] inputRegions, FRDProxy frd,  String option)
      {
          if (option == null )
              option =OPTIMAL;
      
          FaceDetectionInfo fdInfo;
          
          
          if (option.equalsIgnoreCase(PROGRESSIVE))
              fdInfo = extractFaceRegionsInImageProgressively( imageFileName,  inputRegions, frd );
          else
          {
              long faceDetectionFlags = this.getFacefinderFlags(option);
              String[] regions = extractFaceRegions( imageFileName,  inputRegions, frd, faceDetectionFlags);
              fdInfo = new  FaceDetectionInfo(option, regions);
          }
          return fdInfo;
      }
      
      
    /*-------------------------------------------------------------------------------------------------------------------------*/
    // Extract faces in an image invoking the FaceMatch Library using the given FaceRegionDetector 
   //and fmlib flags corresponding to the user specified options
   // null means no faces found
   //   
   // If a set of two elements with first being an error code from FMLIB Proxy, the second one is the error message
   // Note: If regions are specifed, only submit the ones with dimension > 64
   /*-------------------------------------------------------------------------------------------------------------------------*/
     protected String[]  extractFaceRegions(String localImageFileName, String[] inputRegions,
             FRDProxy frd, long faceFindFlags)
     {         
        File localFile = new File(localImageFileName);
        String imageDir = localFile.getParent()+"/";                // FM expects path terminating with "/";
        String imageFilename = localFile.getName();
        log.trace("--- Using FaceFinder flags: " + faceFindFlags );


        String faceMarkers = null;
         FaceFinderProxy  ffObj; 
        if (inputRegions == null)
        {
            ffObj = new FaceFinderProxy(frd,  imageDir, imageFilename,  (int)  faceFindFlags);
            if (ffObj.getFaceFinderHandle() == 0)
            {
               String errMsg = "Could not create Native (JNI) FaceFinder object for image file: " +
                       localImageFileName;
               log.error (errMsg );
               String[] err = new String[]{String.valueOf(FMLIB_OPERATION_ERROR), errMsg};
               return err;         // this is a cludge, since we cannot return a proper error message here
           } 
           if (ffObj.gotFaces(true))           // check faces with lax = true for better results
           {
                faceMarkers =  ffObj.getFaces();
                return FaceRegion.getRegionStrings(faceMarkers); 
           }
           else
                   return null;
        }    
        //-------------------------------------------------------------------------------------------------------//
        // Comes here if we are detecting faces within a given region - as needed for query 
        // Note: We cannot submit large and small regions together for FF with landmarks - as FM 
         // returns only the large one
        // So do it only for large ones;  don't submit for smaller ones at all.
        String[] outputRegions = new String[inputRegions.length];
        for (int i = 0; i < inputRegions.length; i++)
        {
            Rectangle r = FaceRegion.getCoordinates(inputRegions[i]);
            if (r.width < MinLandmarkDim || r.height < MinLandmarkDim)
                outputRegions[i] = inputRegions[i];             // leave it alone
            
            else        // get face with landmarks from FMLib
            {
                ffObj = new FaceFinderProxy(frd,  imageDir, imageFilename,  inputRegions[i], (int)  faceFindFlags);
                if (ffObj.getFaceFinderHandle() == 0)
                {
                   String errMsg = "Could not create Native (JNI) FaceFinder object for image file: " +
                           localImageFileName;
                   log.error (errMsg );
                   String[] err = new String[]{String.valueOf(FMLIB_OPERATION_ERROR), errMsg};
                   return err;         // this is a cludge, since we cannot return a proper error message here
                } 
                if (ffObj.gotFaces(true))           // check faces with lax = true for better results
                {
                    faceMarkers =  ffObj.getFaces();
                    if (faceMarkers == null)
                    {
                        log.warn ("No faces found in image region  " +  inputRegions[i] + "  in input image");
                       return null;
                    }
                    log.trace(" Faces in image : " + localImageFileName +  faceMarkers);
                    String[] regions = FaceRegion.getRegionStrings(faceMarkers);      // should be only one
                    outputRegions[i]    = regions[0];
                }
                else
                   outputRegions[i]  = inputRegions[i];          // just leave it as is
            } // end face with landmarks
        }
         return outputRegions; 
     }
     
     /*-------------------------------------------------------------------------------------------------------------------------*/
    // Extract faces in an image invoking the FaceMatch Library using the given FaceRegionDetector 
     // Currently advanced not used: since optionFlag should include advanceFlags at caller's choice
     // Landmarks are included in higher options
   /*-------------------------------------------------------------------------------------------------------------------------*/
     protected FaceDetectionInfo extractFaceRegionsInImageProgressively(
             String localImageFileName,  String[] inputRegions, FRDProxy frd )
     {         
     
        // Try to extract faces:
        // first with speed flags, if fails, proceed with optimal flags, if fails: proceed with Accuracy flags
         
         String[] options = {SPEED, OPTIMAL, ACCURACY};
         long[] ffFlags =  {SpeedFFflags, OptimalFFflags, AccuracyFFflags};
         
         String[] regions  = null;        
         for (int i = 0; i < ffFlags.length; i++)
         {
              regions = extractFaceRegions( localImageFileName,  inputRegions,  frd,  ffFlags[i]);
              if (regions != null && regions.length > 0)
                   return  new FaceDetectionInfo (options[i], regions);
         }
         return new FaceDetectionInfo( ACCURACY, null);         // nothing detected even with accurary flags
     }     
     
     /*-------------------------------------------------------------------------------------------------------*/
     protected String getInputImageSpec(String imageFileName, String[] inputRegions)
     {
         String imageFileSpec = imageFileName;
        if (inputRegions != null)
        {
            for (int i = 0; i < inputRegions.length; i++)
                imageFileSpec += "\t" + inputRegions[i];
        }
        return imageFileSpec;
     }
     
     //----------------------------------------------------------------------------------------------------------------
     public class  FaceDetectionInfo
     {
         protected String optionUsed;
         protected String[] detectedFaces;
         protected FaceDetectionInfo (String option, String[] faces)
         {
             optionUsed = option;
             detectedFaces = faces;
         }
     }
  }
       
