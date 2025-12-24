#!/bin/bash

# Ejecutar test con configuración específica para H2
export DATABASE_URL="jdbc:h2:mem:testdb"
export DATABASE_USERNAME="sa"
export DATABASE_PASSWORD=""

mvn test -Dtest="FunctionalFlowTest#flujoCompleto_crearClienteYTicket_debeCompletarseCorrectamente" -Dspring.profiles.active=test