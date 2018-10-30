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

/***********************************************************************
 * Interface MenuCommands
 *
 * This interface statically defines all constants and strings used
 * for Menu and other action commands for the FM Workbench
 ***************************************************************************/

package workbench.control;


import java.util.HashMap;

import fmservice.httputils.common.ServiceConstants;


public interface FMMenuCommands extends ServiceConstants
{
    // top menu commands
    // static String MENU_FILE = "File";
    static String MENU_ADMIN = "Admin Functions";
    static String MENU_EXTENT_OPS = "Extent Operations";
    static String MENU_REGION_OPS = "Region Operations";

    static String MENU_STORED_RESULTS = "Stored Result  Review";
    static String MENU_REPORT = "Status/Reports";
    static String MENU_PERFORMANCE = "Performance";
    static String MENU_HELP = "Help";
    
    //-----------------------------------------------------------------------------------------------
    // Submenu
    static String MENU_REGION_OPS_SINGLE = "Single Request";
    static String MENU_REGION_OPS_BATCH = "Batch Mode";
    //-----------------------------------------------------------------------------------------------
    
    // ---- Menu items ----
    
    static String ADD_CLIENT = "Add a FaceMatch client";
    static String SET_FF_PARAMS = "Set Facefinding Flags";
    static String SET_GPU_USE = "Allow/stop GPU use";
    //------------------------------------------------------------------------------------------------

    static String ADD_EXTENT = "Add a new Image Extent";
    static String REMOVE_EXTENT = "Remove an Extent";
    static String ACTIVATE_EXTENT = "Set Extent Active";
    static String DEACTIVATE_EXTENT = "Set Extent Inactive";
    //------------------------------------------------------------------------------------------------

    static String INGEST_BATCH_RT = "Batch Ingest";
    static String REMOVE_BATCH_RT ="Remove Images/Regions";
    static String QUERY_BATCH_RT = "Batch Query";
    static String FF_BATCH_RT = "Batch Face finding";
    static String SCENARIO_TEST = "Scenario Test";
    //----------------------------------------------------------------------------------------------------
     static String INGEST_SINGLE_RT= "Ingest  an Image";  
     static String REMOVE_SINGLE_RT ="Remove Image/ Region";
     static String QUERY_SINGLE_RT = "Query matching Images";   
     static String FF_SINGLE_RT = "Find Faces in Image";

     //------------------------------------------------------------------------------------------------   
    static String VIEW_STORED_FF_DATA = "Review Facefinding Result";
    static String VIEW_STORED_INGEST_DATA= "Review Integest Result ";
    static String VIEW_STORED_QUERY_DATA = "Review Query Result";
    //------------------------------------------------------------------------------------------------
    
    // Reports, except for Client Log etc. are supported only from applications within the dmz
    // since they require explicit  (read-only) access to the database
    static String SHOW_CLIENT_LOG = "Client Log";
    static String REPORT_DATABASE = "Database info";
    static String  REPORT_CLIENT = "Client Report";
    static String  REPORT_EXTENT = "ImageExtent Report";
    static String REPORT_IMAGE = "Image Report";
    static String SHOW_SERVER_LOG = "FM2 Server  Log";
    //----------------------------------------------------------------------------------------------------
    // FM2 Service request operation for a various request types
    static  String FM_FACE_FIND_OP = "facefind";
    static  String FM_REGION_INGEST_OP = "regioningest";
    static  String FM_REGION_QUERY_OP = "regionquery";
     static  String FM_REGION_REMOVE_OP = "removeRegions";
     
     static String CLIENT_PARAM1 = ServiceConstants.CLIENT_PARAM;
     
    //--------------------------------------------------------------------------------------------------------
    // Mapping between Client's Menu command to Server's Web Service request
    //--------------------------------------------------------------------------------------------------------
     static HashMap<String, String> menuCmd2FM2Oper=  new HashMap<String, String>()
     {
         {
            put(MENU_ADMIN, ADMIN);
            put( MENU_EXTENT_OPS, EXTENT);
            put( MENU_REGION_OPS_SINGLE , "");
            put(MENU_REGION_OPS_BATCH, "");
            //
            // Same operation type for batch or single
            put (FF_SINGLE_RT, FM_FACE_FIND_OP);
            put(INGEST_SINGLE_RT, FM_REGION_INGEST_OP);
            put(QUERY_SINGLE_RT,  FM_REGION_QUERY_OP);
            put(REMOVE_SINGLE_RT,  FM_REGION_QUERY_OP);
            //
            put(FF_BATCH_RT , FM_FACE_FIND_OP);
            put(INGEST_BATCH_RT, FM_REGION_INGEST_OP);
            put(QUERY_BATCH_RT,  FM_REGION_QUERY_OP);
            put(REMOVE_BATCH_RT, FM_REGION_REMOVE_OP);  
            
            // stored results for various FM2 operations
            put(VIEW_STORED_FF_DATA,  FM_FACE_FIND_OP);
            put(VIEW_STORED_INGEST_DATA,  FM_REGION_INGEST_OP);
            put(VIEW_STORED_QUERY_DATA,   FM_REGION_QUERY_OP);
            put("Real Time", "realtime");
            put("Stored Result", "storedResult");
         }
     };

}
