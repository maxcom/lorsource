--
-- Name: ban_info_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY ban_info
    ADD CONSTRAINT ban_info_pkey PRIMARY KEY (userid);


--
-- Name: comments_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);

ALTER TABLE comments CLUSTER ON comments_pkey;


--
-- Name: del_info_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY del_info
    ADD CONSTRAINT del_info_pkey PRIMARY KEY (msgid);


--
-- Name: edit_info_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY edit_info
    ADD CONSTRAINT edit_info_pkey PRIMARY KEY (id);


--
-- Name: groups_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);


--
-- Name: ignore_list_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY ignore_list
    ADD CONSTRAINT ignore_list_pkey PRIMARY KEY (userid, ignored);


--
-- Name: jam_p_category; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_category
    ADD CONSTRAINT jam_p_category PRIMARY KEY (child_topic_id, category_name);


--
-- Name: jam_p_config; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_configuration
    ADD CONSTRAINT jam_p_config PRIMARY KEY (config_key);


--
-- Name: jam_p_file; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_file
    ADD CONSTRAINT jam_p_file PRIMARY KEY (file_id);


--
-- Name: jam_p_file_ver; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_file_version
    ADD CONSTRAINT jam_p_file_ver PRIMARY KEY (file_version_id);


--
-- Name: jam_p_gmemb; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_group_members
    ADD CONSTRAINT jam_p_gmemb PRIMARY KEY (id);


--
-- Name: jam_p_group; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_group
    ADD CONSTRAINT jam_p_group PRIMARY KEY (group_id);


--
-- Name: jam_p_interw; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_interwiki
    ADD CONSTRAINT jam_p_interw PRIMARY KEY (interwiki_prefix);


--
-- Name: jam_p_namesp; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_namespace
    ADD CONSTRAINT jam_p_namesp PRIMARY KEY (namespace_id);


--
-- Name: jam_p_namesptr; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_namespace_translation
    ADD CONSTRAINT jam_p_namesptr PRIMARY KEY (namespace_id, virtual_wiki_id);


--
-- Name: jam_p_role; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_role
    ADD CONSTRAINT jam_p_role PRIMARY KEY (role_name);


--
-- Name: jam_p_topic; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_topic
    ADD CONSTRAINT jam_p_topic PRIMARY KEY (topic_id);


--
-- Name: jam_p_topic_links; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_topic_links
    ADD CONSTRAINT jam_p_topic_links PRIMARY KEY (topic_id, link_topic_namespace_id, link_topic_page_name);


--
-- Name: jam_p_topic_ver; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_topic_version
    ADD CONSTRAINT jam_p_topic_ver PRIMARY KEY (topic_version_id);


--
-- Name: jam_p_vwiki; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_virtual_wiki
    ADD CONSTRAINT jam_p_vwiki PRIMARY KEY (virtual_wiki_id);


--
-- Name: jam_p_watchlist; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_watchlist
    ADD CONSTRAINT jam_p_watchlist PRIMARY KEY (wiki_user_id, topic_name, virtual_wiki_id);


--
-- Name: jam_u_file_topic; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_file
    ADD CONSTRAINT jam_u_file_topic UNIQUE (virtual_wiki_id, topic_id);


--
-- Name: jam_u_file_url; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_file
    ADD CONSTRAINT jam_u_file_url UNIQUE (file_url);


--
-- Name: jam_u_filev_url; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_file_version
    ADD CONSTRAINT jam_u_filev_url UNIQUE (file_url);


--
-- Name: jam_u_gauth; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_group_authorities
    ADD CONSTRAINT jam_u_gauth UNIQUE (group_id, authority);


--
-- Name: jam_u_group_name; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_group
    ADD CONSTRAINT jam_u_group_name UNIQUE (group_name);


--
-- Name: jam_u_namesp_namesp; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_namespace
    ADD CONSTRAINT jam_u_namesp_namesp UNIQUE (namespace);


--
-- Name: jam_u_namesptr_namesp; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_namespace_translation
    ADD CONSTRAINT jam_u_namesptr_namesp UNIQUE (virtual_wiki_id, namespace);


--
-- Name: jam_u_topic_name; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_topic
    ADD CONSTRAINT jam_u_topic_name UNIQUE (topic_name, virtual_wiki_id, delete_date);


--
-- Name: jam_u_ublock; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_user_block
    ADD CONSTRAINT jam_u_ublock PRIMARY KEY (user_block_id);


--
-- Name: jam_u_vwiki_name; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY jam_virtual_wiki
    ADD CONSTRAINT jam_u_vwiki_name UNIQUE (virtual_wiki_name);


--
-- Name: memories_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY memories
    ADD CONSTRAINT memories_pkey PRIMARY KEY (id);


--
-- Name: msgbase_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY msgbase
    ADD CONSTRAINT msgbase_pkey PRIMARY KEY (id);


--
-- Name: sections_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY sections
    ADD CONSTRAINT sections_pkey PRIMARY KEY (id);


--
-- Name: tags_msgid_key; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY tags
    ADD CONSTRAINT tags_msgid_key UNIQUE (msgid, tagid);


--
-- Name: tags_values_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY tags_values
    ADD CONSTRAINT tags_values_pkey PRIMARY KEY (id);


--
-- Name: tags_values_value_key; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY tags_values
    ADD CONSTRAINT tags_values_value_key UNIQUE (value);


--
-- Name: user_agents_name_key; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY user_agents
    ADD CONSTRAINT user_agents_name_key UNIQUE (name);


--
-- Name: user_agents_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY user_agents
    ADD CONSTRAINT user_agents_pkey PRIMARY KEY (id);


--
-- Name: user_events_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY user_events
    ADD CONSTRAINT user_events_pkey PRIMARY KEY (id);


--
-- Name: users_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: votenames_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY votenames
    ADD CONSTRAINT votenames_pkey PRIMARY KEY (id);


--
-- Name: votes_pkey; Type: CONSTRAINT; Schema: public; Owner: maxcom; Tablespace: 
--

ALTER TABLE ONLY votes
    ADD CONSTRAINT votes_pkey PRIMARY KEY (id);


--
-- Name: bips_ip; Type: INDEX; Schema: public; Owner: linuxweb; Tablespace: 
--

CREATE UNIQUE INDEX bips_ip ON b_ips USING btree (ip);


--
-- Name: comment_author; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX comment_author ON comments USING btree (userid);


--
-- Name: comment_authordate; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX comment_authordate ON comments USING btree (userid, postdate);


--
-- Name: comment_postdate; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX comment_postdate ON comments USING btree (postdate);


--
-- Name: comment_reply2; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX comment_reply2 ON comments USING btree (replyto) WHERE (replyto IS NOT NULL);


--
-- Name: comment_topic; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX comment_topic ON comments USING btree (topic);


--
-- Name: comment_tracker; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX comment_tracker ON comments USING btree (topic, postdate DESC) WHERE (NOT deleted);


--
-- Name: comments_postip; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX comments_postip ON comments USING btree (postip);


--
-- Name: commit_order2; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX commit_order2 ON topics USING btree (commitdate DESC) WHERE (commitdate IS NOT NULL);


--
-- Name: commit_order3; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX commit_order3 ON topics USING btree (groupid, commitdate DESC) WHERE (commitdate IS NOT NULL);


--
-- Name: del_info_date; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX del_info_date ON del_info USING btree (deldate DESC) WHERE (deldate IS NOT NULL);


--
-- Name: del_info_delby; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX del_info_delby ON del_info USING btree (delby);


--
-- Name: edit_info_msgid; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX edit_info_msgid ON edit_info USING btree (msgid);


--
-- Name: group_section; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX group_section ON groups USING btree (section);


--
-- Name: groups_urlname; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX groups_urlname ON groups USING btree (urlname);


--
-- Name: groups_urlname_u; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE UNIQUE INDEX groups_urlname_u ON groups USING btree (urlname, section);


--
-- Name: i_nick; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE UNIQUE INDEX i_nick ON users USING btree (nick);


--
-- Name: i_votes_vote; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX i_votes_vote ON votes USING btree (vote);


--
-- Name: jam_i_category_cti; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_category_cti ON jam_category USING btree (child_topic_id);


--
-- Name: jam_i_topic_cver; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topic_cver ON jam_topic USING btree (current_version_id);


--
-- Name: jam_i_topic_nmsp; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topic_nmsp ON jam_topic USING btree (namespace_id);


--
-- Name: jam_i_topic_pgnm; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topic_pgnm ON jam_topic USING btree (page_name);


--
-- Name: jam_i_topic_pgnml; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topic_pgnml ON jam_topic USING btree (page_name_lower);


--
-- Name: jam_i_topic_vwiki; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topic_vwiki ON jam_topic USING btree (virtual_wiki_id);


--
-- Name: jam_i_topicv_prv; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topicv_prv ON jam_topic_version USING btree (previous_topic_version_id);


--
-- Name: jam_i_topicv_topic; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topicv_topic ON jam_topic_version USING btree (topic_id);


--
-- Name: jam_i_topicv_udisp; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topicv_udisp ON jam_topic_version USING btree (wiki_user_display);


--
-- Name: jam_i_topicv_uid; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX jam_i_topicv_uid ON jam_topic_version USING btree (wiki_user_id);


--
-- Name: memories_un; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE UNIQUE INDEX memories_un ON memories USING btree (userid, topic);


--
-- Name: tags_msgid; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX tags_msgid ON tags USING btree (msgid);


--
-- Name: tags_tagid; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX tags_tagid ON tags USING btree (tagid);


--
-- Name: topic_author; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX topic_author ON topics USING btree (userid);


--
-- Name: topic_deleted; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX topic_deleted ON topics USING btree (id) WHERE deleted;


--
-- Name: topic_group; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX topic_group ON topics USING btree (groupid);


--
-- Name: topic_postip; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX topic_postip ON topics USING btree (postip);


--
-- Name: topics_date; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX topics_date ON topics USING btree (postdate);


--
-- Name: topics_lastmod; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX topics_lastmod ON topics USING btree (lastmod DESC) WHERE (NOT deleted);


--
-- Name: topics_pkey; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE UNIQUE INDEX topics_pkey ON topics USING btree (id);


--
-- Name: user_events_idx; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE INDEX user_events_idx ON user_events USING btree (userid, event_date);


--
-- Name: vote_users_idx; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE UNIQUE INDEX vote_users_idx ON vote_users USING btree (vote, userid);


--
-- Name: votenames_topic_key; Type: INDEX; Schema: public; Owner: maxcom; Tablespace: 
--

CREATE UNIQUE INDEX votenames_topic_key ON votenames USING btree (topic);


--
-- Name: check_replyto; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER check_replyto
    BEFORE INSERT OR UPDATE ON comments
    FOR EACH ROW
    EXECUTE PROCEDURE check_replyto();


--
-- Name: comins_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER comins_t
    AFTER INSERT ON comments
    FOR EACH ROW
    EXECUTE PROCEDURE comins();


--
-- Name: event_comment_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER event_comment_t
    AFTER INSERT ON comments
    FOR EACH ROW
    EXECUTE PROCEDURE event_comment();


--
-- Name: event_delete_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER event_delete_t
    AFTER INSERT ON del_info
    FOR EACH ROW
    EXECUTE PROCEDURE event_delete();


--
-- Name: msgdel_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER msgdel_t
    AFTER INSERT ON del_info
    FOR EACH ROW
    EXECUTE PROCEDURE msgdel();


--
-- Name: msgedit_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER msgedit_t
    AFTER INSERT ON edit_info
    FOR EACH ROW
    EXECUTE PROCEDURE msgedit();


--
-- Name: msgundel_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER msgundel_t
    AFTER DELETE ON del_info
    FOR EACH ROW
    EXECUTE PROCEDURE msgundel();


--
-- Name: new_event_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER new_event_t
    AFTER INSERT ON user_events
    FOR EACH ROW
    EXECUTE PROCEDURE new_event();


--
-- Name: topins_t; Type: TRIGGER; Schema: public; Owner: maxcom
--

CREATE TRIGGER topins_t
    AFTER INSERT ON topics
    FOR EACH ROW
    EXECUTE PROCEDURE topins();


--
-- Name: ban_info_ban_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY ban_info
    ADD CONSTRAINT ban_info_ban_by_fkey FOREIGN KEY (ban_by) REFERENCES users(id);


--
-- Name: ban_info_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY ban_info
    ADD CONSTRAINT ban_info_userid_fkey FOREIGN KEY (userid) REFERENCES users(id);


--
-- Name: comments_replyto_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY comments
    ADD CONSTRAINT comments_replyto_fkey FOREIGN KEY (replyto) REFERENCES comments(id);


--
-- Name: comments_topic_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY comments
    ADD CONSTRAINT comments_topic_fkey FOREIGN KEY (topic) REFERENCES topics(id);


--
-- Name: comments_ua_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY comments
    ADD CONSTRAINT comments_ua_id_fkey FOREIGN KEY (ua_id) REFERENCES user_agents(id);


--
-- Name: comments_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY comments
    ADD CONSTRAINT comments_userid_fkey FOREIGN KEY (userid) REFERENCES users(id);


--
-- Name: del_info_delby_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY del_info
    ADD CONSTRAINT del_info_delby_fkey FOREIGN KEY (delby) REFERENCES users(id);


--
-- Name: edit_info_editor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY edit_info
    ADD CONSTRAINT edit_info_editor_fkey FOREIGN KEY (editor) REFERENCES users(id);


--
-- Name: edit_info_msgid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY edit_info
    ADD CONSTRAINT edit_info_msgid_fkey FOREIGN KEY (msgid) REFERENCES msgbase(id);


--
-- Name: groups_section_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY groups
    ADD CONSTRAINT groups_section_fkey FOREIGN KEY (section) REFERENCES sections(id);


--
-- Name: ignore_list_ignored_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY ignore_list
    ADD CONSTRAINT ignore_list_ignored_fkey FOREIGN KEY (ignored) REFERENCES users(id);


--
-- Name: ignore_list_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY ignore_list
    ADD CONSTRAINT ignore_list_userid_fkey FOREIGN KEY (userid) REFERENCES users(id);


--
-- Name: jam_f_cat_child_id; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_category
    ADD CONSTRAINT jam_f_cat_child_id FOREIGN KEY (child_topic_id) REFERENCES jam_topic(topic_id);


--
-- Name: jam_f_file_topic; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_file
    ADD CONSTRAINT jam_f_file_topic FOREIGN KEY (topic_id) REFERENCES jam_topic(topic_id);


--
-- Name: jam_f_file_vwiki; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_file
    ADD CONSTRAINT jam_f_file_vwiki FOREIGN KEY (virtual_wiki_id) REFERENCES jam_virtual_wiki(virtual_wiki_id);


--
-- Name: jam_f_filev_file; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_file_version
    ADD CONSTRAINT jam_f_filev_file FOREIGN KEY (file_id) REFERENCES jam_file(file_id);


--
-- Name: jam_f_gauth_authority; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_group_authorities
    ADD CONSTRAINT jam_f_gauth_authority FOREIGN KEY (authority) REFERENCES jam_role(role_name);


--
-- Name: jam_f_gauth_group; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_group_authorities
    ADD CONSTRAINT jam_f_gauth_group FOREIGN KEY (group_id) REFERENCES jam_group(group_id);


--
-- Name: jam_f_gmemb_group; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_group_members
    ADD CONSTRAINT jam_f_gmemb_group FOREIGN KEY (group_id) REFERENCES jam_group(group_id);


--
-- Name: jam_f_log_topic; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_log
    ADD CONSTRAINT jam_f_log_topic FOREIGN KEY (topic_id) REFERENCES jam_topic(topic_id);


--
-- Name: jam_f_log_topic_ver; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_log
    ADD CONSTRAINT jam_f_log_topic_ver FOREIGN KEY (topic_version_id) REFERENCES jam_topic_version(topic_version_id);


--
-- Name: jam_f_log_vwiki; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_log
    ADD CONSTRAINT jam_f_log_vwiki FOREIGN KEY (virtual_wiki_id) REFERENCES jam_virtual_wiki(virtual_wiki_id);


--
-- Name: jam_f_namesp_namesp; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_namespace
    ADD CONSTRAINT jam_f_namesp_namesp FOREIGN KEY (main_namespace_id) REFERENCES jam_namespace(namespace_id);


--
-- Name: jam_f_namesptr_namesp; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_namespace_translation
    ADD CONSTRAINT jam_f_namesptr_namesp FOREIGN KEY (namespace_id) REFERENCES jam_namespace(namespace_id);


--
-- Name: jam_f_namesptr_vwiki; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_namespace_translation
    ADD CONSTRAINT jam_f_namesptr_vwiki FOREIGN KEY (virtual_wiki_id) REFERENCES jam_virtual_wiki(virtual_wiki_id);


--
-- Name: jam_f_rc_p_topic_v; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_recent_change
    ADD CONSTRAINT jam_f_rc_p_topic_v FOREIGN KEY (previous_topic_version_id) REFERENCES jam_topic_version(topic_version_id);


--
-- Name: jam_f_rc_topic; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_recent_change
    ADD CONSTRAINT jam_f_rc_topic FOREIGN KEY (topic_id) REFERENCES jam_topic(topic_id);


--
-- Name: jam_f_rc_topic_ver; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_recent_change
    ADD CONSTRAINT jam_f_rc_topic_ver FOREIGN KEY (topic_version_id) REFERENCES jam_topic_version(topic_version_id);


--
-- Name: jam_f_rc_vwiki; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_recent_change
    ADD CONSTRAINT jam_f_rc_vwiki FOREIGN KEY (virtual_wiki_id) REFERENCES jam_virtual_wiki(virtual_wiki_id);


--
-- Name: jam_f_tlink_namesp; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_topic_links
    ADD CONSTRAINT jam_f_tlink_namesp FOREIGN KEY (link_topic_namespace_id) REFERENCES jam_namespace(namespace_id);


--
-- Name: jam_f_topic_namesp; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_topic
    ADD CONSTRAINT jam_f_topic_namesp FOREIGN KEY (namespace_id) REFERENCES jam_namespace(namespace_id);


--
-- Name: jam_f_topic_topicv; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_topic
    ADD CONSTRAINT jam_f_topic_topicv FOREIGN KEY (current_version_id) REFERENCES jam_topic_version(topic_version_id);


--
-- Name: jam_f_topic_vwiki; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_topic
    ADD CONSTRAINT jam_f_topic_vwiki FOREIGN KEY (virtual_wiki_id) REFERENCES jam_virtual_wiki(virtual_wiki_id);


--
-- Name: jam_f_topicv_pver; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_topic_version
    ADD CONSTRAINT jam_f_topicv_pver FOREIGN KEY (previous_topic_version_id) REFERENCES jam_topic_version(topic_version_id);


--
-- Name: jam_f_topicv_topic; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_topic_version
    ADD CONSTRAINT jam_f_topicv_topic FOREIGN KEY (topic_id) REFERENCES jam_topic(topic_id);


--
-- Name: jam_f_wlist_vwiki; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY jam_watchlist
    ADD CONSTRAINT jam_f_wlist_vwiki FOREIGN KEY (virtual_wiki_id) REFERENCES jam_virtual_wiki(virtual_wiki_id);


--
-- Name: memories_topic_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY memories
    ADD CONSTRAINT memories_topic_fkey FOREIGN KEY (topic) REFERENCES topics(id);


--
-- Name: memories_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY memories
    ADD CONSTRAINT memories_userid_fkey FOREIGN KEY (userid) REFERENCES users(id);


--
-- Name: monthly_stats_section_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY monthly_stats
    ADD CONSTRAINT monthly_stats_section_fkey FOREIGN KEY (section) REFERENCES sections(id);


--
-- Name: tags_msgid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY tags
    ADD CONSTRAINT tags_msgid_fkey FOREIGN KEY (msgid) REFERENCES topics(id);


--
-- Name: tags_tagid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY tags
    ADD CONSTRAINT tags_tagid_fkey FOREIGN KEY (tagid) REFERENCES tags_values(id);


--
-- Name: topics_commitby_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY topics
    ADD CONSTRAINT topics_commitby_fkey FOREIGN KEY (commitby) REFERENCES users(id);


--
-- Name: topics_groupid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY topics
    ADD CONSTRAINT topics_groupid_fkey FOREIGN KEY (groupid) REFERENCES groups(id);


--
-- Name: topics_ua_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY topics
    ADD CONSTRAINT topics_ua_id_fkey FOREIGN KEY (ua_id) REFERENCES user_agents(id);


--
-- Name: topics_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY topics
    ADD CONSTRAINT topics_userid_fkey FOREIGN KEY (userid) REFERENCES users(id);


--
-- Name: user_events_comment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY user_events
    ADD CONSTRAINT user_events_comment_id_fkey FOREIGN KEY (comment_id) REFERENCES comments(id);


--
-- Name: user_events_message_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY user_events
    ADD CONSTRAINT user_events_message_id_fkey FOREIGN KEY (message_id) REFERENCES topics(id);


--
-- Name: user_events_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY user_events
    ADD CONSTRAINT user_events_userid_fkey FOREIGN KEY (userid) REFERENCES users(id);


--
-- Name: vote_users_userid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY vote_users
    ADD CONSTRAINT vote_users_userid_fkey FOREIGN KEY (userid) REFERENCES users(id);


--
-- Name: vote_users_vote_fkey; Type: FK CONSTRAINT; Schema: public; Owner: maxcom
--

ALTER TABLE ONLY vote_users
    ADD CONSTRAINT vote_users_vote_fkey FOREIGN KEY (vote) REFERENCES votenames(id);


--
-- Name: public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE ALL ON SCHEMA public FROM PUBLIC;
REVOKE ALL ON SCHEMA public FROM postgres;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- Name: b_ips; Type: ACL; Schema: public; Owner: linuxweb
--

REVOKE ALL ON TABLE b_ips FROM PUBLIC;
REVOKE ALL ON TABLE b_ips FROM linuxweb;
GRANT ALL ON TABLE b_ips TO linuxweb;
GRANT ALL ON TABLE b_ips TO maxcom;
-- GRANT SELECT ON TABLE b_ips TO maxcom;


--
-- Name: ban_info; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE ban_info FROM PUBLIC;
REVOKE ALL ON TABLE ban_info FROM maxcom;
GRANT ALL ON TABLE ban_info TO maxcom;
GRANT ALL ON TABLE ban_info TO linuxweb;


--
-- Name: comments; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE comments FROM PUBLIC;
REVOKE ALL ON TABLE comments FROM maxcom;
GRANT ALL ON TABLE comments TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE comments TO linuxweb;


--
-- Name: del_info; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE del_info FROM PUBLIC;
REVOKE ALL ON TABLE del_info FROM maxcom;
GRANT ALL ON TABLE del_info TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE del_info TO linuxweb;


--
-- Name: edit_info; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE edit_info FROM PUBLIC;
REVOKE ALL ON TABLE edit_info FROM maxcom;
GRANT ALL ON TABLE edit_info TO maxcom;
GRANT SELECT,INSERT ON TABLE edit_info TO linuxweb;


--
-- Name: edit_info_id_seq; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE edit_info_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE edit_info_id_seq FROM maxcom;
GRANT ALL ON SEQUENCE edit_info_id_seq TO maxcom;
GRANT SELECT,UPDATE ON SEQUENCE edit_info_id_seq TO linuxweb;


--
-- Name: groups; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE groups FROM PUBLIC;
REVOKE ALL ON TABLE groups FROM maxcom;
GRANT ALL ON TABLE groups TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE groups TO linuxweb;


--
-- Name: ignore_list; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE ignore_list FROM PUBLIC;
REVOKE ALL ON TABLE ignore_list FROM maxcom;
GRANT ALL ON TABLE ignore_list TO maxcom;
GRANT ALL ON TABLE ignore_list TO linuxweb;


--
-- Name: users; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE users FROM PUBLIC;
REVOKE ALL ON TABLE users FROM maxcom;
GRANT ALL ON TABLE users TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE users TO linuxweb;


--
-- Name: jam_authorities; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_authorities FROM PUBLIC;
REVOKE ALL ON TABLE jam_authorities FROM maxcom;
GRANT ALL ON TABLE jam_authorities TO maxcom;
GRANT SELECT ON TABLE jam_authorities TO jamwiki;


--
-- Name: jam_category; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_category FROM PUBLIC;
REVOKE ALL ON TABLE jam_category FROM maxcom;
GRANT ALL ON TABLE jam_category TO maxcom;
GRANT ALL ON TABLE jam_category TO jamwiki;


--
-- Name: jam_configuration; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_configuration FROM PUBLIC;
REVOKE ALL ON TABLE jam_configuration FROM maxcom;
GRANT ALL ON TABLE jam_configuration TO maxcom;
GRANT ALL ON TABLE jam_configuration TO jamwiki;


--
-- Name: jam_file; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_file FROM PUBLIC;
REVOKE ALL ON TABLE jam_file FROM maxcom;
GRANT ALL ON TABLE jam_file TO maxcom;
GRANT ALL ON TABLE jam_file TO jamwiki;


--
-- Name: jam_file_version; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_file_version FROM PUBLIC;
REVOKE ALL ON TABLE jam_file_version FROM maxcom;
GRANT ALL ON TABLE jam_file_version TO maxcom;
GRANT ALL ON TABLE jam_file_version TO jamwiki;


--
-- Name: jam_group; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_group FROM PUBLIC;
REVOKE ALL ON TABLE jam_group FROM maxcom;
GRANT ALL ON TABLE jam_group TO maxcom;
GRANT ALL ON TABLE jam_group TO jamwiki;


--
-- Name: jam_group_authorities; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_group_authorities FROM PUBLIC;
REVOKE ALL ON TABLE jam_group_authorities FROM maxcom;
GRANT ALL ON TABLE jam_group_authorities TO maxcom;
GRANT ALL ON TABLE jam_group_authorities TO jamwiki;


--
-- Name: jam_interwiki; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_interwiki FROM PUBLIC;
REVOKE ALL ON TABLE jam_interwiki FROM maxcom;
GRANT ALL ON TABLE jam_interwiki TO maxcom;
GRANT ALL ON TABLE jam_interwiki TO jamwiki;


--
-- Name: jam_log; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_log FROM PUBLIC;
REVOKE ALL ON TABLE jam_log FROM maxcom;
GRANT ALL ON TABLE jam_log TO maxcom;
GRANT ALL ON TABLE jam_log TO jamwiki;


--
-- Name: jam_namespace; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_namespace FROM PUBLIC;
REVOKE ALL ON TABLE jam_namespace FROM maxcom;
GRANT ALL ON TABLE jam_namespace TO maxcom;
GRANT ALL ON TABLE jam_namespace TO jamwiki;


--
-- Name: jam_namespace_translation; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_namespace_translation FROM PUBLIC;
REVOKE ALL ON TABLE jam_namespace_translation FROM maxcom;
GRANT ALL ON TABLE jam_namespace_translation TO maxcom;
GRANT ALL ON TABLE jam_namespace_translation TO jamwiki;


--
-- Name: jam_recent_change; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_recent_change FROM PUBLIC;
REVOKE ALL ON TABLE jam_recent_change FROM maxcom;
GRANT ALL ON TABLE jam_recent_change TO maxcom;
GRANT ALL ON TABLE jam_recent_change TO jamwiki;
GRANT SELECT ON TABLE jam_recent_change TO linuxweb;


--
-- Name: jam_role; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_role FROM PUBLIC;
REVOKE ALL ON TABLE jam_role FROM maxcom;
GRANT ALL ON TABLE jam_role TO maxcom;
GRANT ALL ON TABLE jam_role TO jamwiki;


--
-- Name: jam_topic; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_topic FROM PUBLIC;
REVOKE ALL ON TABLE jam_topic FROM maxcom;
GRANT ALL ON TABLE jam_topic TO maxcom;
GRANT ALL ON TABLE jam_topic TO jamwiki;


--
-- Name: jam_topic_links; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_topic_links FROM PUBLIC;
REVOKE ALL ON TABLE jam_topic_links FROM maxcom;
GRANT ALL ON TABLE jam_topic_links TO maxcom;
GRANT ALL ON TABLE jam_topic_links TO jamwiki;


--
-- Name: jam_topic_pk_seq; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE jam_topic_pk_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE jam_topic_pk_seq FROM maxcom;
GRANT ALL ON SEQUENCE jam_topic_pk_seq TO maxcom;
GRANT ALL ON SEQUENCE jam_topic_pk_seq TO jamwiki;


--
-- Name: jam_topic_version; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_topic_version FROM PUBLIC;
REVOKE ALL ON TABLE jam_topic_version FROM maxcom;
GRANT ALL ON TABLE jam_topic_version TO maxcom;
GRANT ALL ON TABLE jam_topic_version TO jamwiki;
GRANT ALL ON TABLE jam_topic_version TO linuxweb;


--
-- Name: jam_topic_ver_pk_seq; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE jam_topic_ver_pk_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE jam_topic_ver_pk_seq FROM maxcom;
GRANT ALL ON SEQUENCE jam_topic_ver_pk_seq TO maxcom;
GRANT ALL ON SEQUENCE jam_topic_ver_pk_seq TO jamwiki;


--
-- Name: jam_user_block; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_user_block FROM PUBLIC;
REVOKE ALL ON TABLE jam_user_block FROM maxcom;
GRANT ALL ON TABLE jam_user_block TO maxcom;
GRANT ALL ON TABLE jam_user_block TO jamwiki;


--
-- Name: jam_users; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_users FROM PUBLIC;
REVOKE ALL ON TABLE jam_users FROM maxcom;
GRANT ALL ON TABLE jam_users TO maxcom;
GRANT SELECT ON TABLE jam_users TO jamwiki;


--
-- Name: jam_virtual_wiki; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_virtual_wiki FROM PUBLIC;
REVOKE ALL ON TABLE jam_virtual_wiki FROM maxcom;
GRANT ALL ON TABLE jam_virtual_wiki TO maxcom;
GRANT ALL ON TABLE jam_virtual_wiki TO jamwiki;


--
-- Name: jam_watchlist; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_watchlist FROM PUBLIC;
REVOKE ALL ON TABLE jam_watchlist FROM maxcom;
GRANT ALL ON TABLE jam_watchlist TO maxcom;
GRANT ALL ON TABLE jam_watchlist TO jamwiki;


--
-- Name: jam_wiki_user; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE jam_wiki_user FROM PUBLIC;
REVOKE ALL ON TABLE jam_wiki_user FROM maxcom;
GRANT ALL ON TABLE jam_wiki_user TO maxcom;
GRANT SELECT ON TABLE jam_wiki_user TO jamwiki;


--
-- Name: memories; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE memories FROM PUBLIC;
REVOKE ALL ON TABLE memories FROM maxcom;
GRANT ALL ON TABLE memories TO maxcom;
GRANT ALL ON TABLE memories TO linuxweb;


--
-- Name: memories_id_seq; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE memories_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE memories_id_seq FROM maxcom;
GRANT ALL ON SEQUENCE memories_id_seq TO maxcom;
GRANT SELECT,UPDATE ON SEQUENCE memories_id_seq TO linuxweb;


--
-- Name: monthly_stats; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE monthly_stats FROM PUBLIC;
REVOKE ALL ON TABLE monthly_stats FROM maxcom;
GRANT ALL ON TABLE monthly_stats TO maxcom;
GRANT ALL ON TABLE monthly_stats TO linuxweb;


--
-- Name: msgbase; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE msgbase FROM PUBLIC;
REVOKE ALL ON TABLE msgbase FROM maxcom;
GRANT ALL ON TABLE msgbase TO maxcom;
GRANT SELECT,INSERT,UPDATE ON TABLE msgbase TO linuxweb;


--
-- Name: s_guid; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE s_guid FROM PUBLIC;
REVOKE ALL ON SEQUENCE s_guid FROM maxcom;
GRANT ALL ON SEQUENCE s_guid TO maxcom;
GRANT SELECT,UPDATE ON SEQUENCE s_guid TO linuxweb;


--
-- Name: s_msgid; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE s_msgid FROM PUBLIC;
REVOKE ALL ON SEQUENCE s_msgid FROM maxcom;
GRANT ALL ON SEQUENCE s_msgid TO maxcom;
GRANT ALL ON SEQUENCE s_msgid TO linuxweb;


--
-- Name: s_uid; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE s_uid FROM PUBLIC;
REVOKE ALL ON SEQUENCE s_uid FROM maxcom;
GRANT ALL ON SEQUENCE s_uid TO maxcom;
GRANT SELECT,UPDATE ON SEQUENCE s_uid TO linuxweb;


--
-- Name: sections; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE sections FROM PUBLIC;
REVOKE ALL ON TABLE sections FROM maxcom;
GRANT ALL ON TABLE sections TO maxcom;
GRANT SELECT,DELETE,UPDATE ON TABLE sections TO linuxweb;


--
-- Name: tags; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE tags FROM PUBLIC;
REVOKE ALL ON TABLE tags FROM maxcom;
GRANT ALL ON TABLE tags TO maxcom;
GRANT ALL ON TABLE tags TO linuxweb;


--
-- Name: tags_values; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE tags_values FROM PUBLIC;
REVOKE ALL ON TABLE tags_values FROM maxcom;
GRANT ALL ON TABLE tags_values TO maxcom;
GRANT SELECT,INSERT,UPDATE ON TABLE tags_values TO linuxweb;


--
-- Name: tags_values_id_seq; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE tags_values_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE tags_values_id_seq FROM maxcom;
GRANT ALL ON SEQUENCE tags_values_id_seq TO maxcom;
GRANT UPDATE ON SEQUENCE tags_values_id_seq TO linuxweb;


--
-- Name: topics; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE topics FROM PUBLIC;
REVOKE ALL ON TABLE topics FROM maxcom;
GRANT ALL ON TABLE topics TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE topics TO linuxweb;


--
-- Name: user_agents; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE user_agents FROM PUBLIC;
REVOKE ALL ON TABLE user_agents FROM maxcom;
GRANT ALL ON TABLE user_agents TO maxcom;
GRANT SELECT,INSERT ON TABLE user_agents TO linuxweb;


--
-- Name: user_agents_id_seq; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE user_agents_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE user_agents_id_seq FROM maxcom;
GRANT ALL ON SEQUENCE user_agents_id_seq TO maxcom;
GRANT UPDATE ON SEQUENCE user_agents_id_seq TO linuxweb;


--
-- Name: user_events; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE user_events FROM PUBLIC;
REVOKE ALL ON TABLE user_events FROM maxcom;
GRANT ALL ON TABLE user_events TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE user_events TO linuxweb;


--
-- Name: user_events_id_seq; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE user_events_id_seq FROM PUBLIC;
REVOKE ALL ON SEQUENCE user_events_id_seq FROM maxcom;
GRANT ALL ON SEQUENCE user_events_id_seq TO maxcom;
GRANT ALL ON SEQUENCE user_events_id_seq TO linuxweb;


--
-- Name: vote_id; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE vote_id FROM PUBLIC;
REVOKE ALL ON SEQUENCE vote_id FROM maxcom;
GRANT ALL ON SEQUENCE vote_id TO maxcom;
GRANT UPDATE ON SEQUENCE vote_id TO linuxweb;


--
-- Name: vote_users; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE vote_users FROM PUBLIC;
REVOKE ALL ON TABLE vote_users FROM maxcom;
GRANT ALL ON TABLE vote_users TO maxcom;
GRANT SELECT,INSERT ON TABLE vote_users TO linuxweb;


--
-- Name: votenames; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE votenames FROM PUBLIC;
REVOKE ALL ON TABLE votenames FROM maxcom;
GRANT ALL ON TABLE votenames TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE votenames TO linuxweb;


--
-- Name: votes; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE votes FROM PUBLIC;
REVOKE ALL ON TABLE votes FROM maxcom;
GRANT ALL ON TABLE votes TO maxcom;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE votes TO linuxweb;


--
-- Name: votes_id; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON SEQUENCE votes_id FROM PUBLIC;
REVOKE ALL ON SEQUENCE votes_id FROM maxcom;
GRANT ALL ON SEQUENCE votes_id TO maxcom;
GRANT UPDATE ON SEQUENCE votes_id TO linuxweb;


--
-- Name: wiki_recent_change; Type: ACL; Schema: public; Owner: maxcom
--

REVOKE ALL ON TABLE wiki_recent_change FROM PUBLIC;
REVOKE ALL ON TABLE wiki_recent_change FROM maxcom;
GRANT ALL ON TABLE wiki_recent_change TO maxcom;
GRANT SELECT ON TABLE wiki_recent_change TO linuxweb;


--
-- PostgreSQL database dump complete
--

