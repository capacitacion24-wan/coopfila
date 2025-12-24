#!/bin/bash
set -e

echo "🚀 Deploying Ticketero to AWS (Development)"

# 1. Bootstrap (primera vez)
export CDK_DEFAULT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
export CDK_DEFAULT_REGION=us-east-1
cdk bootstrap

# 2. Deploy dev
cdk deploy ticketero-dev --require-approval never

# 3. Get outputs
ECR_URI=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

ALB_DNS=$(aws cloudformation describe-stacks --stack-name ticketero-dev \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text)

echo "✅ Infrastructure deployed!"
echo "📦 ECR URI: $ECR_URI"
echo "🌐 ALB DNS: $ALB_DNS"
echo ""
echo "Next steps:"
echo "1. Build and push Docker image to ECR"
echo "2. Update Telegram Bot Token in Secrets Manager"
echo "3. Force ECS service deployment"