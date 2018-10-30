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
 * Java class to invoke FaceMatch C++ library for FRD.
 * Note: Native classes do not have body (implementation)
 *
 */
public class FRDProxy
{
    private static Logger log = Logger.getLogger(FRDProxy.class);
    protected long nativeObj;
    
    /** 
     * instantiate from scratch with all parameters.
     * 
     * */
    public  FRDProxy (String XMLModelPath, String FaceModelFN, String ProfileModelFN, 
            String SkinColorMapperKind, String SkinColorParmFN, int FaceDiameterMin, int FaceDiameterMax,
            double SkinMassT, double SkinLikelihoodT, 
            double FaceAspectLimit, boolean useGPU, 
            boolean withLock)
   {
       // invoke the native method to create an FRD and store it locally
       if(log.isDebugEnabled())
            n_setVerboseLevel(2);
       else
            n_setVerboseLevel(1);
       
      if (!withLock)
       {
             nativeObj = n_createSimpleFRD( XMLModelPath,  FaceModelFN,  ProfileModelFN, 
             SkinColorMapperKind,  SkinColorParmFN,  FaceDiameterMin,  FaceDiameterMax,
             SkinMassT,  SkinLikelihoodT,  FaceAspectLimit,  useGPU);
       }
       else
       {
           nativeObj = n_createFRDWithLock( XMLModelPath,  FaceModelFN,  ProfileModelFN, 
             SkinColorMapperKind,  SkinColorParmFN,  FaceDiameterMin,  FaceDiameterMax,
             SkinMassT,  SkinLikelihoodT,  FaceAspectLimit,  useGPU);
       }
      if (nativeObj == 0)
         log.error("Error in creating FRD, message: " + n_getErrorMessage(nativeObj));
     else 
       log.info("FRD handle " + (withLock ? "With Lock" : "with no Lock and " )  + 
               (useGPU ? "using GPU" : "using CPU") + " returned from JNI: " + nativeObj);
   } 
    
   /** Instantiate from a stored handle.
   */
   public  FRDProxy(long  handle)
    {
        if (handle == 0)
            throw new java.lang.UnsupportedOperationException("Native object address is NULL");
        nativeObj = handle;
    }
   
    public  long getFRDHandle()
    {
       return nativeObj;
    }

    public boolean  usingGPU()
    {
        return n_usingGPU(nativeObj);
    }
    
    public boolean  deallocate()
    {
        return n_deallocate(nativeObj);
    }
    /**------------------------------------------------------------------------------
    // Set the verbose level for JNI module for debug info etc.
    //@param vlevel : verbose level, 0 => minimal
    //------------------------------------------------------------------------------*/
    static public void setVerboseLevel(int vlevel)
    {
        n_setVerboseLevel(vlevel);
    }   
/*---------------------------------------------------------------------------------------------------------------------------------------------*/
    // Native calls
  /*---------------------------------------------------------------------------------------------------------------------------------------------*/  
    static native  long n_createSimpleFRD(String XMLModelPath, String FaceModelFN, String ProfileModelFN, 
            String SkinColorMapperKind, String SkinColorParmFN, int FaceDiameterMin, int FaceDiameterMax,
            double SkinMassT, double SkinLikelihoodT, double FaceAspectLimit, boolean useGPU);
   
  
    static native  long n_createFRDWithLock(String XMLModelPath, String FaceModelFN, String ProfileModelFN, 
        String SkinColorMapperKind, String SkinColorParmFN, int FaceDiameterMin, int FaceDiameterMax,
        double SkinMassT, double SkinLikelihoodT, double FaceAspectLimit, boolean useGPU);
    
      // Find the FRD, represented by the argument nativeObj is using GPU
   static native  boolean n_usingGPU(long nativeObj);
    
    // Deallocate the corresponding c++ object
    static native  boolean  n_deallocate(long nativeObj);
    
    /*--------------------------------------------------------------------------------------------------------------------------*/  
   // retrieve the error message saved by the JNI class
    static   native String n_getErrorMessage(long nativeObj);
  /*--------------------------------------------------------------------------------------------------------------------------*/
      // set the verbose level for the JNI class for testing/debugging
    static   native  void n_setVerboseLevel (int vlevel);
    /*--------------------------------------------------------------------------------------------------------------------------*/
     
}
