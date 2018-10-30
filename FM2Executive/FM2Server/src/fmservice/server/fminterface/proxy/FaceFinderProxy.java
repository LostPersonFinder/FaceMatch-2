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
package fmservice.server.fminterface.proxy;

import org.apache.log4j.Logger;

/**
 *
 *
 */
public class FaceFinderProxy
{  
    private static Logger log = Logger.getLogger(FaceFinderProxy.class);
    
    long nativeFFObj;    // FaceFinder native (C++) object
       
      /** 
     * instantiate from scratch with all parameters.
     * */
     public FaceFinderProxy(FRDProxy frdObj, String imageDir, String imageFileName,  String faceRegions, 
         int  ffFlags)
     {
        if(log.isDebugEnabled())
             n_setVerboseLevel(2);
        else
             n_setVerboseLevel(1);

       // replace all trailing and leading white spaces
         imageDir = imageDir.trim();
         imageFileName = imageFileName.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
         nativeFFObj = n_FaceFinderCreate(frdObj.getFRDHandle(), imageDir, imageFileName, faceRegions, ffFlags);

         return;
     }
     /*-------------------------------------------------------------------------------------------------*/
       public FaceFinderProxy(FRDProxy frdObj, String imageDir, String imageFileName, 
         int  ffFlags)
     { 
         // replace all trailing and leading white spaces
         imageDir = imageDir.trim();
         imageFileName = imageFileName.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
         nativeFFObj = n_FaceFinderCreate(frdObj.getFRDHandle(), imageDir,  imageFileName, null, ffFlags);
         if (nativeFFObj == 0)
         {
             log.error("Could not create FaceFinder object in FaceMatch Libray");
         }
         return;
     }
       
     /**------------------------------------------------------------------------------
    // Set the verbose level for JNI module for debug info etc.
    //@param vlevel : verbose level, 0 => minimal
    //------------------------------------------------------------------------------*/
    static public void setVerboseLevel(int vlevel)
    {
        n_setVerboseLevel(vlevel);
    }   
    /**------------------------------------------------------------------------------*/
     public long getFaceFinderHandle()
     {
         return nativeFFObj;
     }
     /**------------------------------------------------------------------------------*/
     public boolean  gotFaces(boolean lax )
     {
         return n_gotFaces(nativeFFObj, lax);
     }
     
     /**------------------------------------------------------------------------------*/
     public  String getFaces() 
     {
         return n_getFaces(nativeFFObj);
     }
     
    /**--------------------------------------------------------------------------------------------------------------*/
    // Native call for constructor, returns the  C++ FaceFinder object pointer as a long 
   /*---------------------------------------------------------------------------------------------------------------- */
   static  native  long n_FaceFinderCreate(long frdHandle, String imageDir, String imageFileName,  String faceRegions, 
         int  ffFlags);
    
    // Do we have faces for the associated images?
   static  native boolean n_gotFaces(long nativeFFObj, boolean lax);
    
    // return the Faces associated with each image as a set of Strings (with landmark+coordinate)
   static   native String n_getFaces(long nativeFFObj);
   
   // retrieve the error message saved by the JNI class
    static   native String n_getErrorMessage(long nativeFFObj);
    
     // set the verbose level for the JNI class for testing/debugging
    static   native  void n_setVerboseLevel (int vlevel);
    /*----------------------------------------------------------------------------------------------------------------*/
}   
