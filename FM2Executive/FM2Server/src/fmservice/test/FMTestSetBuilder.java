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

import fmservice.server.util.PropertyLoader;
import fmservice.server.util.Utils;
import fmservice.httputils.common.ServiceConstants;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Properties;

import java.util.HashMap;
import java.util.ArrayList;
import org.apache.log4j.PropertyConfigurator;

/**
 * This class  builds test sets from test file data for testing FaceMatch operations, which may be used
 * either locally on the server side or Remotely from an FM Test client that sends requests to the
 * server as HTTP-REST requests as in operational setting.
 * 
 * To avoid hardcoding of client keys into JSON parameter files, it allows specifying "clientName" as an 
 * alternative, which is then mapped to the key in the client's configuration properties file
 * 
 *
 */
public class FMTestSetBuilder implements ServiceConstants
{   
    
    public static String TESTID = "testId";
    public static String ANNOT_RESULT= "Expected Result";       // for comparison
    protected Properties testProperties;
   
         
    // invoked from another class which read the config file
    public FMTestSetBuilder(Properties confProperties)
    {
           testProperties = confProperties;
    }
    
    //-------------------------------------------------------------------
    // invoked from local a main class to read the test data file
    public FMTestSetBuilder(String fmTestConfigFile)
    {
        int status =  initialize(fmTestConfigFile);
        if (status == 0)
        {
            System.err.print("Could not access test config file " + fmTestConfigFile +". Exiting");
            System.exit(-1);
        }
    }
    
    /*--------------------------------------------------*/    
    
    protected   int initialize(String fmTestConfigFile)
    {
        try
        {
          testProperties = PropertyLoader.loadProperties( fmTestConfigFile);
          if (testProperties == null)
          {
              System.err.println("Exiting due to error in loading configuration data.");
              return 0;
          }
          initLogging(testProperties);
          return 1;
        }
        catch (Exception e)
        {
            System.err.println("Can't load configuration file " +  e.getMessage());
            return 0;
        }
    }
    
    /** initialize logging operation for the application.
     * 
     * @return log file name after successful open
     */
    protected String initLogging(Properties properties)
    {      
        // initialize logging properties and open log file
        String log4jConfProp = "";

        //Load in log4j config properties from  log4j.properties file
        log4jConfProp =  properties.getProperty("log4j.properties");

        // set the log file name as a 
        String log4jFilename = properties.getProperty("log4j.filename");
        if ( log4jConfProp == null || log4jFilename == null)
        {
            System.err.println("Missing log4jPropertyConfigurator and.or log file name in the input Properties");
            return null;
        }
        System.setProperty("log4j.filename", log4jFilename);
        PropertyConfigurator.configure(log4jConfProp);
        return log4jFilename;
    }
    
    /*----------------------------------------------------------------------------*/
    // Read the test dataset and  build an ArrayList of test parameters
    // An additional key indicating the service type, derived from the operation name, 
    // is added to the list of parameters provided in each test object.
    /*-----------------------------------------------------------------------------*/
    /** Get the test parameters for one or more FaceFinding 
     * operations to be performed.
     * 
     * @return: List of parameter sets (as a HashMap) for testing a set of
     *                  FaceFinding operations
     */
    public   ArrayList<HashMap> getTestParams(String testFileName)
    {
        // read the value of the following property from the fm configuration file
        if (testFileName == null || testFileName.isEmpty())
        {
            System.err.print("No Facematch test data file name provided");
            return null;
        }

       ArrayList<HashMap> testParamSet = getFaceMatchTestParams(testFileName);
       return testParamSet;  
    }      
           
   /*------------------------------------------------------------------------------------------------------*/
   // Get the list of test parameters from a given file provided in JSON format
   // ------------------------------------------------------------------------------------------------------*/
    protected ArrayList<HashMap> getFaceMatchTestParams(String faceMatcherTestFile)
    {
        JSONArray testArray =  Utils.readFileAsJSONArray(faceMatcherTestFile);
        if (testArray == null)
            return null;
        // Now decode each entry and create a HashMap
        int nt = testArray.size();
        ArrayList <HashMap> testList = new ArrayList();
        for (int i = 0; i < nt; i++)
        {
            
            // retieve the common parameters

            JSONObject testObject = (JSONObject) testArray.get(i); 
            Long testId =  ((Long) testObject.get(TESTID));
             if (testId == null)
                testId = new Long(-1);
            
            String serviceName =  ((String) testObject.get(SERVICE));
            String operationName =  ((String) testObject.get(OPERATION));
            
            HashMap paramMap = new HashMap();
            String clientKey = null;
            if (!operationName.equals(ADMIN))
            {
                clientKey = (String) testObject.get(CLIENT_KEY);
                if (clientKey == null)
                    clientKey = mapClientName2Key(testObject, testProperties);
            }
            paramMap.put(CLIENT_KEY, clientKey);

            if (serviceName.equalsIgnoreCase(ServiceConstants.EXTENT))
            {
                paramMap = getExtentOpsParams(testObject);
            }
             //-----------------------------------------------------------------------------
            else if (serviceName.equals(FACE_FIND))
            {
               paramMap = getFaceFinderTestParams(testObject); 
            }
            //-----------------------------------------------------------------------------
            else if (serviceName.equals(FACE_MATCH_REGION))
            {
                paramMap = getRegionMatcherTestParams(testObject);
            }
            //-----------------------------------------------------------------------------
            paramMap.put(TESTID,  testId);
            paramMap.put( SERVICE, serviceName);
            paramMap.put(OPERATION, operationName);
            if (clientKey != null)
                paramMap.put(CLIENT_KEY, clientKey);
            
            testList.add(paramMap);
        }
        return testList;
    }
    /*------------------------------------------------------------------------------------------------------*/
    // get list op parameters for an Image Related operation
    // *** Note: the parameter key and values must match the ICD ***
    /*------------------------------------------------------------------------------------------------------*/
    
    protected HashMap  getExtentOpsParams(JSONObject testObject)
    {
        HashMap paramMap = new HashMap();
        
        // common to all 
        String extentNameStr = (String)testObject.get(EXTENT_NAME_PARAM);
        paramMap.put(EXTENT_NAME_PARAM, extentNameStr);
        
        String clientKey = (String) testObject.get(CLIENT_KEY);
         if (clientKey == null)
            clientKey = mapClientName2Key(testObject, testProperties);
         paramMap.put(CLIENT_KEY, clientKey);
      
        String operation =   ((String) testObject.get(OPERATION));
        if (operation.equals(ADD_EXTENT))
        { 
            String description = (String)testObject.get(DESCRIPTION_PARAM);       
            if (description != null)
                 paramMap.put(DESCRIPTION_PARAM, description);
            // get Performance preference
            String  perfPref = (String)testObject.get(PERFORMANCE_PREF);
            if (perfPref != null)
                 paramMap.put(PERFORMANCE_PREF, perfPref);
        }
        else if (operation.equals(REMOVE_EXTENT)|| operation.equals(DEACTIVATE_EXTENT))
        {
                ;  // nothing more to add
        }
         else if (operation.equals(ACTIVATE_EXTENT))
        {
            String perfPref = (String)testObject.get(PERFORMANCE_PREF);
            if (perfPref != null)
                 paramMap.put(PERFORMANCE_PREF, perfPref);
        }
        else if (operation.equals(SET_PERFORMANCE_PREF))
        {
            String ffOption = (String)testObject.get(FF_OPTION);
            if (ffOption != null)
                 paramMap.put(FF_OPTION, ffOption);
        }
        return paramMap;
    }

    /*------------------------------------------------------------------------------------------------------*/
    // Get the list of parameters for a FaceFinding test
    // 
     protected HashMap  getFaceFinderTestParams(JSONObject testObject)
     {
         HashMap paramMap = new HashMap();
               
         String url  = (String) testObject.get(URL);
         paramMap.put(URL, url);
         
         String perfPref  = (String) testObject.get(PERFORMANCE_PREF);
         if (perfPref != null)
                paramMap.put(PERFORMANCE_PREF, perfPref);

         // optional param
        Double inflateBy = null;
        Number inflateNum  =  ((Number)testObject.get(INFLATE_BY));
        if (inflateNum != null)
        {
            inflateBy  =  new Double(inflateNum.doubleValue());
        }
        if (inflateBy != null)
             paramMap.put(INFLATE_BY, inflateBy);
        
        // Check if landmarks requested
        // Boolean landmarks  = (Boolean) testObject.get(LANDMARKS);
        Boolean landmarks = true;       // always ask for testing 
        if (landmarks != null && landmarks.booleanValue() == true)
             paramMap.put(LANDMARKS, "true");
        
         // Check if GPU not requested
        Boolean useGPU  = (Boolean)testObject.get(USE_GPU);
        if (useGPU != null)
             paramMap.put(USE_GPU, useGPU);

        JSONArray regionArray = (JSONArray)testObject.get("annotated region");
        String region = "";
        if (regionArray != null)
        {
            for (int i = 0; i < regionArray.size(); i++)
            {
                if (i > 0)
                    region = region.concat("\t");
                region  = region.concat(regionArray.get(i).toString());
            }    
            paramMap.put("annotated region", region);
        }
         
       return paramMap;
     }   
     
    /*-----------------------------------------------------------------------------------*/
    // Get the list of parameters for ImageMatcher ingest or query test
    //-------------------------------------------------------------------------------------
    protected HashMap  getRegionMatcherTestParams(JSONObject testObject)
    { 
        // get common parameters for all Face matcher operation
        HashMap paramMap = new HashMap();
        
        String operation =  ((String) testObject.get(OPERATION)).toLowerCase();
        if (operation.equals(QUERY_ALL))
        {
            String  extentNames =  (String) testObject.get(EXTENT_NAMES);
            paramMap.put(EXTENT_NAMES, extentNames);
        }
        else
        {
            String  extentName =  (String) testObject.get(EXTENT_NAME);
            paramMap.put(EXTENT_NAME, extentName);
        }
           //------ Get regions only for a subset of images  ------------
            // Really we don't need to parse and recreate it back in the same form!
        String  regions = (String) testObject.get(REGION);
        if (regions != null)
        {
            String[] regionArray = regions.split("\\]\\s*,\\s*");
            int len =  regionArray.length;
            String region = "";
            if (len == 1)
                    region = regionArray[0];
            else
            {
                for (int i = 0; i < len-1; i++)
                    regionArray[i] += "], ";
                for (int i = 0; i < len; i++)
                {
                    region += regionArray[i];
                    System.out.println("i = " + ", Region = " + region);
                }
            } 
            paramMap.put(ServiceConstants.REGION, region);
        }

        //----------------------------------------------------------------------
        if (operation.equals(REMOVE))
        { 
            // Must specify tthe tag of the ingest image which is to be removed       
            String imageTag = (String)testObject.get(IMAGE_TAG);
            paramMap.put(IMAGE_TAG, imageTag);
        }     
        //----------------------------------------------------------------------
        // parameters for ingest or query - get metadata
        //
        if (operation.equals(INGEST) || operation.equals(QUERY) || operation.equals(QUERY_ALL))
        {
           String gender = (String) testObject.get(GENDER);
           if (gender != null)
               paramMap.put(GENDER,  gender);

            // We should have either age or agegroup for a client
           String agegroup = (String)testObject.get(AGE_GROUP);
           if (agegroup != null)
               paramMap.put(AGE_GROUP, agegroup);
          
           Long age = (Long)testObject.get(AGE);
           if (age != null)
               paramMap.put(AGE, age.toString());

           // may not be for all clients 
           String location  = (String)testObject.get(LOCATION);
           if (location != null)
                paramMap.put(LOCATION, location);
           
          String perfPref  = (String) testObject.get(PERFORMANCE_PREF);
          if (perfPref != null)
                paramMap.put(PERFORMANCE_PREF, perfPref);
         }
        
        // specific parameters
        if (operation.equals(INGEST))
        {
            String url   =  (String)testObject.get(URL);
            paramMap.put(URL, url);
            
            String imageTag = (String)testObject.get(IMAGE_TAG);
            paramMap.put(IMAGE_TAG, imageTag);
        }

        else if (operation.equals(QUERY)  || operation.equals(QUERY_ALL) )
        {
            String queryUrl   =  (String)testObject.get(URL);
            paramMap.put(URL, queryUrl);
            
            Double tolerance;
            Number toleranceNum  =  ((Number)testObject.get(TOLERANCE));
            if (toleranceNum != null)
                tolerance  =  new Double(toleranceNum.doubleValue());
             else
                    tolerance = new Double(0.75);
            paramMap.put(TOLERANCE, tolerance.toString());

            Long maxMatches = (Long)testObject.get(MAX_MATCHES);
            if (maxMatches!= null)
                paramMap.put(MAX_MATCHES, maxMatches.toString());          // otherwise, get the client default
            else
                paramMap.put(MAX_MATCHES, "20");
        } 
          String expectedStatus = (String)testObject.get(ANNOT_RESULT);
          if (expectedStatus != null)
              paramMap.put(ANNOT_RESULT, expectedStatus);
          return paramMap;
      }     
 /*-----------------------------------------------------------------------------------------------*/
    protected String mapClientName2Key(JSONObject testObject, Properties testProperties)
       {
             String clientName =  (String) testObject.get("clientName");
             String clientKey = (String)testProperties.get(clientName+".key");
             return clientKey;
         }
  /*-----------------------------------------------------------------------------------------------*/    
    public  static void main(String[] args)
    {
        String testConfigFile = "";
        String testDataPath = "";
        if (args.length > 1)
        {
            testConfigFile = args[0];
            testDataPath = args[1];
        }
        else
        {
            String OS_NAME = System.getProperty("os.name").toLowerCase();
            String homeDir = (OS_NAME.contains("win") ) ?  "C:"  :   "<TestHomeDir>";   // windows or Linux
            testConfigFile = homeDir+"/FaceMatch2/FM2JavaClient/FM2WebClientApp.cfg";
            testDataPath =  homeDir+"/FaceMatch2/FM2JavaClient/testsets/testdata";
        }
        FMTestSetBuilder testBuilder = new FMTestSetBuilder(testConfigFile);
        
        // ArrayList<HashMap> testSets = testBuilder.getTestParams(testDataPath+"/facefind/FaceFinderTest1.json");
        //ArrayList<HashMap> testSets = testBuilder.getTestParams(testDataPath+"/regioningest/RegionIngestTestData.json");
        ArrayList<HashMap> testSets = testBuilder.getTestParams(testDataPath+"/extent/ExtentTestData.1.json");
      
       for (int i = 0; i < testSets.size(); i++)
       {
           System.out.println("Test case: " + i);
           System.out.println(testSets.get(i).toString());
       }
       System.out.println("-----------------------------------------------");
       System.exit(0);
    }
           

}
