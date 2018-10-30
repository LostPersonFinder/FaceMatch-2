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

import org.json.simple.JSONObject;

import org.apache.log4j.Logger;
/**
 * The class contains the results returned by removing an image ir a region
 * within an image from FaceMatching. 
 * 
 * Note: All performance times in milliseconds. 
 * 
 *
 */


public class RemoveImageResult extends FMServiceResult implements ServiceConstants
{
    private static Logger log = Logger.getLogger(RemoveImageResult.class);

    public String extentName;                // client's image set
    public String imageTag;                    // Unique_tag within the Extent for the image
    public int regionIndex ;                      // zero-based index if a region,  -1 if the whole the image

    public int    imageId;                          // database ID of image 
    public float  totalRemoveTimeMsec;     // time to ingest all regions in image (after facefind)
    public int     removeCount;             // number of regions actually removed
    
    
    public RemoveImageResult(String extent, String tag)
    {
        super(FACE_MATCH_REGION_SVC, REGION_REMOVE_OP);
        extentName = extent;
        imageTag = tag;
        regionIndex = -1;
        removeCount = 0;
    }

      /*-------------------------------------------------------------------------------------------*/
    // convert to String for sending to client, do not print image/zone IDs etc.
     public  JSONObject convertToJSONObject()
    {    
          JSONObject  resObj = new JSONObject();
          fillStandardInfo(resObj);
          resObj.put(EXTENT_NAME, extentName);
          resObj.put(INGEST_IMAGE_TAG, imageTag);
          resObj.put(NUM_REGIONS, removeCount);
          if (regionIndex >= 0 && removeCount > 0)
                resObj.put(REGION_INDEX, regionIndex);            // for regionRemove
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
    
      

     

