drop view user_role_view;

CREATE OR REPLACE VIEW public.user_role_view as
select 
	u.user_id as user_id,
 	u.user_pswd as user_pswd,
  r.role_nm as role_nm
from public.user u
inner join public.user_role ur on u.user_id = ur.user_id
inner join public.role r on ur.role_id = r.role_id
