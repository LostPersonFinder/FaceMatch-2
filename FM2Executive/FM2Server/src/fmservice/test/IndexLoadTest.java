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
package fmservice.test;

import fmservice.server.fminterface.adapter.FaceMatchAdapterBase;
import fmservice.server.fminterface.proxy.FRDProxy;
import fmservice.server.fminterface.proxy.FaceRegionMatcherProxy;
import fmservice.server.global.ConfigurationManager;

import java.io.File;
import java.util.ArrayList;

/**
 * This class  tests the timing parameters in loading index files through the FaceMatch Lib
 * 
 *
 */
public class IndexLoadTest extends FaceMatchAdapterBase
{
    static int NANO2MILLI = (int) Math.pow(10, 6);
    
    FaceRegionMatcherProxy  pFaceMatcher;

    
       
    public IndexLoadTest()
    {
        super();
    }
     
   /*-----------------------------------------------------------------------------------------------*/  
       protected   FaceRegionMatcherProxy createImageMatcherProxy()
     {
             // invoke ImageMatcher to match face
            int imagedim = ( (Long) FaceMatchOptions.get( "DefaultWholeImgDim")).intValue();
            int fmFlags = (int) getDefaultFaceMatcherFlags();
             // find  facematching flags and index types to be used by the FaceMatch library.
            long ffFlags = FaceMatchAdapterBase.getDefaultFaceFinderFlags();
            
            FRDProxy frd =  getFRDWithLock(false);
            FaceRegionMatcherProxy faceMatcher = new FaceRegionMatcherProxy(frd,  "DIST", 
                (int)ffFlags, imagedim, fmFlags, true );

            return faceMatcher;
    }
       
    /*-------------------------------------------------------------------------------------------------------------------*/
    // load a set of small index files and concatnat them to create larger Index files
    // Compare average load time/Index  from these files of various sizes
    public  void  doIndexLoadtimeTest(String indexDir, String saveDir, int setSize) throws Exception
    { 
        FaceRegionMatcherProxy  faceMatcher = createImageMatcherProxy();

        if (faceMatcher == null)
        {
            System.out.println("Could not create ImageMatcher  for querying with for index type DIST" );
            return ;
        }
        // Perform the real file load test
        faceMatcher.setVerboseLevel(0);
        findIndexLoadTime(faceMatcher, indexDir, saveDir, setSize);
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");    
        return;
    }
   /*-------------------------------------------------------------------------------------------------------*/ 
    protected void  findIndexLoadTime(FaceRegionMatcherProxy faceMatcher, 
            String indexDir, String saveDir,  int setSize)
    {
            File dir  = new File(indexDir);
            String[] filesInDir = dir.list();
            int numFiles = filesInDir.length;
            //numFiles = Math.min(numFiles, 2000);

            int start = 0;
            int end = 0;
            int ni= 0;
            
            // just test for one sets...dm
            while (start < numFiles & ni < 1)
            {
                
               // create a list of 'setSize' files
                end = start+setSize-1;
                if (end >=numFiles)
                    end =  numFiles-1;
                System.out.println("-- File load range: " + start +" - " + end);

                int nf  = end-start+1;            // number of files to load as a subset
                String[] filesToLoad = new String[nf];
                
                // divide into setSize number of files
                for (int i  = 0; i < nf; i++)
                {
                    String name = filesInDir[start+i];   
                    if (!name.endsWith(".ndx"))
                        continue;

                    filesToLoad[i] = dir+"/"+filesInDir[start+i];     
                }
                
                // build the save file name
                String saveFileName = saveDir+"/"+setSize+"/File-"+start+"_"+end+".ndx";
                doIndexLoadTest(faceMatcher, filesToLoad,  saveFileName);
               
                start += nf;
                ni++;
                
            }
    }
            
     /*-------------------------------------------------------------------------------------------*/
    protected void doIndexLoadTest(FaceRegionMatcherProxy faceMatcher, String[] filesToLoad,   String saveFileName)
    { 
        int setSize = filesToLoad.length;
        System.out.println("\n---------------- ----------- IndexFile set Size: " + setSize + " ------------------------------------------");

        long startTime = System.nanoTime();
        faceMatcher.clearLoadedIndexData();
        
        faceMatcher.loadIndexFiles(filesToLoad);
        long stopTime = System.nanoTime();
        long timeDiff = (stopTime - startTime);
        float millisec = (float)timeDiff/NANO2MILLI;
        System.out.println(">>> ---------------  " + setSize + " individual index files  loaded in : " + millisec+ " millisec ");
        System.out.println (">>> Average index file load time including open/close: "  + (millisec/setSize) + " millisec");

         System.out.println("-----------------------------------------------------------------------------------------------------------\n");
        
        // now write out the indexed data to a single file
        faceMatcher.saveIndexData(saveFileName);
        System.out.println ("-- " + setSize + " index data stored in  single file: " + saveFileName );
        

       // now just load these hugh files without clearing and get time
       String[]  fileNames = new String[1];
       fileNames[0] =  saveFileName;

       long startTime1 = System.nanoTime();
      faceMatcher.clearLoadedIndexData();
      
       faceMatcher.loadIndexFiles(fileNames);
       
       // repeat to test
       System.out.println("--------------------------------  repeating ---------------------------------------------------------------------------\n");
       faceMatcher.loadIndexFiles(fileNames);
       
       long stopTime1 = System.nanoTime();
       float timeDiff1 = (stopTime1 - startTime1)/NANO2MILLI;
       System.out.println("\n---------------- "+"Total time to open/load/close same index data from the single file " + 
               timeDiff1 + " mllisec");
    }                
       /*----------------------------------------------------------------------------------------------------------*/ 
    // folowing not used
    /*----------------------------------------------------------------------------------------------------------*/
      public int loadIndexFilesNSave(FaceRegionMatcherProxy faceMatcher, String[] indexFileNames,
              String saveFileName)
      {
          long start = System.nanoTime();
          faceMatcher.clearLoadedIndexData();
          
         int numLoaded =  faceMatcher.loadIndexFiles(indexFileNames);
          long stop = System.nanoTime();
          long timeDiff = (stop - start);
          float millisec = (float)timeDiff/NANO2MILLI;
          System.out.println("\">>>>>>>>> IndexLoadTest " + ", Average time to open/close/ load " + numLoaded +
                    " index files: " + (millisec/numLoaded) +" millisec <<<<<<<<<<");
          
          // also save it to the disk to check its size and then reload it for tesing
          System.out.println("-----------------------------------------------------------------------------------------------------------\n");
          if (saveFileName != null)
          {
                faceMatcher.saveIndexData( saveFileName);
                String[] fileNames = {saveFileName};
                numLoaded =  faceMatcher.loadIndexFiles(fileNames);
          }
          return numLoaded;          // number of fileNames loaded
      }

  /*----------------------------------------------------------------------------------------------------------------------------*/ 
    
        public static void main(String[] args)
        {
    
            String faceMatchOptionsFile = "<TopDir>/FM2Server/installDir/fmoptions/FFParameters1.json";
            String  indexRoot = "<TopDir>/FM2Server/localTestDir/JNITest/index";
            String configFile ="<TopDir>/FM2Server/installDir/config/FM2ServerLocal.cfg";
            String  libName = "FaceMatchJniV2";
            
            String indexDirecory =  "<TopDir>/FM2DataStore/index/pl/flooduttarakhand/DIST/V2";
            String resizeDirectory = "<TopDir>/tempDir/index";

            // Get system configuration from Proprties file
            ConfigurationManager.loadConfig(configFile);

            // get the Properties object representing the configuration
            java.util.Properties fmConfig = ConfigurationManager.getConfig();
            
            IndexLoadTest loadTester = new IndexLoadTest();
            loadTester.init(faceMatchOptionsFile, 
                    (String) fmConfig.getProperty("facematch.nativeLibName"));
            
            // Test at different set sizes
            try
            {
                int indexSetSize = 1000;
                loadTester.doIndexLoadtimeTest(indexDirecory, resizeDirectory, indexSetSize);
                System.out.println("/n-----------------------------------------------------------------------------------------------------/n");

             /*   indexSetSize = 100;
                loadTester.doIndexLoadtimeTest(indexDirecory, resizeDirectory, indexSetSize);
                System.out.println("/n-----------------------------------------------------------------------------------------------------/n");
                */

               /* indexSetSize = 10;
                loadTester.doIndexLoadtimeTest(indexDirecory, resizeDirectory, indexSetSize);
                System.out.println("/n-----------------------------------------------------------------------------------------------------/n");
               */
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
         }
} 
    
    
