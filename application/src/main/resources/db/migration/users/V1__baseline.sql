-- User database (accounts, groups, accessible databases, user properties)
-- Schema of FOR-IDENT 2.x (PostgreSQL), as created by EclipseLink until 3.0 (SP-16, from a schema-only dump).
-- On an existing database Flyway records this version as baseline without running the script;
-- on an empty database it creates the tables.

CREATE TABLE public.group_authorities (
    group_id bigint,
    authorities character varying(255) NOT NULL
);

CREATE TABLE public.sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0)
);

CREATE TABLE public.tbl_databases (
    id bigint NOT NULL,
    name character varying(255),
    publicaccessible boolean
);

CREATE TABLE public.tbl_user (
    id bigint NOT NULL,
    enabled boolean,
    firstname character varying(255),
    lastname character varying(255),
    mail character varying(255),
    organisation character varying(255),
    password character varying(255),
    username character varying(255)
);

CREATE TABLE public.tbl_user_groups (
    id bigint NOT NULL,
    name character varying(255)
);

CREATE TABLE public.tbl_user_tbl_databases (
    registereduser_id bigint NOT NULL,
    accessibledatabases_id bigint NOT NULL
);

CREATE TABLE public.tbl_user_tbl_user_groups (
    groups_id bigint NOT NULL,
    user_id bigint NOT NULL
);

CREATE TABLE public.userproperties (
    userid bigint NOT NULL,
    properties bytea
);

ALTER TABLE ONLY public.sequence
    ADD CONSTRAINT sequence_pkey PRIMARY KEY (seq_name);

ALTER TABLE ONLY public.tbl_databases
    ADD CONSTRAINT tbl_databases_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_user_groups
    ADD CONSTRAINT tbl_user_groups_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_user
    ADD CONSTRAINT tbl_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_user_tbl_databases
    ADD CONSTRAINT tbl_user_tbl_databases_pkey PRIMARY KEY (registereduser_id, accessibledatabases_id);

ALTER TABLE ONLY public.tbl_user_tbl_user_groups
    ADD CONSTRAINT tbl_user_tbl_user_groups_pkey PRIMARY KEY (groups_id, user_id);

ALTER TABLE ONLY public.userproperties
    ADD CONSTRAINT userproperties_pkey PRIMARY KEY (userid);

ALTER TABLE ONLY public.group_authorities
    ADD CONSTRAINT fk_group_authorities_group_id FOREIGN KEY (group_id) REFERENCES public.tbl_user_groups(id);

ALTER TABLE ONLY public.tbl_user_tbl_databases
    ADD CONSTRAINT fk_tbl_user_tbl_databases_accessibledatabases_id FOREIGN KEY (accessibledatabases_id) REFERENCES public.tbl_databases(id);

ALTER TABLE ONLY public.tbl_user_tbl_databases
    ADD CONSTRAINT fk_tbl_user_tbl_databases_registereduser_id FOREIGN KEY (registereduser_id) REFERENCES public.tbl_user(id);

ALTER TABLE ONLY public.tbl_user_tbl_user_groups
    ADD CONSTRAINT fk_tbl_user_tbl_user_groups_groups_id FOREIGN KEY (groups_id) REFERENCES public.tbl_user_groups(id);

ALTER TABLE ONLY public.tbl_user_tbl_user_groups
    ADD CONSTRAINT fk_tbl_user_tbl_user_groups_user_id FOREIGN KEY (user_id) REFERENCES public.tbl_user(id);

-- Counter of the EclipseLink table sequencing; EclipseLink inserted it when it created the tables itself
INSERT INTO public.sequence (seq_name, seq_count) VALUES ('SEQ_GEN', 0);
