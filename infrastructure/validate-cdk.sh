#!/bin/bash

#=============================================================================
# COOPFILA - CDK Validation Script
#=============================================================================
# Valida que los artefactos CDK sean correctos sin hacer deployment
#=============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    COOPFILA CDK VALIDATION                   ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

ERRORS=0

#=============================================================================
# 1. VALIDATE JAVA COMPILATION
#=============================================================================
echo -e "${YELLOW}1. Validating Java compilation...${NC}"
if mvn clean compile -q; then
    echo -e "   ${GREEN}✓ Java compilation successful${NC}"
else
    echo -e "   ${RED}✗ Java compilation failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 2. VALIDATE CDK SYNTHESIS
#=============================================================================
echo -e "${YELLOW}2. Validating CDK synthesis...${NC}"

# Test dev environment
if cdk synth CoopFila-dev \
    --context environment=dev \
    --context imageUri=coopfila:latest \
    --context account=123456789012 \
    --context region=us-east-1 > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓ CDK synthesis (dev) successful${NC}"
else
    echo -e "   ${RED}✗ CDK synthesis (dev) failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Test prod environment
if cdk synth CoopFila-prod \
    --context environment=prod \
    --context imageUri=123456789012.dkr.ecr.us-east-1.amazonaws.com/coopfila:v1.0.0 \
    --context account=123456789012 \
    --context region=us-east-1 > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓ CDK synthesis (prod) successful${NC}"
else
    echo -e "   ${RED}✗ CDK synthesis (prod) failed${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 3. VALIDATE CLOUDFORMATION TEMPLATES
#=============================================================================
echo -e "${YELLOW}3. Validating CloudFormation templates...${NC}"

# Generate templates
cdk synth CoopFila-dev \
    --context environment=dev \
    --context imageUri=coopfila:latest \
    --context account=123456789012 \
    --context region=us-east-1 \
    --output cdk.out > /dev/null 2>&1

if [ -f "cdk.out/CoopFila-dev.template.json" ]; then
    echo -e "   ${GREEN}✓ CloudFormation template generated${NC}"
    
    # Check template size
    TEMPLATE_SIZE=$(wc -c < "cdk.out/CoopFila-dev.template.json")
    if [ $TEMPLATE_SIZE -gt 51200 ]; then  # 50KB limit
        echo -e "   ${YELLOW}⚠ Template size: ${TEMPLATE_SIZE} bytes (>50KB)${NC}"
    else
        echo -e "   ${GREEN}✓ Template size: ${TEMPLATE_SIZE} bytes${NC}"
    fi
    
    # Check for required resources
    REQUIRED_RESOURCES=("AWS::EC2::VPC" "AWS::RDS::DBInstance" "AWS::ECS::Service" "AWS::ElasticLoadBalancingV2::LoadBalancer")
    for resource in "${REQUIRED_RESOURCES[@]}"; do
        if grep -q "$resource" "cdk.out/CoopFila-dev.template.json"; then
            echo -e "   ${GREEN}✓ Found resource: $resource${NC}"
        else
            echo -e "   ${RED}✗ Missing resource: $resource${NC}"
            ERRORS=$((ERRORS + 1))
        fi
    done
else
    echo -e "   ${RED}✗ CloudFormation template not generated${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 4. VALIDATE SECURITY GROUPS
#=============================================================================
echo -e "${YELLOW}4. Validating security group configurations...${NC}"

if grep -q "Port.tcp(5432)" cdk.out/CoopFila-dev.template.json; then
    echo -e "   ${GREEN}✓ Database port (5432) configured${NC}"
else
    echo -e "   ${RED}✗ Database port not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "Port.tcp(8080)" cdk.out/CoopFila-dev.template.json; then
    echo -e "   ${GREEN}✓ Application port (8080) configured${NC}"
else
    echo -e "   ${RED}✗ Application port not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 5. VALIDATE ENVIRONMENT VARIABLES
#=============================================================================
echo -e "${YELLOW}5. Validating environment variables...${NC}"

REQUIRED_ENV_VARS=("SPRING_PROFILES_ACTIVE" "SERVER_PORT" "SPRING_DATASOURCE_URL")
for env_var in "${REQUIRED_ENV_VARS[@]}"; do
    if grep -q "$env_var" cdk.out/CoopFila-dev.template.json; then
        echo -e "   ${GREEN}✓ Environment variable: $env_var${NC}"
    else
        echo -e "   ${RED}✗ Missing environment variable: $env_var${NC}"
        ERRORS=$((ERRORS + 1))
    fi
done

#=============================================================================
# 6. VALIDATE SECRETS CONFIGURATION
#=============================================================================
echo -e "${YELLOW}6. Validating secrets configuration...${NC}"

if grep -q "AWS::SecretsManager::Secret" cdk.out/CoopFila-dev.template.json; then
    echo -e "   ${GREEN}✓ Secrets Manager configured${NC}"
else
    echo -e "   ${RED}✗ Secrets Manager not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 7. VALIDATE MONITORING SETUP
#=============================================================================
echo -e "${YELLOW}7. Validating monitoring setup...${NC}"

if grep -q "AWS::CloudWatch::Dashboard" cdk.out/CoopFila-dev.template.json; then
    echo -e "   ${GREEN}✓ CloudWatch Dashboard configured${NC}"
else
    echo -e "   ${RED}✗ CloudWatch Dashboard not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "AWS::CloudWatch::Alarm" cdk.out/CoopFila-dev.template.json; then
    echo -e "   ${GREEN}✓ CloudWatch Alarms configured${NC}"
else
    echo -e "   ${RED}✗ CloudWatch Alarms not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 8. VALIDATE RESOURCE TAGS
#=============================================================================
echo -e "${YELLOW}8. Validating resource tags...${NC}"

if grep -q "Project.*CoopFila" cdk.out/CoopFila-dev.template.json; then
    echo -e "   ${GREEN}✓ Project tags configured${NC}"
else
    echo -e "   ${RED}✗ Project tags not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# SUMMARY
#=============================================================================
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                        VALIDATION SUMMARY                     ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ ALL VALIDATIONS PASSED${NC}"
    echo -e "${GREEN}   CDK artifacts are ready for deployment${NC}"
    exit 0
else
    echo -e "${RED}❌ $ERRORS VALIDATION(S) FAILED${NC}"
    echo -e "${RED}   Fix issues before deployment${NC}"
    exit 1
fi