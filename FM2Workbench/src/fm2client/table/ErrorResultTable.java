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
*	ErrorResultTable
*/
package fm2client.table;

import fm2client.analyzer.ResultAnalyzer;
import fm2client.display.DisplayUtils;
import fm2client.display.ImageDisplays;

import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.ServiceUtils;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.util.HashMap;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import org.apache.log4j.Logger;

import org.json.simple.JSONObject;



/********************************************************************************************
* Display FaceFind results, returned by the Server to the client, as a Table.
*
* Add New Rows to the table as data is added.
* Rows are not editable, but selectable.
* When a row is selected, the image with the face and its coordinates are displayed 
* in a new frame.
*
*-*****************************************************************************************/

public class ErrorResultTable extends  ResultTable implements ServiceConstants
{

    private static Logger log = Logger.getLogger(ErrorResultTable.class);

    /*String errorType;
    int testId;
    String service;
    String operation;
    String url;
    String imageExtent;
    String imageTag;
    boolean gpuUsed;
    int errorCode;
    String errorMessage;
    double faceFindTime;
    String  imageRotated;
    double ingestTime;
    double queryTime;*/
    
    protected ResultAnalyzer resultAnalyzer;
     int currentRow;
    public static String[] buttons = {"Display Selected Row Details"}; 

/*---------------------------------------------------------------------------------------*/

    public  ErrorResultTable (ResultAnalyzer analyzer)
    {
         super ( ERROR_DISPLAY, ERROR_TABLE_NAME,  ErrorResultColumnNames, 
                 null,  buttons);
         resultAnalyzer = analyzer;
         setTableColor(new Color(0xF0DDE1));
         currentRow = -1;
        
        //----------------------------------------------------------------------------------------------------------
        // add a listner to know when a row is selected
        //----------------------------------------------------------------------------------------------------------------
        myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent event) {
                if (myTable.getSelectedRow() > -1) 
                {
                    int selectedRow = myTable.getSelectedRow();
                    if (selectedRow == currentRow)
                       return;
                    currentRow = selectedRow;
                    int testId = ((Integer) (myTable.getModel().getValueAt(selectedRow, 2))).intValue();
                    drawSelectedImage(testId, (String)(myTable.getModel().getValueAt(selectedRow, ErrorImageCol)) );     
                }
            }
        });
        
         this.setVisible(false);
    }

//----------------------------------------------------------------------------------------------------------------  

    public int addRow( int testNum, int testId, JSONObject testResult)
    {
        int displayStatus = displayErrorRecord( testId, testResult);
        return displayStatus ;
    }

    /*--------------------------------------------------------------------------------
     * display a new Row in the table
    * using data in the input JSON object
    *--------------------------------------------------------------------------------*/

   protected int displayErrorRecord(int testId, JSONObject errorResult)
    {       
        String errorType;

        String service;
        String operation;
        String imageID;
        String imageExtent;
        String  imageRotated;
        int errorCode;
        String errorMessage;
        double faceFindTime;
        
        JTable table = myTable;
        DefaultTableModel model= (DefaultTableModel)table.getModel();
        try 
        {
            int svc =  ((Long)errorResult.get(SERVICE)).intValue();
            service = ServiceUtils.convertServiceType2Name(svc);
            int oper = ((Long)errorResult.get(OPERATION)).intValue();
            operation = ServiceUtils.convertOperationType2Name(svc, oper);
            errorCode = ((Long)errorResult.get(STATUS_CODE)).intValue();
            errorMessage = (String)errorResult.get(STATUS_MESSAGE);

             if (errorResult.get(FACEFIND_TIME) != null)
                 faceFindTime = ((Double)errorResult.get(FACEFIND_TIME)).doubleValue();
             else
                 faceFindTime = -1; 


              imageRotated = "";      
               imageExtent= "";
             if (oper == ServiceConstants.GET_FACES_OP)
             {
                 imageID= (String)errorResult.get(URL);
                  if (errorResult.get(IMAGE_ROTATED) != null &&  ((Boolean)errorResult.get(IMAGE_ROTATED)) == true)
                      imageRotated = "true";
             }              
            else
            {
                 if (errorResult.get(EXTENT) != null)
                    imageExtent= (String)errorResult.get(EXTENT);
                 
                 if (errorResult.get(URL) != null)
                      imageID = (String)errorResult.get(URL);     // for ingest
                 else if (errorResult.get(IMAGE_TAG) != null)
                      imageID = (String)errorResult.get(IMAGE_TAG);     // for ingest
                 else if (errorResult.get(QUERY_URL) != null)
                       imageID= (String)errorResult.get(QUERY_URL);        // for query
                  else
                      imageID = "NULL";
            }

            Object[] data= { service, operation, testId, imageExtent, imageID, faceFindTime, errorCode, errorMessage};
            model.addRow(data);

        } // end try
        catch(Exception e)
        {
            log.error("Could not display Error data  for test ID " + testId, e);
            return 0;
        } // end catch    
        
        return 1;
    } 

   
 /*----------------------------------------------------------------------------------------------------------------------------------*/
 // Processing of Requests related to FaceFinding
 /*----------------------------------------------------------------------------------------------------------------------------------*/
    protected void drawSelectedImage(int testId, String imageURL)
    {
        if (imageURL == null || imageURL.equalsIgnoreCase("NULL"))
            return;
         JTable table = myTable;
        DefaultTableModel model= (DefaultTableModel)table.getModel();
         String localImagePath  = resultAnalyzer.getLocalImageFilePath(imageURL);
        ImageDisplays.displayImageInFrame("", testId, localImagePath, null, true);
        return; 
    }
    
    
    /*------------------------------------------------------------------------------------------------*/
    // User selected an action button. Call the analyzer to process the 
    // corresponding request.
    /*------------------------------------------------------------------------------------------------*/
    protected void userActionPerformed(ActionEvent evt)
    {
        JButton selectedButton = (JButton) evt.getSource();     // currently only a single button

        int selectedRow = myTable.getSelectedRow();
        if (selectedRow < 0)
        {
            myTable.setRowSelectionInterval(0,0);
            selectedRow = 0;
        }
           
        showSelectedRowDetails(selectedRow);
        /*------------------------------------------------------------------------------------------------*/
    }
    
/*----------------------------------------------------------------------------------------------------------------------------------*/
 // Processing of Requests related to FaceFinding
 /*----------------------------------------------------------------------------------------------------------------------------------*/
    protected void showSelectedRowDetails(int row)
    {
        JTable table = myTable;
        DefaultTableModel model= (DefaultTableModel)table.getModel();
        try 
        {
            //service, operation, testId, imageExtent, url, faceFindTime, errorCode, errorMessage
            String operation =  (String) model.getValueAt(row, 1);
            int testId = ((Integer) model.getValueAt( row, 2)).intValue();
            String imageExtent = (String) model.getValueAt(row, 3);
            String image = (String) model.getValueAt(row, 4);
            Double faceFindTimeMsec =  (Double) model.getValueAt(row, 5);

            Integer statusCode =  (Integer) model.getValueAt(row, 6);
            String statusMsg = (String) model.getValueAt(row, 7);
            String details = 
                    "Operation: " + operation + "\n" +
                    "TestID:  " + testId +"\n" +
                    "ExtentName: " + imageExtent + "\n" +
                    "ImageURL: " + image +"\n" +
                    "FaceFind Time in Msec : "+ faceFindTimeMsec  +"\n"+
                    "Status Code: " + statusCode +"\n" +
                    "Status Message: " + statusMsg;
            DisplayUtils.displayInfoMessage(details);
            return; 
        }
        catch (Exception e)
        {
            DisplayUtils.displayErrorMessage("Error: " + e.getMessage());
        }
    }
} //end class

