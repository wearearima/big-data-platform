# How to deploy 

## Prerrequisites

1. In order to try the architecture locally, we will use [Kind](https://kind.sigs.k8s.io/), a tool for running Kubernetes clusters using Docker containers as nodes. Docker should be configured with at least 8GB memory for Superset or Datahub to install and run properly.

2. [Kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl), the Kubernetes command-line tool.

3. In order to be able to install some of the programs in the Kubernetes cluster, you will need [helm](https://helm.sh/) and [krew](https://krew.sigs.k8s.io/docs/user-guide/setup/install/).

## Installation

Run everything from this folder.

### 1. Set up the cluster

From the current folder as working directory run

```
kind create cluster --config kind-cluster.yaml
```

This will create a Kubernetes cluster with one control plane node and three worker nodes.
You should be able to see the docker containers with the names 'kind-control-plane' and 'kind-worker1' to 'kind-worker3' when checking with `docker ps`.

In order to stop and delete the cluster, do:

```
kind delete cluster
```

### 2. Install Minio

First, create a namespace for Minio's deployment and prepare persistent volumes. Then, install [Minio operator](https://github.com/minio/operator) using krew.

Note that we are using the default storage class provided by Kind. In any other scenario, you might need to create a [storage class](https://kubernetes.io/docs/concepts/storage/storage-classes/) and change the `storageClassName` field on [minio-persistent-volumes.yaml](minio-persistent-volumes.yaml). 

**TO DO: Try Minio's [DirectPV](https://github.com/minio/directpv) to discover and mount drives automatically.**

```
kubectl create ns minio
kubectl apply -f minio-persistent-volumes.yaml
kubectl krew update
kubectl krew install minio
kubectl minio init
```

When the minio operator is up and running (`kubectl get pods -n minio-operator`), do the following to access the operator through your browser on localhost:9090.

```
kubectl minio proxy -n minio-operator
```

A JWT token will be printed on the console, which is necessary to log into the operator.

For testing purposes, we will create an unsecured Minio server pool with 2 servers and 4 drives per server. However, keep in mind that:

> MinIO recommends a minimum of 4 nodes per server pool with 4 drives per server. With the default erasure code parity setting of EC:4 , this topology can continue serving read and write operations despite the loss of up to 4 drives or one node.

[Additional deployment guidelines](https://docs.min.io/minio/baremetal/installation/deploy-minio-distributed.html#id5)

Configure the Minio cluster as shown in the pictures. Additionally, you may disable audit logs and monitoring to save resources.

---
![minio-config-1](img/minio-config-1.png "Minio basic config")
---
![minio-config-2](img/minio-config-2.png "Minio user and password")
---
![minio-config-3](img/minio-config-3.png "Minio disable TLS")
---

Once the minio deployment is ready, you can stop the proxy for the operator.

![minio-ready](img/minio-ready.png "Minio is ready")


This Minio tenant can be removed through the operator or by deleting its namespace (`kubectl delete ns minio`).


#### Access Minio

Now Minio is deployed in the namespace `minio`. Kubernetes internal apps can access it through `minio.minio:80`, with the admin user *minio123* and password *minio123*.

It can be accessed via Minio [mc](https://docs.min.io/docs/minio-client-complete-guide.html), and also via the [aws cli](https://docs.min.io/docs/aws-cli-with-minio.html).

Externally, we can easily access the Minio tenant console directly by port-forwarding one of minio's pods:

```
kubectl port-forward -n minio minio-pool-0-0 9090:9090
```

From localhost:9090 you can manage buckets, users, etc.

Alternatively, all minio tenants can be managed from the Minio operator directly.

**TO DO:  Configure Ingress for this and other services?**



### 3. Install PostgreSQL

*NOTE: You can follow the steps that follow or, alternatively, simply run `set-up.sh` to install
Postgres, Hive Metastore, Trino and Argo Workflows*

We will use Postgres as a database for storing data, but we will also need it to provide databases for Superset and [Hive Metastore](https://cwiki.apache.org/confluence/display/Hive/AdminManual+Metastore+3.0+Administration#AdminManualMetastore3.0Administration-RunningtheMetastoreWithoutHive). Hive Metastore is a tool necessary for managing Iceberg tables.

We will install Postgres through [Kubegres](https://www.kubegres.io/), a Kubernetes operator allowing to deploy Postgres with data replication and failover enabled out-of-the box.

Again, we will be using Kind's default storage class. If this was not the case, additional configuration steps would be necessary.


```
kubectl apply -f https://raw.githubusercontent.com/reactive-tech/kubegres/v1.15/kubegres.yaml 

kubectl wait --for=condition=ready pod --all --namespace=kubegres-system

kubectl apply -f postgres-secret.yaml
kubectl apply -f postgres-init.yaml 
kubectl apply -f postgres.yaml 

kubectl get pods -w

```

The Secret [postgres-secret.yaml](postgres-secret.yaml) contains the secrets for all the databases that will be created on our Postgres cluster. 

The ConfigMap [postgres-init.yaml](postgres-init.yaml) contains the initialization script that will create all the databases that we will need. It can be modified to perform additional initialization steps.

The file [postgres.yaml](postgres.yaml) is a Kubegres resouce that containes the necessary information to set up the Postgres cluster.

**TO DO: ¿INSTALO POSTGRES EN SU PROPIO NAMESPACE TAMBIÉN?  AHORA ESTÁ EN DEFAULT?**

#### Access Posgres

The Postgres service will be avaible for internal applications through `postgres.default:5432`.

Three databases have been created: `hivemetastore`, `superset` and `data`. You can check the access credentials on postgres-secret.yaml.

In order to access through localhost, use port-forward

```
kubectl port-forward postgres-1-0 5432:5432
```

See section 6 [here](https://www.kubegres.io/doc/getting-started.html) to understand the services that were created and how they relate to each other.

#### Delete the postgres cluster

If you want to delete the Postgres cluster that we just created, run:

```
kubectl delete kubegres postgres
```

### 4. Install Hive Metastore

First create the following ConfigMaps, that store configuration information for Hive Metastore.

**TO DO: No sé si tendrían que ser secretos, porque tienen las contraseñas de postgres y de minio. La otra opción sería crear imágenes con variables de entorno, y generar estos ficheros con las contraseñas, etc. al iniciar el contenedor**

```
kubectl apply -f hive-metastore-core-config.yaml 
kubectl apply -f hive-metastore-site-config.yaml
```

Then initialize the HMS database:

```
kubectl apply -f hive-metastore-init-db.yaml 
kubectl wait --for=condition=complete job/init-hms-db 
```

If the job finishes successfully, it can be deleted (the job and the pod)


Now we can deploy hive-metastore and expose it. It will be available internally on `hive-metastore.default:9083`.

```
kubectl apply -f hive-metastore-deployment.yaml 

kubectl expose deployment/hive-metastore 
```

** TO:DO: Solo tiene una réplica. No sé si habría que hacerlo de otra forma.**

### 5. Install Trino

We will install Trino through its Helm charts.

The field `additionalCatalogs` in trino-values.yaml contains the configuration for the databases that Trino will access. Modify it if you need to add more data sources.

```
helm repo add trino https://trinodb.github.io/charts/
helm install trino -f trino-values.yaml ./trino
```

**TO DO when new  version of helm chart is released: I changed the helm charts to add a startup probe. When new version of trino helm
charts is released (v 8.0), we will be able to fix the problem of extremely long start-up time through the trino-values.yaml (I think)**

#### Access Trino

Install a pod to access the [Trino CLI](https://trino.io/docs/current/installation/cli.html)

```
kubectl apply -f trino-cli.yaml 
```

And then access it by running:

```
kubectl exec -it pod/trino-cli -- trino --server trino:8080
```

Trino comes with a [web UI](https://trino.io/docs/current/admin/web-interface.html) that allows to monitor the state of queries. In order to access it, find the pod with the pattern `trino-coordinator-xxxxxxxxxx-xxxxx` and port-forward it.

```
kubectl port-forward trino-coordinator-xxxxxxxxxx-xxxxx 8080:8080
```

You can log in with any username. 

The UI can be useful for accessing to the error logs when a query fails.

##### Check the connections

After starting the Trino CLI (`
kubectl exec -it pod/trino-cli -- trino --server trino:8080`), try these commands to check that connections were established successfully to the databases.

```
# Should see iceberg, minio and postgres
SHOW CATALOGS;

# If the connection is well configured, these will show some schemas 
# Otherwise, an error will be thrown
SHOW SCHEMAS FROM postgres;
SHOW SCHEMAS FROM minio;
SHOW SCHEMAS FROM iceberg;

```

##### Examples

Queries: https://trino.io/docs/current/sql.html

Create a table and add data in Postgres

```
CREATE TABLE postgres.public.people (
	id INTEGER,
	name VARCHAR(20),
	email VARCHAR(255));

INSERT INTO postgres.public.people VALUES(...)
```

Create a schema and an iceberg table:

```
# Create the schema: the bucket needs to exist in Minio
CREATE SCHEMA iceberg.trino WITH (location='s3a://trino/'); 

CREATE TABLE iceberg.trino.people (
	id INTEGER,
	name VARCHAR(20),
	email VARCHAR(255));

INSERT INTO iceberg.trino.people VALUES (...);
```

### 6. Install Argo Workflows

Install Argo on its own namespace, following the quickstart from [here](https://argoproj.github.io/argo-workflows/quick-start/).

```
kubectl create ns argo
kubectl apply -n argo -f argo.yaml
```

Create an admin 

```
kubectl create clusterrolebinding YOURNAME-cluster-admin-binding --clusterrole=cluster-admin --user=YOUREMAIL@email.com
```

Open a port forward to access the UI:

```
kubectl -n argo port-forward deployment/argo-server 2746:2746
```

Install [ARGO CLI](https://github.com/argoproj/argo-workflows/releases/tag/v3.3.1) and:

- Submit jobs:

	````
	argo submit -n argo --watch your-workflow.yaml
	````


- Check logs for latest job:

	```
	argo logs -n argo @latest
	```

- Terminate the job that is running:

  ```
  argo terminate -n argo @latest
  ```
You can also see all the pods by usingn `kubectl get pods` in the argo namespace. The logs are more complete through kubectl.

- [Delete](https://argoproj.github.io/argo-workflows/cli/argo_delete/) completed jobs

	```
	argo delete -n argo --completed
	```



### 7. Install Superset

```
kubectl create ns superset
helm repo add superset https://apache.github.io/superset
helm upgrade --install --values superset-values.yaml -n superset superset superset/superset
```

Even if you get a message saying "Error: failed post-install: timed out waiting for the condition", Superset will get installed eventually.

#### Access and connect to databases 

Access at localhost:8088 with user and password "admin".

```
kubectl port-forward service/superset 8088:8088 -n superset
```

**TO DO: Las conexiones a las bases de datos se podrían dejar hechas automáticamente**

You can connect to Postgres through a Postgres connection or through a Trino connection. 

The advantadge of connecting through Trino is that we can run queries across different databases. Moreover, if we only use Trino, we can manage access to the data in one central location.

##### Setting up a database connection with Trino

Go to Data > Databases and click on **+DATABASE** on the top right corner.

Select Trino from the drop the drop-down menu:

![superset-trino](img/superset-trino.png "Trino through Superset")


Fill in the name you want to give to the database and enter the SQLAlchemy url:


![superset-config-basic](img/superset-config-basic.png "Trino through Superset: basic configuration")


This is the format of the address: `trino://user:password@host:port/database/schema`.

Since we're using Trino without security, the user can be any string. The database name has to match the name of an existing Trino catalog (check with `SHOW CATALOGS;` through the Trino CLI. The 'schema' part is optional, and also has to match an existing schema in the selected catalog (check with `SHOW SCHEMAS FROM catalogName;`).

In the advanced config, check the boxes that will allow you to manipulate data through SQLab.

![superset-advanced-config](img/superset-config-advanced.png "Trino through Superset: advanced configuration")


### 8. Install Datahub

**NOTA: Yo no puedo he podido instalar Datahub, así que no lo he podido probar.**

Install prerequisites: 

```
kubectl create ns datahub
kubectl apply -f datahub-mysql-secret.yaml -n datahub
kubectl apply -f datahub-neo4j-secret.yaml -n datahub
helm repo add datahub https://helm.datahubproject.io/
helm install prerequisites datahub/datahub-prerequisites --values datahub-prerreq-values.yaml -n datahub
```
When they are installed (check in datahub namespace), install datahub:
``` 
helm install datahub datahub/datahub --values datahub-values.yaml -n datahub
```
