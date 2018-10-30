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

import java.io.FileReader;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.HashMap;
import org.json.simple.JSONObject;

/**
 * Read the default parameters, options and other constants used for invoking 
 * FaceMatch operations from a Java application.
 * These are defined in a JSON file, whose name is provided as an input parameter.
 * 
 *
 */
public class FaceMatchOptionsReader
{
     private static Logger log = Logger.getLogger(FaceMatchOptionsReader.class.getName());
    
    public static HashMap ffOptionsMap = null;
    
    /*----------------------------------------------------------------------------------------------*/
   /**  Read the FaceMatch operation related parameters from 
    * the specified file in JSON format.
    * 
    * @param paramFile Name of the file containing FM parameters to be used in FMLib calls
    * @return  HashMap of parameter name vs. value
    */
    public static HashMap  readFFOptions(String paramFile)
    {
        if (ffOptionsMap ==null)
            readFaceMatchOptions(paramFile);
        return (ffOptionsMap);
    }
    
    /*----------------------------------------------------------------------------------------------*/
    /** Get the FaceMatch parameters already read from a file.
     * @return HashMap of parameter name vs. value
     */
    
      public static HashMap getFFOptions()
    {
        return (ffOptionsMap);
    }

      /** 
       * Read the face match parameters from the file and save locally
       * @param ffOptionsFile File in JSON format 
       */
    protected  static void readFaceMatchOptions(String ffOptionsFile)
    {
        try
        {
            FileReader fileReader = new FileReader(ffOptionsFile);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(fileReader);
             ffOptionsMap = (HashMap) obj;            // unorder list of key:value pairs
        }
        catch (IOException ioe) 
        {    
            log.error("Error in reading file " + ffOptionsFile +", " +ioe.getMessage()); 
        }
        catch (ParseException pe) 
        {    
             log.error("Invalid JSON format. Error in parsing " + ffOptionsFile + " contents to JSONArray"); 
             pe.printStackTrace();
        }
    }
   
    
   /*----------------------------------------------------------------------------------------------*/ 
    public static void main(String[] args)
    {
        org.apache.log4j.BasicConfigurator.configure();
        
        // Edit with the actual parameter filename used for this installation
       HashMap ffOptions = FaceMatchOptionsReader.readFFOptions(
           "<TopDir>/FM2Server/installDir/fmoptions/FFParameters1.json");
       printOptions(ffOptions);
       System.out.println(ffOptions.toString());
    }
    
    public static void printOptions(HashMap ffOptions)
    {
          // first create a  FaceRegionDetector with FaceMatch specific default parameters
         //HashMap ffOptions = FaceMatchOptionsReader.getFFOptions();
         
         // get the required parametrs and flags
         
         String XMLModelPath = (String) ffOptions.get("XMLModelPath" );
         String FaceModelFN  = (String) ffOptions.get("FaceModelFN" );
         String ProfileModelFN = (String) ffOptions.get("ProfileModelFN");
         
         // get other parameters
         String SkinColorMapperKind =  (String) ffOptions.get( "SkinColorMapperKind" );
         String SkinColorParmFN =  (String) ffOptions.get( "SkinColorParmFN" );
         
         int  FaceDiameterMin = ((Long)ffOptions.get("FaceDiameterMin" )).intValue();
         int  FaceDiameterMax = ((Long)ffOptions.get("FaceDiameterMax") ).intValue();
         int FaceLandmarkDim = ((Long)ffOptions.get("FaceLandmarkDim") ).intValue();

        float  SkinMassT =  ((Double)ffOptions.get( "SkinMassT" )).floatValue();
        float  SkinLikelihoodT = ((Double)ffOptions.get("SkinLikelihoodT" )).floatValue();
        float  FaceAspectLimit  = ((Double)ffOptions.get("FaceAspectLimit" )).floatValue();
        
        // Get the default flags
      JSONObject  flagBits = (JSONObject)ffOptions.get("FF_Flag_bits" );
      
      String  ffFlags = (String)ffOptions.get("FF_defaultFlags");
      JSONObject  fmFlagBits = (JSONObject)ffOptions.get("FM_Flag_bits" );

    }
}
