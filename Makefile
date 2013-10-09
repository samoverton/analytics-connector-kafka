CONNECTOR_NAME ?= analytics-connector-kafka

# When installing in the product, these are overriden in the spec file
JBIRD_HOME ?= /usr/share/acunu/jbird
PLUGINS_DIR ?= $(JBIRD_HOME)/plugins
CONNECTOR_DIR = $(PLUGINS_DIR)/$(CONNECTOR_NAME)
CONNECTOR_LIB_DIR = $(CONNECTOR_DIR)/lib/
SCHEMAS_DIR ?= /etc/acunu/examples

JBIRD_USER ?= acunu
JBIRD_GROUP ?= acunu

VERSION ?= custom-devel
RELEASE ?= $(shell cat VERSION)
BUILD_DATE := $(shell date -u)

DEB_VERSION    = $(RELEASE).$(shell hg identify --num).$(shell hg identify --id)
MAVEN_VERSION  = $(RELEASE).$(shell hg identify --num)
UPLOAD_VERSION =

.PHONY: all
all: jar

jar: 
	ant jar

################################################################################
install: install-jar

install-jar: jar
	mkdir -p $(CONNECTOR_DIR)
	mkdir -p $(CONNECTOR_LIB_DIR)
	cp build/$(CONNECTOR_NAME).jar $(CONNECTOR_DIR)/
	cp -r lib/* $(CONNECTOR_LIB_DIR)

build:
	ant build

test:
	ant test

clean:
	ant clean
