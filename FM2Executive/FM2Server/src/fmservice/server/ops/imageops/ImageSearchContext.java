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
import fmservice.server.cache.CacheKeyGen;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * This class stores information about the the search context for matching an image. 
 * It includes the branch name in the metadata index tree (which is traversed to get the
 * index file names) and the handle to the FaceMatch library's ImageMatcher object that 
 * reads the index files and performs the match..
 * (But does not contain the index file names)
 * It also saves the handle (pointer) to the  FaceMatchLibrary's ImageMatcher object  which 
 * performs the image matching and returns the results. Note that the  matcher is used for all 
 * queries pertaining to the corresponding search domain. (extent+metadata)
 *  
 * At  initialization time, each ImageMatcher object is loaded with the set of index files associated
 * with that metadata node. This is reflected in the numRecords variable.
 * As new images are ingested or existing ones are removed,  numRecords value changes;
 * 
 * Since data may be ingested (in bulk) by a client before ingest, previously stored data may not be loaded
 * until the first query request is received, unless FM explicitly loads them at startup time.
 *
 */
public class ImageSearchContext
{
    private static Logger log = Logger.getLogger(ImageSearchContext.class);
    
    public  int extentId;
    public  String mdBranchName;                 // unique metadata path + segment  number  in the extent
    public  int segmentNumber;        
    public String contextName;
    public  Object imageMatcher;                        // (Proxy) Object  corresponding to FM ImageMatcher
    public String indexType;                                 //  Type of index data
    public String indexVersion;                             // current version number of indexed data
    public boolean isInitialized;                           // loaded from initial data on disk 
    
   protected ArrayList<String> indexFilesToLoad;              // files to be provided to the imagematcher to load
   protected ArrayList<String>  regionsToRemove;            // loaded regions to be removed
   public float  indexUploadTime;                           // time to upload index files for each new query
   public int  searchIndexSize;                                   // total number of index files  loaded for search
    

    public ImageSearchContext(int extentId, String branchName, int segmentNum)
    {
        this.extentId = extentId;
        this.indexType = indexType;
        mdBranchName = branchName;
        segmentNumber = segmentNum;
        contextName = CacheKeyGen.buildSearchContextName(mdBranchName, segmentNumber);
        imageMatcher = null;
        isInitialized = false;
        
        indexFilesToLoad = new ArrayList();
        regionsToRemove = new ArrayList(); 
        searchIndexSize = 0;
        
        log.trace("-- Extent: "+ extentId +",  created  Search context  for   " + contextName);
    }   
    
    
    //Note: A searchContext's name is unique for a given Imageextent only, whereas
    // a searchContext Key is unique across the system of all clients and extents.
    
    public String getSearchContextName()
    { 
        return contextName;
    }
    
       
    public String getSearchContextKey()
    { 
        return contextName;
    }


    //  This must be set before retriving any index descriptor files
    public void setIndexInfo(String ndxType, String ndxVersion)
    {
        indexType = ndxType;
        indexVersion = ndxVersion;
    }
   
    /*-----------------------------------------------------------------------------------------------------*/
    // Index file related methods - invoked during ingest and queryquery
    //------------------------------------------------------------------------------------------------------*/
   public void  storeIndexFileNames(ArrayList <String>fileNames)
   {
       if (fileNames != null)
             indexFilesToLoad.addAll(fileNames) ;           // files to be provided to the imagematcher to load
   }
   
   public String[] getIndexFileNamesToLoad()
   {
       if (indexFilesToLoad.isEmpty())
           return null;
       String[] fileNames = new String[indexFilesToLoad.size()];
       return indexFilesToLoad.toArray(fileNames) ;                     //  files to be provided to the imagematcher
   }

   //*--------------------------------------------------------------------------------*/
   /** Remove a region  from indexed data for future  query.
     *Usually regions are to be removed one at a time per image
     * @param region - the region to be removed
   //*--------------------------------------------------------------------------------*/
   public void removeRegion(String region)
   {
       if (region != null && !region.isEmpty())
             regionsToRemove.add(region);              // previously loaded regions to be removed
   }
   
  //*--------------------------------------------------------------------------------*/
   /** Identify the indexed regions to be removed from 
    * the search context prior to a new query.
    * @return ID of regions to be removed
    *--------------------------------------------------------------------------------------------*/
   
    public String[] getRegionsToRemove()
   {
         if (regionsToRemove.isEmpty())
           return null;
       String[] regionIds  = new String[regionsToRemove.size()];
       regionsToRemove.toArray(regionIds) ;      
        return regionIds;                     // loaded regions to be removed
   }
   
   public void resetIndexFileNames()
   {
       indexFilesToLoad.clear();
   }
   
   public void resetRemoveRegions()
   {
       regionsToRemove.clear();
   }
   
}



