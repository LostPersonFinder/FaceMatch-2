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
import fmservice.server.entry.FMServiceManager;

import fmservice.server.result.FMServiceResult;
import  fmservice.server.util.PropertyLoader;
import fmservice.server.util.Timer;import fmservice.server.util.Utils;
import java.io.File;
;

import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 *
 */
public class AddClient
{
    
    private static Logger log  = Logger.getLogger(AddClient.class);

    
    static String fmConfigFile;                // For database connection, index store etc.
    private static FMServiceManager fmManager;       // singleton controller

    Properties testProperties;
    Timer initTimer = new Timer();
    
    public AddClient (String configFile) 
    {
        fmConfigFile = configFile;
        initFMSystem();
    }
    
    public void newClient(String clientName, String clientInfoFile)
    {     
         /**-----------------------------------------------------------------------------------------
         ** Send the request to the server 
         *------------------------------------------------------------------------------------------*/
        
       int status = 0;
        HashMap requestParams = new HashMap();
        requestParams.put(ServiceConstants.CLIENT_NAME_PARAM,  clientName);
        requestParams.put(ServiceConstants.CLIENT_INFO, clientInfoFile);
        requestParams.put("user", "fmadmin");
        requestParams.put("password", "fm-ops$$");

        FMServiceResult fmResult = fmManager.performService(
             ServiceConstants.ADMIN_SVC, ServiceConstants.ADD_FMCLIENT,  requestParams);

         // Print out the return information:
         if (!fmResult.isValidRequest())
             System.out.println("Invalid client requst; Error message: "+  fmResult.getStatus().statusMessage);
         else if (!fmResult.isSuccess())
             System.out.println("Request not successful; Error message: "+  fmResult.getStatus().statusMessage);
         else
         {
                System.out.println("AddClient test result:");
                System.out.println( fmResult.convertToJSONString());
         }
          System.out.println("*-----------------------------------------------------------------*");     
     }

        
      /**
      * Load in log4j config properties from  log4j.properties file using PropertyConfigurator.
      * Set the log file name as a system property to be used for logging  messages
      * from the application.
      *
    public static String  initLogging(Properties properties)
    {     
        String log4jConfProp = "";
        //Load in log4j config properties from  log4j.properties file
        log4jConfProp =  properties.getProperty("log4j.properties");

        // set the log file name as a 
        String log4jFilename = properties.getProperty("log4j.filename");
        if ( log4jConfProp == null || log4jFilename == null)
        {
            System.err.println("Missing log4jPropertyConfigurator and/or log file name in the input Properties");
            return null;
        }
        System.setProperty("log4j.filename", log4jFilename);
        PropertyConfigurator.configure(log4jConfProp);
        return log4jFilename;
    }*/
    
     public   int initFMSystem()
    {
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
        
     /*----------------------------------------------------------------------------------------------*/
     // Start the Facematch system by initializing various control objects
     /*------------------------------------------------------------------------------------------------*/ 
     protected  void  startFMServiceManager(String configFile)
     {
         fmManager=   FMServiceManager.createServiceManager(fmConfigFile);    
        int status = fmManager.getStatus();
       // float initTime = initTimer.getElapsedTime();           // in milliseconds
       // Timer.release(initTimer);
        if (status == 1)
        {
            log.info("\n"+
                     "**********************************************************************"
                + "\nFaceMatch system started up successfully in "+ 10 + " seconds"
                + "\n using configuration file: " + configFile  
               + "\n**************************************************************************" );
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

    /*----------------------------------------------------------------------------------------------*/
    public  static void main(String[] args) throws Exception
    {
        String defaultConfigFile = "";
        boolean localTest = true;
        boolean webTest = !localTest;
        
        
        // remote test implies talking to FaceMatch2 Web Server 
        String filename = localTest ? "FaceMatch2LocalTest.cfg" : "FaceMatch2WebTest.cfg";
        if (Utils.isWindows())
        {
            String dir = "<TopDir>/FM2Server/localTestDir";
            defaultConfigFile =  localTest ? (dir+"/FaceMatch2LocalTest.cfg") : 
                                                               (dir + "/FaceMatch2WebTest.cfg");
        }
        else        // assume Unix-based
        {
            String dir = "<TopDir>/FM2Server/localTestDir";
            defaultConfigFile=localTest ? (dir+"/FaceMatch2LocalTest.cfg") : 
                                                               (dir + "/FaceMatch2WebTest.cfg");
        }
  
          String[] clientNames; 
          String[] clientInfoFiles;
          
          if (args.length >= 2)
         {
             clientNames = new String[1];
             clientInfoFiles = new String[1];
             
            clientNames[0] = args[1];
            clientInfoFiles[0] = args[2];           // name of the SQL file
         }
        else
        {
                clientNames =  new String[] {"PL", "NCMEC"};
                clientInfoFiles = new String[] {"<TopDir>/FM2Server/installDir/config/clients/PL_ClientInfo.json",
                                                   "<TopDir>/FM2Server/installDir/config/clients/NCMEC_ClientInfo.json"};
        }
           
         String  testConfigFile =  (args.length > 2) ?  args[2] : defaultConfigFile;        
         
             // make sure that the file exists
        File file = new File(testConfigFile);
        if (!file.exists())
        {
            System.out.println("Test Configuration file " + testConfigFile + " does not exist");
            System.exit(-1);
        }
    
   
        AddClient addClient = new AddClient(testConfigFile);
        for (int i = 0; i < clientNames.length; i++)
        {
                addClient.newClient(clientNames[i], clientInfoFiles[i]);
        }
    }
}
