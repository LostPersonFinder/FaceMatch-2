-- *Example* Script for creating a user and database for Facematch2 operations 
-- for the database named FM2DB
-- Submit this as root

-- CREATE USER <fm2master> IDENTIFIED BY PASSWORD '<password>';
-- 

CREATE DATABASE IF NOT EXISTS FM2DB;
-- USE MYSQL;

-- fm2-stage machine
GRANT ALL PRIVILEGES ON FM2DB.* to
                <fm2master> @<host IP address> IDENTIFIED BY
                '<password>' with GRANT OPTION;

-- local host where executed
GRANT ALL PRIVILEGES ON FM2DB.* to
                <fm2master> @'127.0.0.1' IDENTIFIED BY
                '<password>' with GRANT OPTION;

--  development linux
GRANT ALL PRIVILEGES ON FM2DB.* to
                <fm2dev> @'dev-hostname' IDENTIFIED BY
                '<dev-password>' with GRANT OPTION;
-- ...
--
GRANT ALL ON FM2DB.* to <fm2master> IDENTIFIED BY '<password>';
FLUSH PRIVILEGES;
