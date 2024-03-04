# STRiKE
This codebase contains implementation of a fragment-based rule inference engine accompanied by a heuristic beam-based rule-learning 
method that was published in [STRiKE: Rule-Driven Relational Learning Using Stratified k-Entailment](https://orca.cardiff.ac.uk/id/eprint/130911/1/ECAI2020_STRiKE.pdf) at ECAI'20 
and in *Relaxing Deductive and Inductive Reasoning in Relational Learning* (dissertation, CTU'24). 

If you'd like to cite us, please use the following bib-entry:
```
@article{svatovs2020strike,
  title={STRiKE: Rule-driven relational learning using stratified k-entailment},
  author={Svato{\v{s}}, Martin and Schockaert, Steven and Davis, Jesse and Ku{\v{z}}elka, Ond\v{r}ej},
  year={2020}
}
```

Installation
============
Having standard java installed on your machine is enough. Then, you may either use the fat jar or build one from scratch using 
all the dependencies -- [AUC-PR](https://www.biostat.wisc.edu/~page/rocpr.pdf), `Matching` by Ondřej Kuželka, and SAT4J. This 
codebase evolved from [Hypothesis Pruning](https://github.com/martinsvat/Pruning-Hypotheses) and include some ideas that were 
not published in the original paper.

Inference
=========
The core of the paper and the codebase are tightly connected with a general interference engine that allows one to infer 
sorted list of weighted rules in different manners:
- classical logical entailment (dropping out the weights)
- a [possibilistic logic approach](https://arxiv.org/pdf/1705.07095.pdf) (PosLog)
- one-step inference (1S) that applies only a single forward step of forward-chaining procedure with max-confidency aggregation (dropping out constraints)
- [*k*-entailment](https://arxiv.org/abs/1803.05768) that infers literals which are entailed by a fragment induced with up to $k$ constants (dropping out the weights)
- [STRiKE](https://orca.cardiff.ac.uk/id/eprint/130911/1/ECAI2020_STRiKE.pdf) (STRiKE) that invokes *k*-entailment on gradually larger list of rules (and hence less probable theories)

Let us start with an example:

$\Phi = \{ \forall X giraffe(X) \Rightarrow animal(X),$

$ \forall X \forall Y  friends(X,Y) \Rightarrow friends(Y,X),$

$ \forall X \forall Y  friends(X,Y) \Rightarrow human(X)\}$

$\Gamma = \{ \forall X \lnot human(X) \lor \lnot animal(X) \}$

$E = \{ giraffe(liz), friends(ann, liz)\} $

that models a world ($E$) with a single giraffe (liz) and her friend ann. We also have a theory that contains a Horn rules 
($\Phi$, for making predictions) and constraints ($\Gamma$, to keep integrity of the inferred facts).

Now, we can run different inference engines which produce different outputs:
- classical entailment runs into inconsistency and hence does not derive anything
- one-step inference usually ignores constraints in $\Gamma$, hence it would derive $\{animal(liz), friends(liz, ann), human(ann) \}$. In case that $\Phi$ is a list of weighted Horn-rules, the inferred fact would be accompanied by corresponding (max) weight of rules from which they were inferred.
- *k*-entailment needs the $k$ parameter to be set (i.e., the maximal number of constants that can be taken into account), hence it boils down to inferring all positive facts $\upsilon$ such that $E \cup \Gamma \cup \Phi \models_k \upsilon$, thus
- $k=1$ infers $\{animal(liz)\}$ -- it is also the single fact entailed by $k=2$ since all other possible facts, e.g., $human(liz)$, are blocked by the inconsistency that arises due to this fact and the constraint in $\Gamma$
- to see the full output of STRiKE, we need to firstly construct an ordered list of rules (together with constraints). For example, a list that starts with the constraint and continues as $\Phi$, would lead to deriving $\{animal(liz), friends(liz, ann)\}$ with weights annotated by particular weight from particular rules.   

For practical reasons, we allow only range-restricted, function-free, and constraint-free rules. However, we are not subject to only binary arity, which is the case of many knowledge-graph completion methods.

To apply the engine in this codebase for the above-mentioned cases, invoke the fat jar with the following parameters
```
java
    -Dida.pacReasoning.entailment=[classical|oneS|PL|PLWC|k|strike|strikeWC]
    -Dida.pacReasoning.entailment.k=2
    -Dida.pacReasoning.inputFolder=.\datasets\giraffe\
    -Dida.pacReasoning.theory=.\datasets\giraffe\theory.poss
    -Dida.pacReasoning.outputFolder=.\infer\giraffe\
    -jar STRiKE.jar
```
These predefined modes translates to apply the engine on the whole domain or fragments, stratified or non-stratified theory, and so on. 
Further, the suffix *WC* stands for dropping constraints in that run, so you don't have to create constraint-free theories in different files.
The engine than iterates over all *.db* files in the input folder and applies the given strategy with the theory and stores 
the result to the output folder. The constants in the db file starts with lower-case while variables in theory file starts with an 
upper-case (the only type of terms allowed in there). Finally, you may notice that in the inferred folder, there are two files 
-- one with the inferred facts, i.e. a `.db` file where a fact is
- $-1$ if was from the domain, or
- by `entailedByValue x` where `x` is the value it was predicted with, or
- occupies the whole line which means that a crisp (weight-free) inference method was used

and a second with runtime (`.db.time` with inference time in nanoseconds).

Another example is stored in `.\datasets\crosscountryski`. It is a (positive) variant of the *smoker* case in SRL -- we have database of 
friendships among people where one does cross-country skiing. We also have a rule saying that a friend of a cross-country ski-er 
also does cross-country skiing. You may play around with the inference method to see the impact of (limited) forward-chaining 
from classical entailment, through STRiKE with different $k$, all up to single-step. 
