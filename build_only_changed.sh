#!/bin/bash

#mvn clean compile -pl \!deegree-spring,\!deegree-workspaces,\!deegree-themes,\!deegree-datastores,\!deegree-misc,\!deegree-layers,\!deegree-client,\!deegree-processproviders,\!deegree-tools,\!org.deegree:deegree-webservices -DskipTests

mvn clean deploy -DskipTests -pl org.deegree:deegree-services-wps,org.deegree:deegree-protocol-csw,org.deegree:deegree-protocol-commons
