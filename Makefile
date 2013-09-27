CONNECTOR_NAME ?= acunu-connector-kafka

# When installing in the product, these are overriden in the spec file
JBIRD_HOME ?= /usr/share/acunu/jbird
PLUGINS_DIR ?= $(JBIRD_HOME)/plugins
CONNECTOR_DIR = $(PLUGINS_DIR)/$(CONNECTOR_NAME)
CONNECTOR_LIB_DIR = $(CONNECTOR_DIR)/lib/
SCHEMAS_DIR ?= /etc/acunu/schemas
ETC_DIR ?= /etc/acunu
DOCS_DIR ?= /usr/share/doc/acunu-docs
BIN_DIR ?= /usr/bin
JETTY_HOME ?= /usr/share/jetty
CATALINA_BASE ?= /usr/share/tomcat6
#URL=ui732i73r7gdfal
URL=3c35970c1198326b

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
	cp lib/* $(CONNECTOR_LIB_DIR)

build:
	ant build

test:
	ant test

clean:
	ant clean
