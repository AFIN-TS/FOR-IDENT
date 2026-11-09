-- Substance databases (STOFF-IDENT, PLANT-IDENT and others share this schema)
-- Schema of FOR-IDENT 2.x (PostgreSQL), as created by EclipseLink until 3.0 (SP-16, from a schema-only dump).
-- On an existing database Flyway records this version as baseline without running the script;
-- on an empty database it creates the tables.

CREATE TABLE public.sequence (
    seq_name character varying(50) NOT NULL,
    seq_count numeric(38,0)
);

CREATE TABLE public.tbl_additionalnames (
    id bigint NOT NULL,
    additional character varying(1000),
    editor character varying(255),
    lastmodified date,
    source character varying(255),
    value character varying(3000),
    substance_id bigint
);

CREATE TABLE public.tbl_categories (
    id bigint NOT NULL,
    additional character varying(1000),
    editor character varying(255),
    lastmodified date,
    source character varying(255),
    value character varying(3000)
);

CREATE TABLE public.tbl_logd (
    id bigint NOT NULL,
    additional character varying(1000),
    charge integer,
    editor character varying(255),
    lastmodified date,
    ph double precision,
    source character varying(255),
    value double precision,
    substance_id bigint
);

CREATE TABLE public.tbl_logp (
    id bigint NOT NULL,
    additional character varying(1000),
    editor character varying(255),
    lastmodified date,
    source character varying(255),
    value double precision,
    substance_id bigint
);

CREATE TABLE public.tbl_massbankids (
    id bigint NOT NULL,
    additional character varying(1000),
    editor character varying(255),
    lastmodified date,
    source character varying(255),
    value character varying(3000),
    substance_id bigint
);

CREATE TABLE public.tbl_source_lists (
    id bigint NOT NULL,
    description character varying(255),
    name character varying(255)
);

CREATE TABLE public.tbl_substances (
    id bigint NOT NULL,
    datasourcename character varying(255),
    dtxsid character varying(255),
    lastmodified date,
    publicid character varying(255),
    mass_additional character varying(1000),
    mass_editor character varying(255),
    mass_lastmodified date,
    mass_source character varying(255),
    mass_value double precision,
    cas_additional character varying(1000),
    cas_editor character varying(255),
    cas_lastmodified date,
    cas_source character varying(255),
    cas_value character varying(1000),
    ecnumber_additional character varying(1000),
    ecnumber_editor character varying(255),
    ecnumber_lastmodified date,
    ecnumber_source character varying(255),
    ecnumber_value character varying(20),
    formula_additional character varying(1000),
    formula_editor character varying(255),
    formula_lastmodified date,
    formula_source character varying(255),
    formula_value character varying(1000),
    henry_bond_additional character varying(1000),
    henry_bond_editor character varying(255),
    henry_bond_lastmodified date,
    henry_bond_source character varying(255),
    henry_bond_value double precision,
    henry_exper_additional character varying(1000),
    henry_exper_editor character varying(255),
    henry_exper_lastmodified date,
    henry_exper_source character varying(255),
    henry_exper_value double precision,
    henry_group_additional character varying(1000),
    henry_group_editor character varying(255),
    henry_group_lastmodified date,
    henry_group_source character varying(255),
    henry_group_value double precision,
    inchi_additional character varying(1000),
    inchi_editor character varying(3000),
    inchi_lastmodified date,
    inchi_source character varying(255),
    inchi_value character varying(3000),
    inchi_key_additional character varying(1000),
    inchi_key_editor character varying(255),
    inchi_key_lastmodified date,
    inchi_key_source character varying(255),
    inchi_key_value character varying(27),
    iupac_additional character varying(1000),
    iupac_editor character varying(3000),
    iupac_lastmodified date,
    iupac_source character varying(255),
    iupac_value character varying(3000),
    name_additional character varying(1000),
    name_editor character varying(255),
    name_lastmodified date,
    name_source character varying(255),
    name_value character varying(3000),
    smiles_additional character varying(1000),
    smiles_editor character varying(255),
    smiles_lastmodified date,
    smiles_source character varying(255),
    smiles_value character varying(3000),
    tonnage_additional character varying(1000),
    tonnage_editor character varying(255),
    tonnage_lastmodified date,
    tonnage_source character varying(255),
    tonnage_value character varying(100)
);

CREATE TABLE public.tbl_substances_tbl_categories (
    substances_id bigint NOT NULL,
    categories_id bigint NOT NULL
);

CREATE TABLE public.tbl_substances_tbl_source_lists (
    substances_id bigint NOT NULL,
    sourcelists_id bigint NOT NULL
);

ALTER TABLE ONLY public.sequence
    ADD CONSTRAINT sequence_pkey PRIMARY KEY (seq_name);

ALTER TABLE ONLY public.tbl_additionalnames
    ADD CONSTRAINT tbl_additionalnames_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_categories
    ADD CONSTRAINT tbl_categories_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_logd
    ADD CONSTRAINT tbl_logd_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_logp
    ADD CONSTRAINT tbl_logp_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_massbankids
    ADD CONSTRAINT tbl_massbankids_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_source_lists
    ADD CONSTRAINT tbl_source_lists_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_substances
    ADD CONSTRAINT tbl_substances_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.tbl_substances_tbl_categories
    ADD CONSTRAINT tbl_substances_tbl_categories_pkey PRIMARY KEY (substances_id, categories_id);

ALTER TABLE ONLY public.tbl_substances_tbl_source_lists
    ADD CONSTRAINT tbl_substances_tbl_source_lists_pkey PRIMARY KEY (substances_id, sourcelists_id);

CREATE INDEX index_tbl_substances_id ON public.tbl_substances USING btree (id);

CREATE INDEX tbl_additionalnames_id ON public.tbl_additionalnames USING btree (id);

CREATE INDEX tbl_additionalnames_value ON public.tbl_additionalnames USING btree (upper(((value)::text COLLATE "default")));

CREATE UNIQUE INDEX tbl_substances_id ON public.tbl_substances USING btree (id);

CREATE INDEX tbl_substances_mass_value ON public.tbl_substances USING btree (mass_value);

CREATE INDEX tbl_substances_upper_formula_value ON public.tbl_substances USING btree (upper((formula_value)::text));

CREATE INDEX tbl_substances_upper_iupac_value ON public.tbl_substances USING btree (upper((iupac_value)::text));

CREATE INDEX tbl_substances_upper_name_value ON public.tbl_substances USING btree (upper((name_value)::text));

CREATE INDEX tbl_substances_upper_smiles_value ON public.tbl_substances USING btree (upper((smiles_value)::text));

ALTER TABLE ONLY public.tbl_additionalnames
    ADD CONSTRAINT fk_tbl_additionalnames_substance_id FOREIGN KEY (substance_id) REFERENCES public.tbl_substances(id);

ALTER TABLE ONLY public.tbl_logd
    ADD CONSTRAINT fk_tbl_logd_substance_id FOREIGN KEY (substance_id) REFERENCES public.tbl_substances(id);

ALTER TABLE ONLY public.tbl_logp
    ADD CONSTRAINT fk_tbl_logp_substance_id FOREIGN KEY (substance_id) REFERENCES public.tbl_substances(id);

ALTER TABLE ONLY public.tbl_massbankids
    ADD CONSTRAINT fk_tbl_massbankids_substance_id FOREIGN KEY (substance_id) REFERENCES public.tbl_substances(id);

ALTER TABLE ONLY public.tbl_substances_tbl_categories
    ADD CONSTRAINT fk_tbl_substances_tbl_categories_categories_id FOREIGN KEY (categories_id) REFERENCES public.tbl_categories(id);

ALTER TABLE ONLY public.tbl_substances_tbl_categories
    ADD CONSTRAINT fk_tbl_substances_tbl_categories_substances_id FOREIGN KEY (substances_id) REFERENCES public.tbl_substances(id);

ALTER TABLE ONLY public.tbl_substances_tbl_source_lists
    ADD CONSTRAINT fk_tbl_substances_tbl_source_lists_sourcelists_id FOREIGN KEY (sourcelists_id) REFERENCES public.tbl_source_lists(id);

ALTER TABLE ONLY public.tbl_substances_tbl_source_lists
    ADD CONSTRAINT fk_tbl_substances_tbl_source_lists_substances_id FOREIGN KEY (substances_id) REFERENCES public.tbl_substances(id);

-- Counter of the EclipseLink table sequencing; EclipseLink inserted it when it created the tables itself
INSERT INTO public.sequence (seq_name, seq_count) VALUES ('SEQ_GEN', 0);
