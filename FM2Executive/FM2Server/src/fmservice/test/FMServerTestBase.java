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

import fmservice.server.entry.FMServiceManager;
import fmservice.server.util.Utils;
import java.util.Properties;
 

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 *
 */
public class FMServerTestBase
{
    private static Logger log = Logger.getLogger(FMServerTestBase.class);
    
    protected static String fmConfigFile;                // For database connection, index store etc.
    protected static FMServiceManager fmManager;       // singleton controller
    
    protected Properties testProperties;

   /*-----------------------------------------------------------------------------------------------------*/   
    
    public FMServerTestBase(String configFile)
    {
        int status =  initFMSystem(configFile);
    }
    
    protected   int initFMSystem(String configFile)
    {
        fmConfigFile = configFile;
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
    /*-----------------------------------------------------------------------------------*/
     
     public static String  getConfigFile(boolean localTest)
     {
        String defaultConfigFile = "";
        
        // remote test implies talking to FaceMatch2 Web Server 
        String configFileName = localTest ? "/FaceMatch2LocalTest.cfg" :  "/FaceMatch2WebTest.cfg";

        String configDir = "<TopDir>/FM2Server/localTestDir";
        defaultConfigFile = configDir+"/"+configFileName;
    
        // make sure that the file exists
        File file = new File(defaultConfigFile);
        if (file.exists())
            return defaultConfigFile;
        else
        {
            System.out.println ("Configuration file " + defaultConfigFile + " does not exist");
            return null;
        }
     }
       public  static String  getHomeDir()
       {
           return "<TopDir>";
       }
     
}
