- [1. Post 1: setting up some basic tools](#1-post-1-setting-up-some-basic-tools)
  - [1.1. Example 1: Using Argo workflows to load raw data into MinIO](#11-example-1-using-argo-workflows-to-load-raw-data-into-minio)
  - [1.2. Example 2: Loading part of the data to PostgreSQL](#12-example-2-loading-part-of-the-data-to-postgresql)
  - [1.3. Complete example: downloading the raw data and sending it to Postgres](#13-complete-example-downloading-the-raw-data-and-sending-it-to-postgres)

# 1. Post 1: setting up some basic tools

We have three example workflows. One of them ([1-download-and-send-to-postgres.yaml](1-download-and-send-to-postgres.yaml)) downloads all the data to MinIO and then loads it to Postgres. Since it is a very long and complicated workflow, we have split it in two simplified workflows that can be run separately: one for downloading the data, and one for retrieving part of it and sending it to postgres.

We will be using many of Argo Workflows features, but not all. Their Github repo provides a good set of examples to showcase most of its features [here](https://github.com/argoproj/argo-workflows/tree/master/examples).

## 1.1. Example 1: Using Argo workflows to load raw data into MinIO

We will be using [NOAA Global Historical Climatology Network Daily](https://registry.opendata.aws/noaa-ghcn/), a dataset of global weather observations dating back to the 18th century. Documentation is available [here](https://github.com/awslabs/open-data-docs/tree/main/docs/noaa/noaa-ghcn).

We will download the data from its Amazon S3 bucket and send it to our MinIO instance. 

The workflow `download-raw-data.yaml` can be run with parameters, to indicate which decade span one wishes to download. For instance, the following command will download all the files for the years [1910,1911,..,1958,1959] if they are present. 

We've configured the workflow to download each decade separately and in parallel.

This workflow will also download information about the stations where the weather observations are registered, as well as a table of country codes.

```
argo submit -n argo --watch 1-download-raw-data.yaml -p start-decade=191 -p end-decade=195
```

It is an example of a workflow that uses input parameters, parallelism and the [script](https://argoproj.github.io/argo-workflows/workflow-concepts/#script) template definition. This option allows us to define simple jobs without the need to create an image; it will be generated for us from the code we pass.



Paths indicated in artifacts are relative to MinIO bucket `argo-workflows`. We've configured it like this in `argo.yaml`. 
- It will download data from 1910s to 1950s. Can take a long time to download, especially the 1950s decade (10-15min). Putting everything in MinIO will also take some time. Later decades contain more data, so they will take even longer to download.
- Also download station info and country info.

## 1.2. Example 2: Loading part of the data to PostgreSQL

Using some of the data we downloaded earlier, we will send it to postgres.

```
argo submit -n argo --watch 1-station-data-to-postgres.yaml
```

## 1.3. Complete example: downloading the raw data and sending it to Postgres

This example showcases a complete workflow. In addition to the features shown in the examples above, it containes embedded DAGs, and it shows how to use artifacts created in one step as input to another one. It can be submitted as before:

```
argo submit -n argo --watch 1-station-data-to-postgres.yaml
```

Be aware that it can take a long time to run, especially if you download many decades or very recent decades.

If you port-forward Argo Workflows, you will be able to follow the workflow activity as it unfolds, and see the graphical representation of the DAG.

![argo-ui](img/argo-ui.png)