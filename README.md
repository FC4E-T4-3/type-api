# TypeAPI

![Java](https://img.shields.io/badge/Java-11+-blue.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.6.3-brightgreen.svg) ![Type Sense](https://img.shields.io/badge/TypeSense-25.1-red.svg)


## Table of Contents
- [Introduction](#introduction)
- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)

## Introduction
The **DTR Toolkit** is a framework designed to work with Data Type Registries (DTRs). It provides functionalities for extracting, validating, and managing types, units, and taxonomies from various DTR sources. 

## Features
- **Type Extraction**: Extract types from DTRs of different providers.
- **Validation**: Generate and validate schemas for different types describing schema elements.
- **Taxonomy Management**: Manage and retrieve taxonomies.
- **Unit Handling**: Extract and manage unit entities.
- **RESTful API**: Provides a RESTful API for interaction.

## Installation

### Prerequisites
- Java 11 or higher
- Gradle 6.8 or higher
- A TypeSense 25.1 instance or higher

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/dtr-toolkit.git
   cd dtr-toolkit
2. Start a [TypeSense](https://typesense.org) server. The **TypeAPI** requires a TypeSense server running to store types, units and taxonomies and provide fast search capabilities. The easiest way to set this up is via docker.

```docker run -d --name typesense-server \
  -p 8108:8108 \
  -v /path/to/data:/data \
  typesense/typesense:latest \
  --data-dir /data \
  --api-key xyz \
  --listen-port 8108
```

These are the default settings for port and key. If you wish to choose your own you need to adjust the `src/main/resources/application.properties`file:

```
typesense.url=localhost
typesense.port=8108
typesense.key=xyz
```

3. Build and run the project:
```
./gradlew buildRun
```

### Via Docker Compose
You can also simply use the provided `docker-compose.yml` file to run the service and a typesense instance. The docker image for the TypeAPI is stored in the GitHub Container Registry: `ghcr.io/fc4e-t4-3/dtr-toolkit:latest`. So if you want a simple setup, just run `docker-compose up`in the same folder as the `docker-compose.yml` to start the containers.
## Usage
Once the application is running, you can access the Swagger UI at http://localhost:8080.

## Configuration
The Data Type Registries that are imported are defined in`src/main/config/config.toml`, for example:
```[Typeregistry-EOSC]
url="http://typeregistry.lab.pidconsortium.net/"
suffix="objects?query=*"
style="eosc"
types=["BasicInfoType","InfoType","KernelInformationProfile"]
units=["MeasurementUnit"]
taxonomy=["TaxonomyNode"]
general=["ExtendedMimeType"]
```
Where the `style`describes the underlying schema definitions, `types` contains the names of the schema element types, `units` the name of the measurement units, `taxonomy` the name of the TaxonomyNode schema and under `general` other types can be imported that don't fit other categories. 

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
