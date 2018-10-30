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
 * An interface to hold various table and display related constants for the Fm2Clienr display operations
 */

package fm2client.table;

import java.util.HashMap;

public interface  FM2TableConstants
{
    public  static int DISPLAY_WIDTH = 1600;
    public static int DISPLAY_HEIGHT = 900;
    
    public  static int TESTNUM_COLUMN = 0;
    public  static int TESTID_COLUMN = 1;
    
   public static int FACEMATCH_REGION_OPERATION = 100;          // any Region related operation
   public static int  FACEFIND_OPERATION = 101;
   public static int  REGION_INGEST_OPERATION = 102;
   public static int  REGION_QUERY_OPERATION = 103;
   public static int  REGION_REMOVE_OPERATION = 104;
   

   
   public static int ERROR_DISPLAY = 99;

   
   public static String FF_TABLE_NAME = "FaceFinding Results";
  
   // column names for the  FaceFind table
    public  static String[] GetFacesColumnNames = {"Test Num",  "Test ID", "Image URL", 
        "Num Faces","Coordinates", "Performance Choice", "FaceFind Time", "Total Time", "GPU Used"};
   public static  int FFImageNameCol = 2;           // column number with image name
   public static  int FFCoordCol = 4;               // column name with face coordinates
   
   public static int FF_ANNOT_BUTTON = 1;           // Annotation Display Button
    
  //--------------------------------------------------------------------------------------------------------------------------
   // Ingest Table related consnts  
   public static String INGEST_TABLE_NAME = "Region Ingest Results";
    
    public static String[] RegionIngestColumnNames = {
       " Test Num", "Test ID", "Extent", "Image Tag", "Num Faces", "Coordinates", "FaceFind Time", "Ingest Time", "Total Time"};
    
    public  static  int FMImageTagCol = 3;           // column number with image tag
    public  static  int FMCoordCol = 5;              // column name with face coordinates  
    
    //--------------------------------------------------------------------------------------------------------------------------
    // Query table related constants
     public static String QUERY_TABLE_NAME = "Region Query Results";
       
    public static String[] RegionQueryColumnNames = {
        "Test Num", "Test ID",  "Extent", "Query Image", "Region #", "Query Coords","Tolerance",  "Returned Matches", "Best Match", 
        "Distance", "Match Coords", "FaceFind Time", "Query Time", "IndexUpload Time", "Total Time"};
    
    public  static int QueryImageCol = 3;         // Query Image
    public static int QueryCoordCol = 5;          // column name with face coordinates
    public static int ToleranceCol = 6;             // Tolerance parameter given for query
    public static int TotalMatchesCol = 7;             // Tolerance parameter given for query
    
    public static int MatchImageTagCol = 8;    // Best match image tag
    public static int DistanceCol = 9;               // column name with match coordinates
    public static int MatchCoordCol = 10;         // column name with match coordinates
    
    
//--------------------------------------------------------------------------------------------------------------------------
//SrandardResult  (Default) Table column names
    public static String STANDARD_TABLE_NAME =  "Successful FaceMatch Service Results";
    public static String[] StandardResultColumnNames = {
         "Operation", "Test ID",  "Extent", "Image URL",  "Service Info", "FaceFindTime", "Total Time",
         "Status Code", "Status Message"};
     public  static int StandardImageCol = 4;   
    
//--------------------------------------------------------------------------------------------------------------------------
// ErrorResult  Table column names
    public static String ERROR_TABLE_NAME =  "Unsuccessful Results";
    public static String[] ErrorResultColumnNames = {
         "Service", "Operation", "Test ID",  "Extent", "Image URL/Tag",  "FaceFindTime", "Error Code", "Error Message"};
     public  static int ErrorImageCol = 4;   
     
     
    
}
