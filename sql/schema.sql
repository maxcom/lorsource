--
-- PostgreSQL database dump
--

SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off;

--
-- Name: plpgsql; Type: PROCEDURAL LANGUAGE; Schema: -; Owner: maxcom
--

CREATE PROCEDURAL LANGUAGE plpgsql;


ALTER PROCEDURAL LANGUAGE plpgsql OWNER TO maxcom;

SET search_path = public, pg_catalog;

--
-- Name: event_type; Type: TYPE; Schema: public; Owner: maxcom
--

CREATE TYPE event_type AS ENUM (
    'WATCH',
    'REPLY',
    'DEL',
    'OTHER',
    'REF'
);


ALTER TYPE public.event_type OWNER TO maxcom;

--
-- Name: check_replyto(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION check_replyto() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
IF NEW.replyto IS NOT NULL AND NOT NEW.deleted  THEN
IF NOT EXISTS (SELECT id FROM comments WHERE NEW.replyto=comments.id AND not comments.deleted) THEN
RAISE EXCEPTION 'reply to deleted comment denied';
end if;
end if;
return NEW;
end;
$$;


ALTER FUNCTION public.check_replyto() OWNER TO maxcom;

--
-- Name: comins(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION comins() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
        cgroup int;
BEGIN
        SELECT groupid INTO cgroup FROM topics WHERE topics.id = NEW.topic FOR UPDATE;
        UPDATE topics SET stat1=stat1+1,stat2=stat2+1,stat3=stat3+1,stat4=stat4+1,lastmod=CURRENT_TIMESTAMP WHERE topics.id = NEW.topic;
        UPDATE groups SET stat1=stat1+1,stat2=stat2+1,stat3=stat3+1,stat4=stat4+1 WHERE id = cgroup;
        RETURN NULL;
END;
$$;


ALTER FUNCTION public.comins() OWNER TO maxcom;

--
-- Name: create_user_agent(character varying); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION create_user_agent(character varying) RETURNS integer
    LANGUAGE plpgsql STRICT
    AS $_$
DECLARE ua_id INT;
BEGIN
  SELECT count(*) INTO ua_id FROM user_agents WHERE name = $1;
  IF ua_id=0 THEN
    BEGIN
      INSERT INTO user_agents (name) VALUES($1);
    EXCEPTION WHEN unique_violation THEN
      -- do nothing
    END;
  END IF;
  SELECT id INTO ua_id FROM  user_agents WHERE name = $1;
  RETURN ua_id;
END;
$_$;


ALTER FUNCTION public.create_user_agent(character varying) OWNER TO maxcom;

--
-- Name: event_comment(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION event_comment() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
	parent_author int;
BEGIN
	IF NEW.replyto IS NOT NULL THEN
		SELECT userid INTO parent_author FROM comments WHERE id = NEW.replyto;
		INSERT INTO user_events (userid, type, private, message_id, comment_id) SELECT memories.userid, 'WATCH', 'f', NEW.topic, NEW.id FROM memories WHERE memories.topic = NEW.topic AND NEW.userid != memories.userid AND memories.userid != parent_author AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored=NEW.userid);
	ELSE
		INSERT INTO user_events (userid, type, private, message_id, comment_id) SELECT memories.userid, 'WATCH', 'f', NEW.topic, NEW.id FROM memories WHERE memories.topic = NEW.topic AND NEW.userid != memories.userid AND NOT EXISTS (SELECT ignore_list.userid FROM ignore_list WHERE ignore_list.userid=memories.userid AND ignored=NEW.userid);
	END IF;

        RETURN NULL;
END;
$$;


ALTER FUNCTION public.event_comment() OWNER TO maxcom;

--
-- Name: event_delete(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION event_delete() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
	grid int;
	thetopic topics%ROWTYPE;
	thecomment comments%ROWTYPE;
BEGIN
	SELECT * INTO thetopic FROM topics WHERE id = NEW.msgid;
	IF FOUND THEN
		IF thetopic.userid != NEW.delby THEN
			INSERT INTO user_events (userid, type, private, message_id, message) VALUES (thetopic.userid, 'DEL', 't', NEW.msgid, NEW.reason);
		END IF;
	ELSE
		SELECT * INTO thecomment FROM comments WHERE id = NEW.msgid;
		IF thecomment.userid != NEW.delby THEN
			INSERT INTO user_events (userid, type, private, message_id, comment_id, message) VALUES (thecomment.userid, 'DEL', 't', thecomment.topic, NEW.msgid, NEW.reason);
		END IF;

		DELETE FROM user_events WHERE comment_id = thecomment.id AND type in ('REPLY', 'WATCH', 'REF');
	END IF;
	RETURN NULL;
END;
$$;


ALTER FUNCTION public.event_delete() OWNER TO maxcom;

--
-- Name: get_title(bigint); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION get_title(bigint) RETURNS character varying
    LANGUAGE sql
    AS $_$select title from comments where id=$1 union select title from topics where id=$1$_$;


ALTER FUNCTION public.get_title(bigint) OWNER TO maxcom;

--
-- Name: msgdel(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION msgdel() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE 
        grid int;
stat_1 int;
stat int;
        thetopic topics%ROWTYPE;
        thecomment comments%ROWTYPE;
BEGIN   
        SELECT * INTO thetopic FROM topics WHERE id = NEW.msgid;
        IF FOUND THEN
                SELECT groupid INTO grid FROM topics WHERE id = NEW.msgid;
                UPDATE groups SET stat1=stat1-1 WHERE id = grid;
                UPDATE topics SET lastmod=CURRENT_TIMESTAMP WHERE id = NEW.msgid;
                UPDATE comments SET topic_deleted=true WHERE topic = NEW.msgid;
        ELSE
                SELECT * INTO thecomment FROM comments WHERE id = NEW.msgid;
                SELECT topic INTO grid FROM comments WHERE id = NEW.msgid;
                UPDATE topics SET stat1=stat1-1, lastmod=CURRENT_TIMESTAMP WHERE id = grid;
UPDATE topics SET stat2=stat1 WHERE id=grid AND stat2 > stat1;
UPDATE topics SET stat3=stat1 WHERE id=grid AND stat3 > stat1;
UPDATE topics SET stat4=stat1 WHERE id=grid AND stat4 > stat1;
                SELECT groupid INTO grid FROM topics WHERE id = grid;
                UPDATE groups SET stat1=stat1-1 WHERE id = grid;
        END IF;
        RETURN NULL;
END;
$$;


ALTER FUNCTION public.msgdel() OWNER TO maxcom;

--
-- Name: msgedit(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION msgedit() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
	thetopic topics%ROWTYPE;
BEGIN
	SELECT * INTO thetopic FROM topics WHERE id = NEW.msgid;
	IF FOUND THEN
		UPDATE topics SET lastmod=CURRENT_TIMESTAMP WHERE id = NEW.msgid;
	END IF;
	RETURN NULL;
END;
$$;


ALTER FUNCTION public.msgedit() OWNER TO maxcom;

--
-- Name: msgundel(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION msgundel() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
	grid int;
	thetopic topics%ROWTYPE;
	thecomment comments%ROWTYPE;
BEGIN
	SELECT * INTO thetopic FROM topics WHERE id = OLD.msgid;
	IF FOUND THEN
		SELECT groupid INTO grid FROM topics WHERE id = OLD.msgid;
		UPDATE groups SET stat1=stat1+1 WHERE id = grid;
		UPDATE topics SET lastmod=CURRENT_TIMESTAMP WHERE id = OLD.msgid;
		UPDATE comments SET topic_deleted=false WHERE topic = OLD.msgid;
	END IF;
	RETURN NULL;
END;
$$;


ALTER FUNCTION public.msgundel() OWNER TO maxcom;

--
-- Name: new_event(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION new_event() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	UPDATE users SET unread_events=unread_events+1 WHERE users.id = NEW.userid;
	RETURN NULL;
END;
$$;


ALTER FUNCTION public.new_event() OWNER TO maxcom;

--
-- Name: stat_update(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION stat_update() RETURNS timestamp with time zone
    LANGUAGE plpgsql
    AS $$
DECLARE
	top record;
	st1 int;
	st2 int;
	st3 int;
	st4 int;
	now timestamp;
BEGIN
	now=CURRENT_TIMESTAMP;
	FOR top IN SELECT id FROM topics WHERE stat2!=0 FOR UPDATE LOOP
		SELECT count(*) INTO st1 FROM comments WHERE topic = top.id AND NOT deleted;
		SELECT count(*) INTO st2 FROM comments WHERE topic = top.id AND now-'3 day'::interval<postdate AND NOT deleted;
		SELECT count(*) INTO st3 FROM comments WHERE topic = top.id AND now-'1 day'::interval<postdate AND NOT deleted;
		SELECT count(*) INTO st4 FROM comments WHERE topic = top.id AND now-'1 hour'::interval<postdate AND NOT deleted;
		UPDATE topics SET stat1 = st1,stat2 = st2,stat3 = st3,stat4 = st4 WHERE id = top.id AND (stat1 != st1 OR stat2 != st2 OR stat3 != st3 OR stat4 != st4 );
	END LOOP;
	RETURN now;
END;
$$;


ALTER FUNCTION public.stat_update() OWNER TO maxcom;

--
-- Name: stat_update2(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION stat_update2() RETURNS timestamp with time zone
    LANGUAGE plpgsql
    AS $$
DECLARE
	grp record;
	st1 int;
	st2 int;
	st3 int;
	st4 int;
	t1 int;
	t2 int;
	t3 int;
	t4 int;
	now timestamp;
BEGIN
	now=CURRENT_TIMESTAMP;
	FOR grp IN SELECT id FROM groups WHERE stat2!=0 FOR UPDATE LOOP
		SELECT sum(stat1) INTO st1 FROM topics WHERE groupid = grp.id AND NOT deleted;
		SELECT sum(stat2) INTO st2 FROM topics WHERE groupid = grp.id AND NOT deleted;
		SELECT sum(stat3) INTO st3 FROM topics WHERE groupid = grp.id AND NOT deleted;
		SELECT sum(stat4) INTO st4 FROM topics WHERE groupid = grp.id AND NOT deleted;
		SELECT count(*) INTO t1 FROM topics WHERE groupid = grp.id AND NOT deleted;
		SELECT count(*) INTO t2 FROM topics WHERE groupid = grp.id AND now-'3 day'::interval<postdate AND NOT deleted;
		SELECT count(*) INTO t3 FROM topics WHERE groupid = grp.id AND now-'1 day'::interval<postdate AND NOT deleted;
		SELECT count(*) INTO t4 FROM topics WHERE groupid = grp.id AND now-'1 hour'::interval<postdate AND NOT deleted;
		UPDATE groups SET stat1 = st1 + t1 ,stat2 = st2 + t2 ,stat3 = st3 + t3 ,stat4 = st4 + t4 WHERE id = grp.id AND ( stat1 != st1 + t1 OR stat2 != st2 + t2 OR stat3 != st3 + t3 OR stat4 != st4 + t4 );
	END LOOP;
	RETURN now;
END;
$$;


ALTER FUNCTION public.stat_update2() OWNER TO maxcom;

--
-- Name: topins(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION topins() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
	UPDATE groups SET stat1=stat1+1,stat2=stat2+1,stat3=stat3+1,stat4=stat4+1 WHERE groups.id = NEW.groupid;
	UPDATE topics SET lastmod=CURRENT_TIMESTAMP WHERE id = NEW.id;
	INSERT INTO memories (userid, topic) VALUES (NEW.userid, NEW.id);
	RETURN NULL;
END;
$$;


ALTER FUNCTION public.topins() OWNER TO maxcom;

--
-- Name: update_monthly_stats(); Type: FUNCTION; Schema: public; Owner: maxcom
--

CREATE FUNCTION update_monthly_stats() RETURNS timestamp without time zone
    LANGUAGE plpgsql
    AS $$
begin
delete from monthly_stats;
insert into monthly_stats ( select section, date_part('year', postdate) as year, date_part('month', postdate) as month, count(topics.id) as c from topics, groups, sections where topics.groupid=groups.id and groups.section=sections.id and (topics.moderate or not sections.moderate) and not deleted group by section, year, month);
insert into monthly_stats (section, groupid, year, month, c)  ( select section, groupid, date_part('year', postdate) as year, date_part('month', postdate) as month, count(topics.id) as c from topics, groups, sections where topics.groupid=groups.id and groups.section=sections.id and (topics.moderate or not sections.moderate) and not deleted group by section, groupid, year, month);
return CURRENT_TIMESTAMP;
end;
$$;


ALTER FUNCTION public.update_monthly_stats() OWNER TO maxcom;

SET default_tablespace = '';

SET default_with_oids = true;

--
-- Name: b_ips; Type: TABLE; Schema: public; Owner: linuxweb; Tablespace: 
--

CREATE TABLE b_ips (
    ip inet NOT NULL,
    mod_id integer NOT NULL,
    date timestamp with time zone NOT NULL,
    reason character varying(255),
    ban_date timestamp without time zone
);


--
-- Name: TABLE b_ips; Type: COMMENT; Schema: public; Owner: linuxweb
--

COMMENT ON TABLE b_ips IS 'banned ip list table';


SET default_with_oids = false;

--
-- Name: ban_info; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE ban_info (
    userid integer NOT NULL,
    bandate timestamp without time zone DEFAULT now() NOT NULL,
    reason text NOT NULL,
    ban_by integer NOT NULL
);


ALTER TABLE public.ban_info OWNER TO maxcom;

SET default_with_oids = true;

--
-- Name: comments; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE comments (
    id integer NOT NULL,
    topic integer NOT NULL,
    userid integer NOT NULL,
    title character varying(255) NOT NULL,
    postdate timestamp with time zone NOT NULL,
    replyto integer,
    deleted boolean DEFAULT false NOT NULL,
    postip inet,
    ua_id integer,
    topic_deleted boolean DEFAULT false NOT NULL
);


ALTER TABLE public.comments OWNER TO maxcom;

--
-- Name: del_info; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE del_info (
    msgid integer NOT NULL,
    delby integer NOT NULL,
    reason text,
    deldate timestamp without time zone
);


ALTER TABLE public.del_info OWNER TO maxcom;

SET default_with_oids = false;

--
-- Name: edit_info; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE edit_info (
    id integer NOT NULL,
    msgid integer NOT NULL,
    editor integer NOT NULL,
    oldmessage text,
    editdate timestamp without time zone DEFAULT now() NOT NULL,
    oldtitle text,
    oldtags text,
    oldlinktext text,
    oldurl text
);


ALTER TABLE public.edit_info OWNER TO maxcom;

--
-- Name: edit_info_id_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE edit_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.edit_info_id_seq OWNER TO maxcom;

--
-- Name: edit_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE edit_info_id_seq OWNED BY edit_info.id;


--
-- Name: edit_info_id_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('edit_info_id_seq', 3, true);


SET default_with_oids = true;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE groups (
    id integer NOT NULL,
    title character varying(255) NOT NULL,
    image character varying(255),
    section integer NOT NULL,
    stat1 integer DEFAULT 0 NOT NULL,
    stat2 integer DEFAULT 0 NOT NULL,
    stat3 integer DEFAULT 0 NOT NULL,
    stat4 integer DEFAULT 0 NOT NULL,
    restrict_topics integer,
    info text,
    restrict_comments integer DEFAULT (-9999) NOT NULL,
    longinfo text,
    resolvable boolean DEFAULT false NOT NULL,
    urlname text NOT NULL
);


ALTER TABLE public.groups OWNER TO maxcom;

SET default_with_oids = false;

--
-- Name: ignore_list; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE ignore_list (
    userid integer NOT NULL,
    ignored integer NOT NULL
);


ALTER TABLE public.ignore_list OWNER TO maxcom;

SET default_with_oids = true;

--
-- Name: users; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE users (
    id integer NOT NULL,
    name character varying(255),
    nick character varying(80) NOT NULL,
    passwd character varying(40),
    url character varying(255),
    email character varying(255),
    canmod boolean DEFAULT false NOT NULL,
    photo character varying(100),
    town character varying(100),
    candel boolean DEFAULT false NOT NULL,
    lostpwd timestamp with time zone DEFAULT '1970-01-01 03:00:00+03'::timestamp with time zone NOT NULL,
    blocked boolean,
    score integer,
    max_score integer,
    lastlogin timestamp without time zone,
    regdate timestamp without time zone,
    activated boolean DEFAULT false NOT NULL,
    corrector boolean DEFAULT false NOT NULL,
    userinfo text,
    unread_events integer DEFAULT 0 NOT NULL,
    new_email character varying(255)
);


ALTER TABLE public.users OWNER TO maxcom;

--
-- Name: jam_authorities; Type: VIEW; Schema: public; Owner: maxcom
--

CREATE VIEW jam_authorities AS
    (((((((SELECT users.nick AS username, 'ROLE_ADMIN' AS authority FROM users WHERE (users.canmod = true) UNION SELECT users.nick AS username, 'ROLE_SYSADMIN' AS authority FROM users WHERE (users.id = 1)) UNION SELECT users.nick AS username, 'ROLE_TRANSLATE' AS authority FROM users WHERE (users.canmod = true)) UNION SELECT users.nick AS username, 'ROLE_UPLOAD' AS authority FROM users WHERE (users.id = 1)) UNION SELECT users.nick AS username, 'ROLE_MOVE' AS authority FROM users WHERE (users.id = 1)) UNION SELECT users.nick AS username, 'ROLE_IMPORT' AS authority FROM users WHERE (users.id = 1)) UNION SELECT users.nick AS username, 'ROLE_EDIT_EXISTING' AS authority FROM users WHERE ((NOT users.blocked) AND (users.score >= 0))) UNION SELECT users.nick AS username, 'ROLE_EDIT_NEW' AS authority FROM users WHERE ((NOT users.blocked) AND (users.score >= 0))) UNION SELECT users.nick AS username, 'ROLE_VIEW' AS authority FROM users WHERE ((NOT users.blocked) AND (users.score >= 0));


ALTER TABLE public.jam_authorities OWNER TO maxcom;

SET default_with_oids = false;

--
-- Name: jam_category; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_category (
    child_topic_id integer NOT NULL,
    category_name character varying(200) NOT NULL,
    sort_key character varying(200)
);


ALTER TABLE public.jam_category OWNER TO maxcom;

--
-- Name: jam_configuration; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_configuration (
    config_key character varying(50) NOT NULL,
    config_value character varying(500) NOT NULL
);


ALTER TABLE public.jam_configuration OWNER TO maxcom;

--
-- Name: jam_file; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_file (
    file_id integer NOT NULL,
    virtual_wiki_id integer NOT NULL,
    file_name character varying(200) NOT NULL,
    delete_date timestamp without time zone,
    file_read_only integer DEFAULT 0 NOT NULL,
    file_admin_only integer DEFAULT 0 NOT NULL,
    file_url character varying(200) NOT NULL,
    mime_type character varying(100) NOT NULL,
    topic_id integer NOT NULL,
    file_size integer NOT NULL
);


ALTER TABLE public.jam_file OWNER TO maxcom;

--
-- Name: jam_file_pk_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_file_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_file_pk_seq OWNER TO maxcom;

--
-- Name: jam_file_pk_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_file_pk_seq OWNED BY jam_file.file_id;


--
-- Name: jam_file_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_file_pk_seq', 1, false);


--
-- Name: jam_file_version; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_file_version (
    file_version_id integer NOT NULL,
    file_id integer NOT NULL,
    upload_comment character varying(200),
    file_url character varying(200) NOT NULL,
    wiki_user_id integer,
    upload_date timestamp without time zone DEFAULT now() NOT NULL,
    mime_type character varying(100) NOT NULL,
    file_size integer NOT NULL,
    wiki_user_display character varying(100)
);


ALTER TABLE public.jam_file_version OWNER TO maxcom;

--
-- Name: jam_filev_pk_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_filev_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_filev_pk_seq OWNER TO maxcom;

--
-- Name: jam_filev_pk_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_filev_pk_seq OWNED BY jam_file_version.file_version_id;


--
-- Name: jam_filev_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_filev_pk_seq', 1, false);


--
-- Name: jam_group_members; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_group_members (
    id integer NOT NULL,
    username character varying(100) NOT NULL,
    group_id integer NOT NULL
);


ALTER TABLE public.jam_group_members OWNER TO maxcom;

--
-- Name: jam_gmemb_pk_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_gmemb_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_gmemb_pk_seq OWNER TO maxcom;

--
-- Name: jam_gmemb_pk_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_gmemb_pk_seq OWNED BY jam_group_members.id;


--
-- Name: jam_gmemb_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_gmemb_pk_seq', 1, false);


--
-- Name: jam_group; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_group (
    group_id integer NOT NULL,
    group_name character varying(30) NOT NULL,
    group_description character varying(200)
);


ALTER TABLE public.jam_group OWNER TO maxcom;

--
-- Name: jam_group_authorities; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_group_authorities (
    group_id integer NOT NULL,
    authority character varying(30) NOT NULL
);


ALTER TABLE public.jam_group_authorities OWNER TO maxcom;

--
-- Name: jam_group_members_id_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_group_members_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_group_members_id_seq OWNER TO maxcom;

--
-- Name: jam_group_members_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_group_members_id_seq OWNED BY jam_group_members.id;


--
-- Name: jam_group_members_id_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_group_members_id_seq', 1, false);


--
-- Name: jam_group_pk_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_group_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_group_pk_seq OWNER TO maxcom;

--
-- Name: jam_group_pk_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_group_pk_seq OWNED BY jam_group.group_id;


--
-- Name: jam_group_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_group_pk_seq', 2, true);


--
-- Name: jam_interwiki; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_interwiki (
    interwiki_prefix character varying(30) NOT NULL,
    interwiki_pattern character varying(200) NOT NULL,
    interwiki_type integer NOT NULL
);


ALTER TABLE public.jam_interwiki OWNER TO maxcom;


--
-- Name: jam_log; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_log (
    log_date timestamp without time zone DEFAULT now() NOT NULL,
    virtual_wiki_id integer NOT NULL,
    wiki_user_id integer,
    display_name character varying(200) NOT NULL,
    topic_id integer,
    topic_version_id integer,
    log_type integer NOT NULL,
    log_comment character varying(200),
    log_params character varying(500),
    log_sub_type integer
);


ALTER TABLE public.jam_log OWNER TO maxcom;

--
-- Name: jam_namespace; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_namespace (
    namespace_id integer NOT NULL,
    namespace character varying(200) NOT NULL,
    main_namespace_id integer
);


ALTER TABLE public.jam_namespace OWNER TO maxcom;

--
-- Name: jam_namespace_translation; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_namespace_translation (
    namespace_id integer NOT NULL,
    virtual_wiki_id integer NOT NULL,
    namespace character varying(200) NOT NULL
);


ALTER TABLE public.jam_namespace_translation OWNER TO maxcom;

--
-- Name: jam_recent_change; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_recent_change (
    topic_version_id integer,
    previous_topic_version_id integer,
    topic_id integer,
    topic_name character varying(200),
    change_date timestamp without time zone DEFAULT now() NOT NULL,
    change_comment character varying(200),
    wiki_user_id integer,
    display_name character varying(200) NOT NULL,
    edit_type integer,
    virtual_wiki_id integer,
    virtual_wiki_name character varying(100),
    characters_changed integer,
    log_type integer,
    log_params character varying(500),
    log_sub_type integer
);


ALTER TABLE public.jam_recent_change OWNER TO maxcom;

--
-- Name: jam_role; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_role (
    role_name character varying(30) NOT NULL,
    role_description character varying(200)
);


ALTER TABLE public.jam_role OWNER TO maxcom;

--
-- Name: jam_topic; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_topic (
    topic_id integer NOT NULL,
    virtual_wiki_id integer NOT NULL,
    topic_name character varying(200) NOT NULL,
    delete_date timestamp without time zone,
    topic_read_only integer DEFAULT 0 NOT NULL,
    topic_admin_only integer DEFAULT 0 NOT NULL,
    current_version_id integer,
    topic_type integer NOT NULL,
    redirect_to character varying(200),
    namespace_id integer DEFAULT 0 NOT NULL,
    page_name character varying(200),
    page_name_lower character varying(200)
);


ALTER TABLE public.jam_topic OWNER TO maxcom;

--
-- Name: jam_topic_links; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_topic_links (
    topic_id integer NOT NULL,
    link_topic_namespace_id integer DEFAULT 0 NOT NULL,
    link_topic_page_name character varying(200) NOT NULL
);


ALTER TABLE public.jam_topic_links OWNER TO maxcom;

--
-- Name: jam_topic_pk_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_topic_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_topic_pk_seq OWNER TO maxcom;

--
-- Name: jam_topic_pk_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_topic_pk_seq OWNED BY jam_topic.topic_id;


--
-- Name: jam_topic_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_topic_pk_seq', 8, true);


--
-- Name: jam_topic_version; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_topic_version (
    topic_version_id integer NOT NULL,
    topic_id integer NOT NULL,
    edit_comment character varying(200),
    version_content text,
    wiki_user_id integer,
    edit_date timestamp without time zone DEFAULT now() NOT NULL,
    edit_type integer NOT NULL,
    previous_topic_version_id integer,
    characters_changed integer,
    version_params character varying(500),
    wiki_user_display character varying(100)
);


ALTER TABLE public.jam_topic_version OWNER TO maxcom;

--
-- Name: jam_topic_ver_pk_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_topic_ver_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_topic_ver_pk_seq OWNER TO maxcom;

--
-- Name: jam_topic_ver_pk_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_topic_ver_pk_seq OWNED BY jam_topic_version.topic_version_id;


--
-- Name: jam_topic_ver_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_topic_ver_pk_seq', 6, true);


--
-- Name: jam_user_block; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_user_block (
    user_block_id integer NOT NULL,
    wiki_user_id integer,
    ip_address character varying(39),
    block_date timestamp without time zone DEFAULT now() NOT NULL,
    block_end_date timestamp without time zone,
    block_reason character varying(200),
    blocked_by_user_id integer NOT NULL,
    unblock_date timestamp without time zone,
    unblock_reason character varying(200),
    unblocked_by_user_id integer
);


ALTER TABLE public.jam_user_block OWNER TO maxcom;

--
-- Name: jam_user_block_user_block_id_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_user_block_user_block_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_user_block_user_block_id_seq OWNER TO maxcom;

--
-- Name: jam_user_block_user_block_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_user_block_user_block_id_seq OWNED BY jam_user_block.user_block_id;


--
-- Name: jam_user_block_user_block_id_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_user_block_user_block_id_seq', 1, false);


--
-- Name: jam_users; Type: VIEW; Schema: public; Owner: maxcom
--

CREATE VIEW jam_users AS
    SELECT users.nick AS username, users.passwd AS password, 1 AS enabled FROM users WHERE ((NOT users.blocked) AND (users.score >= 50));


ALTER TABLE public.jam_users OWNER TO maxcom;

--
-- Name: jam_virtual_wiki; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_virtual_wiki (
    virtual_wiki_id integer NOT NULL,
    virtual_wiki_name character varying(100) NOT NULL,
    default_topic_name character varying(200),
    create_date timestamp without time zone DEFAULT now() NOT NULL,
    logo_image_url character varying(200),
    site_name character varying(200),
    meta_description character varying(500)
);


ALTER TABLE public.jam_virtual_wiki OWNER TO maxcom;

--
-- Name: jam_vwiki_pk_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE jam_vwiki_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.jam_vwiki_pk_seq OWNER TO maxcom;

--
-- Name: jam_vwiki_pk_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE jam_vwiki_pk_seq OWNED BY jam_virtual_wiki.virtual_wiki_id;


--
-- Name: jam_vwiki_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('jam_vwiki_pk_seq', 2, true);


--
-- Name: jam_watchlist; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE jam_watchlist (
    wiki_user_id integer NOT NULL,
    topic_name character varying(200) NOT NULL,
    virtual_wiki_id integer NOT NULL
);


ALTER TABLE public.jam_watchlist OWNER TO maxcom;

--
-- Name: jam_wiki_user; Type: VIEW; Schema: public; Owner: maxcom
--

CREATE VIEW jam_wiki_user AS
    SELECT users.id AS wiki_user_id, users.nick AS login, users.name AS display_name, users.regdate AS create_date, users.lastlogin AS last_login_date, '127.0.0.1'::character varying(15) AS create_ip_address, '127.0.0.1'::character varying(15) AS last_login_ip_address, 'ru_RU'::character varying(8) AS default_locale, users.email, 'toolbar'::character varying(50) AS editor, ''::character varying(255) AS signature FROM users;


ALTER TABLE public.jam_wiki_user OWNER TO maxcom;

--
-- Name: memories; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE memories (
    id integer NOT NULL,
    userid integer NOT NULL,
    topic integer NOT NULL,
    add_date timestamp without time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.memories OWNER TO maxcom;

--
-- Name: memories_id_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE memories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.memories_id_seq OWNER TO maxcom;

--
-- Name: memories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE memories_id_seq OWNED BY memories.id;


--
-- Name: memories_id_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('memories_id_seq', 7, true);


SET default_with_oids = true;

--
-- Name: monthly_stats; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE monthly_stats (
    section integer,
    year integer NOT NULL,
    month integer NOT NULL,
    c integer NOT NULL,
    groupid integer
);


ALTER TABLE public.monthly_stats OWNER TO maxcom;

--
-- Name: msgbase; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE msgbase (
    id bigint NOT NULL,
    message text NOT NULL,
    bbcode boolean
);


ALTER TABLE public.msgbase OWNER TO maxcom;

--
-- Name: s_guid; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE s_guid
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 2147483647
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.s_guid OWNER TO maxcom;

--
-- Name: s_guid; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('s_guid', 19359, true);


--
-- Name: s_msg; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE s_msg
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 2147483647
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.s_msg OWNER TO maxcom;

--
-- Name: s_msg; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('s_msg', 1, false);


--
-- Name: s_msgid; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE s_msgid
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 2147483647
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.s_msgid OWNER TO maxcom;

--
-- Name: s_msgid; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('s_msgid', 1948655, true);


--
-- Name: s_uid; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE s_uid
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 2147483647
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.s_uid OWNER TO maxcom;

--
-- Name: s_uid; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('s_uid', 34577, true);


--
-- Name: sections; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE sections (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    moderate boolean NOT NULL,
    imagepost boolean NOT NULL,
    preformat boolean NOT NULL,
    linktext character varying(255),
    havelink boolean NOT NULL,
    expire interval NOT NULL,
    vote boolean DEFAULT false,
    add_info text
);


ALTER TABLE public.sections OWNER TO maxcom;

SET default_with_oids = false;

--
-- Name: tags; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE tags (
    msgid integer,
    tagid integer
);


ALTER TABLE public.tags OWNER TO maxcom;

--
-- Name: tags_values; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE tags_values (
    id integer NOT NULL,
    counter integer DEFAULT 0,
    value character varying(255) NOT NULL
);


ALTER TABLE public.tags_values OWNER TO maxcom;

--
-- Name: tags_values_id_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE tags_values_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.tags_values_id_seq OWNER TO maxcom;

--
-- Name: tags_values_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE tags_values_id_seq OWNED BY tags_values.id;


--
-- Name: tags_values_id_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('tags_values_id_seq', 45, true);


SET default_with_oids = true;

--
-- Name: topics; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE topics (
    id integer NOT NULL,
    groupid integer NOT NULL,
    userid integer NOT NULL,
    title character varying(255) NOT NULL,
    url character varying(255),
    moderate boolean DEFAULT false NOT NULL,
    postdate timestamp with time zone NOT NULL,
    linktext character varying(255),
    deleted boolean DEFAULT false NOT NULL,
    stat1 integer DEFAULT 0 NOT NULL,
    stat2 integer DEFAULT 0 NOT NULL,
    stat3 integer DEFAULT 0 NOT NULL,
    stat4 integer DEFAULT 0 NOT NULL,
    lastmod timestamp with time zone,
    commitby integer,
    notop boolean,
    commitdate timestamp without time zone,
    postscore integer,
    postip inet,
    sticky boolean DEFAULT false NOT NULL,
    ua_id integer,
    resolved boolean,
    minor boolean DEFAULT false NOT NULL
);


ALTER TABLE public.topics OWNER TO maxcom;

SET default_with_oids = false;

--
-- Name: user_agents; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE user_agents (
    id integer NOT NULL,
    name character varying(512) DEFAULT ''::character varying
);


ALTER TABLE public.user_agents OWNER TO maxcom;

--
-- Name: user_agents_id_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE user_agents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.user_agents_id_seq OWNER TO maxcom;

--
-- Name: user_agents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE user_agents_id_seq OWNED BY user_agents.id;


--
-- Name: user_agents_id_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('user_agents_id_seq', 20, true);


--
-- Name: user_events; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE user_events (
    userid integer NOT NULL,
    type event_type NOT NULL,
    private boolean NOT NULL,
    event_date timestamp without time zone DEFAULT now() NOT NULL,
    message_id integer,
    comment_id integer,
    message text,
    unread boolean DEFAULT true NOT NULL,
    id integer NOT NULL
);


ALTER TABLE public.user_events OWNER TO maxcom;

--
-- Name: user_events_id_seq; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE user_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.user_events_id_seq OWNER TO maxcom;

--
-- Name: user_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: maxcom
--

ALTER SEQUENCE user_events_id_seq OWNED BY user_events.id;


--
-- Name: user_events_id_seq; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('user_events_id_seq', 10, true);


--
-- Name: vote_id; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE vote_id
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 2147483647
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.vote_id OWNER TO maxcom;

--
-- Name: vote_id; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('vote_id', 279, true);


--
-- Name: vote_users; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE vote_users (
    vote integer NOT NULL,
    userid integer NOT NULL
);


ALTER TABLE public.vote_users OWNER TO maxcom;

SET default_with_oids = true;

--
-- Name: votenames; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE votenames (
    id integer NOT NULL,
    topic integer DEFAULT 0 NOT NULL,
    multiselect boolean DEFAULT false NOT NULL
);


ALTER TABLE public.votenames OWNER TO maxcom;

--
-- Name: votes; Type: TABLE; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE TABLE votes (
    id integer NOT NULL,
    vote integer NOT NULL,
    label text NOT NULL,
    votes integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.votes OWNER TO maxcom;

--
-- Name: votes_id; Type: SEQUENCE; Schema: public; Owner: maxcom
--

CREATE SEQUENCE votes_id
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 2147483647
    NO MINVALUE
    CACHE 1;


ALTER TABLE public.votes_id OWNER TO maxcom;

--
-- Name: votes_id; Type: SEQUENCE SET; Schema: public; Owner: maxcom
--

SELECT pg_catalog.setval('votes_id', 1877, true);


--
-- Name: wiki_recent_change; Type: VIEW; Schema: public; Owner: maxcom
--

CREATE VIEW wiki_recent_change AS
    SELECT jam_recent_change.wiki_user_id, max(jam_recent_change.topic_version_id) AS topic_version_id, min(jam_recent_change.previous_topic_version_id) AS previos_topic_version_id, jam_recent_change.topic_name, max(jam_recent_change.change_date) AS change_date, sum(jam_recent_change.characters_changed) AS characters_changed FROM jam_recent_change WHERE ((jam_recent_change.change_date > (now() - '48:00:00'::interval)) AND (jam_recent_change.topic_id IS NOT NULL)) GROUP BY jam_recent_change.wiki_user_id, jam_recent_change.topic_name;


ALTER TABLE public.wiki_recent_change OWNER TO maxcom;

--
-- Name: id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE edit_info ALTER COLUMN id SET DEFAULT nextval('edit_info_id_seq'::regclass);


--
-- Name: file_id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_file ALTER COLUMN file_id SET DEFAULT nextval('jam_file_pk_seq'::regclass);


--
-- Name: file_version_id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_file_version ALTER COLUMN file_version_id SET DEFAULT nextval('jam_filev_pk_seq'::regclass);


--
-- Name: group_id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_group ALTER COLUMN group_id SET DEFAULT nextval('jam_group_pk_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_group_members ALTER COLUMN id SET DEFAULT nextval('jam_gmemb_pk_seq'::regclass);


--
-- Name: topic_id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_topic ALTER COLUMN topic_id SET DEFAULT nextval('jam_topic_pk_seq'::regclass);


--
-- Name: topic_version_id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_topic_version ALTER COLUMN topic_version_id SET DEFAULT nextval('jam_topic_ver_pk_seq'::regclass);


--
-- Name: user_block_id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_user_block ALTER COLUMN user_block_id SET DEFAULT nextval('jam_user_block_user_block_id_seq'::regclass);


--
-- Name: virtual_wiki_id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE jam_virtual_wiki ALTER COLUMN virtual_wiki_id SET DEFAULT nextval('jam_vwiki_pk_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE memories ALTER COLUMN id SET DEFAULT nextval('memories_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE tags_values ALTER COLUMN id SET DEFAULT nextval('tags_values_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE user_agents ALTER COLUMN id SET DEFAULT nextval('user_agents_id_seq'::regclass);


--
-- Name: id; Type: DEFAULT; Schema: public; Owner: maxcom
--

ALTER TABLE user_events ALTER COLUMN id SET DEFAULT nextval('user_events_id_seq'::regclass);
