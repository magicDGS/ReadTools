#!/bin/bash

# exit on any failure
set -e

# TODO: parse the version from CHANGELOG?
if [ -z "$1" ]; then
	echo "Should provide a version..."
	exit 1
else
	version=$1
fi

## cheks if the repository is clean
if [[ ! -z "$(git status -s)" ]]; then
	echo "Release requires a clean repository";
	exit 2
fi


## start creating the relase
echo "Create release for version $version (from master branch)"
git checkout master
git branch release_${version}
git checkout release_${version}

# tagging as version
git tag $version

## generate jar for upload
echo "Generate jar"
./gradlew -Drelease=true clean currentJar &> tmp/release_${version}.currentJar.out

# now generate the documentation
echo "Generate javadoc"
./gradlew -Drelease=true javadoc &> tmp/release_${version}.javadoc.out
rm -fr docs/javadoc && mv build/docs/javadoc docs/

## commit and push
git commit -am "Release javadoc" && git push

## generate the online documentation
echo "Generate documentation site"
./gradlew -Drelease=true readtoolsDoc &> tmp/release_${version}.readtoolsDoc.out
mv build/docs/readtools/*.yml docs/_data/ && rm -fr docs/readtools/* && mv build/docs/readtools/*.md docs/readtools/

## commit and push
git commit -am "Release documentation site" && git push


# TODO: Update version in CHANGELOG
echo "WARNING: CHANGELOG version is not updated!! Please, update manually"

echo "Please, upload to the release page the following file(s):"
echo "* build/libs/ReadTools.jar"
