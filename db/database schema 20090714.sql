CREATE SEQUENCE public.artist_artist_id_seq START 100;
CREATE SEQUENCE public.media_list_list_id_seq START 100;
CREATE SEQUENCE public.media_track_track_id_seq START 100;
CREATE SEQUENCE public.media_track_list_list_id_seq START 100;
CREATE SEQUENCE public.mime_type_mime_id_seq START 100;
CREATE SEQUENCE public.user_user_id_seq START 100;


CREATE TABLE artist (ARTIST_ID int8 NOT NULL  DEFAULT nextval('artist_artist_id_seq'::text),
					ARTIST_NAME varchar(1024) NOT NULL,
					ARTIST_AUDIT_USER_ID int4,
					ARTIST_CREATE_TIMESTAMP timestamp,
					ARTIST_MODIFY_TIMESTAMP timestamp,
					PRIMARY KEY (artist_id));



CREATE TABLE artist_alias (ARTIST_ID int8 NOT NULL,
						ARTIST_ALIAS_ID int8 NOT NULL,
						ARTIST_ALIAS_AUDIT_USER_ID int4,
						ARTIST_ALIAS_CREATE_TIMESTAMP timestamp
						PRIMARY KEY (artist_alias_id, artist_id));



CREATE TABLE media_artwork (MEDIA_ID int8 NOT NULL,
							ARTWORK_SEQ int4 NOT NULL,
							ARTWORK_TITLE varchar(30),
							ARTWORK_PATH varchar(2048),
							PRIMARY KEY (media_id, artwork_seq));



CREATE TABLE media_list (LIST_ID int8 NOT NULL NOT NULL DEFAULT nextval('media_list_list_id_seq'::text),
						LIST_NAME varchar(1024),
						LIST_ARTIST_ID int8,
						LIST_RELEASE_DATE date,
						LIST_AUDIT_USER_ID int4,
						LIST_MODIFY_TIMESTAMP timestamp,
						LIST_CREATE_TIMESTAMP varchar(25),
						PRIMARY KEY (list_id),
						CONSTRAINT media_list_artist_fk FOREIGN KEY (list_artist_id) REFERENCES public.artist(artist_id));



CREATE TABLE media_track (TRACK_ID int8 NOT NULL DEFAULT nextval('media_track_track_id_seq'::text),
						TRACK_NAME varchar(1024),
						TRACK_ARTIST_ID int8,
						TRACK_ARTIST_ALIAS_ID int8,
						TRACK_LENGTH_MS int8,
						TRACK_PERSISTENT_ID varchar(32),
						TRACK_AUDIT_USER int4,
						TRACK_AUDIT_TIMESTAMP timestamp,
						TRACK_ADD_TIMESTAMP timestamp,
						PRIMARY KEY (track_id),
						CONSTRAINT media_track_artist_fk FOREIGN KEY (track_artist_id) REFERENCES public.artist(artist_id));



CREATE TABLE media_track_list (LIST_ID int8 NOT NULL DEFAULT nextval('media_track_list_list_id_seq'::text),
							TRACK_ID int8 NOT NULL,
							SEQ_NBR int4 NOT NULL,
							PRIMARY KEY (list_id, seq_nbr, track_id),
							CONSTRAINT media_track_list_list_fk FOREIGN KEY (list_id) REFERENCES public.media_list(list_id),
							CONSTRAINT media_track_list_track_fk FOREIGN KEY (track_id) REFERENCES public.media_track(track_id));



CREATE TABLE media_track_location (TRACK_ID int8 NOT NULL,
								LOCATION_TYPE varchar(1) NOT NULL,
								MIME_ID int4,
								LOCATION_URL varchar(2048) NOT NULL,
								PRIMARY KEY (track_id, location_type),
								CONSTRAINT media_track_location_track_fk FOREIGN KEY (track_id) REFERENCES public.media_track(track_id));



CREATE TABLE media_track_rating (TRACK_ID int8 NOT NULL,
								USER_ID int4 NOT NULL,
								RATING numeric(2,1),
								PLAY_COUNT int4 NOT NULL,
								PRIMARY KEY (track_id, user_id),
								CONSTRAINT media_track_rating_track_fk FOREIGN KEY (track_id) REFERENCES public.media_track(track_id),
								CONSTRAINT media_track_rating_user_fk FOREIGN KEY (user_id) REFERENCES public.user(user_id));



CREATE TABLE mime_type (MIME_ID int4 NOT NULL DEFAULT nextval('mime_type_mime_id_seq'::text),
						MIME_TYPE varchar(11) NOT NULL,
						MIME_SUB_TYPE varchar(25),
						MIME_FILE_EXTENSION varchar(25),
						MIME_FILE_TYPE int8,
						MIME_FILE_CREATOR int8,
						PRIMARY KEY (mime_id));



CREATE TABLE role (ROLE_ID int4 NOT NULL DEFAULT nextval('public.role_role_id_seq'::text),
				ROLE_NM varchar(50) NOT NULL,
				PRIMARY KEY (role_id));



CREATE TABLE user (USER_ID int4 NOT NULL DEFAULT nextval('user_user_id_seq'::text),
				USER_NM varchar(25) NOT NULL,
				USER_PSWD varchar(255),
				ADD CONSTRAINT user_pkey);


CREATE TABLE public.user_role ( 
	user_id	int4 NOT NULL,
	role_id	int4 NOT NULL,
	PRIMARY KEY (role_id, user_id),
	CONSTRAINT user_role_fk1 FOREIGN KEY(user_id) REFERENCES public.user(user_id),
	CONSTRAINT user_role_fk2 FOREIGN KEY(role_id) REFERENCES public.role(role_id)
	);

CREATE OR REPLACE VIEW public.user_role_view as
	select 
		u.user_id as user_id,
		u.user_pswd as user_pswd,
	r.role_nm as role_nm
	from public.user u
	inner join public.user_role ur on u.user_id = ur.user_id
	inner join public.role r on ur.role_id = r.role_id;




-- pre-populate some mime types
INSERT INTO public.mime_type(mime_id, mime_type, mime_sub_type, mime_file_extension, mime_file_type, mime_file_creator)
  VALUES(21, 'audio', 'mpeg', 'mp3', 1297101600, 1296321857);
INSERT INTO public.mime_type(mime_id, mime_type, mime_sub_type, mime_file_extension, mime_file_type, mime_file_creator)
  VALUES(22, 'audio', 'x-m4a', 'm4a', 1295270176, 1752133483);
	

INSERT INTO public.artist(artist_id, artist_name, artist_audit_user_id, artist_create_timestamp, artist_modify_timestamp)
  VALUES(3, 'New Order', 2, '2007-11-23 23:53:59.643', '2007-11-23 23:54:02.539');
INSERT INTO public.media_track(track_id, track_name, track_artist_id, track_artist_alias_id, track_audit_user, track_audit_timestamp, track_add_timestamp, track_length_ms, track_mime_id, track_persistent_id)
  VALUES(1, 'Temptation', 3, 3, 2, '2007-11-23 23:55:22.513', '2007-11-23 23:55:27.904', 11154653, 21, 'a');
INSERT INTO public.mime_type(mime_id, mime_type, mime_sub_type, mime_file_extension, mime_file_type, mime_file_creator)
  VALUES(21, 'audio', 'mpeg', 'mp3', 1297101600, 1296321857);
INSERT INTO public.mime_type(mime_id, mime_type, mime_sub_type, mime_file_extension, mime_file_type, mime_file_creator)
  VALUES(22, 'audio', 'x-m4a', 'm4a', 1295270176, 1752133483);
INSERT INTO public.role(role_id, role_nm)
  VALUES(1, 'music');
INSERT INTO public.role(role_id, role_nm)
  VALUES(2, 'music');
INSERT INTO public.user(user_id, user_nm, user_pswd)
  VALUES(4, 'IMPORT', NULL);
INSERT INTO public.user(user_id, user_nm, user_pswd)
  VALUES(3, 'mary', 'mary');
INSERT INTO public.user(user_id, user_nm, user_pswd)
  VALUES(2, 'andy', 'OBF:1yt01uve1xfn1tvv1tv71xff1uus1ytm');
INSERT INTO public.user_role(user_id, role_id)
  VALUES(2, 1);