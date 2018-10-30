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
import org.json.simple.JSONObject;

import org.apache.log4j.Logger;
/**
 * The class IngestResult contains the results returned by ingesting an image. 
 * For each region of the image that was ingested, it contains the image tag, 
 * faceRegion (without landmarks), index type and the name of the file containing 
 * stored indexed data, For multi-level indexing such as DIST/DIST-MANY, only the
 * top level index file name is stored.
 * 
 * Note: It may be possible for to ingest new  region to an existing image. This is
 * reflected in the newImage parameter (set to false). Currently, this is not supported.
 * 
 * Note: All performance times in milliseconds only. 
 * 
 *
 */


public class ImageIngestResult extends FMServiceResult implements ServiceConstants
{
    private static Logger log = Logger.getLogger(ImageIngestResult.class);
    
    //For  each region ingested
    public class RegionIngestResult
    {
        public  String  faceregion;
        public int regionIndex;
        public int regionId;                  // database ID of ingested region (zone) of image
        public String indexFileName;          // full pathname of the index file
        public float ringestTime;             // time in msec for ingesting the region
       
        
        public RegionIngestResult(String  region,  int index, String filename, float time)
        {
            faceregion = region;
            regionIndex = index;
            indexFileName = filename;         // null implies not ingested
            ringestTime = time;               // time to ingest each region
            regionId = -1;
        }
       // Set the database ID of a region after ingest is complete
        public void setRegionId(int rid)
        {
            regionId = rid;
        }
    }
   /*------------------------------------------------------------------------------------------*
    */ 
    public int extentId;                              // client's image collection
    public String imageURL;                     // original source file name for image
    public String imageTag;                     // Unique_tag within the Extent
    
    public String thumbnailPath;             // relative pathname of thumbtag file, if ceated
    public  boolean newImage = true;
    public String indexType;
    public String indexVersion;
    public int    imageId;                           // database ID of image 
    public float  faceFindTime;                 // time for finding face regions
    public float  totalIngestTime;              // time to ingest all regions in image (after facefind)
    public String faceFindPerfOption;        // faceFind performance option
    public String faceFindOptionUsed;      // which option was used to find faces in the image
    public int      nregCount;                     // number of regions actually ingested
    public ArrayList<RegionIngestResult> regionResults;
    
    // postProcess info for updating the database and search domain with ingest data
     public Status postProcessStatus;
     public float postProcessTime;           
    

    public ImageIngestResult(int extent)
    {
        super(FACE_MATCH_REGION_SVC, REGION_INGEST_OP);
        this.extentId = extent;
        regionResults = new ArrayList();
    }
    
    public ImageIngestResult(int extent, String tag, String index, String version)
    {
        super(FACE_MATCH_REGION_SVC, REGION_INGEST_OP);
        extentId = extent;
        imageTag = tag;
        newImage = true;
        indexType = index;
        indexVersion = version;
        faceFindTime = (float) -1.0;         // not set
        totalIngestTime =  (float) -1.0;    // 
        faceFindPerfOption = "";
        imageId = -1;
        thumbnailPath = "";
        nregCount = 0;
        regionResults = new ArrayList();
    }
    
    /*-------------------------------------------------------------------------------------------*/
    // Set the database ID of an image  after ingest is complete
     public void setImageId(int rid)
     {
         imageId = rid;
     }
      /*-------------------------------------------------------------------------------------------*/
    // Set the relative pathname od the created thumbnail for this image
     public void setThumbnailPath(String  path)
     {
         thumbnailPath = path;
     }

    /*-------------------------------------------------------------------------------------------*/
    // Add results for ingesting a new Region
     public void addResult(String region,  int index, String filename)
    { 
        addResult(region,  index, filename,  -1);
    }
     
    public void addResult(String region,  int regionIndex, String filename, float msec)
    {
        RegionIngestResult result = new RegionIngestResult(region,  regionIndex, filename, msec);
        regionResults.add(result);
        if (filename != null && !filename.isEmpty())
            nregCount++;
    }
  
   /*-------------------------------------------------------------------------------------------*/
    // get number of regions actually ingested
    public int getIngestedRegionCount()
    {
        return nregCount;
    }
    
     /*-------------------------------------------------------------------------------------------*/
    public float  getFaceFindTime()
    {
        return (faceFindTime);
    }
   /*-------------------------------------------------------------------------------------------*/
    public float  getTotalIngestTime()
    {
        return (totalIngestTime);
    }
    
    /*-------------------------------------------------------------------------------------------*/
      public ArrayList<RegionIngestResult> getRegionIngestResults()
    {
        return  regionResults;
    }
      /*-------------------------------------------------------------------------------------------*/
      // convert to String for sending to client, do not print image/zone IDs etc.
       public  JSONObject convertToJSONObject()
      {    
        JSONObject  resObj = new JSONObject();
        
        fillStandardInfo(resObj);
        String extentName =  (extentId <=0) ? "" :  (Scope.getInstance().getExtentNameInfo(extentId)[1]);
        resObj.put(EXTENT_NAME, extentName);
        resObj.put(URL, imageURL);
        resObj.put(INGEST_IMAGE_TAG, imageTag);

         // fill in Face-specific information
        resObj.put(NUM_REGIONS, nregCount);
        resObj.put(FACEFIND_TIME, faceFindTime);
        resObj.put(PERFORMANCE_PREF, faceFindPerfOption);
        resObj.put(PERF_OPTION_USED, faceFindOptionUsed);
        resObj.put(GPU_USED, gpuUsed);
        
        if (nregCount > 0)
        {
           // JSONArray  regionArray = new JSONArray();
            String faceRegionStr = "";
            for (int i = 0; i < regionResults.size(); i++)
            {
                RegionIngestResult region = regionResults.get(i);
                if (region.indexFileName == null)       // false region, not ingested
                    continue;
                if (i > 0)
                    faceRegionStr += "\t";
                faceRegionStr += (region.faceregion);
            }
            resObj.put(FACE_REGIONS, faceRegionStr);
            resObj.put(INDEX_TYPE, indexType);
            resObj.put(INGEST_TIME, totalIngestTime);
            resObj.put("databaseTime", postProcessTime);
        }
        return resObj;
      }
 /*-----------------------------------------------------------------------------------------------*/
 /** Convert self to a JSON string form (to be sent to the client side)
  */
     public  String convertToJSONString() 
     {
         JSONObject jsonObj = convertToJSONObject();
         String  jsonString = jsonObj.toJSONString();
         return jsonString;
     }
}
    
      

     

