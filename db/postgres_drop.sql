
DROP TABLE public.artist CASCADE;
DROP TABLE public.artist_alias CASCADE;
DROP TABLE public.media_artwork CASCADE;
DROP TABLE public.media_list CASCADE;
DROP TABLE public.media_track CASCADE;
DROP TABLE public.media_track_list CASCADE;
DROP TABLE public.media_track_location CASCADE;
DROP TABLE public.media_track_rating CASCADE;
DROP TABLE public.mime_type CASCADE;
DROP TABLE public.role CASCADE;
DROP TABLE public.user CASCADE;
DROP TABLE public.user_role CASCADE;

drop sequence public.artist_artist_id_seq cascade;
drop sequence public.media_list_list_id_seq cascade;
drop sequence public.media_track_track_id_seq cascade;
drop sequence public.media_track_list_list_id_seq cascade;
drop sequence public.mime_type_mime_id_seq cascade;
drop sequence public.user_user_id_seq cascade;

select usename, relname, relkind, relhasrules from pg_class, pg_user where usesysid=relowner and usename = 'music';