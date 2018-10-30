--
-- database_schema.sql
--
-- Version: $Revision: 1.1 $
--
-- Date:    $Date: 2015/02/20$
--
--   FaceMatch SQL schema
--
--   author: 
--
--   This file is used to define the FaceMatch database structure and initialize it. 
-- ----------------------------------------------------------------------------
--
-- The registry tables in the schema refers to static data which are loaded
-- to the database at start-up time. These tables are:
-- fmclient, IndexStore, indextyperegistry, and metadatafieldregistry
-- 	
-- ------------------------------------------------------------------------------

DROP DATABASE FM2DB;
CREATE DATABASE FM2DB;
USE  FM2DB;

--
--
-- -----------------------------------------------------
-- FmClient table - Clients (users) of the FaceMatch system
-- Created at client initialization time
-- -----------------------------------------------------
CREATE TABLE fmclient
(
  client_id 	  	  INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  client_key		  VARCHAR(32) UNIQUE, 			-- key provided to client by FM
  client_name		  VARCHAR(32) UNIQUE,			-- unique name of the client
  store_thumbnails	  BOOL,							-- create and save ingested image thumbnails
  creation_date		  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,	-- date when created
  description         VARCHAR(256)					-- description of the client
);

--
-- -----------------------------------------------------
-- ClientStore table - index sets and other data stored by Facematch 
-- A clientstore belongs to (created for) a single client only
-- It identifies the storage location for index files for client's 
-- image data, as well as thumbnails (if secified) and assigned 
-- internally by FaceMatch Administrator
-- -----------------------------------------------------

CREATE TABLE clientstore
(
  store_id 	  		  INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  client_id	      	  INTEGER REFERENCES fmclient(client_id),		-- owning client
  description         VARCHAR(256),		-- description of the clients image set indexed
  index_root       	  VARCHAR(256),		-- root path where to store index data (indexes)
  thumbnail_root	  VARCHAR(256),		-- root path to store thumbnails, if client stores them
  read_permission	  BOOL	  			-- for world (always true for indexstore owner)
);

--
--
-- -----------------------------------------------------
-- ImageExtent table - Sub-grouping of images for a client
-- Extents may be based upon a common characteristic such as:
-- an event (disaster), a geographic location (country),
-- an ethnic group, a date-range (after 2001-2010), etc. 
-- There is a one-to-many relation between client and 
-- and imageextent
-- -----------------------------------------------------
CREATE TABLE imageextent
(
  extent_id 	  	  INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  client_id	      	  INTEGER REFERENCES fmclient(client_id),
  extent_name		  VARCHAR(32),
  description         VARCHAR(256),
  creation_date		  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,	-- date when created
  is_active		  	  BOOL,			-- is it still active (searchable)?
  facefind_pref	      VARCHAR(16)   -- preferred face_find performance type   (Accurary/Optimal/Speed)
);

--
--
-- -----------------------------------------------------
-- IndexTypeRegistry table - for Image Index Descriptors
-- index type refers to names SURF, SHFT, ORB etc.
-- -----------------------------------------------------
CREATE TABLE indextyperegistry
(
  index_id 	  		  INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  index_type		  VARCHAR(32) NOT NULL UNIQUE,
  file_extension	  VARCHAR(16),
  description         TEXT,
  in_use              BOOL    -- Identify types which are not used operationally
);

-- -----------------------------------------------------
-- FMImage table - holds specifics for each ingested image
-- Note that no image file is stored at the FaceMatch site.
-- Default for image_source is the same as image_handle
-- image_handle of each image is returned in query results
-- -----------------------------------------------------
CREATE TABLE fmimage
(
   image_id			INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
   extent_id		INTEGER REFERENCES imageextent(extent_id),
   unique_tag		VARCHAR(256),	-- a unique image tag (per extent), specified by client at ingest
   image_source		VARCHAR(256),   -- Full name by which image is specified by the client 
   thumbnail_path	VARCHAR(256),	-- relative path of thumbnail in local system (null if no thumbnail)
   is_facial		BOOL,			-- True if image is for matching faces (not whole images)
   num_zones		INTEGER,		-- number of zones detected in the image (zero means no regions)
   creation_date	TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,		-- when first created 
   is_deleted       BOOL			-- if the image is deleted (but this row is not deleted from DB)
);


-- -----------------------------------------------------
-- ImageZone table
-- contains the coordinates of zones detected in each
-- image. For faces, each zone corresponds to a face.
-- For whole images, there is only one zone
-- Individual descriptors are created for each region (index >= 0)
-- -----------------------------------------------------
CREATE TABLE imagezone
(
   zone_id				INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
   image_id				INTEGER REFERENCES imagecontext(image_id),  -- container image 
   zone_index			INTEGER,		-- index number of region in image, 0-based
   zone_coord_x	    	INTEGER,		-- Left top x coordinate 
   zone_coord_y	    	INTEGER,		-- Left top y coordinate 
   zone_width	    	INTEGER,		-- width of face
   zone_height	    	INTEGER,		-- height of face
   is_face				BOOL,			-- true if the region is a face
   is_profile			BOOL,			-- front or profile view, if a face
   creation_date		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP		-- when first created 
);

-- -----------------------------------------------------
-- ZoneDescriptor table
-- A group of n descriptors per image/zone, one per index type
-- Duplicate information such as image_id, region_id etc.
-- are stored here for faster access.
-- -----------------------------------------------------
CREATE TABLE zonedescriptor
(
   descriptor_id		INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
   zone_id				INTEGER REFERENCES zone(zone_id),		 
   image_id		   		INTEGER REFERENCES imagecontext(image_id),	-- image for this descriptor
   index_type      		VARCHAR(16) REFERENCES indextyperegistry(index_type),
   index_version		VARCHAR(16),	-- Revision version number of this indexing  
   file_path            VARCHAR(256),	-- relative path(w.r.t. client's indexRoot) of descriptor file
   size_bytes           BIGINT,			-- file size in bytes
   md5_checksum        VARCHAR(64),
   creation_date		TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP 	-- time when generated
);
-- index by index_type
CREATE INDEX index_typex ON zonedescriptor(index_type);

-- index by image_id
CREATE INDEX image_descr_idx ON zonedescriptor(image_id);


-- -----------------------------------------------------
-- metadatafield registry Tables 
-- image metadata for a person may be age, gender, name, etc.
-- metadata for other types (e.g. whole) images is TBD.
-- Note: Valid_set is comma separated list of valid values (for 
-- text fields only.) If empty, all values accepted 
-- -----------------------------------------------------

CREATE TABLE metadatafield
(
  field_id   		  INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  client_id			  INTEGER REFERENCES fmclient(client_id),
  field_name          VARCHAR(64),
  field_type		  VARCHAR(16),  -- INTEGER/REAL/STRING/DATE		
  valid_set		  	  VARCHAR(256),	-- '|' separated field values/ranges that are valid
  default_value		  VARCHAR (16),	-- default value to use if not given
  is_searchable		  BOOL,			-- is a searchable field for image matching
  scope_note          VARCHAR(64) 
);

-- -----------------------------------------------------
-- indexStore to metadata field - static table
-- Same metadata field may be used by more than one client
-- -----------------------------------------------------
CREATE TABLE indexstore2metadatafield
(
  store2field_id			INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY, 
  indexstore_id				INTEGER REFERENCES indexstore(store_id),
  metadata_field_id 		INTEGER REFERENCES metadatafieldregistry(metadata_field_id)
);


-- -----------------------------------------------------
-- Metadata values pertaining to a given image 
-- provided by client in ingest service request
-- Can be queried by metadata_field_id or image_id 
-- Note: for integer metadata fields (e.g. age), convert value to int
-- -----------------------------------------------------

CREATE TABLE imagemetadata
(
  metadata_entry_id  INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
  image_id           INTEGER REFERENCES imagecontext(image_id),
  metadata_field_id  INTEGER REFERENCES metadatafieldregistry(metadata_field_id),
  metadata_value	 VARCHAR(32)		-- text value of the metadata field 
);


-- -------------------------------------------------------
-- Tables for recording Performance related to various region(face) 
-- match operations such as facefind, ingest, query
-- --------------------------------------------------------

CREATE TABLE facefindperformance
(
	operation_id		INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	client_id			INTEGER REFERENCES imageextent(extent_id),
	image_url			VARCHAR(256),   	-- Location of the given image 			-- 
	num_faces			INTEGER,			-- number of faces found in the image
	service_date		TIMESTAMP,				-- (Completion) time of ingest operation
	facefind_time		REAL,				-- time to localise the regions (faces) in millisec
	facefind_option		int,				-- 1-> accuracy, 2 -> optimal, 3-> speed
	skin_cm_kind		VARCHAR(32),		-- skin color mapper kind (presently ANN or "")
	gpu_used			BOOL				-- True if GPU used for computation, false otherwise


);

CREATE TABLE regioningestperformance
(
	operation_id		INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	extent_id			INTEGER REFERENCES imageextent(extent_id),
	image_id			INTEGER REFERENCES fmimage(image_id),
	service_date		TIMESTAMP,				-- (Completion) time of ingest operation
    index_type			VARCHAR(32),		-- type of FM indexing used in matching
	index_version		VARCHAR(32),		-- verson number of indexing algorithm
	num_faces			INTEGER,			-- number of faces in the image which were ingested
	facefind_time		REAL,				-- time to localise faces in the ingested image in millisec
    facefind_option		int,				-- 1-> accuracy, 2 -> optimal, 3-> speed
	ingest_time			REAL,				-- total time for the ingest operation
	gpu_used			BOOL				-- True if GPU used for computation, false otherwise
);


CREATE TABLE regionqueryperformance
(
	operation_id		INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	extent_id			INTEGER REFERENCES imageextent(extent_id),
	queryimage_url		VARCHAR(256),   	-- Location of the query image as specified by the client 
	service_date		TIMESTAMP,				-- (Completion) time of ingest operation
    index_type			VARCHAR(32),		-- type of FM indexing used in matching
	indexmatch_type		VARCHAR(32),		-- type of match to be performed ("" => default)
	match_tolerance		REAL,				-- cut off tolerance specified by user
	max_return_matches	INTEGER,			-- maximum matches to return specified by user
	facefind_time		REAL,				-- time to localise faces in the query image in millisec
	facefind_option		int,				-- 1-> accuracy, 2 -> optimal, 3-> speed
    num_index_loaded	INTEGER,			-- additional index file uploaded for query	
	index_upload_time	REAL,				-- time to upload additional index files in millisec

	region_num			int,				-- region number of the query image being matched
	query_region		VARCHAR(256),		-- coordinates of the region being matched
	regionmatch_time	REAL,				-- Time to just match the face region (an approximation)
	query_time			REAL,				-- Time to complete entire query operation
	num_matches			INTEGER,			-- number of matches found within given tolerance
	bestmatch_distance	REAL,				-- best match distance 
	gpu_used			BOOL				-- True if GPU used for computation, false otherwise
);


-- -- -----------------------------------------------------
-- -- For System Administrators - for FM startup/shutdown etc.
-- -- -----------------------------------------------------
CREATE TABLE fmadmin
(
	admin_id			INTEGER NOT NULL AUTO_INCREMENT PRIMARY KEY,
	admin_name			VARCHAR(16) NOT NULL UNIQUE,	-- unique name of the administrator
	password			VARCHAR(64) NOT NULL UNIQUE		--	unique MD5 password
);

-- -- ------------------------------------------------------
-- Add additional Indexes to various tables for faster SELECT 
-- -- ------------------------------------------------------

-- index fmimage by extent_id
CREATE INDEX fmimage_extent_idx ON fmimage(extent_id);

-- index fmimage by unique_tag
CREATE INDEX fmimage_tag_idx ON fmimage(unique_tag);

--
-- CREATE TABLE imagemetadata  with fields:
-- (metadata_entry_id, image_id,metadata_field_id, metadata_value) 

CREATE INDEX metadatavalue_image_idx ON imagemetadata(image_id);


-- index imagezone by image_id
CREATE INDEX imagezone_image_idx ON imagezone(image_id);
  
-- index zonedescriptor also by zone_id
CREATE INDEX zonedescriptor_zone_idx ON zonedescriptor(zone_id);

-- -- ------------------------------------------------------


