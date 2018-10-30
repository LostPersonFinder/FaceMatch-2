/*
 * /*
 * Informational Notice:
 * This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
 * an agency of the Department of Health and Human Services, United States Government.
 *
 * The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.
 *
 * The license does not supersede any applicable United States law.
 *
 * The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.
 *
 * Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
 * (FAR) 48 C.F.R. Part52.227-14, Rights in Dataï¿½General.
 * The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
 * non-commercial products.
 *
 * LICENSE:
 *
 * Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
 * as provided by Federal law.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * -	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.
 *
 * -	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
 * in the documentation and/or other materials provided with the distribution.
 *
 * -	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
 * of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package fmservice.test;


import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.ServiceUtils;

import fmservice.server.entry.FMServiceManager;
import fmservice.server.global.ConfigurationManager;
import fmservice.server.ops.FMServiceBroker;

import fmservice.server.util.Timer;

import fmservice.server.result.FMServiceResult;
import fmservice.server.result.FaceFindResult;
import fmservice.server.result.DBQueryResult;
import fmservice.server.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.log4j.Logger;
/**
 *
 *
 */
public class LocalServerTest implements ServiceConstants
{
    private Logger log = Logger.getLogger(LocalServerTest.class.getName());
    
    String fmConfigFile;                // For database connection, index store etc.
    private static FMServiceManager fmManager;       // singleton controller
    private FMServiceBroker fmServiceBroker;       // singleton controller
    
    FMTestSetBuilder  testSetBuilder;
    Properties testProperties;
    
    Timer initTimer = new Timer();
    int status = 0;

/*----------------------------------------------------------------------------------------------*/
  // Note: Logging is initialized by FMServiceManager, so don't do it again
    
    public LocalServerTest (String testConfigFile)
    {
       fmConfigFile = testConfigFile;          // file with test set file names
       status =  initFMSystem();
       
       if (status == 1)
       {
            testProperties = ConfigurationManager.getConfig();
       }
    }
    
    public int initFMSystem()
    {
        initTimer = new Timer();
        //  initialize the facematch system by stating up the FMServiveManager
        startFMServiceManager(fmConfigFile);
        int status = fmManager.getStatus();
        if (status != 1)
       {
           String[] errorCodes = fmManager.getError();
           log.warn("FaceMatch System could not be started, error code: " + errorCodes[0] +
               ", error message:" + errorCodes[1]);
       }
        return status;
    }
    
    // Get FaceMatch system initilaization status
    public int getStatus()
    {
        return status;
    }

     /*----------------------------------------------------------------------------------------------*/
     // Start the Facematch system by initializing various control objects
     /*------------------------------------------------------------------------------------------------*/ 
     protected void  startFMServiceManager(String configFile)
     {
         fmManager=   FMServiceManager.createServiceManager(fmConfigFile);    
        int status = fmManager.getStatus();
        float initTime = initTimer.getElapsedTime();           // in milliseconds
        Timer.release(initTimer);
        if (status == 1)
        {
            log.info("\n"+
                     "**********************************************************************"
                + "\nFaceMatch system started up successfully in "+ (initTime/1000) + " seconds"
                + "\n using configuration file: " + configFile  
               + "\n**************************************************************************" );
            
            fmServiceBroker = new FMServiceBroker();
        }
        else
        {
            String[] errorInfo = fmManager.getError();
            if (!errorInfo[0].equals("1"))
            {
                log.fatal("FaceMatch System could not be started, error code: " + errorInfo[0] +
                    ", error message:" + errorInfo[1]);
                System.exit(status);
            }
        }
     }

     /************************************************************************************************/
    
    public void doFaceMatchLocalTests(String testFileName)
    {
        if (testFileName == null || testFileName.isEmpty())
        {
            System.err.print("Please provide a proper test file in JSON format.");
            return;
        }
        
        FMTestSetBuilder testSetBuilder = new FMTestSetBuilder(testProperties);
         ArrayList<HashMap> fmtestDataSet = testSetBuilder.getTestParams(testFileName);
         if (fmtestDataSet == null)
         {
             log.error("No  test  dataset for FaceMatch testing found in test file.");
         }
         performAllTests(fmtestDataSet);
    }  

    /*****************************************************************************************
     * Perform the set of tests for FaceFinding operation as specified in the input dataset
     * TestNums are 1 based to match with  input test numbers
     * ****************************************************************************************/
    protected void  performAllTests(ArrayList<HashMap>testDataSet)
    {
        int i = 0;
        for (HashMap testParams : testDataSet)
        {
            String serviceName = (String)testParams.get("service");
            int  service = ServiceUtils.getServiceType( serviceName);
            if (service == ServiceConstants.FACE_FIND_SVC)
           {
               performFaceFinderTest(++i, testParams);
           } 
           else if (service == ServiceConstants.FACE_MATCH_REGION_SVC)
           {
               performFaceMatchRegionTest(++i, testParams);
           }
           else if (service == ServiceConstants.ADMIN_SVC)
            {
                ; //performFaceMatchAdminTest(i++, testParams);
            }    
             else if (service == ServiceConstants.IMAGE_EXTENT_SVC)
            {
                performImageExtentOpTest(++i, testParams);
            }    
        }
        System.out.println("Performed " + i + " operations successfully");
        return;
    }
  
    
     /*-------------------------------------------------------------------------------------------------------------*/ 
     // Perform individual FaceFinder test - bypassing the Servlets and invoking
     // the FMServoveBroket, which is the root point of 
     protected void   performImageExtentOpTest( int testNum, HashMap inputParams)
     {
        System.out.println("-----------------------------------------------");
        System.out.println("Test case: " + testNum+ ", Input parameters: ");
        System.out.println(inputParams.toString());
        
        Timer startTimer = new Timer();
        int serviceType = IMAGE_EXTENT_SVC;
        String operation = (String)inputParams.get("operation");
          
        int  operationType = 0;
       
         if (operation.equalsIgnoreCase("add"))
             operationType  =  ServiceConstants.ADD_EXTENT_OP;
         else if (operation.equalsIgnoreCase("remove"))
              operationType =  ServiceConstants.REMOVE_EXTENT_OP; 
         else if  (operation.equalsIgnoreCase("activate"))
              operationType =  ServiceConstants.ACTIVATE_EXTENT_OP;
          else if  (operation.equalsIgnoreCase("deactivate"))
              operationType =  ServiceConstants.DEACTIVATE_EXTENT_OP;
          else if (operation.equalsIgnoreCase("performance"))
              operationType =  ServiceConstants.SET_PERFORMANCE_OP;
           else 
          {
              System.out.println("----------------------------------------------------------------------------------------");
              System.out.println(">>> Invalid ImageExtent Operation " + operation + " In Test # " +testNum);
              return;
          }
        
        FMServiceResult result  = fmServiceBroker.processServiceRequest(serviceType, operationType,  inputParams);
        result.serviceTime = startTimer.getElapsedTime();
        Timer.release(startTimer);
       
        
        if (!result.isValidRequest())
            System.out.println("Invalid client requst; Error message: "+  result.getStatus().statusMessage);
        else if (!result.isSuccess())
        {
            System.out.println("Request not successful; Error message: "+  result.getStatus().statusMessage);
        }
        else
        {
            System.out.println("ImageExtent service result");
            System.out.println( result.convertToJSONString());
        }
          System.out.println("*-----------------------------------------------------------------*");     
     }
        

  /*    String operation =  (String)inputParams.get("operation");
         if (inputParams.get(ServiceConstants.CLIENT_KEY)  == null)
        {
            log.error("Missing Client Key in the input data.");
             return new FM2ServiceResult(IMAGE_EXTENT_SVC, operation,  FM2ServiceResult.INVALID_REQUEST, 
                "Missing Client Key  in the input data."  +operation, null);
        }

        HashMap requestParams = removeExtraParams( inputParams);
        FM2RequestAgent serviceRequestor = new FM2RequestAgent(fm2ServerURL);
        FM2ServiceResult serviceResult = 
                 serviceRequestor.executeExtentRequest(operation, requestParams);
        printServiceResult(testNum, serviceResult, formattedPrint);
        return serviceResult;
     }  
 
  
    /*-------------------------------------------------------------------------------------------------------------*/ 
     // Perform individual FaceFinder test - bypassing the Servlets and invoking
     // the FMServoveBroket, which is the root point of 
     protected void   performFaceFinderTest( int testNum, HashMap faceFinderTestParams)
     {
        int serviceType = ServiceConstants.FACE_FIND_SVC;
        int operationType = ServiceConstants.GET_FACES_OP;
        HashMap inputParams = faceFinderTestParams;


        System.out.println("FaceFind Test case: " + testNum+ ", Input parameters: ");
        System.out.println(inputParams.toString());
       
        Timer startTimer = new Timer();
        FMServiceBroker serviceBroker = new FMServiceBroker();
        FaceFindResult result  = ( FaceFindResult)serviceBroker.processServiceRequest(serviceType, operationType,  inputParams);
        result.serviceTime = startTimer.getElapsedTime();
        Timer.release(startTimer);
        // Print out the return information:
        String annotatedFaces = (String)faceFinderTestParams.get("annotated region");
        if (annotatedFaces == null) annotatedFaces = " -none-";
        
        if (!result.isValidRequest())
            System.out.println("Invalid client requst; Error message: "+  result.getStatus().statusMessage);
        else if (!result.isSuccess())
        {
            System.out.println("Request not successful; Error message: "+  result.getStatus().statusMessage);
            System.out.println(">>  AnnotatedFaces: " + annotatedFaces);
        }
        else
        {
            System.out.println("FaceFinderTest result:");
            System.out.println( result.convertToJSONString());

            String detectedFaces = "";
            for (int i = 0; i < result.numFaces; i++)
            {
               if (i > 0) detectedFaces += "  ";
               detectedFaces += result.faceRegions[i]; 

            }
            System.out.println(">> Detected Faces: " + detectedFaces + ", Annotated Faces: " + annotatedFaces );
        }
          System.out.println("*-----------------------------------------------------------------*");     
     }
   
    /*-------------------------------------------------------------------------------------------------------------*/ 
       // Perform individual FaceFinder test - bypassing the Servlets and invoking
       // the FMServiceBroker 
       protected void   performFaceMatchRegionTest( int testNum, HashMap testParams)
       {
         int  operationType = 0;
         String testType =  (String)testParams.get("operation");
         if (testType.equalsIgnoreCase("ingest"))
             operationType  =  ServiceConstants.REGION_INGEST_OP;
         else if (testType.equalsIgnoreCase("query"))
              operationType =  ServiceConstants.REGION_QUERY_OP; 
         else if  (testType.equalsIgnoreCase("remove"))
              operationType =  ServiceConstants.REGION_REMOVE_OP;

        HashMap requestParams = testParams;
        requestParams.remove("operation");     // not a parameter to the server
        requestParams.remove("service");  

        System.out.println("----------------------------------------------------------------------------------------");
        System.out.println("FaceMatchRegion Test case: " + testNum+ ", Operation: " 
            +testType + ", Input parameters: ");
        System.out.println(requestParams.toString());
        
        Timer startTimer = new Timer();

        FMServiceBroker serviceBroker = new FMServiceBroker();
        FMServiceResult fmResult = serviceBroker.processServiceRequest(
            ServiceConstants.FACE_MATCH_REGION_SVC, operationType,  requestParams);
        fmResult.serviceTime = startTimer.getElapsedTime();
        Timer.release(startTimer);

         // Print out the return information:
         if (!fmResult.isValidRequest())
             System.out.println("Invalid client requst; Error message: "+  fmResult.getStatus().statusMessage);
         else if (!fmResult.isSuccess())
             System.out.println("Request not successful; Error message: "+  fmResult.getStatus().statusMessage);
         else
         {
                System.out.println("FaceMatch Test  result:");
                System.out.println( fmResult.convertToJSONString());
         }
          System.out.println("*-----------------------------------------------------------------*");     
     }

/********************************************************************************************/
    protected void testDBInfoAccess()
    {
        DatabaseInfo dbInfo = new DatabaseInfo();
        DBQueryResult queryResult;

        System.out.println("-\n------------- Client Information----------------------"); 
        queryResult = dbInfo.getClientInfoForName("FMResearch");
        System.out.println(queryResult.convertToJSONString());

        queryResult = dbInfo.getClientInfoForName("PL");
        System.out.println(queryResult.convertToJSONString());

        System.out.println("-\n------------- Extent Information----------------------"); 
        queryResult = dbInfo.getExtentInfoForClient("PL", "test");
        System.out.println(queryResult.convertToJSONString());

        System.out.println("------------  Extent  Information ----------------------------");  
        queryResult = dbInfo.getExtentInfoForClient("FMResearch", "colorferet");
        System.out.println(queryResult.convertToJSONString());

        System.out.println("\n------------- Image Information----------------------"); 
         queryResult = dbInfo.getImageInfoForClient("FMResearch", "ColorFeret", "00002_930831_fb");
         System.out.println(queryResult.convertToJSONString());
    }

/********************************************************************************************/
/** Perform local testing of the FaceMatch system using the standard FMConfiguration file
 * and appropriate test data sets.
 * 
 * The configuration file is located  based on the Operating System we are running in.
 * @param args 
*/
            
    public  static void main(String[] args)
    {
        // remote test implies talking to FaceMatch2 Web Server 
        String filename = "FM2ServerLocalTest.cfg";
        String dir;
        
        if (Utils.isWindows())
        {
             dir = "<TopDir>/FM2Server/fmTestdir";
        }
        else        // assume Unix-based
        {
             dir ="<TopDir>/FM2Server/localTestDir/config";
        }
        String defaultConfigFile =  dir+"/"+filename;
        
        // Check if a different one is specified
        String configFile = System.getProperty("fm2server.config");
        if (configFile == null)
           configFile = defaultConfigFile;
        // make sure that the file exists
       File file = new File(configFile);
       if (!file.exists())
       {
           System.out.println("Test Configuration file " + configFile + " does not exist");
           System.exit(-1);
       }
 
       String testFile = args[0];
       File tfile = new File(testFile);
       if (!tfile.exists())
       {
           System.out.println("Test  file " + testFile + " does not exist");
           System.exit(-1);
       }
       /*--------------------------------------------------------------------------------------*/
    
       LocalServerTest  serverTest = new LocalServerTest( configFile);
       int status = serverTest.getStatus();
       if (status != 1)
       {
           System.exit(-1);
       }
      serverTest.doFaceMatchLocalTests(testFile);
    }
}
