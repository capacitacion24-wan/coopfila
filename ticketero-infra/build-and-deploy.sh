#!/bin/bash
set -e

STACK_NAME=${1:-ticketero-dev}

echo "🔨 Building and deploying Ticketero application to $STACK_NAME"

# 1. Get ECR URI
ECR_URI=$(aws cloudformation describe-stacks --stack-name $STACK_NAME \
  --query 'Stacks[0].Outputs[?OutputKey==`EcrRepositoryUri`].OutputValue' --output text)

echo "📦 ECR Repository: $ECR_URI"

# 2. Login to ECR
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI

# 3. Build Docker image
echo "🏗️ Building Docker image..."
docker build -t $ECR_URI:latest ../

# 4. Push to ECR
echo "📤 Pushing to ECR..."
docker push $ECR_URI:latest

# 5. Update Telegram token (interactive)
echo "🤖 Please update Telegram Bot Token:"
echo "aws secretsmanager put-secret-value --secret-id $STACK_NAME-telegram-token --secret-string '{\"token\":\"YOUR_BOT_TOKEN\"}'"
read -p "Press Enter after updating the token..."

# 6. Force ECS deployment
echo "🔄 Forcing ECS service deployment..."
CLUSTER_NAME="$STACK_NAME-cluster"
SERVICE_NAME="$STACK_NAME-service"

aws ecs update-service \
  --cluster $CLUSTER_NAME \
  --service $SERVICE_NAME \
  --force-new-deployment

# 7. Wait for deployment
echo "⏳ Waiting for service to stabilize..."
aws ecs wait services-stable \
  --cluster $CLUSTER_NAME \
  --services $SERVICE_NAME

# 8. Test deployment
ALB_DNS=$(aws cloudformation describe-stacks --stack-name $STACK_NAME \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' --output text)

echo "🧪 Testing deployment..."
curl -f http://$ALB_DNS/actuator/health

echo ""
echo "✅ Deployment complete!"
echo "🌐 Application URL: http://$ALB_DNS"
echo "📊 Health Check: http://$ALB_DNS/actuator/health"