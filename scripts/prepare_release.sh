#!/bin/bash

# exit on any failure
set -e

if [ -z "$1" ]; then
	echo "Should provide a version..."
	exit 1
else
	version=$1
fi

## checks if the repository is clean
if [[ ! -z "$(git status -s)" ]]; then
	echo "Release requires a clean repository";
	exit 2
fi

# create temp directory
mkdir -p tmp

## start creating the release
echo "[$(date)] Create release for version $version (from master branch)"
git checkout master
git branch release_${version}
git checkout release_${version}
git push --set-upstream origin release_${version}

# tagging as version
git tag $version

## generate jar for upload
echo "[$(date)] Generate jar"
./gradlew -Drelease=true clean currentJar &> tmp/release_${version}.currentJar.out

# now generate the documentation
echo "[$(date)] Generate javadoc"
./gradlew -Drelease=true javadoc &> tmp/release_${version}.javadoc.out

## generate the online documentation
echo "[$(date)] Generate documentation site"
./gradlew -Drelease=true readtoolsDoc &> tmp/release_${version}.readtoolsDoc.out

## and copy to the new locations - no SNAPSHOT
rm -fr docs/javadoc && mv build/docs/javadoc docs/
mv build/docs/readtools/*.yml docs/_data/ && rm -fr docs/readtools/* && mv build/docs/readtools/*.md docs/readtools/

echo "[$(date)] Update CHANGELOG version"
awk -v var=$version '{print $0}; $0=="## [Unreleased]"{print "\n\n## ["var"]"}END{print "["var"]: https://github.com/magicDGS/ReadTools/releases/tag/"var}' CHANGELOG.md > CHANGELOG.md.new && mv CHANGELOG.md.new CHANGELOG.md

## commit and push
echo "[$(date)] Upload to GitHub"
git add docs/javadoc && git commit -m "Release javadoc" && git push
git add docs/ && git commit -m "Release documentation site" && git push
git add CHANGELOG.md && git commit -m "Update CHANGELOG" && git push

echo "Please, upload to the release page the following file(s):"
echo "* build/libs/ReadTools.jar"
