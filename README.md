# Open-source Big Data platform

This repository is associated to a series of blog posts where we build a general-purpose Big-Data platform running on Kubernetes. 

In those posts we explain the reasoning behind our choice of tools, and introduce some key technology concepts. In this repository we build a demo of the platform, and we also provide some examples of usage.

The demo runs on a Kind cluster, and tools are configured without security or with the minimal default security.

The blog posts (in Spanish) can be found here:
   - [Persistence](https://blog.arima.eu/2022/09/14/big-data-platform-1.html)

See the [deployment](deployment/) folder for instructions on how to install the tools on a Kubernetes cluster, and the [examples](examples/) folder for basic usage examples.

The [docker-images](docker-images/) folder contains the Dockerfiles used to prepare some  of the images used in the installation and example yamls.

## Current tools

- [Minio](https://min.io/) as object storage,
- [Postgres](https://www.postgresql.org/), for less resource consuming data projects, and as back-end database for other tools, and
- [Argo workflows](https://argoproj.github.io/argo-workflows/) as workflow manager.



