{{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: APPLICATION_AUTHENTICATION_MATCH_SUBDOMAINS
    value: {{ .Values.env.APPLICATION_AUTHENTICATION_MATCH_SUBDOMAINS | quote }}
  - name: APPLICATION_AUTHENTICATION_UI_WHITELIST
    value: {{ .Values.env.APPLICATION_AUTHENTICATION_UI_WHITELIST | quote }}

  - name: APPLICATION_GOOGLE_TAG_ID
    valueFrom:
      secretKeyRef:
        key: APPLICATION_GOOGLE_TAG_ID
        name: {{ template "app.name" . }}
  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    valueFrom:
      secretKeyRef:
        key: APPLICATIONINSIGHTS_CONNECTION_STRING
        name: {{ template "app.name" . }}

  - name: APPLICATION_NON_PROD_WARNING_ENABLED
    value: {{ .Values.env.APPLICATION_NON_PROD_WARNING_ENABLED | quote }}
  - name: APPLICATION_NON_PROD_WARNING_PROD_URL
    value: {{ .Values.env.APPLICATION_NON_PROD_WARNING_PROD_URL | quote }}
 
  - name: APPLICATION_NOTIFY_KEY
    valueFrom:
      secretKeyRef:
        key: APPLICATION_NOTIFY_KEY
        name: {{ template "app.name" . }}

  - name: APPLICATION_SMOKETEST_ENABLED
    value: "false"

  - name: AUTH_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        key: AUTH_DATASOURCE_PASSWORD
        name: {{ template "app.name" . }}
  - name: AUTH_DATASOURCE_URL
    valueFrom:
      secretKeyRef:
        key: AUTH_DATASOURCE_URL
        name: {{ template "app.name" . }}
  - name: AUTH_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        key: AUTH_DATASOURCE_USERNAME
        name: {{ template "app.name" . }}

  - name: AUTH_JPA_HIBERNATE_DIALECT
    value: "org.hibernate.dialect.SQLServer2012Dialect"

  - name: DELIUS_CLIENT_CLIENT_ID
    valueFrom:
      secretKeyRef:
        key: DELIUS_CLIENT_CLIENT_ID
        name: {{ template "app.name" . }}
  - name: DELIUS_CLIENT_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        key: DELIUS_CLIENT_CLIENT_SECRET
        name: {{ template "app.name" . }}

  - name: DELIUS_ENDPOINT_URL
    value: {{ .Values.env.DELIUS_ENDPOINT_URL | quote }}

  - name: DELIUS_ROLES_MAPPINGS_CTRBT001
    value: "ROLE_PF_STD_PROBATION,ROLE_GLOBAL_SEARCH"

  - name: DELIUS_ROLES_MAPPINGS_CTRBT002
    value: "ROLE_PF_APPROVAL"

  - name: DELIUS_ROLES_MAPPINGS_CTRBT003
    value: "ROLE_PF_STD_PROBATION_RO"

  - name: DELIUS_ROLES_MAPPINGS_CTRBT004
    value: "ROLE_PF_HQ"

  - name: DELIUS_ROLES_MAPPINGS_CWBT200
    value: "ROLE_PREPARE_A_CASE"

  - name: DELIUS_ROLES_MAPPINGS_LHDCBT001
    value: "ROLE_LICENCE_RO,ROLE_GLOBAL_SEARCH"

  - name: DELIUS_ROLES_MAPPINGS_LHDCBT002
    value: "ROLE_LICENCE_RO,ROLE_GLOBAL_SEARCH"

  - name: DELIUS_ROLES_MAPPINGS_LHDCBT003
    value: "ROLE_LICENCE_RO,ROLE_LICENCE_VARY,ROLE_GLOBAL_SEARCH"

  - name: DELIUS_ROLES_MAPPINGS_SOCBT001
    value: "ROLE_SOC_COMMUNITY"

  - name: JWT_COOKIE_EXPIRY_TIME
    value: {{ .Values.env.JWT_COOKIE_EXPIRY_TIME | quote }}
  - name: JWT_JWK_KEY_ID
    value: {{ .Values.env.JWT_JWK_KEY_ID | quote }}

  - name: JWT_KEYSTORE_PASSWORD
    valueFrom:
      secretKeyRef:
        key: JWT_KEYSTORE_PASSWORD
        name: {{ template "app.name" . }}
  - name: JWT_SIGNING_KEY_PAIR
    valueFrom:
      secretKeyRef:
        key: JWT_SIGNING_KEY_PAIR
        name: {{ template "app.name" . }}
  - name: SPRING_DATASOURCE_PASSWORD
    valueFrom:
      secretKeyRef:
        key: SPRING_DATASOURCE_PASSWORD
        name: {{ template "app.name" . }}
  - name: SPRING_DATASOURCE_URL
    valueFrom:
      secretKeyRef:
        key: SPRING_DATASOURCE_URL
        name: {{ template "app.name" . }}
  - name: SPRING_DATASOURCE_USERNAME
    valueFrom:
      secretKeyRef:
        key: SPRING_DATASOURCE_USERNAME
        name: {{ template "app.name" . }}

  - name: SPRING_PROFILES_ACTIVE
    value: {{ .Values.env.SPRING_PROFILES_ACTIVE | quote }}

  - name: TOKENVERIFICATION_CLIENT_CLIENT_ID
    valueFrom:
      secretKeyRef:
        key: TOKENVERIFICATION_CLIENT_CLIENT_ID
        name: {{ template "app.name" . }}
  - name: TOKENVERIFICATION_CLIENT_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        key: TOKENVERIFICATION_CLIENT_CLIENT_SECRET
        name: {{ template "app.name" . }}

  - name: TOKENVERIFICATION_ENDPOINT_URL
    value: {{ .Values.env.TOKENVERIFICATION_ENDPOINT_URL | quote }}

  - name: APPLICATION_AUTHENTICATION_MFA_WHITELIST
    value: {{ include "app.joinListWithComma" .Values.mfa_allowlist | quote }}

  - name: APPLICATION_AUTHENTICATION_MFA_ROLES
    value: "ROLE_MFA,ROLE_PECS_COURT,ROLE_PECS_SUPPLIER,ROLE_PF_STD_PRISON,ROLE_PF_STD_PRISON_RO,ROLE_PF_STD_PROBATION,ROLE_PF_STD_PROBATION_RO,ROLE_PF_POLICE,ROLE_PF_HQ,ROLE_PF_PSYCHOLOGIST,ROLE_PF_LOCAL_READER,ROLE_PF_NATIONAL_READER,ROLE_PPM_ANALYST,ROLE_AUTH_GROUP_MANAGER"

  - name: APPLICATION_SUPPORT_URL
    value: {{ .Values.env.APPLICATION_SUPPORT_URL | quote }}

  - name: AUTH_AZUREOIDC_CLIENT_ID
    valueFrom:
      secretKeyRef:
        key: AUTH_AZUREOIDC_CLIENT_ID
        name: {{ template "app.name" . }}

  - name: AUTH_AZUREOIDC_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        key: AUTH_AZUREOIDC_CLIENT_SECRET
        name: {{ template "app.name" . }}

  - name: AUTH_AZUREOIDC_TENANT_ID
    valueFrom:
      secretKeyRef:
        key: AUTH_AZUREOIDC_TENANT_ID
        name: {{ template "app.name" . }}
{{- end }}

