#!/usr/bin/env sh

AWS_REGION="$1"
CLUSTER_NAME="$2"

if [ -z "$AWS_REGION" ] || [ -z "$CLUSTER_NAME" ]; then
    echo "Usage: $0 <aws-region> <cluster-name>"
    exit 1
fi

AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)
VPC_ID=$(aws eks describe-cluster --name ${CLUSTER_NAME} --region ${AWS_REGION} | grep RESOURCESVPCCONFIG | grep -o 'vpc-[a-z0-9]*')

eksctl utils associate-iam-oidc-provider \
    --region ${AWS_REGION} \
    --cluster ${CLUSTER_NAME} \
    --approve

curl -O https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.14.0/docs/install/iam_policy.json

aws iam create-policy \
    --policy-name AWSLoadBalancerControllerIAMPolicy \
    --policy-document file://iam_policy.json

rm ./iam_policy.json

eksctl create iamserviceaccount \
    --cluster=${CLUSTER_NAME} \
    --namespace=kube-system \
    --name=aws-load-balancer-controller \
    --attach-policy-arn=arn:aws:iam::${AWS_ACCOUNT_ID}:policy/AWSLoadBalancerControllerIAMPolicy \
    --override-existing-serviceaccounts \
    --region ${AWS_REGION} \
    --approve

aws eks update-kubeconfig --region ${AWS_REGION} --name ${CLUSTER_NAME}

helm repo add eks https://aws.github.io/eks-charts
helm repo update eks

helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
    --set clusterName=${CLUSTER_NAME} \
    -n kube-system \
    --set serviceAccount.create=false \
    --set serviceAccount.name=aws-load-balancer-controller \
    --set region=${AWS_REGION} \
    --set vpcId=${VPC_ID}

aws ecr get-login-password --region ${AWS_REGION} | sudo docker login --username AWS --password-stdin ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
sudo docker build -t loan-service ./loan-service
sudo docker tag loan-service:latest ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/service-repo:latest
sudo docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/service-repo:latest

openssl req -x509 -nodes -days 365 \
    -newkey rsa:2048 \
    -keyout tls.key \
    -out tls.crt \
    -subj "/CN=bentein.dev/O=Test"

CERTIFICATE_ARN=$(aws acm import-certificate \
    --certificate fileb://tls.crt \
    --private-key fileb://tls.key)

rm ./tls.crt ./tls.key

cat <<EOF> dynamodb_policy.json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "dynamodb:BatchGetItem",
                "dynamodb:GetItem",
                "dynamodb:Query",
                "dynamodb:Scan",
                "dynamodb:PutItem",
                "dynamodb:UpdateItem",
                "dynamodb:DeleteItem"
            ],
            "Resource": "arn:aws:dynamodb:eu-north-1:${AWS_ACCOUNT_ID}:table/loan-applications"
        }
    ]
}
EOF

aws iam create-policy \
    --policy-name LoanServiceDynamoDbPolicy \
    --policy-document file://dynamodb_policy.json

rm ./dynamodb_policy.json

DYNAMODB_SA_NAME=loan-service-sa 

eksctl create iamserviceaccount \
    --cluster ${CLUSTER_NAME} \
    --namespace default \
    --name ${DYNAMODB_SA_NAME} \
    --attach-policy-arn arn:aws:iam::${AWS_ACCOUNT_ID}:policy/LoanServiceDynamoDbPolicy \
    --override-existing-serviceaccounts \
    --region ${AWS_REGION} \
    --approve

cat <<EOF> manifest.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: loan-service
  labels:
    app: loan-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: loan-service
  template:
    metadata:
      labels:
        app: loan-service
    spec:
      serviceAccountName: ${DYNAMODB_SA_NAME} 
      containers:
        - name: loan-service
          image: ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/service-repo:latest
          ports:
            - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: loan-service
spec:
  type: ClusterIP
  selector:
    app: loan-service
  ports:
    - port: 80
      targetPort: 8080
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: loan-service-ingress
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/certificate-arn: ${CERTIFICATE_ARN}
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTPS":443}]'
spec:
  ingressClassName: alb
  rules:
    - http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: loan-service
                port:
                  number: 80
EOF
