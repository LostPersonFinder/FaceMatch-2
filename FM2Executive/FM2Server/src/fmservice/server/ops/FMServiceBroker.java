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
package fmservice.server.ops;

import fmservice.server.entry.FMServiceManager;

import fmservice.httputils.common.ServiceConstants;
import fmservice.httputils.common.FormatUtils;

import fmservice.server.util.AgeGroupAllocator;
import fmservice.server.result.FMServiceResult;
import fmservice.server.result.FaceRegion;
import fmservice.server.result.Status;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * <P>
 FMServiceBroker provides a bridge between the Web services (servlets) and 
 FaceMatch Service providers. It verifies the parameters coming from the client Apps
 and passes them to the FaceMatch Service manager for image match related work. 
 It also decodes and formats the response returned from the corresponding  FaceMatch 
 operation to be  returned  to the client. 
 * 
 * Note: Since the requests are retrieved from Web HTTPRequest, the HashMaps are
 * in the form <String, String>, multivalued parameters (e.g. face regions) being concatenated 
 * to a single string
 </P>
 * 
 * @version $Revision: 1.1
 * 
 *
 */
public class FMServiceBroker implements ServiceConstants
{

    /** log4j category */
    private static Logger log = Logger.getLogger(FMServiceBroker.class);
    
    private static String anChars = "[a-zA-Z0-9\\.\\-_]+";       // alphaNumeric  chars plus: ' -' and '_ ', and '.'
   //private static final Pattern INVALID_CHARS_PATTERN = 
   //                            Pattern.compile("^.*[~#@*+%{}<>\\[\\]|\"\\].*$");
    
    /*----------------------------------------------------------------------------------------------------------------------*/ 
    /**
     * Process the image operation service request coming from any  caller and 
     * return the response.
     * 
     * @param serviceType
     * @param operation
     * @param inputParams
     * @return Result object with processing information and data to be returned to the caller
     */
    public FMServiceResult  processServiceRequest(int serviceType, int operation, 
        HashMap inputParams)
    {
        log.info("Received request: Service=" + serviceType +", operation=" + operation + 
                ", parameters="+inputParams.toString() );
        
         Status inputStatus = verifyInputParams(serviceType, operation, inputParams);
         if (!inputStatus.isSuccess())
            return  new FMServiceResult(serviceType, operation, inputStatus);
     
        FMServiceManager fmServiceManager = FMServiceManager.getFMServiceManager();
        FMServiceResult result  = fmServiceManager.performService(
                           serviceType, operation, inputParams);

        // Format the results to be returned to the client App.
        return result;
    }

       /*----------------------------------------------------------------------------------------------------------------------*/ 
        /**
         * Verify that the input parameters provided in the incoming request are valid, 
         * based upon the service and operation types
         * 
         * @param serviceType
         * @param operation
         * @param inputParams
         * @return 
         */
        protected Status verifyInputParams (int serviceType, int operation, HashMap inputParams)
        {
            switch (serviceType)
            {
                case IMAGE_EXTENT_SVC:
                    return verifyExtentOpsServiceParams(operation, inputParams);
                    
                case FACE_FIND_SVC:
                    return verifyFaceFinderServiceParams(operation, inputParams);
                //
                case FACE_MATCH_REGION_SVC:
                    return verifyRegionMatcherServiceParams(operation, inputParams);

                case WHOLE_IMAGE_MATCH_SVC:
                     return verifyWholeImageMatcherServiceParams(operation, inputParams);
                //
                case DATABASE_QUERY_SVC:
                    return verifyDatabaseQueryServiceParams(operation, inputParams);
                 
                 default:
                      return new Status(INVALID_OPERATION, "Unknown invalid service request \"" + serviceType +"\"");
            }
        }  
        
    /*---------------------------------------------------------------------------------------------------------*/
     public Status verifyExtentOpsServiceParams(int operation, HashMap inputParams)
     { 
         
         String validPerfOptions = SPEED+"||"+ACCURACY+"||"+OPTIMAL+"||"+PROGRESSIVE;
        
        // All operation requests must have Client's key and the Extent's Alphanumeric name 
        //  (with no spaces or other such characters  
         //
        String clientKey = (String) inputParams.get(CLIENT_KEY);
        String extentName =  (String) inputParams.get(EXTENT_NAME_PARAM);
        if (clientKey == null || clientKey.isEmpty())
        {
             return new Status(MISSING_CLIENT_KEY, " Missing Client Key");
        }
        else if (extentName == null || extentName.isEmpty())
            return new Status( MISSING_PARAM, " Missing extent name: " + EXTENT_NAME_PARAM);

        if (operation == ADD_EXTENT_OP || operation == ACTIVATE_EXTENT_OP)
        {
            // check if performance option is specified
            String option =  (String) inputParams.get(PERFORMANCE_PREF);
            if (option != null && !option.matches(validPerfOptions))         // not specified: okay we use default
           {
               return new Status(INVALID_PARAM, "Face detection performace preferences must be one of "
                    + SPEED + ", "+ ACCURACY + ", " + OPTIMAL +", " + PROGRESSIVE);
           }
        }
        if (operation ==SET_PERFORMANCE_OP)
        {
            // check  preference settings for face detection and face matching
           String option  = (String) inputParams.get(FF_OPTION);
           if (option == null ||  !option.matches(validPerfOptions))         // not specified: okay we use default
           {
               return new Status(INVALID_PARAM, "Face detection  performace preferences must be one of "
                   + SPEED + ", "+ ACCURACY + ", " + OPTIMAL +", " + PROGRESSIVE);
           }
        }
        return new Status(SUCCESS, "");
     }
        
    /*---------------------------------------------------------------------------------------------------------*/
  public Status verifyFaceFinderServiceParams(int operation, HashMap inputParams)
  {
        if (operation == GET_FACES_OP)
        {
            String clientKey = (String) inputParams.get(CLIENT_KEY);
            if (clientKey == null || clientKey.isEmpty())
            {
                 return new Status(MISSING_CLIENT_KEY, " Missing Client Key");
            }

            String url = (String) inputParams.get(URL);
            if (url == null)
                 return new Status(MISSING_PARAM, " Missing image file URL: " + URL);
                
            else if ( !FormatUtils.isValidURISyntax(url))
                 return new Status( INVALID_PARAM,"Invalid Image file URL: " + url);
            
            String regions = (String)  inputParams.get("regions");
            if (regions != null && !FaceRegion.isValidRegionFormat(regions))
                 return new Status (INVALID_PARAM, "Invalid  Image region specified in: " + regions); 
        }
       return new Status(SUCCESS, ""); 
    }
   
   /*---------------------------------------------------------------------------------------------------*/  
    public  Status  verifyRegionMatcherServiceParams(int operation, HashMap inputParams)
    {
        try
        {
            String clientKey = (String) inputParams.get(CLIENT_KEY);
            if (clientKey == null || clientKey.isEmpty())
            {
                 return new Status(MISSING_CLIENT_KEY, " Missing Client Key");
            }

           
           // optional parameter - coordinates of a face annotated by humans or by an earlier FaceFinder call
           // to be used as the region to ingest for subsequent queries
           String regions = (String)  inputParams.get("regions");
           if (regions != null && !FaceRegion.isValidRegionFormat(regions))
                 return new Status (INVALID_PARAM, "Invalid  Image region specified in: " + regions) ; 
         
           // check for valid extent names(s)
           if (operation == MULTIEXTENT_REGION_QUERY_OP)
           {
                 String extents = (String) inputParams.get(EXTENT_NAMES);
                 if (extents == null || extents.isEmpty())
                    return new Status (MISSING_PARAM, "Missing or invalid Image extent names");  
                 // now split them for individual names
                 String[] extentNames = extents.split("\\W*,\\W*");
                 if (extentNames.length == 1)
                        return new Status (INVALID_PARAM, "Must specify multiple Extent names" + regions) ; 
                 for (int i = 0; i < extentNames.length; i++)
                 {
                     if (extentNames[i].isEmpty())
                          return new Status (MISSING_PARAM, "Missing Image extent name in query");  
                 }
           }
           else     // for single extent operation
           {
                
             String extent = (String) inputParams.get(EXTENT_NAME);
             if (extent == null || extent.isEmpty())
                    return new Status (MISSING_PARAM, "Missing or invalid Image extent name");  
           }

             // check for Ingest/query operations
            if (operation == REGION_INGEST_OP || operation == REGION_QUERY_OP 
                    ||  operation == MULTIEXTENT_REGION_QUERY_OP)
            {      
                String url = (String) inputParams.get(URL);
                if (url == null)
                     return new Status(MISSING_PARAM, " Missing image file URL: " + URL);

                else if ( !FormatUtils.isValidURISyntax(url))
                     return new Status( INVALID_PARAM,"Invalid Image file URL: " + url);
            
                
                String gender = (String) inputParams.get(GENDER);
                if (gender != null)
                {
                   gender = gender.toLowerCase();
                   if (!gender.matches("male|female|unknown"))
                          return new Status (INVALID_PARAM, "Invalid gender value. Must be one of male, female, or unknown");
                }
                 //------------------------------------------------------------------------------ 
                // get age or agegroup. If both specified, ignore agegroup.
                // For querying we add 1 year on eithe side of adult|child cutoff age
                // and mark it as "unknown" to be searched in both bins
                //------------------------------------------------------------------------------
                 String agegroup;
                 String age = (String) inputParams.get(AGE);            // user specified age
                 if (age != null)
                 {
                     int ageVal = Integer.valueOf(age);
                     if (operation == REGION_INGEST_OP)
                            agegroup = AgeGroupAllocator.convertAgeToGroup(ageVal);
                     else // for querying
                          agegroup = AgeGroupAllocator.convertAgeToQueryGroup(ageVal);
                     if (agegroup == null)
                         return new Status (INVALID_PARAM, "Invalid age value. Must be between " + MINIMUM_AGE +
                                " and " + MAXIMUM_AGE + ", or -1 if not known");
                 }
                  else   
                 {
                     agegroup = (String) inputParams.get(AGE_GROUP); 
                    if (agegroup != null)
                    {
                        agegroup = agegroup.toLowerCase();
                         if (!agegroup.matches(VALID_AGE_GROUPS))
                            return new Status (INVALID_PARAM, "Invalid age value. Must be one of " + VALID_AGE_GROUPS);
                    }  
                 }
                 //log.info("Using agegroup " + agegroup + " for operation" + operation);
            }
            //----------------------------------------------------------------------------------------
            // test additional parameters specific to each service
           if ( operation == REGION_INGEST_OP || operation == REGION_REMOVE_OP)
           {
                 String imageTag = (String) inputParams.get(IMAGE_TAG);
                 if (imageTag == null)
                     return new Status (MISSING_PARAM, "Missing Image identifier tag: "  + IMAGE_TAG);
                 else if  (!imageTag.matches(anChars))
                    return new Status(BAD_IMAGE_TAG, "Invalid character in image tag, " + imageTag);
                 else if (imageTag.length() > MAX_IMAGETAG_LENGTH)
                     return new Status(BAD_IMAGE_TAG, 
                        "Image tag length must be less than " + MAX_IMAGETAG_LENGTH + "characters");
           }        

           // Check for  additional query/match parameters
           else if (operation == REGION_QUERY_OP || operation == MULTIEXTENT_REGION_QUERY_OP)
           {
                String toleranceStr = (String) inputParams.get(TOLERANCE);
                // Tolerance may not be specified - then use the FM defult value
                if (toleranceStr != null)
                {
                    float tolerance = -1;
                    if ( toleranceStr.matches("[\\.,0-9]+"))
                    {
                        tolerance = Float.valueOf(toleranceStr).floatValue();
                    }
                    if (tolerance < 0.0 || tolerance > 1.0)
                        return new Status (INVALID_PARAM, " Invalid tolerance value: " + toleranceStr +" ; should be between 0 and 1");
                }
                 String limitStr = (String) inputParams.get(MAX_MATCHES);
                 // Limit may not be specified - then use the FM defult value
                 if (limitStr != null && !limitStr.matches("[0-9]+"))
                     return new Status (INVALID_PARAM, "Invalid value for max. matches to return; should be  a positive integer");
           }
           
        }
        catch(ClassCastException ce)
        {
            log.error("Got exception for input parameters:", ce);
            return (new Status(ServiceConstants.BAD_IMAGE_URL, ce.getMessage()));
        }
                   
         return new Status(SUCCESS, "");
    }
  
/*---------------------------------------------------------------------------------------------------------*/
 // -------------------------   Currently not yet implemented  --------------------------------------
/*---------------------------------------------------------------------------------------------------------*/
    public Status verifyWholeImageMatcherServiceParams(int operation, HashMap inputParams)
    {
        String clientKey = (String) inputParams.get(CLIENT_KEY);
        if (clientKey == null)
            return new Status(MISSING_CLIENT_KEY, " Missing Client Key");
         
         String extent = (String) inputParams.get(EXTENT_NAME);
            if (extent == null || extent.isEmpty())
                return new Status (INVALID_PARAM, "Missing or invalid Image Extent name");
            
         // Image ingest ops
       if (operation == WHOLEIMAGE_INGEST_OP)
       { 
            String url = (String) inputParams.get(URL);
            if ( (url == null) ||  ! FormatUtils.isValidURISyntax(url))
                 return new Status (INVALID_PARAM, "Missing or invalid  Image file URL");

            String imageTag = (String) inputParams.get(IMAGE_TAG);
            if (imageTag == null)
                return new Status (INVALID_PARAM, "Missing or invalid Image identifier tag");
            
       }
       
       // Whole Image maching query ops
       else if (operation == WHOLEIMAGE_QUERY_OP)
       {
           String toleranceStr = (String) inputParams.get(TOLERANCE);
            // Tolerance may not be specified - then use the FM defult value
            if (toleranceStr != null)
            {
                float tolerance = -1;
                if ( toleranceStr.matches("[\\.,0-9]+"))
                {
                    tolerance = Float.valueOf(toleranceStr).floatValue();
                }
                if (tolerance < 0.0 || tolerance > 1.0)
                    return new Status (INVALID_PARAM, " Invalid tolerance value: " + toleranceStr +" ; should be between 0 and 1");
            }
         
             String limitStr = (String) inputParams.get(MAX_MATCHES);
             if (limitStr == null  || !limitStr.matches("[0-9]+"))
                 new Status (INVALID_PARAM, "Invalid  max matches value; should be  a positive integer");
       }
       
       // image remove operation
       else if ( operation == WHOLEIMAGE_REMOVE_OP)
       {
            String imageTag = (String) inputParams.get(IMAGE_TAG);
            if (imageTag == null)
                return new Status (INVALID_PARAM, "Missing or invalid Image identifier tag");
       }
        return new Status(SUCCESS, "");
    }
    /*----------------------------------------------------------------------------------------------------------------------*/
     public Status verifyDatabaseQueryServiceParams(int operation, HashMap inputParams)
    {
        String clientKey = (String) inputParams.get(CLIENT_KEY);
        String clientName =  (String) inputParams.get(CLIENT_NAME_PARAM);
         
        if (clientKey == null && clientName == null)
             return new Status(MISSING_CLIENT_KEY, " Missing Client Key");
        
         String extent = (String) inputParams.get(EXTENT_NAME);
         String imageTag = (String) inputParams.get(IMAGE_TAG);
          
         if (operation == EXTENT_QUERY_OP || operation == IMAGE_QUERY_OP)
         {
             if (extent == null && extent.isEmpty())
                   return new Status (INVALID_PARAM, "Missing or invalid Image Extent name");
          }
         // Image ingest ops
         if (operation == IMAGE_QUERY_OP)
        { 
            if (imageTag == null || imageTag.isEmpty())
                return new Status (MISSING_PARAM, "Missing or empty Image identifier tag: " + IMAGE_TAG);
       }
       return new Status(SUCCESS, "");
    }

}