# rbac-client

A Clojure library designed to hold lightweight API clients for PE services.

## Usage

The clients are meant to provide alternate versions of the TK services.
You should be able to merely include them in the bootstrap.

### Configuration

The clients use ssl authorization via the global.certs keys.

- `global.certs.ssl-key`: the key for this clients identity.
- `global.certs.ssl-cert`: the cert for this clients identity.
- `global.certs.ssl-ca-cert`: the ca-cert for this clients cert and the upstream service.

The location of the RBAC and Activity services are configured with the
`rbac-consumer.api-url` and `activity-consumer.api.url` settings respectivetly.

### Activity

The Activity service protocol should be considered temporary and unstable. It may not
directly match the Activity Reporting service protocol, which may itself be unstable.

## License

Copyright © 2016 Puppet

Distributed under the Apache License version 2.0
