#Rotate client script
##Prerequisites

- Ensure you are using bash version 4 or 5
- Have HTTPIE installed (https://httpie.io/docs#installation)
- Access to the namespace where the application is running\
  (i.e. can run `kubectl get pods -n <namespace>` in that namespace)
- All necessary fields completed in the client deployment
- Have a client which has the role - `ROLE_CLIENT_ROTATION_ADMIN`

##Running the script

Within a terminal go to the relevant folder where the script is located

Export the variables in terminal

```
export ENV='<environment e.g. 't3'> '
export USER='<nomis or auth username in that environment>'
export CLIENTID='<clientId with rotate permissions>'
export CLIENTSECRET=<client secret>'
```

Make sure the script `rotate_clientID_cloudplatform_app.sh` is executable


Now run
```./rotate_clientID_cloudplatform_app.sh <BASE_CLIENT_ID>```


##Useful kubectl commands

####manually update secret
```kubectl edit secrets <secret> -n <namespace>```

####roll out the manually updated secret
```kubectl rollout restart deploy <deployment name> -n <namespace>```
