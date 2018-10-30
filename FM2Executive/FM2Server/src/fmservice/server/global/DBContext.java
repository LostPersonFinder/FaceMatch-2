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

import fmservice.server.storage.rdbms.DatabaseManager;
import fmservice.server.cache.dbcontent.DBContentObject;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.Properties;

import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * Class representing the database context for FaceMatch  operation. 
 * This is a singleton object which provides access  to the FaceMatch2  database, via
 *  the static DatabaseManager class, when  needed by the application, and hold other 
 * relevant database information
* <p>
 * It also has an in-memory Cache of Database's Image related objects,
 * identified as "DBContentObject", which are loaded from the DB when they are
 * referred for the first time by the FM2 Server and deleted when the object (row) is deleted 
 * from the database. If a new such  object is created and added to the database, it is 
 * also added to the cache.
 * <p>
 * NOTE: FaceMatch2 database connections are short-lived rather than being permanent.
 * A new connection is created by DBContext  when database access is needed and removed 
 * after the transaction is completed to avoid MySQL timeouts and also for security reason.
 * <P>
 *
 * @version $Revision: 1.0 $
 * 
 * Change Log: 
 */

 
public class DBContext
{
    private static final Logger log = Logger.getLogger(DBContext.class);

    /** Database connection */
    private Connection connection;

    /** Current user - null means anonymous access */
    private String  currentUser;            // user ID
    
     // key: ObjectHandle  - tableName_dbID as String
    protected  HashMap <String, DBContentObject > dbObjectCache;

    public static int numConnections = 0;

    /**
     * Construct a new context object. 
     * A database connection is opened. No user is authenticated.
     * 
     * @param fmConfig The Properties objecting containing Database name, user, password etc.
     * 
     * @exception SQLException  if there was an error obtaining a database connection
     */
    public DBContext(Properties fmConfig ) throws SQLException
    {
        // Obtain a non-auto-committing connection
        // This initializes the static DatabaseManager object (which is "not" a singleton object,
        // but implemented as a set of static database operation functions for the associated DBContext
        // provided as a parameter 
        connection = DatabaseManager.getConnection();
        connection.setAutoCommit(false);

        // Initialize the Cache to hold DB rows as they are read in
        dbObjectCache = new  HashMap <String,DBContentObject > ();
        currentUser = null;
    }

    /**
     * Get the database connection associated with the context.
     * If no connection exists, create a new one.
     * 
     * @return the database connection
     */
    public Connection getDBConnection()
    {
        try
        {
            if (connection == null)
            {
                connection = DatabaseManager.getConnection();
                connection.setAutoCommit(false);
                numConnections++;
            }
             if (numConnections > 1)
                   log.trace("Number of open database connections: " + numConnections);
        }
        catch (SQLException e)
        {
            log.error("Error connecting to database", e);
        }
        return connection;
    }

    /**
     * Set the current user. Authentication must have been performed by the
     * caller - this call does not attempt any authentication.
     * 
     * @param user
     *            the new current user, or <code>null</code> if no user is
     *            authenticated
     */
    public void setCurrentUser(String  user)
    {
        currentUser = user;
    }

    /**
     * Get the current (authenticated) user
     * 
     * @return the current user, or <code>null</code> if no user is
     *         authenticated
     */
    public String  getCurrentUser()
    {
        return currentUser;
    }


    /**
     * Complete all database related transactions and close the connection.
     * Note: Any transaction with the database is committed.
     * @exception SQLException
     *                if there was an error completing the database transaction
     *                or closing the connection
     **
     */
    public void complete() throws SQLException
    {
        // FIXME: Might be good not to do a commit() if nothing has actually
        // been written using this connection
        try
        {
            // Commit any changes made as part of the transaction
            if (connection == null)
                return;
            connection.commit();
        }
        finally
        {
            // Free the connection
            DatabaseManager.freeConnection(connection);
            connection = null;
            numConnections--;
        }
    }

    /**
     * Commit any transaction that is currently in progress, but do not close
     * the context.
     * 
     * @exception SQLException
     *                if there was an error completing the database transaction
     *                or closing the connection
     */
    public void commit() throws SQLException
    {
        // Commit any changes made as part of the transaction
        if (connection != null)
            connection.commit();
    }

    /**
     * Close the connection, without committing any of the changes performed using
     * this context. The database connection is freed. No exception is thrown if
     * there is an error freeing the database connection, since this method may
     * be called as part of an error-handling routine where an SQLException has
     * already been thrown.
     */
    public void abort()
    {
        try
        {
            connection.rollback();
        }
        catch (SQLException se)
        {
            log.error(se.getMessage(), se);
        }
        finally
        {
            DatabaseManager.freeConnection(connection);
            connection = null;
            numConnections--;
        }
    }

    /**
     * Find out if this context is valid. 
     * 
     * @return  true  if the context's connection is still live
     *                  false if is is already completed or aborted
     *         <code>false</code>
     */
    public boolean isValidConnection()
    {
        // Only return true if our DB connection is live
        return (connection != null);
    }

    protected void finalize()
    {
        /*
         * If a context is garbage-collected, we roll back and free up the
         * database connection if there is one.
         */
        if (connection != null)
        {
            abort();
        }
    }
    
    /*-----------------------------------------------------------------------------------------------------------*/
    /**
     * Get the database Object Cache 
     * 
     * @return Map of DB Object key(ID) vs. Object map
     */
   public HashMap <String, DBContentObject > getObjectCache()
   {
       return dbObjectCache;
   }
   
  /*---------------------------------------------------------------------------------------------------------------*/
 /** Add an Object to the cache.
  * The key for the object is constructed from its type and database Id
  * 
  * @param contentObject - Database content object to add
  * @return handle of the object in cache (as: tableName_id);
  */
   public String addToCache(DBContentObject contentObject)
   {
       String handle  = contentObject.getObjectType()+ "_"+contentObject.getID();
       dbObjectCache.put(handle, contentObject);
       return handle;
   }  
  /*---------------------------------------------------------------------------------------------------------------*/
 /** Get an Object from the cache.
  * 
  * @param Class name of object (type)
  * @param ID of object 
  *
  * @return the cached object (or null)
  */
    public DBContentObject  getFromCache(String objectType, int id)
   {
       String handle = objectType + "_"+ id;
       return dbObjectCache.get(handle);
   }  
    /*---------------------------------------------------------------------------------------------------------------*/
     /** Remove an Object from the cache.
  * 
  * @param Class name of object (type)
  * @param ID of object 
  *
  *   * @return the cached object just removed (or null)
  */
    public DBContentObject  removeFromCache(String objectType, int id)
   {
       String handle = objectType + "_"+ id;
       return dbObjectCache.remove(handle);
   } 

}