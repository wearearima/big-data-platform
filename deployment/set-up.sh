# --------------
# POSTGRES
# --------------

kubectl apply -f https://raw.githubusercontent.com/reactive-tech/kubegres/v1.15/kubegres.yaml 

kubectl wait --for=condition=ready pod --all --namespace=kubegres-system

kubectl apply -f postgres-secret.yaml
kubectl apply -f postgres-init.yaml 
kubectl apply -f postgres.yaml 

# --------------
# HIVE METASTORE
# --------------

# wait for postgres and minio to be ready
kubectlwait  --for=condition=ready pod --all 

kubectl apply -f hive-metastore-core-config.yaml 
kubectl apply -f hive-metastore-site-config.yaml
kubectl apply -f hive-metastore-init-db.yaml 
kubectl wait --for=condition=complete job/init-hms-db 
kubectl apply -f hive-metastore-deployment.yaml 
kubectl expose deployment/hive-metastore 

# --------------
# TRINO
# --------------
helm repo add trino https://trinodb.github.io/charts/
helm install trino -f trino-values.yaml ./trino
kubectl apply -f trino-cli.yaml 


# --------------
# ARGO
# --------------

kubectl create ns argo
kubectl apply -n argo -f argo-quick-start.yaml
