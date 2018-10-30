-- 
-- Must first log into mysql database as 
--   > mysql -u <fm2master> -pfm2$$master FM2DB
-- Then truncate all image ingest and index related tables
--
 
USE FM2DB;

TRUNCATE TABLE fmimage;
TRUNCATE TABLE imageextent2fmimage;
TRUNCATE TABLE imagemetadata;
TRUNCATE TABLE imagezone;
TRUNCATE TABLE zonedescriptor;
TRUNCATE TABLE imagezone2descriptor;

TRUNCATE TABLE regioningestperformance;
TRUNCATE TABLE regionqueryperformance;

--
-- system echo 'Facematch image ingest table initialization completed -----------------------'
--
