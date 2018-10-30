/*
Informational Notice:
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
an agency of the Department of Health and Human Services, United States Government.

- The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

- The license does not supersede any applicable United States law.

- The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
(FAR) 48 C.F.R. Part52.227-14, Rights in Data—General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
•	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

•	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.

•	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/* 
*   RegIngestResultDisplay.java
*/
package fm2client.table;

import fmservice.httputils.common.ServiceConstants;
import fm2client.analyzer.ResultAnalyzer;

import org.json.simple.*;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JPanel;

import javax.swing.JPopupMenu;

import java.awt.Color;

import java.util.HashMap;
import java.util.ArrayList;

import org.apache.log4j.Logger;


/********************************************************************************************
*   This class displays the results of a Face Ingest test, in the form of JSON String
*   It is used by the FaceMatch2 Test Client to display data returned by the FM2 server
*
*-*****************************************************************************************/

public class RegionQueryResultTable extends ResultTable implements ServiceConstants
{
    private static Logger log = Logger.getLogger(RegionQueryResultTable.class);
    
  
    public static class RegionMatch 
    {
       public int rank;
       public float distance;
       public String imageTag;
       public String matchCoord;
       public String matchImageUrl;       // url of matched image
    }
    
    // row number vs. queryResultList of all matching regions and their distances
    protected HashMap <Integer, ArrayList<RegionMatch>> queryResultMap; 
    // To map from imageTag to URL for display of  Ingested image;
    protected HashMap<String, String> imageTag2URLMap; 
    
    protected JPopupMenu  popup;
    
    int  currentRow;
    
    
   /*---------------------------------------------------------------------------------------*/
    // implementation of abstract method
     public int getOperationType() { return REGION_QUERY_OPERATION;}
  //------------------------------------------------------------------------------------------------------------------------//  
     
     
    public  RegionQueryResultTable (ResultAnalyzer analyzer)
    {
        super( REGION_QUERY_OPERATION, QUERY_TABLE_NAME, RegionQueryColumnNames, analyzer, null);
        setTableColor(new Color(0xE6E1FC));
        
        queryResultMap = new HashMap();
        imageTag2URLMap = new <String, String> HashMap();
        analyzer.setImageTag2URLMap(imageTag2URLMap);
    }
    
      public  RegionQueryResultTable (JPanel parentPanel, String tableName,  
        ResultAnalyzer analyzer) 
    {
        super(parentPanel, tableName, RegionQueryColumnNames);
        setTableColor(new Color(0xE6E1FC));
        
        queryResultMap = new HashMap();
    }
 
    public int  addRow(int testNum, int testId, JSONObject testResult)
    {
        int displayStatus = displayRegionQueryRecord(testNum, testId, testResult);
        if (displayStatus == 1)
             scrollToVisible(myTable.getModel().getRowCount());
        return displayStatus;
    }

    
    /*----------------------------------------------------------------------------------------------------------
    *  Return all the query matches returned by the server for the given table row
    *----------------------------------------------------------------------------------------------------------*/
    public  ArrayList<RegionMatch>  getRegionMatches(int  rowNum)
    {
        return queryResultMap.get(new Integer(rowNum));
    }
    /*--------------------------------------------------------------------------------
     * Display rows in the table, with top level information - corresponding
     * to each region in the queried image for a single query.
     * Also store all associated information for going to the lower level for the row, by
     * creating a JSONObject from the input resultString, and extract individual elements
     * mentioned above.
     *--------------------------------------------------------------------------------*/
    public int displayRegionQueryRecord(int testNum, int testId, JSONObject testResult)
    {
        JTable table = myTable;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int rowNum = model.getRowCount();         // Srart row for this testNum
        try
        {
            String queryImage = (String)testResult.get(QUERY_URL);
            String extentName = (String)testResult.get(EXTENT);
            float tolerance = ((Double)testResult.get(TOLERANCE)).floatValue();
            double faceFindTime = ((Double)testResult.get(FACEFIND_TIME)).doubleValue();
            double indexUploadTime = ((Double)testResult.get(INDEX_UPLOAD_TIME)).doubleValue();
            double queryTime = ((Double)testResult.get(QUERY_TIME)).doubleValue();
            double totalTime = ((Double)testResult.get(SERVICE_TIME)).doubleValue();
            int numRegions = ((Long)testResult.get(NUM_REGIONS)).intValue();
            
            if (numRegions == 0)
                return 0;               // no matches found
            
            //int curRow = 0;
            JSONArray queryMatches = (JSONArray)testResult.get(QUERY_MATCHES);
            
            // Repeat for each region in the query image
            for (int i = 0; i < queryMatches.size(); i++)
            { 
                JSONObject regionMatch = (JSONObject)queryMatches.get(i);
                String coordinates = (String)regionMatch.get(QUERY_REGION);
                int totalMatches = ((Long)regionMatch.get(NUM_MATCHES)).intValue();
                JSONArray matchArray = (JSONArray)regionMatch.get(REGION_MATCHES);
                
                // get results of individual matches with ingested images
                ArrayList<RegionMatch> queryResultList = new ArrayList();
                RegionMatch bestMatch = null; 
                
                for (int j = 0; j < matchArray.size(); j++)
                {
                    JSONObject matchEntry = (JSONObject)matchArray.get(j);
                    RegionMatch match = new RegionMatch();
                    match.rank = j+1;           // 1-based
                    match.distance = ((Double)matchEntry.get(MATCH_DISTANCE)).floatValue();
                    match.imageTag =(String)matchEntry.get(IMAGE_TAG);
                    match.matchCoord= (String)matchEntry.get(REGION);
                    match.matchImageUrl = (String)matchEntry.get(INGEST_URL);
                    queryResultList.add(match);
                    if (j == 0) 
                        bestMatch = match;
                  // also add to the tag->url map  
                    if (! imageTag2URLMap.containsKey( match.imageTag))
                        imageTag2URLMap.put( match.imageTag,  match.matchImageUrl);      
                }
                int returnMatches =  matchArray.size();
                
                // Note faceFindTime, totalTime etc. are the aggregate of all regions of the test
                Object[] data = {
                    testNum, testId, extentName, queryImage, i, coordinates, tolerance, returnMatches, 
                    bestMatch.imageTag, bestMatch.distance, bestMatch.matchCoord,
                    faceFindTime, queryTime, indexUploadTime,  totalTime}; 
                model.addRow(data);
                queryResultMap.put(new Integer(rowNum++), queryResultList);
            }

            if (model.getRowCount() >= 1)
            {
                 resultTable.setVisible(true);                       // resultTable=> self, the JDialog
            }
	 
        } // end try
        catch (Exception e)
        {
            log.error("Could not display query result for test number " + testNum, e);
            return 0;
        } // end catch  
        return 1;
    }     
}
   

