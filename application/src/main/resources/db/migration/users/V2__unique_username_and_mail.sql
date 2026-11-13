-- User names and mail addresses are unique, independent of case (SP-20, review finding S9).
-- Without these indexes two concurrent registrations could create a duplicate; the login and the password reset of
-- that account would then fail. The migration fails if the table already holds duplicates: find them with
--   SELECT lower(username), count(*) FROM tbl_user GROUP BY 1 HAVING count(*) > 1;
--   SELECT lower(mail), count(*) FROM tbl_user GROUP BY 1 HAVING count(*) > 1;
-- and resolve them by hand before starting the new version.

CREATE UNIQUE INDEX tbl_user_username_lower_key ON public.tbl_user (lower(username));

CREATE UNIQUE INDEX tbl_user_mail_lower_key ON public.tbl_user (lower(mail));
