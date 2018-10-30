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

/**
 * Each derived class of this class implements the following method:
 * 
 * create() - static method: inserts a new row in the database for the class's table, 
 *                  and instantiates a new object of the class with the row id.
 *                  This row's other columns are later filled by higher objects and and update()
 *                  is invoked to commit the data.
 * Constructor (new) 
 *                  - Instantiation of the class object with info from the given row.
 *                  - adds itself the the in-memory object cache. 
 *                  - Maintains  this database row, which is updated by other objects with set methods.
 *                  - instantiates its existing lower level dependent objects (unless marked as deleted)
 *                      by querying  the database -  including looking at the corresponding mapping table.
 *                      [ Note: this it true from ImageExtent downwards, but not for FMClients, since we
 *                       don't want to load all extents for a client.]
 *                  - maintains list of the lower level objects
 * add(lower level object)
 *                  - Adds to its own list. 
 *                  - Adds a row to the mapping table or counter etc., corresponding to the object,  if necessary
 * remove (lower level object)
 *                  - Removes the given lower object from its list
 *                  - Removes the row in the mapping table or counter etc., corresponding to the object,  as necessary
 * delete() - deletes the lower level objects instantiated during new() or add().
 *                  removes its row from the database table
 *                   removes self from the in-memory cache

 * setxxx()   - sets the variable xxx to a given value in the  in-memory row
 * getxxx()   - returns the value of the variable in the in-memory row
 * update() - Updates the database row by writing out the object's current row contents
 * 
 *  find() - Static method:
 *              - first looks for the object (with the given row id) in the  in-memory object cache.
 *                      If founds returns it.
 *              - Else: retrieves the DB table row with this id from the database
 *                       -instantiates a new object calling new(), which adds it to the in-memory cache 
 *                       - returns the instantiated object                
 *        
 *
 * Java access to members of a class
 * Modifier      Class      Package  Subclass  World
 * -------------------------------------------------------------------
* public            Y                Y 	      Y                 Y
* protected      Y 	       Y 	      Y                 N
* no modifier  Y                 Y               N                N
* private 	       Y 	       N               N                N
 * ------------------------------------------------------------------------------------------------------------------------------------------*
 *
 *
 */

    /**
 * Abstract base class for FaceMatch objects
 */
public abstract class DBContentObject
{    
     /**
     * Get the Object type (such as FMImage for an image) of this DBContentObject.
     * 
     * @return  its Java class name
     */
     public abstract String getObjectType();

    /**
     * Get the internal ID (database primary key) of this object
     * 
     * @return internal database ID of object
     */
    public abstract int getID();
}
