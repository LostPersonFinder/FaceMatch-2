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
* FaceMatch Client class to create single request from user parameters and send it to the Server,
* wait for the response and return it to the caller.
* Note: This class does not verify the request parameters, 
*  that must be performed by the caller 
*/

package fm2client.core;


import fm2client.core.FM2ServiceResult;
import fmservice.httputils.client.ClientRequest;
import fmservice.httputils.common.FormatUtils;
import fmservice.httputils.common.ServiceConstants;

import fm2client.util.Timer;

import java.awt.Rectangle;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.http.HttpResponse;

import org.apache.log4j.Logger;

public class FM2RequestAgent implements ServiceConstants
{
    private Logger log = Logger.getLogger(FM2RequestAgent.class);
    
    public static int HTTP_OK = ClientRequest.HTTP_OK;
    
    protected String webServerURL;           // Server URL to send requests
    protected String ffWebServiceURL;       // face find request
    protected String fmWebServiceURL;       // face region match request
    
    protected String imageWebServiceURL;
    protected String extentWebServiceURL;
    protected String infoWebServiceURL;
    protected String adminServiceURL;
    
    protected boolean debug = true;
    
    protected String requestURL = "";
    
    public FM2RequestAgent( String fm2ServerURL)
    {
        webServerURL = fm2ServerURL;                                     // main url to connect to
        ffWebServiceURL =webServerURL+"/ffind";                            // for face related operations
        fmWebServiceURL =webServerURL+"/reg";                            // for face related operations
        imageWebServiceURL =webServerURL+"/fm-image";    // for whole image related operations
        extentWebServiceURL =webServerURL+"/extent";
        infoWebServiceURL =webServerURL+"/info";              // for retrieving information from Server 
        adminServiceURL =  webServerURL+"/admin";
    }
    
    
    
    
   /*-------------------------------------------------------------------------------------------------------------*/  
    // Send a request to add a new Client for FaceMatch2 Services
    // Note: This request is sent by the FM2 Administrator only
    /*-------------------------------------------------------------------------------------------------------------*/  
   public   FM2ServiceResult executeAdminRequest( String operation, HashMap requestParams)  
   {
        return sendReqNGetResponse(ServiceConstants.ADMIN_SVC, adminServiceURL, operation, requestParams);
    }
   
     /*-------------------------------------------------------------------------------------------------------------*/  
    // Send a request to add a new Extent for a previously created Client 
    /*-------------------------------------------------------------------------------------------------------------*/  
   public   FM2ServiceResult executeExtentRequest( String operationType,
       HashMap <String, Object>requestParams)  
   {
       String[] operations = 
                {ADD_EXTENT, REMOVE_EXTENT, ACTIVATE_EXTENT, DEACTIVATE_EXTENT, SET_PERFORMANCE_PREF};
                  
        int service = ServiceConstants.IMAGE_EXTENT_SVC;
       String operation = null;
       for (int i  =0; operation == null && i < operations.length; i++)
       {
           if (operations[i].equalsIgnoreCase(operationType))
              operation = operations[i];
       }
       if (operation == null)
       {
           return new FM2ServiceResult(service, operationType,  FM2ServiceResult.INVALID_REQUEST, 
                 "Invalid operation request: " +operationType, null);
       }
        // valid operation  - send request to the FM2 WebServer
       
       requestURL = extentWebServiceURL;
         FM2ServiceResult reqResult  = sendReqNGetResponse( service,
           requestURL, operation, requestParams);
         return reqResult;
    } 
    /*-----------------------------------------------------------------------------------------------------*/ 
    // Perform individual FaceMatch Service request  - 
    // Return the server response as String to the caller
    // Note: The service, operation and input parameters must match the
    // specification in the ICD (and/or Web.xml )
 
    /*-------------------------------------------------------------------------------------------------------------------------*/
      public  FM2ServiceResult executeFaceFindingRequest( String operation, HashMap<String, Object> inputParams)
      {
            // send the request and receive response
            int service = ServiceConstants.FACE_FIND_SVC;
            String requestURL = ffWebServiceURL;
            return sendReqNGetResponse(service, requestURL, operation, inputParams);
      }
      
     /*----------------------------------------------------------------------------- -----*/ 
     // Perform individual FaceMatch Region service by sending the data in the
     // input parameters to the server
    /*-----------------------------------------------------------------------------------*/    
      public   FM2ServiceResult executeFaceRegionRequest(
        String operation,  HashMap<String, Object>reqParams)

     {
         int service = ServiceConstants.FACE_MATCH_REGION_SVC;
         String requestURL =  fmWebServiceURL;
         // send the request
         return sendReqNGetResponse( service, requestURL, operation, reqParams);
      }
     
    /*----------------------------------------------------------------------------------------------------- -----*/ 
    // Perform individual requests to retrieve database information about an
    // FM2 client, Image Extent or image.
    // input parameters to the server
     // reqId  is a handle provided by the caller to associate the output with the call
   /*-------------------------------------------------------------------------------------------------------------*/

   public   FM2ServiceResult executeReportRequest(
       String operationType, HashMap<String, Object> reqParams)      
   {
       return null;
   }
  
   public String getFullServicePath(String service, String operation)
   {
       return "";
   }   
 
/********************************************************************************************/
// send the request and wait for the receive response
//
// This is the main method which uses the ClientRequest class to send the service
// request to the FM2WebServer, receives the results, decodes the status and the data  
// from the response and returns it to the caller.
/********************************************************************************************/
 public FM2ServiceResult sendReqNGetResponse( int service, String serviceURL,
            String operation,   HashMap<String, Object> inputParams)
    {  
        FM2ServiceResult  serviceResult;
         if (!FormatUtils.isValidURL (serviceURL))
        {
            log.error("Invalid server URL: "+ serviceURL +" specified" );
            serviceResult = new FM2ServiceResult(FM2ServiceResult.INVALID_REQUEST);
            return serviceResult;
        }

         Timer t1 = null;
         if (operation.equals(ServiceConstants.QUERY))
        {
            // Check time taken for query operation by the client
            t1  = new Timer();
            System.out.println(">> Query started at:" + Calendar.getInstance().getTime());
        }
         HttpResponse  serverResponse = ClientRequest.sendGetRequest(
            serviceURL, operation, inputParams);

         if (serverResponse == null)
         {
             log.error ("No response received from server after sending request.");
             return new FM2ServiceResult (service, operation,  -1, null);
             
         }
         int httpCode = serverResponse.getStatusLine().getStatusCode();
         if (httpCode != HTTP_OK)
         {
            String httpMessage = serverResponse.getStatusLine().getReasonPhrase();
            log.error ("HTTP Error in receiving response from  " + webServerURL +
                "\n HTTP code = " + httpCode +", " + httpMessage);
            
            return new FM2ServiceResult (service, operation,  httpCode, httpMessage);
         }
          if (t1 != null)
        {
            System.out.println(">> Query roundtrip time in msec: " + t1.getElapsedTime()) ;
        }
        // decode the information returned in the response body as the content
        byte[] returnData = ClientRequest.getResponseContents(serverResponse);
        String resultStr = new String(returnData);
        if (debug)
            System.out.println("FM2 Server returned data: \n" + resultStr);
        return new FM2ServiceResult(service, operation, 1, "Request Successful",  resultStr);
      }  

  /*--------------------------------------------------------------------------------------------------------------
  // Check if the the Web Server currently running
  *--------------------------------------------------------------------------------------------------------------*/
    public boolean isValidConnection()
    {
        HttpResponse serverResponse = ClientRequest.sendGetRequest(webServerURL, "status", null);
        if (serverResponse == null)
        {
             log.error ("Could not connect to the FaceMatch Server " + webServerURL );
             return false;
        }
        
        int statusCode = serverResponse.getStatusLine().getStatusCode();
        boolean isSuccess =  (statusCode == HTTP_OK);
        if (!isSuccess)
        {
           String statusMessage = serverResponse.getStatusLine().getReasonPhrase();
           log.error ("Error returned from FaceMatch Server " + webServerURL +
               "\n Status code = " + statusCode +", " + statusMessage );
        }
        return isSuccess;
    }
    
    
    public String getRequestURL()
    {
        return requestURL;
    }
 }