SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

CREATE TABLE db_updates (
    id character varying(255)
);

CREATE UNIQUE INDEX db_updates_pkey ON db_updates USING btree (id);

ALTER TABLE public.edit_info OWNER TO maxcom;
REVOKE ALL ON TABLE jam_topic_version FROM maxcom;
GRANT ALL ON TABLE jam_topic_version TO maxcom;
