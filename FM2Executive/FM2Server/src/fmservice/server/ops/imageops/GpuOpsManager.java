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
package fmservice.server.ops.imageops;

//import fmservice.server.util.OpenCLGPUTest;

import fmservice.httputils.common.ServiceConstants;


import java.util.Properties;
import org.apache.log4j.Logger;

/**
*  The singleton GPUManager object determines and stores the status of GPU hardware
 * 
 *
 */
public class GpuOpsManager  implements ServiceConstants
{
    private static Logger log = Logger.getLogger(GpuOpsManager.class);

    // instance variables
    protected int status = 0;
    
    /**  Global data shared by all instances (from all HTTP requests) **/
    static Properties opsProperties;
    static boolean gotGPU = false;                      // if the system is connected to a GPU hardware
    static boolean useGPU = true;                       // externally set/reset
    
    static  GpuOpsManager gpuManager = null;     // singleton object
    
    
   /*----------------------------------------------------------------------------*/
   public  static GpuOpsManager createGpuManager(Properties fmConfig)
   {
       if (gpuManager == null)
           gpuManager = new GpuOpsManager(fmConfig);
       return gpuManager;
   }
   
   /*-----------------------------------------------------------------------------------*/
    protected GpuOpsManager(Properties fmConfig)
    {
        if (gpuManager != null)
        {
            log.error("GPU manager already initialized.");
            return;
        }
        opsProperties = fmConfig;
        status =checkGpuStatus();
        gotGPU = (status == 1);
        gpuManager = this;
    }
    
    /*----------------------------------------------------------------------------*/
   public   static GpuOpsManager getGpuManager()
   {
       return gpuManager;
   }
   /*----------------------------------------------------------------------------*/
   public int  checkGpuStatus()
    {
        /*------------ Disable this for checking GPU status due to Shared library load problem ... dm -----------------*/            
        int status = 0;
        log.info("Proceeding without front-end GPU status check with OpenCL due to shared lib load problem");
        
        /*
        // Check if a GPU unit is operationally available or not . Currently, we perform it 
        // directly, using OpenCL, which bypasses standard OpenCV/CUDA
        OpenCLGPUTest  gpuTestPart1 = new OpenCLGPUTest();
        status = gpuTestPart1.execute();
        String message = gpuTestPart1.getOpsMessage();
        if (status == 1)
            log.info("GPU operations check: " + message);
        else
            log.error("GPU operations check: " + message);
        */       
        return  status;
    }  
        
     /** 
     * Turn the GPU usage to on or off, even if available  ( for testing only)
     * Should be synchronized;
     */
    synchronized public void setGpuUse(boolean gpuOn)
    {
        //useGPU = gpuOn;s
        useGPU = false;         // force only CPU usage
    }
    
 
    
    public  boolean hasGPU()
    {
        return gotGPU;
    }
    
    //-------------------------------------------------------------------------------------------------------------
    // isGpuEnabled is true when both the system has a GPU and usre is authorized to use it.
    // May be turned off  for testing etc.
    //-------------------------------------------------------------------------------------------------------------
    public  boolean  isGpuEnabled()
    {
        return gotGPU&&useGPU;
    }
    
   /*------------------------------------------------------*/
}
