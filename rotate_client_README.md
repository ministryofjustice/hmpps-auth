#Rotate client script
##Prerequisites

- Ensure you are using bash version 4 or 5
- Have HTTPIE installed (https://httpie.io/docs#installation)
- All necessary fields completed in the client deployment
- Have a client which has the clientId rotate permissions

##Running the script

Within a terminal go to the relevant folder where the script is located

Export the variables in terminal

```
export ENV='<environment e.g. 't3'> '
export USER='<your username>'
export CLIENTID='<clientId with rotate permissions>'
export CLIENTSECRET=<client secret>'
```

Make sure the script `rotate_clientID_cloudplatform_app.sh` is executable


Now run
```./rotate_clientID_cloudplatform_app.sh <BASE_CLIENT_ID>```


##Useful kubectl commands

####Get Namespaces
```kubectl get namespaces```

####Get Pods
```kubectl get pods -n <namespace>```

####Get Secrets
```kubectl get secrets -n <namespace>```

####get/view specific secret
```kubectl get secret <secret> -n <namespace> -o yaml```

####manually update secret
```kubectl edit secrets <secret> -n <namespace>```

####roll out the manually updated secret
```kubectl rollout restart deploy <deployment name> -n <namespace>```
