-- -----------------------------------------------------------------------
-- Fill in the different types of image indexing performed 
-- by FM for landmark/face detection and matching
-- ------------------------------------------------------------------------
/*
 Structure of indextyperegistry
(
  index_type_id 	  INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  index_type		  VARCHAR(32) UNIQUE,
  featureset_size	  INTEGER,
  file_extension	  VARCHAR(16),
  description         TEXT,
  
  -- Identifies types which are not (yet?) used operationally
  -- e.g. old index files (with different feature set) before being purged 
  in_use             BOOL
  */
  

USE <FM2DB>;

INSERT INTO  indextyperegistry
values(
	1, 								-- id
	'HAAR',							-- name
	'HAAR.ndx',						-- file extension for storing data
	'HAAR descriptors',	   			-- description
	true							-- being used
);
INSERT INTO  indextyperegistry
values(
	2, 								-- id
	'LBPH',							-- name
	'LBPH.ndx',						-- file extension for storing data
	'LBPH descriptors',	   			-- description
	true							-- being used
);
INSERT INTO  indextyperegistry
values(
	3, 								-- id
	'ORB',							-- name
	'ORB.ndx',						-- file extension for storing data
	'ORB descriptors',	   			-- description
	true							-- being used
);
INSERT INTO  indextyperegistry
values(
	4, 								-- id
	'SURF',							-- name
	'SURF.ndx',						-- file extension for storing data
	'SURF descriptors',	   			-- description
	true							-- being used
);
INSERT INTO  indextyperegistry
values(
	5, 								-- id
	'SIFT',							-- name
	'SIFT.ndx',						-- file extension for storing data
	'SIFT descriptors',	   			-- description
	true							-- being used
);
INSERT INTO  indextyperegistry
values(
	6, 								-- id
	'RSILC',						-- name
	'RSILC.ndx',					-- file extension for storing data
	'RSILC descriptors',	   		-- description
	true							-- being used
);
INSERT INTO  indextyperegistry
values(
	7, 								-- id
	'DIST',							-- name
	'DIST.ndx',						-- file extension for storing data
	'Ranked descriptors',	   		-- description
	true							-- being used
);
