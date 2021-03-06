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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


/**
 * FaceMatch2 Configuration Manager. 
 * Opens and reads the specified configuration data from the input file 
 * in key=value form and stores as a static Java Properties object
 * 
 *
 * 
 */
public class ConfigurationManager
{
    private static  Logger log = Logger.getLogger(ConfigurationManager.class);
    private static Properties properties = null;
    public static int RECURSION_LIMIT=5;
    
  /**
     * Load the FaceMatch configuration properties. 
     * Properties are loaded in from the specified configuration  file,
     * 
     * @param configFile
     *            The <code>fmservice.cfg</code> configuration file to use, or
     */
    public static  void loadConfig(String configFile)
    {
        InputStream is;

        if (properties != null)         // file aleady read and properties loaded
            return;

        if (configFile == null || configFile.trim().length() == 0) 
        {
             System.out.println("No configuration file name specified");
              throw new RuntimeException("Noconfiguration file name specified");  
        }
        
        String  log4jConfProp = null;
        try
        {
            is = new FileInputStream(configFile);
            if (is == null)
             {
                 System.out.println("Could not find specified configuration file:  " + configFile);
                 throw new RuntimeException("Cannot find configuration file " + configFile);
             }
            else
            {
                properties = new Properties();
                properties.load(is);

                // walk values, interpolating any embedded references.
                for (Enumeration pe = properties.propertyNames(); pe.hasMoreElements(); )
                {
                    String key = (String)pe.nextElement();
                    String value = interpolate(key, 1);
                    if (value != null)
                        properties.setProperty(key, value);
                }
                is.close();
            }
            
             // set the log file name as a property in the config file, tather than in log4j.properties  file
            String log4jFilename = properties.getProperty("log4j.filename");
            
           
            // Load in log4j config properties from  log4j.properties file
            log4jConfProp =  properties.getProperty("log4j.properties");
            
            //Specify it as a system property so that log4j would resolve it
            System.setProperty("log4j.filename", log4jFilename);
            System.out.println("log4j filename set to: " + System.getProperty("log4j.filename"));
            PropertyConfigurator.configure(log4jConfProp);
            
            initLog();
            info("FaceMatch  logging installed using log4j.properties in " +  log4jFilename);
             return;
        }
        catch (IOException e)
        {
            fatal("Can't load configuration", e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new RuntimeException("Cannot find "+ log4jConfProp,e);
        }
    }
    
   /*----------------------------------------------------------------------------------------------------*/
    /** Return the static " properties"  object.
   ------------------------------------------------------------------------------------------------*/  
    public static Properties getConfig()
    {
        if (properties == null)
            error("No configuration properties are loaded.");
         return properties;
    }
/*---------------------------------------------------------------------------------------------------------*/
 /**
  * Retrieve a property as specified in the configuration file.
   * @param property  Name of the property whose value is to be returned from the configuration file
   * @return value  associated with the property, null if no such key exists
   --------------------------------------------------------------------------------------------------------*/  
    public static String getProperty(String property)
    {
        if (properties == null)
        {
            error("No configuration properties are loaded.");
            return null;
        }
        return (properties.getProperty(property));
    }
    /*-----------------------------------------------------------------------------------------*/
       /**
     * Get a configuration property as an integer
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>0</code> is returned if the
     *         property does not exist. To differentiate between this case and
     *         when the property actually is zero, use <code>getProperty</code>.
     */
    public static int getIntProperty(String property)
    {
        String stringValue = properties.getProperty(property);
        int intValue = 0;

        if (stringValue != null)
        {
            try
            {
                intValue = Integer.parseInt(stringValue.trim());
            }
            catch (NumberFormatException e)
            {
                warn("Warning: Number format error in property: " + property);
            }
        }
        return intValue;
    }
    /*-----------------------------------------------------------------------------------------*/
       /**
     * Get a configuration property as a boolean. True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     * 
     * @param property
     *            the name of the property
     * 
     * @return the value of the property. <code>false</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property)
    {
        return getBooleanProperty(property, false);
    }

    /*-----------------------------------------------------------------------------------------*/
    /**
     * Get a configuration property as a boolean, with default.
     * True is indicated if the value
     * of the property is <code>TRUE</code> or <code>YES</code> (case
     * insensitive.)
     *
     * @param property
     *            the name of the property
     *
     * @param defaultValue
     *            value to return if property is not found.
     *
     * @return the value of the property. <code>default</code> is returned if
     *         the property does not exist. To differentiate between this case
     *         and when the property actually is false, use
     *         <code>getProperty</code>.
     */
    public static boolean getBooleanProperty(String property, boolean defaultValue)
    {

        String stringValue = properties.getProperty(property);

        if (stringValue != null)
        {
        	stringValue = stringValue.trim();
            return  stringValue.equalsIgnoreCase("true") ||
                    stringValue.equalsIgnoreCase("yes");
        }
        else
        {
            return defaultValue;
        }
    }

    /*
   /* Initialize logging for the application
    */
    private static void initLog()
    {
        log = Logger.getLogger(ConfigurationManager.class);
    }
    

    /**
     * Recursively interpolate variable references in value of
     * property named "key".
     * @return new value if it contains interpolations, or null
     *   if it had no variable references.
     */
    private static String interpolate(String key, int level)
    {
        if (level >RECURSION_LIMIT)
            throw new IllegalArgumentException("ConfigurationManager: Too many levels of recursion in configuration property "
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
                    String ivalue = interpolate(var, level+1);
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
                    warn("Interpolation failed in value of property \""+key+
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

    private static void info(String string)
    {
        if (log == null)
        {
            System.out.println("INFO: " + string);
        }
        else
        {
            log.info(string);
        }
    }

    private static void warn(String string, Exception e)
    {
        if (log == null)
        {
            System.out.println("WARN: " + string);
            e.printStackTrace();
        }
        else
        {
            log.warn(string, e);
        }
    }

    private static void warn(String string)
    {
        if (log == null)
        {
            System.out.println("WARN: " + string);
        }
        else
        {
            log.warn(string);
        }
    }

    private static void error(String string)
    {
        if (log == null)
        {
            System.err.println("ERROR: " + string);
        }
        else
        {
            log.error(string);
        }
    }

    private static void fatal(String string, Exception e)
    {
        if (log == null)
        {
            System.out.println("FATAL: " + string);
            e.printStackTrace();
        }
        else
        {
            log.fatal(string, e);
        }
    }

    private static void fatal(String string)
    {
        if (log == null)
        {
            System.out.println("FATAL: " + string);
        }
        else
        {
            log.fatal(string);
        }
    }
}
