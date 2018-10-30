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
 *  DisplayManager.Java
 */
 
package fm2client.display;

import fm2client.table.FM2TableConstants;

import fm2client.analyzer.ResultAnalyzer;
import fm2client.table.ResultTable;

import fm2client.table.FaceFindResultTable;
import fm2client.table.RegionIngestResultTable;
import fm2client.table.RegionQueryResultTable;
import fm2client.table.StandardResultTable;
import fm2client.table.ErrorResultTable;
import fm2client.testgen.TestResultFile;

import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.ServiceUtils;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

/**
 *
 * Class to initialize the tables to display 
 * There are four different tables handled by the object. They are:
 *      FaceFinder result table
 *      Face Ingest result table
 *      Face Query result table
 *      Error display table (for any service)
 *
 * Each table is initialized with the column model. The first three table rows
 * are selectable and display the corresponding image/face when selected.
 * 
 * Display Manager is either invoked to either display stored  results from a file
 * or to add a server returned data (in realtime mode), one row at a time.
 *  
 * Currently Whole image matching results are not handled.
 *---------------------------------------------------------------------------------
 * @author dmisra
 */
public class DisplayManager implements ServiceConstants, FM2TableConstants
{
    private static Logger log = Logger.getLogger(DisplayManager.class);
    
    // Tables for displaying individual types of data
    protected FaceFindResultTable ffResultTable = null; 
    protected RegionIngestResultTable regIngestResultTable = null;
    protected RegionQueryResultTable regQueryResultTable = null;
    protected StandardResultTable standardResultTable = null;
    protected ErrorResultTable errorResultTable = null;
    
    // set to true when a table is closed by the user  
    protected boolean ffResultTableClosed = false;
    protected boolean regIngestResultTableClosed = false;
    protected boolean regQueryResultTableClosed = false;
    
   
    // Obvject to analyse the server-returned results of each test
    protected ResultAnalyzer resultAnalyzer;
    protected HashMap <String, String> imageAnnotationMap = null;
    
    protected ResultDisplayPane serviceResultDisplayPane = null;
    protected ResultTable activeTable;
    
    // display format
   // int STANDARD_FORMAT = 1;
    //int DETAILED_FORMAT = 2;
    

    JSONParser jparser = new JSONParser();
    
    public DisplayManager(ResultAnalyzer analyzer)
    {
        System.out.println("-- DisplayManager created");
       resultAnalyzer = analyzer;               // To visually analyze the data when user selects a row
    }

    // set the image tag to url mapping info for later retrieval of URLs if needed
    public void setImageAnnotationMap(HashMap<String, String> imageAnnotMap)
    {
        imageAnnotationMap = imageAnnotMap;  
    }


    /*----------------------------------------------------------------------------------------------------
    * Display result of a client test (called for showing real-time test results
    * @param testnum - Test number in the test set.
    *------------------------------------------------------------------------------------------------------*/
    public int displayResult(int testNum, int testId, int serviceType, int operation, String serviceResult)
    {
        try
        {
            JSONObject resultObject = (JSONObject) jparser.parse(serviceResult);
            int statusCode = ( (Long)resultObject.get("statusCode")).intValue();
            int status = addResultToTable(testNum, testId,  serviceType, operation, resultObject);
            return status;
        }
        catch (Exception e)
        {
            log.error ("Cannot display results due to configuration error", e);
            return 0;
        }
    }
    
     /*----------------------------------------------------------------------------------------------------
    * Display result of a client test in a standard  format which is common to all 
    * FaceMatch services.
    * This is used when differenr types of services are requested in a batch as in
    * a Scenario Test
    *------------------------------------------------------------------------------------------------------*/
    public int displayStandardResult(int testNum, int testId, int serviceType, int operation, String serviceResult)
    {
        try
        {
            JSONObject resultObject = (JSONObject) jparser.parse(serviceResult);
            int statusCode = ( (Long)resultObject.get("statusCode")).intValue();
            int status = addResultToStandardTable( testNum, testId, serviceType, operation, resultObject);
            return status;
        }
        catch (Exception e)
        {
            log.error ("Cannot display results due to configuration error", e);
            return 0;
        }
    }
    
    /*-------------------------------------------------------------------------------------------------
    * Display result data returned by the server and stored in a file as records
    * Open and read the file, display individual records.
    * Returns the total number of records read
    *-----------------------------------------------------------------------------------------------*/

    public int displayResultFromFile(String filename)
    {
        TestResultFile resultFile = new TestResultFile(filename, false);
        if (!resultFile.isFileOpened())
            return 0;           // error opening file in read mode
        
        int recordNum = 0;
        while (true)
        {
            String resultData = resultFile.readNextRecord();
            if (resultData == null)         // end of file
                break;
            try
            {
                recordNum++;
                JSONObject resultObject = (JSONObject) jparser.parse(resultData);
                int testNumber = ((Long)resultObject.get("testNumber")).intValue();
                 int testId = ((Long)resultObject.get("testId")).intValue();
                int serviceType = ((Long)resultObject.get(ServiceConstants.SERVICE)).intValue();
                int operation =   ((Long)resultObject.get(ServiceConstants.OPERATION)).intValue();
                String serviceResultStr = (String) resultObject.get("serviceResult");
                JSONObject serviceResult = (JSONObject) jparser.parse(serviceResultStr);
                addResultToTable(testNumber, testId,  serviceType, operation, serviceResult);
            }
            catch (Exception e)
            {
             log.error ("Invalid JSON record received from server; record #:" + recordNum,e);
            }
        }
        return recordNum;
    }
    
    /*-------------------------------------------------------------------------------------------------------*
    * Add a new Row to the table, showing the return result
    * If the first row, create the corresponding TabbedPane window with two panes
    * 1) Pane for successful results of the requested service
    * 2) Pane for results of unsuccesful requests
    * Note: Different tables aer built for display of different types of results.
    * This is useful when the caller is performing a number of similar operations such as
    * ingest/remove ot testing functions such as query in a batch mode
    *--------------------------------------------------------------------------------------------------------*/
    protected int  addResultToTable(int testNum, int testId, int serviceType, 
            int operation, JSONObject resultObject)
    {
         int statusCode = ( (Long)resultObject.get("statusCode")).intValue();
         boolean isSuccess = (statusCode == 1);
        
        int addStatus = 0;
        if (serviceType == FACE_FIND_SVC)
        {
            if (ffResultTable == null)              // first time
            {
                resultAnalyzer.setImageAnnotationMap(imageAnnotationMap);
                ffResultTable = new FaceFindResultTable(resultAnalyzer);
                errorResultTable = new ErrorResultTable(resultAnalyzer);
                serviceResultDisplayPane = createResultDisplayPane(FACE_FIND_SVC, operation, ffResultTable, errorResultTable);
                serviceResultDisplayPane.getFrame().setVisible(true);
                ffResultTableClosed = false;
                activeTable = ffResultTable;
            }
        }
        else if (serviceType == FACE_MATCH_REGION_SVC)
        {
            if (operation == ServiceConstants.REGION_INGEST_OP)
            {
                if (regIngestResultTable == null)              // first time
                {
                    regIngestResultTable = new RegionIngestResultTable( resultAnalyzer);
                    errorResultTable = new ErrorResultTable(resultAnalyzer);
                    serviceResultDisplayPane = createResultDisplayPane(FACE_MATCH_REGION_SVC, operation, 
                            regIngestResultTable, errorResultTable);
                    serviceResultDisplayPane.getFrame().setVisible(true);
                    regIngestResultTableClosed = false;
                    activeTable = regIngestResultTable;
                }
            }

             else if (operation == ServiceConstants.REGION_QUERY_OP)
            {
                if (regQueryResultTable == null)              // first time
                {
                    regQueryResultTable = new RegionQueryResultTable( resultAnalyzer);                  
                    if ( imageAnnotationMap != null)
                        resultAnalyzer.setImageAnnotationMap(imageAnnotationMap);
                    errorResultTable = new ErrorResultTable(resultAnalyzer);
                    
                    serviceResultDisplayPane = createResultDisplayPane(FACE_MATCH_REGION_SVC, operation, 
                            regQueryResultTable, errorResultTable);
                    serviceResultDisplayPane.getFrame().setVisible(true);
                    regQueryResultTableClosed = false;
                    activeTable = regQueryResultTable;
                }  
            }
        }
        else if (operation == ServiceConstants.REGION_REMOVE_OP)
        {
            // all necessary steps are taken by the following call for the StandardTable entry
            addStatus = addResultToStandardTable( testNum, testId,  serviceType, operation,  resultObject);
            return addStatus;
        }
            
        else
        {
            log.warn ("Result display for Service " + serviceType + " or operation " 
                + operation + " not yet supported");    
            return 0;
        }
        
       // Add the result to the appropriate table
        if (isSuccess)
         {
             addStatus =activeTable.addRow(testNum, testId, resultObject);
             activeTable.repaint();
         }
         else
        {
             addStatus = errorResultTable.addRow(testNum, testId, resultObject);
        }
        return addStatus;           // 1 if row was added
    }
        
/*-----------------------------------------------------------------------------------------------------------------
  *  Create a  the Window (JFrame) to display results of the govrn service/operation 
  /*-----------------------------------------------------------------------------------------------------------------*/
    protected  ResultDisplayPane createResultDisplayPane(int service, int operation, 
            ResultTable resultTable, ErrorResultTable errorTable)
    {
         JFrame resultDisplayFrame = new JFrame();
        resultDisplayFrame.addWindowListener( new MyWindowAdapter());
        String resultTitle = ServiceUtils.convertServiceType2Name(service) + " - " +
                             ServiceUtils.convertOperationType2Name(service, operation);
        ResultDisplayPane displayPane =  new ResultDisplayPane(resultDisplayFrame, resultTitle,  resultTable, errorTable);
        return displayPane;
    }
    
   /*-------------------------------------------------------------------------------------------------------*
    * Add a new Row in a standatd format to the to the Standard Display table, showing the return result
    * If the first row, create the corresponding TabbedPane, window with two panes
    * If the first row, create the corresponding TabbedPa window with two panes
    * 1) Pane for successful results of the requested service
    * 2) Pane for results of unsuccesful requests
    *--------------------------------------------------------------------------------------------------------*/
    
     protected int  addResultToStandardTable(int testNum, int testId, int serviceType, 
            int operation, JSONObject resultObject)
    {
         int statusCode = ( (Long)resultObject.get("statusCode")).intValue();
         boolean isSuccess = (statusCode == 1);
        int addStatus = 0;
        if (standardResultTable == null)              // first time
        {
            ResultAnalyzer defaultAnalyzer = resultAnalyzer;
            defaultAnalyzer.setImageAnnotationMap(imageAnnotationMap);
            standardResultTable = new StandardResultTable(defaultAnalyzer);
            errorResultTable = new ErrorResultTable(resultAnalyzer);
            
            // Create the display panes in a new Frame 
              JFrame standardDisplayFrame = new JFrame();
              standardDisplayFrame.addWindowListener( new MyWindowAdapter());     
              
              ResultDisplayPane standardResultPane = 
                      new ResultDisplayPane(standardDisplayFrame, "FaceMatch Operation Results",  
                              standardResultTable, errorResultTable);
            //Add the panes
            standardDisplayFrame.setVisible(true);
            activeTable = standardResultTable;
            serviceResultDisplayPane = standardResultPane;
        }
        
         // Add the result to the appropriate table
        if (isSuccess)
         {
             addStatus =activeTable.addRow(testNum, testId, resultObject);
             activeTable.repaint();
         }
         else
        {
             addStatus = errorResultTable.addRow(testNum, testId, resultObject);
             //errorResultTable.repaint();
        }
        return addStatus;           // 1 if row was added
    }

/*----------------------------------------------------------------------------------------------------*/
    public void wrapup()
    {
        serviceResultDisplayPane.showSucessfulResults();
    }
    
    
    /*----------------------------------------------------------------------------------------------------
    * Display result of a client test that failed  due to http error or other reasons in the server
    * For example: If there is no face in a submitted image, it is a failed request
    *------------------------------------------------------------------------------------------------------*/
    public int displayErrorResult(int testId,  String errorStr)
    {
        if (errorResultTable == null)
        {
                errorResultTable = new ErrorResultTable(resultAnalyzer);
        }
        try
        {
             JSONObject errorObject = (JSONObject) jparser.parse(errorStr);
            int status = errorResultTable.addRow(-1, testId, errorObject);
            return status;
        }
        catch (Exception e)
        {
            log.error ("Cannot display results due to configuration error", e);
            return 0;
        }
    }
    
        
 /*-----------------------------------------------------------------------------------------*/   
    protected class MyWindowAdapter extends WindowAdapter
     {
         public void windowClosing(WindowEvent e)
         {
             System.out.println(">>> Window " + e.getWindow().getName() + " closed");
             if (activeTable == ffResultTable)
                 ffResultTableClosed = true;
             else if (activeTable == regIngestResultTable)
                 regIngestResultTableClosed = true;
              else if (activeTable == regQueryResultTable)
                 regQueryResultTableClosed = true;
             else
                  ;         // ignore
           // Check if user wants to exit the application
            confirmAppExit();
         }
          public void windowClosed(WindowEvent e)
         {
             System.out.println(">>> Window " + e.getWindow().getName() + " closed");
            if (activeTable == ffResultTable)
                 ffResultTableClosed = true;
             else if (activeTable == regIngestResultTable)
                 regIngestResultTableClosed = true;
              else if (activeTable == regQueryResultTable)
                 regQueryResultTableClosed = true;
             else
                  ;         // ignore
            // Check if user wants to exit the application
            confirmAppExit();
         }
        
          /*-----------------------------------------------------------------------------------------*/
          // If all windows are closed, simply exit the application.
          // Otherwise, prompt the User to confirm for exit
          protected void confirmAppExit()
          {
              if (regQueryResultTableClosed && regIngestResultTableClosed && ffResultTableClosed)
              {
                  System.out.println("...Exiting Application...\n");
                  System.exit(0);
              }
              else
              {
                   String[] options = {"Exit", "No"};
                    int option = DisplayUtils.displayConfirmationMessage("Do you want to exit the Main Application?", options);
                    if (option == 0)
                    {
                            System.exit(0);
                    }
              }
          }
    }
    /*-----------------------------------------------------------------------------------------*/
    // If the caller wants to wait till the user exits the result table
    /*-----------------------------------------------------------------------------------------*/
    public void waitTillClose()
    {
        while (true)
        {
            try
            {
                Thread.currentThread().sleep(1000);        // wait for a second
                if ( (activeTable == ffResultTable) && ffResultTableClosed)
                    break;
                else  if ( (activeTable == regIngestResultTable) && regIngestResultTableClosed)
                    break;
                else  if ( (activeTable == regQueryResultTable) && regQueryResultTableClosed)
                    break;
            }
            catch (Exception e)         // interruptedException etc
            {
                DisplayUtils.displayWarningMessage("Received interruption Exception: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }
    }
}