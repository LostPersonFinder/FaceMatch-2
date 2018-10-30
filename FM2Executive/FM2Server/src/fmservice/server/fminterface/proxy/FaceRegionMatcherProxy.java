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
package fmservice.server.fminterface.proxy;


import fmservice.httputils.common.ServiceConstants;
import fmservice.server.result.Status;
import fmservice.server.result.FaceRegion;

import fmservice.server.util.Timer;
import java.nio.ByteBuffer;

import java.io.File;


import org.apache.log4j.Logger;
/**
 *
 *
 */
public class FaceRegionMatcherProxy
{  
    
    private static Logger log = Logger.getLogger(FaceRegionMatcherProxy.class);
    
     long nativeImageMatcher;                    // handle to the native (C++) FaceMatcher object
     String errorMessage;                           // any error message from native modue
     
     long frdHandle;                         // FaceRegionDetector to use (with/without GPU)
     int ffFlags;                                  // flags for face matching
     int  fmFlags;                              // flags for facematching
     int  imageDim;
     String indexType;
     
     // for performing a query
     boolean queryMode;
     boolean indexDataLoaded;
     int NANO2MILLI = (int) Math.pow(10, 6);
     
       
      /** 
     * instantiate from scratch with all parameters.
     * 
     * */
     public FaceRegionMatcherProxy(FRDProxy frd, String indexType,
           int faceFinderFlags,   int imageDim, int facematchFlags, boolean matchMode)
     {
        this.frdHandle = frd.getFRDHandle();
        this.ffFlags = faceFinderFlags;
        this.fmFlags = facematchFlags;
        this.indexType = indexType;
        this.imageDim = imageDim;
        this.queryMode  = matchMode;
        
        // should be set to true after index data loaded for imagematching
        indexDataLoaded = false;
        
        // Trace JNI activities
        if(log.isDebugEnabled())
            n_setVerboseLevel(2);
        else
            n_setVerboseLevel(1);
        
        nativeImageMatcher = n_createImageMatcher(frdHandle, indexType, ffFlags, imageDim, fmFlags);
        
         if (nativeImageMatcher == 0)
         { 
             errorMessage =  n_getErrorMessage(nativeImageMatcher);
             log.error("Error in creating FaceMatch ImageMatcher, message: " + errorMessage);
         }
     }
     
     public boolean isNativeMatcherCreated()
     {
         return (nativeImageMatcher > 0);
     }
     
    public String getErrorMessage()
     {
         return errorMessage;
     }
     
     static public void setVerboseLevel(int vlevel)
    {
        n_setVerboseLevel(vlevel);
    }
     
     /*---------------------------------------------------------------------------------------------------------------------------*/
     // Ingest a single FaceRegion in an image, regionNum is a sequential number 
     // within the image. These numbers are unique since the same image (with same or different regions)
     // is never reingested without being removed first.
     /*----------------------------------------------------------------------------------------------------------------------------*/
     public  Status ingestNsaveRegion(String indexFileName, 
         String imageFileFullName, String imageTag, String faceRegion, String regionTag)
     {
         Status retStatus;          // return stsatus info to caller
         
         // replace all trailing and leading white spaces
         imageFileFullName = imageFileFullName.replaceAll("^\\s+", "").replaceAll("\\s+$", "");

         n_clearIndex(nativeImageMatcher);
         String faceAttr = faceRegion;      
         String fp = faceRegion.substring(0, 1);
         faceAttr = faceAttr.replaceAll("f|p", "").replaceAll("\\{", "").replaceAll("\\}", "");
         // add the full tag with both image and region tags
         faceAttr = faceAttr +"\td["+imageTag+":"+regionTag+"]";
         
         // putback together in proper format for ImageMatcher
         String regionID = fp+"{"+faceAttr+"}";
         int n =  n_ingestRegion(nativeImageMatcher,  indexFileName, imageFileFullName, imageTag, regionID);
         if (n == 0)
         {
             retStatus = new Status(ServiceConstants.FMLIB_OPERATION_ERROR,
                 "Error in ingesting face in image with Tag: " + imageTag + " by the Native FM lib");
             return retStatus;
         }
        return new Status(ServiceConstants.SUCCESS,  "");     
     }
     //---------------------------------------------------------------------------------------------------------//
     // Local calls from higher level Java modules
      public long getFaceMatcherHandle()
     {
         return nativeImageMatcher;
     }
      
      public String getIndexType()
      {
          return indexType;
      }
      
      public boolean isIndexDataLoaded()
      {
          return indexDataLoaded;
      }
      
      public void clearLoadedIndexData()
      {
           n_clearIndex(nativeImageMatcher);
      }
      
    /*----------------------------------------------------------------------------------------------------------*/     
      public int loadIndexData(ByteBuffer[] indexData)
      {
          if (!queryMode)
          {
              //log.error("Not performing queries. No data loaded.");
              log.warn("Not performing queries. No data loaded.");
              return -1;
          }
         int numLoaded =  n_loadIndexData(nativeImageMatcher, indexData);
          indexDataLoaded =  (numLoaded > 0);
         return numLoaded;          // number of data buffers loaded, -ve means error in loading files
      }
      
     /*----------------------------------------------------------------------------------------------------------*/     
      public int loadIndexFiles(String[] indexFileNames)
      {
          long start = System.nanoTime();
          if (!queryMode)
          {
              //log.error("Not performing queries. No data loaded.");
              log.warn("Not performing queries. No data loaded.");
              return -1;
          }
         int numLoaded =  n_loadIndexFiles(nativeImageMatcher, indexFileNames);
          indexDataLoaded =  (numLoaded > 0);
          long stop = System.nanoTime();
          long timeDiff = (stop - start);
          float millisec = (float)timeDiff/NANO2MILLI;
         // log.info("\"*-- NativeImageMatcher: " +nativeImageMatcher + ", Average time to open/close/ load " + numLoaded +
         //           " index files: " + (millisec/numLoaded) +" millisec--");
          return numLoaded;          // number of fileNames loaded
      }
      
       
     /*----------------------------------------------------------------------------------------------------------*/ 
      /* Load an index file which is in master index file format
      /*----------------------------------------------------------------------------------------------------------*/
      
      public int loadMasterIndexFile(String mindexFileName)
      {
          long start = System.nanoTime();
          if (!queryMode)
          {
              //log.error("Not performing queries. No data loaded.");
              log.warn("Not performing queries. No data loaded.");
              return -1;
          }
         int numLoaded =  n_loadMasterIndexFile(nativeImageMatcher, mindexFileName);
          indexDataLoaded =  (numLoaded > 0);
          long stop = System.nanoTime();
          long timeDiff = (stop - start);
          float millisec = (float)timeDiff/NANO2MILLI;
          log.info("\"*-- NativeImageMatcher: " +nativeImageMatcher + ", Average time to open/close/ load " + numLoaded +
                    " index files: " + (millisec/numLoaded) +" millisec--");
          return numLoaded;          // numbder of fileNames loaded
      }
      
       /*----------------------------------------------------------------------------------------------------------*/  
      //Save the indexed data loaded into a FaceMatcher.
      // Note: This is not used by FM2 for ingest, but for other test for debugging etc
      /*----------------------------------------------------------------------------------------------------------*/
      public int saveIndexData(String saveFileName)
      {
          // make sure the directory exist, if not create it
          File file = new File(saveFileName);
          File dir = file.getParentFile();
          try
          {
            if (!dir.exists())
                dir.mkdirs();
          }
          catch (Exception e)
          {
              e.printStackTrace();
              return 0;
          }
          int n = n_saveIndexData( nativeImageMatcher, saveFileName); 
            return n;             // size of index data
      }
 
 
      /*----------------------------------------------------------------------------------------------------------*/  
      // Remove the specifiied region entries from the search set
      /*----------------------------------------------------------------------------------------------------------*/
      public int removeRegionsFromSearch(String[] regionTags)
      {
        if (regionTags.length == 0)
            return 0;
         int numRemoved =  n_removeRegions(nativeImageMatcher,  regionTags);

          return numRemoved;          // number of regions removed
      }
 
      /*---------------------------------------------------------------------------------------------------*/
      // Perform query for similatiry matching of faces in the given image
      // and return results as  a ranked list of high to low similarity
      //  Each returned String represents match results of one region
      //---------------------------------------------------------------------------------------------------*/
     public String queryMatches(String queryImageName, String[] queryRegions, float tolerance )
     {
         if (!indexDataLoaded)
         {
             //log.error("No index data loaded to perform queries");
            log.info("No index data loaded to perform queries");
            return null;
         }
          long start = System.nanoTime();
          String  rankedMatches =  n_queryMatches( nativeImageMatcher,  queryImageName,  queryRegions, tolerance);
         
          long stop = System.nanoTime();
          long timeDiff = (stop - start);
          float millisec = (float)timeDiff/NANO2MILLI;
           log.trace("\"*-- Time to query by Facematch Lib against "  + queryRegions.length + " regions: " + millisec +" millisec--");
         return rankedMatches;
     }
    /*-------------------------------------------------------------------------------------------------------------------------*/
    // Native method implementation
    // using static methods as we are not using any callbacks or references to Java objects 
    /*-------------------------------------------------------------------------------------------------------------------------*/
    static  native  long   n_createImageMatcher( long frdHandle, 
         String indexType, int ffFlags, int imageDim, int fmFlags);

 
 /*-------------------------------------------------------------------------------------------------------------------------*/
 static native int  n_ingestRegion(long faceMatcherHandle, String indexFileName,
    String localImageFileName, String imageTag, String regionID);
 
  /*-------------------------------------------------------------------------------------------------------------------------*/
  static native int  n_clearIndex(long faceMatcherHandle);
  /*-------------------------------------------------------------------------------------------------------------------------*/
  
  /*--------------------------------------------------------------------------------------------------------------------------*/
  static native int n_loadIndexData(long faceMatcherHandle, ByteBuffer[] indexData);
  /*--------------------------------------------------------------------------------------------------------------------------*/
  
 /*--------------------------------------------------------------------------------------------------------------------------*/
  static native int n_loadIndexFiles(long faceMatcherHandle, String[]  indexFileNames);
  /*--------------------------------------------------------------------------------------------------------------------------*/
  
  /*--------------------------------------------------------------------------------------------------------------------------*/
  static native int n_loadMasterIndexFile(long faceMatcherHandle, String mindexFileName);
  /*--------------------------------------------------------------------------------------------------------------------------*/

 /*--------------------------------------------------------------------------------------------------------------------------*/
  static native int n_saveIndexData(long faceMatcherHandle, String indexFilename);
  /*--------------------------------------------------------------------------------------------------------------------------*/
  
  
  /*--------------------------------------------------------------------------------------------------------------------------*/
  static native int n_removeRegions(long faceMatcherHandle, String[]  regionTags);
  /*--------------------------------------------------------------------------------------------------------------------------*/
  
   /*--------------------------------------------------------------------------------------------------------------------------*/
  static native String n_queryMatches(long faceMatcherHandle,
      String queryImageName, String[] queryRegions, float tolerance);
   /*--------------------------------------------------------------------------------------------------------------------------*/  
   // retrieve the error message saved by the JNI class
    static   native String n_getErrorMessage(long nativeFFObj);
  /*--------------------------------------------------------------------------------------------------------------------------*/
      // set the verbose level for the JNI class for testing/debugging
    static   native  void n_setVerboseLevel (int vlevel);
    /*--------------------------------------------------------------------------------------------------------------------------*/
}   