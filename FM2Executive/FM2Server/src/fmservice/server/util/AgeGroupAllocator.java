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
package fmservice.server.util;

import  fmservice.httputils.common.ServiceConstants;

/**
 *
 *
 * 
 *  Determine the age group of a person  with a given age
 *  Also determine other properties w.r.t. a given age group
 */
public class AgeGroupAllocator implements ServiceConstants
{
    private  static AgeGroupAllocator agDeterminant = null; 
    private static String[] ageGroups;
    
    public static int CutOffAge = 18;
    
    
   //---------------------------------------------------------------------------------------------
    // Determine the set of valid slots for determining the age of a person
    public static String[]  getValidSet(int minAge, int maxAge) 
    {
        int numGroups = (maxAge - minAge)/AGE_GROUPING_INTERVAL;
        if  ((maxAge % AGE_GROUPING_INTERVAL) == 0)
              numGroups += 1;  

        ageGroups = new String[numGroups];
        for (int i = 0; i < numGroups; i++)
        {
            int start = AGE_GROUPING_INTERVAL * i;
            int end = start+AGE_GROUPING_INTERVAL-1;
            if (end > MAXIMUM_AGE)
                end = MAXIMUM_AGE;
            ageGroups[i] = start+"-"+end;
        }
        return ageGroups;
    }
    
    //----------------------------------------------------------------
    public static AgeGroupAllocator getInstance() 
    {
        if (agDeterminant == null)
            agDeterminant =  new AgeGroupAllocator();
        return agDeterminant;
    }
    //----------------------------------------------------------------
   // protected  void AgeGroupDeterminant()
    protected  AgeGroupAllocator()
    {
        int numGroups = (MAXIMUM_AGE - MINIMUM_AGE)/AGE_GROUPING_INTERVAL;
        if  ((MAXIMUM_AGE % AGE_GROUPING_INTERVAL) == 0)
              numGroups += 1;  

        ageGroups = new String[numGroups];
        for (int i = 0; i < numGroups; i++)
        {
            int start = AGE_GROUPING_INTERVAL * i;
            int end = start+AGE_GROUPING_INTERVAL-1;
            if (end > MAXIMUM_AGE)
                end = MAXIMUM_AGE;
            ageGroups[i] = start+"-"+end;
        }
    }
    
    //-------------------------------------------------------------------
    public  String[]  getAllAgeGroups()
    {   
        return ageGroups;
    }

     //--------------------------------------------------------------------------
     public  int getAgeGroupSlot (int age)
    {
         int currentGroup = -1;
        
        for (int i = 0; i < ageGroups.length; i++)
        {
            String[] limits = ageGroups[i].split("-");
            int lower = Integer.valueOf(limits[0]).intValue();
            int upper =  Integer.valueOf(limits[1]).intValue();
            if (age >= lower && age <= upper)
                return i;
        }
        return -1;
    }
     
     public  String getAgeGroup (int age)
    {
        int sn= getAgeGroupSlot (age);  // slot number
        if (sn == -1)
            return null;
        else
            return ageGroups[sn];        
      }
    
  
         //--------------------------------------------------------------------------
     public  String getNextAgeGroup (int age)
    {
        int sn = getAgeGroupSlot (age);
        if ( sn == -1 || sn == ageGroups.length-1)    // the last one
            return null;        // invalid age or no more slots
        else 
            return ageGroups[sn+1];        
      }
    
     //--------------------------------------------------------------------------
       public  String getPrevAgeGroup (int age)
    {
        int sn = getAgeGroupSlot (age);
        if ( sn == -1 || sn == 0)    // the first one
            return null;        // invalid age or no more slots
        else 
            return ageGroups[sn-1];        
      }

     //------------------------------------------------------------------------
     // Get the age group adjacent to the given age.
     // The algorithm is as folloes:
     // If the age is below the median in that slot, return the lower one
      // If it is equal to or above the median, return the upper one
      //--------------------------------------------------------------------------
     public  String getNearestAgeGroup (int age)
    {
         int median = age % AGE_GROUPING_INTERVAL;
         if (median < (AGE_GROUPING_INTERVAL/2))
             return getPrevAgeGroup(age);
         else
             return getNextAgeGroup(age);
    }   
     
     
       
    /*------------------------------------------------------------------------------------------------------*/
    // convert a user specified age to a group for fasterr facematching within that group
    //  This is the original FM1 way of allocating an agegroup to a person for search
    //------------------------------------------------------------------------------------------------------*/
    public static String convertAgeToGroup( int age)
    {
        if (age == -1)
            return UNKNOWN;
        if (age >= MINIMUM_AGE &&  age < CutOffAge)
            return CHILD;
        else if (age  >= CutOffAge)
            return ADULT;
        else
            return null;
    }
    
     /*------------------------------------------------------------------------------------------------------*/
    // For Query, ages in range  CutoffAge+=1 are regarded as unknowns as they 
    // are to be searched in both child and adult groups.
    //------------------------------------------------------------------------------------------------------*/
    public static String convertAgeToQueryGroup( int age)
    {
        if (age == -1)
            return UNKNOWN;
        if (age >= MINIMUM_AGE &&  age < (CutOffAge-1))
            return CHILD;
        else if (age  > (CutOffAge+1)   && age <= MAXIMUM_AGE)
            return ADULT;
        else
            return UNKNOWN;         // could be in either group for query
    }

     //--------------------------------------------------------------------------
    
    public static void  main(String[] args)
    {
        
        // Show the age groups    
        String outStr = ("Age groups are: " );
        
        AgeGroupAllocator agDet = AgeGroupAllocator.getInstance();
        String[]  ageGroups = agDet.getAllAgeGroups();
        for (int i = 0; i < ageGroups.length; i++)
            outStr += ageGroups[i]+", ";
        System.out.println(outStr); 
        
        // convert a few ages to its group
        int[] ages = {0, -1 -20, 5, 35, 100, 130, 120, 30, 40, 43, 47, 99, 101}; 

        for (int i = 0; i < ages.length; i++)
        {
            int age = ages[i];
            System.out.println( "\n>> Age: " + ages[i] + ", AgeGroup: " +  agDet.getAgeGroup(age));
            System.out.println("Previous Group: " + agDet.getPrevAgeGroup(age) +", Next Group: " + agDet.getNextAgeGroup(age) 
                            + ", NearestAgeGroup: "  + agDet.getNearestAgeGroup(age));
        }
    }
}
