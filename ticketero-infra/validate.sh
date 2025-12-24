#!/bin/bash

#=============================================================================
# COOPFILA - CDK Docker Validation
#=============================================================================
# Valida código CDK usando Docker
# Usage: ./validate.sh
#=============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                CDK DOCKER VALIDATION SUITE                  ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Build and run validation container
echo -e "${YELLOW}1. Construyendo imagen de validación...${NC}"
docker-compose -f docker-compose.validate.yml build cdk-validator

echo -e "${YELLOW}2. Ejecutando validaciones CDK...${NC}"
docker-compose -f docker-compose.validate.yml run --rm cdk-validator

echo ""
echo -e "${GREEN}✅ Validación CDK completada${NC}"
echo ""
echo -e "${CYAN}Para desarrollo interactivo:${NC}"
echo -e "  ${YELLOW}docker-compose -f docker-compose.validate.yml run --rm cdk-dev bash${NC}"