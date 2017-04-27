#!/bin/bash -x

SBT_CONTAINER_VERSION="latest"

function init() {

  DEPENDENCY_LABEL=$GO_DEPENDENCY_LABEL_SBT_CONTAINER


  if [ -z ${DEPENDENCY_LABEL} ]; then
    SBT_CONTAINER_VERSION="latest"
  else
    SBT_CONTAINER_VERSION="v${DEPENDENCY_LABEL}"
  fi

}

function build_software() {

	# get local .ivy2
	# rsync -r ~/.ivy2 ./
  	docker run --user `id -u`:`id -g` --volume=${PWD}:/build ubirch/sbt-build:${SBT_CONTAINER_VERSION} $1
	# write back to local .ivy2

  if [ $? -ne 0 ]; then
      echo "Docker build failed"
      exit 1
  fi

}

function build_container() {
    # copy artefacts to TMP directory for faster build
    rm -rf TMP/
    mkdir -p TMP
    #get artifact names generated by Scala Build
    source Dockerfile.input
    if [ ! -f $SOURCE ]; then
      echo "Missing $SOURCE file \n did you run $0 assembly?"
      exit 1
    fi

    # get artefact name from Dockerfile

    tar cvf - $SOURCE | (cd TMP; tar xvf - )
    tar cvf - config/src/main/resources/ tools/ | (cd TMP; tar xvf - )
    cp Dockerfile.template TMP/Dockerfile
    #replace artefact name in start.sh
    sed -i.bak "s%@@build-artefact@@%$TARGET%g" TMP/tools/start.sh
    sed -i.bak "s%@@SOURCE@@%$SOURCE%g" TMP/Dockerfile
    sed -i.bak "s%@@TARGET@@%$TARGET%g" TMP/Dockerfile
    cd TMP

if [ -z $GO_PIPELINE_LABEL ]; then
      # building without GoCD
      docker build -t ubirch-auth-service:vmanual .
      docker tag ubirch-auth-service:vmanual tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:vmanual
  else
      # build with GoCD
      docker build -t ubirch-auth-service:v$GO_PIPELINE_LABEL --build-arg GO_PIPELINE_NAME=$GO_PIPELINE_NAME \
      --build-arg GO_PIPELINE_LABEL=$GO_PIPELINE_LABEL \
      --build-arg GO_PIPELINE_COUNTER=$GO_PIPELINE_COUNTER  .

      docker tag ubirch-auth-service:v$GO_PIPELINE_LABEL tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:v$GO_PIPELINE_LABEL
  fi

  if [ $? -ne 0 ]; then
    echo "Docker build failed"
    exit 1
  fi

  # push Docker image
  if [ -z $GO_PIPELINE_LABEL ]; then
    docker push tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:vmanual
  else
    docker push tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:v$GO_PIPELINE_LABEL
  fi
  if [ $? -ne 0 ]; then
    echo "Docker push failed"
    exit 1
  fi

}

function container_tag () {
    docker pull tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:v$GO_PIPELINE_LABEL
    docker tag tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:v$GO_PIPELINE_LABEL tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:latest
    docker push tracklecontainerregistry-on.azurecr.io/ubirch-auth-service:latest

}

case "$1" in
    build)
        init
        build_software "clean compile test"
        ;;
    assembly)
        build_software "clean server/assembly"
        ;;
    containerbuild)
        build_container
        ;;
    containertag)
        container_tag
        ;;
    *)
        echo "Usage: $0 {build|assembly|containerbuild|containertag}"
        exit 1
esac

exit 0