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
* Perform a FaceMatch2 test scenario
*
*/

package fm2client.app;

import fm2client.display.DisplayManager;
import  fm2client.core.FM2ServiceResult;

import fm2client.testgen.FMTestSetBuilder;      //FMTestSetBuilder;
import fm2client.testgen.TestResultFile;        // file to save results from server

import fm2client.analyzer.ResultAnalyzer;
import fm2client.core.FM2ServiceRequestor;
import fm2client.display.DisplayUtils;
import fm2client.util.Utils;

import fmservice.httputils.common.ServiceConstants;

import org.json.simple.JSONObject;

import org.json.simple.parser.JSONParser;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


import java.io.File;
import java.util.Properties;

import org.apache.log4j.Logger;


public class FM2ScenarioTestApp extends FM2WebClient implements ServiceConstants
{
    private static Logger log = Logger.getLogger(FM2ScenarioTestApp.class);

    JSONObject fmCommTemplate;
    JSONObject inputData;
    JSONParser jparser;
        
    FMTestSetBuilder  fmTestSetBuilder;
    DisplayManager displayManager;
    
    LinkedHashMap<String, String> imageTag2urlMap = null;
    LinkedHashMap<String, String> imageAnnotationMap = null;
    
    LinkedHashMap <Integer, String> urlsWithNoFaces;          // no detected faces
    LinkedHashMap <Integer, String> failedResultMap;
    LinkedHashMap <Integer, String> httpErrorMap;
    
 
    protected boolean debug = true;
    
    // Default is to display results from store data
    protected boolean saveResultsToFile = false;
    protected TestResultFile storedResultFile = null;
        
    protected static String testType = "scenario";

   int MAXNUM = 20000;          // max entries to test
    int numIngest = 0;
    
 public  FM2ScenarioTestApp(Properties opsProperties, boolean realtime) throws Exception
    {
        super(opsProperties, realtime);
        
        initApp(realtime);
    }

/*----------------------------------------------------------------------------------------------*/
    public FM2ScenarioTestApp (String testConfigFile,  boolean realtime)  throws Exception
     {
         super(testConfigFile, realtime);
         initApp(realtime);
    }
        
    protected void initApp(boolean realtime)
    {
        boolean realtimeMode = realtime;            // realtime operations or display from stored files
        jparser = new JSONParser(); 
       
         // testProprties generated by Parent class
        fmTestSetBuilder = new FMTestSetBuilder(testProperties);   
        
        if (realtime && !validConnection)
        {
            DisplayUtils.displayErrorMessage("Cannot establish connection with the FM2 Web Server " + fm2ServerURL);
        }
    }

    /*-------------------------------------------------------------------------------------------------------*/
    // Main entry for FaceMatchRegion Operations from external callers
    // Perform the specific FaceMatch tests using user supplied parameters
    // Note:  storedResultFileName is relarive to the repository root
    /*-------------------------------------------------------------------------------------------------------*/
    
    public void performScenarioTest( String fmTestFileName, String storedResultPath, boolean realtimeMode)
    {
        saveResultsToFile = (storedResultPath != null);
        
        // create the collection-specific Result Analyzer and DisplayManager
        // If no specific collection, we use the default Analyzer with the DisplayManager
        // Note that results are always displayed if you are using stored data
 
        if (realtimeMode)
        {
             if ( !validConnection)             // error message already out
                return;
            // connect to the server and perform tests using data from the test file
            doRealtimeScenarioTest( fmTestFileName, storedResultPath, saveResultsToFile);
        }
        else   
       // Display the results from previously stored data
        {
           ; //displayStoredResults( fmTestFileName, storedResultPath);
        }       // end display stored results
    }

 
   /******************************************************************************************************/
    // Perform FaceMatch tests in realtime by sendingrequests to the FM2WebServer
   // @param savResultDir  - name of directory  where test results returned by the server will be saved
   //
   //*******************************************************************************************************/
    protected void doRealtimeScenarioTest(
        String fmTestFileName, String storedResultPath, boolean saveResults)
    {
        if (fmTestFileName == null || fmTestFileName.isEmpty())
        {
            log.error("FaceMatch test file name  not provided");
            return;
        }
        
       /*-----------------------------------------------------------------------------------------------------
        * RegionMatching
        /*-----------------------------------------------------------------------------------------------------*/
        // Check if it is full filename or relative to the path in the config file
        String scenarioTestFile = "";
        File testFile = new File(fmTestFileName);           // relative path of test file
        if (testFile.exists())          // a fully qualified fiename
            scenarioTestFile = fmTestFileName;
        else
        {
            String testdataDir = testProperties.getProperty("fm2test.datadir");
             scenarioTestFile = testdataDir+"/"+fmTestFileName;
        }
        log.info ("\n>>> Performing test  using data from " + scenarioTestFile);
        
        // if results are to be saved, build that file name from thetest file name
        saveResultsToFile = saveResults;
        String savedResultFileSpec = "" ;
        if (saveResultsToFile)
        {
            savedResultFileSpec = Utils.buildStoredResultFileName(testProperties,
                testType, fmTestFileName, storedResultPath);    
            // open the file to store the server returned results
           storedResultFile = new TestResultFile(savedResultFileSpec, true);
        }
       
        boolean displayResults = false;         // currently no GUI test
        ArrayList<HashMap> fmtestDataSet = fmTestSetBuilder.getTestParams(scenarioTestFile);
        if (fmtestDataSet == null)
        {
            log.error("No  test  dataset for FaceMatch testing found in test file.");
            return;
        }

           if (displayResults)
        {
              ResultAnalyzer resultAnalyzer = ResultAnalyzer.getAnalyzer(testProperties,"");
              displayManager = new DisplayManager( resultAnalyzer);     
        }

          performScenarioTest(fmtestDataSet);

        if (saveResults && storedResultFile.isFileOpened())  
        {
          storedResultFile.closeFile();
          System.out.println("Test Results stored in file: " + savedResultFileSpec);
        }
    }  

    /*****************************************************************************************
     * Perform the set of tests for Face matching  operation as specified in the input dataset
     * with random service requests simulating an operational environment
     * ****************************************************************************************/
    protected void  performScenarioTest(ArrayList<HashMap>testDataSet)
    {
        System.out.println("-----------------------------------------------");
        FM2ServiceRequestor  serviceRequestor = new FM2ServiceRequestor(fm2ServerURL);
        int i = 0;
        for (HashMap testParams : testDataSet)
        {
            String serviceName = (String) testParams.get("service");
            if (serviceName == null)
            {
                log.error("Missing  Service name in request.");
                continue;
            }
            try
            {
                 FM2ServiceResult serviceResult  = null;
                 if (testParams.get("service").equals(ServiceConstants.ADMIN))
                {
                    serviceResult = 
                             serviceRequestor.requestAdminService(i, testParams);
                 }
                 else if (testParams.get("service").equals(ServiceConstants.EXTENT))
                {
                    serviceResult = 
                             serviceRequestor.requestExtentOpsService(i, testParams);
                 }
                 else  if (serviceName.equals(ServiceConstants.FACE_FIND))
                {
                     serviceResult = serviceRequestor.requestFaceFinderService(++i, testParams);
                } 
                else if (serviceName.equals(ServiceConstants.FACE_MATCH_REGION))
                {
                     serviceResult = serviceRequestor.requestFaceMatchRegionService(++i, testParams);
                } 
                else
                {
                    log.error("Ignoring unknown Service name: " + serviceName);
                }
                //  store in file if requested
                 if (saveResultsToFile)
                     storedResultFile.writeRecord("\n---- Test Number: " + i + serviceResult.serverReponseContent);                  
            }  
            catch (Exception e)
            {
                log.error("Excepion in performing Extent operation", e);
            }
       }    // end for
        
    }   

    

    /*-------------------------------------------------------------------------------
     * Write the results of the given test to the specified file as a JSON record
     * which may be retrieved and displayed later
     *------------------------------------------------------------------------------*
     protected int writeResultRecord(int testNum, int testId, int serviceType, int operation, 
         String serviceResult)
     {
        JSONObject  resObj = new JSONObject();
        resObj.put("testNumber", testNum);
        resObj.put("testId", testId);
        resObj.put(ServiceConstants.SERVICE, serviceType);
        resObj.put(ServiceConstants.OPERATION, operation);
        resObj.put("serviceResult", serviceResult);
        int status = storedResultFile.writeRecord(resObj.toJSONString());
        return status;  
     }
    */
 


/********************************************************************************************/
/** Perform remote testing of the FaceMatch Web Server using the standard FMConfiguration file
 * and appropriate test data sets.
 * @param args  - specified various options and tests to be performed
 *              realtime/stored result,  operation, client name, collection name, testFile,  display/nodisplay
 *********************************************************************************************/      
    public  static void main(String[] args)
    {
         String   testConfigFile = FM2WebClient.getDefaultConfigFile();
        
        int numArgs = args.length;
        if (numArgs < 3)
        {
            System.out.println(" Number of Arguments should be 3 or more");
            System.exit(-1);
        }
         // display data from previous tests, no need to talk to the server 
         boolean realtimeMode = args[0].equalsIgnoreCase("realtime");
         String testFile = args[1];              // relative path name
         String storedResultPath = args[2];         // relative path wt the repository  result root
         boolean saveResult = (storedResultPath != "NULL");
         String imageTagFilename = null;
         boolean displayResults = false;
         if (numArgs >= 4)
                  displayResults = args[3].equals ("display");
          if (numArgs >= 5)   
             imageTagFilename = args[4];
         

        try
        {
            FM2ScenarioTestApp  scenarioTest= new FM2ScenarioTestApp(testConfigFile, realtimeMode); 
            log.info("----------------------------------------------------------------------------------" +
              "\n Performing FaceMatch Scenario tests for  Test Application using " +
              "\n Configuration file: " + testConfigFile + ", Test data file " + testFile + 
              "\n-------------------------------------------------------------------------------");

            String storedResultFileName = saveResult ? storedResultPath : null;
             scenarioTest.performScenarioTest(testFile, storedResultFileName, true);
        }
        catch (Exception e)
        {
                log.error("Error while activating/running FMClientApp, error: " + e.getMessage() + ". Exiting.");
                e.printStackTrace();
                System.exit(-1);
        }
        // do not exit here as then the displays  vanish
    }
}
