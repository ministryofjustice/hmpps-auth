{{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: APPLICATION_AUTHENTICATION_MATCH_SUBDOMAINS
    valueFrom:
      secretKeyRef:
        key: APPLICATION_AUTHENTICATION_MATCH_SUBDOMAINS
        name: {{ template "app.name" . }}
  - name: APPLICATION_AUTHENTICATION_UI_WHITELIST
    valueFrom:
      secretKeyRef:
        key: APPLICATION_AUTHENTICATION_UI_WHITELIST
        name: {{ template "app.name" . }}
  - name: APPLICATION_GOOGLE_TAG_ID
    valueFrom:
      secretKeyRef:
        key: APPLICATION_GOOGLE_TAG_ID
        name: {{ template "app.name" . }}
  - name: APPLICATION_INSIGHTS_IKEY
    valueFrom:
      secretKeyRef:
        key: APPLICATION_INSIGHTS_IKEY
        name: {{ template "app.name" . }}
  - name: APPLICATION_NON_PROD_WARNING_ENABLED
    valueFrom:
      secretKeyRef:
        key: APPLICATION_NON_PROD_WARNING_ENABLED
        name: {{ template "app.name" . }}
  - name: APPLICATION_NON_PROD_WARNING_PROD_URL
    valueFrom:
      secretKeyRef:
        key: APPLICATION_NON_PROD_WARNING_PROD_URL
        name: {{ template "app.name" . }}
  - name: APPLICATION_NOTIFY_KEY
    valueFrom:
      secretKeyRef:
        key: APPLICATION_NOTIFY_KEY
        name: {{ template "app.name" . }}
  - name: APPLICATION_SMOKETEST_ENABLED
    valueFrom:
      secretKeyRef:
        key: APPLICATION_SMOKETEST_ENABLED
        name: {{ template "app.name" . }}
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
    valueFrom:
      secretKeyRef:
        key: AUTH_JPA_HIBERNATE_DIALECT
        name: {{ template "app.name" . }}
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
  - name: DELIUS_ENABLED
    valueFrom:
      secretKeyRef:
        key: DELIUS_ENABLED
        name: {{ template "app.name" . }}
  - name: DELIUS_ENDPOINT_URL
    valueFrom:
      secretKeyRef:
        key: DELIUS_ENDPOINT_URL
        name: {{ template "app.name" . }}
  - name: DELIUS_ROLES_MAPPINGS_CTRBT001
    valueFrom:
      secretKeyRef:
        key: DELIUS_ROLES_MAPPINGS_CTRBT001
        name: {{ template "app.name" . }}
  - name: DELIUS_ROLES_MAPPINGS_CTRBT002
    valueFrom:
      secretKeyRef:
        key: DELIUS_ROLES_MAPPINGS_CTRBT002
        name: {{ template "app.name" . }}
  - name: DELIUS_ROLES_MAPPINGS_CTRBT003
    valueFrom:
      secretKeyRef:
        key: DELIUS_ROLES_MAPPINGS_CTRBT003
        name: {{ template "app.name" . }}
  - name: DELIUS_ROLES_MAPPINGS_LHDCBT001
    valueFrom:
      secretKeyRef:
        key: DELIUS_ROLES_MAPPINGS_LHDCBT001
        name: {{ template "app.name" . }}
  - name: DELIUS_ROLES_MAPPINGS_LHDCBT002
    valueFrom:
      secretKeyRef:
        key: DELIUS_ROLES_MAPPINGS_LHDCBT002
        name: {{ template "app.name" . }}
  - name: DELIUS_ROLES_MAPPINGS_LHDCBT003
    valueFrom:
      secretKeyRef:
        key: DELIUS_ROLES_MAPPINGS_LHDCBT003
        name: {{ template "app.name" . }}
  - name: JWT_COOKIE_EXPIRY_TIME
    valueFrom:
      secretKeyRef:
        key: JWT_COOKIE_EXPIRY_TIME
        name: {{ template "app.name" . }}
  - name: JWT_JWK_KEY_ID
    valueFrom:
      secretKeyRef:
        key: JWT_JWK_KEY_ID
        name: {{ template "app.name" . }}
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
    valueFrom:
      secretKeyRef:
        key: SPRING_PROFILES_ACTIVE
        name: {{ template "app.name" . }}
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
  - name: TOKENVERIFICATION_ENABLED
    valueFrom:
      secretKeyRef:
        key: TOKENVERIFICATION_ENABLED
        name: {{ template "app.name" . }}
  - name: TOKENVERIFICATION_ENDPOINT_URL
    valueFrom:
      secretKeyRef:
        key: TOKENVERIFICATION_ENDPOINT_URL
        name: {{ template "app.name" . }}
  - name: APPLICATION_AUTHENTICATION_MFA_WHITELIST
    value: {{ include "app.joinListWithComma" .Values.mfa_whitelist | quote }}
{{- end }}

