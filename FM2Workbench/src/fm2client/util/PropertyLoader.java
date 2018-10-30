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
 * PropertyLoader.java
 */

package fm2client.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

/**
 *
 *  Load the information provided in an input file as key=value pair into a Java
 * Properties object, resolving ant reference to another property
 * 
 */
public class PropertyLoader
{
     public static int RECURSION_LIMIT = 5;           // indirection limit
     
     public static  Properties loadProperties(String configFile) throws Exception
    {
        InputStream is;
        if (configFile == null || configFile.trim().length() == 0) 
        {
             System.out.println("No Property  file name specified");
              throw new RuntimeException("Cannot find configuration file " + configFile);
        }

        is = new FileInputStream(configFile);
        if (is == null)
         {
             System.out.println("Could not find specified configuration file:  " + configFile);
             throw new RuntimeException("Cannot find configuration file " + configFile);
         }

        Properties properties = new Properties();
        properties.load(is);

        // walk values, interpolating any embedded references.
        for (Enumeration pe = properties.propertyNames(); pe.hasMoreElements(); )
        {
            String key = (String)pe.nextElement();
            String value = interpolate(properties, key, 1);
            
            // replace the existing value, specified by '$()' with the de-referenced one
            if (value != null)
                properties.setProperty(  key, value);         // replace the exising value with de-referenced one
        }
        is.close();
        return properties;
    }

     /**
     * Recursively interpolate variable references in value of
     * property named "key".
     * @return new value if it contains interpolations, or null
     *   if it had no variable references.
     */
    private static String interpolate(Properties properties, String key, int level) throws Exception
    {
        if (level  > RECURSION_LIMIT)
            throw new IllegalArgumentException("ProprtyLoader: Too many levels of recursion in property file "
                + "variable interpolation, property="+key);
        String value = (String)properties.get(key);
        int from = 0;
        StringBuffer result = null;
        while (from < value.length())
        {
            int start = value.indexOf("${", from);
            if (start >= 0)
            {
                int end = value.indexOf("}", start);
                if (end < 0)
                    break;
                String var = value.substring(start+2, end);
                if (result == null)
                    result = new StringBuffer(value.substring(from, start));
                else
                    result.append(value.substring(from, start));
                if (properties.containsKey(var))
                {
                    String ivalue = interpolate(properties, var, level+1);
                    if (ivalue != null)
                    {
                        result.append(ivalue);
                        properties.setProperty(var, ivalue);
                    }
                    else
                        result.append((String)properties.getProperty(var));
                }
                else
                {
                    System.err.println("Interpolation failed in value of property \""+key+
                             "\", there is no property named \""+var+"\"");
                }
                from = end+1;
            }
            else
                break;
        }
        if (result != null && from < value.length())
            result.append(value.substring(from));
        return (result == null) ? null : result.toString();
    }
    /*-------------------------------------------------------------------------------------------------*/ 
     /**
      * Load in log4j config properties from  log4j.properties file using PropertyConfigurator.
      * Set the log file name as a system property to be used for logging  messages
      * from the application.
      */
    public static String  initLogging(Properties properties)
    {     
        String log4jConfProp = "";
        //Load in log4j config properties from  log4j.properties file
        log4jConfProp =  properties.getProperty("config.template.log4j.properties");

        // set the log file name as a 
        String log4jFilename = properties.getProperty("log4j.filename");
        if ( log4jConfProp == null || log4jFilename == null)
        {
            System.err.println("Missing log4jPropertyConfigurator and.or log file name in the input Properties");
            return null;
        }
        System.setProperty("log4j.filename", log4jFilename);
        PropertyConfigurator.configure(log4jConfProp);
        return log4jFilename;
    }
}
