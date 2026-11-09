#!/bin/sh
# Smoke test of an FOR-IDENT instance after a deployment (SP-08).
# Only read-only HTTP GET requests; no login, no uploads.
#
#   dev/smoke-test.sh https://for-ident.example.org
#   dev/smoke-test.sh http://localhost:8080
#
# Exit code 0 when all checks pass.

BASE=${1:-http://localhost:8080}
BASE=${BASE%/}
FAILED=0

check() {
	# check <description> <path> <expected HTTP status> [text that the response must contain]
	body=$(mktemp)
	status=$(curl -s -o "$body" -w "%{http_code}" -H "Accept: application/json" --max-time 60 "$BASE$2")
	if [ "$status" != "$3" ]; then
		echo "FAIL  $1: HTTP $status (expected $3)  $2"
		FAILED=1
	elif [ -n "$4" ] && ! grep -q "$4" "$body"; then
		echo "FAIL  $1: response does not contain \"$4\"  $2"
		FAILED=1
	else
		echo "ok    $1"
	fi
	rm -f "$body"
}

echo "Smoke test of $BASE"
# 3.x (Vaadin Flow UI, SP-11) serves the stylesheet styles/fi.css; 2.x has the Vaadin 8 widgetset and theme
if [ "$(curl -s -o /dev/null -w "%{http_code}" --max-time 60 "$BASE/styles/fi.css")" = 200 ]; then
	check "UI start page (3.x)" "/" 200 "Vaadin"
	check "UI login page" "/login" 200 "Vaadin"
	check "UI stylesheet" "/styles/fi.css" 200 "fi-primary"
	# SP-14/SP-16: health check (databases included) and API description
	check "Health" "/actuator/health" 200 "\"UP\""
	check "API description (OpenAPI)" "/api/openapi" 200 "searchSubstances"
	check "Swagger UI" "/api/swagger-ui/index.html" 200 "swagger"
else
	check "UI start page" "/" 200 "vaadin"
	check "Vaadin widgetset" "/VAADIN/widgetsets/de.hswt.fi.ui.vaadin.widgetset/de.hswt.fi.ui.vaadin.widgetset.nocache.js" 200
	check "Vaadin theme" "/VAADIN/themes/fi-valo/styles.css" 200
fi
check "Home page (static, html folder)" "/home_en.html" 200
check "API: substance by InChIKey (phenazone)" "/api/substances?inchiKey=VEQOALNAAJBPNY-UHFFFAOYSA-N" 200 "VEQOALNAAJBPNY-UHFFFAOYSA-N"
check "API: substance by formula (chlorpyrifos)" "/api/substances?elementalFormula=C9H11Cl3NO3PS" 200 "C9H11Cl3NO3PS"
check "API: page of InChIKeys" "/api/substances/inchiKeys?page=0&size=5" 200 "content"

if [ "$FAILED" = 0 ]; then
	echo "All checks passed"
else
	echo "Smoke test FAILED"
fi
exit $FAILED
