# Open-source Big Data platform

A Big Data platform running on Kubernetes.

## Tools

- [Minio](https://min.io/) as object storage,
- [Argo workflows](https://argoproj.github.io/argo-workflows/) as workflow manager
- Databases:

   - [Postgres](https://www.postgresql.org/), for less resource consuming data projects,
   - [Iceberg](https://iceberg.apache.org/) table format on minio, to build a data lake,
   - [Pinot](https://pinot.apache.org/)/[Druid](https://druid.apache.org/) for OLAP needs (**NOT YET AVAILABLE**);
- [Trino](https://trino.io/) as database query engine and data access control;
- [Superset](https://superset.apache.org/) as data analysis and visualization tool, and
- [Datahub](https://datahubproject.io/) as data discovery and governance tool.


## Current state:

- no security
- storage: distributed Minio with 8Gb memory only

## Installation

See the `deployment` folder for instructions on how to install the tools on a Kubernetes cluster.

See the `data-project` folder for instructions on how to test the tool by loading some data.


