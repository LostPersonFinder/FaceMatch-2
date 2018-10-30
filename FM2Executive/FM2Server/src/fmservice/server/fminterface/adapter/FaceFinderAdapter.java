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
package fmservice.server.fminterface.adapter;

import java.util.HashMap;

import fmservice.server.fminterface.proxy.FRDProxy;
import fmservice.server.fminterface.proxy.FaceFinderProxy;

import fmservice.httputils.common.ServiceConstants;
import fmservice.server.result.FaceRegion;

import fmservice.server.util.Timer;

import org.apache.log4j.Logger;

/**
 *  Adapter to translate FaceFinder  requests coming from the client  to one or more FaceMatch 
 * library operation calls. Calls to the FM library are made through the FM proxy classes, which pass
 * data back and forth to FM using the native JNI interface
 * <p> 
 * Note: Presently we deal with human faces only
 * 
 *
 */
public class FaceFinderAdapter extends FaceMatchAdapterBase 
                                                            implements ServiceConstants
{
    private static Logger log = Logger.getLogger(FaceFinderAdapter.class.getName());
    
    public FaceFinderAdapter()
    {
        super();
    } 
    
    /** 
     *   Example of a C++ FMLib function related to detecting  faces
     * FaceFinder(FaceRegionDetector & FRD, const string & BasePath, const string & ImgFNLine,
      *      const string & FaceRegions, unsigned flags=selective|HistEQ);
     * @param FRD                         initialized face region detector
     * @param BasePath	 input image base path
     * @param ImgFNLine	 input image file name optionally followed by tab-separated image attributes
     * @param FaceRegions           new-line separated list of face regions with optional attributes; default is empty.
     * 	Each rectangular region is given by {f|p}[x,y;w,h] optionally followed by tab-separated attributes, e.g.<br>
     * 		ID\\tf[22,36;50,60]\\tJoe Smith\\tM\\t32\\n<br>
     * 		ID\\tp[29,32;60,62]\\tMary Bell\\tF\\t25\\n<br>
     * 		...<br>
     * 	If no region list is given, the faces are detected automatically.
     * 
     * @param flags optional processing parameters; default is none
     *
    **/
 
    /*-------------------------------------------------------------------------------------------------------------------------*/
    /** 
     * Get (detect) the face regions in the given image.
     * <p>
     * Note: If the image is given as an URL by the client, is is assumed to be
     * already copied to a  (temporary) local directory from where FMLib may access it.
     * 
     * @param localImageDir   Local directory containing the image
     * @param localimageFileName    Image file name in the local directory
     * @param showLandmarks   If coordinates of the landmarks (eye, nose, mouth etc.) be returned in each face
     * @param inflateBy   If the caller wants the face regions to be inflated by a certain factor (not used currently)
     * @param useGPU whether or not FMLIb should use GPU for its computations
     * @param perfOption - Type of performance chosen by a user. If none specified, used ACCURACY 
     *                                 
     * @return A HashMap of various parameters related to detected faces, if any 
     *-------------------------------------------------------------------------------------------------------------------------*/

     public  HashMap <String, Object> getFaces(String localImageDir, String localImageFileName, 
        String[] inputRegions,  boolean showLandmarks, double inflateBy, boolean useGPU, String perfOption)
     {  
        HashMap <String, Object> outParams = new HashMap();  
        String statusMsg = "";
        if (!isInitialized())
        {
            setStatus(outParams, FM_INIT_ERROR,  "FaceMatch Library interface not initialized");
            return  outParams;
        }
        localImageDir = localImageDir.replaceAll("/+$", "");
        String localImageFile  = localImageDir + "/" + localImageFileName;

        //FRDProxy frd =  getDefaultFRD(useGPU);      
        FRDProxy frd = getFRDWithLock(useGPU);
        if (frd == null)
        {
           statusMsg =  "Could not create FaceRegionDetector.  Cannot proceed.";
           setStatus( outParams, BAD_FRD,  statusMsg);
           return outParams;
        }
        
        if (log.isDebugEnabled())
            FaceFinderProxy.setVerboseLevel(2);
        else
            FaceFinderProxy.setVerboseLevel(0);
        
        if (perfOption == null)
            perfOption = OPTIMAL;
        
        String[] faceRegions;
        String[] returnedResult;
        String perfUsed = "";
        Timer timer = new Timer();
        
     /*   if (perfOption.equalsIgnoreCase(PROGRESSIVE))
        {
            FaceDetectionInfo retInfo = extractFaceRegionsInImageProgressively(localImageFile,  inputRegions, frd );
            outParams.put(PERF_OPTION_USED, retInfo.optionUsed);  
            returnedResult = retInfo.detectedFaces;
        }
        else
        {  
            long faceDetectionFlags;
            faceDetectionFlags = FaceMatchAdapterBase.getFacefinderFlags(perfOption);
            if (showLandmarks) 
                faceDetectionFlags = faceDetectionFlags | LandmarkFlags;
            returnedResult = extractFaceRegionsInImage( localImageFile, inputRegions,  frd,  faceDetectionFlags);
        }
        */
        
         FaceDetectionInfo fdInfo  = extractFaceRegionsInImage( localImageFile, inputRegions,  frd,  perfOption);
         outParams.put(FF_OPTION, perfOption);
         boolean gpuUsed =  frd.usingGPU();
         outParams.put(PERF_OPTION_USED, fdInfo.optionUsed);  
         outParams.put(GPU_USED, gpuUsed);
         
         returnedResult = fdInfo.detectedFaces;
         
        // Check if got an error from the Facematch library
        if (returnedResult != null && returnedResult.length == 2 &&  
                returnedResult[0].equals(String.valueOf(FMLIB_OPERATION_ERROR)) )
        {
                setStatus(outParams, FMLIB_OPERATION_ERROR, returnedResult[1]);
                Timer.release(timer);
                return outParams;
        }
        else
            faceRegions = returnedResult;

         outParams.put(FACEFIND_TIME, new Float(timer.getElapsedTime()));

         // if no faces found, return here
         if (faceRegions == null || faceRegions.length == 0)      // even with rotation
         {
            statusMsg =  "No faces found in given image  using " + (gpuUsed ? "GPU" : "CPU");
            setStatus(outParams, NO_FACES_IN_IMAGE,  statusMsg);   
            log.trace("\n>>>>>  No faces detected using " + (gpuUsed ? "GPU" : "CPU"));

            Timer.release(timer);
            return outParams;
        }

        //--------------------------------------------------------------------------------------------------------------
        // Face detected. compute the display regions fromthe face regions, if wanted
        //--------------------------------------------------------------------------------------------------------------
        String[] displayRegions = faceRegions;          // use: refineFaceRegions(faceRegionMarker,  inflateRate);
        String displayRegion = displayRegions[0];
        for (int j = 1; j < displayRegions.length; j++)
            displayRegion = displayRegion.concat(FaceRegion.RegionSeparator).concat(displayRegions[j]);

        outParams.put(FACE_REGIONS, faceRegions);
        outParams.put(DISPLAY_REGIONS, displayRegions);
        setStatus(outParams, SUCCESS, "Detected " + faceRegions.length + " face(s) successfully.");
         
        // completion
        Timer.release(timer);
        return outParams;
      }
     

     /**------------------------------------------------------------------------------------------------------
     * Split the string with face region markers into individual faces, and inflate the
     * regions as specified. (Useful for viewing by the client)
     * 
     *  If any  face region overlaps with another, combine the two rectangles to a single one
     * *----------------------------------------------------------------------------------------------------------*/
    protected String[] refineFaceRegions(String annotatedRegionSpec, double inflateBy)
    {
        if (annotatedRegionSpec == null || annotatedRegionSpec.length() == 0)
            return null;

        FaceRegion[]  faceRegions = FaceRegion.getRegionsFromFMResponse(annotatedRegionSpec);
        int nr = faceRegions.length;
        if (nr == 1 && inflateBy <= 0.0)            // 0 means no inflation, and we don't deflate
            return (new String[ ]{ faceRegions[0].toString()}) ;

        // Merge regions -- TBD:
        FaceRegion[]  mergedRegions = FaceRegion.mergeOverlaps(faceRegions);
         int nmr = mergedRegions.length;
        
        FaceRegion[] inflatedRegions;
        if (inflateBy <= 0)
            inflatedRegions = mergedRegions;
        else
        {   

             inflatedRegions = new FaceRegion[mergedRegions.length];
             for (int i = 0; i < nmr; i++)
             {
                 double factor = 1+inflateBy;
                 inflatedRegions[i] =  FaceRegion.inflate(mergedRegions[i], factor);
             }
        }
        String[]  regionStr = new String[nmr];
        for (int i = 0; i < nmr; i++)
            regionStr[i] = inflatedRegions[i].toJSONString();
        
        return regionStr;
    }
   /*
            char profile;
            Rectangle rect = new Rectangle();
            ArrayList<Rectangle> faces = new ArrayList<Rectangle>();
            ArrayList<Rectangle> profiles = new ArrayList<Rectangle>();
            

            for (int i = 0; i < numFaces; i++)
            {
                profile = x[5 * i][0];

                if (profile == 'f' || profile == 'p')
                {
                    rect.X = Convert.ToInt32(x[1 + 5 * i]);
                    rect.Y = Convert.ToInt32(x[2 + 5 * i]);
                }
                else
                {
                    //merge i|n|m regions as they are wholly contained
                    continue;
                }

                rect.Width = Convert.ToInt32(x[3 + 5 * i]);
                rect.Height = Convert.ToInt32(x[4 + 5 * i]);

                if (profile == 'f')
                    faces.Add(rect);
                else
                    profiles.Add(rect);
            }


            //merge faces with associated profiles
            for (int i = 0; i < faces.Count; i++)
            {
                foreach (Rectangle r in profiles)
                {
                    //if r entirely contained in face, remove
                    if (faces[i].Contains(r))
                    {
                        profiles.Remove(r);
                        break;
                    }

                    //if face entirely contained in r, remove
                    if (r.Contains(faces[i]))
                    {
                        faces[i] = Rectangle.Union(faces[i], r);
                        profiles.Remove(r);
                        break;
                    }

                    //if r intersects within specified tolerance
                    if (overlap(faces[i], r, overlapTolerance))
                    {
                        faces[i] = Rectangle.Union(faces[i], r);
                        profiles.Remove(r);
                        break;
                    }
                }
            }

            //at this point we have faces and profiles that have no overlap 

            //promote all profiles to faces
            faces.AddRange(profiles);

            //clip to bounding box
            for (int i = 0; i < faces.Count; i++)
            {
                int xOffset = (Convert.ToInt32(faces[i].Width * (1 + inflateBy)) - faces[i].Width) / 2;
                int yOffset = (Convert.ToInt32(faces[i].Height * (1 + inflateBy)) - faces[i].Height) / 2;

                int left = Convert.ToInt32(Convert.ToDouble(faces[i].Left) - xOffset);
                int top = Convert.ToInt32(Convert.ToDouble(faces[i].Top) - yOffset);
                int right = Convert.ToInt32(Convert.ToDouble(faces[i].Right) + xOffset);
                int bottom = Convert.ToInt32(Convert.ToDouble(faces[i].Bottom) + yOffset);

                //clip ltrb
                if (left < 0)
                    left = 0;
                if (top < 0)
                    top = 0;
                if (right > bBox.Right)
                    right = bBox.Right;
                if (bottom > bBox.Bottom)
                    bottom = bBox.Bottom;

                faces[i] = Rectangle.FromLTRB(left, top, right, bottom);
            }

            string s = "";
            for (int i = 0; i < faces.Count; i++)
            {
                if (faces[i].Width != 0 &&
                    faces[i].Height != 0)
                {
                    s += '\t';

                    s += "f[";
                    s += faces[i].X;
                    s += ",";
                    s += faces[i].Y;
                    s += ";";
                    s += faces[i].Width;
                    s += ",";
                    s += faces[i].Height;
                    s += "]";
                }
            }

            return s;
        }

        static bool overlap(Rectangle a, Rectangle b, double tol)
        {
            Rectangle c = a;
            c.Intersect(b);
            double A = 0.5 * (area(a) + area(b));
            return ((area(c) / A) > tol);
        }

        static double area(Rectangle a)
        {
            return a.Width * a.Height;
        }
        */


     /*---------------------------------------------------------------------------------------------------------------*/
      public static void main(String[] args)
    {
        org.apache.log4j.BasicConfigurator.configure();         // for log4j messages
        
        String faceMatchOptionsFile;
        faceMatchOptionsFile = "<TopDir>/FM2Server/installDir/fmoptions/FFParameters.json";
 
        // String[] imageNames = { "injuredFace.jpg", "keith_multi.jpg", "threegirls.png",   "lena.png", "male_25.png"};

         String imageDir  = "<TopDir>/FM2Server/localTestDir/imagefiles/pl";
         String[] imageNames = {"keith_multi.jpg"};

         FaceFinderAdapter  ffAdapter = new FaceFinderAdapter();
         FaceFinderAdapter.init(faceMatchOptionsFile, "FaceMatchLibJni");          //"FaceMatchLibJni" ;

        // try with different options: Note: ACCURACY flags automatically includes ShowLandMark flags
         String[]  options =   {ACCURACY, OPTIMAL, SPEED, PROGRESSIVE};
        
         
         for (int i = 0; i < imageNames.length; i++)
         {
            System.out.println("------------------------------------------------------------------------------------");
            System.out.println(" >> TEST" + (i+1) + " : FaceFinderAdapter:  Testing FaceFinder for image " + imageNames[i] );
            
            boolean showLandmarks = false;
            long ffFlags;
            
            for (int j = 0; j < options.length; j++)
            {
                if (showLandmarks)
                    ffFlags = getFaceFinderWithLandmarkFlags();
                else
                    ffFlags = getFacefinderFlags(options[j]);
                System.out.println(">> Option: " + options[j] +", Show landmarks: " + showLandmarks +
                           ", FF_flags: "  + ffFlags);

                HashMap <String,  Object> outParams =
                        ffAdapter.getFaces( imageDir,  imageNames[i], null, showLandmarks, (float) 1.0, true, options[j]);     
                if (outParams == null)
                     System.out.println(" TEST" + (i+1) + " : FaceFinderAdapter:  Test failed");
                else
                {
                    java.util.Iterator<String> itr = outParams.keySet().iterator();
                    while(itr.hasNext())
                    {
                        String param = itr.next();
                        System.out.println(param + " : " +  outParams.get(param));
                    }
                    System.out.println("-- Time taken: " + (Float)outParams.get(FACEFIND_TIME) + " msec.");
                }
               System.out.println("--------------------------------------------------------------------"); 
             }
         }
    }

}
