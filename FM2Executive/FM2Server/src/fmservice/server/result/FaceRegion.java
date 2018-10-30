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
package fmservice.server.result;
import fmservice.server.util.Utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.awt.Rectangle;
import java.awt.Point;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
/**
 * A data structure to hold the coordinates landmark data for a face region.
 *
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
    public int numLandmarks;           // number of Landmarks included (may be zero)
    public ArrayList <LandmarkData> landmarkInfo;
    
    public static String RegionSeparator = "\t(f|p)";
    protected static String RegionFormat = "T{[x,y;w,h](\t)*L[x2,y2;w2,h2]}"; // where T => Type (f|p), L=> Landmark (i|n|m)
  
    /*-----------------------------------------------------------------------------------------*/
    /* Split the image Specification into its individual components
    * If landmarks are present, they are part of each region
    *------------------------------------------------------------------------------------------*/
     public static String[] getRegionStrings(String fmRegionSpec)
     {
         if (fmRegionSpec == null)
             return null;
         log.trace("FaceRegion: fmRegionSpec=" + fmRegionSpec);
         String regions = fmRegionSpec.replaceAll("^\\s+","").replaceAll("\\s+$","");
         String spliPattern = "\t(f|p)";
         ArrayList<String> segments = Utils.splitNKeepPattern(regions, spliPattern);

         int n = segments.size();
         String[] regionStrs = new String[n];
         for (int i = 0; i < n; i++)
             regionStrs[i] = segments.get(i).replaceFirst("^\t", "");       // remove leading tab char
       
         for (int i = 0; i < regionStrs.length; i++)
         {
             log.trace("Region " + i +": " + regionStrs[i]);
         }
        return regionStrs;
     }
     
    /*-----------------------------------------------------------------------------------------*/
    /* Split the image Specification into its individual components
    * If landmarks are present, they are part of each region
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
     
     
          
    /*-----------------------------------------------------------------------------------------*/
    /* Split the image Specification in an incoming request into its individual components 
    * The faces are assumed to be concatenated with "," and without landmarks
    * It may or may not also have the f|p designation
    *------------------------------------------------------------------------------------------*/
     public static String[] getRegionsInRequest(String fmRegionSpec)
     {
        if (fmRegionSpec == null)
            return null;
        fmRegionSpec = fmRegionSpec.replaceAll("\\^", "").trim();
        String[] regions = fmRegionSpec.split(",\\s*f|p");
        int len =  regions.length;
        if (len == 1)
               return regions;         

        List<String> regionList = splitStr(fmRegionSpec, "f|p");
        String[] faceRegions = new String[regionList.size()];
        regionList.toArray(faceRegions);
        
        for (int i = 0; i < faceRegions.length; i++)
            faceRegions[i] = faceRegions[i].replaceAll("(\\s*,\\s*)$", "");
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
      * Parse a String returned by FaceMatch Lib as a FM Region 
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
            regionStrs[i] = regionStrs[i].replace("//s*,//s*$", "");
            // skip empty strings
            if (regionStrs[i].length() == 0)
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
      
      /*-------------------------------------------------------------------------------------------------------------------------*/
      // Sort the faceRegions accrding to distance from center
      
      public static String[] sortRegions(String[] regions)
      {
          HashMap<FaceRegion,  String> inputList = new HashMap();
          int nf = regions.length;
          if (nf ==1)
              return regions;
          
          ArrayList<FaceRegion> faceList = new ArrayList();
          Rectangle r = new Rectangle() ;
          for (int i = 0; i < nf; i++)
          {
              FaceRegion face = parseFMString(regions[i]);
              if (i == 0)
                  r = face.regionCoord;
              else
                  r = r.union(face.regionCoord);
              faceList.add(face);
              inputList.put(face, regions[i]);
          }
          
          // To arrange according to center from distance, compute the imageRectangle which includes all the faces
          Point center = new Point( (r.x+r.width)/2, (r.y+r.height)/2);
          
          // Now arrange the faces as frontals and then profiles, using the comparator and distance from center
           RegionComparator comp = new FaceRegion.RegionComparator(center);
          Collections.sort(faceList, comp);
          
          String[] sortedRegions = new String[nf];
          for (int i = 0; i < nf; i++)
          {
              FaceRegion sortedFace = faceList.get(i);  
              sortedRegions[i] = inputList.get(sortedFace); 
          }
          return sortedRegions;
        } 

      /*-------------------------------------------------------------------------------------------------------------*/
         public  FaceRegion clone()
        {
            String regionStr =  this.toJSONString();
            return FaceRegion.convertFromJSONString(regionStr, true);
        } 
  
/*-----------------------------------------------------------------------------------------*/
     
      
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
                log.error("Invalid Region marker in " + regionString);
                return null;
            }
            region.landmarkInfo = new ArrayList<LandmarkData>();

            // Make sure the number of left and righr brackets match, as we delete them
            int numLB = regionString.length() - regionString.replace("{", "").length();
            int numRB =  regionString.length() - regionString.replace("}", "").length();
            if (numLB != numRB)
            {
                log.error("Invalid FaceFinder Region String format; " + regionString);
                return null;
            }
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
                    log.warn("Ignoring unknowm landmark type: " + type);
                    continue;
                }

                lmstr = lmstr.substring(1);
                lm.coord = getCoordinates(lmstr);
                region.landmarkInfo.add(lm);
                region.numLandmarks++;
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
            if (segment.length() == 0)      // match at the first character
                continue;
             result.add(splitString+segment);           // add the landmark symbol
            index = splitIndex;
            splitString = m.group();                            // preceeds next segment
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
  public static Rectangle getCoordinates(String coordStr)
  {
        coordStr = coordStr.replaceAll("p|f", "");      // strip the f|p tag, if any
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
            /*-----------------------------------------------------------------------------------------------*/  
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

   /*-----------------------------------------------------------------------------------------------*/  
     // 1, 0, -1 means farther,  equal, closer respectively
      protected static class RegionComparator implements Comparator <FaceRegion> 
     {
         Point center;
         public RegionComparator(Point imageCenter)
        {
            center = imageCenter;
        }
         public int compare(FaceRegion f1, FaceRegion f2)
         {
          // frontal images come before profile images - so profile is larger
          if (f1.isProfile  &&  !f2.isProfile)
            return 1;
          else if (!f1.isProfile && f2.isProfile)
              return -1;
          
          // Within the same frontal/profile group, compare to center from the origin
          Point f1Origin =  new Point((f1.regionCoord.x+f1.regionCoord.width)/2, (f1.regionCoord.y+f1.regionCoord.height)/2);
          double f1Distance = Math.sqrt( (center.x - f1Origin.x)^2 + (center.y - f1Origin.y)^2 );
          
           Point f2Origin =  new Point((f2.regionCoord.x+f2.regionCoord.width)/2, (f2.regionCoord.y+f2.regionCoord.height)/2);
          double f2Distance = Math.sqrt( (center.x - f2Origin.x)^2 + (center.y - f2Origin.y)^2 );
          if (f2Distance > f1Distance)          // f1 is closure
              return -1;         // I am closer
          else if (f2Distance <  f1Distance)
              return 1;            // I am farther
          else
              return 0;
        }
    }
         
    /**--------------------------------------------------------------------------------
     *  Local test
     * ----------------------------------------------------------------------------------
     */
             
     public static void main(String[] args)
    {
      /*  FaceRegion rd  = new FaceRegion();
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
    */    
        String[] fmRegionSpecs = { "f{[10,20;110,120]    i[20, 20; 25,15]   i[80, 20, 25, 15]    m[60,50;20,10]n[60,40,15,10]}",
        "f{[122,238;285,311]	a[adult]	t[light]	g[male]	d[00001]	i[163,53;83,83]	n[115,97;62,138] 	i[39,55;83,83]	m[82,209;124,41]}",
        "f{[124,231;285,308]	a[adult]	t[light]	g[male]	d[00001]	i[39,56;83,83]	i[163,52;83,83]	n[115,99;62,124]	m[86,208;124,41]}",
        "f{[162,244;235,248]	a[adult]	t[light]	g[male]	d[00002]	i[32,48;68,68]	n[95,86;51,64]	m[75,169;102,34]    i[134,40;68,68]	}",
        "f{[149,247;239,237]	a[adult]	t[light]	g[male]	d[00002]	i[32,50;69,69]	i[136,40;69,69]	n[95,90;52,66]	m[71,166;104,35]}",
        "f{[57,208;359,336]	a[adult]	t[light]	g[male]	d[00002]	i[49,66;104,104]	i[205,70;104,104]	n[146,124;78,104]	m[115,240;156,52]}"};
        
  /*      for (int i = 0; i < fmRegionSpecs.length; i++)
        {
            System.out.println("FMRegionString: " + fmRegionSpecs[i] );
            FaceRegion[]  regions = getRegionsFromFMResponse( fmRegionSpecs[i]);
             System.out.println ("Number of face regions: " + regions.length);
            for (int j = 0; j < regions.length; j++)  
            {
                    System.out.println("** FMRegion " + j + " to JSON Sting***\n" + regions[j].toJSONString() +
                "\n--------------------------------------------");
            }
        }
          */
         String[] fmRegionSpecs1 = {
                 "f{[10,20;110,110]    i[20, 20; 25,15]   i[80, 20; 25, 15]    m[60,50;20,10]n[60,40;15,10]}, p[200,100; 60, 60]",
                 "f[10,20;110,110] , p[50, 45; 100, 150],  f[40,20; 100, 100]",
                 "f[110, 120; 50,50]",
                "p[210, 220; 80,80]",
                 "p[10,20;110,110] , f[50, 45; 100, 150]"
                };
           
         for (int i = 0; i < fmRegionSpecs1.length; i++)
        {
            System.out.println("\nFMRegionString: " + fmRegionSpecs1[i] );
            String[]  regions = getRegionsInRequest( fmRegionSpecs1[i]);
             
            System.out.println ("Number of face regions: " + regions.length + ",  Regions are: ");
            for (int j = 0; j < regions.length; j++)  
            {
                    System.out.println(regions[j] + "    ");
             }
        }
        System.exit(0);
    }

}
