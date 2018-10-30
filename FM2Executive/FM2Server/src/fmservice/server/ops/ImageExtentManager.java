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
package fmservice.server.ops;

import fmservice.httputils.common.ServiceConstants;
import fmservice.server.cache.dbcontent.FMClient;

import fmservice.server.global.FMContext;
import fmservice.server.global.Scope;
import fmservice.server.cache.ImageCacheManager;
import fmservice.server.result.FMServiceResult;

import fmservice.server.cache.dbcontent.ImageExtent;

import org.apache.log4j.Logger;
import java.util.HashMap;
import java.util.Properties;


/*****************************************************************************************/
/**
 * This class in called  on the server side to perform add/delete /... operations for an
 * image Extent of aFaceMatch client.
 * An extent must be created first explicitly, before any operation performed on it.
*
 *
 */
public class ImageExtentManager implements ServiceConstants
{
    private static Logger log = Logger.getLogger(ImageExtentManager.class.getName());
    static String validPerformanceOptions = SPEED+"||"+ACCURACY+"||"+OPTIMAL+"||"+PROGRESSIVE;
    
    protected FMDataManager  fmDataManager;
    protected ImageCacheManager  indexCacheManager;
    protected Properties fmConfig;
    protected FMContext fmContext;
    
    protected Scope scope;
    
    boolean okToQuery = false;
    
    int  requestID;                                 // facematching request ID (counter) since start up

    public ImageExtentManager(FMDataManager dataManager)
    {
       fmDataManager = dataManager;
       indexCacheManager = fmDataManager.getIndexCacheManager();
       scope = Scope.getInstance();
       fmContext = scope.getFMContext();
       fmConfig = scope.getFMConfig(); 
    }  
  
   /*----------------------------------------------------------------------------------------------------------
    * Perform the requested Extent  related operation. Also synchronize with the 
    * MetadataIndexTree to create/remove branches etc.
    *--------------------------------------------------------------------------------------------------------------*/
    public FMServiceResult  handleRequest(int reqId, int operation, 
        HashMap <String, Object>inputParams) throws Exception
    {
        System.out.println("ExtentOpsRequestHandler: Received  request: operation=" + operation);
         requestID = reqId;
         FMServiceResult  serviceResult = new FMServiceResult(IMAGE_EXTENT_SVC, operation);
         
         String clientKey = (String)inputParams.get(CLIENT_KEY);
         FMClient client = Scope.getInstance().getClientWithKey(clientKey);
         
         String extentName = (String)inputParams.get(EXTENT_NAME_PARAM);
         
         int extentId  = scope.getExtentId(clientKey, extentName);
         ImageExtent extent = null;

         if (operation == ADD_EXTENT_OP)
         {
             if (extentId >= 1 )      // already exists
            {
                 serviceResult.setStatus(INVALID_PARAM, "Extent with name " + extentName+ " already exists for client with key " + clientKey);
                 return serviceResult;
            }
         }
         // for all other requests
         else 
         {
             if (extentId <= 0)
            {
                 serviceResult.setStatus(INVALID_PARAM, "Extent  " + extentName+ " does not exist for client with key " + clientKey);
                 return serviceResult;
            }
             else
             {
               String clientName = scope.getClientName(clientKey);
               extent = scope.getImageExtent(clientName, extentName);
             }
         }
         // perform the service
         //
         switch (operation)
         {
             case(ADD_EXTENT_OP):
              {
                  String description = (String)inputParams.get(DESCRIPTION_PARAM);
                  extent = client.createImageExtent();
                  extent.setName(extentName);
                  extent.setDescription(description);
                  extent.setActive(true);
                  
                  // check for performance preference, if not specified set to accuracy 
                  String perfOption= (String)inputParams.get(PERFORMANCE_PREF);
                  if (perfOption == null)
                  {
                        perfOption  = fmConfig.getProperty("facefind.pref.default");
                        if (perfOption == null)
                                perfOption = OPTIMAL;
                  }
                  if (perfOption == null || !perfOption.matches(validPerformanceOptions))
                        perfOption = OPTIMAL;

                  extent.setFacefindPerformanceOption(perfOption);
                  extent.update();
                  fmContext.getDBContext().complete();                // commit to DB and free connection
                  indexCacheManager.addNewExtent(extent);
                  
                  // create Inverted metadata IndexTree
                  serviceResult.setStatus(SUCCESS, "Extent  " + extentName+ " added to client with key " + clientKey);
                  break;
              }
             case (ACTIVATE_EXTENT_OP):
              {
                  // set the peformance preference if specified, otherwise don't change the original setting
                  String perfOption= (String)inputParams.get(PERFORMANCE_PREF);
                  if (perfOption != null)
                    extent.setFacefindPerformanceOption(perfOption);

                  extent.setActive(true);
                  extent.update();
                  indexCacheManager.setExtentStatus(extent, true);
                  serviceResult.setStatus(SUCCESS, "Extent  " + extentName+ " set to active");
                  break;
              }
              case (DEACTIVATE_EXTENT_OP):
              {
                  extent.setActive(false);
                  extent.update();
                  indexCacheManager.setExtentStatus(extent, false);
                  serviceResult.setStatus(SUCCESS, "Extent  " + extentName+ " set to inactive");
                  break;
              }
              case (REMOVE_EXTENT_OP):
              {
                   // removes underneath images from the database  too (does not delete actual index files
                  indexCacheManager.removeExtent(extent);
                  client.deleteImageExtent(extent);      
                   fmContext.getDBContext().complete();                // commit to DB and free connection
                  serviceResult.setStatus(SUCCESS, "Extent  " + extentName+ " removed from client's scope");
                  break;
              }
               case (SET_PERFORMANCE_OP):
              {
                   // Set the FaceDetection performance options of this ImageExtent: Default=optimal
                   String performanceOption = (String) inputParams.get(FF_OPTION);
                   if (performanceOption != null)           // 
                        extent.setFacefindPerformanceOption(performanceOption);
                  
                  fmContext.getDBContext().complete();                // commit to DB and free connection
                  serviceResult.setStatus(SUCCESS, "Face detection preference for Extent  " + extentName+ " set to " + 
                          performanceOption == null ? "default"  :  performanceOption);
                  break;
              }
           /*   case (GET_EXTENTINFO_OP):
              {
                   // Get information related to this extent. 
                  // Includes: current status (active/inactive), FaceFind performance,  number of images,
                  // distribution with gender and age
                   String performanceOption = (String) inputParams.get(FF_OPTION);
                   if (performanceOption != null)           // 
                        extent.setFacefindPerformanceOption(performanceOption);
                  
                  fmContext.getDBContext().complete();                // commit to DB and free connection
                  serviceResult.setStatus(SUCCESS, "Face detection preference for Extent  " + extentName+ " set to " + 
                          performanceOption == null ? "default"  :  performanceOption);
                  break;
              }*/
         } 
         serviceResult.requestId = reqId;
         log.info("Service Status: " + serviceResult.getStatus().statusCode + ", " + serviceResult.getStatus().statusMessage);
         return serviceResult;
     }
   
}

        
        
    