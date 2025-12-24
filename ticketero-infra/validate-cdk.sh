#!/bin/bash

#=============================================================================
# COOPFILA - CDK Validation Script
#=============================================================================
# Valida código CDK sin hacer deploy real
# Usage: ./validate-cdk.sh
#=============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}╔══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║                    CDK VALIDATION SUITE                     ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════════════════════════╝${NC}"
echo ""

ERRORS=0

#=============================================================================
# 1. JAVA COMPILATION
#=============================================================================
echo -e "${YELLOW}1. Validando compilación Java...${NC}"

if mvn clean compile -q; then
    echo -e "   ${GREEN}✓ Compilación exitosa${NC}"
else
    echo -e "   ${RED}✗ Error de compilación${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 2. CDK SYNTH (CloudFormation Generation)
#=============================================================================
echo -e "${YELLOW}2. Generando CloudFormation templates...${NC}"

if cdk synth --all > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓ CDK synth exitoso${NC}"
    
    # Verificar archivos generados
    if [ -d "cdk.out" ]; then
        TEMPLATES=$(find cdk.out -name "*.template.json" | wc -l)
        echo -e "   ${GREEN}✓ Templates generados: $TEMPLATES${NC}"
    fi
else
    echo -e "   ${RED}✗ Error en CDK synth${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 3. UNIT TESTS
#=============================================================================
echo -e "${YELLOW}3. Ejecutando tests unitarios...${NC}"

if mvn test -q; then
    echo -e "   ${GREEN}✓ Tests unitarios pasaron${NC}"
else
    echo -e "   ${RED}✗ Tests unitarios fallaron${NC}"
    ERRORS=$((ERRORS + 1))
fi

#=============================================================================
# 4. CDK DIFF (Dry Run)
#=============================================================================
echo -e "${YELLOW}4. Validando diferencias (dry-run)...${NC}"

# Crear contexto dummy para diff
export CDK_DEFAULT_ACCOUNT="123456789012"
export CDK_DEFAULT_REGION="us-east-1"

if cdk diff --all > /dev/null 2>&1; then
    echo -e "   ${GREEN}✓ CDK diff exitoso${NC}"
else
    echo -e "   ${YELLOW}⚠ CDK diff requiere credenciales AWS (normal)${NC}"
fi

#=============================================================================
# 5. CLOUDFORMATION VALIDATION
#=============================================================================
echo -e "${YELLOW}5. Validando templates CloudFormation...${NC}"

VALID_TEMPLATES=0
TOTAL_TEMPLATES=0

if [ -d "cdk.out" ]; then
    for template in cdk.out/*.template.json; do
        if [ -f "$template" ]; then
            TOTAL_TEMPLATES=$((TOTAL_TEMPLATES + 1))
            
            # Validar JSON syntax
            if jq empty "$template" 2>/dev/null; then
                VALID_TEMPLATES=$((VALID_TEMPLATES + 1))
            fi
        fi
    done
    
    if [ $VALID_TEMPLATES -eq $TOTAL_TEMPLATES ] && [ $TOTAL_TEMPLATES -gt 0 ]; then
        echo -e "   ${GREEN}✓ Templates CloudFormation válidos: $VALID_TEMPLATES/$TOTAL_TEMPLATES${NC}"
    else
        echo -e "   ${RED}✗ Templates inválidos: $VALID_TEMPLATES/$TOTAL_TEMPLATES${NC}"
        ERRORS=$((ERRORS + 1))
    fi
fi

#=============================================================================
# 6. SECURITY CHECKS
#=============================================================================
echo -e "${YELLOW}6. Verificando configuraciones de seguridad...${NC}"

# Buscar hardcoded secrets
if grep -r "AKIA\|aws_secret" src/ 2>/dev/null; then
    echo -e "   ${RED}✗ Posibles credenciales hardcodeadas encontradas${NC}"
    ERRORS=$((ERRORS + 1))
else
    echo -e "   ${GREEN}✓ No se encontraron credenciales hardcodeadas${NC}"
fi

# Verificar Security Groups restrictivos
if [ -d "cdk.out" ]; then
    OPEN_SG=$(grep -r "0.0.0.0/0" cdk.out/ 2>/dev/null | grep -v "443\|80" | wc -l)
    if [ $OPEN_SG -eq 0 ]; then
        echo -e "   ${GREEN}✓ Security Groups restrictivos${NC}"
    else
        echo -e "   ${YELLOW}⚠ $OPEN_SG reglas potencialmente abiertas${NC}"
    fi
fi

#=============================================================================
# 7. RESOURCE VALIDATION
#=============================================================================
echo -e "${YELLOW}7. Validando recursos AWS...${NC}"

if [ -d "cdk.out" ]; then
    # Contar recursos por tipo
    VPC_COUNT=$(grep -r "AWS::EC2::VPC" cdk.out/ 2>/dev/null | wc -l)
    RDS_COUNT=$(grep -r "AWS::RDS::DBInstance" cdk.out/ 2>/dev/null | wc -l)
    ECS_COUNT=$(grep -r "AWS::ECS::Service" cdk.out/ 2>/dev/null | wc -l)
    ALB_COUNT=$(grep -r "AWS::ElasticLoadBalancingV2::LoadBalancer" cdk.out/ 2>/dev/null | wc -l)
    
    echo -e "   ${GREEN}✓ Recursos detectados:${NC}"
    echo -e "     - VPC: $VPC_COUNT"
    echo -e "     - RDS: $RDS_COUNT" 
    echo -e "     - ECS: $ECS_COUNT"
    echo -e "     - ALB: $ALB_COUNT"
fi

#=============================================================================
# SUMMARY
#=============================================================================
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ VALIDACIÓN CDK EXITOSA - LISTO PARA DEPLOY${NC}"
    echo ""
    echo -e "${CYAN}Próximos pasos:${NC}"
    echo -e "  1. Configurar credenciales AWS: ${YELLOW}aws configure${NC}"
    echo -e "  2. Bootstrap CDK: ${YELLOW}cdk bootstrap${NC}"
    echo -e "  3. Deploy: ${YELLOW}cdk deploy --all${NC}"
else
    echo -e "${RED}❌ VALIDACIÓN FALLÓ - $ERRORS ERRORES ENCONTRADOS${NC}"
    echo ""
    echo -e "${CYAN}Revisar errores antes de continuar${NC}"
fi
echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"

exit $ERRORS