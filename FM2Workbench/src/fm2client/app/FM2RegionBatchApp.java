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
* Test for a Remote client that sends HTTP GET Requests for Region related operations to the FM2 server,
* receives the response and displays it.
* The requests are received in a batch (as for batch ingest), and sent to the Server individually in a loop,
* optionally displaying the returned FM results visually in a table form
*
* Note: Terms ImageCollection and ImageExtents are used here interchangably for ease of understanding.
*/

package fm2client.app;

import fm2client.display.DisplayManager;
import fm2client.core.FM2ServiceRequestor;
import  fm2client.core.FM2ServiceResult;

import fm2client.testgen.FMTestSetBuilder;      //FMTestSetBuilder;
import fm2client.testgen.TestResultFile;        // file to save results from server

import fm2client.analyzer.ResultAnalyzer;
import fm2client.display.DisplayUtils;
import fm2client.util.Utils;

import fmservice.httputils.common.ServiceConstants;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

import org.apache.http.StatusLine;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.File;

import org.apache.log4j.Logger;


public class FM2RegionBatchApp extends FM2WebClient implements ServiceConstants
{
    private static Logger log = Logger.getLogger(FM2RegionBatchApp.class);
    
     
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
    protected boolean realtimeMode = false;
    protected boolean displayResults = true;
    protected boolean saveResultsToFile = false;
    protected TestResultFile storedResultFile = null;
    protected  FM2ServiceRequestor  serviceRequestor;
        
   String imageCollection= "";                         // for collection-specific displayes, needed only for displayResult
   String clientName = "";
   ResultAnalyzer resultAnalyzer = null;        // for analyzing FaceMatch2 server result resules

   int MAXNUM = 20000;          // default max entries to test
   int testSetSize = 0;
   
    int numIngest = 0;
    
 public  FM2RegionBatchApp(Properties opsProperties, boolean realtime) throws Exception
    {
        super(opsProperties, realtime);
        
        initApp(realtime);
    }

/*----------------------------------------------------------------------------------------------*/
    
    public FM2RegionBatchApp (String testConfigFile,  boolean realtime)  throws Exception
     {
         super(testConfigFile, realtime);
         initApp(realtime);
    }
   
    /*----------------------------------------------------------------------------------------------*/
    protected void initApp(boolean realtime)
{
         realtimeMode = realtime;            // realtime operations or display from stored files

       // Initialize the GUI objects for displaying test results - ues standard analyzer
        imageCollection = "";           // default is no specific one, should be reset explicitly by caller
    
        jparser = new JSONParser(); 
        urlsWithNoFaces = new LinkedHashMap();
        
        httpErrorMap = new LinkedHashMap();
        failedResultMap = new LinkedHashMap();
        
         // testProprties generated by Parent class
        fmTestSetBuilder = new FMTestSetBuilder(testProperties);   
        
        if (realtime && !validConnection)
        {
            DisplayUtils.displayErrorMessage("Cannot establish connection with the FM2 Web Server " + fm2ServerURL);
        }
        if (realtime)
            serviceRequestor  = new FM2ServiceRequestor(fm2ServerURL);
    }

    //----------------------------------------------------------------------------------------------------------
    // Image Collection name is needed to perform collection-specific displays
    // for face finding and queries, using a collection-specific analyzer such as
    // the ColorFERET collection
    //--------------------------------------------------------------------------------------------------------
    public  void setImageCollection(String collectionName)
    {
            imageCollection = collectionName;
            resultAnalyzer = ResultAnalyzer.getAnalyzer(testProperties, imageCollection);
            System.out.println("--- UsingImageAnalyzer "+ resultAnalyzer.getClass().getName());
    }
    
      public  void setClientName(String client)
    {
           clientName = client;
    }
//--------------------------------------------------------------------------------------------------------
    public String getImageCollection()
    {
          return   imageCollection;
    }
    /*-------------------------------------------------------------------------------------------------------*/
    // Main entry for FaceMatchRegion Operations from external callers
    // Perform the specific FaceMatch tests using user supplied parameters
    /*-------------------------------------------------------------------------------------------------------*/
    
    public void performFacematchTests(String testType, String fmTestFileName, 
       boolean displayServiceResults,  String storedResultPath, boolean saveResults)
    {
        displayResults = displayServiceResults;
        saveResultsToFile = saveResults;
        
        // create the collection-specific Result Analyzer and DisplayManager
        // If no specific collection, we use the default Analyzer with the DisplayManager
        // Note that results are always displayed if you are using stored data
 
        if (realtimeMode)
        {
             if ( !validConnection)             // error message already out
                return;
            // connect to the server and perform tests using data from the test file
            doFaceMatchRealtimeTests( testType, fmTestFileName,  storedResultPath,  saveResults);
        }
        else   
       // Display the results from previously stored data
        {
           displayFaceMatchStoredResults( testType,  fmTestFileName, storedResultPath);
        }       // end display stored results
    }

    /*----------------------------------------------------------------------------------------------------------------------*/
    // Read the ImageAnnotation file if available for FaceFinding result analysis
    /*----------------------------------------------------------------------------------------------------------------------*/
    protected void setAnalysisMaps( String testType)
{
        // read the imageAnnotation map if available
        String  imageAnnotFileName = getImageAnnotationFile(imageCollection);
        if (imageAnnotFileName != null)
        {
            imageAnnotationMap = readImageAnnotationMap(imageAnnotFileName);
            displayManager.setImageAnnotationMap(imageAnnotationMap);
        }
        else 
            displayManager.setImageAnnotationMap(null);         // no annotation available
    }
   /******************************************************************************************************/
    // Perform FaceMatch tests in realtime by sendingrequests to the FM2WebServer
   // @param savResultDir  - name of directory  where test results returned by the server will be saved
   //
   //*******************************************************************************************************/
    protected void doFaceMatchRealtimeTests(String testType, 
        String fmTestFileName, String storedResultDir, boolean saveResults)
    {
        
        if (fmTestFileName == null || fmTestFileName.isEmpty())
        {
            log.error("FaceMatch test file name  not provided");
            return;
        }
           if (testType.matches("fullquery"))
         {
             performFullQueryTest(fmTestFileName);
             return;
         }
       /*-----------------------------------------------------------------------------------------------------
        * RegionMatching
        /*-----------------------------------------------------------------------------------------------------*/
        // Check if it is full filename or relative to the path in the config file
        String faceMatchTestFile = "";
        File testFile = new File(fmTestFileName);
        if (testFile.exists())          // a fully qualified fiename
            faceMatchTestFile = fmTestFileName;
        else
        {
            String testdataDir = testProperties.getProperty("fm2test.datadir");
             faceMatchTestFile = testdataDir+"/"+fmTestFileName;
        }
        log.info ("\n>>> Performing test  using data from " + faceMatchTestFile);
        
        // if results are to be saved, build that file name from thetest file name
        saveResultsToFile = saveResults;
        String savedResultFileSpec = "" ;
        if (saveResultsToFile)
        {
            savedResultFileSpec = Utils.buildStoredResultFileName(testProperties,
                testType, fmTestFileName, storedResultDir);    
            // open the file to store the server returned results
           storedResultFile = new TestResultFile(savedResultFileSpec, true);
        }
       
        
         ArrayList<HashMap> fmtestDataSet = fmTestSetBuilder.getTestParams(faceMatchTestFile);
         if (fmtestDataSet == null)
         {
             log.error("No  test  dataset for FaceMatch testing found in test file.");
             return;
         }
         
         testSetSize = Math.min(MAXNUM, fmtestDataSet.size());
         
             
            if (displayResults)
        {
               resultAnalyzer = ResultAnalyzer.getAnalyzer(testProperties, imageCollection);
               displayManager = new DisplayManager( resultAnalyzer);     
               setAnalysisMaps(testType);
        }
            
        String validRegionOp= "(regioningest|regionquery|regionremove)";

         if (testType.matches("facefind"))
         {
            performFaceFinderTests(fmtestDataSet);
         }
         //----------------------------------------------------------------------------------------------------------*/
 
         else  if (testType.matches(validRegionOp))
         {
            performFaceMatchRegionTests(fmtestDataSet);
         } 
        
         else
             DisplayUtils.displayErrorMessage("Invalid test type : "+ testType + " provided.");
         
        if (saveResults && storedResultFile.isFileOpened())  
       {
           storedResultFile.closeFile();
           System.out.println("Test Results stored in file: " + savedResultFileSpec);
       }
        
        int errorCount  = failedResultMap.size() + httpErrorMap.size();
        if (errorCount  > 0)
        {
            DisplayUtils.displayWarningMessage(errorCount + " out of " + this.testSetSize + " operations failed");
        }
        else
        {
            DisplayUtils.displayInfoMessage("All "+ this.testSetSize + " operations passed successfully");  
        }
        
        // after the Success and Error tables are fully built, show the success table
        if (displayManager != null)
            displayManager.wrapup();
    }  
      
     /**********************************************************************************************************
      * Perform a set of operations to check if the search contexts are updated properly for
      *  new ingests and corresponding index files are added to the FaceMatchLib's ImageMatcher
      * Similarly check for removal of index files after regions are deleted.
      * Print the total number of images matches (irrespective of how many returned in MaxMatches)
      * 
      * @param fullQuerytestFilename 
      ***********************************************************************************************************/
    
    protected void performFullQueryTest(String fullQuerytestFile)
    {
        // Open and read the fullQuerytestFile as a JSON Array, where each line is a JSON string 
        // indicating the operation and filename. Perform each operation sequentially as indicated
       //----------------------------------------------------------------------------------------------------------
    }   
    /*****************************************************************************************
     * Perform the set of tests for Face matching  operation as specified in the input dataset
     * with random service requests simulating an operational environment
     * ****************************************************************************************/
    protected void  performAllTests(ArrayList<HashMap>testDataSet)
    {
        int i = 1;
        FM2ServiceResult serviceResult = null;;
        for (HashMap testParams : testDataSet)
        {
            String serviceName = (String) testParams.get("service");
            if (serviceName.equals(ServiceConstants.FACE_FIND))
            {
                 requestFaceFinderService( i++, testParams);   
            } 
            else if (serviceName.equals(ServiceConstants.FACE_MATCH_REGION))
            {
                    requestFaceMatchRegionService(i++, testParams);
            } 
            else
            {
                log.error("Ignoring unknown Service name: " + serviceName);
            }
            //  store in file if requested
             if (saveResultsToFile && serviceResult != null)
                 storedResultFile.writeRecord("\n---- Test Number: " + i + serviceResult.serverReponseContent);             
             
             if (i >= testSetSize)
               break;
        }  
    }  
            
    /*****************************************************************************************
     * Perform the set of exclusive tests for FaceFinding operation as specified in the input 
     * dataset. TestNum is the test record number in the dataset (1-based.)
     * ****************************************************************************************/
    protected void  performFaceFinderTests(ArrayList<HashMap>faceFinderTestDataSet)
    {

        int i = 0;
        for (HashMap testParams : faceFinderTestDataSet)
        {
           i++;
           if (testParams.get("service").equals(ServiceConstants.FACE_FIND))
           {
               requestFaceFinderService( i, testParams);   
           } 
           if (i >= testSetSize)
               break;
        }
    }
    /*---------------------------------------------------------------------------------------*/ 
     // Perform individual (a single) FaceFinder test - 
     // Return the server response as String to the caller
    /*---------------------------------------------------------------------------------------*/
     public int  requestFaceFinderService( int testNum, HashMap inputParams)
     {
          System.out.println("\n-----------------------------------------------");
         int testId = ((Long) inputParams.get("testId")).intValue();
         System.out.println("Test case: " + testNum+ ", Input parameters: ");
         System.out.println(inputParams.toString());

         FM2ServiceResult serviceResult =
             serviceRequestor.requestFaceFinderService(testNum, inputParams);

        String resultStr = serviceResult.serverReponseContent ;              // returned result in JSON format
        checkForNoFaces(testNum, testId,  inputParams, resultStr);
        displayResult(testNum, testId, ServiceConstants.FACE_FIND_SVC, 
            ServiceConstants.GET_FACES_OP, resultStr);
        return 1;
     }
         
    /*****************************************************************************************
    * Perform the set of exclusive tests for Face matching operation as specified in the input 
    * dataset. TestNum is the test record number in the dataset (1-based.)
    * ImageMapFile records the mapping of all ingested images to their unique tags for
    * display of query results. The entries are accumulative for successive tests.
    * ****************************************************************************************/
    protected void  performFaceMatchRegionTests(ArrayList<HashMap>faceMatcherTestDataSet)
    {        
        int i = 0;
        for (HashMap testParams : faceMatcherTestDataSet)
        { 
             i++;
           if (testParams.get("service").equals(ServiceConstants.FACE_MATCH_REGION))
           {
               requestFaceMatchRegionService(i, testParams);
           }  
         if (i >= testSetSize)        // don't do the whole 
            break;
        
        }
    }
    

    /*----------------------------------------------------------------------------- -----*/ 
     // Perform individual FaceMatch Region  test
    /*-----------------------------------------------------------------------------------*/
     public int  requestFaceMatchRegionService( int testNum, HashMap inputParams)
     {
         int testId = -1;  // not provided
        if (inputParams.get("testId") != null)
            testId =((Long) inputParams.get("testId")).intValue();
        String serviceName = (String)inputParams.get("service");
        String operation =  (String)inputParams.get("operation");
        if (operation == null)
        {
             log.error ("IMissing operation type in input data");
             return 0;
        }

        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println("Test case: " + testNum);

        /*-----------------------------------------------------------------------------------------
         ** Send the request to the server 
         *------------------------------------------------------------------------------------------*/
        FM2ServiceRequestor  serviceRequestor = new FM2ServiceRequestor(fm2ServerURL);
        int serviceType = 0;
        int operType = 0;
        FM2ServiceResult serviceResult = null;
        if (serviceName.equals(FACE_FIND))
        {
            serviceType = FACE_FIND_SVC;
            operType = GET_FACES_OP;
            serviceResult = serviceRequestor.requestFaceFinderService(testNum, inputParams);
        } 
        else if (serviceName.equals(ServiceConstants.FACE_MATCH_REGION))
        {
             serviceType = FACE_MATCH_REGION_SVC;
             operType = operation.equals(INGEST) ? REGION_INGEST_OP : REGION_QUERY_OP;
             serviceResult = serviceRequestor.requestFaceMatchRegionService(testNum, inputParams);
        } 
        else
        {
            log.error("Ignoring unknown Service name: " + serviceName);
        }
        //  store in file if requested
         if (saveResultsToFile)
             storedResultFile.writeRecord("\n---- Test Number: " + testId + serviceResult.serverReponseContent);                  
 
        // Add a row to the display table of successful ingests
        int status = displayResult(testNum, testId, serviceType, 
            operType, serviceResult.serverReponseContent);
     
        if (status == 1) 
            log.info("Operation " + operation + " successful");
        else
            log.error ("Error performing operation.");
        return status;
    }
     
    /*****************************************************************************************
    * Perform the set of exclusive tests for Face matching operation as specified in the input dataset
    * ****************************************************************************************/
    protected void  performWholeImageMatchTests(ArrayList<HashMap>faceMatcherTestDataSet)
    {
        int i = 0;
        for (HashMap testParams : faceMatcherTestDataSet)
        { 
            i++;
           if (testParams.get("service").equals(ServiceConstants.WHOLE_IMAGE_MATCH_SVC))
           {
               requestWholeImageMatchService(i, testParams);
           }  
        }
        // if (i == 1)
        //    return;            // just start with a subset
    }
    /*----------------------------------------------------------------------------- -----*/ 
     // Perform individual FaceFinder test - bypassing the Servlets and invoking
     // the FMServiceBroker 
    /*-----------------------------------------------------------------------------------*/
     protected int requestWholeImageMatchService( int testNum, HashMap testParams)
     {
         /*
         Integer testId =  (Integer)testParams.get("testId");
         String operation = "";
         String testType =  (String)testParams.get("operation");
         if (testType.equalsIgnoreCase("ingest"))
             operation  =  ServiceConstants.INGEST;
         else if (testType.equalsIgnoreCase("query"))
              operation =  ServiceConstants.QUERY; 
         else if  (testType.equalsIgnoreCase("remove"))
              operation =  ServiceConstants.REMOVE;

        HashMap requestParams = testParams;
        requestParams.remove("operation");     // not a parameter to the server
        requestParams.remove("service");  
         requestParams.remove("testId");  

        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println("Test case: " + testNum+ ", Input parameters: ");
        System.out.println(requestParams.toString());

    
        byte[] returnData = ClientRequest.getResponseContents(serverResponse);
        String returnStr = new String(returnData);  
        if (debug)
            System.out.println("FM2 Server returned data: \n" + returnStr);
        
       
         // Check for Error response
        String[] opStatus = ClientRequest.getOperationStatus( serverResponse);

        String success = String.valueOf(ServiceConstants.SUCCESS);
        if (!opStatus[0].equals(success))
         {
             log.error("Operation " + operation + "  not successful: errorCode: " +
                 opStatus[0]  +", error message: " +  opStatus[1] );
            addToServerErrorMap(testId, serverResponse.getStatusLine());
             return 0;
         }
      int operationType = ServiceUtils.getOperationType(
            ServiceConstants.WHOLE_IMAGE_MATCH_SVC, operation);
        displayResult(testNum,  testId.intValue(), 
            ServiceConstants.WHOLE_IMAGE_MATCH_SVC, operationType, returnStr);

        
        // if the client wanted the mapping to be recorded, add the mapping,
        // overwriting any earlier ingest results for the same URL
                
        if (operationType == ServiceConstants.WHOLEIMAGE_INGEST_OP && imageTag2urlMap != null)
        {
            String imageTag = (String)requestParams.get(IMAGE_TAG);
            String imageUrl = (String)requestParams.get(URL);
            imageTag2urlMap.put(imageUrl, imageTag); 
        }
        else if (operationType == ServiceConstants.WHOLEIMAGE_REMOVE_OP && imageTag2urlMap != null)
        {
            String imageTag = (String)requestParams.get(IMAGE_TAG);
            imageTag2urlMap.remove(imageTag); 
        }

        log.info("Operation " + operation + " successful");
        // decode the informatioon return in the response body as the content
         return 1;
         */
         return 0;          // not implemented
     }
     
    /*--------------------------------------------------------------------------------------------------------*/ 
     protected String getImageTagMapFileName(String extentName)
     {
         String prefix = imageCollection+".";
         
         // go with the default location
         if (clientName == null || clientName.isEmpty())
            clientName =  getClientName(imageCollection);
         
         String tagRootDir = (String)testProperties.get("fm2test.datadir");
         String filenamePart = (String)testProperties.get("imageFile2tag.name");
         if (filenamePart == null)
              filenamePart = "ImageTag2URL.json";
             
         String imageTagFileName = tagRootDir+"/"+clientName+"/imagetags/"+extentName+"_"+filenamePart;

          log.info("Using imageTag2URL Map file: " + imageTagFileName);
          
         return imageTagFileName;
     }
     /*------------------------------------------------------------------------------------------------------*/
     // get the name of the image annotation file     
     protected String getImageAnnotationFile(String collectionName)
     {
         String prefix =  (collectionName == null || collectionName.isEmpty()) ? ""  : collectionName.toLowerCase()+".";
         String imageAnnotFile = ((String)testProperties.get(prefix+"annotation.filename"));
         if (imageAnnotFile != null)
             imageAnnotFile = imageAnnotFile.trim();
         return imageAnnotFile;
     }

    /*--------------------------------------------------------------------------
     * Display the results, returned by the FM Server for specific operations
     * on the application's console
     * If a row could not be added to the server due to operational failure
     *  (e.g. no faces found or no query matches etc.) it is added to the 
     * failedMap.
     *-------------------------------------------------------------------------/
     */
     
     protected int  displayResult(int testNum, int testId, int serviceType, int operation, 
         String serviceResult)
     {
         if (saveResultsToFile)
             writeResultRecord(testNum, testId, serviceType, operation, serviceResult);
         
         if (!displayResults)
                return 1;
         // Check if the Server returned okay status 
         if (! isValidResult (serviceResult))
         {
             addToFailedResultMap(testId, serviceResult);
            // return -1;         
         }// not valid 
         
         int status = 0;
         if (displayManager != null)
         {
            status = displayManager.displayResult(testNum, testId, serviceType, operation, serviceResult);   
            if (status == 0)
                addToFailedResultMap(testId, serviceResult);
         }
         return status;
     }


    /*-------------------------------------------------------------------------------
     * Write the results of the given test to the specified file as a JSON record
     * which may be retrieved and displayed later
     *------------------------------------------------------------------------------*/
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
     
 /*--------------------------------------------------------------------------*
  * Check if the return result does not have any detected faces. If so, h 
  *  record it wit the annotated values for analysis.
  *-------------------------------------------------------------------------*/

    protected void checkForNoFaces(int testNum, int testId, HashMap inputParams, String result)
    {
        try
        {
            JSONObject resultObject = (JSONObject) jparser.parse(result);
            if (resultObject.get("faceRegions") == null)
            {
                String imageURL = (String) resultObject.get(URL);
                urlsWithNoFaces.put(new Integer(testId), imageURL);
            }
        }
        catch (ParseException e)
        {
        
        }
    }
/*----------------------------------------------------------------------------------------------------------*/
    protected void addToServerErrorMap(int testId, FM2ServiceResult serviceResult)
    {
         String errorMsg = "Http Error code: " + serviceResult.httpStatus + ",  message: " + serviceResult.statusMessage;
         httpErrorMap.put(new Integer(testId), errorMsg);
    }
    
    /*----------------------------------------------------------------------------------------------------------*/
    protected void addToServerErrorMap(int testId,  StatusLine httpStatus)
    {
         String errorMsg = "Http Error code: " + httpStatus.getStatusCode() +
                 ",  message: " + httpStatus.getReasonPhrase();
         httpErrorMap.put(new Integer(testId), errorMsg);
    }
    
  /*----------------------------------------------------------------------------------------------------------*/
    /* Save the server returned result string if the operation was not successful
   /*----------------------------------------------------------------------------------------------------------*/
    
    protected void addToFailedResultMap(int testId,  String serviceResultStr)
    {
        failedResultMap.put(new Integer(testId), serviceResultStr);
    }
/*----------------------------------------------------------------------------------------------------------*/ 
// Read theImage annotions provided as an external text file, with one line 
// per image
/*-------------------------------------------------------------------------------------------------------------*/     
    protected LinkedHashMap<String, String> readImageAnnotationMap(String imageAnnotFile)
    {
         LinkedHashMap <String, String>image2annotMap = new LinkedHashMap();
         File file = new File (imageAnnotFile);
        if (!file.exists())
        {
            log.warn("Image Annotation file " +  imageAnnotFile + " does not exist.");
            return image2annotMap;                // no input provided, create a new Map
        }
        try
        {
            // Open the file
            FileInputStream fstream = new FileInputStream(imageAnnotFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null)  
            {
                // split  the file name and annotation
                String[]  segments = strLine.split("\\s+", 2);
                image2annotMap.put(segments[0], segments[1]);
            }
            fstream.close();
            return  image2annotMap;
        }
        catch(Exception e)
        {
            log.error("Could not open/read image annotation file " + imageAnnotFile, e);
            return null;
        }
    } 
    /************************************************************************************************/
    // Display the results of a FaceMatch system test stored in a results file
    // @param testType - Type of test performed
    //------------------------------------------------------------------------------------------------------------------------*//
    protected  int displayFaceMatchStoredResults(String testType, String testFileName, 
        String storedResultDir)
    {
        displayResults = true;
        saveResultsToFile = false;              // already saved
        // check if the Stored resultPath is a File or a directory. If directory, get the filename from the test file
         String storedResultFile = Utils.buildStoredResultFileName( testProperties,
             testType, testFileName, storedResultDir);    
         
         resultAnalyzer = ResultAnalyzer.getAnalyzer(testProperties, imageCollection);
         displayManager = new DisplayManager( resultAnalyzer);     
         setAnalysisMaps(testType);

        if (testType.equals("facefind"))
        {
               // read the imageAnnotation map if available
            String  imageAnnotFileName = getImageAnnotationFile(imageCollection);
            if (imageAnnotFileName != null)
            {
                String annotFile = this.getImageAnnotationFile(imageCollection);
                imageAnnotationMap = readImageAnnotationMap(imageAnnotFileName);
                displayManager.setImageAnnotationMap(imageAnnotationMap);
            }
        }
        displayManager.displayResultFromFile(storedResultFile);
         return 1;
    }
    
    /*----------------------------------------------------------------------------------------------------------------*/
    protected String buildResultStoreFileName(String testType, String testFileName)
    {
            String fileName = testFileName;
            int index = fileName.lastIndexOf(".");
            if(index > 0)
                fileName = fileName.substring(0,index);
  
            String resultFileName = fileName+"_Results.json";       // just the name part
            String resultDir = testProperties.getProperty("fm2test.resultstoredir")+"/"+testType;
            String savedResultFileSpec = resultDir+"/"+resultFileName;
         
           return savedResultFileSpec;
    }
  
     /*----------------------------------------------------------------------------------------------------------------*/
    // Should the results of a a batch operation be stored. True by default
    // can be overridden in the client's config file
    //
    protected boolean doStoreResults()
    {
        boolean toStore = true;
        String storeProperty = testProperties.getProperty("fm2test.storeResults");
        if (storeProperty != null && storeProperty.equalsIgnoreCase("false"))
            toStore = false;
        return toStore;
    }
    
  /*----------------------------------------------------------------------------------------------------------------*/
    protected boolean storeResults(String savedResultFileSpec)
    {
            storedResultFile = new TestResultFile(savedResultFileSpec, true);
            if (! storedResultFile.isFileOpened())     // error opening file in write mode
            {
               log.error ("Cannot save test results in file" + savedResultFileSpec);
               saveResultsToFile = false;
            }
            else
               saveResultsToFile = true;
            return saveResultsToFile;
    }

  /*-------------------------------------------------------------------*/
  /* Wait till the user exits
   /*-------------------------------------------------------------------*/
    public void waitTillClose()
    {
        // now wait here till the user exits
        displayManager.waitTillClose();
    }

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
        if (numArgs < 6)
        {
            System.out.println(" Number of Arguments should be 6 or 7");
            System.exit(-1);
        }
         // display data from previous tests, no need to talk to the server 
         boolean realtimeMode = args[0].equalsIgnoreCase("realtime");
         String operation = args[1];
         String clientName = args[2];          
         String extentName = args[3];
         String testFile = args[4];              // relative path name
         
         int maxnum = 20000;
         if (numArgs == 7)
             maxnum = Integer.parseInt(args[6]);          // max number of entries to test
  
         boolean facefind = false;
         boolean ingest = false;
         boolean query = false;
         boolean remove = false;
       
             if (operation.equalsIgnoreCase("facefind"))
             {
                facefind = true;
             }
             else if (operation.equalsIgnoreCase("ingest"))
             {
                ingest = true;
            }  
             else if (operation.equalsIgnoreCase("query"))
             {
                query  = true;
             }
             else if (operation.equalsIgnoreCase("remove"))
             {
                remove  = true;
             }
             else
             {
                 System.out.println ("Operation must be facefind or ingest or query or remove");
                 System.exit(-1);
             }   
        try
        {
            FM2RegionBatchApp  regionTest= new FM2RegionBatchApp(testConfigFile, realtimeMode);
            regionTest.MAXNUM = maxnum;
            
            log.info("----------------------------------------------------------------------------------" +
              "\n Performing FaceMatch tests for  Test Application" +
              "\n Operation mode: " + (realtimeMode? " Connect to Server " : "Display stored results") +
              "\n Tests: " + (facefind ? "  facefind ": "")  + (ingest ? " ingest"  : "") + (query ? "  query" : "" )+
              "\n Configuration file: " + testConfigFile + ", Max number of  cases to test: " + maxnum +
              "\n-------------------------------------------------------------------------------");

            regionTest.setImageCollection(extentName);
             regionTest.setClientName(clientName);
            boolean storedMode = !realtimeMode;
             
            String storeResultFilename = regionTest.buildResultStoreFileName(operation,  testFile);
            if (storedMode)
            {
                //  << --------FIX  ME ---------------  give File path>>
                 if (facefind)
                     regionTest.displayFaceMatchStoredResults("facefind", testFile, storeResultFilename);
                 if (ingest)
                     regionTest.displayFaceMatchStoredResults("regioningest",   testFile, storeResultFilename);
                 if (query)
                    regionTest.displayFaceMatchStoredResults("regionquery", testFile, storeResultFilename);
            }
            else
            {
                // Normal operation
                if (!regionTest.isValidConnection())
                {
                    log.error("Cannot talk to the FM2 server. Exiting.");
                    System.exit(-1);
                }  
            
                boolean saveResults = regionTest.doStoreResults();         // for later display &  analysis
                regionTest.displayResults = args[5].equals ("display");
                if (facefind)      
                    regionTest.doFaceMatchRealtimeTests("facefind", testFile, storeResultFilename, saveResults);
                else if (ingest)
                    regionTest.doFaceMatchRealtimeTests("regioningest", testFile, storeResultFilename, saveResults);
                else if (query)
                    regionTest.doFaceMatchRealtimeTests("regionquery", testFile, storeResultFilename, saveResults);  
                else if (remove)
                    regionTest.doFaceMatchRealtimeTests("regionremove", testFile, storeResultFilename, saveResults);  
                if (! regionTest.displayResults)            // don't wait for user to exit
                    System.exit(0);
            }
        }
        catch (Exception e)
        {
                log.error("Error while activating/running FMClientApp, error: " + e.getMessage() + ". Exiting.");
                e.printStackTrace();
                System.exit(-1);
        }
        //System.exit(0);
    }
}
