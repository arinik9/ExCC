# ExCC
Exact partitioning method(s) for the *Correlation Clustering (CC)* problem

* Copyright 2020-21 Nejat Arınık

*ExCC* is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation. For source availability and license information see the file `LICENCE`

* GitHub repo: https://github.com/arinik9/ExCC
* Contact: Nejat Arınık <arinik9@gmail.com>

## Description

*ExCC* aims at solving optimally the Correlation Clustering problem. It offers two different tasks: Obtaining a *single* vs. *all* optimal solution(s).

In both tasks, an ILP model to solve the CC problem can be constructed in two different ways: decision variables defined on 1) vertex-pair (*Fv*: *vertex* formulation type) or 2) edge (Fe: *edge* formulation type). If we denote *n* by the number of vertices in the graph and *m* by the number of edges, there are *(n(n-1)/2)* variables in *Fv*, whereas there are m variables in *Fe*.  Nevertheles, the number of constraints in both methods is of polynomial and exponential orders, respectively. 

Each mentioned task above can be performed with two resolutions methods: *Branch&Bound (B&B)* and *Branch&Cut (B&C)*. In B&B, we just let Cplex solve it with B&B. In B&C, there are two successful applications of B&C in the literature:

1. Adding violated valid inequalities only at the root of the B&B tree through the Cutting Plane (CP) method and then proceeding to the branching phase as in B&B. In the literature, it is also called *Cut&Branch*.
2. Adding violated valid inequalities only for integer solutions during the branching phase. 

According to the literature, the first B&C application is better suited for *Fv* (that we call *B&C(Fv)*), whereas the second one performs better for *Fe* (that we call *B&C(Fv)*). So, we have two successful resolutions methods, *B&C(Fv)* and *B&C(Fe)*, to solve the CC problem. But, which one should be used? 

In chapter 2 of *[Arınık'21]*, some experiments are conducted based on unweighted random signed graphs to clarify this point. These experiments show that the choice of the formulation and its resolution method depends on the characteristics of the network at hand. When a network is sparse (resp. dense), the *B&C(Fe)* (resp. *B&C(Fv)*) better performs. Moreover, for a medium graph density, the *B&C(Fv)* type is less sensitive to increase in *n* than the other methods, hence preferable in this case. 

To solve the CC problem we can either read a signed graph with a .G graph file format or import a Cplex LP file, where a ILP model is already recorded in a previous run. The advantage of doing the second option is that if we provide ExCC with a ILP model containing violated valid inequalities found during a CP method, then it amounts to skip the CP phase of the B&C method. So, it directly proceeds to the second phase: branching. This allows to gain a considerable amount of time.

### Input Parameters


 * **formulationType:** ILP formulation type. Either *vertex* for *Fv* or *edge* for *Fe*.
 * **inFile:** Input file path. See *in/net.G* for the input graph format. 
 * **outDir:** Output directory path. Default "." (i.e. the current directory).
 * **cp:** True if B&C (i.e. Cutting Plane method + branching) will be used. Default false.
 * **enumAll:** True if enumerating all optimal solutions is desired. Default false. We call *OneTreeCC* this enumeration method in Chapter 5 of *[Arınık'21]*, when the formulation type is *Fv*.
 * **tilim:** Time limit in seconds for the whole execution process. Default *-1*, which means no time limit.
 * **tilimForEnumAll** Time limit in seconds when enumerating all optimal solutions, except the first one. This is useful when doing a benchmarking with EnumCC for the OneTreeCC method.
 * **solLim**  Maximum number of optimal solutions to be discovered when OneTreeCC is called. This can be useful when there are a huge number of optimal solutions, e.g. 50,000. Default *-1*.
 * **MaxTimeForRelaxationImprovement:** Max time limit for relaxation improvement in the first phase of the Cutting Plane method. This is independent of the time limit. If there is no considerable improvement for X seconds, it stops and passes to the 2nd phase, which is branching. This parameter can be a considerable impact on the resolution time. For medium-sized instances (e.g. 50,60), it might be beneficial to increase the value of this parameter (e.g. 1800 or 3600s). The default value is 600s.	Moreover, it might be beneficial to decrease the default value to 30s or 60s if the graph is easy to solve or the number of vertices is below 28.
 * **lazyInBB:** Used only for B&C method. True if adding lazily triangle constraints (i.e. lazy callback approach) in the branching phase. If it is False, the whole set of triangle constraints is added before branching. Based on our experiments, we can say that the lazy callback approach is not preferable over the default approach. Default false.
 * **userCutInBB:** Used only for B&C method. True if adding user cuts during the branching phase of the B&C method or in B&B method is desired. Based on our experiments, we can say that it does not yield any advantage, and it might even slow down the optimization process. Default false.
 * **nbThread:** number of threads.
 * **verbose:** Default value is True. When True, it enables to display log outputs during the Cutting Plane method.
 * **initMembershipFilePath** Default value is "". It allows to import an already known solution into the optimization process. Since we solve a minimization problem, the imbalance value of the imported solution is served as the upper bound. It is usually beneficial to use this option, when we possess some good-quality heuristics.
 * **LPFilePath** Default value is "". It allows to import a LP file, corresponding to a ILP formulation. Remark: such a file can be obtained through Cplex by doing *exportModel()*.
 * **onlyFractionalSolution** Useful mostly for the *Fv* formulation type. Default value is False. It allows to run only the cutting plane method in B&C, so the program does not proceed to the branching phase
 * **fractionalSolutionGapPropValue** Useful mostly for the *Fv* formulation type. It allows to limit the gap value to some proportion value during the cutting plane method in B&C. It can be useful when we solve an easy graph. Hence, we do not spent much time by obtaining very tiny improvement when the solution is already close to optimality. Default *-1*.
 * **triangleIneqReducedForm :** Used only for the *Fv* formulation type. When it is set to true, this amounts to remove *redundant* triangle inequalities from the formulation. See *[Miyaichi'18]* for the definition of such *redundancy*. Default value is false, which keeps the whole set of triangle constraints. See *Chapter 2* in *[Arınık'21]*. Note that removing redundant triangle inequalities can substantially accelerate the optimization process for finding a single optimal solution. However, if the goal is to enumerate all optimal solutions, then such removing can degrade the performance. This last point is briefly mentioned in Chapter 5 in [Arınık'21], but it needs to be investigated thoroughly in a follow-up work.

### Instructions & Use

* Install [`IBM CPlex`](https://www.ibm.com/docs/en/icos/20.1.0?topic=2010-installing-cplex-optimization-studio). The default installation location is: `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>`. Tested with Cplex 12.8 and 20.1.
*  Put `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>/cplex/lib/cplex.jar` into the `lib` folder in this repository.
* Compile and get the jar file for *ExCC*: `ant -v -buildfile build.xml compile jar`.
* Run one of the scripts *.sh* available in this repository.

### Examples

See `run-bb.sh`, `run-bb-enum-all.sh`, `run-cp-bb.sh`, `run-cp-bb-enum-all.sh`, `run-cp-only.sh`, `run-lp-bb.sh` and `run-lp-bb-enum-all.sh` for more execution scenarios.

* `run-bb.sh`: Branch&Bound for finding a single optimal solution. It does not include any valid inequalities that can be obtained through Cutting Plane.
* `run-bb-enum-all.sh`: The same as `run-bb.sh`, but for enumerating all optimal solutions.
* `run-cp-bb.sh`: It corresponds to one of two successful B&C applications mentioned abovemethod depending on the formulation type. It is for finding a single optimal solution.
* `run-cp-bb-enum-all.sh`: The same as `run-cp-bb.sh`, but for enumerating all optimal solutions.
* `run-cp-only.sh`: only Cutting Plane method, i.e. strengthing the initial LP model with tight valid inequalities without proceeding to the branching phase. It is only for the *Fv* formulation type.
* `run-lp-bb.sh`: The same as `run-bb.sh`, but it reads the ILP formulation from a Cplex LP file (rather than a signed graph file). It is for finding a single optimal solution.
* `run-lp-bb-enum-all.sh`: The same as `run-lp-bb.sh`, but for enumerating all optimal solutions.

#### Example for B&C(Fv)

```bash
ant clean compile jar; ant -DinFile=in/net.G -DoutDir=out/net -DformulationType="vertex" -Dcp=true -DenumAll=false -DMaxTimeForRelaxationImprovement=120 -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true -Dtilim=300 -DtriangleIneqReducedForm=true run
```

```bash
ant clean compile jar; ant -DinFile=in/net.G -DoutDir=out/net -DformulationType="vertex" -Dcp=true -DenumAll=false -DMaxTimeForRelaxationImprovement=120 -DfractionalSolutionGapPropValue=0.01 -DnbThread=4 -Dverbose=true -Dtilim=300 -DtriangleIneqReducedForm=true run
```

#### Example for B&C(Fe)	  	

```bash
ant clean compile jar; ant -DinFile=in/net.G -DoutDir=out/net -DformulationType="edge" -DenumAll=false -Dcp=true -DMaxTimeForRelaxationImprovement=120 -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="" -DLPFilePath="" -DonlyFractionalSolution=false -DfractionalSolutionGapPropValue=-1.0 -DnbThread=4 -Dverbose=true -Dtilim=300 run
```


### Output

* The names of the optimal solutions are in the following form: '*solXX.txt*', where *XX* is the solution id.
* The ILP model strengthed during the optimization process is exported into a LP file, if the underlying task is to find a single optimal solution. The name of such file is *strengthedModel.lp*. Moreover, if the Cutting Plane (CP) method is used (i.e. the input parameter *cp*), then an additional LP file is also exported just after the end of the CP (and before the branching phase) method and it is called *strengthedModelAfterRootRelaxation.lp*. 

### Remarks

* In the task of enumerating all optimal solutions, the number of optimal solutions found by *B&C(Fv)* and *B&C(Fe)* can be different (similarly, by *B&B(Fv)* and *B&B(Fe)*), if the underlying signed graph does not contain a single connected component defined on positive edges (e.g. the presence of a singleton vertex).

### Acknowledgement

I thank Zacharie Ales for providing me with his code (for a similar problem), which constitutes an early version of this code. 

### References

* **[Arınık'21]** N. Arınık, *Multiplicity in the Partitioning of Signed Graphs*. PhD thesis in Avignon Université (2021).
* **[Miyauchi'18]** A. Miyauchi, T. Sonobe, and N. Sukegawa,  *Exact Clustering via Integer Programming and Maximum Satisfiability*, in: AAAI Conference on Artificial Intelligence 32.1 (2018).

