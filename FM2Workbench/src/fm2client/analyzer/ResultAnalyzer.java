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
 * Result Analyzer.
 */

package fm2client.analyzer;

import fm2client.analyzer.colorferet.ColorFeretResultAnalyzer; //ColorFeretResultAnalyzer;
import fm2client.display.DisplayUtils;

import  fm2client.table.ResultTable;
import java.util.HashMap;
import java.util.Properties;

/**
 * Base  class for analyzing FaceMatch library returned results for images
 * 
 */
public abstract class ResultAnalyzer 
{
    
    // Externally stored map of imageTag to imageURL map
    protected HashMap<String, String>  imageTag2UrlMap;
    
   // Externally stored map of imageTag to Annotations (landmarks) map
   protected HashMap<String, String>  image2AnnotMap = null;
   
   protected ResultTable currentTable;
   
   protected Properties testProperties;
    
    public  static ResultAnalyzer getAnalyzer(Properties properties, String imageCollection)
    {
        if (imageCollection.equalsIgnoreCase("colorferet"))
            return ( new ColorFeretResultAnalyzer(properties));
        else        // if no name specified, or for all other ones
            return   (new DefaultResultAnalyzer(properties));
    }
    //-----------------------------------------------------------------------------------------------*/
      public void setImageTag2URLMap( HashMap<String, String>  tag2UrlMap)
     {
         imageTag2UrlMap = tag2UrlMap;
     }
      
     //-----------------------------------------------------------------------------------------------*/
      public void setImageAnnotationMap( HashMap<String, String> annotMap)
     {
         image2AnnotMap = annotMap;;
     }
    //-----------------------------------------------------------------------------------------------*/
      public boolean isAnnotationAvailable( String imageFileURL)
      {
          return false;         // default, to be overridden in derived class
      }
    
      protected String getImageFileNameFromTag(String imageTag)
      {
          return imageTag2UrlMap.get(imageTag);
      }
      //-----------------------------------------------------------------------------------------------*/
     // in the derived class
     public String[] getActionNames(int operationType)
     { 
         return new String[]{"Exit"};
     }
    
    // in the derived class
     public void processUserRequest( ResultTable  table, int selectedRow,  String request)
    {
    }
     
/*----------------------------------------------------------------------------------------------------------------------------------------*/
  // Default method implementations
  /*----------------------------------------------------------------------------------------------------------------------------------------*/

     // convert the Server returned URI ( image file name) to locally accessible  file name for local display 
  /*  public  static String  getLocalImageFilePath(String imageFilePath)
    {
      return getLocalImageFilePath(null, imageFilePath);
    }*/
     
// convert the Server returned URI ( image file name) to locally accessible  file name for local display 
    public  String  getLocalImageFilePath( String imageFilePath)
    {
        return DisplayUtils.getLocalImageFilePath(testProperties, imageFilePath);                   // default: assumes an URL
    }
}
