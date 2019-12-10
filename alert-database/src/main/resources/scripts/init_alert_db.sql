CREATE SCHEMA IF NOT EXISTS ALERT;

create table if not exists ALERT.PROVIDER_PROJECTS
(
    ID                  SERIAL,
    NAME                VARCHAR(255),
    DESCRIPTION         VARCHAR(255),
    HREF                VARCHAR(255),
    PROJECT_OWNER_EMAIL VARCHAR(255),
    PROVIDER            VARCHAR(255),
    constraint BLACKDUCK_PROJECT_KEY
        primary key (ID)
);

create table if not exists ALERT.PROVIDER_USERS
(
    ID            SERIAL,
    EMAIL_ADDRESS VARCHAR(255),
    OPT_OUT       BOOLEAN,
    PROVIDER      VARCHAR(255),
    constraint BLACKDUCK_USER_KEY
        primary key (ID)
);

create table if not exists ALERT.SYSTEM_STATUS
(
    ID                        BIGINT not null,
    INITIALIZED_CONFIGURATION BOOLEAN,
    STARTUP_TIME              TIMESTAMP,
    constraint SYSTEM_STATUS_KEY
        primary key (ID)
);

create table if not exists ALERT.SYSTEM_MESSAGES
(
    ID         SERIAL,
    CREATED_AT TIMESTAMP,
    SEVERITY   VARCHAR(50),
    CONTENT    VARCHAR(255),
    TYPE       VARCHAR(255),
    constraint SYSTEM_MESSAGES_KEY
        primary key (ID)
);

create table if not exists ALERT.POLARIS_ISSUES
(
    ID             SERIAL,
    ISSUE_TYPE     VARCHAR(255),
    PREVIOUS_COUNT BIGINT,
    CURRENT_COUNT  BIGINT,
    PROJECT_ID     BIGINT,
    constraint POLARIS_ISSUES_KEY
        primary key (ID),
    constraint FK_ISSUE_PROJECT
        foreign key (PROJECT_ID) references ALERT.PROVIDER_PROJECTS (ID)
            on delete cascade
);

create table if not exists ALERT.RAW_NOTIFICATION_CONTENT
(
    ID                     SERIAL,
    CREATED_AT             TIMESTAMP,
    PROVIDER               VARCHAR(255),
    PROVIDER_CREATION_TIME TIMESTAMP,
    NOTIFICATION_TYPE      VARCHAR(255),
    CONTENT                JSON,
    constraint RAW_NOTIFICATION_CONTENT_KEY
        primary key (ID)
);

create table if not exists ALERT.PROVIDER_USER_PROJECT_RELATION
(
    PROVIDER_USER_ID    BIGINT not null,
    PROVIDER_PROJECT_ID BIGINT not null,
    constraint FK_PROVIDER_PROJECT_ID
        foreign key (PROVIDER_PROJECT_ID) references ALERT.PROVIDER_PROJECTS (ID)
            on delete cascade,
    constraint FK_PROVIDER_USER_ID
        foreign key (PROVIDER_USER_ID) references ALERT.PROVIDER_USERS (ID)
            on delete cascade
);

create table if not exists ALERT.ROLES
(
    ID       SERIAL,
    ROLENAME VARCHAR(255),
    CUSTOM   BOOLEAN default FALSE not null,
    constraint ROLE_KEY
        primary key (ID)
);

create table if not exists ALERT.SETTINGS_KEY
(
    ID    SERIAL,
    KEY   VARCHAR(255),
    VALUE VARCHAR(255),
    constraint SETTINGS_KEY_KEY
        primary key (ID),
    unique (KEY)
);

create table if not exists ALERT.REGISTERED_DESCRIPTORS
(
    ID      SERIAL,
    TYPE_ID BIGINT,
    NAME    VARCHAR(255),
    constraint REGISTERED_DESCRIPTORS_KEY
        primary key (ID)
);

create table if not exists ALERT.DESCRIPTOR_TYPES
(
    ID   SERIAL,
    TYPE VARCHAR(255),
    constraint DESCRIPTOR_TYPES_KEY
        primary key (ID)
);

create table if not exists ALERT.DEFINED_FIELDS
(
    ID         SERIAL,
    SOURCE_KEY VARCHAR(255)          not null
        unique,
    SENSITIVE  BOOLEAN default FALSE not null,
    constraint DEFINED_FIELDS_KEY
        primary key (ID)
);

create table if not exists ALERT.DESCRIPTOR_FIELDS
(
    DESCRIPTOR_ID BIGINT not null,
    FIELD_ID      BIGINT not null,
    constraint DESCRIPTOR_FIELDS_KEY
        primary key (DESCRIPTOR_ID, FIELD_ID),
    constraint FK_DESCRIPTOR_FIELD
        foreign key (FIELD_ID) references ALERT.DEFINED_FIELDS (ID)
            on delete cascade,
    constraint FK_FIELD_DESCRIPTOR
        foreign key (DESCRIPTOR_ID) references ALERT.REGISTERED_DESCRIPTORS (ID)
            on delete cascade
);

create table if not exists ALERT.CONFIG_CONTEXTS
(
    ID      SERIAL,
    CONTEXT VARCHAR(31)
        unique,
    constraint CONFIG_CONTEXTS_KEY
        primary key (ID)
);

create table if not exists ALERT.PERMISSION_MATRIX
(
    ROLE_ID       BIGINT  not null,
    OPERATIONS    INTEGER not null,
    DESCRIPTOR_ID BIGINT  not null,
    CONTEXT_ID    BIGINT  not null,
    constraint PERMISSION_MATRIX_KEY_UPDATED
        primary key (ROLE_ID, OPERATIONS, DESCRIPTOR_ID, CONTEXT_ID),
    constraint FK_PERMISSION_CONTEXT_ID
        foreign key (CONTEXT_ID) references ALERT.CONFIG_CONTEXTS (ID)
            on delete cascade,
    constraint FK_PERMISSION_DESCRIPTOR_ID
        foreign key (DESCRIPTOR_ID) references ALERT.REGISTERED_DESCRIPTORS (ID)
            on delete cascade,
    constraint FK_PERMISSION_ROLE
        foreign key (ROLE_ID) references ALERT.ROLES (ID)
            on delete cascade
);

create table if not exists ALERT.DESCRIPTOR_CONFIGS
(
    ID            SERIAL,
    DESCRIPTOR_ID BIGINT,
    CONTEXT_ID    BIGINT,
    CREATED_AT    TIMESTAMP,
    LAST_UPDATED  TIMESTAMP,
    constraint DESCRIPTOR_CONFIGS_KEY
        primary key (ID),
    constraint FK_CONFIG_CONTEXT
        foreign key (CONTEXT_ID) references ALERT.CONFIG_CONTEXTS (ID)
            on delete cascade,
    constraint FK_CONFIG_DESCRIPTOR
        foreign key (DESCRIPTOR_ID) references ALERT.REGISTERED_DESCRIPTORS (ID)
            on delete cascade
);

create table if not exists ALERT.FIELD_CONTEXTS
(
    FIELD_ID   BIGINT not null,
    CONTEXT_ID BIGINT not null,
    constraint FIELD_CONTEXTS_KEY
        primary key (FIELD_ID, CONTEXT_ID),
    constraint FK_CONTEXT_FIELD
        foreign key (FIELD_ID) references ALERT.DEFINED_FIELDS (ID)
            on delete cascade,
    constraint FK_FIELD_CONTEXT
        foreign key (CONTEXT_ID) references ALERT.CONFIG_CONTEXTS (ID)
            on delete cascade
);

create table if not exists ALERT.CONFIG_GROUPS
(
    CONFIG_ID BIGINT not null,
    JOB_ID    UUID   not null,
    constraint CONFIG_GROUPS_KEY
        primary key (CONFIG_ID),
    constraint FK_CONFIG_GROUP_VALUE
        foreign key (CONFIG_ID) references ALERT.DESCRIPTOR_CONFIGS (ID)
            on delete cascade
);

create table if not exists ALERT.FIELD_VALUES
(
    ID          SERIAL,
    CONFIG_ID   BIGINT,
    FIELD_ID    BIGINT,
    FIELD_VALUE VARCHAR(512),
    constraint CONFIG_VALUES_KEY
        primary key (ID),
    constraint FK_DEFINED_FIELD_VALUE
        foreign key (FIELD_ID) references ALERT.DEFINED_FIELDS (ID)
            on delete cascade,
    constraint FK_DESCRIPTOR_CONFIG_VALUE
        foreign key (CONFIG_ID) references ALERT.DESCRIPTOR_CONFIGS (ID)
            on delete cascade
);

create table if not exists ALERT.USERS
(
    ID               SERIAL,
    USERNAME         VARCHAR(2048)
        unique,
    PASSWORD         VARCHAR(2048),
    EMAIL_ADDRESS    VARCHAR(2048),
    EXPIRED          BOOLEAN default FALSE,
    LOCKED           BOOLEAN default FALSE,
    PASSWORD_EXPIRED BOOLEAN default FALSE,
    ENABLED          BOOLEAN default TRUE,
    constraint USER_KEY
        primary key (ID)
);

create table if not exists ALERT.USER_ROLES
(
    USER_ID BIGINT not null,
    ROLE_ID BIGINT not null,
    constraint FK_ROLE_ID
        foreign key (ROLE_ID) references ALERT.ROLES (ID)
            on delete cascade,
    constraint FK_USER_ID
        foreign key (USER_ID) references ALERT.USERS (ID)
            on delete cascade
);

create table if not exists ALERT.AUDIT_ENTRIES
(
    ID                SERIAL
        primary key,
    ERROR_MESSAGE     VARCHAR(255),
    ERROR_STACK_TRACE VARCHAR,
    STATUS            VARCHAR(255),
    TIME_CREATED      TIMESTAMP,
    TIME_LAST_SENT    TIMESTAMP,
    COMMON_CONFIG_ID  UUID
);

create table if not exists ALERT.AUDIT_NOTIFICATION_RELATION
(
    AUDIT_ENTRY_ID  BIGINT not null,
    NOTIFICATION_ID BIGINT not null,
    primary key (AUDIT_ENTRY_ID, NOTIFICATION_ID),
    constraint FK_AUDIT_ENTRY_ID
        foreign key (AUDIT_ENTRY_ID) references ALERT.AUDIT_ENTRIES (ID)
            on delete cascade,
    constraint FK_AUDIT_NOTIFICATION_ID
        foreign key (NOTIFICATION_ID) references ALERT.RAW_NOTIFICATION_CONTENT (ID)
            on delete cascade
);
