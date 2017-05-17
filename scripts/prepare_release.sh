#!/bin/bash

# TODO: parse the version from CHANGELOG?
version=$1

# TODO: check if there are changes and fail if so 
echo "Create release for version $version (from master branch)"
git checkout master
git branch release_${version}
git checkout release_${version}

# tagging as version
git tag $version

# now generate the documentation
echo "Generate javadoc and jar"
./gradlew -Drelease=true clean javadoc currentJar
mv build/docs/javadoc docs/javadoc

git commit -am "Release javadoc"

# TODO: generates the documentation with Barclay
# TODO: see https://github.com/magicDGS/ReadTools/issues/182