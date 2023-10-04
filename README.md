# DTR-Toolkit
This repository contains the DTR-toolkit. It will provide functionalities to further work with the EOSC DTR, for example generating JSON validation schemas from types and search types from different external registries.

## Description



### Endpoints

#### Description Endpoint

Returns the description of a type. Supports JSON and XMl.

>URL:  
/v1/desc/{prefix}/{suffix}  

>HTTP METHOD:  
GET

>PARAMETER:  
{prefix}: The prefix of the type identifier.  
{suffix}: The suffix of the type identifier.  
refresh (optinal): If the requested PID should be refreshed in the cache.

#### Validation Endpoint

Returns the validation schema for a type

>URL:  
/v1/schema/{prefix}/{suffix}

>HTTP METHOD:  
GET

>PARAMETER:  
{prefix}: The prefix of the type identifier.  
{suffix}: The suffix of the type identifier.  
refresh (optional): If the request PID should be refreshed in the cache.

#### Resolve Endpoint
Given a digital object where fields are described by PID's, resolve that type into human readable form by replacing PID's with 
explainable content.

>URL:  
/v1/resolve/{prefix}/{suffix}

>HTTP METHOD:  
GET

>PARAMETER:  
{prefix}: The prefix of the type identifier.  
{suffix}: The suffix of the type identifier.  
depth (optional): True if the subfields of the type should be resolved as well and not just the first layer. 

#### Validate Endpoint

Validates an object against a registered type.

>URL:  
/v1/validate/{prefix}/{suffix}/{prefixObject}/{suffixObject}

>HTTP METHOD:  
GET

>PARAMETER:  
{prefix}: The prefix of the type identifier.  
{suffix}: The suffix of the type identifier.  
{prefixObject}: The prefix part of the object identifier.  
{suffixObject}: The suffix part of the object identifier.

#### Search Endpoint

Search for objects based on query and fields

>URL:  
/v1/search/?query={query}

>HTTP METHOD:  
GET

>PARAMETER:  
{query}: The query name.  
queryby (optional): An array of fields to search within (default: name, authors, desc).  
infix (optional): True if infix search should be performed (default: false).  


## Installation

### Dependencies

<dl>
    <dt>Spring Framework</dt>
    <dd>For handeling HTTP requests and responses.</dd>
    <dt>Jackson</dt>
    <dd>For working with JSON data.</dd>
    <dt>Underscore-java</dt>
    <dd>For converting JSON to XML.</dd>
</dl>

## Example Usage

#### Description Endpoint

>Retrieve JSON:  
/v1/desc/this-prefix/this-suffix

#### Validation Endpoint

> Retrieve JSON-Validation scheam:  
/v1/schema/this-prefix/this-suffix


#### Resolve Endpoint

>Reslove JSON:  
/v1/resolve/this-prefix/this-suffix


#### Validate Endpoint

>Validate object:  
/v1/validate/this-prefix/this-suffix/object-prefix/object-suffix


#### Search Endpoint

>Search with default fields:  
/v1/search/?query=this-query

>Search with custom fields:  
/v1/search/?query=this-query&queryBy=name,author



## Licence
This code is released under the MIT License.


## Contributing
Contributions to this project are welcome. If you'd like to contribute, please follow the standard GitHub fork and pull request workflow.

