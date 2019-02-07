***************
Developer Guide
***************

Welcome to the developers guide. The input data structure used for the nsp4j-optimizer tool is generated using the `nsp4j-api <https://github.com/fcarp10/nsp4j-api>`_ library. The program code of all constraints are defined in the file *../src/main/java/lp/Constraints*. Depending on the chosen objective function some constraints used for the optimization can be predefined and displayed in the browser based GUI. These settings can be defined in the file  *../src/main/resources/public/static/js/lp/form.js*. The structure and content of the browser based GUI cand be defined in the file  *../src/main/resources/public/index.html*

.. toctree::
   :maxdepth: 1
   :hidden:

   1_parameters.rst
   2_variables.rst
   3_general_constraints.rst
   4_opt_models.rst