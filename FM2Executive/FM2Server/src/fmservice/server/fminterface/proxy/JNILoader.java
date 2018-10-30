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
package fmservice.server.fminterface.proxy;

//import sharedload.SharedLibLoader;

import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * This module loads (links with) one or more  shared library/DLLs from a globally-defined path
 * with the application. 
*<p>
 * To run under Tomcat, the shared libraries should be put under Tomcat's
 * java.library.path assigned in catalina.bat (under Windows) as:
 * set JAVA_OPTS="-Djava.library.path=<TomcatInstallDirectory>/shared/lib"
 * and with  similar statement in catalina.sh under Linux
 * <p>
 * However, this does not work -- Needs to be tested and fixed.
 * Presently, the shared libraries are put in LD_LIBRARY_PATH under Linux.
 * This has the problem of stopping and restarting Tomcat every time FM2WebServer has to be restarted.
 *<p>
 * Note: Output to both log file and output device are used here for stand-alone tests without log files 
 */
public class JNILoader
{
    private static Logger log = Logger.getLogger(JNILoader.class.getName());
   

   public static ArrayList<String> loadedLibNames = new ArrayList();
   

/**
 * Load a Java Native Interface as a Shared library for the executing Web applications under Tomcat.
 * Note: It may result in ClassNotFoundException,but not directly from the body of the code.
 * 
 * Note - This does not work
 */
   
  /* static int  loadNativeLibShared(String libName)
    {
           log.trace("... Going to load system libraries");
            int status = SharedLibLoader.loadNativeLibrary(libName);
            if (status == 1)
            {
                  log.info(">> Successfully loaded JNI library: " + libName + " from Tomcat shred path");
                  loadedLibNames.add(libName);
            }
            return 1;
    }
   */
       
  /**
 * Load a Java Native Interface. The Native library must be in the library path 
 * of the executing application.
 * 
 * Added explicit shared lib path name for testing
 */
     
   static int  loadNativeLib(String libName)
    {
       log.trace("... Going to load system libraries");
       String libPath = "";

        libPath = System.getProperty("java.library.path");
        System.out.println(">>> java.library.path is set to: " + libPath);
        if (libPath == null)
        {
              log.error("java.library.path is not defined for the application to load " + libName);
             return 0;
        }
        try
        {
                System.loadLibrary(libName);
                log.info(">> Successfully loaded JNI library: " + libName + " in path " + libPath);
                loadedLibNames.add(libName);
                return 1;
        }
        catch (UnsatisfiedLinkError e)
        {

            String msg = e.getMessage();
            log.error("Error (re)loading  JNI library " + libName + " in java.library.path: "+libPath+", Link Error message: " + msg);
                // Library not found or  already loaded by another app
            if (msg.contains("already loaded"))
            {
                  loadedLibNames.add(libName);
                  return 1;
            }    
            return 0;     
        }
    }
   
     /** 
     * Check if the given shared Library (under Linux) or Dynamic Link Library (under Windows) is
     * loaded by this application
     * @param name of the shared library
     * @return true if the library is loaded, false otherwise.
     */
   /*---------------------------------------------------------------------------------------------------------*/
   public static boolean isLoaded(String libName)
   {
        if (!loadedLibNames.contains(libName))
            loadNativeLib(libName);
       return (loadedLibNames.contains(libName));
   }
}