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
* FM2TestTool.java
 */
package fm2client.interactive;



import fmservice.httputils.client.ClientRequest;

import fm2client.app.FM2WebClient;
import fm2client.app.FM2RegionBatchApp;
import fm2client.display.DisplayUtils;
import fm2client.util.PropertyLoader;
import fm2client.util.Utils;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;


import java.util.Properties;
import java.util.HashMap;
import org.apache.http.HttpResponse;

import org.apache.log4j.Logger;

/****************************************************************************************
 * This is the main entry point for testing and visually displaying  FaceMatch Operation
 *  results, both in real-time and pre-recorded (stored data) mode.
 * 
 * Revision Log:
 *      2-27-2016  - Initial implementation
 *      3-28-2016  - Updated to accommodate different types of collections
 **********************************************************************************************/
public class FM2TestTool                                                          
{
    private static Logger log = Logger.getLogger(FM2TestTool.class);
    
    public static int HTTP_OKAY = 200;

    Properties configProperties;
    String configFile; 
    
    String fmServerURL;                             // URL for connecting to the FaceMatch2 server 
    String  faceWebServiceURL;               // URL for Web services (vs. interactive requests from the Browser
    String  imageWebServiceURL;           // URL for  Whole image related Web services 
    
    InputSelectionDialog inputSelectionDialog;                          // for parameter selecion
    
    //protected  boolean realtime;
    
     protected HashMap <String, String> selectedParams;

    // Note inputParameters are: 
    //      imageCollection, 
    //      testDataFile, imageStoreDir,
    //      operationType, operationMode, 
    
    protected HashMap <String, String> External2InternalString = new HashMap<String, String>() {
        {
            put("Face Find", "facefind");
            put("Region Ingest", "regioningest");
            put("Region Query", "regionquery");
            put("Real Time", "realTime");
            put("Stored Result", "storedResult");
        }
    };
       
    
    /*----------------------------------------------------------------------------------------------------------------------------*/
    // Constructor
    // @param testProperties - configuration ans other standard/default parameter for the application
    /*----------------------------------------------------------------------------------------------------------------------------*/
    public FM2TestTool(String testConfigFile)  throws Exception
    {
        Properties testProperties = PropertyLoader.loadProperties(testConfigFile);
        if (testProperties == null)
        {
            DisplayUtils.displayErrorMessage("Exiting due to error in loading configuration data.");
            System.exit(-1);
        }
        configFile = testConfigFile;
        configProperties = testProperties;
               
        fmServerURL = testProperties.getProperty("fm2ServerURL");
        if (fmServerURL == null)
        {
              DisplayUtils.displayErrorMessage(
                  "No FaceMatch2 server URL provided in the configuration file as \"fmServerURL\", Application exiting .");
              System.exit(-1);
        }
        configProperties = testProperties;
        faceWebServiceURL = fmServerURL+"/ws";           // for face related operations
        imageWebServiceURL = fmServerURL+"/ws-image";    // for whole image related operations
    }
 /*-------------------------------------------------------------------------------------------------------------------*/
    // Start up the TestTool operations by displaing the input pane;
    // to receive user choices and then processing the requests
    /*-------------------------------------------------------------------------------------------------------------------*/
    public  void  start(String[] args ) throws Exception
    {
        String log4jFileName =  Utils.initLogging(configProperties);
        System.out.println("*** Writing results to Log file: " + log4jFileName);
        
        while (true)
        {
                try
                {
                    int status = receiveNProcessRequest();
                    if (status <= 0)
                         exitTestTool();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    break;
                }    
        }
    }
    
    /*---------------------------------------------------------------------------------------------------------*/
    // Perform the following finctions
    /*       1) display the InputSelection panel  Dialog in a separate thread (new Runnable)
    //       2) wait till the Dialog closes and input is available (.invokeAndWait)
    //       3) proceed according to the user choices
    /*---------------------------------------------------------------------------------------------------------*/
    protected int  receiveNProcessRequest()  throws Exception
    {
        SwingUtilities.invokeAndWait(new Runnable() {
          public void run() {
               inputSelectionDialog = createInputSelectionDialog();
               inputSelectionDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
               inputSelectionDialog.addWindowListener(new MyWindowAdapter());

               // inputSelectionDialog.pack();
               inputSelectionDialog.toFront();
               inputSelectionDialog.start();
          }
        });
        
         //-------------------------------------------------------------------------------------------------------------
        // We reach here and  get the selected params only after the Dialogbox closes
        //-------------------------------------------------------------------------------------------------------------
         selectedParams  =  inputSelectionDialog.getSelectedParams();
         System.out.println("\n>>> Selected params: " + selectedParams);
        
        if (selectedParams == null)
        {
             System.out.println("\n>>> User canceled operation");
             return 0;
        }
        // Perform the tests according to user selected options
        return performFM2Test(selectedParams);
    }    

    /**---------------------------------------------------------------------------------------------*
     * Exit the Application, through window closing
     */
    private void exitForm(WindowEvent evt)
    {
        String[] choices = {"Exit", "Cancel"};
       int option = DisplayUtils.displayConfirmationMessage("Do you want to exit? ", choices);
        if (option == 0)
        {
            exitTestTool();
        }
    }
    /*---------------------------------------------------------------------------------------------*/
     public void exitTestTool()
    {
        try
        {
            System.out.println("Exiting FaceMatch2Test Application.");
            System.exit(0);
        } catch (Exception e)
        {
            System.out.println("FaceMatch2Test logout failed.");
            System.exit(-1);
        }
        System.exit(0);
    }


    /**
     * Uses preferred size of application to calculate location for centering.
     */
    private void setFrameCenter(JFrame frame, Dimension dim)
    {
        Dimension screenSize = frame.getToolkit().getScreenSize();
        int x = (screenSize.width / 2) - (dim.width / 2);
        int y = (screenSize.height / 2) - (dim.height / 2);
        frame.setLocation(x, y);
    }
    
    //*-------------------------------------------------------------------------------------------------------------
    // Create the input Selection panel.
    // Check for parameters specified in the config file
    // and set as initial parameters.  Provide defaults if not specified
    //*---------------------------------------------------------------------------------------------------------------
     protected  InputSelectionDialog  createInputSelectionDialog()
     {
         String fileSystemRoot = Utils.getFileSystemRoot();
         HashMap <String, String> initialParams = new HashMap();
       
         String testDataFile =  configProperties.getProperty("fm2test.datadir", fileSystemRoot);
         initialParams.put("testDataFile", testDataFile);
             
         String resultStoreDir = configProperties.getProperty("fm2test.resultstore.dir", fileSystemRoot);
         initialParams.put("resultStoreDir", resultStoreDir);
         
         String clientName = configProperties.getProperty("fm2test.clients", "");
         initialParams.put("client", clientName);
         
         String collectionNames =  configProperties.getProperty("fm2test.imageCollectionList", "");
         initialParams.put("imageCollectionList", collectionNames);
         
         String selectedCollection =  configProperties.getProperty("fm2test.imageCollection", "");
         initialParams.put("selectedCollection", selectedCollection);
         
         String operationMode =  configProperties.getProperty("fm2test.operationMode", "Stored Results");
         initialParams.put("operationMode", operationMode);         // realtime or stored data
         
         String operationType =  configProperties.getProperty("operationType", "Face Find");
         initialParams.put("operationType", operationType); 
         
        JFrame dialogFrame = new JFrame("Input Selection Window");
        inputSelectionDialog= new InputSelectionDialog(dialogFrame, initialParams);
        inputSelectionDialog.setBackground(Color.WHITE);
        
       setFrameCenter(dialogFrame, dialogFrame.getPreferredSize());
        return inputSelectionDialog;
    }
 
    
 /* ***************************************************************************/
    // Handover operations to the RegionTest manager with user provided parameters
    protected int  performFM2Test( HashMap<String, String> selectedParams)
    {
        String clientName = selectedParams.get("client");
        String imageCollection = selectedParams.get("imageCollection");
        String operationType = selectedParams.get("operationType");      // facefind, ingest, query
        String  testDataFile = selectedParams.get("testDataFile");
        String resultStoreDir = selectedParams.get("resultStoreDir");
         
        boolean realtime = (selectedParams.get("operationMode").equalsIgnoreCase("Real Time"));
        if (realtime)
        {
            // contact the remote server to make sure that it is running= 
            boolean available = isServerAvailable();
            if (!available)
            {
                DisplayUtils.displayErrorMessage("Could not establish session with the FaceMatch2 Server, Exiting.");
                return 0;
            } 
        }
        String fm2operationType = External2InternalString.get(operationType);
        try
        {
            // iconify self
           // inputSelectionFrame.setState(JFrame.ICONIFIED);
            FM2RegionBatchApp regionTestApp = new FM2RegionBatchApp(configProperties, realtime);
            regionTestApp.setImageCollection(imageCollection);
            regionTestApp.setClientName(clientName);
            
            boolean displayResults = true;              //default  set to true;
            // TBD: get from selected params
            regionTestApp.performFacematchTests(fm2operationType, testDataFile, displayResults, resultStoreDir, true);
            regionTestApp.waitTillClose();
            System.out.println("Result Display closed by User");
            
            // if we return from the operation
            //inputSelectionFrame.setState(JFrame.NORMAL);
        }
        catch (Exception e)
        {
            DisplayUtils.displayErrorMessage("Exception in Performing FM2RegionTests: " + e.getMessage());
            log.error("Exception in Performing FM2RegionTests: ", e);
            return -1;
        }
        return 1;
    }
       
    /**----------------------------------------------------------------------------------------------------------
    // Make sure that the server is running on the at the given location in the URL
   //------------------------------------------------------------------------------------------------------------*/
    protected boolean isServerAvailable()
    {
        HttpResponse serverResponse = ClientRequest.sendGetRequest(fmServerURL, "status", null);
        if (serverResponse == null)
        {
             log.error ("Could not connect to the FaceMatch Server " + fmServerURL );
             return false;
        }
        int statusCode = serverResponse.getStatusLine().getStatusCode();
        boolean isSuccess =  (statusCode == HTTP_OKAY);
        if (!isSuccess)
        {
           String statusMessage = serverResponse.getStatusLine().getReasonPhrase();
           log.error ("Error returned from FaceMatch Server " + fmServerURL +
               "\n Status code = " + statusCode +", " + statusMessage );
       }
        return isSuccess;
    }

    
     /* ***************************************************************************/
    // Handover operations to the RegionTest manager with user provided parameters
    // not used 
    protected void performLocalFM2Test( )
    {
        String imageCollection = "colorFERET";   //selectedParams.get("imageCollection");
        String fm2operationType = "facefind";   //selectedParams.get("operationType");      // facefind, ingest, query
        String  testDataFile = "C:/DevWork/FaceMatch2/FM2testsets/testdata/FM2Research/colorferet/facefind/ColorFeretFaceFinderTest.1.json";     //selectedParams.get("testDataFile");
        String resultStoreDir =  "C:/DevWork/FaceMatch2/FM2testsets/result/FM2Research/colorferet/facefind/colorferet_save";     //selectedParams.get("resultStoreDir");
        boolean realtime = false;                                            
                        
        try
        {
            // iconify self
           // inputSelectionFrame.setState(JFrame.ICONIFIED);
            FM2RegionBatchApp regionTestApp = new FM2RegionBatchApp(configFile, realtime);
            regionTestApp.setImageCollection(imageCollection);
            
            boolean displayResults = true;              //default  set to true;
            // TBD: get from selected params
            regionTestApp.performFacematchTests(fm2operationType, testDataFile, displayResults, resultStoreDir, true);
            
            // if we return from the operation
            //inputSelectionFrame.setState(JFrame.NORMAL);
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
 /*-----------------------------------------------------------------------------------------*/   
    protected class MyWindowAdapter extends WindowAdapter
     {
         public void windowClosing(WindowEvent e) {
         exitForm(e);
         }
    }

    /*-----------------------------------------------------------------------------------------
    * Process Window closing of spawned wiindows
    *--------------------------------------------------------------------------------------------*/
    /**
     * **********************************************************************
     */
    /**
     * @param args the command line arguments
     */
    public static void main(String args[])
    {
          String configFile;
          
        if (args.length < 1)
        {
            configFile = FM2WebClient.getDefaultConfigFile();
        }          
        else
             configFile = args[0];
        
        if (configFile == null)
        {
             System.out.println("Please specify a Configuration file name for FaceMatch2 client");
            System.exit(-1);
        }


           // Start the Result Analyzer
        try
        {
           FM2TestTool fm2testTool = new FM2TestTool(configFile);
           fm2testTool.start(args);
           // do not exit
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
