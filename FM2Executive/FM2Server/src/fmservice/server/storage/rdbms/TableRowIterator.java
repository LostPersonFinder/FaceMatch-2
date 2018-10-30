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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the results of a database query
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class TableRowIterator
{
    /**
     * Results from a query
     */
    private ResultSet results;

    /**
     * Statement used to submit the query
     */
    private Statement statemt = null;

    /**
     * The name of the RDBMS table
     */
    private String table;

    /**
     * True if there is a next row
     */
    private boolean hasNext = true;

    /**
     * True if we have already advanced to the next row.
     */
    private boolean hasAdvanced = false;

    /**
     * Constructor
     * 
     * @param results -
     *            A JDBC ResultSet
     */
    TableRowIterator(ResultSet results)
    {
        this(results, null);
        statemt = null;
    }

    /**
     * Constructor
     * 
     * @param results -
     *            A JDBC ResultSet
     * @param table -
     *            The name of the table
     */
    TableRowIterator(ResultSet results, String table)
    {
        this.results = results;
        this.table = table;
        statemt = null;
    }

    /**
     * Finalize -- this method is called when this object is GC-ed.
     */
    public void finalize()
    {
        close();
    }

    /**
     * setStatement -- this method saves the statement used to do the query. We
     * must keep this so that the statement can be closed when we are finished.
     * 
     * @param st -
     *            The statement used to do the query that created this
     *            TableRowIterator
     */
    public void setStatement(Statement st)
    {
        statemt = st;
    }

    /**
     * Advance to the next row and return it. Returns null if there are no more
     * rows.
     * 
     * @return - The next row, or null if no more rows
     * @exception SQLException -
     *                If a database error occurs while fetching values
     */
    public TableRow next() throws SQLException
    {
        if (results == null)
        {
            return null;
        }

        if (!hasNext())
        {
            return null;
        }

        hasAdvanced = false;

        return DatabaseManager.process(results, table);
    }

    /**
     * Return true if there are more rows, false otherwise
     * 
     * @return - true if there are more rows, false otherwise
     * @exception SQLException -
     *                If a database error occurs while fetching values
     */
    public boolean hasNext() throws SQLException
    {
        if (results == null)
        {
            close();
            return false;
        }

        if (hasAdvanced)
        {
            return hasNext;
        }

        hasAdvanced = true;
        hasNext = results.next();

        // No more results
        if (!hasNext)
        {
            close();
        }

        return hasNext;
    }

    /**
     * Saves all the values returned by iterator into a list.
     * 
     * As a side effect the result set is closed and no more 
     * operations can be preformed on this object.
     * 
     * @return - A list of all the values returned by the iterator.
     * @exception SQLException -
     *                If a database error occurs while fetching values
     */
    public List toList() throws SQLException
    {
        List resultsList = new ArrayList();

        while (hasNext())
        {
            resultsList.add(next());
        }

        // Close the connection after converting it to a list.
        this.close();
        
        return resultsList;
    }

    /**
     * Close the Iterator and release any associated resources
     */
    public void close()
    {
        try
        {
            if (results != null)
            {
                results.close();
                results = null;
            }
        }
        catch (SQLException sqle)
        {
        }

        // try to close the statement if we have one
        try
        {
            if (statemt != null)
            {
                statemt.close();
                statemt = null;
            }
        }
        catch (SQLException sqle)
        {
        }
    }
}
