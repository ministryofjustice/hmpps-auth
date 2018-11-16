create table CASELOADS (
  CASELOAD_ID   varchar(255) not null,
  DESCRIPTION   varchar(255) not null,
  CASELOAD_TYPE varchar(255),
  primary key (CASELOAD_ID)
);
create table OMS_ROLES (
  ROLE_ID          bigint       not null,
  ROLE_CODE        varchar(255) not null,
  ROLE_FUNCTION    varchar(255) not null,
  ROLE_NAME        varchar(255),
  ROLE_SEQ         integer      not null,
  ROLE_TYPE        varchar(255),
  PARENT_ROLE_CODE varchar(255),
  primary key (ROLE_ID)
);
create table PERSONNEL_IDENTIFICATIONS (
  IDENTIFICATION_NUMBER varchar(255) not null,
  STAFF_ID              bigint       not null,
  IDENTIFICATION_TYPE   varchar(255) not null,
  primary key (IDENTIFICATION_NUMBER, STAFF_ID, IDENTIFICATION_TYPE)
);
create table STAFF_MEMBERS (
  STAFF_ID   bigint       not null,
  FIRST_NAME varchar(255) not null,
  LAST_NAME  varchar(255) not null,
  STATUS     varchar(255),
  primary key (STAFF_ID)
);
create table STAFF_USER_ACCOUNTS (
  USERNAME        varchar(255) not null,
  STAFF_USER_TYPE varchar(255) not null,
  STAFF_ID        bigint,
  primary key (USERNAME)
);
create table USER_ACCESSIBLE_CASELOADS (
  caseload_id varchar(255) not null,
  username    varchar(255) not null,
  START_DATE  date,
  primary key (caseload_id, username)
);
create table USER_CASELOAD_ROLES (
  caseload_id varchar(255) not null,
  role_id     bigint       not null,
  username    varchar(255) not null,
  primary key (caseload_id, role_id, username)
);
create table V_TAG_DBA_USERS (
  USERNAME              varchar(255) not null,
  ACCOUNT_STATUS        varchar(255) not null,
  CREATED               timestamp    not null,
  EXPIRED_FLAG          char(255)    not null,
  EXPIRY_DATE           timestamp,
  LOCK_DATE             timestamp,
  LOCKED_FLAG           char(255)    not null,
  LOGGED_IN             char(255)    not null,
  PROFILE               varchar(255),
  USER_TYPE_DESCRIPTION varchar(255),
  primary key (USERNAME)
);
alter table OMS_ROLES
  add constraint UK_lycfj54t0poe95kvu7t2i8ym unique (ROLE_CODE);
alter table OMS_ROLES
  add constraint FKtkg12mn5hmki1yng6cg21o9u foreign key (PARENT_ROLE_CODE) references OMS_ROLES (ROLE_CODE);
alter table PERSONNEL_IDENTIFICATIONS
  add constraint FKie8txceptgb707s5sayqyn8a8 foreign key (STAFF_ID) references STAFF_MEMBERS;
alter table STAFF_USER_ACCOUNTS
  add constraint FKoycmul6bokfc9slq6pho3atr4 foreign key (STAFF_ID) references STAFF_MEMBERS;
alter table USER_ACCESSIBLE_CASELOADS
  add constraint FKe53wbqi7yakqghee8dfcwikuy foreign key (CASELOAD_ID) references CASELOADS;
alter table USER_ACCESSIBLE_CASELOADS
  add constraint FKkkt1hw5uc1ht9stvhi3qf8yr9 foreign key (USERNAME) references STAFF_USER_ACCOUNTS;
alter table USER_CASELOAD_ROLES
  add constraint FKbw1poiecqk9rxpqy7hjcn1tra foreign key (CASELOAD_ID) references CASELOADS;
alter table USER_CASELOAD_ROLES
  add constraint FKnnq787g2pwue06n18rdy4pds7 foreign key (ROLE_ID) references OMS_ROLES;
alter table USER_CASELOAD_ROLES
  add constraint FKf6nmmym7g53rr6s8wxf787amd foreign key (USERNAME) references STAFF_USER_ACCOUNTS;
