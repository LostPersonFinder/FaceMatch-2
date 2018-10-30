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
* This class is responsible for sending any type of single request to the FM2 server and getting back the result.
* It is invoked from other classes (APP) classes to provide necessary information.
 */

package fm2client.core;

import fmservice.httputils.common.ServiceConstants;
import java.util.HashMap;
import org.apache.log4j.Logger;

public class FM2ServiceRequestor implements ServiceConstants
{
        private static Logger log  = Logger.getLogger(FM2ServiceRequestor.class);
        protected String fm2ServerURL;
        protected FM2RequestAgent serviceRequestor;
        
        public String[] ADMIN_OPERATIONS = {ADD_CLIENT,  GPU_STATUS};
        
        public String[]  EXTENT_OPERATIONS = 
                {ADD_EXTENT, REMOVE_EXTENT, ACTIVATE_EXTENT, DEACTIVATE_EXTENT, SET_PERFORMANCE_PREF};
        
        public String[]  FACE_FIND_OPERATIONS = {GET_FACES};
        
         public String[]  FACE_REGION_OPERATIONS = {INGEST, REMOVE, QUERY, QUERY_ALL};
        
        
        protected boolean formattedPrint = true;         // print results as returned or as decoded
        
        /*--------------------------------------------------------------------------------------------*/
        
        public FM2ServiceRequestor(String fm2URL)
        {
            fm2ServerURL = fm2URL;
            serviceRequestor = new FM2RequestAgent(fm2ServerURL);
        }
        
        
        // For batch-based operation, we maynot want to decode the results,which is done elsewhere
        public void  setFormattedPringt(boolean doFormat)
        {
            formattedPrint = doFormat;;
        }
        
     //---------------------------------------------------------------------------------------------------
     // send a request to the FM2 Web server to add a new client to the system
     // whose characteristics are provided in the accompanying JSON file
     // Note: this is executed by the FM2 system administrator only
     //----------------------------------------------------------------------------------------------------
     public  FM2ServiceResult requestAdminService(int testNum, HashMap inputParams)
     {
        System.out.println("-----------------------------------------------");
        System.out.println("Test case: " + testNum+ ", Input parameters: ");
        System.out.println(inputParams.toString());

       String operation =  (String)inputParams.get("operation");
       if (!isValidOperation(ADMIN_SVC, operation, ADMIN_OPERATIONS))
       {
           return new FM2ServiceResult(ADMIN_SVC,  operation,  FM2ServiceResult.INVALID_REQUEST, 
                 "Invalid operation request: " +operation, null);
       }
       
       if (operation.equals(ADD_CLIENT))
       {       
            HashMap <String, Object>requestParams = new HashMap();
             // make sure that we have provided the client name 
             if (inputParams.get(CLIENT_NAME_PARAM)  == null)
            {
                log.error("Missing Client Key in the input data.");
                 return new FM2ServiceResult(ADMIN_SVC, operation,  FM2ServiceResult.INVALID_REQUEST, 
                    "Missing Client Name  in the input data."  +operation, null);
            }
             else if (inputParams.get(ServiceConstants.CLIENT_INFO)  == null)
            {
                log.error("Missing Client Key in the input data.");
                 return new FM2ServiceResult(ADMIN_SVC, operation,  FM2ServiceResult.INVALID_REQUEST, 
                    "Missing Client Information file name the input data."  +operation, null);
            }
       }
        HashMap requestParams = removeExtraParams( inputParams);
        FM2RequestAgent serviceRequestor = new FM2RequestAgent(fm2ServerURL);
        FM2ServiceResult serviceResult = 
                 serviceRequestor.executeAdminRequest(operation, requestParams);
        printServiceResult(testNum, serviceResult, formattedPrint);
        return serviceResult;
     }   
         
    /*---------------------------------------------------------------------------------------*/ 
     // Perform individual FaceFinder test - 
     // Return the server response as Strinf to the caller
    /*---------------------------------------------------------------------------------------*/
    public  FM2ServiceResult  requestExtentOpsService( int testNum, HashMap inputParams)
    {
        System.out.println("-----------------------------------------------");
        System.out.println("Test case: " + testNum+ ", Input parameters: ");
        System.out.println(inputParams.toString());

       String operation =  (String)inputParams.get("operation");
       if (!isValidOperation(IMAGE_EXTENT_SVC, operation,  EXTENT_OPERATIONS))
       {
           return new FM2ServiceResult(IMAGE_EXTENT_SVC,  operation,  FM2ServiceResult.INVALID_REQUEST, 
                 "Invalid operation request: " +operation, null);
       }
         // make sure that we have provided the client key
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

     /*---------------------------------------------------------------------------------------*/ 
     // Perform individual FaceFinder test - 
     // Return the server response as String to the caller
    /*---------------------------------------------------------------------------------------*/
     public FM2ServiceResult requestFaceFinderService( int testNum, HashMap inputParams)
     {
        System.out.println("-----------------------------------------------");
        System.out.println("Test case: " + testNum+ ", Input parameters: ");
        System.out.println(inputParams.toString());
         
        String operation =  (String)inputParams.get("operation");
       if (!isValidOperation(FACE_FIND_SVC, operation, FACE_FIND_OPERATIONS))
       {
           return new FM2ServiceResult(FACE_FIND_SVC,  operation,  FM2ServiceResult.INVALID_REQUEST, 
                 "Invalid operation request: " +operation, null);
       }
         // make sure that we have provided the client key
         if (inputParams.get(ServiceConstants.CLIENT_KEY)  == null)
        {
            log.error("Missing Client Key or Name in the input data.");
             return new FM2ServiceResult(FACE_FIND_SVC, operation,  FM2ServiceResult.INVALID_REQUEST, 
                "Missing Client Key  in the input data."  +operation, null);
        }
         
         
        HashMap requestParams = removeExtraParams( inputParams);
        FM2ServiceResult serviceResult = 
                 serviceRequestor.executeFaceFindingRequest(operation, requestParams);
        printServiceResult(testNum, serviceResult, formattedPrint);
        return serviceResult;
     }   

    /*----------------------------------------------------------------------------- -----*/ 
     // Perform individual FaceMatch Region  test
    /*-----------------------------------------------------------------------------------*/
     public FM2ServiceResult requestFaceMatchRegionService( int testNum, HashMap inputParams)
     {
         System.out.println("-----------------------------------------------");
         System.out.println("Test case: " + testNum+ ", Input parameters: ");
         System.out.println(inputParams.toString());

       
        String operation =  (String)inputParams.get("operation");
       if (!isValidOperation(ServiceConstants.FACE_MATCH_REGION_SVC, operation, FACE_REGION_OPERATIONS))
       {
           return new FM2ServiceResult(FACE_MATCH_REGION_SVC,  operation,  FM2ServiceResult.INVALID_REQUEST, 
                 "Invalid operation request: " +operation, null);
       }
         // make sure that we have provided the client key
         if (inputParams.get(ServiceConstants.CLIENT_KEY)  == null)
        {
            log.error("Missing Client Key or Name in the input data.");
             return new FM2ServiceResult(FACE_MATCH_REGION_SVC, operation,  FM2ServiceResult.INVALID_REQUEST, 
                "Missing Client Key  in the input data."  +operation, null);
        }

        // remove the parameters that are not for the server (only for the test client) 
        HashMap requestParams = removeExtraParams(inputParams);

        /*-----------------------------------------------------------------------------------------
         ** Send the request to the server 
         *------------------------------------------------------------------------------------------*/ 
          FM2ServiceResult serviceResult =
              serviceRequestor.executeFaceRegionRequest(operation, inputParams);
          
          printServiceResult( testNum, serviceResult, formattedPrint); 
          return serviceResult;
     }
     
    /* -----------------------------------------------------------------------------------------------
    *  Common functions
    *----------------------------------------------------------------------------------------------*/    
   
  /*-----------------------------------------------------------------------------------------
 * Check that the fiven operation is valid for a saervice
 *------------------------------------------------------------------------------------------*/    
       public boolean isValidOperation(int service,String operation, String[] validOperations)
     {
           boolean validOper = false;
            for (int i  = 0;  i < validOperations.length; i++)
            {
                if (operation.equalsIgnoreCase(validOperations[i]))
                    return true;
            }
            return false;
     }   
     
    /*-----------------------------------------------------------------------------------------------------------------------
    // Remove extra entries from the reqiest parameter list, to be provided to the server
    /*-----------------------------------------------------------------------------------------------------------------------*/
    protected HashMap removeExtraParams(HashMap inputParams)
    {
        HashMap requestParams =  (HashMap) inputParams.clone();
    
        requestParams.remove("operation");        // not a parameter to the server
        requestParams.remove("service");
        if (requestParams.get("testId")  != null)
            requestParams.remove("testId");
        return requestParams;
    }
    
        
    public static int  printServiceResult(int testNum, FM2ServiceResult serviceResult, boolean formatted)
    {
       if (testNum >= 0)
           System.out.println("Test Number = " + testNum);
        if (!formatted)           // simply return the way it is
            System.out.println(serviceResult.toString());
        else
         ;  // Print it as a decoded JSON object
       return 1;
    }
}
