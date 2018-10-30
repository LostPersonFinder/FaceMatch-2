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
package fmservice.server.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 *
 * *Static  Utility class to generate top level keys for caching of indexed data in memory.
 * 
 *
 */
public class CacheKeyGen
{
   private static Logger log = Logger.getLogger(CacheKeyGen.class);
   
   private static String PrefixSeparator = "$";                      // separator for client, extent names etc.
   private static String SegmentSeparator = "/";                 // use the "/" characacter to separate each field component
   public  static String branchSegmentPrefix= "_";             // to add a segment number to a metadata branch name

   public  static String getSegmentSeparator()
   {
       return SegmentSeparator;
   }
 
 
   /*------------------------------------------------------------------------------------------------------------*/
    /**  Build the  metadata key strings corresponding to a client's images based upon
     * its metadata fields and their acceptable values (as String arrays)
     * 
     * @param metadataFieldMap
     *          metadata field name and possible  values
     * @return Key to the IndexCache where the values should reside
     * 
    /*------------------------------------------------------------------------------------------------------------*/

    public  static  String[] buildMetadataBranchNames(HashMap <String, String[]> metadataFieldMap)
    {
        int ns = metadataFieldMap.size();
        
         // Create the name segment in the form fieldname:value
        ArrayList <String[]> fieldSets = new ArrayList();        
        Iterator <String> it = metadataFieldMap.keySet().iterator();
        while (it.hasNext())
        {
            String field = it.next();
            String[] fieldValueSet = buildFieldSegments( field, metadataFieldMap.get(field));
            fieldSets.add(fieldValueSet);
        }
        String[] branchNames = concatSegments(fieldSets);
        return branchNames;
    }
    /*---------------------------------------------------------------------------------------------------------------*/
    protected  static String[]  buildFieldSegments( String fieldName, String[] values)
    {
        int nv =  values.length;
        String[]  segments = new String[nv];
        for (int i = 0; i <nv; i++)
        {
            segments[i] = (fieldName+":"+values[i]).toLowerCase();
        }
        return segments;
    }
    /*------------------------------------------------------------------------------------------------------------------------*/
   // concatenates the segments in different sets to create a full set of n*m components
    /*------------------------------------------------------------------------------------------------------------------------*/
    protected static String[] concatSegments(ArrayList<String[]> segmentSets)
    {
        String[] resultSet = segmentSets.get(0);
        for (int i = 1; i < segmentSets.size(); i++)
        {
            resultSet = concatSegments(resultSet,  segmentSets.get(i));
        }
        return resultSet;
    }
  
    /*------------------------------------------------------------------------------------------------------------*/
    protected static String[] concatSegments(String[] set1, String[] set2)
    {
       String[]  concatenated = new String[set1.length*set2.length];
       int nc = 0;
        for (int i = 0; i < set1.length; i++)
        {
            String seg1 = set1[i];
            for (int j = 0; j < set2.length;  j++)
            {
                String newSeg =  seg1+SegmentSeparator+set2[j];
                concatenated[nc++] = newSeg;
            }
        }
        return concatenated;
    }

    /*------------------------------------------------------------------------------------------------------------*/
    protected static ArrayList<String> concatSegments(ArrayList<String> set1,
        ArrayList <String> set2)
    {
        ArrayList<String> concatenated = new ArrayList();
        for (int i = 0; i < set1.size(); i++)
        {
            String seg1 = set1.get(i);
            for (int j = 0; j < set2.size();  j++)
            {
                String newSeg =  seg1+SegmentSeparator+set2.get(j);
                concatenated.add(newSeg);
            }
        }
        return concatenated;
    }
    
    
    /*---------------------------------------------------------------------------------------------------------------------------------------*/
    /**  Find the metadata branch name corresponding an image with specified metadata
     * 
     * @param branchNames  names of a Metadata tree's branches 
     * @param metadata          metadata associated with an image. (If no metadata exists, a default should have been given)
     * @return name of the branch corresponding to the image
     *---------------------------------------------------------------------------------------------------------------------------------------*/
    public  static  String findMetadataBranchName(String[] branchNames, 
        HashMap <String, String> metadata)
    {
        int ns = metadata.size();
        String[] mdSegs = new String[ns];
        Iterator <String> it = metadata.keySet().iterator();
        int i = 0;
        while (it.hasNext())
        {
            String key = it.next();
            mdSegs[i++] = key+":"+ metadata.get(key);
        }
        
        // check the branch containing these segments
        // Note: we hav e to compare this way as the segments orders is unknown
       for (int bi = 0; bi < branchNames.length; bi++)
       {
           boolean matching = true;     // assume a complete match
           String branch = branchNames[bi].toLowerCase();
            for (int si = 0; si < ns && matching; si++)
            {
                 if (!branch.contains(mdSegs[si]))
                   matching = false;
            }
            if (matching)
                return branch;                  // all segments matched
       }
       return null;         // no match - should not happen
   }  
    
   //----------------------------------------------------------------------------------------------------------*
   /** 
    * Get the metadata key/value set from a metadata branch name.
    * Each entry assumed to comprises of the metadata fieldname and field value
    * 
    * @param branchName The metadata branch name to be decoded
    * @return HashMap of <metadataName, fieldValue>
    * 
    //----------------------------------------------------------------------------------------------------------*/
    public static  LinkedHashMap<String, String> decodeMetadataBranchName(String branchName)
    {
        String[] components = branchName.split("\\"+PrefixSeparator);
        if (components.length != 3)
            return null;                        // not a valid branch name
        String mdPart = components[2];
        
        // each Metadata related segment consists of fieldName:fieldValue, separated by segment separator
        LinkedHashMap <String, String> mdFieldValueMap = new LinkedHashMap();
        String[] parts = mdPart.split(SegmentSeparator);
        for (int i = 0; i < parts.length; i++)
        {
            String nameValuePair = parts[i];
            String[] nv = nameValuePair.split(":");
            if (nv.length != 2)
                continue;               // an error, should not happen for a valid braanch
            mdFieldValueMap.put(nv[0], nv[1]);
        }
         return mdFieldValueMap;   
    }
   
    /**
    * Add the prefixes (client name and extent name) to make each key unique.
    * 
    * @param clientName
    * @param extentName
    * @param keyParts
    *           Array of Strings corresponding to the metadata-based value list
    */ 
   public static String[] buildUniqueKeys(String clientName, String extentName, String[] mdKeyParts)
   {
       String[]  uniqueKeys = new String[mdKeyParts.length];
       // convert to lowercases
       clientName = clientName.toLowerCase();
       extentName = extentName.toLowerCase();
       for (int i = 0; i < mdKeyParts.length; i++)
           uniqueKeys[i] = buildUniqueKey(clientName, extentName, mdKeyParts[i]);
       return uniqueKeys;
   }
   
    public static String buildUniqueKey(String clientName, String extentName, String metadataPart)
   {
            String key = clientName.toLowerCase()+PrefixSeparator+extentName.toLowerCase()
                                +PrefixSeparator+metadataPart;
            return key;
   }
    
    public static String buildUniqueSegmentKey(String mdBranchName, int segmentNumber)
   {
           return (mdBranchName+branchSegmentPrefix+String.valueOf(segmentNumber));
   }
    
    
    //----------------------------------------------------------------------------------------------------------------
    // Build a unique key for a search context, based upon its Extent data and  metadata info 
    public static String buildUniqueSearchContextKey(String clientName, String extentName, 
            String branchName, int segmentNum)
    {
        String key = clientName.toLowerCase()+PrefixSeparator+extentName.toLowerCase();
        key = key+PrefixSeparator+branchName;
        String  uniqueKey = key+branchSegmentPrefix+String.valueOf(segmentNum);
        return uniqueKey;
    }
    
    //-------------------------------------------------------------------------------------------------------------------
    // Build the name for a SearchContext based upon the metadata branch name and its segment number
     public static String buildSearchContextName(String branchName, int segmentNum)
    {
        String  contextName  = branchName+branchSegmentPrefix+String.valueOf(segmentNum);
        return contextName;
    }


   /*-------------------------------------------------------------------------------------------------------------------*/
   /*  Unique cache keys corresponding to a table row
   /*-------------------------------------------------------------------------------------------------------------------*/
    // Build the key (Handle)  to a cache a database table row object based on a single table's item id
    public static String buildHandle (String tableName,  int primaryKeyId)
    {
        String handle = tableName +":"+ String.valueOf(primaryKeyId);
        return handle.toLowerCase();
    }
    
    // Handle (key) to a cache object based on a single table's item id
    public static String buildHandle (String tableName, Integer primaryKeyId)
    {
        String handle = tableName +":"+primaryKeyId.toString();
        return handle.toLowerCase();
    }
    
    /*-------------------------------------------------------------------------------------------------------------------*/
    // Handle (key) based upon a join of two tables
    public static String buildHandle(String tableName1,  String tableName2, int  rowId)
    {
        String handle = tableName1 + "_" + tableName2 +":"+ String.valueOf(rowId);
        return handle.toLowerCase();
    }
    
    //-------------------------------------------------------------------------------------------------
    // get the primaryKeyId from the handle
    public static int getPrimaryKeyId(String handle)
    {
        String[] parts = handle.split(":");
        if (parts.length != 2)
        {
            log.error("Cannot retrieve the row ID from invalid Database object handle " + handle);
            return 0;
        }
        int id = Integer.parseUnsignedInt(parts[1]);
        return id;
    }

    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}
