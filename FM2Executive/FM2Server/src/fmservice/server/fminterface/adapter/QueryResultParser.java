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

import  fmservice.server.result.ImageQueryResult;

import fmservice.server.ops.imageops.ImageLoader;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 ** QueryResultParser parses the text string returned by the FaceMatch Library as the
 * similarity matching results corresponding to all (one or more) face regions in an image
 * and returns the results as an  ImageQueryResult object.
 *  * 
 *
 */
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

public class QueryResultParser
{
    private static Logger log = Logger.getLogger(QueryResultParser.class);
    
    private static boolean showParsedResult = false;
    
    public static ImageQueryResult  parseString(String  matchResult, String queryImage, String[] queryRegions)
    {
        if (matchResult == null || matchResult.isEmpty())
            return null;
        
        ImageQueryResult  queryResult = new ImageQueryResult();     // create a dummy one
        ArrayList <String[]> regionData = new ArrayList();
        String[] lines = matchResult.split("\n");
        if (lines.length < 2)
        {
            log.debug("No data in result: " + matchResult);
            return null;
        }
        for (int i = 0; i < lines.length; i++)
        {
            if (lines[i].startsWith(queryImage))
            {
                int  endLine = addRegion(queryResult,  queryImage, lines, i);
                if (endLine == -1)      // Region parse error
                {
                    log.warn("MatchResult: " + matchResult);
                    break;                      // error
                }
                i = endLine;     // start of next set
            }
        }
        return queryResult;
    }
    /*---------------------------------------------------------------------------------------------------------------------
    /** Add amatch results for a new region to the query results and
    * return the line index where it ends.
    */
    protected static int addRegion(ImageQueryResult queryResult, String queryImage, String[] lines, int startLine)
    {
        {
            String regionHeader = lines[startLine];
            regionHeader = regionHeader.replaceAll("^\\s+", "").replaceAll("\\s+$", ""); 

             // split it to invidual words
            String[] segs = regionHeader.split("\\s+");

            // region is the second word
            String imageRegion = segs[1];
            int endLine = startLine+1;
            for (int i  = startLine+1; i < lines.length; i++)
            {
                if (lines[i].startsWith(queryImage))
                {
                    endLine = i-1;
                    break;
                }
                else if (i ==  lines.length-1)      // no match till end, so last set
                    endLine = i;
            }

            // parse the contained lines to get the match result, note watch for endLine = totalLines
            for (int i = startLine+1; i <= endLine && i < lines.length; i++)
            {
                try
                {
                    // parse the line for distance, imageTag, faceRegion in image
                    // Image tag is the annotation returned as d[xxx], usually the last element , followed by "}"
                     String matchData = lines[i];
                     if (showParsedResult)  log.debug(">>" + matchData);
                     String match = matchData.replaceAll("^\\s+", "").replaceAll("\\s+$", "");   
                     String[] seg = match.split("\\s+");

                     Float distance = Float.parseFloat(seg[0]);
                     String localImageFile = seg[1];
                     String matchRegion = seg[2].replaceAll("\\{|\\}", "");
                    
                     String imageTag = null;
                     
                     // Retrieve the image tag from the returned data
                     for (int j= 3; j < seg.length; j++)
                     {
                         seg[j] = seg[j].replaceAll("\\s", "");
                         if (seg[j].startsWith("d"))
                         {
                            imageTag = (seg[j].substring(1)).replaceAll("\\[|\\]", "").replaceAll("\\}", "");
                            break;
                         }
                     }
                     if (imageTag == null)          // not found
                     {
                         log.warn("ImageTag not found in returned query result,  extracting from file name");
                         imageTag = ImageLoader.getImageTagFromFilename(localImageFile);
                     }
                     String[] matchDetails = new String[] {localImageFile, imageTag, matchRegion};
                     queryResult.addMatch(imageRegion, distance, matchDetails);
                }
                catch (Exception e)
                {
                    log.error("Exception in parsing line. startLine=" + startLine+", endLine: " + endLine +", Line number: " + i, e);
                    return -1;
                }
            }
            return endLine;
        }
    }
}