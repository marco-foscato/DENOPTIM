# DENOPTIM
DENOPTIM (De Novo OPTimization of organic and Inorganic Molecules) is a software for de novo design and optimization of functional compounds.

## Content of the source code folder tree

* [build](./build): contains scripts for building the DENOPTIM package from source.

* [contrib](./contrib): contains additional source and data that may be used in relation to DENOPTIM

* [doc](./doc): contains documentation and user manual

* [lib](./lib): The lib directory contains all third-party libraries used by DENOPTIM.

* [src](./src): contains the source code of DENOPTIM's core and modules:

  * DENOPTIM: Generic library of functions and data structures.

  * DenoptimCG: Generator of 3D conformations.

  * DenoptimGA: Genetic algorithm engine that uses the DENOPTIM library for molecular design.

  * DenoptimRND: Dummy evolutionary algorithm using only random selection of new members (no genetic operators).

  * FragSpaceExplorer: Combinatorial algorithm for exploration of fragment spaces.

  * misc: miscellaneous utilities that may be useful in DENOPTIM-based work.

* [test](./test): contains some automated functionality tests and the published test case.



## Quick start
To get started you first have to compile DENOPTIM and its programs. We assume that <code>$DENOPTIM_HOME</code> is the folder you have downloaded/cloned from the GitHub repository. 

1. Preparation. Make sure you have Java installed (1.5 or above). If the following does not result in version statements or the version is too old, you can get and install Java from www.oracle.com or http://openjdk.java.net/:

        java -version
        javac -version

2. Compile DENOPTIM and all the accessories in the src folder.

        cd $DENOPTIM_HOME/build
        bash build-all.sh

3. Done!

After compilation you can run the functionality tests (takes 2-3 minutes).

    cd $DENOPTIM_HOME/test/functional_tests
    bash runAllTests.sh

The tests will use a temporary folder <code>/tmp/denoptim_test</code> where you can find all files related to these tests.

In addition, you can play with the optimization of organometallic ligands sets that weaken the carbonyl bond in Pt(CO)(L)(X)<sub>2</sub> complexes (takes 10-15 minutes).

    cd $DENOPTIM_HOME/test/PtCOLX2
    bash runEvolutionaryExperiment.sh

This will create a playground folder at <code>/tmp/denoptim_PtCO</code> where the evolutionary experiment will be run. Once the experiment is completed, you'll find also the results in the same folder.

## User Manual

The complete user manual is available under the <code>doc</code> folder and is accessible [on line](http://htmlpreview.github.com/?https://github.com/denoptim-project/DENOPTIM/blob/master/doc/user_manual.html)

## License
DENOPTIM is licensed under the terms of the GNU Affero GPL version 3.0 license. 
Instead, additional libraries used by DENOPTIM programs are licensed according to their respective licenses:
* cdk: GNU Lesser GPL Version 2.1
* commons-cli: Apache License Version 2.0
* commons-io: Apache License Version 2.0
* commons-lang: Apache License Version 2.0
* commons-math: Apache License Version 2.0
* vecmath: GNU GPL Version 2


## Cite DENOPTIM
1) DENOPTIM: Software for Computational de Novo Design of Organic and Inorganic Molecules; Marco Foscato, Vishwesh Venkatraman, and Vidar R. Jensen, <i>J. Chem. Inf. Model</i> <b>2019</b> (submitted)
2) Foscato, M.; Occhipinti, G.; Venkatraman, V.; Alsberg, B. K.; Jensen, V. R.; Automated Design of Realistic Organometallic, Molecules from Fragments; <i>J. Chem. Inf. Model.</i> <b>2014</b>, 54, 767–780.
3) Foscato, M.; Venkatraman, V.; Occhipinti, G.; Alsberg, B. K.; Jensen, V. R.; Automated Building of Organometallic Complexes from 3D Fragments; <i>J. Chem. Inf. Model.</i> <b>2014</b>, 54, 1919–1931.
4) Foscato, M.; Houghton, B. J.; Occhipinti, G.; Deeth, R. J.; Jensen, V. R.; Ring Closure To Form Metal Chelates in 3D Fragment-Based de Novo Design. <i>J. Chem. Inf. Model.</i> <b>2015</b>, 55, 1844-1856.


