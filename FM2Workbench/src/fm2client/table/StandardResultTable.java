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
*   StandardResultTable.java
* 
*/
package fm2client.table;
import fm2client.analyzer.ResultAnalyzer;
import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.ServiceUtils;
import java.awt.Color;
import org.json.simple.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.util.HashMap;

import org.apache.log4j.Logger;


/********************************************************************************************
* This table displays the results of a FaceMatch service request in a standard manner.
 * Depending upon the service/operation type, some columns may be empty.,
*
*-*****************************************************************************************/

public class StandardResultTable extends ResultTable implements ServiceConstants, FM2TableConstants
{
    
    private static Logger log = Logger.getLogger(StandardResultTable.class);
    
    protected HashMap<String, String> imageTag2UrlMap = null;
    
    // list of standard fields: 
    // TestID, operation, extentName, imageURL, FaceFindTime, customField, statusCode, statusMsg

    int currentRow;
    public static String[] buttons = {"Display Selected Row Details"}; 

    
    //-------------------------------------------------------------------------------------------------------------------------//
    // Constructors - to create the TablePanel and add it to a parent Panel or to put it
    // in its own JFrame
    //-------------------------------------------------------------------------------------------------------------------------//
    /*
     * ColumnNameMap shows the name of the columns to be displayed corresponding to the 
    *   element names in the result.
    */
    public  StandardResultTable (ResultAnalyzer analyzer) 
    {
       super ( FACEMATCH_REGION_OPERATION, STANDARD_TABLE_NAME, StandardResultColumnNames, 
                 null,  buttons);
        analyzer.setImageTag2URLMap(null);
        setTableColor(new Color(0xD9F4F9));
        //imageTag2UrlMap = imageTagMap;
    }
    
    //-------------------------------------------------------------------------------------------------------------------------//
       public  StandardResultTable (JPanel parentPanel, String tableName, HashMap<String, String> imageTagMap)
    {
        super(parentPanel, tableName, RegionIngestColumnNames);
        setTableColor(new Color(0xD9F4F9));
        imageTag2UrlMap = imageTagMap;
    }
    
    public int  addRow(int testNum, int testId, JSONObject testResult)
    {
        int displayStatus = displayTestResultRecord(testNum, testId, testResult);
        if (displayStatus == 1)
             scrollToVisible(myTable.getModel().getRowCount());
        return displayStatus;
    }
     
    /*--------------------------------------------------------------------------------
     * Display  a single row in the table with standard information
     * from success information in the  JSONObject (info returned from Server)
     *--------------------------------------------------------------------------------*/
    public int displayTestResultRecord(int testNum, int testId, JSONObject testResult)
    {
        JTable table = myTable;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        
        try
        {
            int svc =  ((Long)testResult.get(SERVICE)).intValue();
            int oper = ((Long)testResult.get(OPERATION)).intValue();
            String operation = ServiceUtils.convertOperationType2Name(svc, oper);
            double totalTime = ((Double)testResult.get(SERVICE_TIME)).doubleValue();
            double operationTime;           // ingest or query
            String imageId= "";
            
            
            String extent = "N/A";
            if (testResult.get(EXTENT_NAME) != null)
            {
                extent = (String)testResult.get(EXTENT_NAME);
            }
            int statusCode = ((Long)testResult.get(STATUS_CODE)).intValue();
            String statusMessage = (String)testResult.get(STATUS_MESSAGE);
            //String url= (String)testResult.get("url");

            String faceFindTime = "N/A";
            if (testResult.get(FACEFIND_TIME) != null)
            {
               faceFindTime = ((Double)testResult.get(FACEFIND_TIME)).toString();
               if ((testResult.get(URL) != null))
                   imageId = (String)testResult.get(URL);
            }
            
            // Extract info specific to ingest
             if (oper == ServiceConstants.REGION_INGEST_OP)
             {
                  if ((testResult.get(URL) != null))
                   imageId = (String)testResult.get(URL);
                 
                int numRegions = ((Long)testResult.get(INGESTED_REGIONS)).intValue();
                String coordinates = "";
                if (numRegions > 0)
                {
                    coordinates = (String)testResult.get(FACE_REGIONS);
                    operationTime = ((Double)testResult.get(INGEST_TIME)).doubleValue();
                }
             }
             
                // Extract info specific to query
             if (oper == ServiceConstants.REGION_QUERY_OP)
             {
                 if ((testResult.get(QUERY_URL) != null))
                 imageId = (String)testResult.get(QUERY_URL);
                 int numRegions = ((Long)testResult.get(NUM_REGIONS)).intValue();
                 String coordinates = "";
                  if (numRegions > 0)
                  {
                      coordinates = (String)testResult.get(FACE_REGIONS);
                      operationTime = ((Double)testResult.get(QUERY_TIME)).doubleValue();
                 }
             }
                // Extract info specific to remove 
             if (oper == ServiceConstants.REGION_REMOVE_OP)
             {
                 int numRegions  = 0;
                  if ((testResult.get(IMAGE_TAG) != null))
                        imageId = (String)testResult.get(IMAGE_TAG);
                  if (testResult.get(NUM_REGIONS) != null)
                      numRegions = ((Long)testResult.get(NUM_REGIONS)).intValue();
               ;
             }
            // Note faceFindTime, totalTime etc. are the aggregate of all regions of the test
            //  "Operation", "Test ID",  "Extent", "Image URL",  "Service Info", "FaceFindTime", "Total Time", "Status Code", "Status Message"};
            Object[] data = {operation, testId, extent, imageId, "",faceFindTime, totalTime, statusCode, statusMessage};
            model.addRow(data);
        }

        catch (Exception e)
        {
            log.error("Could not display FaceFind result for test number " + testNum, e);
            return 0;
        } // end catch  
        return 1;
    }
}
   

