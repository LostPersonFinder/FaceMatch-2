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
 * FaceRegion.java
 */

package fm2client.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.Rectangle;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
/**
 * A data structure to hold the coordinates landmark data for a face region.
 *
 */
public class FaceRegion
{
    private static Logger log = Logger.getLogger(FaceRegion.class);
    
    // Identifier of a landmark  in a face
    public  static class LandmarkData
    {
        public String landmark;     // face or eye, mouth, nose, etc
        public Rectangle coord;   // rectangle:  relative top left corner, width, height
        public LandmarkData() {};
           
    }
    
    public boolean isProfile;          // false if full face, true if  in-profile
    public Rectangle regionCoord;
    int numLandmarks;           // number of Landmarks included (may be zero)
    ArrayList <LandmarkData> landmarkInfo;
    
    public static String RegionSeparator = "\t";
    protected static String RegionFormat = "T{[x,y;w,h](\t)*L[x2,y2;w2,h2]}"; // where T => Type (f|p), L=> Landmark (i|n|m)
  
    /*-----------------------------------------------------------------------------------------*/
    /* Split the image Specification into its individual components
    * If landmarks are present, they are part of each region
    *------------------------------------------------------------------------------------------*/
     public static String[] getRegionStrings(String fmRegionSpec)
     {
         String regions = fmRegionSpec.replaceAll("^\\s+","").replaceAll("\\s+$","");
         String[] regionStrs;
         if (regions.contains("}"))               // with landmarks)
          {
                String[] segs =regions.split("\\}\t");
                regionStrs = new String[segs.length];
                for (int i = 0; i < segs.length; i++)
                    regionStrs[i] = segs[i].concat("}");        // putback the "}"
         }
         else
             regionStrs = regions.split("\t");   
        return regionStrs;
     }
     
    /*-----------------------------------------------------------------------------------------*/
    /* Split the image Specification and return individual faces 
     * All landmark data is ignored.
    *------------------------------------------------------------------------------------------*/
     public static String[] getFaceRegion(String fmRegionSpec)
     {
         String[]  regions =  getRegionStrings( fmRegionSpec);
         String[] faceRegions = new String[regions.length];
         for (int i = 0;i < regions.length; i++)
         {
             String region = regions[i];
             if (region.contains("{"))
             {
                 region = region.replace("{", "");
                 int index = region.indexOf("]");           // get the first right bracket
                 faceRegions[i] =  region.substring(0, index);
             }     
            else
                faceRegions[i] = regions[i];   
         } 
         return faceRegions;
     }
     
     
     
     /*-------------------------------------------------------------------------------------------------------------*/
     /** Separate the imageTag from the face regions if specified together
      * 
      * @param imageID
      * @return Array with first element indicating just the imageTag
      *------------------------------------------------------------------------------------------------------------*/
     public static  String[]  getImageRegionsFromSpec(String imageSpec)
     {
         String[] components =  getRegionStrings(imageSpec);
         String[] faceRegions = null;
         int nc = components.length;
         if ( nc > 1)
         {
             faceRegions = new String [nc-1];
             for (int i = 0; i <nc; i++)
                 faceRegions[i] = components[i+1];
         }
       return faceRegions;
     }
     
     /*-------------------------------------------------------------------------------------------------------------*/
     /** Separate the imageTag from the face regions if specified together
      * 
      * @param imageID
      * @return Array with first element indicating just the imageTag
      *------------------------------------------------------------------------------------------------------------*/
     public static  String getImageNameFromSpec(String imageSpec)
     {
         String[] components =getRegionStrings(imageSpec);
         return components[0];
     }
     
    /*-----------------------------------------------------------------------------------------*/
    /***
      * Parse a String returned by FaceMatch Lib as a FM Region and build 
      * the corresponding RegionData Java object
      * @param String with regionInfo
      * 
      * @return   Region Data object
      * */
      public static FaceRegion[]  getRegionsFromFMResponse(String fmRegionSpec)
     {
        String[] regionStrs = fmRegionSpec.split(RegionSeparator);
        ArrayList<FaceRegion> regions = new ArrayList();
        for (int i = 0; i < regionStrs.length; i++)
        {
            // skip empty strings
            if (regionStrs[i].replaceAll("//s+","").length() == 0)
                continue;
           FaceRegion region = FaceRegion.parseFMString(regionStrs[i]);
           if (region != null)
               regions.add(region);
        }
        FaceRegion[]   regionArray= new FaceRegion[regions.size()];
        regions.toArray(regionArray);
        return regionArray;
     }
      
     /*------------------------------------------------------------------------------------------------*/
      /** Inflate a Face Region by a given factor 
       * 
       * @param inRegion
       * @param factor - inflection factor (> 0)
       * @return  inflated region
       */
      public static FaceRegion inflate(FaceRegion inRegion, double factor)
      {
          if ( factor <= 0)
              return null;
          // first clone it and then change rectangle dimensions
          FaceRegion inflRegion = inRegion.clone();
          Rectangle coords = inflRegion.regionCoord;
          coords.x = (int)(coords.x*factor);
          coords.y = (int)  (coords.y*factor);
          coords.width = (int)  (coords.width*factor);
          coords.height= (int)  (coords.height*factor);
          
          // change dimension of landmarks too
          if (inRegion.landmarkInfo == null || inRegion.landmarkInfo.size() == 0)
              return inflRegion;
          
          // inflate the dimension of each landmark
          int nl = inRegion.landmarkInfo.size();
          for (int i = 0; i < nl; i++)
          {
              LandmarkData landmarkInfo = inflRegion.landmarkInfo.get(i);
              Rectangle lrect = landmarkInfo.coord;
              lrect.x = (int)lrect.x;
              lrect.y = (int)lrect.y;
              lrect.width = (int)lrect.width;
              lrect.height = (int)lrect.height;
          }
          return   inflRegion;
      }
       /*------------------------------------------------------------------------------------------------*/
      /** Merge overlapping Regions in a set , taking  into account the landmarks
       * Two regions are defined as overlapping if there is 25% overlap in size
       * 
       * @param inData
       * @return 
       * TBD: Take into account the landmarks. 
       */
      /*------------------------------------------------------------------------------------------------*/
      public static FaceRegion[]  mergeOverlaps(FaceRegion[] inRegions)
      {
         ArrayList<FaceRegion> outRegions = new ArrayList();;
         int n = inRegions.length;
         boolean[]  mergedEntries = new boolean[n];          // entries that are aleady merged
         
         FaceRegion  region0  =  inRegions[0];
         
         for (int i = 1; i < n; i++)
         {
             if (mergedEntries[i] == true)
                 continue;
                 
                Rectangle rect0 = region0.regionCoord;
                Rectangle rect1 = inRegions[i].regionCoord;
                if (!rect0.intersects(rect1))
                    continue;
                
                // get overlapping area
                Rectangle rcommon  = rect0.intersection(rect1);
                int area0 = rect0.width*rect0.height;       // area in pixels
                int area1 = rect1.width*rect1.height;
                int largerArea = Math.max(area0, area1);   
                FaceRegion  largerRegion = (largerArea == area0 ) ? region0 : inRegions[i];

                int areaCommon = rcommon.width * rcommon.height;
                if (areaCommon < largerArea*0.25)
                    continue;
                 int entryToMerge  = (largerArea == area0) ? -1 : i;
                 
                 
                FaceRegion outRegion = largerRegion.clone();
                outRegion.regionCoord = rect0.union(rect1);
                outRegions.add(outRegion);
                if (entryToMerge > 0)
                     mergedEntries[entryToMerge] = true;
         }
         // add the ones that were not merged
         for (int i = 0; i < n; i++)
             if (mergedEntries[i] == false)
                 outRegions.add(inRegions[i]);
         
         // return as an array
         FaceRegion[] outputData = new FaceRegion[outRegions.size()];
         outRegions.toArray(outputData);
          return outputData;
      }
          
/*-----------------------------------------------------------------------------------------*/
        public  FaceRegion clone()
        {
            String regionStr =  this.toJSONString();
            return FaceRegion.convertFromJSONString(regionStr, true);
        } 
            
      
      
      /***
      * Parse a String  as a FaceRegion and build 
      * the corresponding RegionData Java object
      * @param String with regionInfo
      * 
      * @return   Region Data object
      * */
    public static FaceRegion parseFMString(String regionString)
    {
        try
        {
            FaceRegion region = new FaceRegion();
            regionString = regionString.replaceAll("\\s+", "").trim();
            char firstCh = regionString.charAt(0);
            if (firstCh == 'f')
            {
                region.isProfile = false;
            } 
            else if (firstCh == 'p')
            {
                region.isProfile = true;
            } 
            else
            {
                log.error("Invalid Region marker from FaceMatch Library");
                return null;
            }
            region.landmarkInfo = new ArrayList<LandmarkData>();

            regionString = regionString.substring(1).replaceAll("\\{|\\}", "");       // remove the bracket
          
            // For easier splitting, do the following
            regionString = regionString.replaceAll("\\]", "\\]\\|");
            String[] landmarks  = regionString.split("\\|");
            List<String> landmarkStr = new ArrayList();
            for (int i = 0; i < landmarks.length; i++)
            {
               if (landmarks[i].length() > 0)
                   landmarkStr.add(landmarks[i]);
            }
            region.numLandmarks = landmarkStr.size() - 1 ;           // first one is for faces

            // get the face coordinates from the first segment
            String fc = landmarkStr.get(0);
            region.regionCoord = getCoordinates(fc);

            // get the landmarks and their coordinates
            for (int i = 1; i < landmarkStr.size(); i++)
            {
                LandmarkData lm = new LandmarkData();
                String lmstr = landmarkStr.get(i);
                String type =  lmstr.substring(0, 1);
                if ( type.equals("i"))
                    lm.landmark = "eye";
                else if ( type.equals("m"))
                    lm.landmark = "mouth";
                else if ( type.equals("n"))
                    lm.landmark = "nose";
                else
                {
                    log.error("Ignoring unknowm landmark type: " + type);
                    continue;
                }

                lmstr = lmstr.substring(1);
                lm.coord = getCoordinates(lmstr);
                region.landmarkInfo.add(lm);
            }
            return region;
        } 
        catch (Exception e)
        {
            log.error("Invalid FaceFinder Region String format; " + regionString);
            return null;
        }
    }
    /*-------------------------------------------------------------------------------------------*/
    public static FaceRegion[] getRegions(String[] regionStrings)
    {
        int n = regionStrings.length;
        FaceRegion[] regions = new FaceRegion[n];
        for (int i = 0; i <n; i++)
        {
            regions[i] =  parseFMString(regionStrings[i]);
        }
        return regions;
    }

 /*--------------------------------------------------------------------------*/    
 // Split the String using RegularExpression - not used 
    //
  public static List<String> splitStr(String string, String splitRegex)
      {
    List<String> result = new ArrayList<String>();

    Pattern p = Pattern.compile(splitRegex);        // i|n|m
    Matcher m = p.matcher(string);
    int index = 0;
    int i = 0;
   String splitString = "";                // segment splitter
    while (index < string.length()) 
    {
        if (m.find())
        {
            int splitIndex = m.end();
            String segment = string.substring(index,splitIndex-1);
             result.add(splitString+segment);           // add the landmark symbol
            index = splitIndex;
            splitString = m.group();                            // preceeds nect segment
            i++;
        } 
        else
        {
             String segment = string.substring(index);
             result.add(splitString+segment);  
             break;
        }
    }
    return result;
}
  /*---------------------------------------------------------------------------------------*/
  /** Get the Rectangle in format "[x, y; w, h]".
   * 
   * @param coordStr
   * @return 
   */
  protected static Rectangle getCoordinates(String coordStr)
  {
        coordStr = coordStr.replaceAll("\\[|\\]", "");       // remove the bracket
        String[] coords = coordStr.split(",|;");
        Rectangle r = new Rectangle ();
        r.x =  Integer.parseInt(coords[0]);
        r.y =  Integer.parseInt(coords[1]);
        r.width =  Integer.parseInt(coords[2]);
        r.height =  Integer.parseInt(coords[3]);
        return r;
  }
 /*------------------------------------------------------------------------------------------------------------*/ 
    public static boolean isValidRegionFormat(String faceRegionString)
    {
        String[]  regions = getRegionStrings(faceRegionString);
        for (int i = 0; i < regions.length; i++)
        {
            if (parseFMString(regions[i]) == null)
                return false;
        }
        return true;
    }
    /*-----------------------------------------------------------------------------------------*/
    // Create a faceRegion String from its components (no landmarks)
    // It is the reverse of parsing it
    //--------------------------------------------------------------------------------------------*/
    public static String buildRegionString(boolean isProfile, int x, int y,  int w, int h)
    {
        String region = (isProfile? "p" : "f");
        String rect = String.valueOf(x)+","+ String.valueOf(y)+";"
                               + String.valueOf(w)+","+ String.valueOf(h);
        return (region+"["+rect+"]");
    }
    /*-----------------------------------------------------------------------------------------*/
    // Create a faceRegion String from its components (no landmarks)
    // It is the reverse of parsing it
    //--------------------------------------------------------------------------------------------*/
    public static String buildRegionSpec(String imageTag, boolean isProfile, int x, int y,  int w, int h)
    {
        String regionStr = buildRegionString( isProfile,  x,  y,   w,  h);
        return (imageTag+RegionSeparator+regionStr);
    }
    
/*------------------------------------------------------------------------------------------------------------*/
    /***
    * Parse a Jason String and build the corresponding RegionData object
    * @param String with regionInfo
    * @param includeLandmarls: currently ignored as the serverside does not receive that info
    * from the client
    * @return   Region Data object
    */
     public static FaceRegion convertFromJSONString(String regionStr, boolean includeLandmarks)
     {
        JSONParser parser = new JSONParser();
        try
        {
            Object obj  = parser.parse(regionStr);
            JSONObject  regionObj = (JSONObject)obj;
            
            FaceRegion  regionData = new FaceRegion();
            // get face or profile
            String type = (String) regionObj.get( "Type");
            regionData.isProfile = type.equalsIgnoreCase( "profile");
            
            // get the rectangle coordinates
            Rectangle rect = new Rectangle();
            JSONObject  rectObj = (JSONObject) regionObj.get("coordinates");
            rect.x =((Long)rectObj.get("x")).intValue();
            rect.y =  ( (Long)rectObj.get("y")).intValue();
            rect.width =  ( (Long)rectObj.get("w")).intValue();
            rect.height =  ( (Long)rectObj.get("h")).intValue();
            regionData.regionCoord = rect;
            return regionData;
            
            // TBD: landmarks
        }
        catch (Exception e)
        {
            log.error("Got JSON Exception in parsing Region data", e);
            return null;
        }
     }
     
  
  
   /*------------------------------------------------------------------------------------------ 
    // create a String version of it using JSON
    *------------------------------------------------------------------------------------------*/
    public String toJSONString()
    {
        JSONObject  jsonObject = convertToJSONObject();
        return jsonObject.toString();
    }
    
   /*------------------------------------------------------------------------------------------
    // Convert the region data to a JSON object   
    /*---------------------------------------------------------------------------------------*/
     public  JSONObject convertToJSONObject()
      {    
        JSONObject regionObj = new JSONObject();
        String ps = isProfile ? "profile": "face";       // profile or face
        regionObj.put("Type", ps);
        
        // add coordinates
        JSONObject  coordObj = new JSONObject();        // maintain order
         coordObj.put("x", new Integer(regionCoord.x));
         coordObj.put("y", new Integer(regionCoord.y));
         coordObj.put("w",  new Integer(regionCoord.width));
         coordObj.put("h",  new Integer(regionCoord.height));
         regionObj.put("coordinates",  coordObj);
         
         // add landmark info 
          if (numLandmarks > 0)
          {
               regionObj.put("numLandmarks",  new Integer(numLandmarks));
                // add type and relative coordinate of each landmark
                JSONArray lmArray = new JSONArray();
                for (int i = 0; i < numLandmarks; i++)
                {
                    LandmarkData  lmData = landmarkInfo.get(i);
                    JSONObject lmObj = new JSONObject();
                    lmObj.put("Type", lmData.landmark);       // eye, nose or mouth

                    JSONObject lmCoordObj = new JSONObject();
                    lmCoordObj.put("x", new Integer(lmData.coord.x));
                    lmCoordObj.put("y", new Integer(lmData.coord.y));
                    lmCoordObj.put("w", new Integer(lmData.coord.width));
                    lmCoordObj.put("h", new Integer(lmData.coord.height));
                    lmObj.put("relativeCoords",  new JSONObject(lmCoordObj));
                    lmArray.add(lmObj);
                }
                 regionObj.put("landmarks",  lmArray);
          }     // end of Landmark objects in the region
          return regionObj;     
    }
    /*-----------------------------------------------------------------------------------------------------------*/
      /***
      * Convert a list of Region information to a Jason String form
      * @param regionArray
      * @return  String version of the Region Data Array
      */
     public static String convertToString( ArrayList<FaceRegion> regionArray)
     {
         JSONArray jsonArray = new JSONArray();
         for (int i = 0; i < regionArray.size(); i++)
             jsonArray.add(regionArray.get(i).convertToJSONObject());
         return jsonArray.toString();
     }
     
    
         
    /**--------------------------------------------------------------------------------
     *  Local test
     * ----------------------------------------------------------------------------------
     */
             
     public static void main(String[] args)
    {
        FaceRegion rd  = new FaceRegion();
        rd.isProfile = false;
        rd.regionCoord =  new Rectangle(10, 20, 110, 120);
        rd.numLandmarks = 4;
        
       ArrayList<FaceRegion.LandmarkData> lmArray =  new ArrayList();
   
        LandmarkData lmData1 =  new FaceRegion.LandmarkData();
        lmData1.landmark = "eye";
        lmData1.coord = new Rectangle(20, 20, 15, 25);
        
        LandmarkData lmData2 = new FaceRegion.LandmarkData();
        lmData2.landmark = "eye";
        lmData2.coord = new Rectangle(80, 20, 15, 25);
        
        LandmarkData lmData3 = new FaceRegion.LandmarkData();
        lmData3.landmark = "nose";
        lmData3.coord = new Rectangle(60, 40, 15, 10);
            
         LandmarkData lmData4 =new FaceRegion.LandmarkData();
        lmData4.landmark = "mouth";
        lmData4.coord = new Rectangle(60, 50, 20, 10);    

        ArrayList<LandmarkData> landmarks = new ArrayList();
        landmarks.add(lmData1);
        landmarks.add(lmData2);
        landmarks.add(lmData3);
        landmarks.add(lmData4);
        
        rd.landmarkInfo = landmarks;
       
        //now conver it to String
        String jsonStr = rd.toJSONString();
        System.out.println(jsonStr);
        
        
        // convert it back to Regions
        String JSONString = new String(
         "{\"Type\":\"face\" "+
            " \"numLandmarks\":4, " +
            "\"coordinates\":{\"w\":110,\"x\":10,\"h\":120,\"y\":20}, " +
            "\"landmarks\":" +
            " [ "+
                " {\"Type\":\"eye\",\"relativeCoords\":{\"x\":20,\"h\":25,\"y\":20,\"w\":15}},"+
                " {\"Type\":\"eye\",\"relativeCoords\":{\"x\":80,\"h\":25,\"y\":20,\"w\":15}}," +
                "{\"Type\":\"nose\",\"relativeCoords\":{\"x\":60,\"h\":10,\"y\":40,\"w\":15}}," +
                "{\"Type\":\"mouth\",\"relativeCoords\":{\"x\":60,\"h\":10,\"y\":50,\"w\":20}}," +
          "]" + 
        "}");
        FaceRegion regionData = FaceRegion.convertFromJSONString(jsonStr, true);
        
        String fmRegionSpec = "f{[10,20;110,120]    i[20, 20; 25,15]   i[80, 20, 25, 15]    m[60,50;20,10]n[60,40,15,10]}";
        FaceRegion[]  regions = getRegionsFromFMResponse( fmRegionSpec);
        System.out.println("** FMRegions to JSON Sting***\n" + regions[0].toJSONString());
        System.exit(0);
    }

}
