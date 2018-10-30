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
public class MultiExtentQueryResult extends FMServiceResult implements ServiceConstants
{
    
    private static Logger log = Logger.getLogger(MultiExtentQueryResult.class); 

    public String[] queryRegions;
    public float  faceFindTime;               // time for finding face regions in query image
    
    ArrayList<ImageQueryResult> meQueryResults;

  public MultiExtentQueryResult()
  {
        super(FACE_MATCH_REGION_SVC, MULTIEXTENT_REGION_QUERY_OP);
         meQueryResults = new ArrayList();
    }
    
    
    /*----------------------------------------------------------------------------------------------*/ 
     public void addMatchResult(ImageQueryResult matchResult)
     {
         meQueryResults.add(matchResult);
         int i = meQueryResults.size();
         if (i == 1)            // do it only once
         {
               queryRegions = matchResult.getQueryRegions();          // face regions to match against
               faceFindTime = matchResult.faceFindTime;

         }
     }
    

   /*-------------------------------------------------------------------------------------------*/
    public int getNumQueryRegions()
    {
        return (queryRegions == null) ? 0 : queryRegions.length;
    }
    
     /*-------------------------------------------------------------------------------------------*/
    public float  getFaceFindTime()
    {
        return (faceFindTime);
    }

      /*-------------------------------------------------------------------------------------------*/
    
      // convert to String for sending to client, do not print image/zone IDs etc.
       public  JSONObject convertToJSONObject()
      {    
        JSONObject  resultObj = new JSONObject();
        
        // Fill-in the general information
        fillStandardInfo(resultObj);
        resultObj.put(FACEFIND_TIME,  new Float(faceFindTime));

        // fill in common info from the first entry
         if (meQueryResults.size() > 0)
        {
            ImageQueryResult  qr = meQueryResults.get(0);
            resultObj.put(QUERY_URL, qr.imageURL);
            resultObj.put(INDEX_TYPE, qr.indexType);
            resultObj.put(INDEX_VERSION, qr.indexVersion);  
            resultObj.put(TOLERANCE, new Float(qr.tolerance)); 
            resultObj.put(MAX_MATCHES, new Integer(qr.maxMatches)); 
            resultObj.put(GPU_USED, qr.gpuUsed);
           // resultObj.put(NUM_REGIONS, qr.regionMatchResults.size());
         
            String[] fieldNames = {EXTENT_NAME, 
                PERFORMANCE_PREF, INDEX_UPLOAD_TIME, QUERY_TIME, QUERY_MATCHES};
         
            ArrayList<JSONObject>  extentResultArray = new JSONArray();
            for (int i = 0; i < meQueryResults.size(); i++)
            {
                JSONObject meResult = meQueryResults.get(i).convertToJSONObject();
                JSONObject extentResult = new JSONObject();
                for (int j = 0; j < fieldNames.length; j++)
                {
                    extentResult.put(fieldNames[j], meResult.get(fieldNames[j])); 
                }
                extentResultArray.add(extentResult);
            }
           resultObj.put(QUERY_RESULTS, extentResultArray);
        }
        return resultObj;
      }        
        /***********************************************************************/
        public String convertToJSONString()
        {
            JSONObject queryResultObj = this.convertToJSONObject();
            return queryResultObj.toJSONString();
        }    
}

