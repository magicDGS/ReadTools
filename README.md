[![Build Status](https://travis-ci.org/magicDGS/ReadTools.svg?branch=master)](https://travis-ci.org/magicDGS/ReadTools)
[![codecov](https://codecov.io/gh/magicDGS/ReadTools/branch/master/graph/badge.svg)](https://codecov.io/gh/magicDGS/ReadTools)
[![Dependency Status](https://www.versioneye.com/user/projects/58821329e25f59003995102e/badge.svg?style=flat-square)](https://www.versioneye.com/user/projects/58821329e25f59003995102e)
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/dd842750e7a74112870a5156a24a8cbf)](https://www.codacy.com/app/daniel-gomez-sanchez/ReadTools?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=magicDGS/ReadTools&amp;utm_campaign=Badge_Grade)
[![Sputnik](https://sputnik.ci/conf/badge)](https://sputnik.ci/app#/builds/magicDGS/ReadTools)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

# _ReadTools_: Standardizing Sources For Sequencing Data 

ReadTools provides a consistent and highly tested set of tools for processing any kind of source of sequencing data.

For more information about the program from the user side, go to the [Wiki Page].
Download main releases in the [Releases] section. Changes for every release could be found in the [CHANGELOG].

_Note: to use unreleased changes it is required to install from source. See the Building section for more details._

---

## Bug reports

Please, if you find any problem add a new [Issue] or contact by email <daniel.gomez.sanchez@hotmail.es>

---

## DEVELOPERS

### Requirements

* Java 8
* Git (developers)
* Gradle 3.3 or greater (developers)

## Dependencies

- [HTSJDK] library
- [GATK v.4] framework

## Building

Clone the git repository using `git clone https://github.com/magicDGS/ReadTools.git` and change to the __ReadTools__ directory.
If you want a specific version, change the branch using `git checkout ${version_number}`;
otherwise, the master branch contains unreleased changes.

For building an executavle jar, run `./gradlew shadowJar`. The __ReadTools.jar__ will be under the __build/libs/__ directory.


## Testing

Code coverage reports for the project can be found in the [Codecov project page](https://codecov.io/gh/magicDGS/ReadTools).
If you want to evaluate _ReadTools_ locally:
* To run all tests, run `./gradlew test`. Test reports will be in __build/reports/tests/index.html__
* To compute a coverage report, run `./gradlew jacocoTestReport`. The report will be in __build/reports/jacoco/test/html/index.html__

## Guidelines

* __Pushing directly to master branch is not allowed.__
* It is recommended to name branches with a short form of your name and a explanatory name. Example: _dgs_fix_issue30_.
* Pull Requests should be reviewed before merging by other developer.
* Any new code will require unit/integration tests.
* Use [org.apache.logging.log4j.Logger](https://logging.apache.org/log4j/2.0/log4j-api/apidocs/org/apache/logging/log4j/Logger.html) for logging.
* Use [TestNG](http://testng.org/doc/index.html) for testing.
* Use [magicDGS Style Guide](https://github.com/magicDGS/styleguide) for code formatting.

## Versioning and changelog

We use [semantic versioning](http://semver.org/) (_MAJOR.MINOR.PATCH_).
[build.gradle] and Unreleased tag (within [CHANGELOG]) should be updated accordingly in Pull Request to follow the convention:

1. Backwards-compatible bug fixes: increase _PATCH_ number. Add a __Fixed__ entry to [CHANGELOG].
2. Backwards-compatible changes: increase _MINOR_ number and reset _PATCH_ to 0. Add a __Added__ entry to [CHANGELOG].
3. Backwards-incompatible changes: only if the current unreleased version _MAJOR_ number is the same as the last release, increase _MAJOR_ number and reset _MINOR_ and _PATCH_ to 0;
otherwise apply the backwards-compatible rules (1 or 2). Add a __Changed__ entry to [CHANGELOG] in any case.

Please, distinguish within every category (__Fixed__/__Added__/__Changed__) if a change 
correspond only to API changes or it have some effect to the user side.

---

## License
Licensed under the [MIT License]. See [LICENSE] file.

## Citing

If you use this software, please add the citation as following:

>Gómez-Sánchez D (2017): ReadTools ${version}, Institut für Populationsgenetik, Vetmeduni Vienna.



<references>
[MIT License]: https://opensource.org/licenses/MIT
[Wiki Page]: https://github.com/magicDGS/ReadTools/wiki
[Releases]: https://github.com/magicDGS/ReadTools/releases
[Issue]: https://github.com/magicDGS/ReadTools/issues
[HTSJDK]: https://samtools.github.io/htsjdk/
[GATK v.4]: https://github.com/broadinstitute/gatk
[CHANGELOG]: https://github.com/magicDGS/ReadTools/blob/master/CHANGELOG.md
[LICENSE]: https://github.com/magicDGS/ReadTools/blob/master/LICENSE
[build.gradle]:  https://github.com/magicDGS/ReadTools/blob/master/build.gradle