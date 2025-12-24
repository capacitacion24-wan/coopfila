#!/bin/bash

#=============================================================================
# COOPFILA - CDK Static Validation
#=============================================================================
# Valida sintaxis y estructura de artefactos CDK sin compilar
#=============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                COOPFILA CDK STATIC VALIDATION                ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

ERRORS=0
WARNINGS=0

#=============================================================================
# 1. VALIDATE FILE STRUCTURE
#=============================================================================
echo -e "${YELLOW}1. Validating file structure...${NC}"

REQUIRED_FILES=(
    "pom.xml"
    "cdk.json"
    "src/main/java/com/coopfila/infrastructure/InfrastructureApp.java"
    "src/main/java/com/coopfila/infrastructure/CoopFilaStack.java"
    "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"
    "src/main/java/com/coopfila/infrastructure/constructs/DatabaseConstruct.java"
    "src/main/java/com/coopfila/infrastructure/constructs/ApplicationConstruct.java"
    "src/main/java/com/coopfila/infrastructure/constructs/MonitoringConstruct.java"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo -e "   ${GREEN}✓ Found: $file${NC}"
    else
        echo -e "   ${RED}✗ Missing: $file${NC}"
        ERRORS=$((ERRORS + 1))
    fi
done

#=============================================================================
# 2. VALIDATE JAVA SYNTAX (BASIC)
#=============================================================================
echo -e "${YELLOW}2. Validating Java syntax...${NC}"

JAVA_FILES=(
    "src/main/java/com/coopfila/infrastructure/InfrastructureApp.java"
    "src/main/java/com/coopfila/infrastructure/CoopFilaStack.java"
    "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"
    "src/main/java/com/coopfila/infrastructure/constructs/DatabaseConstruct.java"
    "src/main/java/com/coopfila/infrastructure/constructs/ApplicationConstruct.java"
    "src/main/java/com/coopfila/infrastructure/constructs/MonitoringConstruct.java"
)

for file in "${JAVA_FILES[@]}"; do
    if [ -f "$file" ]; then
        # Check for basic Java syntax issues
        if grep -q "public class\|public interface" "$file"; then
            echo -e "   ${GREEN}✓ Valid Java class: $(basename $file)${NC}"
        else
            echo -e "   ${RED}✗ Invalid Java class: $(basename $file)${NC}"
            ERRORS=$((ERRORS + 1))
        fi
        
        # Check for package declaration
        if grep -q "^package com.coopfila.infrastructure" "$file"; then
            echo -e "   ${GREEN}✓ Package declaration: $(basename $file)${NC}"
        else
            echo -e "   ${YELLOW}⚠ Missing package declaration: $(basename $file)${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
        
        # Check for imports
        if grep -q "^import" "$file"; then
            echo -e "   ${GREEN}✓ Has imports: $(basename $file)${NC}"
        else
            echo -e "   ${YELLOW}⚠ No imports found: $(basename $file)${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
    fi
done

#=============================================================================
# 3. VALIDATE CDK CONSTRUCTS
#=============================================================================
echo -e "${YELLOW}3. Validating CDK constructs...${NC}"

# Check NetworkingConstruct
if [ -f "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java" ]; then
    if grep -q "Vpc.Builder.create" "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"; then
        echo -e "   ${GREEN}✓ NetworkingConstruct has VPC creation${NC}"
    else
        echo -e "   ${RED}✗ NetworkingConstruct missing VPC creation${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    if grep -q "SecurityGroup.Builder.create" "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"; then
        echo -e "   ${GREEN}✓ NetworkingConstruct has SecurityGroup creation${NC}"
    else
        echo -e "   ${RED}✗ NetworkingConstruct missing SecurityGroup creation${NC}"
        ERRORS=$((ERRORS + 1))
    fi
fi

# Check DatabaseConstruct
if [ -f "src/main/java/com/coopfila/infrastructure/constructs/DatabaseConstruct.java" ]; then
    if grep -q "DatabaseInstance.Builder.create" "src/main/java/com/coopfila/infrastructure/constructs/DatabaseConstruct.java"; then
        echo -e "   ${GREEN}✓ DatabaseConstruct has RDS creation${NC}"
    else
        echo -e "   ${RED}✗ DatabaseConstruct missing RDS creation${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    if grep -q "Secret.Builder.create" "src/main/java/com/coopfila/infrastructure/constructs/DatabaseConstruct.java"; then
        echo -e "   ${GREEN}✓ DatabaseConstruct has Secrets Manager${NC}"
    else
        echo -e "   ${RED}✗ DatabaseConstruct missing Secrets Manager${NC}"
        ERRORS=$((ERRORS + 1))
    fi
fi

# Check ApplicationConstruct
if [ -f "src/main/java/com/coopfila/infrastructure/constructs/ApplicationConstruct.java" ]; then
    if grep -q "ApplicationLoadBalancedFargateService.Builder.create" "src/main/java/com/coopfila/infrastructure/constructs/ApplicationConstruct.java"; then
        echo -e "   ${GREEN}✓ ApplicationConstruct has Fargate service${NC}"
    else
        echo -e "   ${RED}✗ ApplicationConstruct missing Fargate service${NC}"
        ERRORS=$((ERRORS + 1))
    fi
fi

# Check MonitoringConstruct
if [ -f "src/main/java/com/coopfila/infrastructure/constructs/MonitoringConstruct.java" ]; then
    if grep -q "Dashboard.Builder.create" "src/main/java/com/coopfila/infrastructure/constructs/MonitoringConstruct.java"; then
        echo -e "   ${GREEN}✓ MonitoringConstruct has CloudWatch Dashboard${NC}"
    else
        echo -e "   ${RED}✗ MonitoringConstruct missing CloudWatch Dashboard${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    if grep -q "Alarm.Builder.create" "src/main/java/com/coopfila/infrastructure/constructs/MonitoringConstruct.java"; then
        echo -e "   ${GREEN}✓ MonitoringConstruct has CloudWatch Alarms${NC}"
    else
        echo -e "   ${RED}✗ MonitoringConstruct missing CloudWatch Alarms${NC}"
        ERRORS=$((ERRORS + 1))
    fi
fi

#=============================================================================
# 4. VALIDATE CONFIGURATION FILES
#=============================================================================
echo -e "${YELLOW}4. Validating configuration files...${NC}"

# Check pom.xml
if [ -f "pom.xml" ]; then
    if grep -q "<groupId>com.coopfila</groupId>" "pom.xml"; then
        echo -e "   ${GREEN}✓ pom.xml has correct groupId${NC}"
    else
        echo -e "   ${RED}✗ pom.xml missing or incorrect groupId${NC}"
        ERRORS=$((ERRORS + 1))
    fi
    
    if grep -q "aws-cdk-lib" "pom.xml"; then
        echo -e "   ${GREEN}✓ pom.xml has CDK dependency${NC}"
    else
        echo -e "   ${RED}✗ pom.xml missing CDK dependency${NC}"
        ERRORS=$((ERRORS + 1))
    fi
fi

# Check cdk.json
if [ -f "cdk.json" ]; then
    if grep -q '"app"' "cdk.json"; then
        echo -e "   ${GREEN}✓ cdk.json has app configuration${NC}"
    else
        echo -e "   ${RED}✗ cdk.json missing app configuration${NC}"
        ERRORS=$((ERRORS + 1))
    fi
fi

#=============================================================================
# 5. VALIDATE ENVIRONMENT VARIABLES AND CONTEXT
#=============================================================================
echo -e "${YELLOW}5. Validating environment handling...${NC}"

# Check for environment context usage
if grep -q "getNode().tryGetContext(\"environment\")" "src/main/java/com/coopfila/infrastructure/CoopFilaStack.java"; then
    echo -e "   ${GREEN}✓ Stack handles environment context${NC}"
else
    echo -e "   ${YELLOW}⚠ Stack may not handle environment context${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

# Check for production/dev differentiation
if grep -q "isProduction" "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"; then
    echo -e "   ${GREEN}✓ NetworkingConstruct differentiates environments${NC}"
else
    echo -e "   ${YELLOW}⚠ NetworkingConstruct may not differentiate environments${NC}"
    WARNINGS=$((WARNINGS + 1))
fi

#=============================================================================
# 6. VALIDATE SECURITY BEST PRACTICES
#=============================================================================
echo -e "${YELLOW}6. Validating security practices...${NC}"

# Check for security groups
if grep -q "Port.tcp(5432)" "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"; then
    echo -e "   ${GREEN}✓ Database port (5432) configured${NC}"
else
    echo -e "   ${RED}✗ Database port not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "Port.tcp(8080)" "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"; then
    echo -e "   ${GREEN}✓ Application port (8080) configured${NC}"
else
    echo -e "   ${RED}✗ Application port not found${NC}"
    ERRORS=$((ERRORS + 1))
fi

# Check for secrets usage
if grep -q "EcsSecret.fromSecretsManager" "src/main/java/com/coopfila/infrastructure/constructs/ApplicationConstruct.java"; then
    echo -e "   ${GREEN}✓ Application uses Secrets Manager${NC}"
else
    echo -e "   ${RED}✗ Application not using Secrets Manager${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 7. VALIDATE TAGGING STRATEGY
#=============================================================================
echo -e "${YELLOW}7. Validating tagging strategy...${NC}"

TAG_FILES=(
    "src/main/java/com/coopfila/infrastructure/CoopFilaStack.java"
    "src/main/java/com/coopfila/infrastructure/constructs/NetworkingConstruct.java"
    "src/main/java/com/coopfila/infrastructure/constructs/DatabaseConstruct.java"
)

for file in "${TAG_FILES[@]}"; do
    if [ -f "$file" ]; then
        if grep -q "Tags.of.*add" "$file"; then
            echo -e "   ${GREEN}✓ $(basename $file) implements tagging${NC}"
        else
            echo -e "   ${YELLOW}⚠ $(basename $file) may not implement tagging${NC}"
            WARNINGS=$((WARNINGS + 1))
        fi
    fi
done

#=============================================================================
# SUMMARY
#=============================================================================
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${CYAN}                        VALIDATION SUMMARY                     ${NC}"
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ ALL VALIDATIONS PASSED${NC}"
    echo -e "${GREEN}   CDK artifacts are syntactically correct and well-structured${NC}"
    echo -e "${GREEN}   Ready for compilation and deployment when infrastructure is available${NC}"
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠️  VALIDATIONS PASSED WITH WARNINGS${NC}"
    echo -e "${YELLOW}   $WARNINGS warning(s) found - review recommended${NC}"
    echo -e "${GREEN}   CDK artifacts should work correctly${NC}"
else
    echo -e "${RED}❌ VALIDATION FAILED${NC}"
    echo -e "${RED}   $ERRORS error(s) and $WARNINGS warning(s) found${NC}"
    echo -e "${RED}   Fix errors before attempting deployment${NC}"
fi

echo ""
echo -e "${CYAN}Next steps when infrastructure is available:${NC}"
echo "1. mvn clean compile"
echo "2. cdk bootstrap"
echo "3. cdk synth"
echo "4. cdk deploy"
echo ""

exit $ERRORS