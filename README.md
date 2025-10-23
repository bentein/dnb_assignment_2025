# DNB Assignment

This repository contains everything needed to run and host a rudimentary loan
application microservice on an EKS cluster on an AWS account.

## Prerequisites

In order to run the project successfully, one needs the following CLIs:
* curl
* aws
* eksctl
* kubectl
* helm
* docker
* openssl

Other requirements include an AWS account with credits/a payment plan, and AWS
credentials configured in the user's terminal.

## Setup

The application can be run by following these steps:
1. Using the `template.yaml` file, create a CloudFormation stack in an AWS
   environment.
2. When the stack has successfully been created, run `sh init_cluster.sh
   ${aws_region} ${cluster_name}`, substituting the two variables with
   appropriate values, which generates a file `manifest.yaml`.
3. Run `kubectl apply -f manifest.yaml`.

Note that docker is run with sudo in the `init_cluster.sh` script. Edit the
script if this is undesirable.

## Usage

A few minutes after the manifest has been applied, an ingress will be
provisioned. This is the entrypoint of the service. The ingress' url can be
found by running `kubectl get ingress`. Note the url.

The create application endpoint can be invoked by running the following
command:
```sh
curl -d '{"ssn": "10101000000", "givenName": "Ola", "surname": "Nordmann", "equity": "1000000", "salary": "500000", "amount": "2500000"}' -u customer:password -H 'Content-Type: application/json' -k https://${url}/loan-applications
```
Substitue the variables with appropriate values.

After a loan application has been created, the application id is returned. To
invoke the manage application endpoint, run the following command:
```sh
curl -d '{"action":"APPROVE"}' -u customer:password -H 'Content-Type: application/json' -k https://${url}/loan-applications/${loan_application_uuid}
```
Again, substitue the variables with appropriate values. The value of the
`action` field in the request body can be set to either `APPROVE` or `REJECT`.

## Infrastructure Description

The `template.yaml` file describes a CloudFormation stack that spins up a basic
EKS cluster. Since the cluster is a dev cluster meant to be used as a demo, it
does not follow all best practices. The cluster's nodes run on private subnets
spread over availability zones. These subnets connect to the internet through a
single NAT gateway. Two public subnets in different AZs are created for the
cluster's ingresses. An ECR repository is created for kubernetes to pull images
from, and a DynamoDB table is created to persist loan applications.

The `init_cluster.sh` sets up the cluster with resources that could not be
directly provisioned in CloudFormation. The AWS load balance controller is used
to automatically provision Application Load Balancers for the cluster's
ingress(es). A service account is created for this purpose. Helm is used to
install the controller. 

Docker is then used to build the application image, using a two-stage build and
a rootless minimal image as the base image. A hardened base image should ideally
be used. The image is tagged and pushed to the provisioned ECR repository.

A TLS certificate is generated and imported to ACM. This certificate is
self-signed, and will trigger warnings in http clients. Http clients must be
configured to accept self-signed certificates to run this demo, ie. by using
curl's `-k` flag, as in the run instructions above.

### Security Considerations

Since this is a demo cluster, it is not configured to be as secure as a
production cluster, ie. public API endpoint access is enabled. 

TLS is enabled on the load balancer level, meaning requests are encrypted while in
flight to the cluster. Only TLS traffic is accepted. Intra-cluster
communication, however, is unencrypted, as TLS is terminated on the load
balancer. The cluster could be customized to use mTLS to encrypt this traffic as
well, but that is out of scope. One has to consider the potential attack vectors
mitigated by mTLS. Only third parties without access to the hardware itself
would be foiled. The host itself can still access the unencrypted memory of the
underlying hardware, so if total protection of Personally Identifiable
Information is the intent, mTLS needs to be coupled with confidential computing,
which is also out of scope. 

All that is to say, terminating TLS at the ingress is a servicable solution.
Intra-cluster TLS does not mitigate the biggest concern when handling PII in
the cloud, which is not third party attackers, but the platform owner itself.

IAM roles for service accounts is used to provide credentials for the service.
This avoids putting secrets in the cluster itself.

## Code Description

The loan application service itself is named LoanService, and is a barebones
Spring Boot application. The code is structured in the standard Spring 3-layer
model of Controller/Service/Repository. It has two endpoints, one for users to
create applications, and one for advisors to manage applications. Both endpoints
use role based method security, stopping users who lack the required role from
performing actions they should not be able to perform. The service has been
configured with basic auth for simplicity's sake, but an OIDC provider and and
access token could be used instead, ie. JWT with a role claim.

When a request arrives, it is immediately used to update a dynamodb table
containing users' loan applications. It is assigned a randomly generated UUID,
as users might be able to apply for multiple or different kinds of loans.
Dynamodb was selected as it's simple to provision.

The endpoints follow a RESTful design, utilizing HTTP verbs and the
`/resources/{id}` url pattern.

### Security Considerations

SSNs are represented by its own type which masks its value when toString() is
invoked on it. The type can be deserialized directly from a String. This keeps
the SSN from accidentally appearing in logs, so long as it is kept in its
wrapper.

SSNs are persisted without being encrypted or otherwise obfuscated. The SSN
needs to be searchable in a theoretical fully fledged system. Whether SSNs
should be encrypted at rest depends on regulation and internal requirements.
Relatedly, SSNs are encrypted up to, but not after, the cluster ingress. 

Certain exceptions have been caught and given custom error responses. Care has
been taken to avoid exposing internals in the error response messages.

## Automation Description

CloudFormation was picked as the orchestration tool for this demo. Terraform
could've been used instead, however, using Terraform would confer no real
benefit for this simple scenario. While using kubernetes is (largely) a cloud
agnostic choice, it is clear that some vendor lock-in is inevitable if one wants
to follow EKS best practices. This goes for Terraform as well.

Initializing the cluster using sh was the simplest alternative to make the demo
easily runnable for you, the reader. The cluster needs two service accounts, two
external or dynamic IAM policies, to install AWS load balancer controller, to
build and push a service image, to generate and import a TLS certificate, and to
build dynamic manifests. 

## Cleanup

To cleanly tear down the demo, one must complete the following steps in order.
1. Delete TLS certificate from ACM.
2. Delete the service-repo ECR repository.
3. Delete the loan-applications dynamodb table.
4. Delete the AWS load balancer controller-generated Application Load Balancer.
5. Delete the two kubectl-generated CloudFormation stacks.
6. Delete the two IAM policies `AWSLoadBalancerControllerIAMPolicy` and
   `LoanServiceDynamoDbPolicy`.
7. Delete the kubectl-generated OIDC identity providers.
8. Delete the initial CloudFormation stack.

If these steps are followed, most resources should be gone, and the initial
CloudFormation stack should delete cleanly.

## Things Left Out

* There are very few configurable values in the project. The ones present are
mainly for demonstration purposes.
* Monitoring.
* The rest of this section.
