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
*   RegionIngestResultTable.java
*/
package fm2client.table;
import fm2client.analyzer.ResultAnalyzer;

import fmservice.httputils.common.ServiceConstants;
import java.awt.Color;
import org.json.simple.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.util.HashMap;

import org.apache.log4j.Logger;


/********************************************************************************************
*   This class displays the results of a Face Ingest test, in the form of JSON String
*   It is used by the FaceMatch2 Test Client to display data returned by the FM2 server
*
*-*****************************************************************************************/

public class RegionIngestResultTable extends ResultTable implements FM2TableConstants, ServiceConstants
{
    
    private static Logger log = Logger.getLogger(RegionIngestResultTable.class);
    
    // To map from imageTag to URL for display of  Ingested image;
    protected HashMap<String, String> imageTag2URLMap = new <String, String> HashMap();
    //-------------------------------------------------------------------------------------------------------------------------//
    // Constructors - to create the TablePanel and add it to a parent Panel or to put it
    // in its own JFrame
    //-------------------------------------------------------------------------------------------------------------------------//
    /*
     * ColumnNameMap shows the name of the columns to be displayed corresponding to the 
    *   element names in the result.
    */
    public  RegionIngestResultTable ( ResultAnalyzer analyzer)
    {
        super( REGION_INGEST_OPERATION, INGEST_TABLE_NAME, RegionIngestColumnNames, analyzer, null);
        setTableColor(new Color(0xD9F4F9));
        
        analyzer.setImageTag2URLMap(imageTag2URLMap);
    }
    
    //-------------------------------------------------------------------------------------------------------------------------//
 /*      public  RegionIngestResultTable (JPanel parentPanel, String tableName)
    {
        super(parentPanel, tableName, RegionIngestColumnNames);
        setTableColor(new Color(0xD9F4F9));
    }
   */ 
    public int  addRow(int testNum, int testId, JSONObject testResult)
    {
        int displayStatus = displayRegionIngestRecord(testNum, testId, testResult);
        String imageTag = (String)testResult.get(IMAGE_TAG);
        String imageURL = (String)testResult.get(URL);
        imageTag2URLMap.put(imageTag, imageURL);
        if (displayStatus == 1)
             scrollToVisible(myTable.getModel().getRowCount());
        return displayStatus;
    }
    

    /*--------------------------------------------------------------------------------
     * Display  a single row in the table
     * Create a JSONObject from the resultString, and extract individual elements
     *--------------------------------------------------------------------------------*/
    public int displayRegionIngestRecord(int testNum, int testId, JSONObject testResult)
    {
        JTable table = myTable;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        try
        {
            String imageTag = (String)testResult.get(IMAGE_TAG);
            //String imageUrl = (String)testResult.get(URL);
            String extent = (String)testResult.get(EXTENT_NAME);
            double faceFindTime = ((Double)testResult.get(FACEFIND_TIME)).doubleValue();
            int numRegions = ((Long)testResult.get(INGESTED_REGIONS)).intValue();
            String coordinates = "";
            double ingestTime = 0.0;
            if (numRegions > 0)
            {
                coordinates = (String)testResult.get("faceRegions");
                ingestTime = ((Double)testResult.get("ingestTimeMsec")).doubleValue();
            }
            double totalTime = ((Double)testResult.get("serviceTimeMsec")).doubleValue();
          
            Object[] data = {
                testNum, testId, extent, imageTag, numRegions, coordinates, 
                faceFindTime, ingestTime, totalTime}; 
            model.addRow(data);

            if (model.getRowCount() >= 1)
            {
                resultTable.setVisible(true);                       // resultTable=> self, the JDialog
            }
	 
        } // end try
        catch (Exception e)
        {
            log.error("Could not display FaceFind result for test number " + testNum, e);
            return 0;
        } // end catch  
        return 1;
    }
 
}
   

