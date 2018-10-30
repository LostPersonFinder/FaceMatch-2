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
package fmservice.server.global;

import fmservice.httputils.common.ServiceConstants;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Class FMContext. This is the top level singleton class which maintains the context 
 * of the FaceMatch system. It is a data-only class (structure) which contains:
 *   1- Database context (DBContext object) for retrieving data from the database
 *   2 - FaceMatch system configuration
 *   3 - GPU Context: Status : The status/config of the GPU for subsequent face match operations 
 *   4 - Number of processor cores
 *   5 - Other miscellaneous flags/properties
 * 
 *  It supports a set of get and set methods for its components
 * 
 *
 */


public class FMContext
{
     private static final Logger log = Logger.getLogger(FMContext.class);

    protected DBContext dbContext;
    
    protected boolean hasGpu;
    protected boolean  gpuAvailable;
    protected int  perfMon;         // performance monitoring bits
    
    protected int numCores;
    protected Properties  fmConfig;
    
    protected static FMContext fmContext = null;        // singleton object
    
    protected static  int  facefindPerf = 0x1;
    protected static int ingestPerf = 0x2;
    protected static int queryPerf = 0x4;
    
    
    /**
     * Get the FMContext object
     * 
     * @return the singleton object
     */
    
    public static FMContext getFMContext()
    {
        return fmContext;
    }
   
  /*---------------------------------------------------------------------------------------------*/  
    /**
     * Constructor.
     * @param dbContext   database context (with Database Mannager and object cache)
     * @param config  configuration related information for the application
     * @param hasGpu whether or not system has a GPU, physically. in its hardware configuration
     * @param gpuAvailable  the GPU is available for face match operations (not disabled)
     /*---------------------------------------------------------------------------------------------*/
    public FMContext (DBContext  dbCtx, Properties config,
                boolean hasGPU, boolean gpuAvailable)
    {
        // Create only the first time - ignore subsequent calls
        if (fmContext == null)
        {
            dbContext = dbCtx;
            fmConfig = config;
            this.hasGpu = hasGPU;
            this.gpuAvailable = gpuAvailable;                          // should we use GPU for FM operations
            
            perfMon = 0;        // default: no monitoring

            // Other frequently accessed info
            int numProcessors = Runtime.getRuntime().availableProcessors();
            numCores = numProcessors;  //  not true ???
            fmContext = this;
            
            // build scope after fmContext is defined
            Scope.setDomainScope(fmContext);
        }
    }
    
    /*-----------------------------------------------------------------------------------------------*/
   
   public DBContext getDBContext()
   {
       return dbContext;
   }

   public Scope getDomainScope()
   {
       return Scope.getInstance();
   }
 
   
   // Record if the GPU is available to FM tasks or not
   public void setGpuStatus(boolean available)
   {
       gpuAvailable = available;
   }
   
   
   // Is GPU ready to use => available and allowed to use
    public boolean isGpuAvailable()
   {
       return gpuAvailable;
   }
   

    // Record in the system has a connected  GPU hardware
   public boolean  hasGPU()            
   {
       return (this.hasGpu);
   }
   
   //--------------------------------------------------------------------------------
   // Should facematch operation performance be recorded
   //
    public void recordFMPerformance(boolean on)
    {
        recordFMPerformance(on, null);
    }
   
   public void recordFMPerformance(boolean on, String type)
   {
       if (type == null)
           perfMon = on ? (facefindPerf |  ingestPerf |  queryPerf) : 0;
       
       else if (type.equals(ServiceConstants.GET_FACES))
            perfMon = on ?  (perfMon | facefindPerf) : (perfMon ^ facefindPerf);
       
         else if (type.equals(ServiceConstants.INGEST))
            perfMon = on ? (perfMon |  ingestPerf) : (perfMon ^ ingestPerf);
        
       else if (type.equals(ServiceConstants.QUERY))
            perfMon = on ? (perfMon | queryPerf) : (perfMon ^ queryPerf);
   }
   
   public boolean isRecordingPeformance(String type)
   {
       if (type == null)
           return  (perfMon != 0);
       
       else if (type.equals(ServiceConstants.GET_FACES))
            return (perfMon & facefindPerf) != 0;
       
       else if (type.equals(ServiceConstants.INGEST))
               return (perfMon & ingestPerf) != 0;
        
       else if (type.equals(ServiceConstants.QUERY))
               return (perfMon & queryPerf) != 0;
       else 
           return false;
   }
   //--------------------------------------------------------------------------------
   
   public Properties getFMConfiguration()
   {
       return fmConfig;
   }
}