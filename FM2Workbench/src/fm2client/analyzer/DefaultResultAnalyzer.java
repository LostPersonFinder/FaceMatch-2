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
 * Analyzer to provide"visual" analysis of FaceMatch results by displaying the 
 * concerened  images and the corresponding results in various GUI screens
 */

package fm2client.analyzer;

import fm2client.display.ImageDisplays;
import fm2client.display.ImageDisplays.ImageInfo;
import fm2client.display.DisplayUtils;

import fm2client.table.FM2TableConstants;
import fm2client.table.ResultTable;
import fm2client.table.FaceFindResultTable;
import fm2client.table.RegionQueryResultTable;
import fm2client.table.RegionQueryResultTable.RegionMatch;

import javax.swing.JFrame;
import javax.swing.JTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;


public class DefaultResultAnalyzer extends ResultAnalyzer implements FM2TableConstants
                 
{
    public  String ImageCollectionName = "";
    public double imageScaleFactor = 0.5;
    
    public static String[] facefindReviews = {"Display Image",  "Display GT Annotations", "Compare OpenCV Results", "Exit"};
    public static String[] ingestReviews = {"Display Ingested Regions",  "Show Face Landmarks",  "Remove Image/Region", "Exit"};
    public static String[] queryReviews = {"Show Best Match",  "Show All Matches", "Exit"};
   

    public  DefaultResultAnalyzer(Properties configProperties)
    {
        super();
        super.testProperties = configProperties;
    }
    
      public String[] getActionNames(int operationType)
     { 
         if (operationType == FACEFIND_OPERATION)
             return facefindReviews;
         else if (operationType == REGION_INGEST_OPERATION)
             return ingestReviews;
         else if (operationType == REGION_QUERY_OPERATION)
             return queryReviews;
         else
         {
            DisplayUtils.displayErrorMessage("Unknown operation type: " + operationType);
            return new String[]{"Exit"};
         }
     }

    //--------------------------------------------------------------------------------------------------------------------------------
    // Implementation of abstract method in super class
    //--------------------------------------------------------------------------------------------------------------------------------
     public void processUserRequest( ResultTable  resultTable, int selectedRow,  String request)
     {
         int operationType = resultTable.getOperationType();
         JTable table = resultTable.getTable();
         switch (operationType)
         {
             case FACEFIND_OPERATION:
             {
                 processFaceFindRequest( resultTable, selectedRow, request);
                 break;
             }
             case REGION_INGEST_OPERATION:
             {
                 processRegionIngestRequest( resultTable, selectedRow, request);
                 break;
             }
              case REGION_QUERY_OPERATION:
             {
                 processRegionQueryRequest( resultTable, selectedRow, request);
                 break;
             }
         }
     }
  /*------------------------------------------------------------------------------------------------------------------*/      
    protected void processFaceFindRequest(ResultTable resultTable, int selectedRow, String request)
    {
        JTable table = resultTable.getTable();
        if (request.equals("Display Image"))
        {
           drawSelectedImage(table, selectedRow);
        }
        else if (request.equals("Display GT Annotations"))
        {
            if (image2AnnotMap == null)             // if an annotationMap is provided for this set
                return;
            else
                drawSelectedNAnnotatedImages( table, selectedRow);
        }
        else
            DisplayUtils.displayErrorMessage("Unknown FaceFind request \"" + request + "\" ignored.");
     }
 /*----------------------------------------------------------------------------------------------------------------------------------*/
 // Processing of Requests related to FaceFinding
 /*----------------------------------------------------------------------------------------------------------------------------------*/
    protected void drawSelectedImage(JTable table, int selectedRow)
    {
        int testId = (int) table.getModel().getValueAt(selectedRow,  TESTID_COLUMN);
        String imageTitle = "Facefind " + " Test ID:  " + testId;
        String imageFileName  = (String) table.getModel().getValueAt(selectedRow, FFImageNameCol);
        String imageFilePath   = getLocalImageFilePath(imageFileName);
        if (imageFilePath  == null)
        {
            DisplayUtils.displayErrorMessage("Could not get local image file Path.");
            return;                       // cannot get the image file locally to draw
        }
        String coordStr = (String) table.getModel().getValueAt(selectedRow, FaceFindResultTable.FFCoordCol);
        if (coordStr == null || coordStr.isEmpty())
            DisplayUtils.displayInfoMessage("No faces found for the image.");           // just show the face

        ImageDisplays.displayImageInFrame(imageTitle, testId, imageFilePath, coordStr, true);
        return; 
    }
    /*-----------------------------------------------------------------------------------------------------------------*/   
    // Here we draw the face and landmarks as returned by the FM2Server, along with 
    // the corresponding annotated image if that exists
    // Note the selected image must be accessible locally, either as an URL or on a local file system
    /*-----------------------------------------------------------------------------------------------------------------*/
    protected void  drawSelectedNAnnotatedImages(JTable table, int selectedRow)
    {
        if (image2AnnotMap == null)
            return;
        
         int testId = (int) table.getModel().getValueAt(selectedRow,  TESTID_COLUMN);
         String imageFileName  = (String) table.getModel().getValueAt(selectedRow, FFImageNameCol);
         String imageFilePath   = getLocalImageFilePath(imageFileName);
        if (imageFilePath  == null)
        {
            DisplayUtils.displayErrorMessage("Could not get local image file Path.");
            return;                       // cannot get the image file locally to draw
        }
        // cannot get the image file locally to draw
        String coordStr = (String) table.getModel().getValueAt(selectedRow, FFCoordCol);
        
        // get the annotated file name, along with the coordinates
        String annotCoordStr  = getFaceAnnotations(imageFileName);
        if (annotCoordStr == null || annotCoordStr.isEmpty())
        {
            DisplayUtils.displayInfoMessage("No face annotations available for image " +  imageFileName);
            return;
        }
        ImageDisplays.ImageInfo[] imageInfo = new ImageDisplays.ImageInfo[2];
        imageInfo[0] = new ImageDisplays.ImageInfo();
        imageInfo[0].imageFileName = imageFilePath;
        imageInfo[0].faceCoordinates  = coordStr;
        imageInfo[0].imageLabel = "Facefind Coordinates";
        imageInfo[0].imageAnnotation = null;
        
        imageInfo[1] = new ImageDisplays.ImageInfo();
        imageInfo[1].imageFileName = imageFilePath;
        imageInfo[1].faceCoordinates  = annotCoordStr;
        imageInfo[1].imageLabel = "Manually Annotated Coordinates";
        imageInfo[1].imageAnnotation = null;
        
        ImageDisplays.displayMultipleImages("FaceMatch generated vs. Manual annotations", testId,  2, imageInfo);
    }
    
     protected void processRegionIngestRequest(ResultTable resultTable, int selectedRow, String request)
    {
        JTable table = resultTable.getTable();
        if (request.equals("Display Ingested Regions"))
        {
           drawSelectedIngestedImage(table, selectedRow);
        }
        else if ( request.equals("Show Face Landmarks") || request.equals( "Remove Image/Region"))
        {
            DisplayUtils.displayWarningMessage("To be Implemented...");
        }
        else
            DisplayUtils.displayErrorMessage("Unknown  Ingest  request \"" + request + "\" ignored.");
     }
  /*----------------------------------------------------------------------------------------------------------------------------------*/
 // Processing of Requests related to FaceFinding
 /*----------------------------------------------------------------------------------------------------------------------------------*/
    protected void drawSelectedIngestedImage(JTable table, int selectedRow)
    {
        int testId = (int) table.getModel().getValueAt(selectedRow,  TESTID_COLUMN);
        String imageTitle = "Ingested Image " + " Test ID:  " + testId;
        String imageTag  = (String) table.getModel().getValueAt(selectedRow, FMImageTagCol);
        String imageFileName =  getImageFileNameFromTag(imageTag);
        String imageFilePath   = getLocalImageFilePath(imageFileName);
        if (imageFilePath  == null)
        {
            DisplayUtils.displayErrorMessage("Could not get local image file Path.");
            return;                       // cannot get the image file locally to draw
        }
        String coordStr = (String) table.getModel().getValueAt(selectedRow, FaceFindResultTable.FMCoordCol);
        if (coordStr == null || coordStr.isEmpty())
            DisplayUtils.displayErrorMessage("No ingested region information found for the image.");           // just show the face

        ImageDisplays.displayImageInFrame(imageTitle, testId, imageFilePath, coordStr, true);
        return; 
    }
    
    /*-----------------------------------------------------------------------------------------------------------------*/
    // Display the query results as requested by the user
    // Options are: "Show Best Match", "Show Image Set ", "Show All Matches"
    /*-----------------------------------------------------------------------------------------------------------------*/
    protected void  processRegionQueryRequest(ResultTable resultTable, int selectedRow, String request)
    {
        JTable table = resultTable.getTable();
        if (request.equals ("Show Best Match"))
         {
             drawQueryNBestmatchImages(table, selectedRow);
         }
         else if (request.equals( "Show All Matches"))
         {
            showAllQueryMatches(resultTable, selectedRow);
         }
         else
            DisplayUtils.displayErrorMessage("Unknown Query request \"" + request + "\" ignored.");
    }
    
/*-----------------------------------------------------------------------------------------------------------------*/   
// Here we draw the face and landmarks as returned by the FM2Server, along with 
// the corresponding annotated image if that exists
// Note the selected image must be accessible locally, either as an URL or on a local file system
/*-----------------------------------------------------------------------------------------------------------------*/
    protected void  drawQueryNBestmatchImages(JTable table, int selectedRow)
    {
         int testId = (int) table.getModel().getValueAt(selectedRow,  TESTID_COLUMN);
        String queryImageName  = (String) table.getModel().getValueAt(selectedRow,  QueryImageCol);
        String queryImageFilePath   = getLocalImageFilePath(queryImageName);
        if (queryImageFilePath  == null)
        {
            System.out.println("Null Path query Image Name:" + queryImageName);
            return ;                        // cannot get the image file locally to draw
        }
        String queryCoordStr = (String) table.getModel().getValueAt(selectedRow, QueryCoordCol);
        
        String matchImageTag  = (String) table.getModel().getValueAt(selectedRow,  MatchImageTagCol);
        String matchImageName   =  getImageFileNameFromTag(matchImageTag);
        if (matchImageName == null)
        {
            DisplayUtils.displayErrorMessage("No  URL  found for image tag " + matchImageTag);
        }
        String matchImageFilePath   = getLocalImageFilePath(matchImageName);
        if (matchImageFilePath  == null)
        {
            System.out.println("Null Path match Image Name:" + matchImageName);            
            return ;                        // cannot get the image file locally to draw
        }
        String matchCoordStr = (String) table.getModel().getValueAt(selectedRow, MatchCoordCol);
        float distance =  ((Float) table.getModel().getValueAt(selectedRow, DistanceCol)).floatValue();
        
        // get the annotated file name, along with the coordinates
        String annotCoordStr  = ""; //getAnnotatedRegions (imageFilePath);
        
       ImageInfo[] imageInfo = new ImageDisplays.ImageInfo[2];
        imageInfo[0] = new ImageDisplays.ImageInfo();
        imageInfo[0].imageFileName = queryImageFilePath;
        imageInfo[0].faceCoordinates  = queryCoordStr;
        imageInfo[0].imageLabel = "Query Image";
        imageInfo[0].imageAnnotation = null;
        
        imageInfo[1] = new ImageDisplays.ImageInfo();
        imageInfo[1].imageFileName = matchImageFilePath;
        imageInfo[1].faceCoordinates  = matchCoordStr;
        imageInfo[1].imageLabel = "Bestmatch Image, distance: "+ distance;
        imageInfo[1].imageAnnotation = null;
        
        ImageDisplays.displayMultipleImages("Test ID: " + testId + " Query And Bestmatch Images", testId,  2, imageInfo);
    }
    
    
    // ---------------------------------------------------------------------------------------------------------
    // Show all matching images corresponding to a query images.  Resize the
    // query image if it is larger than the query image
       protected void showAllQueryMatches(ResultTable resultTable, int selectedRow)
       {
           drawQueryNMatchingImages(resultTable, selectedRow, true);
       }

    /*---------------------------------------------------------------------------------------------------------------------------------------*/  
    protected JFrame drawQueryNMatchingImages(ResultTable resultTable,  int selectedRow, boolean resizeImages)
    {
        ArrayList<RegionQueryResultTable.RegionMatch> returnedMatches =
            ((RegionQueryResultTable) resultTable).getRegionMatches(selectedRow);
        int numMatches = returnedMatches.size();
       
        JTable table = resultTable.getTable();

        String queryImageURL = (String)table.getModel().getValueAt(selectedRow, QueryImageCol);
        String queryCoordStr = (String) table.getModel().getValueAt(selectedRow, QueryCoordCol);
        String tolerance = ((Float)table.getModel().getValueAt(selectedRow, ToleranceCol)).toString();
        String totalMatches = ((Integer) table.getModel().getValueAt(selectedRow, TotalMatchesCol)).toString();
        String localQueryImageURL = getLocalImageFilePath(queryImageURL);
      
       // Specify the Query image to be drawn
       ImageInfo queryImageInfo = new ImageDisplays.ImageInfo();
       queryImageInfo.imageFileName = localQueryImageURL;
       queryImageInfo.faceCoordinates = queryCoordStr;
       // give a multiline lable as an HTML string
       String imageLabel = 
           "<HTML> Query Image - Tolerance: " + tolerance +
           "<br> Total number of matches: " + totalMatches +
           "<br> Number of returned matches: " + numMatches +"</HTML>";
       queryImageInfo.imageLabel = imageLabel;
       
       // Specify all returned match images
    
       int ndraw = numMatches; // > 25 ? 25 : numMatches;               // numbers to draw
       ImageInfo[] matchImageInfos = new ImageDisplays.ImageInfo[ndraw];
       for (int i = 0; i < ndraw; i++)
       {
           RegionMatch qmatch = returnedMatches.get(i);
           ImageInfo matchInfo = new ImageDisplays.ImageInfo();
           matchInfo.imageFileName = getLocalImageFilePath(qmatch.matchImageUrl);
           matchInfo.faceCoordinates = qmatch.matchCoord;
           matchInfo.imageLabel =
               "<HTML>Rank: "+ qmatch.rank +", distance: " + qmatch.distance +
               "<br> Tag: " + qmatch.imageTag +"</HTML>";
           matchInfo.imageAnnotation = null;
          matchImageInfos[i] = matchInfo;
       }
       // now display the imageset
        int testId = (int) table.getModel().getValueAt(selectedRow,  TESTID_COLUMN);
        JFrame matchFrame;
        if (!resizeImages)
            matchFrame = ImageDisplays.displayQueryNMatchingImages("Test ID: " + testId +" Returned  Query Matches" , 
                    queryImageInfo,  matchImageInfos );
        else 
            matchFrame = ImageDisplays.displayQueryNMatchingImages("Test ID: " + testId +" Returned  Query Matches" , 
                    queryImageInfo,  matchImageInfos,  imageScaleFactor);
            
        return matchFrame;
    }
    
   /*---------------------------------------------------------------------------------------------------------*/
    // Show all images in the same set belonging to the Selected Person
    // Image Names are retrieved from the ImageTag2URL map, where the tags 
    // start  with the same xxx_date numbers for the ColorFERET images
    //-----------------------------------------------------------------------------------------------------------*/
   protected void  showAllImagesInSet( ResultTable resultTable,  int selectedRow)
   {
       JTable table = resultTable.getTable();
        String queryImageURL = (String)table.getModel().getValueAt(selectedRow, QueryImageCol);
        File file = new File(queryImageURL);
        String fileName = file.getName();
        String[] parts = fileName.split("_");
        String selectedPrefiix = parts[0]+"_"+parts[1]+"_";

      //  Look for all fileNames in the imageTag2Uri map, starting with this tag
        ArrayList<String>  imageList = new ArrayList();
        Iterator<String> it = imageTag2UrlMap.keySet().iterator();
        while(it.hasNext())
        {
            boolean listStarted = false;
            String tag = it.next();
            if (tag.startsWith(selectedPrefiix))
            {
                imageList.add(tag);
            }
            else        // not a match in the ordered map
            {
                if (listStarted)            // no need to loop any more
                    break;
            }
        }
        System.out.println("Number of images in the Set: " + imageList.size());
        // get the query matches to see if any one is also in he returned set

        int ns = imageList.size();
        if (ns  <= 1)
        {
            DisplayUtils.displayInfoMessage("No more images available in the set.");
            return;
        }
        // display the images in a new window with label
       ImageDisplays.ImageInfo[] imageSetInfo = new ImageDisplays.ImageInfo[ns];
       for (int i = 0; i < ns; i++)
       {
            ImageDisplays.ImageInfo imageInfo = new ImageDisplays.ImageInfo();
            String imageTag = imageList.get(i);
            String  fname =  imageTag2UrlMap.get(imageTag);
            imageInfo.imageFileName = getLocalImageFilePath(fname);  
            imageInfo.imageAnnotation = null;
            RegionQueryResultTable.RegionMatch qmatch =  getQueryMatch(resultTable, selectedRow, imageTag);
            if (qmatch == null)
            {  
                File file1 = new File(imageInfo.imageFileName );
                 imageInfo.imageLabel  =  "<HTML>image File:"+ file1.getName() +
                    "<br> Tag: " +  imageTag +"</HTML>";
                    imageInfo.faceCoordinates = null;
            }
            else
            {
                imageInfo.faceCoordinates = qmatch.matchCoord;
                imageInfo.imageLabel =    "<HTML>Rank: "+ qmatch.rank +", distance: " + qmatch.distance +
                "<br> Tag: " + qmatch.imageTag +"</HTML>";
            }
            imageSetInfo[i] = imageInfo;
       }
       // Display the set
       int numRows = (imageSetInfo.length+4)/ 5;
       ImageDisplays.displayMultipleImages("Images in Set of " + fileName, 0, imageSetInfo.length,  imageSetInfo, 0.5) ;
       return;
   }
   
/*-----------------------------------------------------------------------------------------------------------
    // Get the entry from the matching query image corresponding to the given image
    */
    protected RegionQueryResultTable.RegionMatch getQueryMatch(ResultTable resultTable, 
          int selectedRow, String imageTag)
    {
            ArrayList<RegionQueryResultTable.RegionMatch> returnedMatches =
                  ((RegionQueryResultTable) resultTable).getRegionMatches(selectedRow);
            for (int i = 0; i < returnedMatches.size(); i++)
            {
                if (returnedMatches.get(i).imageTag.equals(imageTag))
                    return returnedMatches.get(i);
            }
            return null;
    }

   /*---------------------------------------------------------------------------------------------------------
    *  Check if face coordinate annotations are available for this image
    *---------------------------------------------------------------------------------------------------------*/ 
    public boolean isAnnotationAvailable( String imageFileURL)
    {
        if (image2AnnotMap == null)
            return false;
        File file = new File(imageFileURL);
        String imageName = file.getName();
        String annotString = image2AnnotMap.get(imageName);
        return (annotString != null);
    }
        
  /*---------------------------------------------------------------------------------------------------------
    * Get the face coordinates from the annotated data provided externally
    *---------------------------------------------------------------------------------------------------------*/ 
    protected String getFaceAnnotations( String imageFileURL)
    {
        // Get only the filename part from the input URL
        File file = new File(imageFileURL);
        String imageName = file.getName();
        String annotString = image2AnnotMap.get(imageName);
        if (annotString == null)
            return null;
        
        // only get the face and landmark coordinates
        String[] annots = annotString.split("\t");
        String annotCoords = annots[0];
        for (int i = 1; i < annots.length; i++)
        {
            if (annots[i].startsWith("i") || annots[i].startsWith("m") || annots[i].startsWith("n"))
                annotCoords += "\t"+ annots[i];
        }
        System.out.println ("Image: " + imageName+ ", Annotations: " + annotCoords);
        return annotCoords;
    }
}
