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
*  Add a new Client to the FaceMatch2 system for conducting image  ingest and queries
* All associated information is provided in a JSON file.
*
* Note: This service may be invoked only when run by the FM2 System Administrator.
*
 */

package fm2client.app;

import  fm2client.app.FM2WebClient;
import  fm2client.core.FM2ServiceRequestor;
import  fm2client.core.FM2ServiceResult;

import fmservice.httputils.common.ServiceConstants;

import java.util.HashMap;
import java.io.File;
import org.apache.log4j.Logger;


public class AddClientApp extends FM2WebClient
{
    
    private static Logger log  = Logger.getLogger(AddClientApp.class);
     
     public AddClientApp( String testConfigFile) throws Exception
     {
         super (testConfigFile, true);                  // performed only invoking the FM2WebServer
     }
    
     //---------------------------------------------------------------------------------------------------
     // send a request to the FM2 Web server to add a new client to the system
     // whose characteristics are provided in the accompanying JSON file
     // Note: this is executed by the FM2 system administrator only
     //----------------------------------------------------------------------------------------------------
     protected  FM2ServiceResult addFM2Client(String clientName, String clientInfoFile)
     {
     
           String adminPW = testProperties.getProperty("fm2Admin.password");
           if (adminPW == null || adminPW.isEmpty())
           {
               log.error("Admin password property \"fm2Admin.Password\" not defined in the configuration file." );
               return null;
           }
            HashMap <String, Object>requestParams = new HashMap();
            requestParams.put("service", ServiceConstants.ADMIN_SVC);
            requestParams.put("operation", ServiceConstants.ADD_CLIENT);
            requestParams.put(ServiceConstants.CLIENT_NAME_PARAM,  clientName);
            requestParams.put(ServiceConstants.CLIENT_INFO, clientInfoFile);
            requestParams.put("user", "fmadmin");
            requestParams.put("password", testProperties.getProperty("fm2Admin.password"));
            
           FM2ServiceRequestor  serviceRequestor = new FM2ServiceRequestor(fm2ServerURL);
            int requestId = 0;
            
            FM2ServiceResult serviceResult = 
                serviceRequestor.requestAdminService(requestId, requestParams);
            log.info("Request completed to add  new client to FM2 system using data in " + clientInfoFile);
            System.out.println("**** Returned Result: \n"   + serviceResult.serverReponseContent);
            return  serviceResult;
       }
     //------------------------------------------------------------------------------------------------------------------------
     public  static void main(String[] args) throws Exception
    {
             if (args.length == 0)
            {
                System.out.println ("Please provide the FM2 Client name  and  (optionally) information file");
                System.exit(-1);
            }
          
        //-----------------------------------------------------------------------------------------------------------
        String clientName = args[0].replaceAll("\n\r", ""); 
          String clientInfoFile = "";
        if (args.length > 1)
            clientInfoFile = args[1].replaceAll("\n\r", ""); 
        else
             clientInfoFile = clientName + "_ClientInfo.json";
        System.out.println (">>> In AddClientApp main:  ClientName: " + clientName);
        System.out.println (">>> ClientInfo file: " + clientInfoFile);
        System.out.println("-------------------------------------------------------" );
        
        
        // files on the server's ops directory 
        String  testConfigFile =  null;
         if (args.length > 2) 
             testConfigFile =  args[2] ;
         else
             testConfigFile  =  getDefaultConfigFile();
         

         if ( ! (new File(testConfigFile)).exists())
         {
            System.out.println(" Test configuration file " + testConfigFile + " does not exist or  is not accessble.");
            System.exit(-1);
         }
        // connect to the FM2 server and send the request
        AddClientApp clientApp  = new AddClientApp(testConfigFile);
        if (!clientApp.isValidConnection())
        {
            System.out.println("Cannot talk to the FM2 server. Exiting.");
            System.exit(-1);
        } 
        FM2ServiceResult result = clientApp.addFM2Client(clientName,  clientInfoFile);

        System.out.println("------------------  AddClient status: " + result.statusCode + "--------------------------------");
        System.exit(1);
    }
}
