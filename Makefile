SONAR_URL   ?= http://localhost:9000
SONAR_TOKEN ?=

.PHONY: help deps install test sonar security-deps

help:
	@echo ""
	@echo "  fixm-model-ed254 — available targets"
	@echo "  ─────────────────────────────────────────────────────────"
	@echo ""
	@echo "    deps               Show which sibling repos must be installed first"
	@echo "    install            Build + install to local Maven repo"
	@echo "    test               Unit tests"
	@echo "    sonar              SonarQube analysis  (requires SONAR_TOKEN=<token>)"
	@echo "    security-deps      OWASP Dependency-Check"
	@echo ""
	@echo "  Variables: SONAR_URL=$(SONAR_URL)"

deps:
	@echo ""
	@echo "  Required sibling repos — install to local Maven repo first:"
	@echo ""
	@echo "    git clone https://github.com/swim-developer/swim-developer-root"
	@echo "    cd swim-developer-root && ./mvnw install -N -DskipTests"
	@echo ""

install:
	./mvnw clean install -DskipTests

test:
	./mvnw test

sonar:
	./mvnw clean verify sonar:sonar \
		-Dsonar.host.url=$(SONAR_URL) \
		$(if $(SONAR_TOKEN),-Dsonar.login=$(SONAR_TOKEN),) \
		-Dsonar.projectKey=fixm-model-ed254 \
		-Dsonar.projectName=fixm-model-ed254

security-deps:
	./mvnw org.owasp:dependency-check-maven:aggregate \
		-DfailBuildOnCVSS=7 -Dformats=HTML,JSON -DskipTests \
		-DsuppressionFile=owasp-suppressions.xml
	@echo "Report: target/dependency-check-report.html"
