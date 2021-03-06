#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "locationtech/geowave" ] && [ "$BUILD_AND_PUBLISH" == "true" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then  
  echo $GPG_SECRET_KEYS | base64 --decode | $GPG_EXECUTABLE --import
  echo $GPG_OWNERTRUST | base64 --decode | $GPG_EXECUTABLE --import-ownertrust
  
  # Build the dev-resources jar
  dev-resources-exists=$(curl -I -s https://oss.sonatype.org/service/local/repositories/releases/content/org/locationtech/geowave/geowave-dev-resources/${DEV_RESOURCES_VERSION}/geowave-dev-resources-${DEV_RESOURCES_VERSION}.pom | grep HTTP)
  if [[ ${dev-resources-exists} != *"200"* ]];then
    pushd dev-resources
    echo -e "Deploying dev-resources..."
    mvn deploy --settings ../.utility/.maven.xml -DskipTests -Dspotbugs.skip -B -U -Prelease
    popd
  fi
  echo -e "Deploying geowave artifacts..."
  mvn deploy --settings .utility/.maven.xml -DskipTests -Dspotbugs.skip -B -U -Prelease
  
  # Get the version from the build.properties file
  filePath=deploy/target/classes/build.properties
  GEOWAVE_VERSION=$(grep project.version $filePath|  awk -F= '{print $2}')
  
  # Don't publish snapshots to PyPi
  if [[ ! "$GEOWAVE_VERSION" =~ "SNAPSHOT" ]] ; then
    if [[ -z "${PYPI_CREDENTIALS}" ]]; then
  	  echo -e "No PyPi credentials, skipping PyPi distribution..."
    else
      echo -e "Deploying pygw to PyPi..."
      pushd python/src/main/python
      pyenv global 3.7.1
      python -m venv publish-venv
      source ./publish-venv/bin/activate
    
      pip install --upgrade pip wheel setuptools twine
      python setup.py bdist_wheel --python-tag=py3 sdist
      twine upload --skip-existing -u geowave -p $PYPI_CREDENTIALS dist/*
      deactivate
      popd
    fi
  fi 
  
  echo -e "Generating changelog...\n"
  export CHANGELOG_GITHUB_TOKEN=$GH_TOKEN
  gem install github_changelog_generator
  github_changelog_generator
  pandoc -f markdown -t html -s -c stylesheets/changelog.css CHANGELOG.md > changelog.html
  cp changelog.html target/site/

  echo -e "Publishing site ...\n"
  cp -R target/site $HOME/site

  cd $HOME
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"
  git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/locationtech/geowave gh-pages > /dev/null

  cd gh-pages 

  # Check if this is not a snapshot version
  if [[ ! "$GEOWAVE_VERSION" =~ "SNAPSHOT" ]] ; then

    # Make a new directory for this release and copy the contents to it
    echo "creating a new versioned documentation directory"
    mkdir previous-versions/$GEOWAVE_VERSION
    cp -Rf $HOME/site/* previous-versions/$GEOWAVE_VERSION/
    rm -f previous-versions/$GEOWAVE_VERSION/*.epub *.pdf *.pdfmarks

  fi

  # Save previous versions of the documentation
  cp -r previous-versions/ $HOME/site/

  git rm -rf .
  cp -Rf $HOME/site/* .

  # Don't check in big binary blobs
  # TODO: Push to S3 if we want to link to them via the web site
  rm -f *.epub *.pdf *.pdfmarks

  git add -f .
  git commit -m "Lastest javadoc on successful travis build $TRAVIS_BUILD_NUMBER auto-pushed to gh-pages"
  git push -fq origin gh-pages > /dev/null

  echo -e "Published Javadoc to gh-pages.\n"
  
fi
