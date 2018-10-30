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
* This class performs parallel  FM2 operations, mainly queries, based upon the  the
* corresponding data providedin the testfile as an argument.
* To simulate a real scenario, each query is is performed on a different thread, by sending
* the request through a corresponding ServiceRequestor.
*
* The results are recorded in an output file.
* >>>>>>>> Note: Check why it does not work...dmisra <<<<<<<
 */

package fm2client.app;

import fm2client.core.FM2RequestAgent;
import fm2client.core.FM2ServiceResult;

import fm2client.display.DisplayManager;
import fm2client.analyzer.DefaultResultAnalyzer;

import fm2client.testgen.FMTestSetBuilder;
import fm2client.testgen.TestResultFile;
import fm2client.util.Timer;
import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.ServiceUtils;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.LinkedHashMap;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;


public class FM2ParallelQueryApp extends FM2WebClient
{
    private Logger log = Logger.getLogger(FM2ParallelQueryApp.class);
    
    JSONObject fmCommTemplate;
    JSONObject inputData;
    JSONParser jparser;
        
    FMTestSetBuilder  fmTestSetBuilder;
    DisplayManager displayManager;
    
    LinkedHashMap <Integer, String> urlsWithNoFaces;          // no detected faces
    
    protected boolean debug = true;
    protected boolean saveResultsToFile = false;
    TestResultFile storedResultFile = null;
   
    //FM2ServiceRequestor serviceRequestor = null;
    
    public FM2ParallelQueryApp(String testConfigFile)  throws Exception
    {
        super(testConfigFile, true);
        displayManager = new DisplayManager(new DefaultResultAnalyzer(super.testProperties));
    }
    
    // ------------------------------------------------------------------------------------------------------
    // Do  parallel perations pertaining to an FM2 operations: currently only queries. Test data is
    // provided in the test file as JSON Object array
   // ------------------------------------------------------------------------------------------------------
     protected void doParallelTest( String testFileName, int maxThreads)
     {
         FMTestSetBuilder  fmTestSetBuilder = new FMTestSetBuilder(testProperties);            // testProprties generated by Parent class
         String testdataTopDir = (String)testProperties.get("fm2test.datadir");
         
         String testFile = testdataTopDir+"/"+testFileName;
         ArrayList<HashMap>testDataSet = fmTestSetBuilder.getTestParams(testFile);
         if (testDataSet == null)
         {
             log.error("No valid  test  dataset for FaceMatch testing found in test file "+ testFile);
             return;
         }
           ArrayList<FM2ServiceResult> serviceResults = performParallelTests(testDataSet, maxThreads);
           displayResults(serviceResults);                // dsplay as a table
     }
      /*-----------------------------------------------------------------------------------------------------------------------------*/  
       /** Create a different ServiceRequestor  and get results for each request sent to the server.
        * Use total number of threads is provided as the parameter maxThreads
       /*-------------------------------------------------------------------------------------------------------------------------------*/
    
    public    ArrayList<FM2ServiceResult> performParallelTests( ArrayList<HashMap>testDataSet, int maxThreads)
    {
        System.out.println("------- Number of parallel threads  used ---------" + maxThreads);

        Timer qtimer = new Timer();
        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        ArrayList<Future<FM2ServiceResult>> taskList = new ArrayList<Future<FM2ServiceResult>>();

        int numRequests = testDataSet.size();        // number of requests to send)
        for (int i = 0; i < maxThreads; i++)
        {
            Callable<FM2ServiceResult> worker = new FMParallelRequestSender(i, testDataSet.get(i));
             Future<FM2ServiceResult> task = executor.submit(worker);
             taskList.add(task);
         }

        // wait till all the submitted tasks finish execution, 
        // and combine the result of FM similaritymatching

        ArrayList<FM2ServiceResult> allResults = new ArrayList();
        for (Future<FM2ServiceResult> atask : taskList)
        {
              try 
              {
                  FM2ServiceResult result = atask.get();
                  if (result != null)
                        allResults.add( result);     //   future.get  invokes worker.call()
             } 
              catch (InterruptedException e)
              {
                 log.error("Error in parallel query execution:", e);
              }
              catch (ExecutionException e) 
              {
                  log.error("Error in parallel query execution:", e);
             }
               catch (ArrayIndexOutOfBoundsException ae) 
              {
                  log.error("Error in parallel query execution:", ae);
                  return null;
             }
        }
         executor.shutdown();
         System.out.println("Parallel execution time: " + qtimer.getElapsedTime() + " msec");
         Timer.release(qtimer);
         return allResults; 
    }
     /*---------------------------------------------------------------------------------------------------
      * save the results onto a disk file file and display on the console as a table
     *-------------------------------------------------------------------------------------------------------*/
     protected void  displayResults(ArrayList<FM2ServiceResult> serviceResults)
     {
         for(int i = 0; i < serviceResults.size(); i++)
         {
             FM2ServiceResult result = serviceResults.get(i);
             int service = result.service;
             String operation = result.operation;
             int oper = ServiceUtils.getOperationType(service, operation);
             String serviceResultStr = serviceResults.get(i).serverReponseContent;
             displayResult(i,  i,  service, oper, serviceResultStr);
         }
     }
    
    /*---------------------------------------------------------------------------------------------------------------*/
    protected class FMParallelRequestSender implements Callable<FM2ServiceResult >
    {
        int testNum;
        HashMap requestData;
        
        protected FMParallelRequestSender(int testNum, HashMap testData)
        {
            this.testNum = testNum;
            this.requestData = testData;
        }

        /*------------------------------------------------------------------------------------------------*/
        public FM2ServiceResult call()
        {
            try
            {
               FM2ServiceResult  result = performOperation(testNum, requestData);
               return result;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

    /*-----------------------------------------------------------------------------------------------*/ 
    // Perform the designated facematch operation (ingest, or query or facefind)
    /*------------------------------------------------------------------------------------------------*/
     protected FM2ServiceResult  performOperation( int testNum, HashMap inputParams)
     {
         System.out.println("-----------------------------------------------");
         System.out.println("Test case: " + testNum+ ", Input parameters: ");
         System.out.println(inputParams.toString());
         
         int serviceType = ((Integer) inputParams.get("service")).intValue();  
         String operation =   (String)inputParams.get("operation");  
         
         if (serviceType !=  ServiceConstants.FACE_MATCH_REGION_SVC &&
                 serviceType != ServiceConstants.FACE_FIND_SVC)
         {
             log.error ("Invalid  service type " + serviceType + " specified");
             return null;
         }
       
         HashMap requestParams = inputParams;
         requestParams.remove("operation");        // not a parameter to the server
         requestParams.remove("service");
         requestParams.remove("testId");
         
         FM2RequestAgent serviceRequestor =  new FM2RequestAgent(fm2ServerURL);
         FM2ServiceResult  serviceResult = serviceRequestor.executeFaceRegionRequest(
                 operation, inputParams);
         
        if (debug)
        {
            if (serviceResult != null)        
                System.out.println("FM2 Server returned data: \n" + serviceResult.serverReponseContent);
        }
        return serviceResult;
     }
    }       // end of inner class
     
     /*--------------------------------------------------------------------------
     * Display the results, returned by the FM Server for specific operations
     * on the application's console
     *-------------------------------------------------------------------------/
     */
     
     protected int  displayResult(int testNum, int testId, int serviceType, int operation, 
         String serviceResultStr)
     {
         if (saveResultsToFile)
             writeResultRecord(testNum, testId, serviceType, operation, serviceResultStr);

         // Check if the Server returned okay status 
         if (! isValidResult (serviceResultStr))
             return -1;                            // not valid 
         if (displayManager != null)
             displayManager.displayResult(testNum, testId, serviceType, operation, serviceResultStr);
         return 1;
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
  /********************************************************************************************/
/** Perform parallel service testing of the FaceMatch Web Server using the standard 
 * FMConfiguration file and appropriate test data sets.
 * @param args 
 *********************************************************************************************/      
    public  static void main(String[] args)
    {
        String   defaultConfigFile = FM2WebClient.getDefaultConfigFile();
   
        if (args.length < 2)
        {
            System.out.println("No test  file name provided in the argument");
            System.exit(-1);
        }

        try
        {
            String testDataFile = args[0];
            int maxThreads = Integer.parseInt(args[1]);
            String  testConfigFile =  (args.length > 2) ?  args[2] : defaultConfigFile;
            System.out.println("Using configuration file: " + testConfigFile);
            FM2ParallelQueryApp  parallelTestApp = new FM2ParallelQueryApp(testConfigFile);
            
            if (!parallelTestApp.isValidConnection())
            {
                System.out.println("Cannot talk to the FM2 server. Exiting.");
                System.exit(-1);
            }  
           // topDir =  homeDir+"/DevWork/FaceMatch2/FM2JavaClient/testsets/testdata/"
             parallelTestApp.doParallelTest(testDataFile, maxThreads);
        }
        catch (Exception e)
        {
                System.out.println("Error while activating/running FMClientApp, error: " + e.getMessage() + ". Exiting.");
                e.printStackTrace();
                System.exit(-1);
        }
    }
}
