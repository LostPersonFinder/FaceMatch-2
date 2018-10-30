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
 *	ResultDisplay.java
 */
package fm2client.table;


import fm2client.analyzer.ResultAnalyzer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.json.simple.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.util.HashMap;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import org.apache.log4j.Logger;


/********************************************************************************************
* Display FaceFind results, returned by the Server to the client, as a Table.
*
* Add New Rows to the table as data is added.
* Rows are not editable, but selectable.
* When a row is selected, the image with the face and its coordinates are displayed 
* in a new frame.
*
*-*****************************************************************************************/

public class FaceFindResultTable extends ResultTable 
{

    private static Logger log = Logger.getLogger(FaceFindResultTable.class);
    
    public HashMap noFaceURLMap;
    
    
    /*---------------------------------------------------------------------------------------*/
    // implementation of abstract method
     public int getOperationType() { return FACEFIND_OPERATION;}

/*---------------------------------------------------------------------------------------*/

    public  FaceFindResultTable (ResultAnalyzer analyzer) 
    {
        super( FACEFIND_OPERATION, FF_TABLE_NAME, GetFacesColumnNames, analyzer, null);
        setTableColor(new Color(0xF6F9D9));;
        noFaceURLMap =  new HashMap();
        
        //----------------------------------------------------------------------------------------------------------
        // add a listner to know when a row is selected
        // Enable/Disable the Annotation Display button accordingly
        //----------------------------------------------------------------------------------------------------------------
        myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent event) {
                if (myTable.getSelectedRow() > -1) 
                {
                    // enable/disable the Annotation drawing button
                    int selectedRow = myTable.getSelectedRow();
                    String imageURL = (String)myTable.getValueAt(selectedRow, FFImageNameCol);
                    boolean annot = analyzer.isAnnotationAvailable(imageURL);
                    actionButtons[FF_ANNOT_BUTTON].setEnabled(annot);
                }
            }
        });
    }

    
     public  FaceFindResultTable (JPanel parentPanel, String tableName) 
    {
        super(parentPanel,  tableName, GetFacesColumnNames);
        setTableColor(new Color(0xF6F9D9));;
        noFaceURLMap =  new HashMap();
        this.setVisible(true);
    }

    public int addRow(int testNum, int testId, JSONObject testResult)
    {
        int displayStatus = displayFaceFinderRecord(testNum, testId, testResult);
        return displayStatus ;
    }


    /*--------------------------------------------------------------------------------
     * display a new Row in the table
    * using data in the input JSON object
    *--------------------------------------------------------------------------------*/

    public int displayFaceFinderRecord(int testNum, int testId, JSONObject testResult)
    {         
        JTable table = myTable;
        DefaultTableModel model= (DefaultTableModel)table.getModel();
        try 
        {
            String imageURL = (String) testResult.get("url");
            //String imageTag = (String) testResult.get("imageTag");
            
            String coordinates = "";
            JSONArray arr =   (JSONArray) testResult.get("faceRegions");
            int numFaces = (arr == null) ? 0 : arr.size();
            if (numFaces > 0)
            {
                for (int j=0; j< arr.size(); j++)
                {
                    coordinates += (String) arr.get(j);
                    if (j < (arr.size()-1)) 
                        coordinates += "  ";
                }
            }
            
            float serviceTime = ((Double) testResult.get("serviceTimeMsec")).floatValue();
            float faceFindTime = ((Double) testResult.get("faceFindTimeMsec")).floatValue();
            boolean gpuUsed = ((Boolean)testResult.get("gpuUsed")).booleanValue();
            String perfType= ((String)testResult.get("performance"));

            Object[] data= {testNum, testId, imageURL, numFaces, coordinates, perfType,
                faceFindTime, serviceTime, gpuUsed};	
            model.addRow(data);
            /* for testing only 
            System.out.println("Number of rows: " + model.getRowCount());
           */
           // Thread.currentThread().sleep(100);
            
        } // end try
        catch(Exception e)
        {
            log.error("Could not display FaceFind result for test number " + testNum, e);
            return 0;
        } // end catch    
        
       int rowCount = model.getRowCount();
        if  (rowCount >= 1)
        {
             resultTable.setVisible(true);                      // self, the JDialog
            // resultTable.requestFocus();
        }
      
        // make the last table rows visible
        scrollToVisible(rowCount);
        return 1;
    } 
    
    public HashMap getURLsWithNoFaces()
    {
        return noFaceURLMap;
    }

} //end class

