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
package fmservice.server.cache.dbcontent;

import fmservice.server.global.DBContext;

import fmservice.server.storage.rdbms.DatabaseManager;
import fmservice.server.storage.rdbms.TableRow;
import fmservice.server.storage.rdbms.TableRowIterator;

import java.util.ArrayList;

import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 *
 * Database access related utilities.
 * 
 *
 */
public class DBUtils
{
    private static Logger log = Logger.getLogger(DBUtils.class);
    
     protected static String QUERY_CLIENT_INFO = "Select * from fmclient";
     protected static String QUERY_ADMIN_INFO  = "Select * from fmadmin";
  
     /*------------------------------------------------------------------------------------------------------*/
     /** get information for all clients.
      * 
      * @param dbContext
      * @return  list of clients from database. If a client is not in the cache
      *  it is also added to the cache - by the find() call
      *-----------------------------------------------------------------------------------------------------*/
      public  static ArrayList<FMClient>  getAllClientInfo(DBContext dbContext)
      {
          ArrayList<FMClient> clientList = new ArrayList();
        try
        {
            TableRowIterator tri = DatabaseManager.queryTable(dbContext, "fmclient", QUERY_CLIENT_INFO);
             while (tri.hasNext())
            {
                TableRow row = tri.next();
                FMClient client =   FMClient.find(dbContext, row.getIntColumn("client_id")); 
                clientList.add(client);
            }
            tri.close();
            log.info("Total number of clients recorded in the database: " + clientList.size());
            return clientList;
        }
        catch(SQLException se)
        {
            log.error("Error loading 'fmclient' information from database", se);
            return null;
        }
      }
/*---------------------------------------------------------------------------------------------------------*/      
      /** get Information about a single client based upon its unique name
       * 
       * @param dbContext
       * @param clientName
       * @return 
       */
      public  static FMClient   getClientInfo(DBContext dbContext, String clientName)
      {
         ArrayList<FMClient> clientList = getAllClientInfo(dbContext);
         if (clientList == null || clientList.isEmpty())
         {
             log.error ("No clients are recorded in the FM database.");
             return null;
         }
         for (FMClient client : clientList)
         {
             if (client.getName().equalsIgnoreCase(clientName))
                 return client;
         }
        log.warn ("FMClient with name " + clientName + " does not exist in the database.");
        return null;
    }
   /*------------------------------------------------------------------------------------------------*/
    /** get names of all ImageExtents within this client
     * 
     * @param clientId database id
     * @return 
     */
    public static ArrayList<ImageExtent> getImageExtentsForClient(DBContext dbContext, int clientId)
    {
         try
         {
             FMClient client = FMClient.find(dbContext, clientId);
            if (client == null)
            {
                if (clientId  <= 0)
                    log.error("Invalid clientID " + clientId + " provided to find FMClient object");
                else
                    log.error("No client with ID " + clientId + " exists in the database.");
                return null;
            }
            return client.getImageExtents();
         }
       catch (SQLException sqle)
       {
            log.error("Error retrieving ImageExtent data  for Client:  " + clientId + " from database", sqle);
            return null;
       }
    }
      
      /*-------------------------------------------------------------------------------------------------------*/
     /**
     * Get the ID of the client for a given zone
     * @param context
     * @param zoneId
     * @return 
     */
    public static int getClientIDForZoneDescriptor(DBContext context, int descrId)
     {
         try
         {
            ZoneDescriptor  descriptor =   ZoneDescriptor.find(context, descrId);
             int imageId = descriptor.getImageID();
             return getClientIDForImage(context, imageId);
         }
         catch(SQLException sqle)
         {
             log.error("No client ID found for imageZone : " + descrId);
             return -1;
         }
     }
    /**
     * Get the ID of the client for a given zone
     * @param context
     * @param zoneId
     * @return 
     */
    public static int getClientIDForZone(DBContext context, int zoneId)
     {
         try
         {
             ImageZone zone = ImageZone.find(context, zoneId);
             int imageId = zone.getImageID();
              return getClientIDForImage(context, imageId);
         }
         catch(SQLException sqle)
         {
             log.error("No client ID found for imageZone : " + zoneId);
             return -1;
         }
     }
    /*-------------------------------------------------------------------------------------------------*/
    /**
     * Get the ID of the client for a given image
     * @param context
     * @param imageId
     * @return 
     */
     public static  int getClientIDForImage(DBContext context, int imageId)
     {
         try
         {
             FMImage image = FMImage.find(context, imageId);
             int extentId = image.getExtentID();
             return  (ImageExtent.find(context, extentId)).getClientID();
         }
         catch(SQLException sqle)
         {
             log.error("Exception in getting clitnt ID for image id: " +  imageId, sqle);
             return -1;
         }
     }        
    /*-----------------------------------------------------------------------------------------*/   
        /**
     * Get the ID of the client for a given extent
     * @param context
     * @param imageId
     * @return 
     */
     public static  int getClientIDForExtent(DBContext context, int extentId)
     {
         try
         {
             ImageExtent extent = ImageExtent.find(context, extentId);
             if (extentId < 1 || extent == null)
             {
                 log.error("Invalid image extent provided");
                 return -1;
             }
             return extent.getClientID();
         }
         catch(SQLException sqle)
         {
             log.error("Exception in getting client  ID for ImageExtent id: " +  extentId, sqle);
             return -1;
         }
     }        
     
       /*-----------------------------------------------------------------------------------------*/   
        /**
     * Get the ID of the client for a given extent
     * @param context
     * @param imageId
     * @return 
     */
     public static  ImageExtent getExtentWithID(DBContext context, int extentId)
     {
         try
         {
             ImageExtent extent = ImageExtent.find(context, extentId);
             return extent;
         }
         catch(SQLException sqle)
         {
             log.error("Exception in getting ImageExtent  with id: " +  extentId, sqle);
             return null;
         }
     }        
    /*-----------------------------------------------------------------------------------------*/   
     /**
     * Check if the given credentials are valid for an FM Administrator
     * @param name
     * @param password
     * @return 
     * 
     * Note: These database rows, which are rarely used are not copied to the Object cache
     */
    public static boolean  isAdmin(DBContext dbContext, String name, String password)
    {
         try
        {
            boolean valid = false;
            TableRowIterator tri = DatabaseManager.queryTable(dbContext,
                "fmadmin", QUERY_ADMIN_INFO);
             while (tri.hasNext())
            {
                TableRow row = tri.next();
                if (row.getStringColumn("admin_name").equals(name) &&
                    row.getStringColumn("password").equals(password))
                {
                    valid = true;
                    break;
                }
            }
            tri.close();
            return valid;
        }
        catch(SQLException se)
        {
            log.error("Error loading 'fmadmin information from database", se);
        }
        return false ;  
    }   
    
}
