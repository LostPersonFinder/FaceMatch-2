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
package fmservice.httputils.common;

import fmservice.server.result.FMServiceResponse;

import fmservice.server.result.FaceRegion; 

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;




/**
 *
 * Miscellaneous FaceMatch Service related utilities.
 * 
 * Note: Each FaceMatch Service has one or more "Operations" associated with it.
 * 
 *
 */
public class ServiceUtils implements ServiceConstants
{
    private static Logger log = Logger.getLogger(ServiceUtils.class);
    
    public static String[] FM_ServiceNames = new String[] {
                    ADMIN, EXTENT, 
                    FACE_FIND, FACE_MATCH_REGION, WHOLE_IMAGE_MATCH};
     public static int[] FM_ServiceTypes = new int[] {
                    ADMIN_SVC, IMAGE_EXTENT_SVC, FACE_FIND_SVC, 
                    FACE_MATCH_REGION_SVC, WHOLE_IMAGE_MATCH_SVC};
     
     public static String[] ExtentOpsNames = new String[] {
                    ADD_EXTENT,  REMOVE_EXTENT, ACTIVATE_EXTENT, DEACTIVATE_EXTENT, SET_PERFORMANCE_PREF};
     public static int[] ExtentOpsTypes = new int[] {
                    ADD_EXTENT_OP,  REMOVE_EXTENT_OP, ACTIVATE_EXTENT_OP, DEACTIVATE_EXTENT_OP, SET_PERFORMANCE_OP};
         
     
     public static String[] FaceFindOpsNames = new String[] {GET_FACES};
     public static int[] FaceFindOpsTypes = new int[] { GET_FACES_OP};
     
     public static String[] RegionMatchOpsNames = new String[] {
         INGEST, QUERY, REMOVE, QUERY_ALL};      
     public static int[] RegionMatchOpsTypes = new int[] {
         REGION_INGEST_OP,  REGION_QUERY_OP,  REGION_REMOVE_OP, MULTIEXTENT_REGION_QUERY_OP};
     
       public static String[] WholeImageMatchOpsNames = new String[] {
            INGEST, QUERY, REMOVE};
       public static int[] WholeImageMatchOpsTypes = new int[] {
            WHOLEIMAGE_INGEST_OP,  WHOLEIMAGE_QUERY_OP, WHOLEIMAGE_REMOVE_OP};

/*----------------------------------------------------------------------------------------------------------*/
    /*
    * Return the integer  Service type for a FaceMatch service name.
    */
    public static int getServiceType(String serviceName)
    {   
        for (int i = 0; i < FM_ServiceNames.length; i++)
        {
            if (serviceName.equalsIgnoreCase(FM_ServiceNames[i]))
                return FM_ServiceTypes[i];
        }
        // no match
         return INVALID_SERVICE;
    }
    
    /*
    * Return the integer  Service type for a FaceMatch service name.
    */
    public static String convertServiceType2Name(int serviceType)
    {   
        for (int i = 0; i < FM_ServiceTypes.length; i++)
        {
            if (serviceType == FM_ServiceTypes[i])
                return FM_ServiceNames[i];
        }
        // no match
         return null;
    }
    /*----------------------------------------------------------------------------------------------------------*/
    
    /**
   * Return the integer Operation type for a FaceMatch service name/function
   */
    public static int getOperationType(int serviceType, String operation)
    {
        if (serviceType == IMAGE_EXTENT_SVC)
            return opsName2Type(ExtentOpsNames, ExtentOpsTypes, operation);
        if (serviceType == FACE_FIND_SVC)
            return opsName2Type(FaceFindOpsNames, FaceFindOpsTypes, operation);
        else if (serviceType == FACE_MATCH_REGION_SVC)
            return opsName2Type(RegionMatchOpsNames, RegionMatchOpsTypes, operation);
        else if (serviceType == WHOLE_IMAGE_MATCH_SVC)
            return opsName2Type(WholeImageMatchOpsNames, WholeImageMatchOpsTypes, operation);
        else
            return INVALID_SERVICE;
    }
    
        
    /**
   * Return the integer Operation type for a FaceMatch service name/function
   */
    public static String convertOperationType2Name(int serviceType, int operation)
    {
        if (serviceType == FACE_FIND_SVC)
            return opsType2Name(FaceFindOpsNames, FaceFindOpsTypes, operation);
        else if (serviceType == FACE_MATCH_REGION_SVC)
            return opsType2Name(RegionMatchOpsNames, RegionMatchOpsTypes, operation);
        else if (serviceType == WHOLE_IMAGE_MATCH_SVC)
            return opsType2Name(WholeImageMatchOpsNames, WholeImageMatchOpsTypes, operation);
        else
            return null;
    }
    /*----------------------------------------------------------------------------------------------------------*/
    
    protected static int opsName2Type(String[] opsNames, int[] opsTypes, String operation)
    {
        for (int i = 0; i < opsNames.length; i++)
        {
            if (opsNames[i].equalsIgnoreCase(operation))
                return opsTypes[i];
        }
        return INVALID_OPERATION;
    }
    
    protected static String  opsType2Name(String[] opsNames, int[] opsTypes, int operationType)
    {
        for (int i = 0; i < opsTypes.length; i++)
        {
            if (opsTypes[i] == operationType)
                return opsNames[i];
        }
        return null;
    }
      /*----------------------------------------------------------------------------------------------------------*/
    /** 
     * Build a Response object encapsulating the results of the requested FaceMatch 
     * operation (to be sent to the Client).
     * 
     * @param serviceType
     * @param operationType
     * @param resultParams
     * @return 
     */
    public static FMServiceResponse formatResponse(int  serviceName, int operationType,
            HashMap inputParam, HashMap resultParams)
    {
        FMServiceResponse fmResponse = null;
        // Format the headers and return results for each saervice/operation
        if (serviceName== FACE_FIND_SVC)
         fmResponse =  formatFaceFindingResponse(operationType, inputParam, resultParams);
        
        else if (serviceName == FACE_MATCH_REGION_SVC)
           fmResponse =  formatFaceMatchingResponse(operationType, inputParam, resultParams);
        else if (serviceName == WHOLE_IMAGE_MATCH_SVC)
           fmResponse = null ; //  formatWholeImageMatchingResponse(operationType, inputParam, resultParams);
         return fmResponse; 
         
    }
    /*-------------------------------------------------------------------------------------------------------*/
     /** 
     * Build a Response object for FaceFinding operation 
     * 
     * @param operationType
     * @param resultParams
     * @return 
     * 
     *  Header contains  Service type and  operation
     * Parameters are: input imageURL/ID, 
     */
    public static FMServiceResponse formatFaceFindingResponse( int operationType,
            HashMap <String, Object>  inputParams, HashMap resultParams)
    {          
        if (operationType != GET_FACES_OP)
            return null;
        
        // Create the Response message  the status code and reason phrase
       int statusCode = (Integer) resultParams.get(STATUS_CODE);
       String statusMessage = (String)resultParams.get(STATUS_MESSAGE);
               
        // set standard header with Service,operation
       FMServiceResponse  fmResponse = new FMServiceResponse(statusCode, statusMessage);
       fmResponse.returnHeader= buildResponseHeader ("FACE_FIND", "getFacesForUI");
        
        // additional  header
         ArrayList<FaceRegion> queryRegions = 
             (ArrayList<FaceRegion>) resultParams.get(FACE_REGIONS);
        Integer numRegions = (queryRegions == null || queryRegions.isEmpty()) ? 
                            0 : queryRegions.size();
        fmResponse.returnHeader.put("numFaces", numRegions);
        if (numRegions == 0)
            return fmResponse;          // no content
        
        // Create a JSON object with the output results
        JSONObject jsonObj = new JSONObject();
        
        // add the face Regions with landmarks
        JSONArray regionArray = new JSONArray();
        for (int i = 0; i < queryRegions.size(); i++)
            regionArray.add(queryRegions.get(i).convertToJSONObject());
        jsonObj.put(FACE_REGIONS,  regionArray);
        
        // add the  percentage of inflation
        jsonObj.put(INFLATE_BY, (Double)resultParams.get(INFLATE_BY));
        
        // add the inflated region data
         ArrayList<FaceRegion> displayRegions = 
             (ArrayList<FaceRegion>) resultParams.get(DISPLAY_REGIONS);
        JSONArray displayArray = new JSONArray();
        for (int i = 0; i < displayRegions.size(); i++)
            displayArray.add(displayRegions.get(i).convertToJSONObject());
        jsonObj.put(DISPLAY_REGIONS,  displayArray);
        
        // add the performance data
        jsonObj.put (URL_FETCH_MILLI,   (Integer)resultParams.get(URL_FETCH_MILLI));
        jsonObj.put (FM_PROCESS_MILLI,   (Integer)resultParams.get(FM_PROCESS_MILLI)  );
        
         fmResponse.returnContent = jsonObj.toString();
         return fmResponse;
    }
   
    /*-------------------------------------------------------------------------------------------------------------/
    /**
    * Build a Response object for FaceMatching operation.
     * 
     * @param operationType
     * @param resultParams
     * @return 
    */
     public static FMServiceResponse formatFaceMatchingResponse( int operationType,
            HashMap <String, Object>  inputParams, HashMap resultParams)
    {
      // Create the Response message  the status code and reason phrase
       int statusCode = (Integer) resultParams.get(STATUS_CODE);
       String statusMessage = (String)resultParams.get(STATUS_MESSAGE);
               
        // set standard header with Service,operation
       FMServiceResponse  fmResponse = new FMServiceResponse(statusCode, statusMessage);
       if (operationType == REGION_INGEST_OP)
       {
            fmResponse.returnHeader= buildResponseHeader (FACE_MATCH_REGION, INGEST);
       }
      else if (operationType == REGION_QUERY_OP)
       {
            fmResponse.returnHeader= buildResponseHeader (FACE_MATCH_REGION, QUERY);
       }
       else if (operationType == ServiceConstants.REGION_REMOVE_OP)
       {
            fmResponse.returnHeader= buildResponseHeader (FACE_MATCH_REGION, REMOVE);
       }
       return fmResponse;
    }
       
    
    /*-------------------------------------------------------------------------------------------------------------*/
    // Build the header of an FMResponse message with service name and operation type
    /*-------------------------------------------------------------------------------------------------------------*/         
    protected static HashMap <String, String> buildResponseHeader(String service, String operation)
    {
        HashMap<String, String> headerMap = new HashMap();
        headerMap.put("Service", service);
        headerMap.put("Operation", operation);
        return headerMap;
    }
    
     /*-------------------------------------------------------------------------------------------------------------*/
    //Format an Invalid message with given error code anf message as a HashMap
    /*-------------------------------------------------------------------------------------------------------------*/         
    public static HashMap <String,  Object>formatInvalidReqResponse (int errorCode, String errorMsg)
    {
        HashMap <String,  Object> errorMap = new HashMap();
        errorMap.put(STATUS_CODE, String.valueOf(errorCode));
        errorMap.put(STATUS_MESSAGE,  errorMsg);
        return errorMap;
    } 
    
    
      
   //-------------------------------------------------------------------------------------------------------------
   // Return the server's result in the HTTPResponse as a formatted set of strings for printing
   // The result  has the following elements
   //   public int statusCode
   //   public String statusMsg;
   //   public String  returnContent;          
   //-------------------------------------------------------------------------------------------------------------
   public static ArrayList<String> formatServerResponse(ResponseMessage serverMessage)
   {
       ArrayList<String> formattedResponse = new ArrayList();
       formattedResponse.add("Status code: " + serverMessage.status + ", message:  " +  serverMessage.returnMsg);
       
       JSONParser parser = new JSONParser();
       try
       {
            JSONObject retObj = (JSONObject)parser.parse(serverMessage.returnMsg);
            Iterator <String> it = retObj.keySet().iterator();
            while (it.hasNext())
            {
                String key = it.next();
                String value =  retObj.get(key).toString();
                formattedResponse.add ( key +" " + value);
            }
       }
       catch (ParseException pe)
       {
            formattedResponse.add("InvalidJSON format for server rerurned data: \n" +  serverMessage.returnMsg);
       }
       return  formattedResponse;
   }
   
   // Format the server response as a  single "formatted" String for pinting
      public static String formatServerResponse2String(String serviceMessage)
      {
         try
        {
            String formattedMsg = "";
            JSONParser jparser = new JSONParser();
            JSONObject jobj = (JSONObject)jparser.parse(serviceMessage);
             Iterator <String> it = jobj.keySet().iterator();
            while (it.hasNext())
            {
                String key = it.next();
                String value =  jobj.get(key).toString();
                formattedMsg+=  ( "\n" + key +":  " + value);
            }
            return formattedMsg;  
        }
        catch (Exception e)
        {
             log.warn("Invalid JSON record received from server: " +  e.getMessage());
            return "";
        }
      } 
}
