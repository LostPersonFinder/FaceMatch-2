-- -----------------------------------------------------------------------
-- Initialize static tables related to Facematch System Admin
-- ------------------------------------------------------------------------
USE FM2DB;

--
-- create a single administrator
--
INSERT INTO fmadmin 
values(
	1, 										-- id
	'fmadmin',								-- name
	'<password>'	    					-- MD5 password of  admin operator
);



