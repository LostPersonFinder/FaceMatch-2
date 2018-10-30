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

import fmservice.server.fminterface.proxy.FaceRegionMatcherProxy;

import fmservice.server.ops.imageops.ImageSearchContext;
import fmservice.server.result.ImageQueryResult;

import fmservice.server.util.Timer;
import fmservice.server.util.Utils;
import java.io.File;

import java.util.ArrayList;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;


/**
 * This class requests query matches to be performed using parallel threads on multiCore servers
 * Note that the timing information is only approximate because the number of parallel threads 
 * are only same as the number of processors, which may be less than the total number of 
 * metadata search paths.
 * 
 *
 */
public class ParallelQueryProcessor
{
    
    private static Logger log = Logger.getLogger(ParallelQueryProcessor.class.getName());
    
     boolean bufferMode;             // data to be passed through memory buffers vs. as files
     
     boolean verboseMode = true;
     boolean buildMasterIndex = false;          // do not use MasterIndexFile setup
    
      public ParallelQueryProcessor (boolean bufferMode)
      {
          this.bufferMode = bufferMode;
      }
      
      public void setVerboseMode(boolean vmode)
      {
          verboseMode = vmode;
      }
    
    
      /*-----------------------------------------------------------------------------------------------------------------------------*/  
       /** Create a different image matcher for each search context and get matches for each search space.
        * Use total number of threads same as the number of cores (CPUs) in the system
        * 
        * @param searchContexts
        * @param queryImageName
        * @param regions
        * @param tolerance
        * @param maxMatches
        * @param useGPU
        * @return 
        */
       // Combine the results and return the accumulative data
       /*-------------------------------------------------------------------------------------------------------------------------------*/
    
    public ImageQueryResult getMatchResults(ImageSearchContext[] searchContexts,  String queryImageName, 
            String[] regions,  float  tolerance, int maxMatches,  boolean useGPU)
    {
        int nctx = searchContexts.length;
        if (nctx == 0)
            return null;

        int NUM_PROCESSORS =   Runtime.getRuntime().availableProcessors();
       
        Timer qtimer = new Timer();
        int npUsed = 0;                // number of processors used for the query
        int numIndexes = 0;
        ExecutorService executor = Executors.newFixedThreadPool(NUM_PROCESSORS);
        ArrayList<Future<ImageQueryResult>> list = new ArrayList<Future<ImageQueryResult>>();
        for (int i = 0; i < nctx; i++)
        {
            if ((searchContexts[i].imageMatcher == null) || (searchContexts[i].searchIndexSize == 0) )           // nothing to match here
                continue;
            npUsed++;
            numIndexes += searchContexts[i].searchIndexSize;
            
            Callable<ImageQueryResult> worker = new FMQueryHandler(searchContexts[i],
                    queryImageName,  regions,  tolerance,  maxMatches,   useGPU);
            Future<ImageQueryResult> task = executor.submit(worker);
            list.add(task);
        }
       
       if (list.size() == 0)            // no ingested images in any search context
       {
           log.info("No images found to match against the given query image");
           return null;
       }
       else
       {
           log.trace("Matching " + numIndexes + " faces using " + npUsed + " parallel  channels,  with " + NUM_PROCESSORS + " available processors");
       }

       
        // wait till all the submitted tasks finish execution, 
        // and combine the result of FM similaritymatching

        ArrayList<ImageQueryResult> allMatches = new ArrayList();
        for (Future<ImageQueryResult> future : list)
        {
              try 
              {
                  ImageQueryResult result = future.get();
                  if (result != null)
                        allMatches.add( result);     //   future.get  invokes worker.call()
             } 
              catch (InterruptedException e)
              {
                 log.error("Error in parallel query execution:", e);
              }
              catch (ExecutionException e) 
              {
                  log.error("Error in parallel query execution:", e);
             }
               catch (ArrayIndexOutOfBoundsException ae) 
              {
                  log.error("Error in parallel query execution:", ae);
             }
        }
         executor.shutdown();
         float matchTime = qtimer.getElapsedTime();
        log.trace("Parallel execution time for all " + searchContexts.length + ": " + matchTime + " msec");

        Timer.release(qtimer);
         
          ImageQueryResult outputResult =           // default if no matches
              new  ImageQueryResult( searchContexts[0].extentId, searchContexts[0].indexType, searchContexts[0].indexVersion);
          if (!allMatches.isEmpty())
          {
              outputResult =  mergeMatches(allMatches);
          }
          outputResult.totalQueryTime = matchTime;
          return outputResult;
         
    }
    
    
    /*---------------------------------------------------------------------------------------------------------------*/
    /**  Merge the match results obtained for each query face in all search domains 
    *   and return the results.
    *  Note: Also get the total number of files loaded and the total time taken
     */
    protected    ImageQueryResult mergeMatches( ArrayList<ImageQueryResult> allMatchingSets)
    {    
        ImageQueryResult outputResult = allMatchingSets.get(0);
        ArrayList<ImageQueryResult.RegionMatchResult> parsedRegions =  outputResult.getRegionMatchResults();
       
        // merge results of subsequent results to the first one iteratively
        for (int i = 1; i < allMatchingSets.size(); i++)
        {
            ImageQueryResult currentResult = allMatchingSets.get(i);
            // for each region 
            for (int j = 0; j < parsedRegions.size(); j++)
            {
                ImageQueryResult.RegionMatchResult parsedRegion= parsedRegions.get(j);
                ImageQueryResult.RegionMatchResult currentRegion = currentResult.findRegionMatchResult(parsedRegion);
                if (currentRegion != null)              // atch exixtx for this region
                    outputResult.mergeRegionMatches(currentRegion);
            }
            outputResult.numIndexFilesLoaded += currentResult.numIndexFilesLoaded;
            outputResult.indexUploadTime = Math.max(outputResult.indexUploadTime, currentResult.indexUploadTime);
        }
        return outputResult;
    }
      
    /*---------------------------------------------------------------------------------------------------------------*
    *  Inner class to perform the matching on a single search context
    *----------------------------------------------------------------------------------------------------------------*/
    protected class FMQueryHandler implements Callable<ImageQueryResult >
    {
        ImageQueryResult queryResult;
        
        ImageSearchContext searchContext;
        String queryImageName;
         String[] regions;
         float  tolerance;
         int maxMatches;
         boolean useGPU;
         
          ImageQueryResult matchResult;
        
        protected FMQueryHandler(ImageSearchContext searchContext,  String queryImageName, 
         String[] regions,  float  tolerance, int maxMatches,  boolean useGPU)
        {
            this.searchContext = searchContext;
            this.queryImageName = queryImageName;
            this.regions = regions;
            this.tolerance = tolerance;
            this.maxMatches = maxMatches;
            this.useGPU = useGPU;
        }

        public ImageQueryResult call()
        {
            try
            {
                int numIndexes = searchContext.searchIndexSize;
                log.trace("\n<---- Query started for searchContext  " + searchContext.getSearchContextName()+", index count = "+ numIndexes);
                
                // Load additional index files that were created since last query       
                Timer  queryTimer = new Timer();            
                
                FaceRegionMatcherProxy imageMatcher = (FaceRegionMatcherProxy ) searchContext.imageMatcher;
                if (imageMatcher != null)
                    initForNewSearch( searchContext) ;   
             
                matchResult = getMatches(imageMatcher,  queryImageName,  regions, tolerance);
                float  queryTime = queryTimer.getElapsedTime();
                log.trace("<---- Query ended for  searchContext: "+ searchContext  + " , time taken: " + 
                        queryTime + " msec --->\n" );
               Timer.release(queryTimer);
                if  (matchResult == null)
                    log.warn ("No matches with tolerance " + String.valueOf(tolerance) +" found by query matcher;");
               else
                {
                    matchResult.indexUploadTime = searchContext.indexUploadTime;
                    matchResult.numIndexFilesLoaded = numIndexes;  
                    matchResult.totalQueryTime = queryTime;
                } 
                return matchResult;
            } 
            catch (Exception e)
            {
                log.error("Exception in Call() to future object", e);
                return null;
            }
        }
        
       /*--------------------------------------------------------------------------------------------*/
       protected void initForNewSearch(ImageSearchContext searchContext)
       {
           searchContext.indexUploadTime = 0;
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

                 searchContext.resetIndexFileNames();            // not to load again for next search
                 float loadTime = loadTimer.getElapsedTime();
                 searchContext.indexUploadTime = loadTime;
                 Timer.release(loadTimer);
                 // log.debug("-- ImageMatcher: Loaded index data from " + nf + " Image descriptors in " + loadTime + " msec");

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
            }
            log.trace ("Total number of indexes to search : "+ (searchContext.searchIndexSize - nr));
       }
 /*---------------------------------------------------------------------------------------------------------------------*/   
    // Not used 
    //-------------------------------------------------------------------------------------------------------------------*/
       protected  String createMasterIndexFileToLoad( String[] indexFileNames)
        {
            int n = indexFileNames.length; 
            if (n == 0)      // nothing to load
                return null;
            // File names are based upon imageTags and regions
            File tempDir = new File (System.getProperty("java.io.tmpDir"));
            String prefix = Utils.getFileBaseName(indexFileNames[0]);
            prefix =  Utils.getFileBaseName(prefix);            // replace the .DIST if any
            
            String postfix = Utils.getFileBaseName(indexFileNames[n-1]);
            postfix =Utils.getFileBaseName(postfix);   
            String mfName =prefix+"TO"+postfix+".mdx";
            
            String listFileName = Utils.createListFile( indexFileNames, tempDir, mfName);
            if (listFileName == null)
                return null;
           
            log.info("Created temp master index file " + listFileName);
            return listFileName; 
        }
        /*---------------------------------------------------------------------------------------------------------------------*/   
    
    //--------- load master index file ------------------
    
        // Creating and loading a master index files -- NOT used ---
       // If there are any more ingests prior to this query, load these files to ImageMatcher
                // Note: file names must be reset by the ImageMatcher after loading
                //
     /*           String[] indexFileNames = searchContext.getIndexFileNamesToLoad();

                String masterIndexFile = null;
                if (indexFileNames != null && indexFileNames.length > 0)
                {
                    if ( !buildMasterIndex)
                    {
                        imageMatcher.loadIndexFiles(indexFileNames);
                    }
                    else
                    {
                        // create a master index file with the name of all index files to load
                        // This is a more efficient way of loading the index descriptors by the FaceMatcher
                        // which also builds vertical data structurs after  allindex data is loaded
                        masterIndexFile = createMasterIndexFileToLoad(indexFileNames);
                        if (masterIndexFile == null)
                        {
                            log.error("Could not build master index file.");
                            return null;
                        }
                        imageMatcher.loadMasterIndexFile(masterIndexFile);
                    }
                    indexLoadTime = queryTimer.getElapsedTime();
                    numIndexFiles = indexFileNames.length;
                    searchContext.resetIndexFileNames();            // clear after loading
                    log.info ("Time to load " + numIndexFiles + " additional index files for search: "+ 
                                    indexLoadTime + " msec" );
                   
                     // If a master index file was created, we want to delete it
                     if (buildMasterIndex)
                     {
                            deleteMasterIndexFile(masterIndexFile);
                     }
                } // end if
                
                // Also,  if any regions were removed, inform the matcher
                String[] removeRegionIds = searchContext.getRegionsToRemove();
                if (removeRegionIds != null) 
                {
                    Timer  removeTimer = new Timer();
                     imageMatcher.removeRegionsFromSearch(removeRegionIds);
                     searchContext.resetRemoveRegions();
                    log.trace("<---- Time to remove " + removeRegionIds.length + " regions from search: "+ 
                                    removeTimer.getElapsedTime() + " msec --->\n" );
                     Timer.release(removeTimer);
                }
*/
                
       
       
        /*-------------------------------------------------------------------------------------------------------*
        * Query the given ImageMatch object for ranked matches for the faces
        *  Add its returned result to the results from other matchers.
        * and return the sorted list  in ascending order of  Similarity distance 
        * (Note: Building of the QueryResult object from FM returned String is
        * performed by the utility Class QueryResultParser.
        *-----------------------------------------------------------------------------------------------------------------*/

       protected ImageQueryResult  getMatches (
                FaceRegionMatcherProxy faceMatcher,  String queryImageName, 
                String[] regions, float tolerance)   
        {
            // Get  the FaceMatcher result returned as a String
            String matchResult = faceMatcher.queryMatches(queryImageName, regions, tolerance);
           //System.out.println("QueryResult: \n " + matchResult);
            ImageQueryResult parsedResult = QueryResultParser.parseString(matchResult, queryImageName, regions);
            if (parsedResult != null && parsedResult.getNumQueryRegions() > 0)
            {
                return parsedResult;
            }
            return null;
        }
    }
}

 
