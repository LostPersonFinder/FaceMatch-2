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

/*
 * Defines constants related to  FaceMatch system software
 */

/**
 *
 *
 */
public interface ServiceConstants
{
   // Integer values assigned to Services and Operations internally
    //Service types 
    public static int INFO_SVC = 100;   
    public static int ADMIN_SVC = 200;   
    
    public static int IMAGE_EXTENT_SVC = 400;
    
    public static int FACE_FIND_SVC = 500;                              // finding  face(s)  in an image
    public static int FACE_MATCH_REGION_SVC = 600;           // finding matching region(s) in ingested images
    public static int WHOLE_IMAGE_MATCH_SVC = 700;          // matching of whole images
    public static int DATABASE_QUERY_SVC = 800;                 // information from database tables about images, etc.
   
    
    // Info Service subtypes (operations)
     public static int GET_SYSTEM_STATUS = 101;                   // Server status and settings
     public static int GET_GPU_STATUS = 102;                         // only GPU status
     public static int GET_PERF_MON = 103;                             // FaceMatch performance monitoring settings
     
   // Admin Service  integer values used by the Admin Servlet for different servlet paths 
   // (hardcoded in the server)
     public static int FM_SHUTDOWN = 201;
     public static int GPU_ON = 211;
     public static int GPU_OFF = 212;
     public static int ADD_FMCLIENT = 221;
     
     public static int RECORD_PERF = 240;						// Performance recording turm on/off (toggle)
     
     public static int ADD_EXTENT_OP = 401;
     public static int REMOVE_EXTENT_OP = 402;                      // Delete all ingested images in an extent
     public static int ACTIVATE_EXTENT_OP = 403;                    // Enable image set for ingest/querying
     public static int DEACTIVATE_EXTENT_OP = 404;               // disable extent for ingest/query
     public static int SET_PERFORMANCE_OP = 405;                   // Set preferred performance level in face query
    
    // Face Finder functions 
     public static int GET_FACES_OP = 501;                              // get face(s) in an image
     
     // Face Regions functoins
     public static int REGION_INGEST_OP = 601;                      // ingest a photo or image for a person
     public static int REGION_QUERY_OP = 602;                       // Query to get matching faces in an extent
     public static int REGION_REMOVE_OP = 603;                     // Remove an ingested image/region
     public static int MULTIEXTENT_REGION_QUERY_OP = 604;    //Query to get matching faces in multple extents
                         
     //Whole Image functions - not implemented
     public static int WHOLEIMAGE_INGEST_OP = 701;              // ingest a general photo or image
     public static int WHOLEIMAGE_QUERY_OP = 702;               // Query to get matching entities
     public static int WHOLEIMAGE_REMOVE_OP = 703;             // Remove an ingested photo/image

     // database query operations - not implemented
     public  static int ALL_CLIENT_QUERY_OP = 801;
     public  static int CLIENT_QUERY_OP = 802;
     public  static int EXTENT_QUERY_OP = 803;
     public  static int IMAGE_QUERY_OP = 804;
     
     public static String FACE_SEPARATOR = "\t";
    
     
     // Operation successful
     public static int SUCCESS = 1;
     public static int FAILURE = -1;
     
     public static int DATA_CHUNK_SIZE = 8*1024;            // 8k size buffer for chunk reading of input stream
     
    // supported age of a person for search by grouping
     public static int MINIMUM_AGE = 0;
     public static int MAXIMUM_AGE = 120;
     public static int AGE_GROUPING_INTERVAL = 10;    // grouping for searching 
     public static String INVALID_AGE = "Invalid age. Must be between " + MINIMUM_AGE +" and "+ MAXIMUM_AGE;
     public static int MAX_IMAGETAG_LENGTH = 256;      // max length of image's unique tag
     
     //-----------------------------------------------------------------------------------------
     // Names of parameters for each FM service/operation
     // as received in a HTTP request - corresponding to each xxx_op
     //--------------------------------------------------------------------------------------------
     // servlet paths for admin services
     public static String USE_GPU = "usegpu";
     public static String GPU_STATUS = "gpustatus";
     public static String ADD_CLIENT = "addclient";
     public static String SHUT_DOWN = "shutdown";
     
     // Service parameters
     public static String CLIENT_PARAM = "client";
     public static String CLIENT_NAME_PARAM = "clientname";
     public static String CLIENT_INFO = "infofile";
     public static String DESCRIPTION_PARAM = "description";        // for any service
     
     public static String CLIENT_KEY = "key";
     public static String EXTENT_NAME = "extent"; 
     public static String EXTENT_NAMES = "extents"; 
     
     public static String EXTENT_NAME_PARAM = "name"; 
     public static String IMAGE_TAG_PARAM = "tag";
     
     // performance preferences parameters and options
     public static String FF_OPTION= "option";
     public static String PERFORMANCE_PREF= "performance";
     public static String PERFORMANCE_SPEC = "performanceSpec";
     public static String PERF_OPTION_USED = "performanceUsed";
     public static String SKIN_COLORMAP_KIND= "skinColorMapKind";
     public static String NOT_USED = "notUsed";
     public static String OPTIMAL = "optimal";
     public static String SPEED = "speed";
     public static String ACCURACY = "accuracy";
     public static String PROGRESSIVE = "progressive";
     
     public static String IMAGE_TAG = "tag";                  // Identifier of incoming image 
     public static String INGEST_IMAGE_TAG = "tag";    // tag of a matched image
     public static String INGEST_URL = "ingestUrl";
     public static String QUERY_URL = "queryUrl";  
     
     public static String URL = "url";
     public static String LANDMARKS = "landmarks";          // if true show landmarks
     public static String REGION = "region";
     public static String INFLATE_BY = "inflateby";
     
      // possible metadata fields
     public static String AGE = "age";
     public static String AGE_GROUP = "agegroup";
     public static String GENDER = "gender";
     public static String LOCATION = "location";
     public static String UNKNOWN = "unknown";
     public static String CHILD = "child";
     public static String ADULT = "adult";
     public static String YOUTH  = "youth";
     
     public static String VALID_AGE_GROUPS = "adult|youth|child|unknown";
     
    
     public static String USER = "user";
     public static String PASSWORD = "password";

     //query/search specific params
     public static String TOLERANCE = "tolerance";          //  float: >= 0; exact match => 0
     public static String MAX_MATCHES= "maxmatches";       // max number of matches requested
     public static String IMAGE_ROTATED = "imageRotated";   // was image rotated to detect faces
     
     //----------------------------------------------------------
     // returned parameters to the HTTP client in the Result object
     //----------------------------------------------------------
     public static String INDEX_TYPE = "indexType";
     public static String INDEX_VERSION = "indexVersion";
     public static String NUM_MATCHES = "numMatches";          // number of matches found <= max_matches  
     public static String MATCH_DISTANCE = "distance";
     public static String INDEX_UPLOAD_TIME = "indexUploadTime";
     public static String NUM_INDEX_FILES_LOADED = "numIndexLoaded";
     
     public static String NUM_REGIONS = "numRegions";       // (faces) found in an image 
     public static String FACE_REGIONS = "faceRegions";
     public static String DISPLAY_REGIONS = "displayRegions";   
     public static String INGESTED_REGIONS  = "ingestedRegions";
     public static String QUERY_REGION  = "queryRegion";
     public static String QUERY_MATCHES  = "allMatches";
     public static String REGION_MATCHES  = "regionMatches";
     public static String QUERY_RESULTS  = "queryResults";
     public static String REGION_INDEX = "regionIndex";     // zoro-based index within image in "remove region"
     public static String REGION_TAG = "rgn";                  // for indexing/removing an image region
     
     public static String GPU_USED = "gpuUsed";             // was GPU used by FMLib
    
     public static String URL_FETCH_MILLI = "urlFetchMsec";
     public static String FM_PROCESS_MILLI = "processingMsec";
     

     public static String RESULT = "result";                // formatted Results Array
     //public static String NUM_RECORD = "num-records";
     
     public static String[] METADATA = {AGE_GROUP, GENDER};           // currently only these two
     
     
     //----------------------------------------------------------------
     // define all FM services and operations used internally in String form
     //----------------------------------------------------------------
     
     public static String ADMIN = "admin";
     public static String EXTENT = "extent";
     
     public static String FACE_FIND = "facefind";
     public static String FACE_MATCH_REGION = "regionmatch";
     public static String WHOLE_IMAGE_MATCH = "wholeImagematch"; 
     
     public static String  ADD_EXTENT = "add";
     public static String REMOVE_EXTENT = "remove";
     public static String ACTIVATE_EXTENT = "activate";                             // Enable image set for ingest/querying
     public static String DEACTIVATE_EXTENT = "deactivate";                     // disable for ingest/query
     public static String SET_PERFORMANCE_PREF = "performance";         // set performance preference for extent

     public static String GET_FACES = "getfaces";
     public static String INGEST = "ingest";
     public static String QUERY = "query";
     public static String QUERY_ALL = "queryall";
     public static String REMOVE = "remove";
     public static String DELETE_EXTENT = "delete";
     
     public static String FACE_REGION_SEPARATOR="\t";      // Tab character as face separateor
      
     //----------------------------------------------------------
     // Parameters usually set in the request/response header
     //----------------------------------------------------------
     public static String CONTENT_LENGTH = "content-length";
     public static String RETURN_VALUE = "return-value";    // for int and booleans
     
     // Status respose via HTTPResponse
     public static String STATUS_CODE = "statusCode";
     public static String STATUS_MESSAGE = "statusMessage";

     // Time factors
     public static String SERVICE = "service";
     public static String OPERATION = "operation";
     public static String SERVICE_DATE = "serviceDate";
     public static String SERVICE_TIME = "serviceTimeMsec";
     public static String FACEFIND_TIME = "faceFindTimeMsec";
     public static String INGEST_TIME = "ingestTimeMsec";
     public static String QUERY_TIME = "totalQueryTimeMsec";
          
     // define NanoSecond to MilliSecond conversion
     public static int NANO2MILLISEC= (int) Math.pow(10, 6);
     
   //-------------------------------------------------------------------------------------------------------------- 
    // HTTP-based status codes 
    // All errors below  start above 1000 (are four digits) to avoid clash with HTTP error codes
    //---------------------------------------------------------------------------------------------------------------  

 
     // FM2 Server level errors - mainly at system startup time
     public static int FM_INIT_ERROR = 1101;
     public static int FMLIB_LOAD_ERROR = 1102;
     public static int FMLIB_OPTION_ERROR = 1103;
     public static int FMLIB_VERSION_MISMATCH = 1104;
     public static int FMLIB_OPERATION_ERROR = 1105;
     public static int DATABASE_ACCESS_EXCEPTION = 1106;
     public static int INTERNAL_SERVER_ERROR = 1110;
     
     // Errors corresponding to bad user request - in 400 series
     public static int INVALID_SERVICE = 1401;
     public static int INVALID_OPERATION = 1402;
     public static int MISSING_PARAM = 1403;
     public static int INVALID_PARAM = 1404;
     public static int MISSING_CLIENT_KEY = 1405;
     public static int INVALID_CLIENT_KEY = 1406;
     public static int INVALID_USER_ID = 1407;
     public static int BAD_IMAGE_URL = 1408;
     public static int BAD_IMAGE_TAG = 1409;
     
     // Errors in adding a new client to FM2
     public static int INACCESIBLE_CLIENT_FILE = 1501;
     public static int INVALID_CLIENT_FILE = 1502;
     public static int MISSING_CLIENTNAME_INFILE = 1503;
     public static int MISMATCHED_CLIENTNAME = 1504;
     public static int DUPLICATE_CLIENT_NAME = 1505;
     public static int BAD_CLIENT_KEY_INFILE = 1506;
     public static int BAD_METADATA_INFILE = 1507;
     public static int INVALID_INDEXSTORE_PATH = 1508;
     public static int CLIENTFILE_READ_EXCEPTION = 1509;
    
     // Errors specific to an image extent 
     public static int DUPLICATE_EXTENT_NAME = 1601;
     public static int INVALID_EXTENT_NAME = 1602; 
     public static int INACTIVE_IMAGE_EXTENT = 1603;
    
    // Facematch operation-specific errors
    public static int FUNCTION_NOT_IMPLEMENTED = 1701;
    public static int BAD_FRD = 1702;
     public static int NO_FACES_IN_IMAGE = 1703;
     public static int DUPLICATE_IMAGE_TAG = 1704;
     public static int NO_IMAGE_AVAILABE = 1705;
     public static int INVALID_FACE_REGION = 1706;
     public static int NO_FACES_IN_REGION = 1707;
     public static int NO_VALID_FACES_IN_REGION = 1708;
}
