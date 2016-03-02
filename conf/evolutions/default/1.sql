# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table cluster (
  id                        bigint not null,
  uri                       varchar(1496) not null,
  cert_directory            varchar(1496) not null,
  name                      varchar(1000) not null,
  user                      bigint not null,
  constraint pk_cluster primary key (id))
;

create table filter (
  id                        bigint not null,
  namespace                 varchar(1496) not null,
  name                      varchar(1496) not null,
  constraint pk_filter primary key (id))
;

create table user (
  id                        bigint not null,
  username                  varchar(255) not null,
  sha_password              varbinary(64) not null,
  token                     varchar(255) not null,
  tenant                    varchar(255),
  userid                    varchar(255),
  expire_date               timestamp not null,
  constraint uq_user_username unique (username),
  constraint pk_user primary key (id))
;

create sequence cluster_seq;

create sequence filter_seq;

create sequence user_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists cluster;

drop table if exists filter;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists cluster_seq;

drop sequence if exists filter_seq;

drop sequence if exists user_seq;

