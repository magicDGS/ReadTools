#!/bin/bash

# TODO: parse the version from CHANGELOG?

if [ -z "$1" ]; then
	echo "Should provide a version..."
	exit 1
else
	version=$1
fi

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
rm -fr docs/javadoc && mv build/docs/javadoc docs/

git commit -am "Release javadoc"

# TODO: generates the documentation with Barclay
# TODO: see https://github.com/magicDGS/ReadTools/issues/182

# TODO: change the CHANGELOG to include the version