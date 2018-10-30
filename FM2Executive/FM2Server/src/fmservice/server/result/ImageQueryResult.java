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
package fmservice.server.result;

import fmservice.httputils.common.ServiceConstants;

import fmservice.server.global.Scope;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.apache.log4j.Logger;

/**
 *
 *
 * 
 * NOTE: This class does not perform any parsing of the results returned by the 
 * FaceMatch library. It simply stores the final results.
 */
public class ImageQueryResult extends FMServiceResult implements ServiceConstants
{
    
    private static Logger log = Logger.getLogger(ImageQueryResult.class); 
    /*---------------------------------------------------------------------------------------------------*/
    /* Holds match results of each region in the query image
    * Note: The results are combined from all ImageMatchers performing the query
    * The treeMap retains the natural order of the keys in ascending order
    * which implied that distances are sorted automatically when data is inserted
    *
    * Note: There may be multiple matches with the same distance for a query region.
     * This is specially true if the same image is ingested under more than one URL or Tag
    * So <values>  of  matchResult TreeMap is an ArrayList agaist a given distance.  
    * The element String[] contains the two elements of the ingested image: image URL and tag
    /*----------------------------------------------------------------------------------------------------*/
    public class RegionMatchResult
    {
        public  String  queryRegion;
        public TreeMap <Float, ArrayList<String[]> > matchResult;          // Distance vs.  ImageTag, Region,; sorted by Distance
        public int numMatches;
        public RegionMatchResult(String  region)
        {
            queryRegion = region;
            matchResult = new TreeMap();  
        }

        /*-------------------------------------------------------------------------------------*/
        // Update results provided by the caller for each match in binary form
        /*-------------------------------------------------------------------------------------*/
        public int  addMatch( Float distance, String[]matchDetails)
        {
            // Check if there is already a match for this distane
                ArrayList <String[]> matches = matchResult.get(distance);
                if (matches == null)        // no matches at this distance, create a new one
                {
                     matches = new ArrayList<String[]>();
                     matchResult.put(distance, matches);
                }
                matches.add(matchDetails);
                numMatches++;
                return numMatches;
         } 
        
        public TreeMap<Float, ArrayList<String[]>> getMatches()
        {
            return matchResult;
        }
        
        public int getMatchSize()
        {
            int ns = 0;
            Iterator <Float>  it =matchResult.keySet().iterator();
            while(it.hasNext())
            {
               Float distance = it.next();
               ns += (matchResult.get(distance)).size();
            }
            return ns;             // should be the same as numMatches;
        }
        
        /*--------------------------------------------------------------------------------------*/
        // Merge the match entries of the new results to self)
        // Used for combining  Results of parallel queries
        /*----------------------------------------------------------------------------------------*/
        public int mergeMatches(RegionMatchResult newResults)
        {
            TreeMap <Float, ArrayList<String[]> > newMatches = newResults.getMatches();
            Iterator <Float>  it = newMatches.keySet().iterator();
            while(it.hasNext())
            {
               Float distance = it.next();
               if (!matchResult.containsKey(distance) )          // a new distance
                    matchResult.put(distance, newMatches.get(distance));
               else     // a new match with same distance; put together
               {
                   ArrayList <String[]> curEntries = matchResult.get(distance);
                   curEntries.addAll(newMatches.get(distance));
               }  
            }
            return getMatchSize();
        }

        /*----------------------------------------------------------------------------------------*/
        // Truncate the number of entries in the given map
        // We create a new map, and copy required elements  if the results to be truncated 
        // Note that a "distance" may have multiple matches, so stop at the limit in that. 
        // @param naxNum - maximum matches to be returned to the caller
        /*----------------------------------------------------------------------------------------*/
        public int truncateEntries(int maxNum)
        {  
           int oldSize = getMatchSize();           // total number of entries in this match
           if (oldSize <= maxNum)
               return oldSize;
           
          TreeMap <Float, ArrayList<String[] >> returnedResult = new TreeMap(); 
           Iterator <Float> it = matchResult.keySet().iterator();
          
           int nadd = maxNum;           //  number of matches to add in the truncated tree
           while (it.hasNext())
           {
               Float distance = it.next();
               ArrayList <String[]> matches = matchResult.get(distance);
               int ndist  = matches.size();     // number of matches at this distance
              
               if (ndist <= nadd)       // there is room to add all elements
               {
                   returnedResult.put(distance, matches);
                   nadd = nadd-ndist;
               }
               else
               {
                  // create and add a partial set - do not remove from original set, which is error-prone
                    ArrayList <String[]> subset = new ArrayList();
                    for (int ir = 0; ir < nadd; ir++)
                        subset.add(matches.get(ir));
                   returnedResult.put(distance,subset);
                   nadd = 0;
               }
               if (nadd == 0)
                   break;
           }
           matchResult = returnedResult;
           log.info ("Truncated query match size from " + oldSize +" to " + this.getMatchSize());
           return matchResult.size();
        }
    }
    /*-----------------------------------------------------------------------------------------*/
    /*              Main class
   /*------------------------------------------------------------------------------------------*/
    public int extentId;                           // extent in which query is made
    public String imageURL;                   // original source file name for  queryimage
    public String localImageName;       // temporary copy, for facematch operations
    public String indexType;
    public String indexVersion;
    public float tolerance;                      // tolerance for similarity matching
    public int maxMatches;                   // maximum # of matches returned per face
    
    public float  faceFindTime;               // time for finding face regions in query image
    public String faceFindPerfOption;     // Performance option in finding faces
    public String faceFindOptionUsed;   // Performance option in finding faces
    public boolean gpuUsed;                 // was GPU used by the FaceMatchLib
    public int     numIndexFilesLoaded;  // number of index files loaded for this search
    public float  indexUploadTime;        // time to load additional index data from files
    public float  totalQueryTime;           // time to find matches for all faces in the image, including indexLoad

    public ArrayList<ImageQueryResult.RegionMatchResult> regionMatchResults;
    
    
    public ImageQueryResult()
    {
        this(0, "", "");
    }

    public ImageQueryResult(int extentId)
    {
        this(extentId, "", "");
    }

    public ImageQueryResult(int extent, String index, String version)
    {
        super(FACE_MATCH_REGION_SVC, REGION_QUERY_OP);
        extentId = extent;
        indexType = index;
        indexVersion = version;
        
        faceFindTime = (float) -1.0;         // not set
        totalQueryTime =  (float) -1.0;             // unknown
        regionMatchResults = new ArrayList();
    }
    
    /*----------------------------------------------------------------------------------------------*/ 
     protected RegionMatchResult addRegion(String queryRegion)
     {
          RegionMatchResult regionResult = new RegionMatchResult(queryRegion);
          regionMatchResults.add(regionResult);
          return regionResult;
     }
     /*----------------------------------------------------------------------------------------------*/
     protected RegionMatchResult getRegionMatchResult(String queryRegion)
     {
         for (RegionMatchResult result : regionMatchResults)
         {
             if (result.queryRegion.equals(queryRegion))            // TBD compare coordinates
                 return result;
         }
         return null;
     }
     
    /*----------------------------------------------------------------------------------------------*/ 
    public  RegionMatchResult findRegionMatchResult( RegionMatchResult  region)
    {
        if (region == null || region.queryRegion == null)
            return null;
         RegionMatchResult regionResult = getRegionMatchResult(region.queryRegion) ;
         return regionResult;
    }
    /*----------------------------------------------------------------------------------------------*/
    //         Public Methods
    /*----------------------------------------------------------------------------------------------*/
    /*
    /** Add the match results of a given region to self.
    // If the Region does not exist, simply add it to the set.
    // Otherwise, find it and add the matches to the region's existing list
    // Note: The results sare always sorted in ascending " similarity distance"
    /*----------------------------------------------------------------------------------------------*/

    public int  mergeRegionMatches(RegionMatchResult newResult)
    {
        RegionMatchResult currentResult = findRegionMatchResult(newResult);
        if (currentResult == null)
            currentResult = addRegion(newResult.queryRegion);
        int nm = currentResult.mergeMatches(newResult);            // total number of matches 
        currentResult.numMatches = currentResult.matchResult.size();
        return nm;
    }
    /*-----------------------------------------------------------------------------------------------
    * Truncate the entries to the givem maximum size
    * Should be invoked only after mergeRegionMatches() to return the highest ranking ones
    *-------------------------------------------------------------------------------------------------*/
    public void  truncateMatches()
    {
        int numRegions =  regionMatchResults.size();
         for (int i = 0; i < numRegions; i++)
         {
              RegionMatchResult regMatchResult  = regionMatchResults.get(i);
              regMatchResult.truncateEntries(maxMatches);
         }
         return;
    }
   /*----------------------------------------------------------------------------------------------*/
     // Add a matching result to an already stored region
    //----------------------------------------------------------------------------------------------*/
     public int addMatch (String queryRegion, Float distance, String[]matchDetails)
     {
         RegionMatchResult regionResult = getRegionMatchResult(queryRegion);
         if (regionResult == null)
            regionResult = addRegion(queryRegion);
         int nm = regionResult.addMatch(distance, matchDetails);
         return nm;         // total # of matches added
     }

   /*-------------------------------------------------------------------------------------------*/
    public int getNumQueryRegions()
    {
        return regionMatchResults.size();
    }
    
    /*-------------------------------------------------------------------------------------------*/
    public String[] getQueryRegions()
    {
        int n = regionMatchResults.size();
        String[] queryRegions = new String[n];
        for (int i = 0; i < n; i++)
            queryRegions[i] = (regionMatchResults.get(i)).queryRegion;
        return queryRegions;
    }
    
     /*-------------------------------------------------------------------------------------------*/
    public float  getFaceFindTime()
    {
        return (faceFindTime);
    }
   /*-------------------------------------------------------------------------------------------*/
    public float  getTotalTime()
    {
        return (totalQueryTime);
    }
    
    /*-------------------------------------------------------------------------------------------*/
    public ArrayList<RegionMatchResult> getRegionMatchResults()
    {
        return  regionMatchResults;
    }
    
   
      /*-------------------------------------------------------------------------------------------*/
    
      // convert to String for sending to client, do not print image/zone IDs etc.
       public  JSONObject convertToJSONObject()
      {    
        JSONObject  resultObj = new JSONObject();
        
        // Fill-in the general information
        fillStandardInfo(resultObj);

         // fill in Face-specific information
        String extentName =  (extentId <= 0) ? "UNKOWN or INVALID" : 
            (Scope.getInstance().getExtentNameInfo(extentId)[1]);  
            
        resultObj.put(EXTENT_NAME,  extentName);   
        resultObj.put(QUERY_URL, imageURL);
        resultObj.put(INDEX_TYPE, indexType);
        resultObj.put(INDEX_VERSION, indexVersion);  
        resultObj.put(TOLERANCE, new Float(tolerance)); 
        resultObj.put(MAX_MATCHES, new Integer(maxMatches)); 

        resultObj.put(PERFORMANCE_PREF, faceFindPerfOption);
        resultObj.put(PERF_OPTION_USED, faceFindOptionUsed);
        resultObj.put(FACEFIND_TIME,  new Float(faceFindTime));
        resultObj.put(GPU_USED, gpuUsed);
        resultObj.put(NUM_INDEX_FILES_LOADED, numIndexFilesLoaded);
        resultObj.put(INDEX_UPLOAD_TIME,  new Float(indexUploadTime));
        resultObj.put(QUERY_TIME,  new Float(totalQueryTime));
        
         // fill in Face-specific  match information
        int numRegions =  regionMatchResults.size();
        resultObj.put(NUM_REGIONS, numRegions);

        if (numRegions > 0)
        {
            JSONArray  regionArray = new JSONArray();
            
             // create an Array of distance vs. face of each entry of this region
            for (int i = 0; i < numRegions; i++)
            {
                RegionMatchResult regMatchResult  = regionMatchResults.get(i);
                regMatchResult.truncateEntries(maxMatches);
                
                JSONObject matchObject = new JSONObject();          // all matches for a query region
                matchObject.put(QUERY_REGION, regMatchResult.queryRegion);
                matchObject.put(NUM_MATCHES, regMatchResult.numMatches);
               
                // add match info for each matched image
                int n = regMatchResult.matchResult.entrySet().size();
                JSONArray matchArray = new JSONArray();
                Iterator <Float>  it =  regMatchResult.matchResult.keySet().iterator(); 
                while (it.hasNext())
                {
                    Float distance = it.next();
                    ArrayList<String[] >data = regMatchResult.matchResult.get(distance);
                    int ns = data.size();
                    for (int j= 0; j < ns; j++)
                    {
                        JSONObject entryObj = new JSONObject();     // individual entry
                        String[] matchInfo = data.get(j);
                        entryObj.put(MATCH_DISTANCE, distance);
                        entryObj.put(INGEST_URL, matchInfo[0]);
                        entryObj.put(INGEST_IMAGE_TAG, matchInfo[1]);
                        entryObj.put(REGION, matchInfo[2]);
                        matchArray.add(entryObj);
                    }
                }
                matchObject.put(REGION_MATCHES, matchArray); 
                regionArray.add(matchObject);
            }
             resultObj.put(QUERY_MATCHES, regionArray);      // one entry for each region
        }   // end numRegions
         
        return resultObj;
      }
        
        /***********************************************************************/
        public String convertToJSONString()
        {
            JSONObject queryResultObj = this.convertToJSONObject();
            return queryResultObj.toJSONString();
        }
            
}

