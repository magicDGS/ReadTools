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

# TODO: Update version in CHANGELOG and docs:
# ## in docs, we require to update _data/sidebars/home_sidebar.yml and _data/topnav.yml
echo "WARNING: CHANGELOG and docs are not updated!!"


## start creating the relase
echo "Create release for version $version (from master branch)"
git checkout master
git branch release_${version}
git checkout release_${version}

# tagging as version
git tag $version

# now generate the documentation
echo "Generate javadoc and jar"
./gradlew -Drelease=true clean javadoc currentJar &> tmp/release_${version}.gradle_output
rm -fr docs/javadoc && mv build/docs/javadoc docs/

git commit -am "Release javadoc"

# TODO: generates the documentation with Barclay (https://github.com/magicDGS/ReadTools/issues/182)