CREATE SEQUENCE public.artist_artist_id_seq START 100;
CREATE SEQUENCE public.media_list_list_id_seq START 100;
CREATE SEQUENCE public.media_track_track_id_seq START 100;
CREATE SEQUENCE public.media_track_list_list_id_seq START 100;
CREATE SEQUENCE public.mime_type_mime_id_seq START 100;
CREATE SEQUENCE public.user_user_id_seq START 100;

CREATE TABLE public.artist ( 
	artist_id              	int8 NOT NULL DEFAULT nextval('artist_artist_id_seq'::text),
	artist_name            	varchar(1024) NOT NULL,
	artist_audit_user_id   	int4 NULL,
	artist_create_timestamp	timestamp NULL,
	artist_modify_timestamp	timestamp NULL 
	);
CREATE TABLE public.artist_alias ( 
	artist_id      	int8 NOT NULL,
	artist_alias_id	int8 NOT NULL,
	artist_alias_audit_user_id int4 NULL,
	artist_alias_create_timestamp timestamp NULL
	);
CREATE TABLE public.media_artwork ( 
	media_id     	int8 NOT NULL,
	artwork_seq  	int4 NOT NULL,
	artwork_title	varchar(30) NULL,
	artwork_path 	varchar(2048) NULL 
	);
CREATE TABLE public.media_list ( 
	list_id              	int8 NOT NULL DEFAULT nextval('media_list_list_id_seq'::text),
	list_name            	varchar(1024) NULL,
	list_artist_id       	int8 NULL,
	list_release_date    	date NULL,
	list_audit_user_id   	int4 NULL,
	list_modify_timestamp	timestamp NULL,
	list_create_timestamp	varchar(25) NULL 
	);
CREATE TABLE public.media_track ( 
	track_id             	int8 NOT NULL DEFAULT nextval('media_track_track_id_seq'::text),
	track_name           	varchar(1024) NULL,
	track_artist_id      	int8 NULL,
	track_artist_alias_id	int8 NULL,
	TRACK_ALBUM				varchar(1024),
	track_length_ms      	int8 NULL,
	track_persistent_id  	varchar(32) NULL,
	track_audit_user     	int4 NULL,
	track_audit_timestamp	timestamp NULL,
	track_add_timestamp  	timestamp NULL
	);
CREATE TABLE public.media_track_location (
	track_id	int8 NOT NULL,
	location_type varchar(1) not null,
	mime_id int4 NULL,
	location_size int8 NULL,
	location_url	varchar(2048) not null
	);
CREATE TABLE public.media_track_list ( 
	list_id 	int8 NOT NULL DEFAULT nextval('media_track_list_list_id_seq'::text),
	track_id	int8 NOT NULL,
	seq_nbr 	int4 NOT NULL 
	);
CREATE TABLE public.media_track_rating ( 
	track_id  	int8 NOT NULL,
	user_id   	int4 NOT NULL,
	rating    	decimal(2,1) NULL,
	play_count 	int4 NOT NULL DEFAULT 0 
	);
CREATE TABLE public.mime_type ( 
	mime_id            	int4 NOT NULL DEFAULT nextval('mime_type_mime_id_seq'::text),
	mime_type          	varchar(11) NOT NULL,
	mime_sub_type      	varchar(25) NULL,
	mime_file_extension	varchar(25) NULL,
	mime_file_type     	int8 NULL,
	mime_file_creator  	int8 NULL 
	);
CREATE TABLE public.role ( 
	role_id	int4 NOT NULL DEFAULT nextval('public.role_role_id_seq'::text),
	role_nm	varchar(50) NOT NULL 
	);
CREATE TABLE public.user ( 
	user_id  	int4 NOT NULL DEFAULT nextval('user_user_id_seq'::text),
	user_nm  	varchar(25) NOT NULL,
	user_pswd	varchar(255) NULL 
	);
CREATE TABLE public.user_role ( 
	user_id	int4 NOT NULL,
	role_id	int4 NOT NULL 
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
INSERT INTO public.mime_type(mime_id, mime_type, mime_sub_type, mime_file_extension, mime_file_type, mime_file_creator) VALUES(21, 'audio', 'mpeg', 'mp3', 1297101600, 1296321857);
INSERT INTO public.mime_type(mime_id, mime_type, mime_sub_type, mime_file_extension, mime_file_type, mime_file_creator) VALUES(22, 'audio', 'x-m4a', 'm4a', 1295270176, 1752133483);
insert into public.mime_type (mime_type,mime_sub_type, mime_file_extension) values ('audio', 'flac','flac');

INSERT INTO public.role(role_id, role_nm) VALUES(1, 'music');
INSERT INTO public.role(role_id, role_nm) VALUES(2, 'music');
INSERT INTO public.user(user_id, user_nm, user_pswd) VALUES(4, 'IMPORT', NULL);
INSERT INTO public.user(user_id, user_nm, user_pswd) VALUES(3, 'mary', 'mary');
INSERT INTO public.user(user_id, user_nm, user_pswd) VALUES(2, 'andy', 'OBF:1yt01uve1xfn1tvv1tv71xff1uus1ytm');
INSERT INTO public.user_role(user_id, role_id) VALUES(2, 1);
  
  
  
--CREATE UNIQUE INDEX artist_name_unq ON public.artist(artist_name);
--CREATE UNIQUE INDEX artist_pkey	ON public.artist(artist_id);
--CREATE UNIQUE INDEX artist_alias_pkey	ON public.artist_alias(artist_id, artist_alias_id);
--CREATE UNIQUE INDEX artist_alias_unq	ON public.artist_alias(artist_id);
--CREATE UNIQUE INDEX media_artwork_pkey	ON public.media_artwork(media_id, artwork_seq);
--CREATE UNIQUE INDEX media_list_pkey	ON public.media_list(list_id);
--CREATE UNIQUE INDEX media_track_pkey	ON public.media_track(track_id);
--CREATE UNIQUE INDEX media_track_location_pkey	ON public.media_track_location(track_id, location_type);
--CREATE UNIQUE INDEX media_track_list_pkey	ON public.media_track_list(list_id, track_id, seq_nbr);
--CREATE UNIQUE INDEX media_track_rating_pkey	ON public.media_track_rating(track_id, user_id);
--CREATE UNIQUE INDEX mime_type_unq	ON public.mime_type(mime_type, mime_sub_type);
--CREATE UNIQUE INDEX mime_type_pkey	ON public.mime_type(mime_id);
--CREATE UNIQUE INDEX role_pkey	ON public.role(role_id);
--CREATE UNIQUE INDEX user_pkey	ON public.user(user_id);
--CREATE UNIQUE INDEX user_unq	ON public.user(user_nm);
--CREATE UNIQUE INDEX user_role_pkey	ON public.user_role(user_id, role_id);

CREATE INDEX media_track_ix1 ON public.media_track (track_id);
CREATE INDEX media_track_ix2 ON public.media_track (track_id,track_artist_id);	
CREATE INDEX media_track_location_ix1 ON public.media_track_location (track_id, location_type);
CREATE INDEX artist_ix1 ON public.artist (artist_id);

ALTER TABLE public.artist	ADD CONSTRAINT artist_pkey	PRIMARY KEY (artist_id);
ALTER TABLE public.artist_alias	ADD CONSTRAINT artist_alias_pkey	PRIMARY KEY (artist_alias_id, artist_id);
ALTER TABLE public.media_artwork	ADD CONSTRAINT media_artwork_pkey	PRIMARY KEY (media_id, artwork_seq);
ALTER TABLE public.media_list	ADD CONSTRAINT media_list_pkey	PRIMARY KEY (list_id);
ALTER TABLE public.media_track	ADD CONSTRAINT media_track_pkey	PRIMARY KEY (track_id);
ALTER TABLE public.media_track_location	ADD CONSTRAINT media_track_location_pkey	PRIMARY KEY (track_id, location_type);
ALTER TABLE public.media_track_list	ADD CONSTRAINT media_track_list_pkey	PRIMARY KEY (list_id, seq_nbr, track_id);
ALTER TABLE public.media_track_rating	ADD CONSTRAINT media_track_rating_pkey	PRIMARY KEY (track_id, user_id);
ALTER TABLE public.mime_type	ADD CONSTRAINT mime_type_pkey	PRIMARY KEY (mime_id);
ALTER TABLE public.role	ADD CONSTRAINT role_pkey	PRIMARY KEY (role_id);
ALTER TABLE public.user	ADD CONSTRAINT user_pkey	PRIMARY KEY (user_id);
ALTER TABLE public.user_role	ADD CONSTRAINT user_role_pkey	PRIMARY KEY (role_id, user_id);
ALTER TABLE public.artist	ADD CONSTRAINT artist_name_unq	UNIQUE (artist_name);
ALTER TABLE public.artist_alias	ADD CONSTRAINT artist_alias_unq	UNIQUE (artist_id);
ALTER TABLE public.mime_type	ADD CONSTRAINT mime_type_unq	UNIQUE (mime_sub_type, mime_type);
ALTER TABLE public.user	ADD CONSTRAINT user_unq	UNIQUE (user_nm);
ALTER TABLE public.media_list	ADD CONSTRAINT media_list_artist_fk	FOREIGN KEY(list_artist_id)	REFERENCES public.artist(artist_id);
ALTER TABLE public.media_track_location	ADD CONSTRAINT media_track_mime_fk	FOREIGN KEY(mime_id)	REFERENCES public.mime_type(mime_id);
ALTER TABLE public.media_track_location ADD CONSTRAINT media_track_location_track_fk	FOREIGN KEY(track_id)	REFERENCES public.media_track(track_id);
ALTER TABLE public.media_track	ADD CONSTRAINT media_track_artist_fk	FOREIGN KEY(track_artist_id)	REFERENCES public.artist(artist_id);
ALTER TABLE public.media_track_list	ADD CONSTRAINT media_track_list_track_fk	FOREIGN KEY(track_id)	REFERENCES public.media_track(track_id);
ALTER TABLE public.media_track_list	ADD CONSTRAINT media_track_list_list_fk	FOREIGN KEY(list_id)	REFERENCES public.media_list(list_id);
ALTER TABLE public.media_track_rating	ADD CONSTRAINT media_track_rating_user_fk	FOREIGN KEY(user_id)	REFERENCES public.user(user_id);
ALTER TABLE public.media_track_rating	ADD CONSTRAINT media_track_rating_track_fk	FOREIGN KEY(track_id)	REFERENCES public.media_track(track_id);
ALTER TABLE public.user_role	ADD CONSTRAINT user_role_fk2	FOREIGN KEY(role_id)	REFERENCES public.role(role_id);
ALTER TABLE public.user_role	ADD CONSTRAINT user_role_fk1	FOREIGN KEY(user_id)	REFERENCES public.user(user_id);


select usename, relname, relkind, relhasrules from pg_class, pg_user where usesysid=relowner and usename = 'music';


