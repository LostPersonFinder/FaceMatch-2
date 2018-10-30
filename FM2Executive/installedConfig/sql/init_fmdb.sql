-- 
-- Must first log into mysql database as 
--   > mysql -u <fm2master> -pfm2$$MASTER FM2DB
-- Then load the admin table and the following registries
--
-- NOTE: Make sure that the FaceMatch2 server or TOMCAT/WebFM2 are not running
-- since they have opened connections to the database.
--
 
USE FM2DB;

-- create the facematch database 
source fm2DBschema.sql;


-- Initialize the following tables individually 
--
source set_admin.sql;
source indextype_registry.sql;

--
-- system echo 'Facematch database initialization completed \n -----------------------'
--
