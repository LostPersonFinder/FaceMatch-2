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
package fmservice.server.storage.rdbms;

import  fmservice.httputils.common.ServiceConstants;

import fmservice.server.global.DBContext;
import fmservice.server.result.FMServiceResult;
import fmservice.server.result.*;

import java.util.Iterator;

import java.sql.Date;
import java.sql.SQLException;
import fmservice.server.global.Scope;

import org.apache.log4j.Logger;

/**
 *
 *
 */
public class PerformanceRecorder 
{
     private static Logger log = Logger.getLogger(PerformanceRecorder.class);
     
     static String[] ffPerfOptions = {
         ServiceConstants.SPEED,  ServiceConstants.OPTIMAL, 
         ServiceConstants.ACCURACY, ServiceConstants.PROGRESSIVE};
     
    
     
     /*------------------------------------------------------------------------------------------*/
     static int performancePref2int(String option)
     {
         if (option == null || option == ServiceConstants.NOT_USED)
             return -1;
         for (int i = 0; i < ffPerfOptions.length; i++)
         {
             if (option.equalsIgnoreCase(ffPerfOptions[i]))
                 return (i+1);              // one-based
         }
         return -1;
     }
      
    /*--------------------------------------------------------------------------------------------------------*/
       // Record the time used to complete the operation by the FaceMatch2 server, including
       // the time taken by the FaceMatch library
       /*--------------------------------------------------------------------------------------------------------*/
       public static void recordServerPerformance(DBContext dbContext, 
              FMServiceResult serviceResult)
       {
            if (serviceResult.getStatusCode() != 1)
                return;                                 // operation not successful, so don'r record in DB
            
            try
           {
                TableRow tr;
                if (serviceResult.getOperationType() == ServiceConstants.GET_FACES_OP)
                {
                    // Record faceFinding performance in the database
                    FaceFindResult ffResult = (FaceFindResult)serviceResult;
                    
                    tr = DatabaseManager.create(dbContext, "facefindperformance");
                    tr.setColumn("client_id", ffResult.client_id);
                    tr.setColumn("image_url", ffResult.imageURL);
                    tr.setColumn("num_faces", ffResult.numFaces);
                    tr.setColumn("service_date", new java.sql.Date(System.currentTimeMillis()));
                    tr.setColumn("facefind_time",  ffResult.faceFindTime);
                    tr.setColumn("facefind_option", performancePref2int(ffResult.perfOptionUsed));
                    tr.setColumn("gpu_used", ffResult.gpuUsed);
                    tr.setColumn("skin_cm_kind", ffResult.skinColorMapperKind);
                    DatabaseManager.update(dbContext, tr);
                }
                else if (serviceResult.getOperationType() == ServiceConstants.REGION_INGEST_OP)  // ingest a photo or image for a person
                {
                     // Record ingest operation performance in the database
                    ImageIngestResult ingestResult = (ImageIngestResult)serviceResult;
                    
                    tr = DatabaseManager.create(dbContext, "regioningestperformance");
                    tr.setColumn("extent_id", ingestResult.extentId);
                    tr.setColumn("image_id", ingestResult.imageId);
                    tr.setColumn("service_date", new java.sql.Date(System.currentTimeMillis()));
                    tr.setColumn("index_type", ingestResult.indexType);
                    tr.setColumn("index_version", ingestResult.indexVersion);
                    tr.setColumn("num_faces", ingestResult.nregCount);
                    tr.setColumn("facefind_time", ingestResult.faceFindTime);       // time in millisec
                    tr.setColumn("ingest_time", ingestResult.totalIngestTime);
                    tr.setColumn("facefind_option", performancePref2int(ingestResult.faceFindOptionUsed));
                    tr.setColumn("gpu_used", ingestResult.gpuUsed);
                    DatabaseManager.update(dbContext, tr);
                }
                
                else if (serviceResult.getOperationType() == ServiceConstants.REGION_QUERY_OP)
                {
                      // Record query match performance in the database
                    ImageQueryResult queryResult = (ImageQueryResult)serviceResult;            
                    int numRegions = queryResult.getNumQueryRegions();
                    for (int i = 0; i < numRegions; i++)
                    {
                        tr = DatabaseManager.create(dbContext, "regionqueryperformance");
                        tr.setColumn("extent_id", queryResult.extentId);
                        tr.setColumn("service_date", new java.sql.Date(System.currentTimeMillis()));
                        tr.setColumn("queryimage_url", queryResult.imageURL);
                        tr.setColumn("index_type", queryResult.indexType);
                        tr.setColumn("indexmatch_type", Scope.getInstance().getIndexMatchType());
                        tr.setColumn("match_tolerance", queryResult.tolerance);
                        tr.setColumn("max_return_matches", queryResult.maxMatches);
                        tr.setColumn("facefind_option", performancePref2int(queryResult.faceFindOptionUsed));
                        tr.setColumn("gpu_used", queryResult.gpuUsed);
                        tr.setColumn("query_time", queryResult.totalQueryTime);
                        
                        // For results done only once for all regions, record only under the first one
                        if (i == 0)
                        {
                            tr.setColumn("facefind_time", queryResult.faceFindTime);       // time in millisec - record only once
                            tr.setColumn("num_index_loaded", queryResult.numIndexFilesLoaded);  // number of index files loaded for this search
                            tr.setColumn("index_upload_time", queryResult.indexUploadTime);        // time to load additional index data from files
                        }
                        else
                        {
                             tr.setColumn("facefind_time", 0.0);       // combined under the first region
                             tr.setColumn("num_index_loaded", 0);  
                             tr.setColumn("index_upload_time", 0.0);   
                        }
                        ImageQueryResult.RegionMatchResult regionResult = queryResult.getRegionMatchResults().get(i);
                        tr.setColumn("region_number", i);
                        tr.setColumn("query_region", regionResult.queryRegion); 
                        // Assume equal time taken for each region as an approximation
                        tr.setColumn("regionmatch_time", (queryResult.totalQueryTime-queryResult.faceFindTime)/numRegions);
                        tr.setColumn("num_matches", regionResult.numMatches); 
                        
                        Iterator <Float>  it =  regionResult.matchResult.keySet().iterator(); 
                        Double val = new Double(it.next().floatValue());
                        tr.setColumn("bestmatch_distance",  val);
                   
                        DatabaseManager.update(dbContext, tr);
                    }
                }
                 else if (serviceResult.getOperationType() == ServiceConstants.REGION_REMOVE_OP)
                {
                    // Recordimage remove or delete performance in the database
                    log.warn ("Performance recording for '\"removeRegion'\" not implemented");
                    return;
                }
                 else
                 {
                     log.warn ("Unknown FM2 Operation type: " + serviceResult.getOperationType());
                     return;
                 }
           }
            catch (SQLException e)
            {
                log.error("Database Exception in recording Facematch performance in FM2 database", e);
            }
       }
}
