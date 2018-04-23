[![Build Status](https://travis-ci.org/magicDGS/ReadTools.svg?branch=master)](https://travis-ci.org/magicDGS/ReadTools)
[![codecov](https://codecov.io/gh/magicDGS/ReadTools/branch/master/graph/badge.svg)](https://codecov.io/gh/magicDGS/ReadTools)
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/dd842750e7a74112870a5156a24a8cbf)](https://www.codacy.com/app/daniel-gomez-sanchez/ReadTools?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=magicDGS/ReadTools&amp;utm_campaign=Badge_Grade)
[![MIT licensed](https://img.shields.io/badge/license-MIT-blue.svg)][mit-license]
[![Github Releases](https://img.shields.io/github/release/magicDGS/ReadTools.svg?logo=github)](https://github.com/magicDGS/ReadTools/releases/latest)
[![Github Releases](https://img.shields.io/github/downloads/atom/atom/total.svg?logo=github)](http://www.somsubhra.com/github-release-stats/?username=magicDGS&repository=ReadTools)

# _ReadTools_: A universal toolkit for handling sequence data from different sequencing platforms

_ReadTools_ is an open‐source toolkit designed to standardize and preprocess
sequencing read data from different platforms. It manages FASTQ‐ and
SAM‐formatted inputs while dealing with platform‐specific peculiarities and
provides a standard SAM compliant output.


## Getting started

For more user-friendly information about the project, visit our
[Documentation Page][documentation-page]. There, you can find the information
for download and the complete tool documentation for the current version.

For developers, please check our [Wiki][wiki-page] for more details or fill an [issue][issue_tracker] if you have any question: we would be happy to include any extra information
to the [Wiki][wiki-page] with your feedback!

### Pre-requisites

- A Java 8 JDK
- Git 2.5 or greater
- Gradle 3.3 or greater

### Building

**Important: to build ReadTools, you should clone the git repository (release
zip files would not work).

```
git clone https://github.com/magicDGS/ReadTools.git
cd ReadTools
./gradle currentJar
```

The jar file would be located in __build/libs/ReadTools.jar__.

__To build a released version of ReadTools, use `git checkout ${version_number}`.


## Contributing

Please read [CONTRIBUTING.md](https://github.com/magicDGS/ReadTools/blob/master/CONTRIBUTING.md)
for details on our code of conduct, and the process for collaborating on the project.

### Bug reports

Before submitting a new bug report, please search for keywords in our
[Issue Tracker][issue_tracker]. If you cannot find your problem, fill in
a new [issue][issue_tracker].


## Versioning

This project adheres to [Semantic Versioning (SemVer)](http://semver.org/) for the user-side.
A short description with notable changes appears in each [release](https://github.com/magicDGS/ReadTools/releases). All the changes can be found in the [CHANGELOG](https://github.com/magicDGS/ReadTools/blob/master/CHANGELOG.md).


## License

_ReadTools_ is licensed under the [MIT License][mit-license]. See [LICENSE](https://github.com/magicDGS/ReadTools/blob/master/LICENSE) file.


## Citing

If you use _ReadTools_, please cite:

> Gómez-Sánchez D, Schlötterer C. _ReadTools_: A universal toolkit for handling sequence data from different sequencing platforms. Mol Ecol Resour. 2017;00:1–5. [doi:10.1111/1755-0998.12741](http://onlinelibrary.wiley.com/doi/10.1111/1755-0998.12741/abstract)

[mit-license]: https://opensource.org/licenses/MIT
[documentation-page]: http://magicdgs.github.io/ReadTools/
[issue_tracker]: https://github.com/magicDGS/ReadTools/issues
[wiki-page]: https://github.com/magicDGS/ReadTools/wiki
