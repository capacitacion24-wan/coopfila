#!/bin/bash

#=============================================================================
# COOPFILA - CDK Deployment Script
#=============================================================================
# Usage: ./deploy.sh [environment] [image-uri]
# Examples:
#   ./deploy.sh dev
#   ./deploy.sh prod 123456789012.dkr.ecr.us-east-1.amazonaws.com/coopfila:v1.0.0
#=============================================================================

set -e

ENVIRONMENT=${1:-dev}
IMAGE_URI=${2:-coopfila:latest}

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    COOPFILA CDK DEPLOYMENT                   ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}Environment:${NC} $ENVIRONMENT"
echo -e "${YELLOW}Image URI:${NC} $IMAGE_URI"
echo ""

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(dev|staging|prod)$ ]]; then
    echo -e "${RED}Error: Environment must be 'dev', 'staging', or 'prod'${NC}"
    exit 1
fi

# Check if AWS CLI is configured
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    echo -e "${RED}Error: AWS CLI not configured. Run 'aws configure' first.${NC}"
    exit 1
fi

# Get AWS account and region
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=$(aws configure get region || echo "us-east-1")

echo -e "${YELLOW}AWS Account:${NC} $AWS_ACCOUNT"
echo -e "${YELLOW}AWS Region:${NC} $AWS_REGION"
echo ""

# Bootstrap CDK if needed
echo -e "${YELLOW}1. Checking CDK bootstrap...${NC}"
if ! aws cloudformation describe-stacks --stack-name CDKToolkit --region $AWS_REGION > /dev/null 2>&1; then
    echo "   Bootstrapping CDK..."
    cdk bootstrap aws://$AWS_ACCOUNT/$AWS_REGION
else
    echo "   ✓ CDK already bootstrapped"
fi

# Compile the project
echo -e "${YELLOW}2. Compiling CDK project...${NC}"
mvn clean compile -q

# Synthesize CloudFormation template
echo -e "${YELLOW}3. Synthesizing CloudFormation template...${NC}"
cdk synth \
    --context environment=$ENVIRONMENT \
    --context imageUri=$IMAGE_URI \
    --context account=$AWS_ACCOUNT \
    --context region=$AWS_REGION

# Deploy the stack
echo -e "${YELLOW}4. Deploying stack...${NC}"
cdk deploy CoopFila-$ENVIRONMENT \
    --context environment=$ENVIRONMENT \
    --context imageUri=$IMAGE_URI \
    --context account=$AWS_ACCOUNT \
    --context region=$AWS_REGION \
    --require-approval never

echo ""
echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
echo ""

# Get stack outputs
echo -e "${YELLOW}Stack Outputs:${NC}"
aws cloudformation describe-stacks \
    --stack-name CoopFila-$ENVIRONMENT \
    --region $AWS_REGION \
    --query 'Stacks[0].Outputs[?OutputKey!=`ExportsOutputRefApplicationFargateServiceLoadBalancerDNS`].[OutputKey,OutputValue]' \
    --output table

echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                         NEXT STEPS                            ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "1. Update Telegram bot token in AWS Secrets Manager"
echo "2. Update database connection string in application secrets"
echo "3. Configure DNS (if using custom domain)"
echo "4. Set up monitoring alerts"
echo ""
echo -e "${YELLOW}Useful commands:${NC}"
echo "  View logs: aws logs tail /ecs/coopfila-$ENVIRONMENT --follow"
echo "  Scale service: aws ecs update-service --cluster coopfila-$ENVIRONMENT --service coopfila-$ENVIRONMENT --desired-count 3"
echo "  Destroy stack: cdk destroy CoopFila-$ENVIRONMENT"
echo ""