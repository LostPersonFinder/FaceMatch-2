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
import fmservice.server.ops.imageops.ImageSearchContext;
import fmservice.server.storage.index.IndexLoader;
import fmservice.server.ops.imageops.GpuOpsManager;

import fmservice.server.fminterface.proxy.FRDProxy;
import fmservice.server.fminterface.proxy.FaceRegionMatcherProxy;

import fmservice.server.result.ImageQueryResult;
import fmservice.server.util.Timer;
import fmservice.server.util.Utils;

import fmservice.server.global.ConfigurationManager;
import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.io.File;

import org.apache.log4j.Logger;

/**
 * FaceMatch2 server  interface to the FaceMatch FaceRegion related operations to
 * the FaceMatch Library.
 * This module invokes the "native"  Java modules which are then implemented in C++ to 
 * invoke the FaceMatch library calls.
 *
 */
public class RegionQueryAdapter extends FaceMatchAdapterBase
{
    private static Logger log = Logger.getLogger(RegionQueryAdapter.class.getName());
    
    // for testing only
    //private ArrayList<String> sindexFileNames ;     // static version, for initial testing
    protected ByteBuffer[] indexDataBuffers;
    protected  boolean bufferMode = false;              // don't read index data to memory buffers 
    
    boolean verboseMode = false;                            // for writing to stdout
    boolean INIT_FIRST = false;
    boolean flannMatch = false;
    long fm_matcherFlags = FaceMatchAdapterBase.getDefaultFaceMatcherFlags();
    
 
    //* Instantiate to query using the specified indextype/version as specified in the optionsMap
    // or if null: in the FMOptions file
    public RegionQueryAdapter()
    {
       super();
    }
 
  /*-------------------------------------------------------------------------------------------------------*/
    // Set whether index data is to be transferred the FaceMatch library as files or
     // DirectByteBuffers in memory  to which data is read on the Java side
     // 
     public void setBufferMode(boolean bmode)
     {
         bufferMode = bmode;
     }
     
     // Set whether index data is to be transferred the FaceMatch library as files or
     // DirectByteBuffers in memory  to which data is read on the Java side
     // 
     public void setVerboseMode(boolean vmode)
     {
         verboseMode = vmode;
     }
     
     public void setMatchType(String type)
     {
         if (type.equals("FLANN"))
         {
             flannMatch = true;
             fm_matcherFlags += FaceMatchAdapterBase.getFLANNFaceMatcherFlags();
         }
     }
     
 /*-------------------------------------------------------------------------------------------------------*/
    public ImageQueryResult   queryMatches(int extentId, ImageSearchContext[] searchContexts,
            String localQueryImage,  String[] queryRegions, Float tolerance, Integer maxMatches, 
           boolean useGPU, String facefindPref )
    {
        // initialize
        if (searchContexts.length == 0)
            return null;                                                // just to be on the safe side
        
        log.trace("Using "+ searchContexts.length + " search contextx for query");
        Timer queryTimer = new Timer();
        String indexType = searchContexts[0].indexType;
        String indexVersion = searchContexts[0].indexVersion;
        ImageQueryResult  queryResult  = new ImageQueryResult(extentId, indexType, indexVersion);
        queryResult.indexType = searchContexts[0].indexType;       // same for all SC
        queryResult.faceFindPerfOption = (facefindPref == null) ? OPTIMAL : facefindPref ;
        queryResult.localImageName = localQueryImage;
         if (!isInitialized())
         {
             queryResult.setStatus(FM_INIT_ERROR,  "FaceMatch system not initialized");
             Timer.release(queryTimer);
             return  queryResult;
         }
       
         /***
          * Get the static FaceRegionDetector object
          */
        FRDProxy frd = getFRDWithLock(useGPU);
        if (frd == null)
        {
            log.error("Could not create FaceRegionDetector. Exiting.");
            Timer.release(queryTimer);
            return null;
        }
        long ffFlags = getFacefinderFlags(facefindPref); 
        //System.out.println("-- RegionRequestAdapter: FaceMatch request with  FRD with GPU = " + frd.usingGPU());
        queryResult.gpuUsed = frd.usingGPU();
        
        // Determine the actual Query regions (with or without landmarks to be specified to the ImageMatcher)
        String[] regionsToQuery;
         if (queryRegions == null)
        {
            FaceDetectionInfo faceInfo = extractFaceRegionsInImage(localQueryImage, null, frd, facefindPref);
            //System.out.println("-- RegionRequestAdapter: extract face regions in image:  FRD with GPU = " + frd.usingGPU());
            queryResult.faceFindOptionUsed = faceInfo.optionUsed;
            regionsToQuery =  faceInfo.detectedFaces;
            queryResult.faceFindTime = queryTimer.getElapsedTime();
        }           
         else       // user specified one or more query regions - check for proper specification
         {
            regionsToQuery = getRegionsToQuery(queryResult,  queryRegions, frd, facefindPref);
         }
         
        if (regionsToQuery == null)       // No  valid faces found in the image
        {
            String  statusMsg =  "No valid face regions found in  query  image ";
            queryResult.setStatus( NO_FACES_IN_IMAGE,  statusMsg);
            queryResult.faceFindTime = queryTimer.getElapsedTime();
            Timer.release(queryTimer);
            return queryResult;
        }
       
          /**
         // Get FM default option values, if not specified by the caller
         */
          float  fmTolerance = (tolerance == null) ?
                 ((Double)FaceMatchOptions.get("Match.Tolerance")).floatValue() :  tolerance.floatValue();
          queryResult.tolerance = fmTolerance;
          
          int maxnum = (maxMatches == null) ? 
              ((Long) FaceMatchOptions.get("Match.Maxnum")).intValue() : maxMatches.intValue();    
          queryResult.maxMatches = maxnum;
          
       
       /*-----------------------------------------------------------------------------------------------------------------------------*/  
       /** Create a different image matcher for each search context only once,
        * and get matches in each search space.
        * If an ImageMatcher already exists, 
        *   - Add and delete search regions reflecting other Image operations between successive searches
        *   - Combine the results and return the accumulative data
       /*-------------------------------------------------------------------------------------------------------------------------------*/

       int nctx = searchContexts.length;                // all metadata branches
       for (int i = 0; i < nctx; i++)
       {
           ImageSearchContext searchContext = searchContexts[i];
           
           // If there are no images in a search space, do not create an imagematcher
           int nf = ( searchContext.getIndexFileNamesToLoad() == null) ?
                            0 : searchContext.getIndexFileNamesToLoad().length;
           if (nf != 0)
           {
               searchContext.searchIndexSize = nf;
              log.trace("Number of index files in SearchContext: " +  searchContext.getSearchContextName()+   " = " 
                      +  searchContext.searchIndexSize);
              //log.trace("File name [0]: " + (searchContext.getIndexFileNamesToLoad())[0]);
           }
            if ( (searchContext.imageMatcher == null) &&    nf == 0)    // not yet created 
                        continue;
             
          // make sure that each search domain has an instantiated ImageMatcher
           // note: each ImageMatcher loads its  existing index files in ParallelQuery object
           //float fileLoadStart = queryTimer.getElapsedTime();
           FaceRegionMatcherProxy imageMatcher = 
               getImageMatcher(searchContexts[i],  queryResult.indexType, useGPU, ffFlags);
           if (imageMatcher != null && INIT_FIRST)
               initForNewSearch(searchContexts[i]);
       }  
        /**-------------------
         * Now perform similarity matches in parallel using available processor cores
         * and retrieve the combined result. ParallelQueryProcessor returns after all parallel
         * processors complete their matching.
         */
        float matchStartTime  = queryTimer.getElapsedTime();

        ParallelQueryProcessor queryProcessor = new ParallelQueryProcessor(false);
        queryProcessor.setVerboseMode(verboseMode);
        ImageQueryResult  pqueryResult  =  queryProcessor.getMatchResults(searchContexts, 
            localQueryImage,  regionsToQuery,  fmTolerance, maxnum, useGPU);
        /*--------------------*/

        // copy the results and add incremental file upload data
        if (pqueryResult != null)
        {
             queryResult.regionMatchResults = pqueryResult.regionMatchResults;
             queryResult.numIndexFilesLoaded += pqueryResult.numIndexFilesLoaded;
             queryResult.indexUploadTime += pqueryResult.indexUploadTime;
        }
        float matchEndTime = queryTimer.getElapsedTime();
        queryResult.totalQueryTime = matchEndTime - matchStartTime;
        int numQRegions = queryResult.getRegionMatchResults().size();

        if (numQRegions < 0)
        {
            queryResult.setStatus(ServiceConstants.INTERNAL_SERVER_ERROR, 
                    "Error loading index data by Facematch library for query");       
        }
        if (numQRegions == 0)
        {
             queryResult.setStatus( NO_VALID_FACES_IN_REGION, "No matching results found for any query region");           
        }
        else
        {
             queryResult.setStatus( SUCCESS,  (numQRegions + " face regions in input image matched in " + 
                 (queryResult.totalQueryTime + queryResult.faceFindTime) + " msec."));
        }
         Timer.release(queryTimer);
        
         return  queryResult;      
    }   

   /*------------------------------------------------------------------------------------------------------------------------------------*/
    protected String[] getRegionsToQuery(ImageQueryResult queryResult, String[] queryRegions,
             FRDProxy frd,  String faceFinderPref)
    {

        if (queryRegions == null)
            return null;
        
         // make sure it is a valid region and find if we need to re-extract faces with landmarks
        boolean extractWithLandmarks = false;
        for (int i = 0; i < queryRegions.length; i++ )
       {
            if (!isValidRegion(queryRegions[i])) 
           {
                String  statusMsg =  "Invalid image region format or size: " +  queryRegions[i] + " specified for query ";
                queryResult.setStatus( INVALID_FACE_REGION,  statusMsg);
                return  null;
            }
            else
            {
                boolean doLandmarks = addLandmarksForQuery(queryRegions[i], faceFinderPref);
                extractWithLandmarks =extractWithLandmarks | doLandmarks;
            }
        }
        if (! extractWithLandmarks)         //regions are okay and no additional landmark  work needed
        {
            queryResult.faceFindTime = 0;
            queryResult.faceFindOptionUsed = NOT_USED;
            return queryRegions;
        }

        // extract the given faces with Landmarks
        String[] regionsToQuery;
        FaceDetectionInfo faceInfo =
                    extractFaceRegionsInImage(queryResult.localImageName, queryRegions,  frd,  faceFinderPref);
        regionsToQuery = faceInfo.detectedFaces;
        queryResult.faceFindOptionUsed = faceInfo.optionUsed;
        return regionsToQuery;
    }
    /*---------------------------------------------------------------------------------------------------------*/
    // Get the ImageMatcher (Proxy of the C++ FMLib object) for a search context.
    // Create a new one if it does not already exist, 
    // Don't load image files here, since ImageMatchers are created sequentially
    // Load them in parallel thread
    /*---------------------------------------------------------------------------------------------------------*/
    protected FaceRegionMatcherProxy  getImageMatcher(
           ImageSearchContext searchContext, String  indexType, boolean useGPU,  long ffFlags)
    {
        // instantiate the ImageMatcher object if we are loading for the first time
         FaceRegionMatcherProxy  faceMatcher = 
             (FaceRegionMatcherProxy)(searchContext.imageMatcher);
        
         if (faceMatcher != null )           // already created
             return faceMatcher;
       
        // instantiate the FaceRegionMatcher and load corresponding index data
        int status = createImageMatcher(searchContext, indexType, useGPU, (int)ffFlags, true);
        if (status == 0)
        {
            log.error("Error in creating a new FaceMatcher object.");
            return null;
        }
        faceMatcher = (FaceRegionMatcherProxy)searchContext.imageMatcher;
        return faceMatcher;
    }
    
   
    /*----------------------------------------------------------------------------------------------------------*/
    protected int createImageMatcher(ImageSearchContext searchContext, 
        String indexType, boolean useGPU, int ffFlags, boolean queryMode)
     {
             // invoke ImageMatcher to match face
            int imagedim = ( (Long) FaceMatchOptions.get( "DefaultWholeImgDim")).intValue();
            int matchFlags = (int) fm_matcherFlags;
            //System.out.println ("Using index match flags: " + matchFlags);
            
            FRDProxy frd = getFRDWithLock(useGPU);
            FaceRegionMatcherProxy faceMatcher = new FaceRegionMatcherProxy(frd, indexType, 
                (int)ffFlags, imagedim, matchFlags, queryMode );
           System.out.println("-- RegionRequestAdapter: createImageMatcher :  FRD with GPU = " + frd.usingGPU());
            if (faceMatcher == null)
            {
                log.error("Could not create ImageMatcher  for querying with for index type " + indexType);
                return 0;
            }
            searchContext.imageMatcher = faceMatcher;
            searchContext.isInitialized = false;
            searchContext.indexType = indexType;
            return 1;
    }
    
     /*--------------------------------------------------------------------------------------------*/
       protected void initForNewSearch(ImageSearchContext searchContext)
       {
           FaceRegionMatcherProxy  imageMatcher = (FaceRegionMatcherProxy) searchContext.imageMatcher;
           if (imageMatcher == null)
               return;
           // Load all  new index files
            String[] indexFileNames = searchContext.getIndexFileNamesToLoad();
            if (indexFileNames == null || indexFileNames.length == 0)
            {
                    log.trace("No new index descriptor files to be loaded for query");
            }
            else
            {
                Timer  loadTimer = new Timer();
                 int nf =  imageMatcher.loadIndexFiles(indexFileNames);         // number of records loaded
                // log.debug("-- ImageMatcher: Loaded index data from " + nf + " Image descriptors");

                 searchContext.resetIndexFileNames();            // not to load again for next search
                 float loadTime = loadTimer.getElapsedTime();
                 searchContext.indexUploadTime = loadTime;
                 Timer.release(loadTimer);

                  if ( nf > 500)          // find the available memory etc.
                        log.info (">>> Heap Memory usage after loading "+ nf + " index files for search   " + Utils.heapMemUsage() );   
            }

            // remove all deleted face regions index files from search
            int nr = ( searchContext.getRegionsToRemove() == null) ?
                             0 : searchContext.getRegionsToRemove().length;
            if (nr != 0)
            {
                imageMatcher.removeRegionsFromSearch(searchContext.getRegionsToRemove());
                searchContext.resetRemoveRegions();
                 log.trace("Removed region[0]: " + (searchContext.getRegionsToRemove())[0]);
            }
       }

/*---------------------------------------------------------------------------------------------------*/
 //  SearchContext contains the   ImageMatcher object instant which would
 // be used to perform the imageMatching. If it is null, it is created and initialized with 
 // the index files specified in the search context (that is from the search domain.)
 // Note that index descriptors are added (after ingest) and removed (by explicit remove()
 // call from the client during operation.
 // Load the index Files into memory and add to  the ImageMatcher scope/
 //----------------------------------------------------------------------------------------------------//
    protected  ByteBuffer[] loadSearchDomainData(ImageSearchContext  searchContext,  String[] indexFileNames)
    {
        ByteBuffer[] indexBuffers = null;
        FaceRegionMatcherProxy imageMatcher;

        int n = indexFileNames.length;
        ArrayList indexBufferList  = new ArrayList(); //ByteBuffer[n];
        for (int i = 0; i <   n; i++)
        {
            ByteBuffer byteBuffer = IndexLoader.loadDataToDirectByteBuffer(indexFileNames[i]);
            if (byteBuffer !=null)  
            {
                indexBufferList.add(byteBuffer);
                log.trace("Loaded index data from " + indexFileNames[i]+ " to DirectByte buffer");
            }
        }  // end for
        indexBuffers = new ByteBuffer[indexBufferList.size()];
        indexBufferList.toArray(indexBuffers);
        return indexBuffers; 
    } // end ifLoaded
    
    /*--------------------------------------------------------------------------------------------------------*/
    // Insert new index files to the FaceMatcher. 
    // Instantiate the face matcher if not yet running
    /*---------------------------------------------------------------------------------------------------------*/
    public int insertIndexData(ImageSearchContext  searchContext, String indexType, 
        String[] indexFileNames, boolean useGPU, long ffFlags)
    {
        //Add one or more index files from the set id loaded set
        FaceRegionMatcherProxy  faceMatcher = (FaceRegionMatcherProxy) searchContext.imageMatcher;
        if (faceMatcher == null)
        {
                log.trace("No ImageMatcher exists for this search domain. Creating a new one");
            // instantiate the ImageMatcher object if we are loading for the first time
           int status =  createImageMatcher(searchContext,  indexType,  useGPU, (int)ffFlags, false);
           if (status == 0)
               return 0;
        }
 
         return insertIndexData(searchContext,   indexFileNames );
    }
    
   /*--------------------------------------------------------------------------------------------------------*/
    // insert new index files to an already instantiated  FaceMatcher
    /*---------------------------------------------------------------------------------------------------------*/
    public  int insertIndexData(ImageSearchContext  searchContext,  String[] indexFileNames )
    {
         FaceRegionMatcherProxy faceMatcher =  (FaceRegionMatcherProxy)searchContext.imageMatcher;
         if (faceMatcher == null)
         {
             log.error("No imageMatcher object exisis for this search context. Please generate one first.");
             return -1;
         }
        int nr = 0;         //searchContext.numRecords;
        if (bufferMode)
        {
           ByteBuffer[] indexData =   loadSearchDomainData(searchContext,  indexFileNames);
           nr +=  faceMatcher.loadIndexData(indexData);         // number of records loaded
        }
        else
        {
          nr +=  faceMatcher.loadIndexFiles(indexFileNames);         // number of records loaded
            log.trace(">> Inserted index data from " + nr + " Image descriptors");
        }
        return nr;           // number of descriptors currently loaded
    }

    /** -------------------------------------------------------------------------------------------------
     * Remove faces from index descriptor set for comparison
     * A regionId consists of the imageTag:regionIndex
     * Note: This method is called for local tests only (without accessing FM2 database etc.)
    /*---------------------------------------------------------------------------------------------------------*/
    public  int removeFromSearchLocal(ImageSearchContext  searchContext, String[] regionIds)
    {
        //remove one or more index files from the set of loaded set
        FaceRegionMatcherProxy  imageMatcher = (FaceRegionMatcherProxy) searchContext.imageMatcher;
        if (imageMatcher == null)
        {
                log.trace("No imageMatcher exists for this search domain");
            return 0;
        }
        int n = imageMatcher.removeRegionsFromSearch(regionIds);
        searchContext.searchIndexSize = searchContext.searchIndexSize-n;
        return n;           // number of descriptors removed
    }


    /*------------------------------------------------------------------------------------------------*/
    // Local testing
    // Should be invoked after running RegionIngestAdapter,.
    // Image names and tags must match
    /*-----------------------------------------------------------------------------------------------*/ 
    public static void main(String[] args)
    {
        org.apache.log4j.BasicConfigurator.configure();         // for log4j messages
        boolean query = true;
       //  int NANO2MILLI = (int) Math.pow(10, 6);
        
        String faceMatchOptionsFile;
        String indexRoot;
        String configFile;
        String libName;  

        faceMatchOptionsFile = "<TopDir>/FM2Server/installDir/fmoptions/FFParameters.json";
        indexRoot = "<TopDir>/FM2Server/localTestDir/index";
        configFile ="<TopDir>/FM2Server/installDir/config/FM2ServerLocal.cfg";


        // Get system configuration from Proprties file
        ConfigurationManager.loadConfig(configFile);
      
        
        // get the Properties object representing the configuration
        java.util.Properties fmConfig = ConfigurationManager.getConfig();
        GpuOpsManager gpuManager =   GpuOpsManager.createGpuManager(fmConfig);
        boolean useGPU = GpuOpsManager.getGpuManager().isGpuEnabled();
        System.out.println("--------------- Performing tests " + (useGPU ? " with " : " without " )+ "GPU --------------------------" +
            "\n---------------------------------------------------------------------------------------------\n");;

         /** Test ImageMatching
          * 
          * @param fileNames 
          */
        if (query)
        {
             System.out.println("\n-----------------------------  Start Query -------------------------------------");
            
             // set the directory where to create the master index file
             System.setProperty("java.io.tmpDir", fmConfig.getProperty("temp.masterindex.dir"));
            Timer timer = new Timer();
            RegionQueryAdapter  rgmAdapter1 = new RegionQueryAdapter();
            rgmAdapter1.init(faceMatchOptionsFile,  (String) fmConfig.get("facematch.nativeLibName"));  
            ArrayList<String> indexFileNames = new ArrayList();
            
            String indexType = rgmAdapter1.getIndexType();
            String indexVersion = rgmAdapter1.getIndexVersion();
            String indexFileDir = indexRoot+"/"+indexType+"/"+indexVersion;
            
            
            File file = new File(indexFileDir);
            String[] fileList = file.list();
            
            // get only the binary files
            for (int i = 0; i < fileList.length; i++)
            {
                if (fileList[i].endsWith(indexType+".ndx"))
                      indexFileNames.add( indexFileDir+"/"+fileList[i]);
            }
            System.out.println(">>> Total number of indexed files in directory: "  + indexFileNames.size());

         //  indexFileNames.add(indexRoot+"/SIFT/DM2_rgn1.ndx");
            rgmAdapter1.setBufferMode(false);
            String queryImage = "<TopDir>/FM2Server/localTestDir/imagefiles/ncmec/lena.png";
                    //<TopDir>/imagefiles/ncmec/Shekhar.jpg";

           // give it a slightly different size than the face
           String[]  queryRegions = new String[] { "f[200, 50;150, 150]" };
            System.out.println("  Query Image: "  + queryImage + ": Region: " + queryRegions[0]);
            
          // create two search contexts with diferent metadata from these index files
           int nf = indexFileNames.size();
           int extentId = 0;       // since a standalone test

           ImageSearchContext searchContext1 = 
               new ImageSearchContext(extentId, "male_adult", 0); 

          ArrayList<String> indexFileList1 = new ArrayList();
           for (int i = 0; i < nf; i++)
               indexFileList1.add(indexFileNames.get(i));
           
           searchContext1.storeIndexFileNames(indexFileList1);
           searchContext1.indexType = indexType;
           searchContext1.indexVersion = indexVersion;
           searchContext1.imageMatcher = null;           // to be created at query time
           
                 
           ImageSearchContext[] searchContexts = new  ImageSearchContext[] {searchContext1}; //, searchContext2};
           ImageQueryResult queryResult = rgmAdapter1.queryMatches(extentId, searchContexts, queryImage,  
                   queryRegions, new Float(0.99), null,  useGPU, PROGRESSIVE);
           printQueryResult(queryResult);
           
           float timeDiff = timer.getElapsedTime();
           System.out.println(queryResult.convertToJSONString());
           System.out.println("**** Total time to match with set of " + nf + " faces: " + timeDiff +" millisec*****");
           System.out.println("--------------------------------------------------------------------------------------");
            
       /*    // now query again
            timer.resetStartTime();
            queryImage = "<TopDir>/FM2Server/localTestDir/imagefiles/ncmec/threegirls.png";
            //C:/DevWork/FaceMatch2/testdata_web/imagefiles/ncmec/ThreeGirls.png";
                    
            queryRegions = new String[] {"f[120,63;44,44]", "f[200,85;45,45]"};
            System.out.println("  2nd Query Image: "  + queryImage + ": Region: " + queryRegions[0]);
            queryResult = rgmAdapter1.queryMatches(extentId, searchContexts,  queryImage,  queryRegions, 
                    new Float(0.99), null,  useGPU, ACCURACY);
            printQueryResult(queryResult);
          
           float timeDiff1 = timer.getElapsedTime();
           System.out.println("**** Total time to match "+ queryResult.regionMatchResults.size()+ " faces: " + timeDiff1 +" millisec*****");
           System.out.println("------------------------------------------------------------  ");
     */      
         // test remove 
           timer.resetStartTime();
           String[] regions2remove = new String[] {"ThreeG\tf[200,86;44,44]"}; // "ThreeG\tf[119,63;45,45]"};
           rgmAdapter1.removeFromSearchLocal(searchContext1, regions2remove); 
           //System.out.println("** Removed all faces from image with tag \"ThreeG\"");
           System.out.println("Number of descriptors to match after removal ="  + searchContext1.searchIndexSize +"\n");
        
           // now query again 
            queryImage = "<TopDir>/FM2Server/localTestDir/imagefiles/ncmec/lena.png";
            queryResult = rgmAdapter1.queryMatches(extentId, searchContexts,  queryImage, null, new Float(0.88), null,  useGPU, ACCURACY);
            printQueryResult(queryResult);
            System.out.println("------------------------------------------------------------  ");
           
           String[] regions2remove1 = new String[] {"ThreeG"}; //lena-1"};
           rgmAdapter1.removeFromSearchLocal(searchContext1, regions2remove1); 
           System.out.println("Number of descriptors to match after removal = "  + searchContext1.searchIndexSize +"\n");
           
           /* queryResult = rgmAdapter1.queryMatches(extentId, searchContexts, queryImage, null, 
                   new Float(0.99), null,  useGPU, ACCURACY);
            printQueryResult(queryResult);
           float timeDiff2 = timer.getElapsedTime();
            System.out.println("**** Total time to match after removing two  faces: " + timeDiff1 +" millisec*****");
           */
           System.out.println("--------------------------------------------------------------------------------------");
           Timer.release(timer);
          } 
    }
    
    
/*---------------------------------------------------------------------------------*/
// Print match info from returned in queryResult
//----------------------------------------------------------------------------------(/
protected static void printQueryResult( ImageQueryResult queryResult)
{
        // convert to String for sending to client, do not print image/zone IDs etc.
       String str = "";
       str += "FaceFindTime: " +   queryResult.faceFindTime;     
       str += "\nIndexUploadTime: " +   queryResult.indexUploadTime;
    
        // fill in Face-specific  match information
        int numRegions =  queryResult.regionMatchResults.size();
         str += "\nNumRegions: " +  numRegions;
       
         if (numRegions > 0)
        {
            
             // create an Array of distance vs. face of each entry of this region
            for (int i = 0; i < numRegions; i++)
            {
                 ImageQueryResult.RegionMatchResult regMatchResult  = queryResult.regionMatchResults.get(i);
                 str += "\nRegion: " +  regMatchResult.queryRegion;
                 str += "\n NumMatches: " +  regMatchResult.numMatches;

                int n = regMatchResult.matchResult.entrySet().size();
               java.util.Iterator <Float>  it =  regMatchResult.matchResult.keySet().iterator(); 
                while (it.hasNext())
                {
                    Float distance = it.next();
                    ArrayList<String[]> data = regMatchResult.matchResult.get(distance);
                    for (int j = 0; j < data.size(); j++)
                         str += "\n     imageTag: " + (data.get(j))[0] +", Region: " + (data.get(j))[1] + ", Distance: " + distance;
                }
                str += "\n---------------------------------";
            }
            System.out.println(str);
        }
      }

     /* --------------------------------------------------------------------------------------------------------------------------------------------*/
/** Example of a query result returned by FMLib for an image with two regions.
*
C:/DevWork/FaceMatch2/testDir/temp/PL/1-2015-atacama-floods.personfinder.google.orgSLASHperson.4804389472567296__1865716431.png	f[24,32;60,60] found 6
0	test-img1	f[24,32;60,60]
0.328467	keith-test4	f[109,125;135,135]
0.35927	keith-test4	f[207,66;131,131]
0.576346	test-img1	f[82,50;52,52]
0.598451	keith-test4	f[378,113;87,87]
0.91899	keith-test4	f[4,155;34,34]
C:/DevWork/FaceMatch2/testDir/temp/PL/1-2015-atacama-floods.personfinder.google.orgSLASHperson.4804389472567296__1865716431.png	f[82,50;52,52] found 6
0	test-img1	f[82,50;52,52]
0.316098	keith-test4	f[109,125;135,135]
0.347439	keith-test4	f[207,66;131,131]
0.350103	keith-test4	f[378,113;87,87]
0.574326	test-img1	f[24,32;60,60]
0.90648	keith-test4	f[4,155;34,34]
* 
**---------------------------------------------------------------------------------------------------------------------------*/
     
}
