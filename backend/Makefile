SHELL := /bin/bash

.PHONY: build-ApiFunction build-WorkerFunction build-WebSocketFunction build-SchemaMigrationFunction build-lambda-artifact

build-ApiFunction: build-lambda-artifact
build-WorkerFunction: build-lambda-artifact
build-WebSocketFunction: build-lambda-artifact
build-SchemaMigrationFunction: build-lambda-artifact

build-lambda-artifact:
	mvn -q -DskipTests package dependency:copy-dependencies
	rm -rf "$(ARTIFACTS_DIR)"
	mkdir -p "$(ARTIFACTS_DIR)/lib"
	cp -R target/classes/. "$(ARTIFACTS_DIR)/"
	cp target/dependency/*.jar "$(ARTIFACTS_DIR)/lib/"
