/*
Informational Notice:
This software was developed under contract funded by the National Library of Medicine, which is part of the National Institutes of Health, 
an agency of the Department of Health and Human Services, United States Government.

- The license of this software is an open-source BSD license.  It allows use in both commercial and non-commercial products.

- The license does not supersede any applicable United States law.

- The license does not indemnify you from any claims brought by third parties whose proprietary rights may be infringed by your usage of this software.

Government usage rights for this software are established by Federal law, which includes, but may not be limited to, Federal Acquisition Regulation 
(FAR) 48 C.F.R. Part52.227-14, Rights in Data—General.
The license for this software is intended to be expansive, rather than restrictive, in encouraging the use of this software in both commercial and 
non-commercial products.

LICENSE:

Government Usage Rights Notice:  The U.S. Government retains unlimited, royalty-free usage rights to this software, but not ownership,
as provided by Federal law.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
•	Redistributions of source code must retain the above Government Usage Rights Notice, this list of conditions and the following disclaimer.

•	Redistributions in binary form must reproduce the above Government Usage Rights Notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.

•	The names,trademarks, and service marks of the National Library of Medicine, the National Cancer Institute, the National Institutes 
of Health,  and the names of any of the software developers shall not be used to endorse or promote products derived from this software without 
specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE U.S. GOVERNMENT AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITEDTO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE U.S. GOVERNMENT
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

/*
 * TestResult Saver
 */
package fm2client.testgen;


import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


import org.apache.log4j.Logger;
/**
 *
 * Store the results returned by the Server in a disk file (for later display)
 * 
 * If the file is opened in read mode, it should already exist.
 * If opened in write mode, file is created if does not exist, data is always append to it.
 * 
 */
public class TestResultFile 
{
    private static Logger log = Logger.getLogger(TestResultFile.class);
    protected String filename;
    
    protected Object fileHandle = null;
    protected boolean isWrite;
    protected boolean openOk = false;
    
   /*-----------------------------------------------------------------*/
   // Open a file in Write (create) or Read mode
   /*-----------------------------------------------------------------*/
    public TestResultFile(String filename, boolean writeMode)
    {
        isWrite = writeMode;
        this.filename = filename;
        
        if (writeMode)
        {    
            try
            {
               File outFile = new File(filename);
               File path = outFile.getParentFile();
               if (!path.exists())
               {
                   path.mkdirs();
                   log.info("Createed  file path " + path.getCanonicalPath());
               }
                if (!outFile.exists())
                    outFile.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(filename, false));            // true => append
                fileHandle = bw;
            }
            catch (Exception e)
            {
                log.error("Could not open file" + filename + " for writing", e);
            }
        }  
        
        else    // read mode
        {
            try
            {
                File inFile = new File(filename);
                if (!inFile.exists())
                {
                    log.error(filename + " does not exist for reading");
                    return;
                }
                BufferedReader br = new BufferedReader(new FileReader(filename));
                fileHandle = br;
            }
            catch (Exception e)
            {
                log.error("Error in creating " + filename + " for reading");
            }
        }
        openOk = true;
    }
    /*-----------------------------------------------------------------*/
    // Add a record
    /*-----------------------------------------------------------------*/
    public int writeRecord (String data)
    {
        if (fileHandle == null || filename == null)
        {
            log.error( "File not yet  created");
            return 0;           // file not opened
        }
        else if (!isWrite)
        {
            log.error( "File " + filename + " created in read mode");
            return 0;           // file not opened
        }
        try
        {
            BufferedWriter bw = (BufferedWriter) fileHandle;
            bw.write(data);
            bw.newLine();
            bw.flush();
            return 1;   
        }
         catch (Exception e)
        {
           log.error("Could not write records to  file" + filename, e);
        }
        return 0;
    }
    
    public boolean isFileOpened()
    {
        return openOk;
    }
    
    /*-----------------------------------------------------------------*/
    // Read next record from file
    /*-----------------------------------------------------------------*/
    public String readNextRecord ()
    {
        if (fileHandle == null || filename == null)
        {
            log.error( "File not yet opened");
            return null;           
        }
        else if (isWrite)
        {
            log.error( "File " + filename + " was created in write mode");
            return null;           // file not opened
        }

        BufferedReader br = (BufferedReader) fileHandle;
        try
        {
            String record = br.readLine();
            if (record == null)
                br.close();
            return record;
        }
         catch (Exception e)
        {
           log.error("Could not read record from file" + filename, e);
        }
        return null;
    }
    
  /*-----------------------------------------------------------------*/
   // close the file
    /*-----------------------------------------------------------------*/
    public int  closeFile()
    {
        BufferedWriter bw = null;
        BufferedReader br = null;
        try
        {
            if (isWrite)
            {
                ((BufferedWriter)fileHandle).close();
            }
            else 
                ((BufferedReader)fileHandle).close();
            return 1;
        }
        catch (Exception e)
        {
           log.error("Could not close file" + filename, e);  
        }
        return 0;
    }
}
